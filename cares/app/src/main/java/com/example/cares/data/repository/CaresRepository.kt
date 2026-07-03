// file: data/repository/CaresRepository.kt
package com.example.cares.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import com.example.cares.data.manager.FirebaseAuthManager
import com.example.cares.data.manager.StoryManager
import com.example.cares.data.manager.UserPreferencesManager
import com.example.cares.data.models.FriendRequest
import com.example.cares.data.models.LeaderboardEntry
import com.example.cares.utils.Quest
import com.example.cares.utils.checkAndResetQuests
import com.example.cares.utils.generateDailyQuests
import com.example.cares.utils.generateMonthlyQuests
import com.example.cares.utils.generateWeeklyQuests
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import com.example.cares.data.models.FriendInfo
import com.example.cares.data.models.PublicUserProfile
import com.example.cares.data.manager.UserPreferencesManager.XpBoostData

/**
 * Repository centrale che gestisce l'accesso ai dati locali (DataStore) e remoti (Firestore).
 *
 * **Responsabilità principali:**
 * - Fornire un'interfaccia unificata per i ViewModel.
 * - Gestire la persistenza locale tramite DataStore.
 * - Sincronizzare i dati con Firestore.
 * - Gestire il fallback in modalità offline.
 *
 * **Strategia di sincronizzazione:**
 * - I dati vengono prima salvati in locale (DataStore) per garantire la reattività.
 * - La sincronizzazione remota (Firestore) avviene in background tramite [syncUserData].
 * - In assenza di rete o in modalità locale (ID utente che inizia con "local_user_"),
 *   i dati remoti vengono sostituiti da dati di fallback generati localmente.
 *
 * **Thread-safety:** Tutte le operazioni sono sospendibili e thread-safe grazie a DataStore e Firestore.
 *
 * @param context Il contesto per l'accesso a DataStore e altre risorse.
 */
class CaresRepository(private val context: Context) {

    // ================================ PROPRIETÀ ================================

    /** Gestore delle preferenze utente per l'accesso a DataStore. */
    private val prefs = UserPreferencesManager(context)

    /** Istanza di Firestore per l'accesso al database remoto. */
    private val firestore = FirebaseFirestore.getInstance()

    /** Nome della collezione degli utenti su Firestore. */
    private val USERS_COLLECTION = "users"

    /** Flusso condiviso per gli eventi di aggiornamento dello streak. */
    private val _streakEvents = MutableSharedFlow<Int>(replay = 0)

    /** Esposizione pubblica degli eventi di streak. */
    val streakEvents: SharedFlow<Int> = _streakEvents.asSharedFlow()

    /**
     * Restituisce il contesto dell'applicazione, utile per i ViewModel che necessitano di risorse.
     */
    fun getContext(): Context = context

    // ================================ DATI UTENTE (ONBOARDING E PREFERENZE) ================================

    /**
     * Salva le preferenze iniziali dell'utente (avatar, livello attività, obiettivo fitness, nome utente).
     *
     * Questa operazione viene eseguita durante l'onboarding e inizializza tutte le statistiche
     * di gioco a zero (XP, livello, streak, missioni, badge).
     *
     * @param avatar L'emoji dell'avatar selezionato.
     * @param level  Il livello di attività ("Sedentario", "Moderato", "Attivo").
     * @param goal   L'obiettivo fitness ("Perdere peso", "Tonificare", "Resistenza").
     * @param username Il nome utente scelto.
     */
    suspend fun saveUserPreferences(
        avatar: String,
        level: String,
        goal: String,
        username: String
    ) {
        prefs.saveUserPreferences(avatar, level, goal, username)
    }

    /**
     * Salva le preferenze iniziali dell'utente, includendo l'obiettivo giornaliero.
     *
     * @param avatar L'emoji dell'avatar selezionato.
     * @param level  Il livello di attività ("Sedentario", "Moderato", "Attivo").
     * @param goal   L'obiettivo fitness ("Perdere peso", "Tonificare", "Resistenza").
     * @param username Il nome utente scelto.
     * @param dailyGoal L'obiettivo quotidiano (es. "5000 passi", "50 push-up").
     */
    suspend fun saveUserPreferences(
        avatar: String,
        level: String,
        goal: String,
        username: String,
        dailyGoal: String
    ) {
        prefs.saveUserPreferences(avatar, level, goal, username, dailyGoal)
    }

    /** Restituisce un Flow che indica se l'onboarding è stato completato. */
    fun getOnboardingStatus(): Flow<Boolean> = prefs.getOnboardingStatus()

    /** Restituisce un Flow con tutte le preferenze utente in un unico oggetto [Preferences]. */
    fun getAllPreferences(): Flow<Preferences> = prefs.getAllPreferences()

    // ================================ DIARIO DELLE EMOZIONI ================================

    /** Salva l'umore di oggi (valore tra 1 e 5). */
    suspend fun saveMood(mood: Int) = prefs.saveMood(mood)

    /** Recupera l'umore di oggi. */
    suspend fun getTodayMood(): Int? = prefs.getTodayMood()

    /** Recupera la cronologia degli umori per gli ultimi N giorni. */
    suspend fun getMoodHistory(days: Int = 7): Map<String, Int> = prefs.getMoodHistory(days)

    /** Calcola la media degli umori degli ultimi N giorni. */
    suspend fun getAverageMood(days: Int = 7): Float = prefs.getAverageMood(days)

    /** Restituisce il livello di attività dell'utente. */
    fun getActivityLevel(): Flow<String> = prefs.getActivityLevel()

    /** Restituisce l'obiettivo fitness dell'utente. */
    fun getFitnessGoal(): Flow<String> = prefs.getFitnessGoal()

    // ================================ OBIETTIVO GIORNALIERO ================================

    /** Restituisce l'obiettivo giornaliero dell'utente come Flow. Default: "Nessun obiettivo". */
    fun getDailyGoal(): Flow<String> = prefs.getDailyGoal()

    /** Imposta l'obiettivo giornaliero dell'utente. */
    suspend fun setDailyGoal(goal: String) = prefs.setDailyGoal(goal)

    // ================================ XP E LIVELLO ================================

