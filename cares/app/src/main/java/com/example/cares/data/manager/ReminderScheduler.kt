// file: data/manager/ReminderScheduler.kt
package com.example.cares.data.manager

import android.content.Context
import androidx.work.*
import com.example.cares.utils.checkAndResetQuests
import com.example.cares.utils.generateDailyQuests
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Worker eseguito periodicamente per verificare lo stato delle missioni e inviare notifiche.
 *
 * Il Worker viene eseguito ogni 15 minuti e svolge due controlli:
 * 1. **Promemoria di scadenza**: se il timer della missione corrente è ≤ 5 minuti,
 *    invia una notifica di promemoria all'utente.
 * 2. **Reset giornaliero**: se è iniziato un nuovo giorno, resetta le missioni completate
 *    e invia una notifica per segnalare la disponibilità di nuove missioni.
 *
 * @see ReminderScheduler per la configurazione periodica.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // ================================ METODI PUBBLICI ================================

    /**
     * Esegue il lavoro in background in modo asincrono.
     *
     * **Flusso di esecuzione:**
     * 1. Recupera il tempo rimanente del timer della missione.
     * 2. Se il timer è tra 1 e 300 secondi (≤ 5 minuti), invia un promemoria.
     * 3. Verifica se è iniziato un nuovo giorno e, in caso affermativo,
     *    resetta le missioni e invia una notifica.
     *
     * @return [Result.success] se il lavoro è stato completato correttamente,
     *         [Result.failure] in caso di errore (non utilizzato in questa implementazione).
     */
    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = UserPreferencesManager(context)
        val notificationHelper = NotificationHelper(context)

        // Controlla se il timer della missione è in scadenza (≤ 5 minuti)
        val remainingSeconds = prefs.getMissionTimerRemaining().first()
        if (remainingSeconds in 1..300) {
            val questTitle = getCurrentQuestTitle(prefs)
            notificationHelper.sendReminderNotification(questTitle)
        }

        // Controlla se è un nuovo giorno e resetta le missioni
        val isNewDay = checkAndResetQuests(prefs)
        if (isNewDay) {
            notificationHelper.sendNewDayNotification()
        }

        return Result.success()
    }

    // ================================ METODI PRIVATI ================================

    /**
     * Recupera il titolo della missione corrente in base al livello dell'utente.
     *
     * Le missioni giornaliere vengono generate al momento della chiamata.
     * Se non ci sono missioni disponibili, restituisce un titolo di fallback.
     *
     * @param prefs Il manager per accedere alle preferenze utente.
     * @return Il titolo della prima missione disponibile, oppure "Missione" come fallback.
     */
    private suspend fun getCurrentQuestTitle(prefs: UserPreferencesManager): String {
        val userLevel = prefs.getLevel().first()
        val activityLevel = prefs.getActivityLevel().first()
        val fitnessGoal = prefs.getFitnessGoal().first()
        val dailyGoal = prefs.getDailyGoal().first()
        val quests = generateDailyQuests(userLevel, activityLevel, fitnessGoal, dailyGoal)
        return quests.firstOrNull()?.title ?: "Missione"
    }
}

/**
 * Scheduler per avviare e fermare il worker periodico delle notifiche.
 *
 * Il Worker viene eseguito ogni 15 minuti per garantire che:
 * - I promemoria di scadenza vengano inviati tempestivamente.
 * - Le missioni giornaliere vengano resettate entro pochi minuti dalla mezzanotte.
 *
 * **Note sul ciclo di vita:**
 * - [scheduleReminder] deve essere chiamato all'avvio dell'app (MainActivity).
 * - [cancelReminder] può essere usato per fermare il worker (es. durante il logout).
 *
 * **Perché 15 minuti?**
 * Il periodo di 15 minuti è un buon compromesso tra:
 * - Tempestività delle notifiche (massimo 15 minuti di ritardo).
 * - Consumo di batteria (minore rispetto a intervalli più brevi).
 * - Vincoli di sistema (WorkManager non garantisce esecuzioni più frequenti).
 */
object ReminderScheduler {

    // ================================ COSTANTI ================================

    /** Tag univoco per identificare il worker e gestirlo in modo univoco. */
    private const val WORK_TAG = "reminder_work"

    /** Intervallo di esecuzione del worker in minuti. */
    private const val REMINDER_INTERVAL_MINUTES = 15L

    // ================================ API PUBBLICHE ================================

    /**
     * Avvia il worker periodico che esegue ogni 15 minuti.
     *
     * Utilizza [ExistingPeriodicWorkPolicy.KEEP] per evitare la creazione di worker duplicati
     * se [scheduleReminder] viene chiamato più volte (es. a ogni avvio dell'app).
     *
     * **Vincoli:**
     * - Nessun vincolo sulla batteria, il worker può eseguire anche quando la batteria è scarica.
     * - WorkManager gestisce automaticamente il fallback in caso di assenza di rete o altre condizioni.
     *
     * @param context Il contesto necessario per accedere a WorkManager.
     */
    fun scheduleReminder(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false) // Lavoro non critico, non bloccare per batteria
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            REMINDER_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP, // Mantiene l'istanza esistente
                workRequest
            )
    }

    /**
     * Ferma il worker periodico delle notifiche cancellando tutti i worker associati al tag.
     *
     * Utile in scenari come:
     * - Logout dell'utente.
     * - Disattivazione delle notifiche dalle impostazioni.
     * - Debug o test.
     *
     * @param context Il contesto necessario per accedere a WorkManager.
     */
    fun cancelReminder(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
    }
}