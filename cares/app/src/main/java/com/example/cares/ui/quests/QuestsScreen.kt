// file: ui/quests/QuestsScreen.kt
package com.example.cares.ui.quests

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.compose.rememberAsyncImagePainter
import com.example.cares.data.manager.UserPreferencesManager
import com.example.cares.data.repository.CaresRepository
import com.example.cares.ui.animation.AnimatedEntrance
import com.example.cares.utils.Quest
import com.example.cares.utils.VerificationType
import com.example.cares.utils.checkBadges
import com.example.cares.ui.theme.Gradients
import com.example.cares.ui.theme.Neon
import com.example.cares.ui.theme.GlassCard
import com.example.cares.utils.getTodayDate
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.io.File
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

// ================================ QUESTS VIEW MODEL ================================

/**
 * ViewModel per la gestione delle missioni (giornaliere, settimanali, mensili).
 *
 * **Responsabilità:**
 * - Caricare e gestire le missioni con cache persistente.
 * - Gestire il completamento delle missioni con aggiornamento XP e streak.
 * - Gestire le missioni a step con progress tracking e controllo temporale.
 * - Gestire le verifiche con foto (ML Kit) e testo.
 * - Gestire le ricompense speciali (scudi streak, boost XP, frammenti di storia).
 * - Gestire i timer di reset per missioni giornaliere/settimanali/mensili.
 *
 * @param repository Repository per l'accesso ai dati.
 */
