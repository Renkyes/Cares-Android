// file: data/models/LeaderboardEntry.kt
package com.example.cares.data.models

/**
 * Rappresenta un'entrata nella classifica (Leaderboard).
 *
 * Questa data class viene utilizzata per visualizzare la posizione
 * e le statistiche degli utenti nella classifica globale.
 *
 * **Utilizzo tipico:**
 * - Visualizzazione della classifica nella UI.
 * - Evidenziazione dell'utente corrente tra gli altri partecipanti.
 * - Calcolo della posizione relativa dell'utente.
 * - Confronto delle statistiche tra amici e altri utenti.
 *
 * **Ordinamento tipico:**
 * - Primario: XP (dall'alto verso il basso)
 * - Secondario: Livello (in caso di parità di XP)
 * - Terziario: Streak (in caso di ulteriore parità)
 *
 * @property userId Identificatore univoco dell'utente (UID di Firebase).
 * @property avatar Emoji o icona rappresentativa dell'utente (es. "🧙", "🦸", "🧝").
 * @property username Nome visualizzato dell'utente.
 * @property xp Esperienza totale accumulata. Determina la posizione in classifica.
 * @property level Livello raggiunto (calcolato da XP/100 + 1).
 * @property streak Serie di giorni consecutivi di attività (forza della sequenza).
 * @property completedQuests Numero totale di missioni completate (storico).
 * @property isCurrentUser Flag che indica se l'entrata corrisponde all'utente corrente.
 *                         Utilizzato per evidenziare l'utente nella classifica.
 *
 * @see LeaderboardManager per la gestione e il calcolo delle posizioni.
 */
data class LeaderboardEntry(
    val userId: String,
    val avatar: String,
    val username: String,
    val xp: Int,
    val level: Int,
    val streak: Int,
    val completedQuests: Int,
    val isCurrentUser: Boolean = false
) {
    /**
     * Crea una copia di questa entrata con il flag [isCurrentUser] impostato a `true`.
     *
     * Utile quando si recuperano dati da Firebase e si vuole marcare
     * l'utente corrente senza modificare l'istanza originale.
     *
     * @return Una nuova istanza con [isCurrentUser] = `true`.
     */
    fun markAsCurrentUser(): LeaderboardEntry {
        return this.copy(isCurrentUser = true)
    }

    /**
     * Verifica se l'utente ha raggiunto un livello minimo.
     *
     * @param minLevel Il livello minimo da verificare.
     * @return `true` se il livello dell'utente è >= minLevel.
     */
    fun hasReachedLevel(minLevel: Int): Boolean = level >= minLevel

    /**
     * Verifica se l'utente ha completato almeno un certo numero di missioni.
     *
     * @param minQuests Il numero minimo di missioni da verificare.
     * @return `true` se le missioni completate sono >= minQuests.
     */
    fun hasCompletedQuests(minQuests: Int): Boolean = completedQuests >= minQuests

    /**
     * Verifica se l'utente ha una streak attiva.
     *
     * @return `true` se la streak è maggiore di 0.
     */
    fun hasActiveStreak(): Boolean = streak > 0

    /**
     * Calcola la posizione stimata dell'utente in base a XP.
     *
     * **Nota:** Questo è un calcolo approssimativo. La posizione reale
     * viene calcolata confrontando tutti gli utenti nella classifica.
     *
     * @param totalUsers Numero totale di utenti nella classifica.
     * @return Posizione stimata (1 = primo posto).
     */
    fun getEstimatedRank(totalUsers: Int): Int {
        // Calcolo approssimativo: supponiamo una distribuzione uniforme
        // (valore tra 1 e totalUsers in base alla percentuale di XP)
        // NOTA: Per una classifica precisa, usare LeaderboardManager
        return 1
    }
}

/**
 * Estensione per creare una lista di [LeaderboardEntry] con indici di posizione.
 *
 * @param currentUserId ID dell'utente corrente per marcare l'entrata.
 * @return Lista di coppie (posizione, entry) dove la posizione parte da 1.
 */
fun List<LeaderboardEntry>.withRanks(currentUserId: String? = null): List<Pair<Int, LeaderboardEntry>> {
    return this.mapIndexed { index, entry ->
        // Se l'ID corrente è fornito e corrisponde, marca l'entrata
        val markedEntry = if (currentUserId != null && entry.userId == currentUserId) {
            entry.markAsCurrentUser()
        } else {
            entry
        }
        (index + 1) to markedEntry // La posizione è index + 1 (1-based)
    }
}

/**
 * Estensione per ottenere la posizione di un utente specifico nella classifica.
 *
 * @param userId ID dell'utente da cercare.
 * @return La posizione (1-based) dell'utente, o `null` se non trovato.
 */
fun List<LeaderboardEntry>.getRankOfUser(userId: String): Int? {
    return this.indexOfFirst { it.userId == userId }
        .takeIf { it >= 0 }
        ?.let { it + 1 } // La posizione è index + 1 (1-based)
}

/**
 * Estensione per filtrare la classifica per livello minimo.
 *
 * @param minLevel Il livello minimo richiesto.
 * @return Lista di entrate con livello >= minLevel.
 */
fun List<LeaderboardEntry>.filterByMinLevel(minLevel: Int): List<LeaderboardEntry> {
    return this.filter { it.level >= minLevel }
}

/**
 * Estensione per ottenere il valore massimo di XP nella classifica.
 *
 * @return Il valore massimo di XP, o 0 se la lista è vuota.
 */
fun List<LeaderboardEntry>.getMaxXp(): Int {
    return this.maxOfOrNull { it.xp } ?: 0
}

/**
 * Estensione per ottenere il valore medio di XP nella classifica.
 *
 * @return Il valore medio di XP, o 0.0 se la lista è vuota.
 */
fun List<LeaderboardEntry>.getAverageXp(): Double {
    return if (this.isEmpty()) 0.0 else this.map { it.xp }.average()
}