    /** Restituisce l'esperienza totale dell'utente. */
    fun getXp(): Flow<Int> = prefs.getXp()

    /** Restituisce il livello corrente dell'utente. */
    fun getLevel(): Flow<Int> = prefs.getLevel()

    /**
     * Aggiunge XP all'utente e aggiorna automaticamente il livello.
     * Applica automaticamente il moltiplicatore se un Boost XP è attivo.
     *
     * @param amount La quantità di XP da aggiungere.
     * @return L'XP effettivamente aggiunto (dopo l'applicazione del moltiplicatore).
     */
    suspend fun addXp(amount: Int) = prefs.addXp(amount)

    // ================================ MISSIONI GIORNALIERE ================================

    /** Restituisce l'insieme degli ID delle missioni completate oggi. */
    fun getCompletedQuests(): Flow<Set<String>> = prefs.getCompletedQuests()

    /**
     * Completa una missione giornaliera.
     * Incrementa il totale storico e sincronizza i dati su Firestore.
     *
     * @param questId L'ID della missione completata.
     */
    suspend fun completeQuest(questId: String) {
        prefs.completeQuest(questId)
        syncUserData()
    }

    /**
     * Genera le missioni giornaliere in base al profilo dell'utente.
     * Se non vengono forniti parametri, legge automaticamente i valori dal DataStore.
     *
     * @param level         Livello dell'utente (se null, letto da DataStore).
     * @param activityLevel Livello di attività (se null, letto da DataStore).
     * @param fitnessGoal   Obiettivo fitness (se null, letto da DataStore).
     * @param dailyGoal     Obiettivo giornaliero (se null, letto da DataStore).
     * @return Lista di [Quest] personalizzate.
     */
    suspend fun getDailyQuests(
        level: Int? = null,
        activityLevel: String? = null,
        fitnessGoal: String? = null,
        dailyGoal: String? = null
    ): List<Quest> {
        val actualLevel = level ?: getLevel().first()
        val actualActivity = activityLevel ?: getActivityLevel().first()
        val actualFitnessGoal = fitnessGoal ?: getFitnessGoal().first()
        val actualDailyGoal = dailyGoal ?: getDailyGoal().first()
        return generateDailyQuests(actualLevel, actualActivity, actualFitnessGoal, actualDailyGoal)
    }

    /**
     * Verifica se è un nuovo giorno e, in caso affermativo, resetta le missioni.
     *
     * @return `true` se è stato effettuato un reset, `false` altrimenti.
     */
    suspend fun checkAndResetQuests(): Boolean = checkAndResetQuests(prefs)

    // ================================ CONTATORE PASSI ================================

    /** Restituisce il valore base del contatore passi. */
    fun getStepCounterBase(): Flow<Long> = prefs.getStepCounterBase()

    /** Salva il valore base del contatore passi. */
    suspend fun setStepCounterBase(value: Long) = prefs.setStepCounterBase(value)

    /** Restituisce l'ultimo valore totale del contatore passi. */
    fun getStepCounterLastTotal(): Flow<Long> = prefs.getStepCounterLastTotal()

    /** Salva l'ultimo valore totale del contatore passi. */
    suspend fun setStepCounterLastTotal(value: Long) = prefs.setStepCounterLastTotal(value)

    /** Resetta i dati del contatore passi. */
    suspend fun resetStepCounter() = prefs.resetStepCounter()

    /** Restituisce la data dell'ultimo reset del contatore passi. */
    fun getStepCounterLastResetDate(): Flow<String> = prefs.getStepCounterLastResetDate()

    /** Imposta la data dell'ultimo reset del contatore passi. */
    suspend fun setStepCounterLastResetDate(date: String) = prefs.setStepCounterLastResetDate(date)

    // ================================ STREAK ================================

    /** Restituisce lo streak corrente (giorni consecutivi di attività). */
    fun getStreak(): Flow<Int> = prefs.getStreak()

    /**
     * Aggiorna lo streak in base alla data di oggi.
     * Emette un evento se lo streak è aumentato.
     *
     * @return Il nuovo valore dello streak.
     */
    suspend fun updateStreak(): Int {
        val oldStreak = getStreak().first()
        val newStreak = prefs.updateStreak()
        if (newStreak > oldStreak) {
            _streakEvents.emit(newStreak)
        }
        return newStreak
    }

    /**
     * Controlla se lo streak è scaduto e, in caso affermativo,
     * lo mette in stato di "pericolo" stile Duolingo.
     */
    suspend fun checkStreakExpiry() = prefs.checkStreakExpiry()

    // ================================ STREAK IN PERICOLO (stile Duolingo) ================================

    /** Restituisce `true` se la streak è attualmente in pericolo. */
    fun getStreakAtRisk(): Flow<Boolean> = prefs.getStreakAtRisk()

    /** Restituisce il valore della streak salvato quando è entrata in pericolo. */
    fun getStreakRiskSavedStreak(): Flow<Int> = prefs.getStreakRiskSavedStreak()

    /**
     * Salva la streak usando uno scudo (consuma uno scudo dall'inventario).
     * @return `true` se lo scudo è stato usato con successo, `false` altrimenti.
     */
    suspend fun saveStreakWithShield(): Boolean = prefs.saveStreakWithShield()

    /** Perde la streak (reset a 0) e rimuove lo stato di pericolo. */
    suspend fun loseStreak() = prefs.loseStreak()

    // ================================ BADGE ================================

    /** Restituisce l'insieme degli ID dei badge sbloccati. */
    fun getUnlockedBadges(): Flow<Set<String>> = prefs.getUnlockedBadges()

    /** Sblocca un badge per l'utente. */
    suspend fun unlockBadge(badgeId: String) = prefs.unlockBadge(badgeId)

    // ================================ CAPITOLI DELLA STORIA ================================

    /** Restituisce l'insieme degli ID dei capitoli della storia sbloccati. */
    fun getUnlockedChapters(): Flow<Set<String>> = prefs.getUnlockedChapters()

