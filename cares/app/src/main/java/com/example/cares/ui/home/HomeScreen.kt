// file: ui/home/HomeScreen.kt
package com.example.cares.ui.home

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cares.data.manager.NotificationHelper
import com.example.cares.data.manager.StepCounterManager
import com.example.cares.data.manager.UserPreferencesManager
import com.example.cares.data.models.weather.Weather
import com.example.cares.data.repository.CaresRepository
import com.example.cares.data.repository.WeatherRepository
import com.example.cares.ui.celebration.StreakCelebrationManager
import com.example.cares.utils.checkBadges
import com.example.cares.ui.theme.Gradients
import com.example.cares.ui.theme.Neon
import com.example.cares.ui.theme.GlassCard
import com.example.cares.ui.animation.*
import com.example.cares.utils.Quest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ================================ HOME VIEW MODEL ================================

/**
 * ViewModel per la schermata principale (Home).
 *
 * **Responsabilità principali:**
 * - Gestire lo stato dell'utente (avatar, XP, livello, streak, obiettivi)
 * - Gestire la missione principale (timer, stato, completamento)
 * - Gestire l'obiettivo giornaliero personalizzato (step e durata)
 * - Gestire il meteo e le attività consigliate
 * - Gestire il diario delle emozioni
 * - Gestire la ricompensa settimanale
 * - Gestire il tracking dei passi
 * - Gestire il Boost XP
 *
 * **Flusso dei dati:**
 * 1. All'inizializzazione, osserva le preferenze utente e carica i dati.
 * 2. Le modifiche ai dati (XP, livello, streak) vengono riflesse automaticamente.
 * 3. Le missioni vengono caricate e gestite con timer e tracking passi.
 * 4. L'obiettivo giornaliero supporta due modalità: step (con registrazione manuale) e durata (con timer).
 *
 * @param repository Repository per l'accesso ai dati e alle operazioni di rete.
 */
