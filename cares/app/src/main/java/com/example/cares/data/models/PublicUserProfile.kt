// file: data/models/PublicUserProfile.kt
package com.example.cares.data.models

/**
 * Rappresenta il profilo pubblico di un utente, visualizzabile da altri utenti.
 *
 * Questo modello contiene tutte le informazioni che appaiono nella schermata
 * del profilo di un utente quando visitato da altri. Non include dati sensibili
 * come email, preferenze personali o dati di autenticazione.
 *
 * **Utilizzo tipico:**
 * - Visualizzazione del profilo di un amico o di un altro utente.
 * - Condivisione dei risultati e dei traguardi raggiunti.
 * - Confronto tra profili nella classifica.
 * - Visualizzazione delle statistiche pubbliche di un utente.
 *
 * **Differenze con il profilo privato:**
 * - Il profilo privato ([UserPreferencesManager]) contiene impostazioni,
 *   preferenze e dati sensibili.
 * - Il profilo pubblico ([PublicUserProfile]) contiene solo statistiche
 *   e risultati condivisibili.
 *
 * @property userId Identificatore univoco dell'utente (UID di Firebase).
 * @property avatar Emoji o icona rappresentativa dell'utente (es. "🧙", "🦸", "🧝").
 * @property username Nome visualizzato dell'utente.
 * @property xp Esperienza totale accumulata.
 * @property level Livello raggiunto (calcolato da XP/100 + 1).
 * @property streak Serie di giorni consecutivi di attività.
 * @property completedQuests Numero totale di missioni completate (storico).
 * @property unlockedBadges Insieme degli ID dei badge sbloccati dall'utente.
 * @property unlockedChapters Insieme degli ID dei capitoli della storia sbloccati.
 * @property friends Lista di informazioni sintetiche sugli amici dell'utente.
 *
 * @see FriendInfo
 * @see UserPreferencesManager per i dati del profilo privato.
 */
data class PublicUserProfile(
    val userId: String,
    val avatar: String,
    val username: String,
    val xp: Int,
    val level: Int,
    val streak: Int,
    val completedQuests: Int,
    val unlockedBadges: Set<String>,
    val unlockedChapters: Set<String>,
    val friends: List<FriendInfo>
) {
    /**
     * Verifica se l'utente ha sbloccato un badge specifico.
     *
     * @param badgeId L'ID del badge da verificare.
     * @return `true` se il badge è sbloccato, `false` altrimenti.
     */
    fun hasBadge(badgeId: String): Boolean = unlockedBadges.contains(badgeId)

    /**
     * Verifica se l'utente ha sbloccato un capitolo specifico.
     *
     * @param chapterId L'ID del capitolo da verificare.
     * @return `true` se il capitolo è sbloccato, `false` altrimenti.
     */
    fun hasChapter(chapterId: String): Boolean = unlockedChapters.contains(chapterId)

    /**
     * Restituisce il numero di badge sbloccati.
     *
     * @return Conteggio dei badge sbloccati.
     */
    fun getBadgeCount(): Int = unlockedBadges.size

    /**
     * Restituisce il numero di capitoli sbloccati.
     *
     * @return Conteggio dei capitoli sbloccati.
     */
    fun getChapterCount(): Int = unlockedChapters.size

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
     * Verifica se l'utente ha raggiunto un livello minimo.
     *
     * @param minLevel Il livello minimo da verificare.
     * @return `true` se il livello dell'utente è >= minLevel.
     */
    fun hasReachedLevel(minLevel: Int): Boolean = level >= minLevel

    /**
     * Restituisce una versione del profilo con solo le informazioni di base.
     *
     * Utile per anteprime in lista o quando si vuole ridurre il carico dati.
     *
     * @return Un profilo minimale con solo ID, avatar, username e XP.
     */
    fun toMinimalProfile(): MinimalUserProfile {
        return MinimalUserProfile(
            userId = userId,
            avatar = avatar,
            username = username,
            xp = xp
        )
    }
}

/**
 * Versione minimale del profilo di un utente, contenente solo le informazioni essenziali.
 *
 * **Utilizzo tipico:**
 * - Visualizzazione in liste di amici.
 * - Visualizzazione in classifiche e ranking.
 * - Anteprime veloci senza caricare dati completi.
 * - Riduzione del carico di rete per operazioni frequenti.
 *
 * @property userId Identificatore univoco dell'utente.
 * @property avatar Emoji o icona rappresentativa dell'utente.
 * @property username Nome visualizzato dell'utente.
 * @property xp Esperienza totale accumulata.
 */
data class MinimalUserProfile(
    val userId: String,
    val avatar: String,
    val username: String,
    val xp: Int
) {
    /**
     * Crea un profilo pubblico completo a partire da questo profilo minimale
     * con valori di default per i campi mancanti.
     *
     * @param level Livello da impostare (default: 1).
     * @param streak Streak da impostare (default: 0).
     * @param completedQuests Missioni completate (default: 0).
     * @param unlockedBadges Badge sbloccati (default: vuoto).
     * @param unlockedChapters Capitoli sbloccati (default: vuoto).
     * @param friends Lista amici (default: vuota).
     * @return Un profilo pubblico completo.
     */
    fun toFullProfile(
        level: Int = 1,
        streak: Int = 0,
        completedQuests: Int = 0,
        unlockedBadges: Set<String> = emptySet(),
        unlockedChapters: Set<String> = emptySet(),
        friends: List<FriendInfo> = emptyList()
    ): PublicUserProfile {
        return PublicUserProfile(
            userId = userId,
            avatar = avatar,
            username = username,
            xp = xp,
            level = level,
            streak = streak,
            completedQuests = completedQuests,
            unlockedBadges = unlockedBadges,
            unlockedChapters = unlockedChapters,
            friends = friends
        )
    }
}

/**
 * Informazioni minime su un amico per la visualizzazione in elenchi.
 *
 * **Utilizzo tipico:**
 * - Visualizzazione della lista degli amici nel profilo.
 * - Invio di sfide o richieste di amicizia.
 * - Lista di contatti per la classifica tra amici.
 *
 * @property userId Identificatore univoco dell'amico.
 * @property username Nome visualizzato dell'amico.
 * @property avatar Emoji o icona rappresentativa dell'amico.
 */
data class FriendInfo(
    val userId: String,
    val username: String,
    val avatar: String
) {
    /**
     * Verifica se l'amico ha un avatar valido.
     *
     * @return `true` se l'avatar non è vuoto.
     */
    fun hasValidAvatar(): Boolean = avatar.isNotBlank()

    /**
     * Verifica se l'amico ha un username valido.
     *
     * @return `true` se l'username non è vuoto.
     */
    fun hasValidUsername(): Boolean = username.isNotBlank()
}

/**
 * Estensione per convertire una lista di [FriendInfo] in una lista di ID.
 */
fun List<FriendInfo>.toUserIdList(): List<String> = this.map { it.userId }

/**
 * Estensione per verificare se una lista di amici contiene un ID specifico.
 */
fun List<FriendInfo>.containsUserId(userId: String): Boolean =
    this.any { it.userId == userId }