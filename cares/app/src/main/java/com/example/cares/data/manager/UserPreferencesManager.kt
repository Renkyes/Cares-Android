// file: data/manager/UserPreferencesManager.kt
package com.example.cares.data.manager

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale

/**
 * Estensione per il DataStore delle preferenze utente.
 */
private val Context.dataStore by preferencesDataStore(name = "user_preferences")

/**
 * Gestisce la persistenza dei dati utente e delle impostazioni utilizzando DataStore.
 *
 * **Principali aree di gestione:**
 * - **Profilo utente**: avatar, livello attività, obiettivi, username
 * - **Gamification**: XP, livello, streak, badge, missioni (giornaliere/settimanali/mensili)
 * - **Timer missioni**: stato, durata, scadenza
 * - **Inventario**: Scudo Streak, Boost XP, Frammenti di Storia
 * - **Diario delle emozioni**: umore giornaliero e storico
 * - **Contatore passi**: base e ultimo totale
 * - **Amici e capitoli della storia**: liste di ID
 * - **Tema**: colore primario, modalità scura/chiara
 * - **Sistema Streak in Pericolo**: stile Duolingo
 * - **Cache missioni**: persistenza delle missioni generate
 *
 * **Thread-safety:** Tutte le operazioni sono sospendibili e thread-safe grazie a DataStore.
 *
 * @param context Il contesto per l'accesso al DataStore.
 */
class UserPreferencesManager(private val context: Context) {

    // ================================ DATASTORE ================================

    private val dataStore = context.dataStore
    private val gson = Gson()

    // ================================ CHIAVI PREFERENCE ================================

    companion object {
        // --- Profilo utente ---
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SELECTED_AVATAR = stringPreferencesKey("selected_avatar")
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
        val FITNESS_GOAL = stringPreferencesKey("fitness_goal")
        val DAILY_GOAL = stringPreferencesKey("daily_goal")
        val USERNAME = stringPreferencesKey("username")

        // --- Gamification ---
        val USER_XP = intPreferencesKey("user_xp")
        val USER_LEVEL = intPreferencesKey("user_level")
        val COMPLETED_QUESTS = stringSetPreferencesKey("completed_quests")
        val UNLOCKED_BADGES = stringSetPreferencesKey("unlocked_badges")
        val DAILY_STREAK = intPreferencesKey("daily_streak")
        val LAST_COMPLETED_DATE = stringPreferencesKey("last_completed_date")
        val TOTAL_COMPLETED_QUESTS = intPreferencesKey("total_completed_quests")

        // --- Timer missione ---
        val QUEST_TIMER_SECONDS = intPreferencesKey("quest_timer_seconds")
        val QUEST_TIMER_EXPIRY = longPreferencesKey("quest_timer_expiry")
        val MISSION_TIMER_START = longPreferencesKey("mission_timer_start")
        val MISSION_DURATION = intPreferencesKey("mission_duration")
        val MISSION_IS_RUNNING = booleanPreferencesKey("mission_is_running")

        // --- Amici e capitoli ---
        val FRIENDS_LIST = stringSetPreferencesKey("friends_list")
        val UNLOCKED_CHAPTERS = stringSetPreferencesKey("unlocked_chapters")

        // --- Tema ---
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")

        // --- Reset date (legacy / debug) ---
        val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")

        // --- Missioni settimanali ---
        val WEEKLY_QUESTS_COMPLETED = stringSetPreferencesKey("weekly_quests_completed")
        val LAST_WEEK_RESET = stringPreferencesKey("last_week_reset")

        // --- Missioni mensili ---
        val MONTHLY_QUESTS_COMPLETED = stringSetPreferencesKey("monthly_quests_completed")
        val LAST_MONTH_RESET = stringPreferencesKey("last_month_reset")

        // --- Diario delle emozioni ---
        val MOOD_HISTORY = stringSetPreferencesKey("mood_history")

        // --- Ricompensa settimanale (7 days) ---
        val LAST_CLAIM_DATE = stringPreferencesKey("last_claim_date")
        val CLAIM_STREAK = intPreferencesKey("claim_streak")

        // --- Contatore passi ---
        val STEP_COUNTER_BASE = longPreferencesKey("step_counter_base")
        val STEP_COUNTER_LAST_TOTAL = longPreferencesKey("step_counter_last_total")
        val STEP_COUNTER_LAST_RESET_DATE = stringPreferencesKey("step_counter_last_reset_date")

        // --- Inventario: Scudo Streak ---
        val INVENTORY_STREAK_SAVES = intPreferencesKey("inventory_streak_saves")

        // --- Inventario: Boost XP ---
        val INVENTORY_XP_BOOSTS = stringPreferencesKey("inventory_xp_boosts") // JSON

        // --- Inventario: Frammenti di Storia ---
        val INVENTORY_STORY_FRAGMENTS = stringSetPreferencesKey("inventory_story_fragments")

        // --- Boost XP attivo ---
        val XP_BOOST_ACTIVE = booleanPreferencesKey("xp_boost_active")
        val XP_BOOST_MULTIPLIER = floatPreferencesKey("xp_boost_multiplier")
        val XP_BOOST_DAYS_REMAINING = intPreferencesKey("xp_boost_days_remaining")
        val XP_BOOST_ACTIVATED_AT = longPreferencesKey("xp_boost_activated_at")

        // --- Cache missioni ---
        val DAILY_QUESTS_JSON = stringPreferencesKey("daily_quests_json")
        val DAILY_QUESTS_DATE = stringPreferencesKey("daily_quests_date")
        val WEEKLY_QUESTS_JSON = stringPreferencesKey("weekly_quests_json")
        val WEEKLY_QUESTS_DATE = stringPreferencesKey("weekly_quests_date")
        val MONTHLY_QUESTS_JSON = stringPreferencesKey("monthly_quests_json")
        val MONTHLY_QUESTS_DATE = stringPreferencesKey("monthly_quests_date")

        // --- Streak in pericolo ---
        val STREAK_AT_RISK = booleanPreferencesKey("streak_at_risk")
        val STREAK_RISK_SAVED_STREAK = intPreferencesKey("streak_risk_saved_streak")
        val STREAK_RISK_LAST_COMPLETED = stringPreferencesKey("streak_risk_last_completed")
    }

    // ================================ UTILITY DATE ================================