    /**
     * Controlla se sono disponibili nuovi capitoli da sbloccare in base ai progressi dell'utente.
     *
     * Questa funzione deve essere chiamata dopo ogni operazione che modifica le statistiche
     * (XP, livello, streak, missioni completate) per garantire che i capitoli vengano
     * sbloccati tempestivamente.
     */
    suspend fun checkAndUnlockChapters() {
        val prefsData = prefs.getAllPreferences().first()
        val xp = prefsData[UserPreferencesManager.USER_XP] ?: 0
        val level = prefsData[UserPreferencesManager.USER_LEVEL] ?: 1
        val streak = prefsData[UserPreferencesManager.DAILY_STREAK] ?: 0
        val completedQuests = prefsData[UserPreferencesManager.COMPLETED_QUESTS]?.size ?: 0
        val unlocked = prefsData[UserPreferencesManager.UNLOCKED_CHAPTERS] ?: emptySet()
        val avatar = prefsData[UserPreferencesManager.SELECTED_AVATAR] ?: "🧙"

        val newChapters = StoryManager.checkUnlockedChapters(
            xp = xp,
            level = level,
            streak = streak,
            completedQuests = completedQuests,
            unlockedChapters = unlocked,
            avatar = avatar
        )

        newChapters.forEach { chapter ->
            prefs.unlockChapter(chapter.id)
        }
    }

    // ================================ TIMER DELLE MISSIONI ================================

    /** Restituisce il tempo residuo del timer di attesa (secondi). */
    fun getTimerSeconds(): Flow<Int> = prefs.getTimerSeconds()

    /** Salva il tempo residuo del timer di attesa. */
    suspend fun saveTimerSeconds(seconds: Int) = prefs.saveTimerSeconds(seconds)

    /** Restituisce il tempo residuo del timer di una missione in corso (secondi). */
    suspend fun getMissionTimerRemaining(): Flow<Int> = prefs.getMissionTimerRemaining()

    /** Avvia il timer per una missione. */
    suspend fun startMissionTimer(durationSeconds: Int) = prefs.startMissionTimer(durationSeconds)

    /** Segna la missione come completata e ferma il timer. */
    suspend fun completeMission() = prefs.completeMission()

    /** Resetta lo stato del timer della missione ai valori di default. */
    suspend fun resetMissionState() = prefs.resetMissionState()

    // ================================ MISSIONI SETTIMANALI E MENSILI ================================

    /** Restituisce l'insieme degli ID delle missioni settimanali completate. */
    fun getWeeklyCompletedQuests(): Flow<Set<String>> = prefs.getWeeklyCompletedQuests()

    /**
     * Completa una missione settimanale.
     * Incrementa il totale storico e sincronizza i dati su Firestore.
     */
    suspend fun completeWeeklyQuest(questId: String) {
        prefs.completeWeeklyQuest(questId)
        prefs.incrementTotalCompletedQuests()
        syncUserData()
    }

    /** Resetta le missioni settimanali. */
    suspend fun resetWeeklyQuests() = prefs.resetWeeklyQuests()

    /** Controlla se è iniziata una nuova settimana e, in caso affermativo, resetta le missioni. */
    suspend fun checkAndResetWeeklyQuests() = prefs.checkAndResetWeeklyQuests()

    /** Restituisce l'insieme degli ID delle missioni mensili completate. */
    fun getMonthlyCompletedQuests(): Flow<Set<String>> = prefs.getMonthlyCompletedQuests()

    /**
     * Completa una missione mensile.
     * Incrementa il totale storico e sincronizza i dati su Firestore.
     */
    suspend fun completeMonthlyQuest(questId: String) {
        prefs.completeMonthlyQuest(questId)
        prefs.incrementTotalCompletedQuests()
        syncUserData()
    }

    /** Resetta le missioni mensili. */
    suspend fun resetMonthlyQuests() = prefs.resetMonthlyQuests()

    /** Controlla se è iniziato un nuovo mese e, in caso affermativo, resetta le missioni. */
    suspend fun checkAndResetMonthlyQuests() = prefs.checkAndResetMonthlyQuests()

    /**
     * Genera le missioni settimanali personalizzate.
     * Se non vengono forniti parametri, legge automaticamente i valori dal DataStore.
     *
     * @param level         Livello dell'utente (se null, letto da DataStore).
     * @param activityLevel Livello di attività (se null, letto da DataStore).
     * @param fitnessGoal   Obiettivo fitness (se null, letto da DataStore).
     * @return Lista di [Quest] settimanali personalizzate.
     */
    suspend fun getWeeklyQuests(
        level: Int? = null,
        activityLevel: String? = null,
        fitnessGoal: String? = null
    ): List<Quest> {
        val actualLevel = level ?: getLevel().first()
        val actualActivity = activityLevel ?: getActivityLevel().first()
        val actualGoal = fitnessGoal ?: getFitnessGoal().first()
        return generateWeeklyQuests(actualLevel, actualActivity, actualGoal)
    }

    /**
     * Genera le missioni mensili personalizzate.
     * Se non vengono forniti parametri, legge automaticamente i valori dal DataStore.
     *
     * @param level         Livello dell'utente (se null, letto da DataStore).
     * @param activityLevel Livello di attività (se null, letto da DataStore).
     * @param fitnessGoal   Obiettivo fitness (se null, letto da DataStore).
     * @return Lista di [Quest] mensili personalizzate.
     */
    suspend fun getMonthlyQuests(
        level: Int? = null,
        activityLevel: String? = null,
        fitnessGoal: String? = null
    ): List<Quest> {
        val actualLevel = level ?: getLevel().first()
        val actualActivity = activityLevel ?: getActivityLevel().first()
        val actualGoal = fitnessGoal ?: getFitnessGoal().first()
        return generateMonthlyQuests(actualLevel, actualActivity, actualGoal)
    }

    // ================================ PERSISTENZA MISSIONI (CACHE) ================================

    // ---- MISSIONI GIORNALIERE ----

    /** Salva le missioni giornaliere in DataStore con la data corrente. */
    suspend fun saveDailyQuests(quests: List<Quest>) = prefs.saveDailyQuests(quests)

    /** Carica le missioni giornaliere da DataStore. */
    suspend fun loadDailyQuests(): List<Quest>? = prefs.loadDailyQuests()