class QuestsViewModel(
    private val repository: CaresRepository
) : ViewModel() {

    // ================================ PROPRIETÀ PRIVATE ================================

    private var showSnackbar: ((String) -> Unit)? = null
    private var timerJob: Job? = null
    private var dailyQuestsLoaded = false
    private var weeklyQuestsLoaded = false
    private var monthlyQuestsLoaded = false

    // ================================ STATO MISSIONI ================================

    private val _dailyQuests = MutableStateFlow<List<Quest>>(emptyList())
    private val _weeklyQuests = MutableStateFlow<List<Quest>>(emptyList())
    private val _monthlyQuests = MutableStateFlow<List<Quest>>(emptyList())

    private val _dailyCompletedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _weeklyCompletedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _monthlyCompletedIds = MutableStateFlow<Set<String>>(emptySet())

    val dailyQuests: StateFlow<List<Quest>> = _dailyQuests.asStateFlow()
    val weeklyQuests: StateFlow<List<Quest>> = _weeklyQuests.asStateFlow()
    val monthlyQuests: StateFlow<List<Quest>> = _monthlyQuests.asStateFlow()
    val dailyCompletedIds: StateFlow<Set<String>> = _dailyCompletedIds.asStateFlow()
    val weeklyCompletedIds: StateFlow<Set<String>> = _weeklyCompletedIds.asStateFlow()
    val monthlyCompletedIds: StateFlow<Set<String>> = _monthlyCompletedIds.asStateFlow()

    // ================================ STATO UTENTE ================================

    private val _streak = MutableStateFlow(0)
    private val _level = MutableStateFlow(1)
    private val _isLoading = MutableStateFlow(true)

    val streak: StateFlow<Int> = _streak.asStateFlow()
    val level: StateFlow<Int> = _level.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ================================ STATO BOOST XP ================================

    private val _xpBoostActive = MutableStateFlow(false)
    val xpBoostActive: StateFlow<Boolean> = _xpBoostActive.asStateFlow()

    private val _xpBoostMultiplier = MutableStateFlow(1.0f)
    val xpBoostMultiplier: StateFlow<Float> = _xpBoostMultiplier.asStateFlow()

    // ================================ STATO TIMER RESET ================================

    private val _dailyResetTime = MutableStateFlow("")
    private val _weeklyResetTime = MutableStateFlow("")
    private val _monthlyResetTime = MutableStateFlow("")
    private val _isNewDay = MutableStateFlow(false)

    val dailyResetTime: StateFlow<String> = _dailyResetTime.asStateFlow()
    val weeklyResetTime: StateFlow<String> = _weeklyResetTime.asStateFlow()
    val monthlyResetTime: StateFlow<String> = _monthlyResetTime.asStateFlow()
    val isNewDay: StateFlow<Boolean> = _isNewDay.asStateFlow()

    // ================================ STATO PROGRESSO ================================

    private val _questProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val questProgress: StateFlow<Map<String, Int>> = _questProgress.asStateFlow()

    // ================================ STATO VERIFICA ================================

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    // ================================ INIZIALIZZAZIONE ================================

    init {
        observeData()
        startResetTimers()
    }

    // ================================ METODI PUBBLICI ================================

    /**
     * Imposta la funzione per mostrare snackbar.
     */
    fun setSnackbarFunction(onShowSnackbar: (String) -> Unit) {
        showSnackbar = onShowSnackbar
    }

    /**
     * Registra un progresso per una missione a step.
     * Se il progresso raggiunge il target, la missione viene completata.
     *
     * **Controllo temporale:**
     * - Se `progressUnit == "giorni"` → attesa di 24 ore tra le registrazioni.
     * - Se `duration > 0` → attesa di `duration` minuti.
     * - Altrimenti → nessuna attesa.
     */
    fun registerProgress(quest: Quest) {
        viewModelScope.launch {
            // Controllo temporale per missioni step
            if (quest.targetProgress > 1) {
                val lastTime = repository.getLastRegisterTime(quest.id).first()
                val now = System.currentTimeMillis()
                val elapsedMinutes = (now - lastTime) / (60 * 1000)

                val requiredMinutes = when {
                    quest.progressUnit == "giorni" -> 1440
                    quest.duration > 0 -> quest.duration
                    else -> 0
                }

                if (requiredMinutes > 0 && lastTime != 0L && elapsedMinutes < requiredMinutes) {
                    val remaining = requiredMinutes - elapsedMinutes
                    val display = if (quest.progressUnit == "giorni") {
                        val days = (remaining / (24 * 60)).toInt()
                        val hours = (remaining % (24 * 60) / 60).toInt()
                        if (days > 0) "${days} giorni e ${hours} ore" else "${hours} ore"
                    } else {
                        "${remaining.toInt()} minuti"
                    }
                    showFeedback("⏳ Attendi ancora $display prima di registrare un nuovo progresso.")
                    return@launch
                }
            }

            // Procedi con la registrazione del progresso
            val current = _questProgress.value[quest.id] ?: 0
            val newProgress = current + 1
            repository.setQuestProgress(quest.id, newProgress)
            _questProgress.value = _questProgress.value + (quest.id to newProgress)
            repository.setLastRegisterTime(quest.id, System.currentTimeMillis())

            if (newProgress >= quest.targetProgress) {
                when {
                    quest.id.startsWith("q") -> completeQuest(quest)
                    quest.id.startsWith("w") -> completeWeeklyQuest(quest)
                    quest.id.startsWith("m") -> completeMonthlyQuest(quest)
                }
                repository.setQuestProgress(quest.id, 0)
                _questProgress.value = _questProgress.value + (quest.id to 0)
            }
        }
    }

    /**
     * Completa una missione giornaliera.
     */
    fun completeQuest(quest: Quest) {
        viewModelScope.launch {
            repository.completeQuest(quest.id)
            val addedXp = repository.addXp(quest.xpReward)
            repository.updateStreak()

            val currentXp = repository.getXp().first()
            val currentLevel = repository.getLevel().first()
            val completedQuests = repository.getCompletedQuests().first()
            val currentStreak = repository.getStreak().first()
            val unlockedBadges = repository.getUnlockedBadges().first()

            val newBadges = checkBadges(
                xp = currentXp,
                level = currentLevel,
                completedQuests = completedQuests.size,
                streak = currentStreak,
                unlockedBadges = unlockedBadges
            )

            if (newBadges.isNotEmpty()) {
                val badgeNames = newBadges.joinToString(", ") { it.name }
                showFeedback("🏅 Badge sbloccato: $badgeNames! 🎉")
            }

            newBadges.forEach { badge ->
                repository.unlockBadge(badge.id)
            }

            _dailyCompletedIds.value = repository.getCompletedQuests().first()
            _streak.value = repository.getStreak().first()

            handleSpecialReward(quest)

            repository.syncUserData()
            repository.checkAndUnlockChapters()

            showFeedback("✅ Missione completata! +$addedXp XP")
        }
    }

    /**
     * Completa una missione settimanale.
     */
    fun completeWeeklyQuest(quest: Quest) {
        viewModelScope.launch {
            repository.completeWeeklyQuest(quest.id)
            val addedXp = repository.addXp(quest.xpReward)
            _weeklyCompletedIds.value = repository.getWeeklyCompletedQuests().first()
            handleSpecialReward(quest)
            repository.syncUserData()
            repository.checkAndUnlockChapters()
            showFeedback("✅ Missione settimanale completata! +$addedXp XP")
        }
    }

    /**
     * Completa una missione mensile.
     */
    fun completeMonthlyQuest(quest: Quest) {
        viewModelScope.launch {
            repository.completeMonthlyQuest(quest.id)
            val addedXp = repository.addXp(quest.xpReward)
            _monthlyCompletedIds.value = repository.getMonthlyCompletedQuests().first()
            handleSpecialReward(quest)
            repository.syncUserData()
            repository.checkAndUnlockChapters()
            showFeedback("✅ Missione mensile completata! +$addedXp XP")
        }
    }

    /**
     * Verifica una missione di tipo PHOTO o PHOTO_RECOGNITION con ML Kit.
     */
    fun verifyWithPhoto(quest: Quest, imageUri: Uri) {
        viewModelScope.launch {
            _isVerifying.value = true
            try {
                if (quest.verification == VerificationType.PHOTO_RECOGNITION) {
                    val recognized = recognizeObject(imageUri, quest.verificationExtra)
                    if (!recognized) {
                        val message = when (quest.verificationExtra.lowercase()) {
                            "book" -> "📖 Non vedo un libro nella foto. Assicurati che sia ben visibile."
                            "text" -> "✍️ Non vedo del testo scritto. La foto deve contenere scritte chiare."
                            "banana" -> "🍌 Non vedo una banana. Prova a inquadrarla bene."
                            else -> "❌ Oggetto '${quest.verificationExtra}' non riconosciuto."
                        }
                        showFeedback(message)
                        _isVerifying.value = false
                        return@launch
                    }
                }
                if (quest.targetProgress > 1) {
                    registerProgress(quest)
                } else {
                    when {
                        quest.id.startsWith("q") -> completeQuest(quest)
                        quest.id.startsWith("w") -> completeWeeklyQuest(quest)
                        quest.id.startsWith("m") -> completeMonthlyQuest(quest)
                    }
                }
                showFeedback("📸 Verifica completata! ✅")
            } catch (e: Exception) {
                showFeedback("❌ Errore durante la verifica: ${e.message}")
            } finally {
                _isVerifying.value = false
            }
        }
    }

    /**
     * Verifica una missione di tipo TEXT.
     */
    fun verifyWithText(quest: Quest, text: String) {
        viewModelScope.launch {
            if (text.isBlank()) {
                showFeedback("❌ Inserisci un testo valido.")
                return@launch
            }
            if (quest.targetProgress > 1) {
                registerProgress(quest)
            } else {
                when {
                    quest.id.startsWith("q") -> completeQuest(quest)
                    quest.id.startsWith("w") -> completeWeeklyQuest(quest)
                    quest.id.startsWith("m") -> completeMonthlyQuest(quest)
                }
            }
            showFeedback("✏️ Testo salvato!")
        }
    }

    /**
     * Forza il refresh delle missioni (utile per debug).
     */
    fun forceRefreshQuests() {
        viewModelScope.launch {
            repository.invalidateAllQuestCaches()
            dailyQuestsLoaded = false
            weeklyQuestsLoaded = false
            monthlyQuestsLoaded = false
            observeData()
            showFeedback("🔄 Missioni rigenerate!")
        }
    }

    /**
     * Sincronizza i dati utente su Firestore.
     */
    fun syncUserData() {
        viewModelScope.launch {
            repository.syncUserData()
        }
    }

    // ================================ FUNZIONI PRIVATE ================================

    private fun showFeedback(message: String) {
        showSnackbar?.invoke(message)
    }

    /**
     * Gestisce l'assegnazione della ricompensa speciale di una missione.
     */
    private suspend fun handleSpecialReward(quest: Quest) {
        val reward = quest.specialReward ?: return

        when (reward) {
            is com.example.cares.utils.SpecialReward.StreakSave -> {
                repository.addStreakSave(reward.amount)
                showFeedback("🛡️ Hai ottenuto ${reward.amount} Scudo/i Streak!")
            }
            is com.example.cares.utils.SpecialReward.XpBoost -> {
                repository.addXpBoost(reward.multiplier, reward.days)
                showFeedback("⚡ Hai ottenuto un Boost XP x${reward.multiplier} per ${reward.days} giorni! Attivalo dall'inventario.")
            }
            is com.example.cares.utils.SpecialReward.StoryFragment -> {
                repository.addStoryFragment(reward.chapterId)
                showFeedback("📖 Hai sbloccato il frammento di storia: '${reward.chapterTitle}'!")
            }
        }
    }

    /**
     * Riconoscimento oggetti e testo con ML Kit.
     */
    private suspend fun recognizeObject(imageUri: Uri, expectedObject: String): Boolean {
        return suspendCoroutine { continuation ->
            try {
                val context = repository.getContext()
                val image = InputImage.fromFilePath(context, imageUri)

                when {
                    expectedObject.equals("text", ignoreCase = true) -> {
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                recognizer.close()
                                continuation.resume(visionText.text.isNotBlank())
                            }
                            .addOnFailureListener { _ ->
                                recognizer.close()
                                continuation.resume(false)
                            }
                    }
                    else -> {
                        val options = ObjectDetectorOptions.Builder()
                            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                            .enableMultipleObjects()
                            .build()
                        val detector = ObjectDetection.getClient(options)
                        detector.process(image)
                            .addOnSuccessListener { objects ->
                                detector.close()
                                val found = objects.any { obj ->
                                    obj.labels.any { label ->
                                        label.text.contains(expectedObject, ignoreCase = true)
                                    }
                                }
                                continuation.resume(found)
                            }
                            .addOnFailureListener { _ ->
                                detector.close()
                                continuation.resume(false)
                            }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume(false)
            }
        }
    }

    /**
     * Osserva i dati utente e carica le missioni con cache persistente.
     */
    private fun observeData() {
        viewModelScope.launch {
            repository.checkAndResetQuests()
            repository.checkAndResetWeeklyQuests()
            repository.checkAndResetMonthlyQuests()
            repository.checkXpBoostExpiry()
            repository.checkStreakExpiry()

            repository.getAllPreferences().collect { prefs ->
                val userLevel = prefs[UserPreferencesManager.USER_LEVEL] ?: 1
                val activityLevel = prefs[UserPreferencesManager.ACTIVITY_LEVEL] ?: "Moderato"
                val fitnessGoal = prefs[UserPreferencesManager.FITNESS_GOAL] ?: "Mantenimento"
                val dailyGoal = prefs[UserPreferencesManager.DAILY_GOAL] ?: "Nessun obiettivo"

                _level.value = userLevel
                _streak.value = prefs[UserPreferencesManager.DAILY_STREAK] ?: 0
                _xpBoostActive.value = prefs[UserPreferencesManager.XP_BOOST_ACTIVE] ?: false
                _xpBoostMultiplier.value = prefs[UserPreferencesManager.XP_BOOST_MULTIPLIER] ?: 1.0f

                // ---- MISSIONI GIORNALIERE ----
                if (!dailyQuestsLoaded || _dailyQuests.value.isEmpty()) {
                    val savedQuests = repository.loadDailyQuests()
                    if (savedQuests != null && savedQuests.isNotEmpty()) {
                        _dailyQuests.value = savedQuests
                    } else {
                        val newQuests = repository.getDailyQuests(
                            level = userLevel,
                            activityLevel = activityLevel,
                            fitnessGoal = fitnessGoal,
                            dailyGoal = dailyGoal
                        )
                        _dailyQuests.value = newQuests
                        repository.saveDailyQuests(newQuests)
                    }
                    dailyQuestsLoaded = true
                }
                _dailyCompletedIds.value = prefs[UserPreferencesManager.COMPLETED_QUESTS] ?: emptySet()

                // ---- MISSIONI SETTIMANALI ----
                if (!weeklyQuestsLoaded || _weeklyQuests.value.isEmpty()) {
                    val savedQuests = repository.loadWeeklyQuests()
                    if (savedQuests != null && savedQuests.isNotEmpty()) {
                        _weeklyQuests.value = savedQuests
                    } else {
                        val newQuests = repository.getWeeklyQuests(userLevel, activityLevel, fitnessGoal)
                        _weeklyQuests.value = newQuests
                        repository.saveWeeklyQuests(newQuests)
                    }
                    weeklyQuestsLoaded = true
                }
                _weeklyCompletedIds.value = prefs[UserPreferencesManager.WEEKLY_QUESTS_COMPLETED] ?: emptySet()

                // ---- MISSIONI MENSILI ----
                if (!monthlyQuestsLoaded || _monthlyQuests.value.isEmpty()) {
                    val savedQuests = repository.loadMonthlyQuests()
                    if (savedQuests != null && savedQuests.isNotEmpty()) {
                        _monthlyQuests.value = savedQuests
                    } else {
                        val newQuests = repository.getMonthlyQuests(userLevel, activityLevel, fitnessGoal)
                        _monthlyQuests.value = newQuests
                        repository.saveMonthlyQuests(newQuests)
                    }
                    monthlyQuestsLoaded = true
                }
                _monthlyCompletedIds.value = prefs[UserPreferencesManager.MONTHLY_QUESTS_COMPLETED] ?: emptySet()

                // ---- Progresso missioni ----
                val allQuests = _dailyQuests.value + _weeklyQuests.value + _monthlyQuests.value
                val progressMap = allQuests.associate { quest ->
                    quest.id to (repository.getQuestProgress(quest.id).first())
                }
                _questProgress.value = progressMap

                _isLoading.value = false
            }
        }
    }

    /**
     * Avvia i timer di reset per missioni giornaliere/settimanali/mensili.
     */
    private fun startResetTimers() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                updateDailyResetTime()
                updateWeeklyResetTime()
                updateMonthlyResetTime()
                delay(1000L)
            }
        }
    }

    private fun updateDailyResetTime() {
        val now = Calendar.getInstance()
        val nextReset = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val diff = nextReset.timeInMillis - System.currentTimeMillis()
        _dailyResetTime.value = formatTime(diff)
        if (diff <= 0) {
            viewModelScope.launch {
                resetDailyQuestsAndRefresh()
            }
        }
    }

    private fun updateWeeklyResetTime() {
        val now = Calendar.getInstance()
        val nextReset = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val currentDay = get(Calendar.DAY_OF_WEEK)
            val daysUntilMonday = if (currentDay == Calendar.MONDAY) 7 else (Calendar.MONDAY - currentDay + 7) % 7
            add(Calendar.DAY_OF_YEAR, daysUntilMonday)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 7)
            }
        }
        val diff = nextReset.timeInMillis - System.currentTimeMillis()
        _weeklyResetTime.value = formatTime(diff)
        if (diff <= 0) {
            viewModelScope.launch {
                resetWeeklyQuestsAndRefresh()
            }
        }
    }

    private fun updateMonthlyResetTime() {
        val now = Calendar.getInstance()
        val nextReset = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.MONTH, 1)
            }
        }
        val diff = nextReset.timeInMillis - System.currentTimeMillis()
        _monthlyResetTime.value = formatTime(diff)
        if (diff <= 0) {
            viewModelScope.launch {
                resetMonthlyQuestsAndRefresh()
            }
        }
    }

    private fun formatTime(millis: Long): String {
        if (millis <= 0) return "0s"
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis)
        val days = totalSeconds / (24 * 3600)
        val hours = (totalSeconds % (24 * 3600)) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val parts = mutableListOf<String>()
        if (days > 0) parts.add("${days}d")
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0) parts.add("${minutes}m")
        parts.add("${seconds}s")
        return parts.joinToString(" ")
    }

    private suspend fun resetDailyQuestsAndRefresh() {
        repository.resetDailyQuests()
        repository.resetStepCounter()
        repository.invalidateDailyQuestsCache()

        val prefs = repository.getAllPreferences().first()
        val userLevel = prefs[UserPreferencesManager.USER_LEVEL] ?: 1
        val activityLevel = prefs[UserPreferencesManager.ACTIVITY_LEVEL] ?: "Moderato"
        val fitnessGoal = prefs[UserPreferencesManager.FITNESS_GOAL] ?: "Mantenimento"
        val dailyGoal = prefs[UserPreferencesManager.DAILY_GOAL] ?: "Nessun obiettivo"

        val oldDailyIds = _dailyQuests.value.map { it.id }.toSet()
        repository.resetQuestProgresses(oldDailyIds)

        val newQuests = repository.getDailyQuests(
            level = userLevel,
            activityLevel = activityLevel,
            fitnessGoal = fitnessGoal,
            dailyGoal = dailyGoal
        )
        _dailyQuests.value = newQuests
        repository.saveDailyQuests(newQuests)

        _dailyCompletedIds.value = repository.getCompletedQuests().first()
        _streak.value = repository.getStreak().first()
        _level.value = userLevel
        _isNewDay.value = true
        dailyQuestsLoaded = true
        delay(3000L)
        _isNewDay.value = false
    }

    private suspend fun resetWeeklyQuestsAndRefresh() {
        repository.resetWeeklyQuests()
        repository.invalidateWeeklyQuestsCache()

        val prefs = repository.getAllPreferences().first()
        val userLevel = prefs[UserPreferencesManager.USER_LEVEL] ?: 1
        val activityLevel = prefs[UserPreferencesManager.ACTIVITY_LEVEL] ?: "Moderato"
        val fitnessGoal = prefs[UserPreferencesManager.FITNESS_GOAL] ?: "Mantenimento"

        val oldWeeklyIds = _weeklyQuests.value.map { it.id }.toSet()
        repository.resetQuestProgresses(oldWeeklyIds)

        val newQuests = repository.getWeeklyQuests(userLevel, activityLevel, fitnessGoal)
        _weeklyQuests.value = newQuests
        repository.saveWeeklyQuests(newQuests)

        _weeklyCompletedIds.value = repository.getWeeklyCompletedQuests().first()
        weeklyQuestsLoaded = true
    }

    private suspend fun resetMonthlyQuestsAndRefresh() {
        repository.resetMonthlyQuests()
        repository.invalidateMonthlyQuestsCache()

        val prefs = repository.getAllPreferences().first()
        val userLevel = prefs[UserPreferencesManager.USER_LEVEL] ?: 1
        val activityLevel = prefs[UserPreferencesManager.ACTIVITY_LEVEL] ?: "Moderato"
        val fitnessGoal = prefs[UserPreferencesManager.FITNESS_GOAL] ?: "Mantenimento"

        val oldMonthlyIds = _monthlyQuests.value.map { it.id }.toSet()
        repository.resetQuestProgresses(oldMonthlyIds)

        val newQuests = repository.getMonthlyQuests(userLevel, activityLevel, fitnessGoal)
        _monthlyQuests.value = newQuests
        repository.saveMonthlyQuests(newQuests)

        _monthlyCompletedIds.value = repository.getMonthlyCompletedQuests().first()
        monthlyQuestsLoaded = true
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        fun factory(repository: CaresRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(QuestsViewModel::class.java)) {
                        return QuestsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

// ================================ QUESTS SCREEN (UI) ================================

/**
 * Schermata delle missioni con tab per giornaliere, settimanali e mensili.
 *
 * **Caratteristiche:**
 * - Tab per navigare tra le tre categorie di missioni.
 * - Timer di reset per ogni categoria.
 * - Visualizzazione dello streak corrente e del boost XP attivo.
 * - Supporto per missioni a step con barra di progresso.
 * - Supporto per verifiche con foto (ML Kit) e testo.
 * - Dialog per anteprima foto e inserimento testo.
 *
 * @param viewModel ViewModel per la gestione della logica.
 * @param isDarkTheme Se `true`, applica il tema scuro.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QuestsScreen(
    viewModel: QuestsViewModel,
    isDarkTheme: Boolean = true
) {
    val dailyQuests by viewModel.dailyQuests.collectAsState()
    val weeklyQuests by viewModel.weeklyQuests.collectAsState()
    val monthlyQuests by viewModel.monthlyQuests.collectAsState()
    val dailyCompletedIds by viewModel.dailyCompletedIds.collectAsState()
    val weeklyCompletedIds by viewModel.weeklyCompletedIds.collectAsState()
    val monthlyCompletedIds by viewModel.monthlyCompletedIds.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val level by viewModel.level.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val dailyResetTime by viewModel.dailyResetTime.collectAsState()
    val weeklyResetTime by viewModel.weeklyResetTime.collectAsState()
    val monthlyResetTime by viewModel.monthlyResetTime.collectAsState()
    val isNewDay by viewModel.isNewDay.collectAsState()
    val questProgress by viewModel.questProgress.collectAsState()
    val isVerifying by viewModel.isVerifying.collectAsState()
    val xpBoostActive by viewModel.xpBoostActive.collectAsState()
    val xpBoostMultiplier by viewModel.xpBoostMultiplier.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("📅 Giornaliere", "📆 Settimanali", "🗓️ Mensili")

    // ---- Stato dialog ----
    var showTextDialog by remember { mutableStateOf(false) }
    var selectedQuestForText by remember { mutableStateOf<Quest?>(null) }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedQuestForPhoto by remember { mutableStateOf<Quest?>(null) }
    var showPhotoPreview by remember { mutableStateOf(false) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    // ---- Launcher fotocamera ----
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            previewUri = photoUri
            showPhotoPreview = true
        } else {
            photoUri = null
            selectedQuestForPhoto = null
        }
    }

    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )

    // ---- Funzioni di verifica ----
    fun startPhotoVerification(quest: Quest) {
        if (cameraPermissionState.status.isGranted) {
            val file = File(context.cacheDir, "quest_photo_${UUID.randomUUID()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            photoUri = uri
            selectedQuestForPhoto = quest
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    fun startTextVerification(quest: Quest) {
        selectedQuestForText = quest
        showTextDialog = true
    }

    // ================================ UI ================================

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Caricamento missioni...", color = MaterialTheme.colorScheme.onBackground)
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Gradients.mainBackground(isDarkTheme))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ---- Header ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Missioni",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🔥", fontSize = 18.sp)
                        Text(
                            streak.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Neon.Orange
                        )
                    }
                }

                Text(
                    text = "Livello $level - Missioni potenziate!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // ---- XP Boost Indicator ----
                if (xpBoostActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                color = Neon.Cyan.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Neon.Cyan.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⚡ Boost XP attivo! x$xpBoostMultiplier",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Neon.Cyan
                        )
                    }
                }

                // ---- Tab ----
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Neon.Green,
                            height = 2.dp
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) Neon.Green else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            },
                            selectedContentColor = Neon.Green,
                            unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ---- Timer Reset ----
                val resetTime = when (selectedTabIndex) {
                    0 -> dailyResetTime
                    1 -> weeklyResetTime
                    else -> monthlyResetTime
                }
                val resetLabel = when (selectedTabIndex) {
                    0 -> if (isNewDay) "Missioni appena aggiornate!" else "Prossimo reset giornaliero"
                    1 -> "Prossimo reset settimanale"
                    else -> "Prossimo reset mensile"
                }
                val isNew = selectedTabIndex == 0 && isNewDay

                TimerResetRow(
                    resetTime = resetTime,
                    label = resetLabel,
                    isNew = isNew
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ---- Lista Missioni ----
                when (selectedTabIndex) {
                    0 -> QuestList(
                        quests = dailyQuests,
                        completedIds = dailyCompletedIds,
                        progressMap = questProgress,
                        onCompleteQuest = { viewModel.completeQuest(it) },
                        onRegisterProgress = { viewModel.registerProgress(it) },
                        onVerifyPhoto = { startPhotoVerification(it) },
                        onVerifyText = { startTextVerification(it) },
                        isVerifying = isVerifying,
                        isDarkTheme = isDarkTheme,
                        emptyMessage = "Nessuna missione giornaliera disponibile."
                    )
                    1 -> QuestList(
                        quests = weeklyQuests,
                        completedIds = weeklyCompletedIds,
                        progressMap = questProgress,
                        onCompleteQuest = { viewModel.completeWeeklyQuest(it) },
                        onRegisterProgress = { viewModel.registerProgress(it) },
                        onVerifyPhoto = { startPhotoVerification(it) },
                        onVerifyText = { startTextVerification(it) },
                        isVerifying = isVerifying,
                        isDarkTheme = isDarkTheme,
                        emptyMessage = "Nessuna missione settimanale disponibile."
                    )
                    else -> QuestList(
                        quests = monthlyQuests,
                        completedIds = monthlyCompletedIds,
                        progressMap = questProgress,
                        onCompleteQuest = { viewModel.completeMonthlyQuest(it) },
                        onRegisterProgress = { viewModel.registerProgress(it) },
                        onVerifyPhoto = { startPhotoVerification(it) },
                        onVerifyText = { startTextVerification(it) },
                        isVerifying = isVerifying,
                        isDarkTheme = isDarkTheme,
                        emptyMessage = "Nessuna missione mensile disponibile."
                    )
                }
            }
        }
    }

    // ---- Dialog Verifica Testo ----
    if (showTextDialog && selectedQuestForText != null) {
        var textInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                showTextDialog = false
                selectedQuestForText = null
            },
            title = { Text("✏️ Verifica missione") },
            text = {
                Column {
                    Text(
                        text = selectedQuestForText!!.verificationPrompt.ifBlank {
                            "Inserisci il testo richiesto per completare la missione:"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Testo") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Neon.Green,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.verifyWithText(selectedQuestForText!!, textInput)
                            showTextDialog = false
                            selectedQuestForText = null
                        }
                    }
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTextDialog = false
                        selectedQuestForText = null
                    }
                ) {
                    Text("Annulla")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    // ---- Dialog Anteprima Foto ----
    if (showPhotoPreview && previewUri != null && selectedQuestForPhoto != null) {
        AlertDialog(
            onDismissRequest = { /* Non permettere dismiss senza azione */ },
            title = { Text("📸 Anteprima verifica") },
            text = {
                Column {
                    Text(
                        text = "Confermi questa foto per la verifica?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        painter = rememberAsyncImagePainter(previewUri),
                        contentDescription = "Foto verifica",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val quest = selectedQuestForPhoto
                        val uri = previewUri
                        showPhotoPreview = false
                        previewUri = null
                        if (quest != null && uri != null) {
                            viewModel.verifyWithPhoto(quest, uri)
                        }
                        photoUri = null
                        selectedQuestForPhoto = null
                    },
                    enabled = !isVerifying
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Conferma")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPhotoPreview = false
                        previewUri = null
                        photoUri = null
                        selectedQuestForPhoto = null
                    }
                ) {
                    Text("Riprova")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ================================ COMPONENTI UI ================================

/**
 * Riga del timer di reset per missioni.
 */
@Composable
fun TimerResetRow(
    resetTime: String,
    label: String,
    isNew: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = if (isNew) Neon.Green.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (isNew) Neon.Green.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isNew) "✅" else "⏳",
                fontSize = 18.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isNew) Neon.Green else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Text(
            text = if (isNew) "🎉" else resetTime,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isNew) Neon.Green else Neon.Cyan,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Lista delle missioni con animazione di entrata.
 */
@Composable
fun QuestList(
    quests: List<Quest>,
    completedIds: Set<String>,
    progressMap: Map<String, Int>,
    onCompleteQuest: (Quest) -> Unit,
    onRegisterProgress: (Quest) -> Unit,
    onVerifyPhoto: (Quest) -> Unit,
    onVerifyText: (Quest) -> Unit,
    isVerifying: Boolean,
    isDarkTheme: Boolean,
    emptyMessage: String = "Nessuna missione disponibile."
) {
    if (quests.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = quests,
                key = { it.id }
            ) { quest ->
                AnimatedEntrance(
                    delay = quests.indexOf(quest) * 80
                ) {
                    QuestItem(
                        quest = quest,
                        isCompleted = completedIds.contains(quest.id),
                        progress = progressMap[quest.id] ?: 0,
                        onComplete = { onCompleteQuest(quest) },
                        onRegister = { onRegisterProgress(quest) },
                        onVerify = {
                            when (quest.verification) {
                                VerificationType.PHOTO, VerificationType.PHOTO_RECOGNITION -> onVerifyPhoto(quest)
                                VerificationType.TEXT -> onVerifyText(quest)
                                else -> {}
                            }
                        },
                        isVerifying = isVerifying,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}

/**
 * Item singolo di una missione.
 */
@Composable
fun QuestItem(
    quest: Quest,
    isCompleted: Boolean,
    progress: Int,
    onComplete: () -> Unit,
    onRegister: () -> Unit,
    onVerify: () -> Unit,
    isVerifying: Boolean,
    isDarkTheme: Boolean
) {
    val color = if (isCompleted) Neon.Green else Neon.Cyan
    val isStepQuest = quest.targetProgress > 1
    val progressFraction = if (isStepQuest) progress.toFloat() / quest.targetProgress else 0f
    val isPersonalGoal = quest.id.startsWith("q_daily_goal")
    val needsVerification = quest.verification != VerificationType.NONE

    val (buttonText, buttonAction) = when {
        isCompleted -> "✅" to ({} as () -> Unit)
        isVerifying && needsVerification -> "⏳" to ({} as () -> Unit)
        needsVerification && quest.verification == VerificationType.TEXT -> "✏️" to onVerify
        needsVerification && (quest.verification == VerificationType.PHOTO || quest.verification == VerificationType.PHOTO_RECOGNITION) -> "📸" to onVerify
        isStepQuest -> "Registra" to onRegister
        else -> "Vai" to onComplete
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        darkTheme = isDarkTheme
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isPersonalGoal && !isCompleted) {
                        Text(text = "🎯", fontSize = 16.sp)
                    }
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isPersonalGoal) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isPersonalGoal) Neon.Green else MaterialTheme.colorScheme.onSurface
                    )
                    if (needsVerification && !isCompleted) {
                        when (quest.verification) {
                            VerificationType.PHOTO -> Text("📸", fontSize = 14.sp)
                            VerificationType.PHOTO_RECOGNITION -> Text("🔍", fontSize = 14.sp)
                            VerificationType.TEXT -> Text("✏️", fontSize = 14.sp)
                            else -> {}
                        }
                    }
                }

                Text(
                    text = quest.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "+${quest.xpReward} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neon.Green
                    )
                    if (quest.duration > 0) {
                        Text(
                            text = "${quest.duration} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = Neon.Cyan
                        )
                    }
                    if (isStepQuest) {
                        Text(
                            text = "${quest.targetProgress} ${quest.progressUnit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Neon.Purple
                        )
                    }
                }

                // Barra di progresso per missioni a step
                if (isStepQuest && !isCompleted) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isPersonalGoal) Neon.Green else Neon.Cyan,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Text(
                            text = "$progress/${quest.targetProgress} ${quest.progressUnit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPersonalGoal) Neon.Green else Neon.Cyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Prompt di verifica
                if (needsVerification && !isCompleted && quest.verificationPrompt.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 ${quest.verificationPrompt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // Pulsante azione
            GlassButton(
                text = buttonText,
                onClick = buttonAction,
                modifier = Modifier,
                color = when {
                    isCompleted -> Neon.Green
                    isPersonalGoal -> Neon.Green
                    else -> color
                },
                enabled = !isCompleted && !isVerifying
            )
        }
    }
}

/**
 * Pulsante in stile glassmorphism.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Neon.Green,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(40.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = null
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
            letterSpacing = 0.3.sp
        )
    }
}