    private fun getTodayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun getYesterdayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time)

    private fun getPreviousDate(date: String): String {
        if (date.isEmpty()) return getTodayDate()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val cal = Calendar.getInstance().apply {
                time = format.parse(date) ?: Date()
                add(Calendar.DAY_OF_YEAR, -1)
            }
            format.format(cal.time)
        } catch (_: Exception) {
            getTodayDate()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getCurrentWeekKey(): String {
        val calendar = Calendar.getInstance()
        return String.format("%d-W%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.WEEK_OF_YEAR))
    }

    private fun getCurrentMonthKey(): String =
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    // ================================ ACCESSO BASE ================================

    /**
     * Restituisce un flusso di tutte le preferenze, utile per l'osservazione aggregata.
     */
    fun getAllPreferences(): Flow<Preferences> = dataStore.data

    // ================================ ONBOARDING E PROFILO ================================

    /**
     * Restituisce lo stato di completamento dell'onboarding.
     */
    fun getOnboardingStatus(): Flow<Boolean> =
        dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    /**
     * Restituisce il livello di attività dell'utente come Flow.
     */
    fun getActivityLevel(): Flow<String> =
        dataStore.data.map { it[ACTIVITY_LEVEL] ?: "Moderato" }

    /**
     * Restituisce l'obiettivo fitness dell'utente come Flow.
     */
    fun getFitnessGoal(): Flow<String> =
        dataStore.data.map { it[FITNESS_GOAL] ?: "Mantenimento" }

    /**
     * Restituisce l'obiettivo giornaliero dell'utente come Flow.
     */
    fun getDailyGoal(): Flow<String> =
        dataStore.data.map { it[DAILY_GOAL] ?: "Nessun obiettivo" }

    /**
     * Imposta l'obiettivo giornaliero dell'utente.
     */
    suspend fun setDailyGoal(goal: String) {
        dataStore.edit { prefs ->
            prefs[DAILY_GOAL] = goal
        }
    }

    // ================================ SALVATAGGIO PREFERENZE ================================

    /**
     * Salva le preferenze iniziali dell'utente durante l'onboarding.
     *
     * Inizializza anche i valori di default per XP, livello, streak, missioni e badge.
     * Imposta il timer della missione al valore di default (300 secondi).
     *
     * @param avatar L'emoji dell'avatar scelto.
     * @param level  Il livello di attività selezionato ("Sedentario", "Moderato", "Attivo").
     * @param goal   L'obiettivo fitness ("Perdere peso", "Tonificare", "Resistenza").
     * @param username Il nome utente scelto.
     * @param dailyGoal L'obiettivo giornaliero (opzionale, default "").
     */
    suspend fun saveUserPreferences(
        avatar: String,
        level: String,
        goal: String,
        username: String,
        dailyGoal: String = ""
    ) {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = true
            prefs[SELECTED_AVATAR] = avatar
            prefs[ACTIVITY_LEVEL] = level
            prefs[FITNESS_GOAL] = goal
            prefs[DAILY_GOAL] = dailyGoal
            prefs[USER_XP] = 0
            prefs[USER_LEVEL] = 1
            prefs[COMPLETED_QUESTS] = emptySet()
            prefs[UNLOCKED_BADGES] = emptySet()
            prefs[DAILY_STREAK] = 0
            prefs[LAST_COMPLETED_DATE] = ""
            prefs[USERNAME] = username
            resetTimerInternal(prefs)
        }
    }

    /**
     * Salva le preferenze da una mappa (usato per importazione dati).
     */
    suspend fun saveUserPreferencesFromMap(data: Map<String, Any>) {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = true
            prefs[SELECTED_AVATAR] = data["avatar"] as? String ?: "🧙"
            prefs[ACTIVITY_LEVEL] = data["activityLevel"] as? String ?: "Base"
            prefs[FITNESS_GOAL] = data["fitnessGoal"] as? String ?: "Mantenimento"
            prefs[DAILY_GOAL] = data["dailyGoal"] as? String ?: ""
            prefs[USER_XP] = (data["xp"] as? Number)?.toInt() ?: 0
            prefs[USER_LEVEL] = (data["level"] as? Number)?.toInt() ?: 1
            prefs[DAILY_STREAK] = (data["streak"] as? Number)?.toInt() ?: 0
            prefs[USERNAME] = data["username"] as? String ?: "Eroe"
            prefs[LAST_COMPLETED_DATE] = data["lastCompletedDate"] as? String ?: ""
        }
    }

    /**
     * Svuota tutti i dati dell'utente corrente (chiamato al logout).
     */
    suspend fun clearAll() {
        dataStore.edit { prefs ->
            val theme = prefs[IS_DARK_THEME]
            prefs.clear()
            if (theme != null) prefs[IS_DARK_THEME] = theme
        }
    }

    // ================================ TEMA PERSONALIZZATO ================================

    /**
     * Restituisce la preferenza per il tema scuro/chiaro.
     * Default: `true` (tema scuro).
     */
    fun getDarkThemePreference(): Flow<Boolean?> =
        dataStore.data.map { it[IS_DARK_THEME] }

    /**
     * Imposta la preferenza per il tema scuro/chiaro.
     */
    suspend fun setDarkThemePreference(isDark: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_DARK_THEME] = isDark
        }
    }

    // ================================ XP E LIVELLO ================================

    /**
     * Restituisce l'esperienza totale dell'utente.
     */
    fun getXp(): Flow<Int> =
        dataStore.data.map { it[USER_XP] ?: 0 }

    /**
     * Restituisce il livello corrente dell'utente (calcolato automaticamente).
     */
    fun getLevel(): Flow<Int> =
        dataStore.data.map { it[USER_LEVEL] ?: 1 }

    /**
     * Aggiunge XP all'utente, applicando il moltiplicatore se un boost XP è attivo,
     * e aggiorna il livello in base alla formula: livello = (XP / 100) + 1.
     *
     * @param amount La quantità di XP da aggiungere.
     * @return L'XP effettivamente aggiunto (dopo l'applicazione del moltiplicatore).
     */
    suspend fun addXp(amount: Int): Int {
        var finalAmount = 0
        dataStore.edit { prefs ->
            val boostActive = prefs[XP_BOOST_ACTIVE] ?: false
            val multiplier = if (boostActive) {
                prefs[XP_BOOST_MULTIPLIER] ?: 1.5f
            } else 1.0f

            finalAmount = (amount * multiplier).toInt()
            val currentXp = prefs[USER_XP] ?: 0
            prefs[USER_XP] = currentXp + finalAmount
            prefs[USER_LEVEL] = ((currentXp + finalAmount) / 100) + 1
        }
        return finalAmount
    }

    // ================================ MISSIONI GIORNALIERE ================================

    /**
     * Restituisce l'insieme degli ID delle missioni completate oggi.
     */
    fun getCompletedQuests(): Flow<Set<String>> =
        dataStore.data.map { it[COMPLETED_QUESTS] ?: emptySet() }

    /**
     * Segna una missione come completata e incrementa il totale storico.
     *
     * @param questId L'ID della missione completata.
     */
    suspend fun completeQuest(questId: String) {
        dataStore.edit { prefs ->
            val current = prefs[COMPLETED_QUESTS] ?: emptySet()
            prefs[COMPLETED_QUESTS] = current + questId
        }
        incrementTotalCompletedQuests()
    }

    /**
     * Resetta l'insieme delle missioni completate oggi (senza azzerare il totale storico).
     * Invalida anche la cache JSON delle missioni giornaliere.
     */
    suspend fun resetQuests() {
        dataStore.edit { prefs ->
            prefs[COMPLETED_QUESTS] = emptySet()
        }
    }

    /**
     * Resetta le missioni giornaliere e aggiorna la data di reset a oggi.
     * Invalida anche la cache JSON.
     */
    suspend fun resetDailyQuests() {
        dataStore.edit { preferences ->
            preferences[COMPLETED_QUESTS] = emptySet()
            preferences[LAST_RESET_DATE] = getTodayDate()
            preferences.remove(DAILY_QUESTS_JSON)
            preferences.remove(DAILY_QUESTS_DATE)
        }
    }

    /**
     * Incrementa il contatore totale delle missioni completate.
     */
    suspend fun incrementTotalCompletedQuests() {
        dataStore.edit { prefs ->
            val current = prefs[TOTAL_COMPLETED_QUESTS] ?: 0
            prefs[TOTAL_COMPLETED_QUESTS] = current + 1
        }
    }

    // ================================ MISSIONI SETTIMANALI ================================

    /**
     * Restituisce l'insieme degli ID delle missioni settimanali completate.
     */
    fun getWeeklyCompletedQuests(): Flow<Set<String>> =
        dataStore.data.map { it[WEEKLY_QUESTS_COMPLETED] ?: emptySet() }

    /**
     * Segna una missione settimanale come completata.
     */
    suspend fun completeWeeklyQuest(questId: String) {
        dataStore.edit { prefs ->
            val current = prefs[WEEKLY_QUESTS_COMPLETED] ?: emptySet()
            prefs[WEEKLY_QUESTS_COMPLETED] = current + questId
        }
    }

    /**
     * Resetta le missioni settimanali e aggiorna la data dell'ultimo reset.
     * Invalida anche la cache JSON delle missioni settimanali.
     */
    suspend fun resetWeeklyQuests() {
        dataStore.edit { prefs ->
            prefs[WEEKLY_QUESTS_COMPLETED] = emptySet()
            prefs[LAST_WEEK_RESET] = getCurrentWeekKey()
            prefs.remove(WEEKLY_QUESTS_JSON)
            prefs.remove(WEEKLY_QUESTS_DATE)
        }
    }

    /**
     * Controlla se è iniziata una nuova settimana e, in caso affermativo, resetta le missioni.
     * Da chiamare all'avvio dell'app.
     */
    suspend fun checkAndResetWeeklyQuests() {
        val lastWeek = dataStore.data.first()[LAST_WEEK_RESET] ?: ""
        val currentWeek = getCurrentWeekKey()
        if (lastWeek != currentWeek) {
            resetWeeklyQuests()
        }
    }

    // ================================ MISSIONI MENSILI ================================

    /**
     * Restituisce l'insieme degli ID delle missioni mensili completate.
     */
    fun getMonthlyCompletedQuests(): Flow<Set<String>> =
        dataStore.data.map { it[MONTHLY_QUESTS_COMPLETED] ?: emptySet() }

    /**
     * Segna una missione mensile come completata.
     */
    suspend fun completeMonthlyQuest(questId: String) {
        dataStore.edit { prefs ->
            val current = prefs[MONTHLY_QUESTS_COMPLETED] ?: emptySet()
            prefs[MONTHLY_QUESTS_COMPLETED] = current + questId
        }
    }

    /**
     * Resetta le missioni mensili e aggiorna la data dell'ultimo reset.
     * Invalida anche la cache JSON delle missioni mensili.
     */
    suspend fun resetMonthlyQuests() {
        dataStore.edit { prefs ->
            prefs[MONTHLY_QUESTS_COMPLETED] = emptySet()
            prefs[LAST_MONTH_RESET] = getCurrentMonthKey()
            prefs.remove(MONTHLY_QUESTS_JSON)
            prefs.remove(MONTHLY_QUESTS_DATE)
        }
    }

    /**
     * Controlla se è iniziato un nuovo mese e, in caso affermativo, resetta le missioni.
     * Da chiamare all'avvio dell'app.
     */
    suspend fun checkAndResetMonthlyQuests() {
        val lastMonth = dataStore.data.first()[LAST_MONTH_RESET] ?: ""
        val currentMonth = getCurrentMonthKey()
        if (lastMonth != currentMonth) {
            resetMonthlyQuests()
        }
    }

    // ================================ BADGE ================================

    /**
     * Restituisce l'insieme degli ID dei badge sbloccati.
     */
    fun getUnlockedBadges(): Flow<Set<String>> =
        dataStore.data.map { it[UNLOCKED_BADGES] ?: emptySet() }

    /**
     * Sblocca un badge per l'utente.
     */
    suspend fun unlockBadge(badgeId: String) {
        dataStore.edit { prefs ->
            val current = prefs[UNLOCKED_BADGES] ?: emptySet()
            prefs[UNLOCKED_BADGES] = current + badgeId
        }
    }

    /**
     * Resetta tutti i badge (debug/development).
     */
    suspend fun resetBadges() {
        dataStore.edit { prefs ->
            prefs[UNLOCKED_BADGES] = emptySet()
        }
    }

    // ================================ STREAK ================================

    /**
     * Restituisce lo streak corrente (giorni consecutivi di attività).
     */
    fun getStreak(): Flow<Int> =
        dataStore.data.map { it[DAILY_STREAK] ?: 0 }

    /**
     * Aggiorna lo streak in base alla data di oggi.
     *
     * La logica è la seguente:
     * - Se non c'è una data di completamento precedente, imposta streak = 1.
     * - Se l'ultimo completamento è oggi, lascia lo streak invariato.
     * - Se l'ultimo completamento è ieri, incrementa lo streak di 1.
     * - Se l'ultimo completamento è più vecchio di ieri, resetta lo streak a 1.
     */
    suspend fun updateStreak(): Int {
        dataStore.edit { prefs ->
            val today = getTodayDate()
            val lastCompleted = prefs[LAST_COMPLETED_DATE] ?: ""
            val currentStreak = prefs[DAILY_STREAK] ?: 0

            // Se l'utente era in pericolo e completa una missione, usa il valore salvato
            val atRisk = prefs[STREAK_AT_RISK] ?: false
            val baseStreak = if (atRisk) {
                prefs[STREAK_RISK_SAVED_STREAK] ?: currentStreak
            } else {
                currentStreak
            }

            val newStreak = when {
                lastCompleted.isEmpty() -> 1
                lastCompleted == today -> baseStreak
                lastCompleted == getYesterdayDate() -> baseStreak + 1
                else -> 1
            }

            prefs[DAILY_STREAK] = newStreak
            prefs[LAST_COMPLETED_DATE] = today

            // Rimuovi lo stato di rischio se l'utente ha ripreso l'attività
            if (atRisk) {
                prefs[STREAK_AT_RISK] = false
                prefs.remove(STREAK_RISK_SAVED_STREAK)
                prefs.remove(STREAK_RISK_LAST_COMPLETED)
            }
        }
        return getStreak().first()
    }

    /**
     * Resetta lo streak e le date di completamento (debug).
     */
    suspend fun debugResetStreakAndDates() {
        dataStore.edit { prefs ->
            prefs[DAILY_STREAK] = 0
            prefs[LAST_COMPLETED_DATE] = ""
        }
    }

    /**
     * Resetta lo streak (alias di [debugResetStreakAndDates]).
     */
    suspend fun resetStreak() = debugResetStreakAndDates()

    // ================================ STREAK IN PERICOLO ================================

    /**
     * Controlla se lo streak è scaduto (nessuna attività per più di un giorno).
     * Se l'utente ha saltato un giorno, lo mette in stato di "pericolo" invece di resettarlo subito.
     * Questo permette all'utente di usare uno scudo per salvare la streak.
     * Deve essere chiamato all'avvio della Home.
     */
    suspend fun checkStreakExpiry() {
        dataStore.edit { prefs ->
            val today = getTodayDate()
            val lastCompleted = prefs[LAST_COMPLETED_DATE] ?: ""
            val yesterday = getYesterdayDate()

            val alreadyAtRisk = prefs[STREAK_AT_RISK] ?: false
            if (alreadyAtRisk) return@edit

            if (lastCompleted.isNotEmpty() && lastCompleted != today && lastCompleted != yesterday) {
                // L'utente ha saltato un giorno! Metti la streak in pericolo
                val currentStreak = prefs[DAILY_STREAK] ?: 0
                prefs[STREAK_AT_RISK] = true
                prefs[STREAK_RISK_SAVED_STREAK] = currentStreak
                prefs[STREAK_RISK_LAST_COMPLETED] = lastCompleted
                // NON resettare la streak ancora!
            }
        }
    }

    /**
     * Salva la streak usando uno scudo.
     * Consuma uno scudo dall'inventario e rimuove lo stato di rischio.
     *
     * @return `true` se lo scudo è stato usato con successo, `false` altrimenti.
     */
    suspend fun saveStreakWithShield(): Boolean {
        var shieldUsed = false
        dataStore.edit { prefs ->
            val atRisk = prefs[STREAK_AT_RISK] ?: false
            if (!atRisk) return@edit

            val currentSaves = prefs[INVENTORY_STREAK_SAVES] ?: 0
            if (currentSaves > 0) {
                prefs[INVENTORY_STREAK_SAVES] = currentSaves - 1
                prefs[LAST_COMPLETED_DATE] = getYesterdayDate()
                prefs[STREAK_AT_RISK] = false
                prefs.remove(STREAK_RISK_SAVED_STREAK)
                prefs.remove(STREAK_RISK_LAST_COMPLETED)
                shieldUsed = true
            }
        }
        return shieldUsed
    }

    /**
     * Perde la streak (reset a 0).
     * Rimuove lo stato di rischio e azzera la streak.
     */
    suspend fun loseStreak() {
        dataStore.edit { prefs ->
            val atRisk = prefs[STREAK_AT_RISK] ?: false
            if (!atRisk) return@edit

            prefs[DAILY_STREAK] = 0
            prefs[LAST_COMPLETED_DATE] = ""
            prefs[STREAK_AT_RISK] = false
            prefs.remove(STREAK_RISK_SAVED_STREAK)
            prefs.remove(STREAK_RISK_LAST_COMPLETED)
        }
    }

    /**
     * Restituisce se la streak è attualmente in pericolo.
     */
    fun getStreakAtRisk(): Flow<Boolean> =
        dataStore.data.map { it[STREAK_AT_RISK] ?: false }

    /**
     * Imposta lo stato di pericolo della streak.
     */
    suspend fun setStreakAtRisk(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[STREAK_AT_RISK] = value
        }
    }

    /**
     * Restituisce il valore della streak salvato quando è entrata in pericolo.
     */
    fun getStreakRiskSavedStreak(): Flow<Int> =
        dataStore.data.map { it[STREAK_RISK_SAVED_STREAK] ?: 0 }

    /**
     * Imposta il valore della streak salvato quando è entrata in pericolo.
     */
    suspend fun setStreakRiskSavedStreak(value: Int) {
        dataStore.edit { prefs ->
            prefs[STREAK_RISK_SAVED_STREAK] = value
        }
    }

    /**
     * Restituisce l'ultima data di completamento prima che la streak entrasse in pericolo.
     */
    fun getStreakRiskLastCompleted(): Flow<String> =
        dataStore.data.map { it[STREAK_RISK_LAST_COMPLETED] ?: "" }

    /**
     * Imposta l'ultima data di completamento prima che la streak entrasse in pericolo.
     */
    suspend fun setStreakRiskLastCompleted(date: String) {
        dataStore.edit { prefs ->
            prefs[STREAK_RISK_LAST_COMPLETED] = date
        }
    }

    // ================================ TIMER MISSIONE ================================

    /**
     * Avvia il timer per una missione (ad esempio durante un allenamento).
     *
     * @param durationSeconds La durata della missione in secondi.
     */
    suspend fun startMissionTimer(durationSeconds: Int) {
        dataStore.edit { prefs ->
            prefs[MISSION_TIMER_START] = System.currentTimeMillis()
            prefs[MISSION_DURATION] = durationSeconds
            prefs[MISSION_IS_RUNNING] = true
        }
    }

    /**
     * Segna la missione come completata e ferma il timer.
     */
    suspend fun completeMission() {
        dataStore.edit { prefs ->
            prefs[MISSION_IS_RUNNING] = false
            prefs[MISSION_TIMER_START] = 0L
            prefs[MISSION_DURATION] = 0
        }
    }

    /**
     * Resetta lo stato del timer della missione ai valori di default.
     */
    suspend fun resetMissionState() {
        dataStore.edit { prefs ->
            prefs[MISSION_IS_RUNNING] = false
            prefs[MISSION_TIMER_START] = 0L
            prefs[MISSION_DURATION] = 0
            prefs[QUEST_TIMER_SECONDS] = 1800
            prefs[QUEST_TIMER_EXPIRY] = System.currentTimeMillis() + (1800 * 1000L)
        }
    }

    /**
     * Salva il tempo residuo del timer di attesa (per la missione corrente).
     *
     * @param seconds Il numero di secondi rimanenti prima della scadenza.
     */
    suspend fun saveTimerSeconds(seconds: Int) {
        dataStore.edit { prefs ->
            prefs[QUEST_TIMER_SECONDS] = seconds
            prefs[QUEST_TIMER_EXPIRY] = System.currentTimeMillis() + (seconds * 1000L)
        }
    }

    /**
     * Restituisce il tempo residuo del timer di attesa (secondi).
     * Il valore viene calcolato in base alla scadenza salvata.
     */
    fun getTimerSeconds(): Flow<Int> =
        dataStore.data.map {
            val expiry = it[QUEST_TIMER_EXPIRY] ?: 0L
            val remaining = ((expiry - System.currentTimeMillis()) / 1000).toInt()
            if (remaining > 0) remaining else 0
        }

    /**
     * Restituisce il tempo residuo del timer di una missione in corso (secondi).
     */
    fun getMissionTimerRemaining(): Flow<Int> =
        dataStore.data.map {
            val start = it[MISSION_TIMER_START] ?: 0L
            val duration = it[MISSION_DURATION] ?: 0
            if (start == 0L || duration == 0) return@map 0
            val elapsed = ((System.currentTimeMillis() - start) / 1000).toInt()
            val remaining = duration - elapsed
            if (remaining > 0) remaining else 0
        }

    /**
     * Resetta il timer di attesa al valore di default (300 secondi).
     */
    suspend fun resetTimer() {
        dataStore.edit { resetTimerInternal(it) }
    }

    private fun resetTimerInternal(prefs: MutablePreferences) {
        val defaultSeconds = 300
        prefs[QUEST_TIMER_SECONDS] = defaultSeconds
        prefs[QUEST_TIMER_EXPIRY] = System.currentTimeMillis() + (defaultSeconds * 1000L)
    }

    // ================================ PROGRESSO MISSIONI ================================

    private fun getQuestProgressKey(questId: String) = intPreferencesKey("quest_progress_$questId")

    /**
     * Restituisce il progresso di una missione specifica.
     */
    fun getQuestProgress(questId: String): Flow<Int> =
        dataStore.data.map { it[getQuestProgressKey(questId)] ?: 0 }

    /**
     * Imposta il progresso di una missione specifica.
     */
    suspend fun setQuestProgress(questId: String, progress: Int) {
        dataStore.edit { it[getQuestProgressKey(questId)] = progress }
    }

    /**
     * Resetta i progressi di più missioni.
     */
    suspend fun resetQuestProgresses(questIds: Set<String>) {
        dataStore.edit { prefs ->
            questIds.forEach { id ->
                prefs.remove(getQuestProgressKey(id))
            }
        }
    }

    // ================================ TEMPORIZZAZIONE MISSIONI ================================

    private fun getLastRegisterTimeKey(questId: String) = longPreferencesKey("quest_last_register_time_$questId")

    /**
     * Restituisce l'ultimo timestamp (millisecondi) in cui l'utente ha registrato
     * un progresso per la missione specificata.
     *
     * @param questId ID della missione.
     * @return Flow con il timestamp (0 se mai registrato).
     */
    fun getLastRegisterTime(questId: String): Flow<Long> =
        dataStore.data.map { it[getLastRegisterTimeKey(questId)] ?: 0L }

    /**
     * Salva il timestamp corrente come ultima registrazione per la missione.
     *
     * @param questId ID della missione.
     * @param time Timestamp in millisecondi (System.currentTimeMillis()).
     */
    suspend fun setLastRegisterTime(questId: String, time: Long) {
        dataStore.edit { prefs ->
            prefs[getLastRegisterTimeKey(questId)] = time
        }
    }

    /**
     * Resetta il timestamp di ultima registrazione per una missione.
     */
    suspend fun resetLastRegisterTime(questId: String) {
        dataStore.edit { prefs ->
            prefs.remove(getLastRegisterTimeKey(questId))
        }
    }

    // ================================ DIARIO DELLE EMOZIONI ================================

    /**
     * Salva l'umore di oggi (valore tra 1 e 5).
     *
     * Se esiste già un umore registrato per la data odierna, viene sovrascritto.
     * La data è nel formato "yyyy-MM-dd".
     *
     * @param mood Il valore dell'umore (1 = molto triste, 5 = molto felice).
     */
    suspend fun saveMood(mood: Int) {
        dataStore.edit { prefs ->
            val history = prefs[MOOD_HISTORY]?.toMutableSet() ?: mutableSetOf()
            val today = getTodayDate()
            history.removeAll { it.startsWith("$today:") }
            history.add("$today:$mood")
            prefs[MOOD_HISTORY] = history
        }
    }

    /**
     * Recupera l'umore di una data specifica (formato "yyyy-MM-dd").
     *
     * @return Il valore dell'umore (1-5) se presente, altrimenti `null`.
     */
    suspend fun getMoodForDate(date: String): Int? {
        val history = dataStore.data.first()[MOOD_HISTORY] ?: emptySet()
        return history.find { it.startsWith("$date:") }
            ?.substringAfter(":")
            ?.toIntOrNull()
    }

    /**
     * Recupera l'umore di oggi.
     *
     * @return Il valore dell'umore (1-5) se presente, altrimenti `null`.
     */
    suspend fun getTodayMood(): Int? = getMoodForDate(getTodayDate())

    /**
     * Recupera la cronologia degli umori per gli ultimi N giorni.
     *
     * @param days Il numero di giorni da considerare (default 7).
     * @return Una mappa che associa la data (yyyy-MM-dd) al valore dell'umore (1-5).
     */
    suspend fun getMoodHistory(days: Int = 7): Map<String, Int> {
        val history = dataStore.data.first()[MOOD_HISTORY] ?: emptySet()
        val result = mutableMapOf<String, Int>()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        repeat(days) {
            val date = format.format(calendar.time)
            history.find { it.startsWith("$date:") }?.let { entry ->
                val mood = entry.substringAfter(":").toIntOrNull()
                if (mood != null) {
                    result[date] = mood
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return result
    }

    /**
     * Calcola la media degli umori degli ultimi N giorni.
     *
     * @param days Il numero di giorni da considerare (default 7).
     * @return La media come valore float (0.0 se non ci sono dati).
     */
    suspend fun getAverageMood(days: Int = 7): Float {
        val history = getMoodHistory(days)
        return if (history.isEmpty()) 0f else history.values.average().toFloat()
    }

    // ================================ RICOMPENSA SETTIMANALE (7 DAYS) ================================

    /**
     * Restituisce lo streak di ritiro settimanale (0-7).
     * 0 = nessun giorno reclamato, 1-7 = giorni consecutivi reclamati.
     */
    fun getClaimStreak(): Flow<Int> =
        dataStore.data.map { it[CLAIM_STREAK] ?: 0 }

    /**
     * Verifica se l'utente può reclamare oggi.
     * @return `true` se non ha ancora reclamato oggi.
     */
    suspend fun canClaimReward(): Boolean {
        val lastClaim = dataStore.data.first()[LAST_CLAIM_DATE] ?: ""
        val today = getTodayDate()
        return lastClaim != today
    }

    /**
     * Reclama la ricompensa del giorno corrente.
     * Gestisce un ciclo settimanale di 7 giorni:
     * - Se non si è mai reclamato o si è saltato un giorno → reset a 1.
     * - Se si è reclamato ieri → incrementa di 1 (massimo 7).
     * - Dopo il giorno 7, il giorno successivo riparte da 1.
     *
     * XP per giorno:
     *   Day 1: 10 XP
     *   Day 2: 15 XP
     *   Day 3: 20 XP
     *   Day 4: 25 XP
     *   Day 5: 30 XP
     *   Day 6: 35 XP
     *   Day 7: 50 XP (bonus)
     *
     * @return L'XP guadagnato per il giorno corrente.
     */
    suspend fun claimReward(): Int {
        val today = getTodayDate()
        val lastClaim = dataStore.data.first()[LAST_CLAIM_DATE] ?: ""
        val currentStreak = dataStore.data.first()[CLAIM_STREAK] ?: 0

        val newStreak = when {
            lastClaim.isEmpty() -> 1
            lastClaim == today -> currentStreak
            lastClaim == getYesterdayDate() -> if (currentStreak == 7) 1 else currentStreak + 1
            else -> 1
        }

        val xpReward = when (newStreak) {
            1 -> 10
            2 -> 15
            3 -> 20
            4 -> 25
            5 -> 30
            6 -> 35
            7 -> 50
            else -> 10
        }

        dataStore.edit { prefs ->
            prefs[LAST_CLAIM_DATE] = today
            prefs[CLAIM_STREAK] = newStreak
        }

        return xpReward
    }

    /**
     * Restituisce il giorno corrente da reclamare (1-7).
     */
    suspend fun getCurrentClaimDay(): Int {
        val streak = getClaimStreak().first()
        val canClaim = canClaimReward()
        return if (canClaim) {
            if (streak == 7) 1 else streak + 1
        } else {
            if (streak == 0) 1 else streak
        }
    }

    // ================================ CONTATORE PASSI ================================

    /**
     * Restituisce il valore base del contatore passi salvato.
     */
    fun getStepCounterBase(): Flow<Long> =
        dataStore.data.map { it[STEP_COUNTER_BASE] ?: 0L }

    /**
     * Salva il valore base del contatore passi.
     */
    suspend fun setStepCounterBase(value: Long) {
        dataStore.edit { prefs ->
            prefs[STEP_COUNTER_BASE] = value
        }
    }

    /**
     * Restituisce l'ultimo valore totale del contatore passi salvato.
     */
    fun getStepCounterLastTotal(): Flow<Long> =
        dataStore.data.map { it[STEP_COUNTER_LAST_TOTAL] ?: 0L }

    /**
     * Salva l'ultimo valore totale del contatore passi.
     */
    suspend fun setStepCounterLastTotal(value: Long) {
        dataStore.edit { prefs ->
            prefs[STEP_COUNTER_LAST_TOTAL] = value
        }
    }

    /**
     * Resetta i dati del contatore passi (base e ultimo totale).
     */
    suspend fun resetStepCounter() {
        dataStore.edit { prefs ->
            prefs.remove(STEP_COUNTER_BASE)
            prefs.remove(STEP_COUNTER_LAST_TOTAL)
        }
    }

    /**
     * Restituisce la data dell'ultimo reset del contatore passi.
     */
    fun getStepCounterLastResetDate(): Flow<String> =
        dataStore.data.map { it[STEP_COUNTER_LAST_RESET_DATE] ?: "" }

    /**
     * Imposta la data dell'ultimo reset del contatore passi.
     */
    suspend fun setStepCounterLastResetDate(date: String) {
        dataStore.edit { prefs ->
            prefs[STEP_COUNTER_LAST_RESET_DATE] = date
        }
    }

    // ================================ AMICI ================================

    /**
     * Restituisce l'insieme degli ID degli amici.
     */
    fun getFriends(): Flow<Set<String>> =
        dataStore.data.map { it[FRIENDS_LIST] ?: emptySet() }

    /**
     * Sostituisce l'intera lista degli amici.
     */
    suspend fun saveFriends(friends: Set<String>) {
        dataStore.edit { prefs ->
            prefs[FRIENDS_LIST] = friends
        }
    }

    /**
     * Aggiunge un amico alla lista.
     */
    suspend fun addFriend(userId: String) {
        dataStore.edit { prefs ->
            val current = prefs[FRIENDS_LIST] ?: emptySet()
            prefs[FRIENDS_LIST] = current + userId
        }
    }

    /**
     * Rimuove un amico dalla lista.
     */
    suspend fun removeFriend(userId: String) {
        dataStore.edit { prefs ->
            val current = prefs[FRIENDS_LIST] ?: emptySet()
            prefs[FRIENDS_LIST] = current - userId
        }
    }

    // ================================ CAPITOLI STORIA ================================

    /**
     * Restituisce l'insieme degli ID dei capitoli della storia sbloccati.
     */
    fun getUnlockedChapters(): Flow<Set<String>> =
        dataStore.data.map { it[UNLOCKED_CHAPTERS] ?: emptySet() }

    /**
     * Sblocca un nuovo capitolo della storia.
     */
    suspend fun unlockChapter(chapterId: String) {
        dataStore.edit { prefs ->
            val current = prefs[UNLOCKED_CHAPTERS] ?: emptySet()
            prefs[UNLOCKED_CHAPTERS] = current + chapterId
        }
    }

    /**
     * Resetta tutti i capitoli sbloccati (debug/development).
     */
    suspend fun resetChapters() {
        dataStore.edit { prefs ->
            prefs[UNLOCKED_CHAPTERS] = emptySet()
        }
    }

    // ================================ INVENTARIO - SCUDO STREAK ================================

    /**
     * Restituisce il numero di scudi streak posseduti.
     */
    fun getStreakSaves(): Flow<Int> =
        dataStore.data.map { it[INVENTORY_STREAK_SAVES] ?: 0 }

    /**
     * Aggiunge uno o più scudi streak all'inventario.
     */
    suspend fun addStreakSave(amount: Int = 1) {
        dataStore.edit { prefs ->
            val current = prefs[INVENTORY_STREAK_SAVES] ?: 0
            prefs[INVENTORY_STREAK_SAVES] = current + amount
        }
    }

    /**
     * Usa uno scudo streak (lo consuma).
     * @return `true` se lo scudo è stato usato, `false` se non ce ne sono.
     */
    suspend fun useStreakSave(): Boolean {
        var used = false
        dataStore.edit { prefs ->
            val current = prefs[INVENTORY_STREAK_SAVES] ?: 0
            if (current > 0) {
                prefs[INVENTORY_STREAK_SAVES] = current - 1
                used = true
            }
        }
        return used
    }

    // ================================ INVENTARIO - BOOST XP ================================

    /**
     * Struttura dati per un boost XP salvato.
     */
    data class XpBoostData(
        val multiplier: Float,
        val days: Int,
        val activatedAt: Long = System.currentTimeMillis()
    ) {
        /**
         * Verifica se il boost è ancora attivo.
         */
        fun isActive(): Boolean {
            val elapsedDays = (System.currentTimeMillis() - activatedAt) / (24 * 60 * 60 * 1000)
            return elapsedDays < days
        }

        /**
         * Calcola i giorni rimanenti del boost.
         */
        fun getDaysRemaining(): Int {
            val elapsedDays = (System.currentTimeMillis() - activatedAt) / (24 * 60 * 60 * 1000)
            return (days - elapsedDays).coerceAtLeast(0).toInt()
        }

        /**
         * Serializza l'oggetto in una stringa per il salvataggio.
         */
        fun toJson(): String = "$multiplier|$days|$activatedAt"

        companion object {
            /**
             * Deserializza una stringa in un oggetto XpBoostData.
             */
            fun fromJson(json: String): XpBoostData? {
                return try {
                    val parts = json.split("|")
                    if (parts.size == 3) {
                        XpBoostData(
                            multiplier = parts[0].toFloat(),
                            days = parts[1].toInt(),
                            activatedAt = parts[2].toLong()
                        )
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Restituisce la lista dei boost XP posseduti (non attivi).
     */
    fun getXpBoosts(): Flow<List<XpBoostData>> =
        dataStore.data.map { prefs ->
            val json = prefs[INVENTORY_XP_BOOSTS] ?: "[]"
            if (json == "[]") {
                emptyList()
            } else {
                json.split(",").mapNotNull { XpBoostData.fromJson(it) }
            }
        }

    /**
     * Aggiunge un boost XP all'inventario.
     */
    suspend fun addXpBoost(multiplier: Float, days: Int) {
        val newBoost = XpBoostData(multiplier, days, System.currentTimeMillis())
        dataStore.edit { prefs ->
            val current = getXpBoostsFromPrefs(prefs)
            val updated = current + newBoost
            prefs[INVENTORY_XP_BOOSTS] = updated.joinToString(",") { it.toJson() }
        }
    }

    /**
     * Usa un boost XP (lo consuma e lo attiva).
     * @param index Indice del boost nella lista.
     * @return Il boost usato, o `null` se l'indice non è valido.
     */
    suspend fun useXpBoost(index: Int): XpBoostData? {
        var usedBoost: XpBoostData? = null
        dataStore.edit { prefs ->
            val current = getXpBoostsFromPrefs(prefs)
            if (index in current.indices) {
                usedBoost = current[index]
                val updated = current.toMutableList().apply { removeAt(index) }
                prefs[INVENTORY_XP_BOOSTS] = updated.joinToString(",") { it.toJson() }
            }
        }
        usedBoost?.let { boost ->
            activateXpBoost(boost.multiplier, boost.days)
        }
        return usedBoost
    }

    private fun getXpBoostsFromPrefs(prefs: Preferences): List<XpBoostData> {
        val json = prefs[INVENTORY_XP_BOOSTS] ?: "[]"
        return if (json == "[]") emptyList()
        else json.split(",").mapNotNull { XpBoostData.fromJson(it) }
    }

    private fun getXpBoostsFromPrefs(prefs: MutablePreferences): List<XpBoostData> {
        val json = prefs[INVENTORY_XP_BOOSTS] ?: "[]"
        return if (json == "[]") emptyList()
        else json.split(",").mapNotNull { XpBoostData.fromJson(it) }
    }

    // ================================ INVENTARIO - FRAMMENTI DI STORIA ================================

    /**
     * Restituisce l'insieme degli ID dei frammenti di storia posseduti.
     */
    fun getStoryFragments(): Flow<Set<String>> =
        dataStore.data.map { it[INVENTORY_STORY_FRAGMENTS] ?: emptySet() }

    /**
     * Aggiunge un frammento di storia all'inventario.
     */
    suspend fun addStoryFragment(chapterId: String) {
        dataStore.edit { prefs ->
            val current = prefs[INVENTORY_STORY_FRAGMENTS] ?: emptySet()
            prefs[INVENTORY_STORY_FRAGMENTS] = current + chapterId
        }
    }

    /**
     * Usa un frammento di storia (sblocca il capitolo e lo rimuove dall'inventario).
     * @return `true` se il frammento è stato usato, `false` se non esiste.
     */
    suspend fun useStoryFragment(chapterId: String): Boolean {
        var used = false
        dataStore.edit { prefs ->
            val current = prefs[INVENTORY_STORY_FRAGMENTS] ?: emptySet()
            if (current.contains(chapterId)) {
                prefs[INVENTORY_STORY_FRAGMENTS] = current - chapterId
                val unlocked = prefs[UNLOCKED_CHAPTERS] ?: emptySet()
                prefs[UNLOCKED_CHAPTERS] = unlocked + chapterId
                used = true
            }
        }
        return used
    }

    // ================================ BOOST XP ATTIVO ================================

    /**
     * Restituisce se un boost XP è attivo.
     */
    fun getXpBoostActive(): Flow<Boolean> =
        dataStore.data.map { it[XP_BOOST_ACTIVE] ?: false }

    /**
     * Restituisce il moltiplicatore del boost XP attivo.
     */
    fun getXpBoostMultiplier(): Flow<Float> =
        dataStore.data.map { it[XP_BOOST_MULTIPLIER] ?: 1.5f }

    /**
     * Restituisce i giorni rimanenti del boost XP attivo.
     */
    fun getXpBoostDaysRemaining(): Flow<Int> =
        dataStore.data.map { it[XP_BOOST_DAYS_REMAINING] ?: 0 }

    /**
     * Attiva un boost XP.
     * Se un boost è già attivo con lo stesso moltiplicatore, accumula i giorni.
     * Se il moltiplicatore è diverso, sovrascrive.
     */
    suspend fun activateXpBoost(multiplier: Float, days: Int) {
        dataStore.edit { prefs ->
            val currentActive = prefs[XP_BOOST_ACTIVE] ?: false
            val currentMultiplier = prefs[XP_BOOST_MULTIPLIER] ?: 1.5f
            val currentDays = prefs[XP_BOOST_DAYS_REMAINING] ?: 0

            if (currentActive && currentMultiplier == multiplier) {
                prefs[XP_BOOST_DAYS_REMAINING] = currentDays + days
            } else {
                prefs[XP_BOOST_ACTIVE] = true
                prefs[XP_BOOST_MULTIPLIER] = multiplier
                prefs[XP_BOOST_DAYS_REMAINING] = days
                prefs[XP_BOOST_ACTIVATED_AT] = System.currentTimeMillis()
            }
        }
    }

    /**
     * Decrementa i giorni rimanenti del boost XP (da chiamare ogni giorno).
     * Se i giorni scendono a 0, disattiva il boost.
     */
    suspend fun decrementXpBoost() {
        dataStore.edit { prefs ->
            val active = prefs[XP_BOOST_ACTIVE] ?: false
            if (active) {
                val days = prefs[XP_BOOST_DAYS_REMAINING] ?: 0
                if (days > 1) {
                    prefs[XP_BOOST_DAYS_REMAINING] = days - 1
                } else {
                    prefs[XP_BOOST_ACTIVE] = false
                    prefs[XP_BOOST_DAYS_REMAINING] = 0
                    prefs[XP_BOOST_MULTIPLIER] = 1.5f
                    prefs[XP_BOOST_ACTIVATED_AT] = 0L
                }
            }
        }
    }

    /**
     * Controlla se il boost XP è scaduto (da chiamare all'avvio dell'app).
     */
    suspend fun checkXpBoostExpiry() {
        dataStore.edit { prefs ->
            val active = prefs[XP_BOOST_ACTIVE] ?: false
            if (active) {
                val activatedAt = prefs[XP_BOOST_ACTIVATED_AT] ?: 0L
                val days = prefs[XP_BOOST_DAYS_REMAINING] ?: 0
                val elapsedDays = (System.currentTimeMillis() - activatedAt) / (24 * 60 * 60 * 1000)
                if (elapsedDays >= days) {
                    prefs[XP_BOOST_ACTIVE] = false
                    prefs[XP_BOOST_DAYS_REMAINING] = 0
                    prefs[XP_BOOST_MULTIPLIER] = 1.5f
                    prefs[XP_BOOST_ACTIVATED_AT] = 0L
                } else {
                    val remaining = (days - elapsedDays).toInt()
                    prefs[XP_BOOST_DAYS_REMAINING] = remaining.coerceAtLeast(0)
                }
            }
        }
    }

    // ================================ PERSISTENZA MISSIONI (CACHE) ================================

    // ---- Giornaliere ----

    /**
     * Salva le missioni giornaliere in DataStore con la data corrente.
     */
    suspend fun saveDailyQuests(quests: List<com.example.cares.utils.Quest>) {
        val json = gson.toJson(quests)
        dataStore.edit { prefs ->
            prefs[DAILY_QUESTS_JSON] = json
            prefs[DAILY_QUESTS_DATE] = getTodayDate()
        }
    }

    /**
     * Carica le missioni giornaliere da DataStore.
     * @return Lista di missioni se esiste una cache valida per oggi, altrimenti `null`.
     */
    suspend fun loadDailyQuests(): List<com.example.cares.utils.Quest>? {
        val prefs = dataStore.data.first()
        val json = prefs[DAILY_QUESTS_JSON]
        val date = prefs[DAILY_QUESTS_DATE]
        if (json.isNullOrEmpty() || date != getTodayDate()) return null
        val type = object : TypeToken<List<com.example.cares.utils.Quest>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Invalida la cache delle missioni giornaliere.
     */
    suspend fun invalidateDailyQuestsCache() {
        dataStore.edit { prefs ->
            prefs.remove(DAILY_QUESTS_JSON)
            prefs.remove(DAILY_QUESTS_DATE)
        }
    }

    // ---- Settimanali ----

    /**
     * Salva le missioni settimanali in DataStore con la data corrente.
     */
    suspend fun saveWeeklyQuests(quests: List<com.example.cares.utils.Quest>) {
        val json = gson.toJson(quests)
        dataStore.edit { prefs ->
            prefs[WEEKLY_QUESTS_JSON] = json
            prefs[WEEKLY_QUESTS_DATE] = getTodayDate()
        }
    }

    /**
     * Carica le missioni settimanali da DataStore.
     * @return Lista di missioni se esiste una cache valida per oggi, altrimenti `null`.
     */
    suspend fun loadWeeklyQuests(): List<com.example.cares.utils.Quest>? {
        val prefs = dataStore.data.first()
        val json = prefs[WEEKLY_QUESTS_JSON]
        val date = prefs[WEEKLY_QUESTS_DATE]
        if (json.isNullOrEmpty() || date != getTodayDate()) return null
        val type = object : TypeToken<List<com.example.cares.utils.Quest>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Invalida la cache delle missioni settimanali.
     */
    suspend fun invalidateWeeklyQuestsCache() {
        dataStore.edit { prefs ->
            prefs.remove(WEEKLY_QUESTS_JSON)
            prefs.remove(WEEKLY_QUESTS_DATE)
        }
    }

    // ---- Mensili ----

    /**
     * Salva le missioni mensili in DataStore con la data corrente.
     */
    suspend fun saveMonthlyQuests(quests: List<com.example.cares.utils.Quest>) {
        val json = gson.toJson(quests)
        dataStore.edit { prefs ->
            prefs[MONTHLY_QUESTS_JSON] = json
            prefs[MONTHLY_QUESTS_DATE] = getTodayDate()
        }
    }

    /**
     * Carica le missioni mensili da DataStore.
     * @return Lista di missioni se esiste una cache valida per oggi, altrimenti `null`.
     */
    suspend fun loadMonthlyQuests(): List<com.example.cares.utils.Quest>? {
        val prefs = dataStore.data.first()
        val json = prefs[MONTHLY_QUESTS_JSON]
        val date = prefs[MONTHLY_QUESTS_DATE]
        if (json.isNullOrEmpty() || date != getTodayDate()) return null
        val type = object : TypeToken<List<com.example.cares.utils.Quest>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Invalida la cache delle missioni mensili.
     */
    suspend fun invalidateMonthlyQuestsCache() {
        dataStore.edit { prefs ->
            prefs.remove(MONTHLY_QUESTS_JSON)
            prefs.remove(MONTHLY_QUESTS_DATE)
        }
    }

    /**
     * Invalida tutte le cache delle missioni.
     */
    suspend fun invalidateAllQuestCaches() {
        dataStore.edit { prefs ->
            prefs.remove(DAILY_QUESTS_JSON)
            prefs.remove(DAILY_QUESTS_DATE)
            prefs.remove(WEEKLY_QUESTS_JSON)
            prefs.remove(WEEKLY_QUESTS_DATE)
            prefs.remove(MONTHLY_QUESTS_JSON)
            prefs.remove(MONTHLY_QUESTS_DATE)
        }
    }

    // ================================ RESET DATI ================================

    /**
     * Resetta solo i dati di gioco (XP, streak, missioni, badge, ecc.),
     * mantenendo le impostazioni dell'app (tema, musica).
     * Resetta anche inventario, cache missioni e stato di rischio.
     */
    suspend fun clearGameData() {
        dataStore.edit { prefs ->
            prefs.remove(USER_XP)
            prefs.remove(USER_LEVEL)
            prefs.remove(COMPLETED_QUESTS)
            prefs.remove(UNLOCKED_BADGES)
            prefs.remove(DAILY_STREAK)
            prefs.remove(LAST_COMPLETED_DATE)
            prefs.remove(TOTAL_COMPLETED_QUESTS)
            prefs.remove(WEEKLY_QUESTS_COMPLETED)
            prefs.remove(MONTHLY_QUESTS_COMPLETED)
            prefs.remove(LAST_WEEK_RESET)
            prefs.remove(LAST_MONTH_RESET)
            prefs.remove(MOOD_HISTORY)
            prefs.remove(LAST_CLAIM_DATE)
            prefs.remove(CLAIM_STREAK)
            prefs.remove(UNLOCKED_CHAPTERS)
            prefs.remove(FRIENDS_LIST)
            prefs.remove(DAILY_GOAL)
            prefs.remove(STEP_COUNTER_BASE)
            prefs.remove(STEP_COUNTER_LAST_TOTAL)
            prefs.remove(INVENTORY_STREAK_SAVES)
            prefs.remove(INVENTORY_XP_BOOSTS)
            prefs.remove(INVENTORY_STORY_FRAGMENTS)
            prefs.remove(XP_BOOST_ACTIVE)
            prefs.remove(XP_BOOST_MULTIPLIER)
            prefs.remove(XP_BOOST_DAYS_REMAINING)
            prefs.remove(XP_BOOST_ACTIVATED_AT)
            prefs.remove(DAILY_QUESTS_JSON)
            prefs.remove(DAILY_QUESTS_DATE)
            prefs.remove(WEEKLY_QUESTS_JSON)
            prefs.remove(WEEKLY_QUESTS_DATE)
            prefs.remove(MONTHLY_QUESTS_JSON)
            prefs.remove(MONTHLY_QUESTS_DATE)
            prefs.remove(STREAK_AT_RISK)
            prefs.remove(STREAK_RISK_SAVED_STREAK)
            prefs.remove(STREAK_RISK_LAST_COMPLETED)
            // Non rimuovere: ONBOARDING_COMPLETED, SELECTED_AVATAR,
            // ACTIVITY_LEVEL, FITNESS_GOAL, USERNAME, IS_DARK_THEME
        }
    }

    /**
     * Restituisce la data dell'ultimo reset delle missioni giornaliere.
     */
    fun getLastResetDate(): Flow<String> = dataStore.data.map { it[LAST_RESET_DATE] ?: "" }

    /**
     * Imposta la data dell'ultimo reset delle missioni giornaliere.
     */
    suspend fun setLastResetDate(date: String) {
        dataStore.edit { preferences ->
            preferences[LAST_RESET_DATE] = date
        }
    }

    // ================================ DEBUG ================================

    /**
     * Avanza la data di un giorno per scopi di debug.
     * Utile per testare il reset giornaliero delle missioni e lo streak in pericolo.
     */
    suspend fun debugAdvanceDay() {
        dataStore.edit { prefs ->
            val baseCompletedDate = prefs[LAST_COMPLETED_DATE]?.takeIf { it.isNotEmpty() } ?: getTodayDate()
            prefs[LAST_COMPLETED_DATE] = getPreviousDate(baseCompletedDate)

            val baseResetDate = prefs[LAST_RESET_DATE]?.takeIf { it.isNotEmpty() } ?: getTodayDate()
            prefs[LAST_RESET_DATE] = getPreviousDate(baseResetDate)

            prefs[COMPLETED_QUESTS] = emptySet()
        }
        checkStreakExpiry()
    }
}