    /** Invalida la cache delle missioni giornaliere. */
    suspend fun invalidateDailyQuestsCache() = prefs.invalidateDailyQuestsCache()

    // ---- MISSIONI SETTIMANALI ----

    /** Salva le missioni settimanali in DataStore con la data corrente. */
    suspend fun saveWeeklyQuests(quests: List<Quest>) = prefs.saveWeeklyQuests(quests)

    /** Carica le missioni settimanali da DataStore. */
    suspend fun loadWeeklyQuests(): List<Quest>? = prefs.loadWeeklyQuests()

    /** Invalida la cache delle missioni settimanali. */
    suspend fun invalidateWeeklyQuestsCache() = prefs.invalidateWeeklyQuestsCache()

    // ---- MISSIONI MENSILI ----

    /** Salva le missioni mensili in DataStore con la data corrente. */
    suspend fun saveMonthlyQuests(quests: List<Quest>) = prefs.saveMonthlyQuests(quests)

    /** Carica le missioni mensili da DataStore. */
    suspend fun loadMonthlyQuests(): List<Quest>? = prefs.loadMonthlyQuests()

    /** Invalida la cache delle missioni mensili. */
    suspend fun invalidateMonthlyQuestsCache() = prefs.invalidateMonthlyQuestsCache()

    /** Invalida tutte le cache delle missioni. */
    suspend fun invalidateAllQuestCaches() = prefs.invalidateAllQuestCaches()

    // ================================ RESET E DEBUG ================================

    /**
     * Resetta le missioni giornaliere e aggiorna la data dell'ultimo reset.
     * Invalida anche la cache JSON.
     */
    suspend fun resetDailyQuests() {
        prefs.resetDailyQuests()
        prefs.invalidateDailyQuestsCache()
    }

    // ================================ GESTIONE AMICI ================================

    /** Restituisce l'insieme degli ID degli amici. */
    fun getFriends(): Flow<Set<String>> = prefs.getFriends()

    // ================================ PROGRESSO MISSIONI ================================

    /** Restituisce il progresso di una missione specifica. */
    fun getQuestProgress(questId: String): Flow<Int> = prefs.getQuestProgress(questId)

    /** Imposta il progresso di una missione specifica. */
    suspend fun setQuestProgress(questId: String, progress: Int) = prefs.setQuestProgress(questId, progress)

    /** Resetta i progressi di più missioni. */
    suspend fun resetQuestProgresses(questIds: Set<String>) = prefs.resetQuestProgresses(questIds)

    // ================================ TEMPORIZZAZIONE MISSIONI ================================

    /**
     * Restituisce l'ultimo timestamp (millisecondi) in cui l'utente ha registrato
     * un progresso per la missione specificata.
     */
    fun getLastRegisterTime(questId: String): Flow<Long> = prefs.getLastRegisterTime(questId)

    /**
     * Salva il timestamp corrente come ultima registrazione per la missione.
     */
    suspend fun setLastRegisterTime(questId: String, time: Long) = prefs.setLastRegisterTime(questId, time)

    /**
     * Resetta il timestamp di ultima registrazione per una missione.
     */
    suspend fun resetLastRegisterTime(questId: String) = prefs.resetLastRegisterTime(questId)

    // ================================ INVENTARIO ================================

    // ---- SCUDO STREAK ----

    /** Restituisce il numero di scudi streak posseduti. */
    fun getStreakSaves(): Flow<Int> = prefs.getStreakSaves()

    /** Aggiunge uno o più scudi streak all'inventario. */
    suspend fun addStreakSave(amount: Int = 1) = prefs.addStreakSave(amount)

    /**
     * Usa uno scudo streak (lo consuma).
     * @return `true` se lo scudo è stato usato, `false` se non ce ne sono.
     */
    suspend fun useStreakSave(): Boolean = prefs.useStreakSave()

    // ---- BOOST XP ----

    /** Restituisce la lista dei boost XP posseduti (non attivi). */
    fun getXpBoosts(): Flow<List<XpBoostData>> = prefs.getXpBoosts()

    /** Aggiunge un boost XP all'inventario. */
    suspend fun addXpBoost(multiplier: Float, days: Int) = prefs.addXpBoost(multiplier, days)

    /**
     * Usa un boost XP (lo consuma e lo attiva).
     * @param index Indice del boost nella lista.
     * @return Il boost usato, o `null` se l'indice non è valido.
     */
    suspend fun useXpBoost(index: Int): XpBoostData? = prefs.useXpBoost(index)

    // ---- FRAMMENTI DI STORIA ----

    /** Restituisce l'insieme degli ID dei frammenti di storia posseduti. */
    fun getStoryFragments(): Flow<Set<String>> = prefs.getStoryFragments()

    /** Aggiunge un frammento di storia all'inventario. */
    suspend fun addStoryFragment(chapterId: String) = prefs.addStoryFragment(chapterId)

    /**
     * Usa un frammento di storia (sblocca il capitolo e lo rimuove dall'inventario).
     * @return `true` se il frammento è stato usato, `false` se non esiste.
     */
    suspend fun useStoryFragment(chapterId: String): Boolean = prefs.useStoryFragment(chapterId)

    // ---- XP BOOST ATTIVO ----

    /** Restituisce se un boost XP è attivo. */
    fun getXpBoostActive(): Flow<Boolean> = prefs.getXpBoostActive()

    /** Restituisce il moltiplicatore del boost XP attivo. */
    fun getXpBoostMultiplier(): Flow<Float> = prefs.getXpBoostMultiplier()

    /** Restituisce i giorni rimanenti del boost XP attivo. */
    fun getXpBoostDaysRemaining(): Flow<Int> = prefs.getXpBoostDaysRemaining()

    /**
     * Attiva un boost XP.
     * Se un boost è già attivo con lo stesso moltiplicatore, accumula i giorni.
     * Se il moltiplicatore è diverso, sovrascrive.
     */
    suspend fun activateXpBoost(multiplier: Float, days: Int) = prefs.activateXpBoost(multiplier, days)

