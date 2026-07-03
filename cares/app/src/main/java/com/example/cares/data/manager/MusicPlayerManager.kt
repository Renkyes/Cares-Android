// file: data/manager/MusicPlayerManager.kt
package com.example.cares.data.manager

import android.content.Context
import android.media.MediaPlayer
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.cares.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Estensione per il DataStore delle impostazioni.
 */
private val Context.dataStore by preferencesDataStore(name = "settings_prefs")

/**
 * Gestisce la riproduzione della musica di sottofondo come singleton.
 *
 * Il player persiste tra le diverse schermate e mantiene lo stato di riproduzione
 * anche quando l'app è in background (pausa) o in primo piano (riproduzione).
 *
 * La preferenza "musica abilitata" viene salvata in DataStore in modo persistente,
 * in modo che lo stato venga mantenuto tra una sessione e l'altra.
 *
 * **Note sul ciclo di vita:**
 * - [init] deve essere chiamato una volta all'avvio dell'app.
 * - [start] e [pause] vengono invocati rispettivamente in onResume e onPause dell'activity.
 * - [stop] viene chiamato in onDestroy per rilasciare le risorse.
 */
object MusicPlayerManager {

    // ================================ COSTANTI ================================

    /** Chiave privata per il salvataggio dello stato della musica. */
    private const val MUSIC_KEY = "background_music_enabled"

    /** Chiave pubblica esposta per leggere o osservare la preferenza. */
    val MUSIC_ENABLED_KEY = booleanPreferencesKey(MUSIC_KEY)

    // ================================ STATO INTERNO ================================

    /** Istanza del MediaPlayer per la riproduzione della musica. */
    private var mediaPlayer: MediaPlayer? = null

    /** Flag che indica se la musica è abilitata (secondo le preferenze). */
    private var isEnabled: Boolean = true

    /** Flag per evitare inizializzazioni multiple. */
    private var isInitialized: Boolean = false

    // ================================ API PUBBLICHE ================================

    /**
     * Inizializza il player leggendo le preferenze salvate.
     *
     * La lettura delle preferenze avviene in modo sincrono tramite [runBlocking],
     * poiché l'inizializzazione deve essere completata prima che l'app inizi
     * a interagire con l'utente. Questo blocco è breve e non blocca l'UI.
     *
     * @param context Il contesto necessario per accedere alle risorse e al DataStore.
     */
    fun init(context: Context) {
        if (isInitialized) return

        // Legge lo stato salvato in modo sincrono (una tantum all'avvio)
        runBlocking {
            val prefs = context.dataStore.data.first()
            isEnabled = prefs[MUSIC_ENABLED_KEY] ?: true
        }

        // Crea il player solo se la musica è abilitata
        if (isEnabled) {
            createMediaPlayer(context)
        }
        isInitialized = true
    }

    /**
     * Avvia la riproduzione della musica.
     *
     * La riproduzione parte solo se:
     * - La musica è abilitata nelle preferenze.
     * - Il MediaPlayer è stato creato e non è già in riproduzione.
     */
    fun start() {
        if (isEnabled && mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
        }
    }

    /**
     * Mette in pausa la riproduzione (senza rilasciare le risorse).
     *
     * Va chiamato quando l'app va in background per evitare che la musica
     * continui a riprodursi mentre l'utente è su altre app.
     */
    fun pause() {
        mediaPlayer?.pause()
    }

    /**
     * Ferma la musica e rilascia le risorse del MediaPlayer.
     *
     * Deve essere chiamato alla distruzione dell'app (onDestroy)
     * per evitare memory leak e rilasciare il file audio.
     */
    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isInitialized = false
    }

    /**
     * Attiva o disattiva la musica e salva la preferenza in modo persistente.
     *
     * Se viene abilitata e il MediaPlayer non esiste, viene creato e avviato.
     * Se viene disabilitata, la riproduzione viene messa in pausa.
     *
     * @param context Il contesto per accedere al DataStore.
     * @param enabled True per attivare la musica, false per disattivarla.
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        isEnabled = enabled

        if (enabled) {
            // Crea il player solo se non esiste già
            if (mediaPlayer == null) {
                createMediaPlayer(context)
            }
            start()
        } else {
            pause()
        }

        // Salva la preferenza in modo sincrono (operazione breve)
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[MUSIC_ENABLED_KEY] = enabled
            }
        }
    }

    /**
     * Restituisce lo stato corrente della musica (abilitata/disabilitata).
     *
     * @return True se la musica è abilitata, false altrimenti.
     */
    fun isMusicEnabled(): Boolean = isEnabled

    // ================================ FUNZIONI PRIVATE ================================

    /**
     * Crea e configura il MediaPlayer.
     *
     * Il file audio è atteso in `res/raw/backgroun_music`.
     * Il player viene impostato in loop e il volume viene ridotto al 50% per
     * non sovrastare eventuali altri suoni o voci.
     *
     * In caso di errore (es. file mancante), l'eccezione viene stampata a schermo
     * e il player rimane null.
     *
     * @param context Il contesto per accedere alle risorse.
     */
    private fun createMediaPlayer(context: Context) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, R.raw.backgroun_music).apply {
                isLooping = true
                setVolume(0.5f, 0.5f)
            }
        } catch (e: Exception) {
            // Log dell'errore: il file potrebbe non esistere o essere corrotto
            e.printStackTrace()
        }
    }
}