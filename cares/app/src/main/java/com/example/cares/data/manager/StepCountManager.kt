// file: data/manager/StepCounterManager.kt
package com.example.cares.data.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.cares.data.repository.CaresRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestisce il sensore dei passi (TYPE_STEP_COUNTER) con persistenza.
 *
 * I valori base e l'ultimo totale vengono salvati in DataStore,
 * permettendo di mantenere il conteggio anche dopo la chiusura dell'app.
 *
 * **Strategia di persistenza:**
 * - `baseStepCount`: valore del sensore all'inizio della giornata (salvato in DataStore).
 * - `lastTotalSteps`: ultimo valore letto dal sensore (salvato in DataStore).
 * - `lastResetDate`: data dell'ultimo reset, per rilevare l'inizio di un nuovo giorno.
 *
 * **Note sul funzionamento:**
 * - Il sensore TYPE_STEP_COUNTER fornisce un conteggio totale dei passi dal riavvio del dispositivo.
 * - I passi giornalieri vengono calcolati come differenza tra il totale corrente e il base.
 * - All'inizio di un nuovo giorno, il base viene resettato e ricalcolato al primo evento.
 */
object StepCounterManager {

    // ================================ STATO INTERNO ================================

    /** Manager per l'accesso ai sensori del dispositivo. */
    private var sensorManager: SensorManager? = null

    /** Sensore dei passi (TYPE_STEP_COUNTER o fallback TYPE_STEP_DETECTOR). */
    private var stepCounterSensor: Sensor? = null

    /** Flag che indica se il listener è attivo. */
    private var isListening: Boolean = false

    /** Repository per l'accesso ai dati persistenti. */
    private var repository: CaresRepository? = null

    /** Scope per le operazioni asincrone su DataStore. */
    private val scope = CoroutineScope(Dispatchers.IO)

    /** Flow per osservare i passi di oggi. */
    private val _stepsToday = MutableStateFlow(0)
    val stepsToday: StateFlow<Int> = _stepsToday.asStateFlow()

    // ================================ VALORI PERSISTITI ================================

    /** Valore del sensore all'inizio della giornata. */
    private var baseStepCount: Long = 0L

    /** Ultimo valore totale letto dal sensore. */
    private var lastTotalSteps: Long = 0L

    /** Data dell'ultimo reset nel formato "yyyy-MM-dd". */
    private var lastResetDate: String = ""

    // ================================ LISTENER DEL SENSORE ================================

    /**
     * Listener per gli eventi del sensore dei passi.
     *
     * **Flusso di aggiornamento:**
     * 1. Riceve il totale dei passi dal sensore.
     * 2. Se il base non è impostato, lo imposta al valore corrente.
     * 3. Calcola i passi del giorno come differenza (totale - base).
     * 4. Aggiorna il StateFlow e salva l'ultimo totale se cambiato.
     */
    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    val totalSteps = it.values[0].toLong()

                    // Se non abbiamo ancora la base, la impostiamo al valore corrente
                    if (baseStepCount == 0L) {
                        baseStepCount = totalSteps
                        saveBaseCount()
                    }

                    // Calcola i passi del giorno (assicurandoci che non sia negativo)
                    val todaySteps = (totalSteps - baseStepCount).toInt()
                    if (todaySteps >= 0) {
                        _stepsToday.value = todaySteps
                    }