    /** Decrementa i giorni rimanenti del boost XP. Se scende a 0, disattiva il boost. */
    suspend fun decrementXpBoost() = prefs.decrementXpBoost()

    /** Controlla se il boost XP è scaduto (da chiamare all'avvio dell'app). */
    suspend fun checkXpBoostExpiry() = prefs.checkXpBoostExpiry()

    // ================================ PROFILO UTENTE SPECIFICO ================================

    /**
     * Recupera il profilo di un utente specifico da Firestore.
     *
     * @param userId ID dell'utente da recuperare.
     * @return [LeaderboardEntry] con i dati dell'utente, oppure null se non trovato.
     */
    suspend fun getUserProfile(userId: String): LeaderboardEntry? {
        return try {
            val doc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            if (!doc.exists()) return null
            val data = doc.data ?: return null
            LeaderboardEntry(
                userId = doc.id,
                avatar = data["avatar"] as? String ?: "🧙",
                username = data["username"] as? String ?: "Anonimo",
                xp = (data["xp"] as? Number)?.toInt() ?: 0,
                level = (data["level"] as? Number)?.toInt() ?: 1,
                streak = (data["streak"] as? Number)?.toInt() ?: 0,
                completedQuests = (data["completedQuests"] as? Number)?.toInt() ?: 0,
                isCurrentUser = false
            )
        } catch (e: Exception) {
            Log.e("CaresRepository", "Error fetching user profile: ${e.message}")
            null
        }
    }

    // ================================ SINCRONIZZAZIONE CON FIRESTORE ================================

    /**
     * Sincronizza i dati dell'utente su Firestore.
     *
     * **Dati sincronizzati:**
     * - Profilo: avatar, username, xp, level, activityLevel, fitnessGoal, dailyGoal, streak
     * - Social: friends, unlockedBadges, unlockedChapters
     * - Inventario: streakSaves, xpBoosts, storyFragments
     * - XP Boost attivo: xpBoostActive, xpBoostMultiplier, xpBoostDaysRemaining
     * - Streak in pericolo: streakAtRisk, streakRiskSavedStreak, streakRiskLastCompleted
     */
    suspend fun syncUserData() {
        val userId = FirebaseAuthManager.getCurrentUserId() ?: return
        if (userId.startsWith("local_user_")) return

        try {
            val prefsData = prefs.getAllPreferences().first()
            val friendsSet = prefsData[UserPreferencesManager.FRIENDS_LIST] ?: emptySet()
            val unlockedBadges = prefsData[UserPreferencesManager.UNLOCKED_BADGES] ?: emptySet()
            val unlockedChapters = prefsData[UserPreferencesManager.UNLOCKED_CHAPTERS] ?: emptySet()
            val storyFragments = prefsData[UserPreferencesManager.INVENTORY_STORY_FRAGMENTS] ?: emptySet()
            val streakSaves = prefsData[UserPreferencesManager.INVENTORY_STREAK_SAVES] ?: 0
            val xpBoostsJson = prefsData[UserPreferencesManager.INVENTORY_XP_BOOSTS] ?: "[]"
            val xpBoostActive = prefsData[UserPreferencesManager.XP_BOOST_ACTIVE] ?: false
            val xpBoostMultiplier = prefsData[UserPreferencesManager.XP_BOOST_MULTIPLIER] ?: 1.5f
            val xpBoostDaysRemaining = prefsData[UserPreferencesManager.XP_BOOST_DAYS_REMAINING] ?: 0
            val streakAtRisk = prefsData[UserPreferencesManager.STREAK_AT_RISK] ?: false
            val streakRiskSavedStreak = prefsData[UserPreferencesManager.STREAK_RISK_SAVED_STREAK] ?: 0
            val streakRiskLastCompleted = prefsData[UserPreferencesManager.STREAK_RISK_LAST_COMPLETED] ?: ""

            val userData = mapOf(
                "avatar" to (prefsData[UserPreferencesManager.SELECTED_AVATAR] ?: "🧙"),
                "username" to (prefsData[UserPreferencesManager.USERNAME] ?: "Eroe"),
                "xp" to (prefsData[UserPreferencesManager.USER_XP] ?: 0),
                "level" to (prefsData[UserPreferencesManager.USER_LEVEL] ?: 1),
                "activityLevel" to (prefsData[UserPreferencesManager.ACTIVITY_LEVEL] ?: "Base"),
                "fitnessGoal" to (prefsData[UserPreferencesManager.FITNESS_GOAL] ?: "Mantenimento"),
                "dailyGoal" to (prefsData[UserPreferencesManager.DAILY_GOAL] ?: "Nessun obiettivo"),
                "streak" to (prefsData[UserPreferencesManager.DAILY_STREAK] ?: 0),
                "completedQuests" to (prefsData[UserPreferencesManager.TOTAL_COMPLETED_QUESTS] ?: 0),
                "friends" to friendsSet.toList(),
                "unlockedBadges" to unlockedBadges.toList(),
                "unlockedChapters" to unlockedChapters.toList(),
                "streakSaves" to streakSaves,
                "xpBoosts" to xpBoostsJson,
                "storyFragments" to storyFragments.toList(),
                "xpBoostActive" to xpBoostActive,
                "xpBoostMultiplier" to xpBoostMultiplier,
                "xpBoostDaysRemaining" to xpBoostDaysRemaining,
                "streakAtRisk" to streakAtRisk,
                "streakRiskSavedStreak" to streakRiskSavedStreak,
                "streakRiskLastCompleted" to streakRiskLastCompleted,
                "lastUpdated" to System.currentTimeMillis()
            )

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("CaresRepository", "Error syncing user data: ${e.message}")
            e.printStackTrace()
        }
    }

    // ================================ CLASSIFICA GLOBALE ================================