class HomeViewModel(
    private val repository: CaresRepository
) : ViewModel() {

    // ================================ ENUM ================================

    /**
     * Stati possibili per una missione.
     */
    enum class MissionState {
        IDLE,       // Inattiva, in attesa di nuova missione
        WAITING,    // In attesa che l'utente la inizi
        RUNNING,    // In esecuzione (timer attivo)
        COMPLETED   // Completata
    }

    // ================================ PROPRIETÀ PRIVATE ================================

    private var showSnackbar: ((String) -> Unit)? = null
    private var timerJob: Job? = null
    private var stepTrackingJob: Job? = null
    private var goalTimerJob: Job? = null

    private val notificationHelper = NotificationHelper(repository.getContext())
    private val weatherRepository = WeatherRepository(repository.getContext())
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(repository.getContext())

    // Flag per evitare ricariche continue
    private var dailyQuestLoaded = false
    private var previousLevel = 0
    private var previousDailyGoal = ""
    private var previousActivityLevel = ""
    private var previousFitnessGoal = ""

    private var lastCelebratedStreak = 0

    // ================================ STATO UTENTE ================================

    private val _avatar = MutableStateFlow("🧙")
    private val _level = MutableStateFlow("Non impostato")
    private val _goal = MutableStateFlow("Non impostato")
    private val _dailyGoal = MutableStateFlow("Nessun obiettivo")
    private val _xp = MutableStateFlow(0)
    private val _userLevel = MutableStateFlow(1)
    private val _streak = MutableStateFlow(0)
    private val _isLoading = MutableStateFlow(true)

    val avatar: StateFlow<String> = _avatar.asStateFlow()
    val level: StateFlow<String> = _level.asStateFlow()
    val goal: StateFlow<String> = _goal.asStateFlow()
    val dailyGoal: StateFlow<String> = _dailyGoal.asStateFlow()
    val xp: StateFlow<Int> = _xp.asStateFlow()
    val userLevel: StateFlow<Int> = _userLevel.asStateFlow()
    val streak: StateFlow<Int> = _streak.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ================================ STATO MISSIONE PRINCIPALE ================================

    private val _missionState = MutableStateFlow(MissionState.IDLE)
    private val _timerRemaining = MutableStateFlow(0)
    private val _questTitle = MutableStateFlow("Caricamento missione...")
    private val _questDuration = MutableStateFlow(0)
    private val _permissionError = MutableStateFlow<String?>(null)

    val missionState: StateFlow<MissionState> = _missionState.asStateFlow()
    val timerRemaining: StateFlow<Int> = _timerRemaining.asStateFlow()
    val questTitle: StateFlow<String> = _questTitle.asStateFlow()
    val questDuration: StateFlow<Int> = _questDuration.asStateFlow()
    val permissionError: StateFlow<String?> = _permissionError.asStateFlow()

    // ================================ STATO OBIETTIVO GIORNALIERO ================================

    private val _dailyGoalQuest = MutableStateFlow<Quest?>(null)
    val dailyGoalQuest: StateFlow<Quest?> = _dailyGoalQuest.asStateFlow()

    private val _dailyGoalProgress = MutableStateFlow(0)
    val dailyGoalProgress: StateFlow<Int> = _dailyGoalProgress.asStateFlow()

    private val _dailyGoalTarget = MutableStateFlow(0)
    val dailyGoalTarget: StateFlow<Int> = _dailyGoalTarget.asStateFlow()

    // Stato timer per obiettivo a durata
    private val _goalMissionState = MutableStateFlow(MissionState.IDLE)
    private val _goalTimerRemaining = MutableStateFlow(0)

    val goalMissionState: StateFlow<MissionState> = _goalMissionState.asStateFlow()
    val goalTimerRemaining: StateFlow<Int> = _goalTimerRemaining.asStateFlow()

    // ================================ STATO METEO ================================

    private val _weather = MutableStateFlow<Weather?>(null)
    private val _recommendedActivity = MutableStateFlow<String>("")
    private val _isWeatherLoading = MutableStateFlow(false)

    val weather: StateFlow<Weather?> = _weather.asStateFlow()
    val recommendedActivity: StateFlow<String> = _recommendedActivity.asStateFlow()
    val isWeatherLoading: StateFlow<Boolean> = _isWeatherLoading.asStateFlow()

    // ================================ STATO DIARIO EMOZIONI ================================

    private val _todayMood = MutableStateFlow<Int?>(null)
    private val _moodHistory = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _averageMood = MutableStateFlow(0f)
    private val _isMoodLoading = MutableStateFlow(false)

    val todayMood: StateFlow<Int?> = _todayMood.asStateFlow()
    val moodHistory: StateFlow<Map<String, Int>> = _moodHistory.asStateFlow()
    val averageMood: StateFlow<Float> = _averageMood.asStateFlow()
    val isMoodLoading: StateFlow<Boolean> = _isMoodLoading.asStateFlow()

    // ================================ STATO PASSI ================================

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    // ================================ STATO MISSIONE ================================

    private val _currentQuestTarget = MutableStateFlow(0)
    val currentQuestTarget: StateFlow<Int> = _currentQuestTarget.asStateFlow()

    // ================================ STATO BOOST XP ================================

    private val _xpBoostActive = MutableStateFlow(false)
    val xpBoostActive: StateFlow<Boolean> = _xpBoostActive.asStateFlow()
    private val _xpBoostMultiplier = MutableStateFlow(1.0f)
    val xpBoostMultiplier: StateFlow<Float> = _xpBoostMultiplier.asStateFlow()

    // ================================ STATO RICOMPENSA SETTIMANALE ================================

    private val _canClaimReward = MutableStateFlow(false)
    val canClaimReward: StateFlow<Boolean> = _canClaimReward.asStateFlow()
    private val _claimStreak = MutableStateFlow(0)
    val claimStreak: StateFlow<Int> = _claimStreak.asStateFlow()
    private val _claimDay = MutableStateFlow(1)
    val claimDay: StateFlow<Int> = _claimDay.asStateFlow()
    private val _isClaiming = MutableStateFlow(false)
    val isClaiming: StateFlow<Boolean> = _isClaiming.asStateFlow()

    // ================================ INIZIALIZZAZIONE ================================

    init {
        StepCounterManager.init(repository.getContext(), repository)
        observeUserData()
        observeStreakEvents()
    }

    /**
     * Osserva gli eventi di aggiornamento dello streak per attivare le celebrazioni.
     */
    private fun observeStreakEvents() {
        viewModelScope.launch {
            repository.streakEvents.collect { newStreak ->
                if (newStreak > lastCelebratedStreak && newStreak > 0) {
                    StreakCelebrationManager.triggerCelebration(newStreak)
                    lastCelebratedStreak = newStreak
                }
            }
        }
    }

    // ================================ METODI PUBBLICI ================================

    /**
     * Imposta la funzione per mostrare snackbar di feedback.
     */
    fun setSnackbarFunction(onShowSnackbar: (String) -> Unit) {
        showSnackbar = onShowSnackbar
    }

    /**
     * Mostra un messaggio di feedback all'utente.
     */
    private fun showFeedback(message: String) {
        showSnackbar?.invoke(message)
    }

    /**
     * Sincronizza i dati utente su Firestore.
     */
    fun syncUserData() {
        viewModelScope.launch {
            repository.checkStreakExpiry()
            repository.syncUserData()
        }
    }

    /**
     * Avvia la missione principale (se in stato WAITING).
     */
    fun startMission() {
        if (!hasActivityPermission()) {
            _permissionError.value = "⚠️ Permesso attività fisica richiesto per tracciare i passi"
            return
        }
        _permissionError.value = null

        if (_missionState.value == MissionState.WAITING) {
            viewModelScope.launch {
                timerJob?.cancel()
                val durationSeconds = if (_questDuration.value > 0) {
                    _questDuration.value * 60
                } else {
                    300
                }
                repository.startMissionTimer(durationSeconds)
                _missionState.value = MissionState.RUNNING
                _timerRemaining.value = durationSeconds
                _questTitle.value = "🏃‍♂️ Esercizio in corso..."
                startMissionTimer()
            }
        } else {
            showFeedback("⏳ Attendi: la missione non è ancora pronta per partire")
        }
    }

    /**
     * Completa manualmente la missione principale (se in stato RUNNING).
     */
    fun completeMissionManually() {
        viewModelScope.launch {
            if (_missionState.value == MissionState.RUNNING) {
                timerJob?.cancel()
                _missionState.value = MissionState.COMPLETED
                _questTitle.value = "✅ Missione completata! 🎉"
                repository.completeMission()
                val addedXp = repository.addXp(20)
                _streak.value = repository.updateStreak()
                _xp.value = repository.getXp().first()
                _userLevel.value = repository.getLevel().first()
                repository.syncUserData()
                showFeedback("🎉 Missione completata! +$addedXp XP")
                notificationHelper.sendMissionCompletedNotification(
                    questTitle = _questTitle.value,
                    xpGained = addedXp
                )
            }
        }
    }

    /**
     * Avvia una nuova missione (resetta il timer e ricarica la missione).
     */
    fun startNewQuest() {
        viewModelScope.launch {
            timerJob?.cancel()
            repository.resetMissionState()
            repository.saveTimerSeconds(1800)
            _missionState.value = MissionState.WAITING
            _timerRemaining.value = 1800
            dailyQuestLoaded = false
            loadDailyQuest()
            startReminderTimer()
        }
    }

    /**
     * Avvia il timer per l'obiettivo a durata.
     */
    fun startDailyGoalMission() {
        val quest = _dailyGoalQuest.value ?: return
        if (quest.duration <= 0 || quest.targetProgress != 1) {
            showFeedback("⚠️ Questo obiettivo non è a durata.")
            return
        }

        viewModelScope.launch {
            if (_goalMissionState.value == MissionState.WAITING) {
                goalTimerJob?.cancel()
                val durationSeconds = quest.duration * 60
                repository.startMissionTimer(durationSeconds)
                _goalMissionState.value = MissionState.RUNNING
                _goalTimerRemaining.value = durationSeconds
                startGoalTimer()
                showFeedback("⏱️ Obiettivo avviato!")
            } else {
                showFeedback("⏳ Attendi: l'obiettivo non è ancora pronto")
            }
        }
    }

    /**
     * Completa manualmente l'obiettivo a durata.
     */
    fun completeDailyGoalManually() {
        val quest = _dailyGoalQuest.value ?: return
        if (quest.duration <= 0 || quest.targetProgress != 1) {
            showFeedback("⚠️ Questo obiettivo non è a durata.")
            return
        }

        viewModelScope.launch {
            if (_goalMissionState.value == MissionState.RUNNING) {
                goalTimerJob?.cancel()
                _goalMissionState.value = MissionState.COMPLETED
                repository.completeMission()
                completeDailyGoalInternal()
            } else {
                showFeedback("⚠️ Avvia prima l'obiettivo.")
            }
        }
    }

    /**
     * Registra un progresso per l'obiettivo a step.
     */
    fun registerDailyGoalProgress() {
        viewModelScope.launch {
            val quest = _dailyGoalQuest.value ?: return@launch
            if (quest.targetProgress <= 1) {
                showFeedback("⚠️ Questo obiettivo non è a step.")
                return@launch
            }

            // Controllo temporale per evitare registrazioni troppo ravvicinate
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

            val current = _dailyGoalProgress.value
            val newProgress = current + 1
            repository.setQuestProgress(quest.id, newProgress)
            repository.setLastRegisterTime(quest.id, System.currentTimeMillis())
            _dailyGoalProgress.value = newProgress

            if (newProgress >= quest.targetProgress) {
                completeDailyGoalInternal()
            } else {
                showFeedback("✅ Progresso registrato: ${newProgress}/${quest.targetProgress}")
            }
        }
    }

    /**
     * Reclama la ricompensa settimanale.
     */
    fun claimDailyReward() {
        viewModelScope.launch {
            if (!_canClaimReward.value) return@launch
            _isClaiming.value = true
            val xpGained = repository.claimReward()
            repository.addXp(xpGained)
            _xp.value = repository.getXp().first()
            _userLevel.value = repository.getLevel().first()
            _canClaimReward.value = false
            _claimStreak.value = repository.getClaimStreak().first()
            _isClaiming.value = false
            showFeedback("🎉 Ricompensa ritirata! +$xpGained XP")
        }
    }

    /**
     * Salva l'umore di oggi.
     */
    fun saveMood(mood: Int) {
        viewModelScope.launch {
            repository.saveMood(mood)
            _todayMood.value = mood
            _moodHistory.value = repository.getMoodHistory(7)
            _averageMood.value = repository.getAverageMood(7)
        }
    }

    /**
     * Dismiss dell'errore di permesso.
     */
    fun dismissPermissionError() {
        _permissionError.value = null
    }

    /**
     * Recupera il meteo.
     */
    @SuppressLint("MissingPermission")
    fun fetchWeather() {
        viewModelScope.launch {
            weatherRepository.getWeather()?.let { cached ->
                if (_weather.value == null) {
                    _weather.value = cached
                    _recommendedActivity.value = getRecommendedActivity(cached, _userLevel.value, _streak.value)
                }
            }

            _isWeatherLoading.value = true

            val location = try {
                if (hasLocationPermission()) {
                    val lastLocation = fusedLocationClient.lastLocation.await()
                    lastLocation ?: fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        null
                    ).await()
                } else {
                    null
                }
            } catch (e: SecurityException) {
                Log.e("HomeViewModel", "SecurityException getting location: ${e.message}")
                null
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error getting location: ${e.message}")
                null
            }

            val weather = if (location != null) {
                weatherRepository.getWeather(lat = location.latitude, lon = location.longitude)
            } else {
                weatherRepository.getWeather()
            }

            if (weather != null) {
                _weather.value = weather
                _recommendedActivity.value = getRecommendedActivity(
                    weather = weather,
                    level = _userLevel.value,
                    streak = _streak.value
                )
            }
            _isWeatherLoading.value = false
        }
    }

    // ================================ FUNZIONI PRIVATE ================================

    /**
     * Osserva i dati utente e aggiorna lo stato.
     */
    private fun observeUserData() {
        viewModelScope.launch {
            repository.checkStreakExpiry()
            repository.checkXpBoostExpiry()
            fetchWeather()

            repository.getAllPreferences().collect { prefs ->
                val newAvatar = prefs[UserPreferencesManager.SELECTED_AVATAR] ?: "🧙"
                val newLevel = prefs[UserPreferencesManager.ACTIVITY_LEVEL] ?: "Base"
                val newGoal = prefs[UserPreferencesManager.FITNESS_GOAL] ?: "Mantenimento"
                val newDailyGoal = prefs[UserPreferencesManager.DAILY_GOAL] ?: "Nessun obiettivo"
                val newXp = prefs[UserPreferencesManager.USER_XP] ?: 0
                val newUserLevel = prefs[UserPreferencesManager.USER_LEVEL] ?: 1
                val newStreak = prefs[UserPreferencesManager.DAILY_STREAK] ?: 0
                val xpBoostActive = prefs[UserPreferencesManager.XP_BOOST_ACTIVE] ?: false
                val xpBoostMultiplier = prefs[UserPreferencesManager.XP_BOOST_MULTIPLIER] ?: 1.0f

                _avatar.value = newAvatar
                _level.value = newLevel
                _goal.value = newGoal
                _dailyGoal.value = newDailyGoal
                _xp.value = newXp
                _userLevel.value = newUserLevel
                _streak.value = newStreak
                _xpBoostActive.value = xpBoostActive
                _xpBoostMultiplier.value = xpBoostMultiplier

                if (newUserLevel != previousLevel ||
                    newDailyGoal != previousDailyGoal ||
                    newLevel != previousActivityLevel ||
                    newGoal != previousFitnessGoal) {
                    dailyQuestLoaded = false
                    previousLevel = newUserLevel
                    previousDailyGoal = newDailyGoal
                    previousActivityLevel = newLevel
                    previousFitnessGoal = newGoal
                }

                val completedQuests = prefs[UserPreferencesManager.COMPLETED_QUESTS] ?: emptySet()
                val unlockedBadges = prefs[UserPreferencesManager.UNLOCKED_BADGES] ?: emptySet()
                val newBadges = checkBadges(
                    xp = newXp,
                    level = newUserLevel,
                    completedQuests = completedQuests.size,
                    streak = newStreak,
                    unlockedBadges = unlockedBadges
                )
                newBadges.forEach { badge -> repository.unlockBadge(badge.id) }
                repository.checkAndUnlockChapters()

                val isRunning = prefs[UserPreferencesManager.MISSION_IS_RUNNING] ?: false
                handleMissionState(isRunning)

                if ((_missionState.value == MissionState.IDLE || _missionState.value == MissionState.WAITING) && !dailyQuestLoaded) {
                    loadDailyQuest()
                    dailyQuestLoaded = true
                }

                _isLoading.value = false
                loadMoodData()
                updateRewardStatus()
            }
        }
    }

    /**
     * Gestisce lo stato della missione in base al flag di running.
     */
    private suspend fun handleMissionState(isRunning: Boolean) {
        if (isRunning && _missionState.value != MissionState.RUNNING) {
            val remaining = repository.getMissionTimerRemaining().first()
            if (remaining > 0) {
                _missionState.value = MissionState.RUNNING
                _timerRemaining.value = remaining
                _questTitle.value = "🏃‍♂️ Esercizio in corso..."
                loadDailyQuest()
                startMissionTimer()
            } else {
                _missionState.value = MissionState.COMPLETED
                _questTitle.value = "✅ Missione completata! 🎉"
                repository.completeMission()
            }
        } else if (!isRunning && _missionState.value == MissionState.RUNNING) {
            _missionState.value = MissionState.IDLE
            dailyQuestLoaded = false
            loadDailyQuest()
        }

        if (_missionState.value == MissionState.IDLE || _missionState.value == MissionState.WAITING) {
            if (_questTitle.value == "Caricamento missione..." || _questTitle.value == "Nessuna missione disponibile") {
                loadDailyQuest()
            }
            if (_missionState.value == MissionState.IDLE) {
                startNewQuest()
            }
        }
    }

    /**
     * Carica la missione attuale e l'obiettivo personalizzato.
     */
    private fun loadDailyQuest() {
        viewModelScope.launch {
            val level = repository.getLevel().first()
            val dailyGoal = _dailyGoal.value
            val quests = repository.getDailyQuests(
                level = level,
                dailyGoal = dailyGoal
            )

            // Carica obiettivo giornaliero
            val goalQuest = quests.firstOrNull { it.id.startsWith("q_daily_goal") }
            _dailyGoalQuest.value = goalQuest

            if (goalQuest != null) {
                val target = goalQuest.targetProgress
                _dailyGoalTarget.value = target

                val savedProgress = repository.getQuestProgress(goalQuest.id).first()
                _dailyGoalProgress.value = savedProgress

                // Se l'obiettivo è a durata, gestisci il timer
                if (goalQuest.duration > 0 && target == 1) {
                    val isGoalRunning = repository.getMissionTimerRemaining().first() > 0
                    if (isGoalRunning) {
                        _goalMissionState.value = MissionState.RUNNING
                        _goalTimerRemaining.value = repository.getMissionTimerRemaining().first()
                        startGoalTimer()
                    } else {
                        _goalMissionState.value = MissionState.WAITING
                        _goalTimerRemaining.value = 1800
                    }
                }
            }

            // Carica missione principale (casuale, escluso l'obiettivo)
            val quest = quests.firstOrNull { !it.id.startsWith("q_daily_goal") }
            if (quest != null) {
                _questTitle.value = quest.title
                _questDuration.value = quest.duration
                _currentQuestTarget.value = quest.targetProgress

                val shouldTrackSteps = (quest.progressUnit == "passi" || quest.id == "q_daily_goal_steps") ||
                        (goalQuest != null && _dailyGoalTarget.value > 0 && dailyGoal.contains("passi", ignoreCase = true))

                if (shouldTrackSteps) {
                    startStepTracking(quest, _dailyGoalTarget.value)
                } else {
                    stopStepTracking()
                }
                dailyQuestLoaded = true
            } else {
                _questTitle.value = "Nessuna missione disponibile"
                _questDuration.value = 0
                _currentQuestTarget.value = 0
                stopStepTracking()
                dailyQuestLoaded = false
            }
        }
    }

    /**
     * Avvia il tracking dei passi.
     */
    private fun startStepTracking(quest: Quest, goalTarget: Int) {
        if (!hasActivityPermission()) {
            _permissionError.value = "⚠️ Permesso attività fisica richiesto per tracciare i passi"
            return
        }
        _permissionError.value = null

        StepCounterManager.startListening()

        stepTrackingJob?.cancel()
        stepTrackingJob = viewModelScope.launch {
            _todaySteps.value = StepCounterManager.stepsToday.value

            StepCounterManager.stepsToday.collect { steps ->
                _todaySteps.value = steps

                if (quest.progressUnit == "passi" || quest.id == "q_daily_goal_steps") {
                    val progress = minOf(steps, quest.targetProgress)
                    repository.setQuestProgress(quest.id, progress)

                    if (steps >= quest.targetProgress) {
                        repository.completeQuest(quest.id)
                        val addedXp = repository.addXp(quest.xpReward)
                        repository.updateStreak()
                        _streak.value = repository.getStreak().first()
                        _xp.value = repository.getXp().first()
                        _userLevel.value = repository.getLevel().first()
                        repository.syncUserData()
                        repository.checkAndUnlockChapters()
                        showFeedback("🎉 Missione passi completata! +$addedXp XP")
                        repository.setQuestProgress(quest.id, 0)
                        stopStepTracking()
                        dailyQuestLoaded = false
                        loadDailyQuest()
                    }
                }
            }
        }
    }

    /**
     * Ferma il tracking dei passi.
     */
    private fun stopStepTracking() {
        stepTrackingJob?.cancel()
        StepCounterManager.stopListening()
    }

    /**
     * Avvia il timer della missione principale.
     */
    private fun startMissionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var seconds = _timerRemaining.value
            while (seconds > 0 && _missionState.value == MissionState.RUNNING) {
                delay(1000L)
                seconds--
                _timerRemaining.value = seconds
            }

            if (seconds <= 0 && _missionState.value == MissionState.RUNNING) {
                _missionState.value = MissionState.COMPLETED
                _questTitle.value = "✅ Missione completata! 🎉"
                repository.completeMission()
                val addedXp = repository.addXp(20)
                _streak.value = repository.updateStreak()
                _xp.value = repository.getXp().first()
                _userLevel.value = repository.getLevel().first()
                repository.syncUserData()
                notificationHelper.sendMissionCompletedNotification(
                    questTitle = _questTitle.value,
                    xpGained = addedXp
                )
                showFeedback("🎉 Missione completata! +$addedXp XP")
            }
        }
    }

    /**
     * Avvia il timer di reminder per la missione in attesa.
     */
    private fun startReminderTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var seconds = repository.getTimerSeconds().first()
            if (seconds <= 0) seconds = 1800
            _timerRemaining.value = seconds

            var reminderSent = false

            while (seconds > 0 && _missionState.value == MissionState.WAITING) {
                delay(1000L)
                seconds--
                _timerRemaining.value = seconds

                if (seconds == 300 && !reminderSent) {
                    notificationHelper.sendReminderNotification(_questTitle.value)
                    reminderSent = true
                }

                if (seconds % 5 == 0) {
                    repository.saveTimerSeconds(seconds)
                }
            }

            if (seconds <= 0 && _missionState.value == MissionState.WAITING) {
                _missionState.value = MissionState.IDLE
                _questTitle.value = "⏰ Tempo scaduto! Torna domani."
                repository.saveTimerSeconds(0)
            }
        }
    }

    /**
     * Avvia il timer per l'obiettivo a durata.
     */
    private fun startGoalTimer() {
        goalTimerJob?.cancel()
        goalTimerJob = viewModelScope.launch {
            var seconds = _goalTimerRemaining.value
            while (seconds > 0 && _goalMissionState.value == MissionState.RUNNING) {
                delay(1000L)
                seconds--
                _goalTimerRemaining.value = seconds
            }

            if (seconds <= 0 && _goalMissionState.value == MissionState.RUNNING) {
                _goalMissionState.value = MissionState.COMPLETED
                completeDailyGoalInternal()
            }
        }
    }

    /**
     * Completa l'obiettivo giornaliero (interno).
     */
    private suspend fun completeDailyGoalInternal() {
        val quest = _dailyGoalQuest.value ?: return

        repository.completeQuest(quest.id)
        val addedXp = repository.addXp(quest.xpReward)
        repository.updateStreak()
        _streak.value = repository.getStreak().first()
        _xp.value = repository.getXp().first()
        _userLevel.value = repository.getLevel().first()
        repository.syncUserData()
        repository.checkAndUnlockChapters()

        showFeedback("🎯 Obiettivo completato! +$addedXp XP")

        repository.setQuestProgress(quest.id, 0)
        _dailyGoalProgress.value = 0
        _goalMissionState.value = MissionState.IDLE
        _goalTimerRemaining.value = 0

        dailyQuestLoaded = false
        loadDailyQuest()
    }

    /**
     * Carica i dati del diario delle emozioni.
     */
    private fun loadMoodData() {
        viewModelScope.launch {
            _isMoodLoading.value = true
            _todayMood.value = repository.getTodayMood()
            _moodHistory.value = repository.getMoodHistory(7)
            _averageMood.value = repository.getAverageMood(7)
            _isMoodLoading.value = false
        }
    }

    /**
     * Aggiorna lo stato della ricompensa settimanale.
     */
    private fun updateRewardStatus() {
        viewModelScope.launch {
            _canClaimReward.value = repository.canClaimReward()
            _claimStreak.value = repository.getClaimStreak().first()
            _claimDay.value = repository.getCurrentClaimDay()
        }
    }

    /**
     * Verifica se l'utente ha il permesso di localizzazione.
     */
    private fun hasLocationPermission(): Boolean {
        val context = repository.getContext()
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Verifica se l'utente ha il permesso di attività fisica.
     */
    private fun hasActivityPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        val context = repository.getContext()
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Restituisce un'attività consigliata in base al meteo.
     */
    private fun getRecommendedActivity(weather: Weather, level: Int, streak: Int): String {
        val description = weather.description.lowercase()
        val temp = weather.temperature
        val cappedLevel = level.coerceAtMost(10)

        return when {
            description.contains("rain") || description.contains("thunderstorm") || description.contains("drizzle") -> {
                listOf(
                    "🧘 Meditazione guidata (10 min)",
                    "🏋️ Allenamento a corpo libero (15 min)",
                    "🧘 Stretching dinamico (10 min)",
                    "💪 Esercizi con bande elastiche (12 min)"
                ).random()
            }
            temp > 30 -> {
                listOf(
                    "💧 Idratazione e stretching leggero (10 min)",
                    "🧘 Yoga rinfrescante (15 min)",
                    "🚶 Passeggiata all'ombra (15 min)"
                ).random()
            }
            temp < 5 -> {
                listOf(
                    "🏃 Salta la corda (10 min)",
                    "🧘 Yoga al caldo (20 min)",
                    "💪 Circuito di forza (15 min)"
                ).random()
            }
            else -> {
                val outdoorActivities = listOf(
                    "🚶 Camminata veloce (${15 + cappedLevel * 5} min)",
                    "🏃 Corsa leggera (${10 + cappedLevel * 3} min)",
                    "🚴 Giro in bici (${20 + cappedLevel * 5} min)",
                    "🧘 Yoga all'aperto (15 min)"
                )
                val baseIndex = if (streak > 10) 1 else 0
                outdoorActivities[(baseIndex + (cappedLevel % 2)) % outdoorActivities.size]
            }
        }
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        goalTimerJob?.cancel()
        stopStepTracking()
        viewModelScope.launch {
            if (_missionState.value == MissionState.WAITING) {
                repository.saveTimerSeconds(_timerRemaining.value)
            }
            if (_goalMissionState.value == MissionState.WAITING) {
                repository.saveTimerSeconds(_goalTimerRemaining.value)
            }
        }
    }

    companion object {
        fun factory(repository: CaresRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                        return HomeViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

// ================================ HOME SCREEN (UI) ================================

/**
 * Schermata principale dell'applicazione.
 *
 * **Sezioni UI:**
 * 1. Avatar e streak
 * 2. Obiettivo giornaliero (con controlli step/durata)
 * 3. Meteo e attività consigliata
 * 4. Diario delle emozioni
 * 5. Ricompensa settimanale
 * 6. Missione attuale (con timer e controlli)
 * 7. Barra di progresso XP
 *
 * **Adattabilità:**
 * - Supporta orientamento verticale (portrait) e orizzontale (landscape).
 * - Si adatta al tema scuro/chiaro.
 *
 * @param viewModel ViewModel per la gestione della logica.
 * @param isDarkTheme Se `true`, applica il tema scuro.
 */
@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("DefaultLocale")
@Composable
fun HomeScreen(viewModel: HomeViewModel, isDarkTheme: Boolean = true) {
    // ================================ STATI ================================

    val avatar by viewModel.avatar.collectAsState()
    val level by viewModel.level.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val xp by viewModel.xp.collectAsState()
    val userLevel by viewModel.userLevel.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val missionState by viewModel.missionState.collectAsState()
    val timerRemaining by viewModel.timerRemaining.collectAsState()
    val questTitle by viewModel.questTitle.collectAsState()
    val questDuration by viewModel.questDuration.collectAsState()
    val permissionError by viewModel.permissionError.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val recommendedActivity by viewModel.recommendedActivity.collectAsState()
    val isWeatherLoading by viewModel.isWeatherLoading.collectAsState()
    val xpBoostActive by viewModel.xpBoostActive.collectAsState()
    val xpBoostMultiplier by viewModel.xpBoostMultiplier.collectAsState()
    val todayMood by viewModel.todayMood.collectAsState()
    val moodHistory by viewModel.moodHistory.collectAsState()
    val averageMood by viewModel.averageMood.collectAsState()
    val isMoodLoading by viewModel.isMoodLoading.collectAsState()
    val canClaimReward by viewModel.canClaimReward.collectAsState()
    val claimStreak by viewModel.claimStreak.collectAsState()
    val claimDay by viewModel.claimDay.collectAsState()
    val isClaiming by viewModel.isClaiming.collectAsState()
    val todaySteps by viewModel.todaySteps.collectAsState()
    val currentQuestTarget by viewModel.currentQuestTarget.collectAsState()
    val dailyGoalTarget by viewModel.dailyGoalTarget.collectAsState()

    // Nuovi stati per l'obiettivo
    val dailyGoalQuest by viewModel.dailyGoalQuest.collectAsState()
    val dailyGoalProgress by viewModel.dailyGoalProgress.collectAsState()
    val goalMissionState by viewModel.goalMissionState.collectAsState()
    val goalTimerRemaining by viewModel.goalTimerRemaining.collectAsState()

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // ================================ EFFETTI ================================

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            viewModel.fetchWeather()
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // ================================ FORMATTAZIONE ================================

    val timerFormatted = String.format("%02d:%02d", timerRemaining / 60, timerRemaining % 60)
    val goalTimerFormatted = String.format("%02d:%02d", goalTimerRemaining / 60, goalTimerRemaining % 60)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val animatedColor by animateColorAsState(
        targetValue = when (missionState) {
            HomeViewModel.MissionState.WAITING -> Neon.Green
            HomeViewModel.MissionState.RUNNING -> Neon.Cyan
            HomeViewModel.MissionState.COMPLETED -> Neon.Purple
            else -> Neon.Green
        },
        animationSpec = Animations.extraSlowTween(),
        label = "bgColor"
    )

    // ================================ UI ================================

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ShimmerLoader(height = 400.dp)
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Gradients.mainBackground(isDarkTheme))
        ) {
            // Sfondo decorativo animato
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(animatedColor.copy(alpha = 0.15f), Color.Transparent),
                            radius = 400f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomStart)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Neon.Cyan.copy(alpha = 0.1f), Color.Transparent),
                            radius = 300f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLandscape) {
                    LandscapeHomeContent(
                        avatar = avatar,
                        goal = goal,
                        dailyGoal = dailyGoal,
                        dailyGoalTarget = dailyGoalTarget,
                        xp = xp,
                        userLevel = userLevel,
                        streak = streak,
                        todaySteps = todaySteps,
                        questTarget = currentQuestTarget,
                        missionState = missionState,
                        timerRemaining = timerRemaining,
                        timerFormatted = timerFormatted,
                        questTitle = questTitle,
                        questDuration = questDuration,
                        permissionError = permissionError,
                        isDarkTheme = isDarkTheme,
                        onStartMission = { viewModel.startMission() },
                        onCompleteManual = { viewModel.completeMissionManually() },
                        onNewQuest = { viewModel.startNewQuest() },
                        onDismissError = { viewModel.dismissPermissionError() },
                        weather = weather,
                        recommendedActivity = recommendedActivity,
                        isWeatherLoading = isWeatherLoading,
                        xpBoostActive = xpBoostActive,
                        xpBoostMultiplier = xpBoostMultiplier,
                        todayMood = todayMood,
                        moodHistory = moodHistory,
                        averageMood = averageMood,
                        isMoodLoading = isMoodLoading,
                        onMoodSelected = { viewModel.saveMood(it) },
                        canClaimReward = canClaimReward,
                        claimStreak = claimStreak,
                        currentDay = claimDay,
                        isClaiming = isClaiming,
                        onClaimReward = { viewModel.claimDailyReward() },
                        dailyGoalQuest = dailyGoalQuest,
                        dailyGoalProgress = dailyGoalProgress,
                        goalMissionState = goalMissionState,
                        goalTimerRemaining = goalTimerRemaining,
                        goalTimerFormatted = goalTimerFormatted,
                        onStartDailyGoal = { viewModel.startDailyGoalMission() },
                        onCompleteDailyGoal = { viewModel.completeDailyGoalManually() },
                        onRegisterDailyGoal = { viewModel.registerDailyGoalProgress() }
                    )
                } else {
                    PortraitHomeContent(
                        avatar = avatar,
                        dailyGoal = dailyGoal,
                        dailyGoalTarget = dailyGoalTarget,
                        xp = xp,
                        userLevel = userLevel,
                        streak = streak,
                        todaySteps = todaySteps,
                        questTarget = currentQuestTarget,
                        missionState = missionState,
                        timerRemaining = timerRemaining,
                        timerFormatted = timerFormatted,
                        questTitle = questTitle,
                        questDuration = questDuration,
                        isDarkTheme = isDarkTheme,
                        onStartMission = { viewModel.startMission() },
                        onCompleteManual = { viewModel.completeMissionManually() },
                        onNewQuest = { viewModel.startNewQuest() },
                        weather = weather,
                        recommendedActivity = recommendedActivity,
                        isWeatherLoading = isWeatherLoading,
                        xpBoostActive = xpBoostActive,
                        xpBoostMultiplier = xpBoostMultiplier,
                        todayMood = todayMood,
                        moodHistory = moodHistory,
                        averageMood = averageMood,
                        isMoodLoading = isMoodLoading,
                        onMoodSelected = { viewModel.saveMood(it) },
                        canClaimReward = canClaimReward,
                        claimStreak = claimStreak,
                        currentDay = claimDay,
                        isClaiming = isClaiming,
                        onClaimReward = { viewModel.claimDailyReward() },
                        dailyGoalQuest = dailyGoalQuest,
                        dailyGoalProgress = dailyGoalProgress,
                        goalMissionState = goalMissionState,
                        goalTimerRemaining = goalTimerRemaining,
                        goalTimerFormatted = goalTimerFormatted,
                        onStartDailyGoal = { viewModel.startDailyGoalMission() },
                        onCompleteDailyGoal = { viewModel.completeDailyGoalManually() },
                        onRegisterDailyGoal = { viewModel.registerDailyGoalProgress() }
                    )
                }
            }
        }

        // Dialog di errore permessi
        if (permissionError != null) {
            val context = androidx.compose.ui.platform.LocalContext.current
            AlertDialog(
                onDismissRequest = { viewModel.dismissPermissionError() },
                title = { Text("Permesso necessario") },
                text = { Text(permissionError!!) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissPermissionError() }) {
                        Text("Ok")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                            viewModel.dismissPermissionError()
                        }
                    ) {
                        Text("Apri impostazioni")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ================================ PORTRAIT HOME CONTENT ================================

/**
 * Contenuto della Home in orientamento verticale (portrait).
 *
 * Organizza le card in una colonna scrollabile con spaziatura ottimale.
 */
@SuppressLint("DefaultLocale")
@Composable
fun PortraitHomeContent(
    avatar: String,
    dailyGoal: String,
    dailyGoalTarget: Int,
    xp: Int,
    userLevel: Int,
    streak: Int,
    todaySteps: Int,
    questTarget: Int,
    missionState: HomeViewModel.MissionState,
    timerRemaining: Int,
    timerFormatted: String,
    questTitle: String,
    questDuration: Int,
    isDarkTheme: Boolean,
    onStartMission: () -> Unit,
    onCompleteManual: () -> Unit,
    onNewQuest: () -> Unit,
    weather: Weather?,
    recommendedActivity: String,
    isWeatherLoading: Boolean,
    xpBoostActive: Boolean,
    xpBoostMultiplier: Float,
    todayMood: Int?,
    moodHistory: Map<String, Int>,
    averageMood: Float,
    isMoodLoading: Boolean,
    onMoodSelected: (Int) -> Unit,
    canClaimReward: Boolean,
    claimStreak: Int,
    currentDay: Int,
    isClaiming: Boolean,
    onClaimReward: () -> Unit,
    // Nuovi parametri obiettivo
    dailyGoalQuest: Quest?,
    dailyGoalProgress: Int,
    goalMissionState: HomeViewModel.MissionState,
    goalTimerRemaining: Int,
    goalTimerFormatted: String,
    onStartDailyGoal: () -> Unit,
    onCompleteDailyGoal: () -> Unit,
    onRegisterDailyGoal: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ==================== AVATAR E STREAK ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .scaleOnPress()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Neon.Green.copy(alpha = 0.3f), Color.Transparent),
                            radius = 40f
                        )
                    )
            ) {
                Text(text = avatar, fontSize = 32.sp, modifier = Modifier.align(Alignment.Center))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🔥", fontSize = 18.sp)
                Text(
                    streak.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Neon.Orange
                )
                Text("giorni", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }

        // ==================== OBIETTIVO GIORNALIERO ====================
        if (dailyGoal.isNotBlank() && dailyGoal != "Nessun obiettivo") {
            AnimatedEntrance(delay = 50) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    darkTheme = isDarkTheme
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 Obiettivo giornaliero",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = dailyGoal,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Neon.Green
                            )
                        }

                        val quest = dailyGoalQuest

                        // ---- STEP ----
                        if (quest != null && quest.targetProgress > 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = quest.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Progresso:",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "$dailyGoalProgress / ${quest.targetProgress}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dailyGoalProgress >= quest.targetProgress) Neon.Green else Neon.Cyan
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (dailyGoalProgress.toFloat() / quest.targetProgress).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (dailyGoalProgress >= quest.targetProgress) Neon.Green else Neon.Cyan,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (dailyGoalProgress < quest.targetProgress) {
                                GlassButton(
                                    text = if (quest.progressUnit == "passi") "👣 Registra passi" else "➕ Registra",
                                    onClick = onRegisterDailyGoal,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Neon.Green
                                )
                            } else {
                                Text(
                                    text = "✅ Obiettivo completato! 🎉",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Neon.Green,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }

                        // ---- DURATA ----
                        if (quest != null && quest.duration > 0 && quest.targetProgress == 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = quest.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val timerColor = when {
                                goalTimerRemaining > 300 -> Neon.Green
                                goalTimerRemaining > 60 -> Neon.Orange
                                else -> Color(0xFFFF1744)
                            }

                            Text(
                                text = "⏱️ $goalTimerFormatted",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = timerColor,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            when (goalMissionState) {
                                HomeViewModel.MissionState.WAITING -> {
                                    GlassButton(
                                        text = "Inizia",
                                        onClick = onStartDailyGoal,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Neon.Green
                                    )
                                }
                                HomeViewModel.MissionState.RUNNING -> {
                                    GlassButton(
                                        text = "✅ Completa",
                                        onClick = onCompleteDailyGoal,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Neon.Orange
                                    )
                                }
                                HomeViewModel.MissionState.COMPLETED, HomeViewModel.MissionState.IDLE -> {
                                    if (goalMissionState == HomeViewModel.MissionState.COMPLETED) {
                                        Text(
                                            text = "✅ Obiettivo completato! 🎉",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Neon.Green,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    } else {
                                        GlassButton(
                                            text = "🔄 Ricomincia",
                                            onClick = onStartDailyGoal,
                                            modifier = Modifier.fillMaxWidth(),
                                            color = Neon.Cyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==================== METEO ====================
        AnimatedEntrance(delay = 100) {
            WeatherCard(
                weather = weather,
                isWeatherLoading = isWeatherLoading,
                recommendedActivity = recommendedActivity,
                isDarkTheme = isDarkTheme
            )
        }

        // ==================== DIARIO DELLE EMOZIONI ====================
        AnimatedEntrance(delay = 200) {
            MoodDiaryCard(
                todayMood = todayMood,
                moodHistory = moodHistory,
                averageMood = averageMood,
                isMoodLoading = isMoodLoading,
                onMoodSelected = onMoodSelected,
                isDarkTheme = isDarkTheme
            )
        }

        // ==================== RICOMPENSA SETTIMANALE ====================
        AnimatedEntrance(delay = 300) {
            WeeklyRewardCard(
                canClaim = canClaimReward,
                claimStreak = claimStreak,
                currentDay = currentDay,
                isClaiming = isClaiming,
                onClaim = onClaimReward,
                isDarkTheme = isDarkTheme
            )
        }

        // ==================== MISSIONE ATTUALE ====================
        AnimatedEntrance(delay = 400) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                darkTheme = isDarkTheme
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Statistiche compatte
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(icon = "🏆", value = userLevel.toString(), label = "Livello", isCompact = true)
                        StatItem(
                            icon = if (xpBoostActive) "⚡" else "⭐",
                            value = if (xpBoostActive) "$xp (x$xpBoostMultiplier)" else xp.toString(),
                            label = "XP",
                            isCompact = true,
                            valueColor = if (xpBoostActive) Neon.Cyan else MaterialTheme.colorScheme.onSurface
                        )
                        StatItem(icon = "📋", value = questDuration.toString(), label = "min", isCompact = true)
                    }

                    // Titolo missione
                    Text(
                        "⚡ MISSIONE ATTUALE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        questTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Timer e Progress Bar
                    if (missionState != HomeViewModel.MissionState.COMPLETED) {
                        val timerColor = when {
                            timerRemaining > 300 -> Neon.Green
                            timerRemaining > 60 -> Neon.Orange
                            else -> Color(0xFFFF1744)
                        }
                        val timerScale by animateFloatAsState(
                            targetValue = if (timerRemaining <= 10 && timerRemaining > 0) 1.05f else 1f,
                            animationSpec = repeatable(
                                iterations = Integer.MAX_VALUE,
                                animation = Animations.slowTween()
                            ),
                            label = "timerPulse"
                        )
                        val timerEmoji = when {
                            timerRemaining > 300 -> "⏳"
                            timerRemaining > 60 -> "⏰"
                            timerRemaining > 10 -> "⌛"
                            else -> "🔥"
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "$timerEmoji $timerFormatted",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = timerColor,
                            modifier = Modifier.scale(timerScale)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val timerProgress = timerRemaining.toFloat() / 1800f
                        LinearProgressIndicator(
                            progress = { timerProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = timerColor,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Pulsante azione
                    when (missionState) {
                        HomeViewModel.MissionState.WAITING -> {
                            GlassButton("INIZIA", onStartMission, Modifier.fillMaxWidth())
                        }
                        HomeViewModel.MissionState.RUNNING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassButton("COMPLETA", onCompleteManual, Modifier.weight(1f), Neon.Orange)
                            }
                        }
                        HomeViewModel.MissionState.COMPLETED,
                        HomeViewModel.MissionState.IDLE -> {
                            GlassButton("NUOVA", onNewQuest, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        // ==================== BARRA XP ====================
        AnimatedEntrance(delay = 500) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val xpInCurrentLevel = xp - ((userLevel - 1) * 100)
                val progress = (xpInCurrentLevel.toFloat() / 100).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progresso", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Text("${(progress * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Neon.Green,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                )
            }
        }
    }
}

// ================================ LANDSCAPE HOME CONTENT ================================

/**
 * Contenuto della Home in orientamento orizzontale (landscape).
 *
 * Organizza le card in due colonne per sfruttare lo spazio orizzontale.
 */
@SuppressLint("DefaultLocale")
@Composable
fun LandscapeHomeContent(
    avatar: String,
    goal: String,
    dailyGoal: String,
    dailyGoalTarget: Int,
    xp: Int,
    userLevel: Int,
    streak: Int,
    todaySteps: Int,
    questTarget: Int,
    missionState: HomeViewModel.MissionState,
    timerRemaining: Int,
    timerFormatted: String,
    questTitle: String,
    questDuration: Int,
    permissionError: String?,
    isDarkTheme: Boolean,
    onStartMission: () -> Unit,
    onCompleteManual: () -> Unit,
    onNewQuest: () -> Unit,
    onDismissError: () -> Unit,
    weather: Weather?,
    recommendedActivity: String,
    isWeatherLoading: Boolean,
    xpBoostActive: Boolean,
    xpBoostMultiplier: Float,
    todayMood: Int?,
    moodHistory: Map<String, Int>,
    averageMood: Float,
    isMoodLoading: Boolean,
    onMoodSelected: (Int) -> Unit,
    canClaimReward: Boolean,
    claimStreak: Int,
    currentDay: Int,
    isClaiming: Boolean,
    onClaimReward: () -> Unit,
    // Nuovi parametri obiettivo
    dailyGoalQuest: Quest?,
    dailyGoalProgress: Int,
    goalMissionState: HomeViewModel.MissionState,
    goalTimerRemaining: Int,
    goalTimerFormatted: String,
    onStartDailyGoal: () -> Unit,
    onCompleteDailyGoal: () -> Unit,
    onRegisterDailyGoal: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ==================== COLONNA SINISTRA ====================
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .scaleOnPress()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Neon.Green.copy(alpha = 0.3f), Color.Transparent),
                            radius = 50f
                        )
                    )
            ) {
                Text(text = avatar, fontSize = 38.sp, modifier = Modifier.align(Alignment.Center))
            }

            // Streak
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 16.sp)
                Text(streak.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Neon.Orange)
                Text(" giorni", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }

            // Statistiche
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("🏆", userLevel.toString(), "Livello", isCompact = true)
                StatItem(
                    icon = if (xpBoostActive) "⚡" else "⭐",
                    value = if (xpBoostActive) "$xp (x$xpBoostMultiplier)" else xp.toString(),
                    label = "XP",
                    isCompact = true,
                    valueColor = if (xpBoostActive) Neon.Cyan else MaterialTheme.colorScheme.onSurface
                )
            }

            // Obiettivo giornaliero compatto
            if (dailyGoal.isNotBlank() && dailyGoal != "Nessun obiettivo") {
                val quest = dailyGoalQuest
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(
                            if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯", fontSize = 16.sp)
                        Text(
                            text = dailyGoal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Neon.Green,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    if (quest != null && quest.targetProgress > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$dailyGoalProgress / ${quest.targetProgress}",
                                fontSize = 11.sp,
                                color = if (dailyGoalProgress >= quest.targetProgress) Neon.Green else Neon.Cyan
                            )
                            LinearProgressIndicator(
                                progress = { (dailyGoalProgress.toFloat() / quest.targetProgress).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (dailyGoalProgress >= quest.targetProgress) Neon.Green else Neon.Cyan,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            if (dailyGoalProgress < quest.targetProgress) {
                                Text(
                                    text = "➕",
                                    fontSize = 16.sp,
                                    modifier = Modifier.clickable { onRegisterDailyGoal() }
                                )
                            } else {
                                Text("✅", fontSize = 16.sp)
                            }
                        }
                    }

                    if (quest != null && quest.duration > 0 && quest.targetProgress == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱️ $goalTimerFormatted",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    goalTimerRemaining > 300 -> Neon.Green
                                    goalTimerRemaining > 60 -> Neon.Orange
                                    else -> Color(0xFFFF1744)
                                }
                            )
                            when (goalMissionState) {
                                HomeViewModel.MissionState.WAITING -> {
                                    Text(
                                        text = "▶️",
                                        fontSize = 18.sp,
                                        modifier = Modifier.clickable { onStartDailyGoal() }
                                    )
                                }
                                HomeViewModel.MissionState.RUNNING -> {
                                    Text(
                                        text = "⏹️",
                                        fontSize = 18.sp,
                                        modifier = Modifier.clickable { onCompleteDailyGoal() }
                                    )
                                }
                                else -> {
                                    if (goalMissionState == HomeViewModel.MissionState.COMPLETED) {
                                        Text("✅", fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Meteo compatto
            if (!isWeatherLoading && weather != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(
                            if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(getWeatherEmoji(weather), fontSize = 24.sp)
                    Text(
                        "${weather.temperature.toInt()}°C",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        weather.city,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (recommendedActivity.isNotEmpty()) {
                        Text(
                            "💡 $recommendedActivity",
                            fontSize = 11.sp,
                            color = Neon.Green,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis                        )
                    }
                }
            }

            // Diario emozioni compatto
            if (!isMoodLoading && todayMood != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(
                            if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💭 Oggi:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(getMoodEmoji(todayMood), fontSize = 28.sp)
                    if (averageMood > 0) {
                        Text(
                            "${String.format("%.1f", averageMood)}/5",
                            fontSize = 12.sp,
                            color = when {
                                averageMood >= 4 -> Neon.Green
                                averageMood >= 3 -> Neon.Orange
                                else -> Color(0xFFFF1744)
                            }
                        )
                    }
                }
            }

            // Ricompensa settimanale compatta
            if (!isClaiming) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(
                            if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎁", fontSize = 20.sp)
                    Text(
                        text = if (canClaimReward) "📥 Ritira!" else "✅ Fatto!",
                        fontSize = 12.sp,
                        color = if (canClaimReward) Neon.Green else Neon.Cyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { if (canClaimReward) onClaimReward() }
                    )
                    if (claimStreak > 0) {
                        Text("🔥 $claimStreak", fontSize = 12.sp, color = Neon.Orange)
                    }
                }
            }
        }

        // ==================== COLONNA DESTRA (MISSIONE ATTUALE) ====================
        GlassCard(
            modifier = Modifier
                .weight(1.5f)
                .padding(vertical = 4.dp),
            darkTheme = isDarkTheme
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "MISSIONE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp
                )
                Text(
                    questTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                if (missionState != HomeViewModel.MissionState.COMPLETED) {
                    val timerColor = when {
                        timerRemaining > 300 -> Neon.Green
                        timerRemaining > 60 -> Neon.Orange
                        else -> Color(0xFFFF1744)
                    }
                    val timerScale by animateFloatAsState(
                        targetValue = if (timerRemaining <= 10 && timerRemaining > 0) 1.05f else 1f,
                        animationSpec = repeatable(
                            iterations = Integer.MAX_VALUE,
                            animation = tween(500, easing = FastOutSlowInEasing)
                        ),
                        label = "timerPulse"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = timerFormatted,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = timerColor,
                        modifier = Modifier.scale(timerScale)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val timerProgress = timerRemaining.toFloat() / 1800f
                    LinearProgressIndicator(
                        progress = { timerProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = timerColor,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                when (missionState) {
                    HomeViewModel.MissionState.WAITING -> {
                        GlassButton("▶INIZIA", onStartMission, Modifier.fillMaxWidth())
                    }
                    HomeViewModel.MissionState.RUNNING -> {
                        GlassButton("COMPLETA", onCompleteManual, Modifier.fillMaxWidth(), Neon.Orange)
                    }
                    HomeViewModel.MissionState.COMPLETED,
                    HomeViewModel.MissionState.IDLE -> {
                        GlassButton("NUOVA", onNewQuest, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

// ================================ FUNZIONI DI SUPPORTO ================================

/**
 * Restituisce un'emoji in base alle condizioni meteo.
 */
fun getWeatherEmoji(weather: Weather): String {
    val description = weather.description.lowercase()
    return when {
        description.contains("clear") -> "☀️"
        description.contains("cloud") -> "☁️"
        description.contains("rain") -> "🌧️"
        description.contains("thunderstorm") -> "⛈️"
        description.contains("snow") -> "❄️"
        description.contains("mist") || description.contains("fog") -> "🌫️"
        description.contains("drizzle") -> "🌦️"
        else -> "🌈"
    }
}

/**
 * Restituisce un'emoji in base al valore dell'umore.
 */
private fun getMoodEmoji(mood: Int): String = when (mood) {
    1 -> "😢"
    2 -> "😐"
    3 -> "🙂"
    4 -> "😊"
    5 -> "🤩"
    else -> "❓"
}

/**
 * Card per il diario delle emozioni.
 */
@Composable
fun MoodDiaryCard(
    todayMood: Int?,
    moodHistory: Map<String, Int>,
    averageMood: Float,
    isMoodLoading: Boolean,
    onMoodSelected: (Int) -> Unit,
    isDarkTheme: Boolean
) {
    val moods = listOf(
        1 to "😢",
        2 to "😐",
        3 to "🙂",
        4 to "😊",
        5 to "🤩"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        darkTheme = isDarkTheme
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💭 Come ti senti oggi?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (todayMood != null) {
                    Text(
                        text = "✅ Registrato",
                        fontSize = 11.sp,
                        color = Neon.Green
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isMoodLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shimmer(shape = RoundedCornerShape(12.dp))
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    moods.forEach { (value, emoji) ->
                        val isSelected = todayMood == value
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .scaleOnPress()
                                .clickable { onMoodSelected(value) }
                                .background(
                                    if (isSelected) Neon.Green.copy(alpha = 0.25f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Neon.Green else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                                modifier = Modifier.scale(if (isSelected) 1.1f else 1f)
                            )
                        }
                    }
                }

                if (averageMood > 0) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Media ultimi 7 giorni:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${String.format("%.1f", averageMood)} / 5",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                averageMood >= 4 -> Neon.Green
                                averageMood >= 3 -> Neon.Orange
                                else -> Color(0xFFFF1744)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (i in 6 downTo 0) {
                            val date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                            val dateString = dateFormat.format(date.time)
                            val mood = moodHistory[dateString]
                            val dayName = when (date.get(Calendar.DAY_OF_WEEK)) {
                                Calendar.MONDAY -> "Lun"
                                Calendar.TUESDAY -> "Mar"
                                Calendar.WEDNESDAY -> "Mer"
                                Calendar.THURSDAY -> "Gio"
                                Calendar.FRIDAY -> "Ven"
                                Calendar.SATURDAY -> "Sab"
                                Calendar.SUNDAY -> "Dom"
                                else -> "?"
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (mood != null) getMoodEmoji(mood) else "⬜",
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = dayName,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Seleziona il tuo umore per iniziare a tracciare il trend!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Card per la ricompensa settimanale.
 */
@Composable
fun WeeklyRewardCard(
    canClaim: Boolean,
    claimStreak: Int,
    currentDay: Int,
    isClaiming: Boolean,
    onClaim: () -> Unit,
    isDarkTheme: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        darkTheme = isDarkTheme
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎁 Ricompensa settimanale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Griglia 7 giorni
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (day in 1..7) {
                    val isReclaimed = day <= claimStreak
                    val isToday = day == currentDay && canClaim
                    val isLocked = day > claimStreak && !isToday

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = when {
                                        isReclaimed -> Neon.Green.copy(alpha = 0.3f)
                                        isToday -> Neon.Cyan.copy(alpha = 0.3f)
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    },
                                    shape = CircleShape
                                )
                                .border(
                                    width = if (isToday) 2.dp else 1.dp,
                                    color = when {
                                        isReclaimed -> Neon.Green
                                        isToday -> Neon.Cyan
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    isReclaimed -> "✅"
                                    isToday -> "$day"
                                    else -> "🔒"
                                },
                                fontSize = 14.sp,
                                fontWeight = if (isToday || isReclaimed) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isReclaimed -> Neon.Green
                                    isToday -> Neon.Cyan
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )
                        }

                        Text(
                            text = "Day $day",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pulsante Claim
            if (canClaim) {
                Button(
                    onClick = onClaim,
                    enabled = !isClaiming,
                    modifier = Modifier
                        .height(40.dp)
                        .scaleOnPress()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Neon.Green.copy(alpha = 0.3f),
                                    Neon.Green.copy(alpha = 0.1f)
                                )
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Neon.Green.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = null
                ) {
                    if (isClaiming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "📥 Claim Day $currentDay",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                Text(
                    text = if (claimStreak == 7) "🏆 Settimana completata! Torna domani." else "✅ Già reclamato oggi",
                    fontSize = 13.sp,
                    color = Neon.Green,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Accedi ogni giorno per collezionare le ricompense!",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Card per il meteo.
 */
@Composable
fun WeatherCard(
    weather: Weather?,
    isWeatherLoading: Boolean,
    recommendedActivity: String,
    isDarkTheme: Boolean
) {
    if (isWeatherLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .shimmer(shape = RoundedCornerShape(12.dp))
        )
    } else if (weather != null) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            darkTheme = isDarkTheme
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = getWeatherEmoji(weather), fontSize = 32.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${weather.temperature.toInt()}°C",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = weather.city,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Text(
                        text = weather.description.replaceFirstChar { it.uppercase() },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                if (recommendedActivity.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Neon.Green.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Oggi ti consiglio:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = recommendedActivity,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Neon.Green
                        )
                    }
                }
            }
        }
    }
}

/**
 * Item statistiche compatto.
 */
@Composable
fun StatItem(
    icon: String,
    value: String,
    label: String,
    isCompact: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = if (isCompact) 18.sp else 24.sp)
        Text(
            value,
            fontSize = if (isCompact) 15.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            label,
            fontSize = if (isCompact) 10.sp else 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

/**
 * Bottone glassmorphism per azioni.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Neon.Green
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(44.dp)
            .scaleOnPress()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.1f))
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = null
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            letterSpacing = 0.5.sp
        )
    }
}