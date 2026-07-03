// file: ui/celebration/StreakCelebrationManager.kt
package com.example.cares.ui.celebration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestisce lo stato della celebrazione dello streak a livello globale.
 *
 * Questo manager è responsabile di:
 * - Tracciare lo streak corrente da celebrare.
 * - Prevenire celebrazioni duplicate per lo stesso valore di streak.
 * - Fornire un'interfaccia pulita per attivare e chiudere le celebrazioni.
 *
 * **Flusso di utilizzo:**
 * 1. Quando un nuovo streak viene raggiunto, viene chiamato [triggerCelebration].
 * 2. Se lo streak è maggiore dell'ultimo celebrato, viene attivata la celebrazione.
 * 3. La UI osserva [streakToCelebrate] per mostrare la celebrazione.
 * 4. Dopo aver mostrato la celebrazione, la UI chiama [dismissCelebration].
 *
 * **Note:**
 * - La celebrazione viene attivata solo per streak crescenti.
 * - Se un utente raggiunge streak 5, poi 5 di nuovo, la celebrazione non viene riattivata.
 * - Per forzare il reset (es. in fase di debug), utilizzare [reset].
 */
object StreakCelebrationManager {

    // ================================ STATO ================================

    /**
     * Flow che contiene lo streak da celebrare, o null se nessuna celebrazione è attiva.
     *
     * La UI dovrebbe osservare questo flow per mostrare o nascondere la celebrazione.
     */
    private val _streakToCelebrate = MutableStateFlow<Int?>(null)
    val streakToCelebrate: StateFlow<Int?> = _streakToCelebrate.asStateFlow()

    /**
     * Ultimo valore di streak che è stato celebrato.
     * Utilizzato per prevenire celebrazioni duplicate dello stesso valore.
     */
    private var lastCelebratedStreak: Int = 0

    // ================================ API PUBBLICHE ================================

    /**
     * Attiva la celebrazione solo se lo streak è maggiore dell'ultimo celebrato.
     *
     * Questo previene riattivazioni multiple per lo stesso valore di streak,
     * evitando che la celebrazione venga mostrata più volte per lo stesso traguardo.
     *
     * @param streak Il valore dello streak da celebrare.
     */
    fun triggerCelebration(streak: Int) {
        if (streak > lastCelebratedStreak) {
            _streakToCelebrate.value = streak
            lastCelebratedStreak = streak
        }
    }

    /**
     * Chiude la celebrazione corrente.
     *
     * Da chiamare quando la UI ha terminato di mostrare la celebrazione,
     * ad esempio dopo l'animazione o dopo che l'utente ha chiuso il dialog.
     */
    fun dismissCelebration() {
        _streakToCelebrate.value = null
    }

    /**
     * Reset forzato dello stato del manager.
     *
     * Utile in scenari di debug o test, per resettare completamente lo stato
     * e permettere di testare nuovamente le celebrazioni.
     *
     * **Attenzione:** L'uso in produzione potrebbe causare la ripetizione
     * di celebrazioni già mostrate.
     */
    fun reset() {
        _streakToCelebrate.value = null
        lastCelebratedStreak = 0
    }
}