    /**
     * Recupera la classifica globale da Firestore, ordinata per XP decrescente.
     *
     * @return Lista di [LeaderboardEntry] ordinata per XP decrescente (massimo 50 utenti).
     */
    suspend fun getGlobalLeaderboard(): List<LeaderboardEntry> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .orderBy("xp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val currentUserId = FirebaseAuthManager.getCurrentUserId()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                LeaderboardEntry(
                    userId = doc.id,
                    avatar = data["avatar"] as? String ?: "🧙",
                    username = data["username"] as? String ?: "Anonimo",
                    xp = (data["xp"] as? Number)?.toInt() ?: 0,
                    level = (data["level"] as? Number)?.toInt() ?: 1,
                    streak = (data["streak"] as? Number)?.toInt() ?: 0,
                    completedQuests = (data["completedQuests"] as? Number)?.toInt() ?: 0,
                    isCurrentUser = doc.id == currentUserId
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getLocalLeaderboardData()
        }
    }

    // ================================ GESTIONE AMICI ================================

    /**
     * Cerca utenti per username (parziale).
     * @param query La stringa di ricerca.
     * @return Lista di utenti trovati.
     */
    suspend fun searchUsers(query: String): List<LeaderboardEntry> {
        if (query.length < 2) return emptyList()

        val userId = FirebaseAuthManager.getCurrentUserId() ?: return emptyList()
        if (userId.startsWith("local_user_")) return emptyList()

        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .limit(20)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val docId = doc.id
                if (docId == userId) return@mapNotNull null

                val friends = prefs.getFriends().first()
                if (friends.contains(docId)) return@mapNotNull null

                LeaderboardEntry(
                    userId = docId,
                    avatar = data["avatar"] as? String ?: "🧙",
                    username = data["username"] as? String ?: "Anonimo",
                    xp = (data["xp"] as? Number)?.toInt() ?: 0,
                    level = (data["level"] as? Number)?.toInt() ?: 1,
                    streak = (data["streak"] as? Number)?.toInt() ?: 0,
                    completedQuests = (data["completedQuests"] as? Number)?.toInt() ?: 0,
                    isCurrentUser = false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Invia una richiesta di amicizia a un utente.
     * @param toUserId ID dell'utente destinatario.
     * @return `true` se la richiesta è stata inviata con successo.
     */
    suspend fun sendFriendRequest(toUserId: String): Boolean {
        val fromUserId = FirebaseAuthManager.getCurrentUserId() ?: return false
        if (fromUserId.startsWith("local_user_")) return false

        return try {
            // Verifica se esiste già una richiesta pendente in entrambe le direzioni
            val existing = firestore.collection("friend_requests")
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("status", FriendRequest.RequestStatus.PENDING.name)
                .get()
                .await()

            if (!existing.isEmpty) return false

            val reverse = firestore.collection("friend_requests")
                .whereEqualTo("fromUserId", toUserId)
                .whereEqualTo("toUserId", fromUserId)
                .whereEqualTo("status", FriendRequest.RequestStatus.PENDING.name)
                .get()
                .await()

            if (!reverse.isEmpty) return false

            // Recupera i dati degli utenti per la richiesta
            val fromUserDoc = firestore.collection(USERS_COLLECTION).document(fromUserId).get().await()
            val fromData = fromUserDoc.data ?: return false

            val toUserDoc = firestore.collection(USERS_COLLECTION).document(toUserId).get().await()
            val toData = toUserDoc.data ?: return false

            val request = FriendRequest(
                fromUserId = fromUserId,
                toUserId = toUserId,
                status = FriendRequest.RequestStatus.PENDING,
                fromUsername = fromData["username"] as? String ?: "Eroe",
                fromAvatar = fromData["avatar"] as? String ?: "🧙",
                toUsername = toData["username"] as? String ?: "Eroe",
                toAvatar = toData["avatar"] as? String ?: "🧙"
            )

            firestore.collection("friend_requests")
                .add(request.toMap())
                .await()
            true
        } catch (e: Exception) {
            Log.e("CaresRepository", "Error sending friend request: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Carica i dati di un utente da Firestore e li salva in DataStore.
     * @param userId ID dell'utente da caricare.
     */
    suspend fun loadUserDataFromFirestore(userId: String) {
        try {
            val doc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            if (doc.exists()) {
                val data = doc.data ?: return

                val avatar = data["avatar"] as? String ?: "🧙"
                val level = data["activityLevel"] as? String ?: "Base"
                val goal = data["fitnessGoal"] as? String ?: "Mantenimento"
                val username = data["username"] as? String ?: "Eroe"
                val dailyGoal = data["dailyGoal"] as? String ?: "Nessun obiettivo"

                // Salva preferenze base
                prefs.saveUserPreferences(avatar, level, goal, username, dailyGoal)

                // Amici
                val friendsList = (data["friends"] as? List<*>)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
                prefs.saveFriends(friendsList)

                // XP e livello
                val xp = (data["xp"] as? Number)?.toInt() ?: 0
                prefs.addXp(xp)

                // Badge
                val unlockedBadges = (data["unlockedBadges"] as? List<*>)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
                unlockedBadges.forEach { badgeId ->
                    prefs.unlockBadge(badgeId)
                }

                // Capitoli
                val unlockedChapters = (data["unlockedChapters"] as? List<*>)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
                unlockedChapters.forEach { chapterId ->
                    prefs.unlockChapter(chapterId)
                }

                // Inventario - Scudo Streak
                val streakSaves = (data["streakSaves"] as? Number)?.toInt() ?: 0
                if (streakSaves > 0) {
                    prefs.addStreakSave(streakSaves)
                }

                // Inventario - Boost XP
                val xpBoostsJson = data["xpBoosts"] as? String ?: "[]"
                if (xpBoostsJson != "[]") {
                    val boosts = xpBoostsJson.split(",").mapNotNull { XpBoostData.fromJson(it) }
                    boosts.forEach { boost ->
                        prefs.addXpBoost(boost.multiplier, boost.days)
                    }
                }

                // Inventario - Frammenti di Storia
                val storyFragments = (data["storyFragments"] as? List<*>)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
                storyFragments.forEach { chapterId ->
                    prefs.addStoryFragment(chapterId)
                }

                // XP Boost attivo
                val xpBoostActive = data["xpBoostActive"] as? Boolean ?: false
                val xpBoostMultiplier = (data["xpBoostMultiplier"] as? Number)?.toFloat() ?: 1.5f
                val xpBoostDaysRemaining = (data["xpBoostDaysRemaining"] as? Number)?.toInt() ?: 0
                if (xpBoostActive && xpBoostDaysRemaining > 0) {
                    prefs.activateXpBoost(xpBoostMultiplier, xpBoostDaysRemaining)
                }

                // Streak in pericolo
                val streakAtRisk = data["streakAtRisk"] as? Boolean ?: false
                val streakRiskSavedStreak = (data["streakRiskSavedStreak"] as? Number)?.toInt() ?: 0
                val streakRiskLastCompleted = data["streakRiskLastCompleted"] as? String ?: ""
                if (streakAtRisk) {
                    prefs.setStreakAtRisk(true)
                    prefs.setStreakRiskSavedStreak(streakRiskSavedStreak)
                    prefs.setStreakRiskLastCompleted(streakRiskLastCompleted)
                }
            }
        } catch (e: Exception) {
            Log.e("CaresRepository", "Error loading user data: ${e.message}")
        }
    }

    /**
     * Esegue il logout pulendo sia Firebase che i dati locali.
     */
    suspend fun logout() {
        FirebaseAuthManager.signOut()
        prefs.clearAll()
    }

    /**
     * Recupera le richieste di amicizia ricevute (in attesa).
     */
    suspend fun getPendingFriendRequests(): List<FriendRequest> {
        val userId = FirebaseAuthManager.getCurrentUserId() ?: return emptyList()
        if (userId.startsWith("local_user_")) return emptyList()

        return try {
            Log.d("CaresRepository", "Fetching pending requests for user: $userId")
            val snapshot = firestore.collection("friend_requests")
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", FriendRequest.RequestStatus.PENDING.name)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                FriendRequest.fromMap(doc.id, data)
            }
        } catch (e: Exception) {
            Log.e("CaresRepository", "Error getting pending requests: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Recupera le richieste di amicizia inviate (in attesa).
     */
    suspend fun getSentFriendRequests(): List<FriendRequest> {
        val userId = FirebaseAuthManager.getCurrentUserId() ?: return emptyList()
        if (userId.startsWith("local_user_")) return emptyList()

        return try {
            val snapshot = firestore.collection("friend_requests")
                .whereEqualTo("fromUserId", userId)
                .whereEqualTo("status", FriendRequest.RequestStatus.PENDING.name)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                FriendRequest.fromMap(doc.id, data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Accetta una richiesta di amicizia.
     * @param requestId ID della richiesta da accettare.
     * @return `true` se la richiesta è stata accettata con successo.
     */
    suspend fun acceptFriendRequest(requestId: String): Boolean {
        val userId = FirebaseAuthManager.getCurrentUserId() ?: return false
        if (userId.startsWith("local_user_")) return false

        return try {
            val doc = firestore.collection("friend_requests").document(requestId).get().await()
            val data = doc.data ?: return false
            val request = FriendRequest.fromMap(requestId, data)

            // Aggiorna lo stato della richiesta
            firestore.collection("friend_requests")
                .document(requestId)
                .update("status", FriendRequest.RequestStatus.ACCEPTED.name)
                .await()

            // Aggiungi l'amicizia in entrambe le direzioni
            val receiverRef = firestore.collection(USERS_COLLECTION).document(userId)
            receiverRef.update("friends", com.google.firebase.firestore.FieldValue.arrayUnion(request.fromUserId)).await()

            val senderRef = firestore.collection(USERS_COLLECTION).document(request.fromUserId)
            senderRef.update("friends", com.google.firebase.firestore.FieldValue.arrayUnion(request.toUserId)).await()

            // Aggiorna la lista amici locale
            prefs.addFriend(request.fromUserId)

            syncUserData()
            true
        } catch (e: Exception) {
            Log.e("CaresRepository", "Error accepting friend request: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Rifiuta una richiesta di amicizia.
     * @param requestId ID della richiesta da rifiutare.
     * @return `true` se la richiesta è stata rifiutata con successo.
     */
    suspend fun rejectFriendRequest(requestId: String): Boolean {
        val userId = FirebaseAuthManager.getCurrentUserId() ?: return false
        if (userId.startsWith("local_user_")) return false

        return try {
            firestore.collection("friend_requests")
                .document(requestId)
                .update("status", FriendRequest.RequestStatus.REJECTED.name)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Rimuove un amico dalla lista.
     * @param friendId ID dell'amico da rimuovere.
     * @return `true` se l'amico è stato rimosso con successo.
     */
    suspend fun removeFriend(friendId: String): Boolean {
        val userId = FirebaseAuthManager.getCurrentUserId() ?: return false
        if (userId.startsWith("local_user_")) return false

        return try {
            prefs.removeFriend(friendId)
            syncUserData()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ================================ CLASSIFICA AMICI ================================

    /**
     * Recupera la classifica degli amici da Firestore.
     *
     * @param friendIds Lista degli ID degli amici.
     * @return Lista di [LeaderboardEntry] ordinata per XP decrescente.
     */
    suspend fun getFriendsLeaderboard(friendIds: List<String>): List<LeaderboardEntry> {
        if (friendIds.isEmpty()) {
            val current = getCurrentUserEntry()
            return if (current != null) {
                if (current.userId.startsWith("local_user_")) getLocalFriendsData()
                else listOf(current)
            } else {
                getLocalFriendsData()
            }
        }

        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereIn(FieldPath.documentId(), friendIds)
                .orderBy("xp", Query.Direction.DESCENDING)
                .get()
                .await()

            val currentUserId = FirebaseAuthManager.getCurrentUserId()
            val entries = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                LeaderboardEntry(
                    userId = doc.id,
                    avatar = data["avatar"] as? String ?: "🧙",
                    username = data["username"] as? String ?: "Anonimo",
                    xp = (data["xp"] as? Number)?.toInt() ?: 0,
                    level = (data["level"] as? Number)?.toInt() ?: 1,
                    streak = (data["streak"] as? Number)?.toInt() ?: 0,
                    completedQuests = (data["completedQuests"] as? Number)?.toInt() ?: 0,
                    isCurrentUser = doc.id == currentUserId
                )
            }.toMutableList()

            if (entries.none { it.isCurrentUser }) {
                getCurrentUserEntry()?.let { entries.add(it) }
            }
            entries.sortedByDescending { it.xp }
        } catch (e: Exception) {
            e.printStackTrace()
            getLocalFriendsData()
        }
    }

    // ================================ RICOMPENSA GIORNALIERA ================================

    /** Restituisce lo streak di ritiro settimanale (0-7). */
    fun getClaimStreak(): Flow<Int> = prefs.getClaimStreak()

    /** Verifica se l'utente può reclamare oggi. */
    suspend fun canClaimReward(): Boolean = prefs.canClaimReward()

    /** Reclama la ricompensa del giorno corrente. */
    suspend fun claimReward(): Int = prefs.claimReward()

    /** Restituisce il giorno corrente da reclamare (1-7). */
    suspend fun getCurrentClaimDay(): Int = prefs.getCurrentClaimDay()

    // ================================ DATI DI FALLBACK LOCALI ================================

    /**
     * Genera una classifica locale di fallback con l'utente corrente.
     * Utilizzato quando Firebase non è disponibile o l'utente è in modalità locale.
     *
     * @return Lista di [LeaderboardEntry] contenente l'utente corrente.
     */
    private suspend fun getLocalLeaderboardData(): List<LeaderboardEntry> {
        val prefsData = prefs.getAllPreferences().first()
        val userId = FirebaseAuthManager.getCurrentUserId() ?: "local_user"

        val currentUser = LeaderboardEntry(
            userId = userId,
            avatar = prefsData[UserPreferencesManager.SELECTED_AVATAR] ?: "🧙",
            username = prefsData[UserPreferencesManager.USERNAME] ?: "Tu",
            xp = prefsData[UserPreferencesManager.USER_XP] ?: 0,
            level = prefsData[UserPreferencesManager.USER_LEVEL] ?: 1,
            streak = prefsData[UserPreferencesManager.DAILY_STREAK] ?: 0,
            completedQuests = prefsData[UserPreferencesManager.TOTAL_COMPLETED_QUESTS] ?: 0,
            isCurrentUser = true
        )

        return listOf(currentUser).sortedByDescending { it.xp }
    }

    /**
     * Genera una lista locale di amici fittizi contenente solo l'utente corrente.
     */
    private suspend fun getLocalFriendsData(): List<LeaderboardEntry> {
        val prefsData = prefs.getAllPreferences().first()
        val userId = FirebaseAuthManager.getCurrentUserId() ?: "local_user"

        val currentUser = LeaderboardEntry(
            userId = userId,
            avatar = prefsData[UserPreferencesManager.SELECTED_AVATAR] ?: "🧙",
            username = prefsData[UserPreferencesManager.USERNAME] ?: "Tu",
            xp = prefsData[UserPreferencesManager.USER_XP] ?: 0,
            level = prefsData[UserPreferencesManager.USER_LEVEL] ?: 1,
            streak = prefsData[UserPreferencesManager.DAILY_STREAK] ?: 0,
            completedQuests = prefsData[UserPreferencesManager.TOTAL_COMPLETED_QUESTS] ?: 0,
            isCurrentUser = true
        )
        return listOf(currentUser).sortedByDescending { it.xp }
    }

    /**
     * Recupera l'entry dell'utente corrente da Firestore.
     * Utilizzato per assicurarsi che l'utente sia presente nella classifica amici.
     *
     * @return L'entry dell'utente corrente, o `null` se non trovata o in caso di errore.
     */
    private suspend fun getCurrentUserEntry(): LeaderboardEntry? {
        val userId = FirebaseAuthManager.getCurrentUserId() ?: return null
        return try {
            val doc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            val data = doc.data ?: return null
            LeaderboardEntry(
                userId = doc.id,
                avatar = data["avatar"] as? String ?: "🧙",
                username = data["username"] as? String ?: "Tu",
                xp = (data["xp"] as? Number)?.toInt() ?: 0,
                level = (data["level"] as? Number)?.toInt() ?: 1,
                streak = (data["streak"] as? Number)?.toInt() ?: 0,
                completedQuests = (data["completedQuests"] as? Number)?.toInt() ?: 0,
                isCurrentUser = true
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Recupera il profilo pubblico completo di un utente, inclusi badge, capitoli e amici.
     * @param userId ID dell'utente da recuperare.
     * @return [PublicUserProfile] con tutti i dati, oppure null se non trovato.
     */
    suspend fun getPublicUserProfile(userId: String): PublicUserProfile? {
        return try {
            val doc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            if (!doc.exists()) return null
            val data = doc.data ?: return null

            val avatar = data["avatar"] as? String ?: "🧙"
            val username = data["username"] as? String ?: "Anonimo"
            val xp = (data["xp"] as? Number)?.toInt() ?: 0
            val level = (data["level"] as? Number)?.toInt() ?: 1
            val streak = (data["streak"] as? Number)?.toInt() ?: 0
            val completedQuests = (data["completedQuests"] as? Number)?.toInt() ?: 0

            // Badge e capitoli
            val unlockedBadges = (data["unlockedBadges"] as? List<*>)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
            val unlockedChapters = (data["unlockedChapters"] as? List<*>)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()

            // Amici
            val friendIds = (data["friends"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val friendsInfo = if (friendIds.isNotEmpty()) {
                val friendDocs = firestore.collection(USERS_COLLECTION)
                    .whereIn(FieldPath.documentId(), friendIds.take(10))
                    .get()
                    .await()
                friendDocs.documents.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    FriendInfo(
                        userId = doc.id,
                        username = d["username"] as? String ?: "Anonimo",
                        avatar = d["avatar"] as? String ?: "🧙"
                    )
                }
            } else {
                emptyList()
            }

            PublicUserProfile(
                userId = userId,
                avatar = avatar,
                username = username,
                xp = xp,
                level = level,
                streak = streak,
                completedQuests = completedQuests,
                unlockedBadges = unlockedBadges,
                unlockedChapters = unlockedChapters,
                friends = friendsInfo
            )
        } catch (e: Exception) {
            Log.e("CaresRepository", "Error fetching public profile: ${e.message}")
            null
        }
    }
}