                    // Salva l'ultimo totale se è cambiato
                    if (lastTotalSteps != totalSteps) {
                        lastTotalSteps = totalSteps
                        saveLastTotal()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Non utilizzato, ma richiesto dall'interfaccia
        }
    }

    // ================================ API PUBBLICHE ================================

    /**
     * Inizializza il manager con il contesto e il repository per la persistenza.
     * Deve essere chiamato una volta all'avvio dell'app.
     *
     * **Flusso:**
     * 1. Ottiene i sensori dal sistema.
     * 2. Se TYPE_STEP_COUNTER non è disponibile, usa TYPE_STEP_DETECTOR come fallback.
     * 3. Ripristina i valori salvati da DataStore.
     * 4. Se è un nuovo giorno, resetta il conteggio.
     *
     * @param context    Il contesto per accedere ai servizi di sistema.
     * @param repository Il repository per la persistenza dei dati.
     */
    fun init(context: Context, repository: CaresRepository) {
        this.repository = repository

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounterSensor == null) {
            // Fallback: TYPE_STEP_DETECTOR (meno preciso ma disponibile su più dispositivi)
            stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        }

        // Ripristina i valori salvati
        restorePersistedValues()
    }

    /**
     * Avvia l'ascolto del sensore.
     *
     * Se il sensore non è disponibile, l'operazione viene ignorata.
     * Se il base è zero (appena riavviata o nuovo giorno), verrà impostato
     * al primo evento ricevuto.
     */
    fun startListening() {
        if (!isListening && stepCounterSensor != null && sensorManager != null) {
            sensorManager?.registerListener(
                listener,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_UI
            )
            isListening = true
        }
    }

    /**
     * Ferma l'ascolto del sensore.
     *
     * Da chiamare quando l'app va in background o viene distrutta
     * per risparmiare batteria.
     */
    fun stopListening() {
        if (isListening) {
            sensorManager?.unregisterListener(listener)
            isListening = false
        }
    }

    /**
     * Resetta il conteggio di base (da chiamare a inizio giornata).
     *
     * Imposta il base a 0, quindi verrà ricalcolato al prossimo evento del sensore.
     * Aggiorna anche la data di reset per evitare reset multipli nello stesso giorno.
     */
    fun resetBaseCount() {
        baseStepCount = 0L
        lastTotalSteps = 0L
        _stepsToday.value = 0

        scope.launch {
            repository?.resetStepCounter()
            val today = getTodayDate()
            repository?.setStepCounterLastResetDate(today)
            lastResetDate = today
        }
    }

    /**
     * Verifica se il sensore dei passi è disponibile sul dispositivo.
     *
     * @return True se almeno uno dei sensori (TYPE_STEP_COUNTER o TYPE_STEP_DETECTOR)
     *         è disponibile, false altrimenti.
     */
    fun isStepCounterAvailable(): Boolean = stepCounterSensor != null

    // ================================ FUNZIONI PRIVATE ================================

    /**
     * Ripristina i valori salvati in DataStore.
     *
     * Se è un nuovo giorno (data diversa), resetta il conteggio.
     * Altrimenti, ripristina base e ultimo totale.
     */
    private fun restorePersistedValues() {
        scope.launch {
            val repo = repository ?: return@launch
            try {
                val today = getTodayDate()
                lastResetDate = repo.getStepCounterLastResetDate().first()

                // Se è un nuovo giorno, resetta tutto
                if (lastResetDate != today) {
                    // Resetta il base: sarà ricalcolato al primo evento sensore
                    baseStepCount = 0L
                    lastTotalSteps = 0L
                    _stepsToday.value = 0

                    repo.resetStepCounter()
                    repo.setStepCounterLastResetDate(today)
                    lastResetDate = today
                } else {
                    // Ripristina i valori salvati
                    baseStepCount = repo.getStepCounterBase().first()
                    lastTotalSteps = repo.getStepCounterLastTotal().first()

                    // Se abbiamo un base ma non possiamo calcolare i passi oggi
                    // (non abbiamo il totale corrente del sensore, lo faremo al primo evento)
                    // Nel frattempo, mostriamo 0
                    _stepsToday.value = 0
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // In caso di errore, resetta tutto per sicurezza
                baseStepCount = 0L
                lastTotalSteps = 0L
                _stepsToday.value = 0
            }
        }
    }

    /**
     * Salva il valore base in DataStore.
     */
    private fun saveBaseCount() {
        scope.launch {
            repository?.setStepCounterBase(baseStepCount)
        }
    }

    /**
     * Salva l'ultimo totale in DataStore.
     */
    private fun saveLastTotal() {
        scope.launch {
            repository?.setStepCounterLastTotal(lastTotalSteps)
        }
    }

    /**
     * Ottiene la data odierna nel formato "yyyy-MM-dd".
     *
     * @return La data corrente come stringa formattata.
     */
    private fun getTodayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}