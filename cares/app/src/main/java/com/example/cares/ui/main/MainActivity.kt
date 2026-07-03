// file: ui/main/MainActivity.kt
package com.example.cares.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cares.data.manager.*
import com.example.cares.data.repository.CaresRepository
import com.example.cares.ui.celebration.StreakCelebrationManager
import com.example.cares.ui.debug.DebugScreen
import com.example.cares.ui.home.HomeScreen
import com.example.cares.ui.home.HomeViewModel
import com.example.cares.ui.leaderboard.LeaderboardScreen
import com.example.cares.ui.onboarding.*
import com.example.cares.ui.profile.ProfileScreen
import com.example.cares.ui.quests.QuestsScreen
import com.example.cares.ui.quests.QuestsViewModel
import com.example.cares.ui.settings.SettingsScreen
import com.example.cares.ui.splash.LoadingScreen
import com.example.cares.ui.splash.SplashScreen
import com.example.cares.ui.story.StoryScreen
import com.example.cares.ui.story.StoryViewModel
import com.example.cares.ui.theme.CaresTheme
import com.example.cares.viewmodel.LeaderboardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.example.cares.ui.celebration.StreakCelebrationScreen
import com.example.cares.ui.friends.AddFriendScreen
import com.example.cares.ui.friends.AddFriendViewModel
import com.example.cares.ui.friends.FriendsScreen
import com.example.cares.ui.friends.FriendsViewModel
import com.example.cares.ui.profile.PublicProfileScreen
import com.example.cares.ui.StreakRiskDialog

// ================================ ENUM PER LA NAVIGAZIONE ================================

/**
 * Enum che rappresenta le schermate del flusso di onboarding.
 */
enum class OnboardingScreen {
    WELCOME,                    // Schermata di benvenuto
    LOGIN,                      // Schermata di login
    REGISTER,                   // Schermata di registrazione
    CHARACTER_CUSTOMIZATION,    // Personalizzazione del personaggio
    GOAL_SELECTION,             // Selezione dell'obiettivo
    PREFERENCES                 // Preferenze finali
}

/**
 * Enum che rappresenta le schermate principali dell'app dopo l'onboarding.
 */
enum class MainScreen(val route: String) {
    HOME("home"),              // Schermata principale
    QUESTS("quests"),          // Schermata delle missioni
    PROFILE("profile"),        // Profilo utente
    LEADERBOARD("leaderboard"), // Classifica
    SETTINGS("settings"),       // Impostazioni
    DEBUG("debug")              // Strumenti di debug
}

// ================================ MAIN ACTIVITY ================================

/**
 * Activity principale dell'applicazione.
 *
 * **Responsabilità:**
 * - Gestire il ciclo di vita dell'app.
 * - Inizializzare il music player.
 * - Gestire la transizione tra le schermate.
 * - Aggiornare lo stato delle barre di sistema.
 *
 * **Ciclo di vita del music player:**
 * - `onCreate`: inizializzazione.
 * - `onResume`: avvio della riproduzione.
 * - `onPause`: pausa della riproduzione.
 * - `onDestroy`: rilascio delle risorse.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MainApp() }

        // Inizializza il music player all'avvio dell'app
        MusicPlayerManager.init(this)
    }

    override fun onResume() {
        super.onResume()
        MusicPlayerManager.start()
    }

    override fun onPause() {
        super.onPause()
        MusicPlayerManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicPlayerManager.stop()
    }

    /**
     * Aggiorna il colore delle icone della barra di stato.
     *
     * @param dark Se `true`, le icone sono scure (tema chiaro), altrimenti chiare (tema scuro).
     */
    fun updateStatusBarIcons(dark: Boolean) {
        val window = window
        val decorView = window.decorView
        val insetsController = WindowCompat.getInsetsController(window, decorView)
        insetsController?.isAppearanceLightStatusBars = !dark
    }
}

// ================================ MAIN APP ================================

/**
 * Componente principale dell'applicazione.
 *
 * **Gestisce:**
 * - Tema (scuro/chiaro) persistente tramite DataStore.
 * - Stato di onboarding.
 * - Schermata di caricamento iniziale.
 * - Navigazione tra le schermate principali.
 *
 * **Flusso di avvio:**
 * 1. SplashScreen → LoadingScreen → MainScreens o OnboardingFlow.
 * 2. Caricamento delle preferenze utente e dello stato di onboarding.
 * 3. Inizializzazione del NotificationHelper e del ReminderScheduler.
 * 4. Login anonimo su Firebase (se non già autenticato).
 */
@Composable
fun MainApp() {
    val context = LocalContext.current
    val repository = remember { CaresRepository(context) }
    val prefsManager = remember { UserPreferencesManager(context) }

    // ================================ STATO ================================
    var isDarkTheme by remember { mutableStateOf(true) }
    var isOnboardingCompleted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showSplash by remember { mutableStateOf(true) }
    var showLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val systemDark = isSystemInDarkTheme()

    // ================================ EFFETTI DI INIZIALIZZAZIONE ================================

    LaunchedEffect(Unit) {
        // Carica il tema salvato o usa quello di sistema
        val savedTheme = prefsManager.getDarkThemePreference().first()
        isDarkTheme = savedTheme ?: systemDark

        // Verifica se l'onboarding è già stato completato
        isOnboardingCompleted = repository.getOnboardingStatus().first()
        isLoading = false

        // Inizializza le notifiche
        val notificationHelper = NotificationHelper(context)
        notificationHelper.createNotificationChannel()
        ReminderScheduler.scheduleReminder(context)

        // Login anonimo su Firebase
        if (FirebaseAuthManager.getCurrentUserId() == null) {
            FirebaseAuthManager.signInAnonymously(context)
        }
    }

    // ================================ FUNZIONI ================================

    /**
     * Alterna il tema scuro/chiaro e salva la preferenza.
     */
    val onToggleTheme: () -> Unit = {
        isDarkTheme = !isDarkTheme
        CoroutineScope(Dispatchers.IO).launch {
            prefsManager.setDarkThemePreference(isDarkTheme)
        }
    }

    // ================================ UI ================================

    CaresTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                // Schermata di splash
                showSplash -> {
                    SplashScreen(
                        onTap = {
                            showSplash = false
                            showLoading = true
                        }
                    )
                }
                // Schermata di caricamento
                showLoading -> {
                    LoadingScreen(
                        onLoadingComplete = {
                            showLoading = false
                        }
                    )
                }
                // Caricamento iniziale dei dati
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Caricamento...")
                    }
                }
                // Contenuto principale
                else -> {
                    if (isOnboardingCompleted) {
                        MainScreens(
                            repository = repository,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = onToggleTheme,
                            onLogout = {
                                FirebaseAuthManager.signOut()
                                scope.launch {
                                    isOnboardingCompleted = false
                                }
                            }
                        )
                    } else {
                        OnboardingFlow(
                            repository = repository,
                            onComplete = { isOnboardingCompleted = true }
                        )
                    }
                }
            }
        }
    }
}

// ================================ ONBOARDING FLOW ================================

/**
 * Flusso di onboarding per i nuovi utenti.
 *
 * **Schermate in sequenza:**
 * 1. WelcomeScreen - Benvenuto
 * 2. LoginScreen - Login o Registrazione
 * 3. RegisterScreen - Registrazione nuovo utente
 * 4. CharacterCustomizationScreen - Scelta avatar
 * 5. GoalSelectionScreen - Scelta obiettivo
 * 6. PreferencesScreen - Impostazioni finali
 *
 * @param repository Repository per salvare le preferenze utente.
 * @param onComplete Callback eseguito al completamento dell'onboarding.
 */
@Composable
fun OnboardingFlow(
    repository: CaresRepository,
    onComplete: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(OnboardingScreen.WELCOME) }
    var selectedAvatar by remember { mutableStateOf("🧙") }
    var selectedDailyGoal by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    when (currentScreen) {
        OnboardingScreen.WELCOME -> {
            WelcomeScreen(
                onStartClicked = { currentScreen = OnboardingScreen.LOGIN }
            )
        }
        OnboardingScreen.LOGIN -> {
            LoginScreen(
                onLoginClicked = { onComplete() },
                onRegisterClicked = { currentScreen = OnboardingScreen.REGISTER }
            )
        }
        OnboardingScreen.REGISTER -> {
            RegisterScreen(
                onRegisterSuccess = { currentScreen = OnboardingScreen.CHARACTER_CUSTOMIZATION },
                onBackToLogin = { currentScreen = OnboardingScreen.LOGIN }
            )
        }
        OnboardingScreen.CHARACTER_CUSTOMIZATION -> {
            CharacterCustomizationScreen(
                onContinue = { avatar ->
                    selectedAvatar = avatar
                    currentScreen = OnboardingScreen.GOAL_SELECTION
                }
            )
        }
        OnboardingScreen.GOAL_SELECTION -> {
            GoalSelectionScreen(
                onGoalSelected = { goal ->
                    selectedDailyGoal = goal
                    currentScreen = OnboardingScreen.PREFERENCES
                }
            )
        }
        OnboardingScreen.PREFERENCES -> {
            PreferencesScreen(
                selectedAvatar = selectedAvatar,
                dailyGoal = selectedDailyGoal,
                onComplete = { level, goal, username, goalDaily ->
                    scope.launch {
                        repository.saveUserPreferences(
                            avatar = selectedAvatar,
                            level = level,
                            goal = goal,
                            username = username,
                            dailyGoal = goalDaily
                        )
                        repository.syncUserData()
                        onComplete()
                    }
                }
            )
        }
    }
}

// ================================ MAIN SCREENS ================================

/**
 * Schermate principali dell'applicazione dopo l'onboarding.
 *
 * **Architettura:**
 * - BottomNavigationBar per la navigazione tra le 5 schermate principali.
 * - HorizontalPager per lo swipe tra le schermate.
 * - NavHost per le schermate secondarie (navigazione a schermo intero).
 *
 * **Schermate principali:**
 * 1. Home (HomeScreen)
 * 2. Missioni (QuestsScreen)
 * 3. Classifica (LeaderboardScreen)
 * 4. Profilo (ProfileScreen)
 * 5. Impostazioni (SettingsScreen)
 *
 * **Schermate secondarie:**
 * - Debug
 * - Storia
 * - Amici
 * - Aggiungi amico
 * - Profilo pubblico
 *
 * @param repository Repository per l'accesso ai dati.
 * @param isDarkTheme Se `true`, applica il tema scuro.
 * @param onToggleTheme Callback per alternare il tema.
 * @param onLogout Callback per il logout.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreens(
    repository: CaresRepository,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ================================ VIEWMODELS ================================
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(repository))
    val questsViewModel: QuestsViewModel = viewModel(factory = QuestsViewModel.factory(repository))

    // ================================ EFFETTI DI INIZIALIZZAZIONE ================================

    // Sincronizzazione all'avvio
    LaunchedEffect(Unit) {
        homeViewModel.syncUserData()
        questsViewModel.syncUserData()
    }

    // Streak celebration - ascolta gli eventi di aggiornamento dello streak
    LaunchedEffect(Unit) {
        repository.streakEvents.collect { newStreak ->
            StreakCelebrationManager.triggerCelebration(newStreak)
        }
    }

    val celebrationStreak by StreakCelebrationManager.streakToCelebrate.collectAsState()

    // Configurazione snackbar per i ViewModel
    LaunchedEffect(Unit) {
        homeViewModel.setSnackbarFunction { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        }
        questsViewModel.setSnackbarFunction { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        }
    }

    // ================================ NAVIGAZIONE ================================

    val navController = rememberNavController()

    // Lista delle schermate principali in ordine
    val mainScreens = listOf(
        MainScreen.HOME,
        MainScreen.QUESTS,
        MainScreen.LEADERBOARD,
        MainScreen.PROFILE,
        MainScreen.SETTINGS
    )

    // Pager state per le schede principali (5 pagine)
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { mainScreens.size }
    )

    // Quando il pager cambia, assicuriamoci che il NavHost sia sulla route principale
    LaunchedEffect(pagerState.currentPage) {
        if (navController.currentDestination?.route != "main_pager") {
            navController.navigate("main_pager") {
                launchSingleTop = true
                popUpTo("main_pager") { inclusive = true }
            }
        }
    }

    // ================================ STREAK IN PERICOLO ================================

    val streakAtRisk by repository.getStreakAtRisk().collectAsState(initial = false)
    val streakRiskSavedStreak by repository.getStreakRiskSavedStreak().collectAsState(initial = 0)
    val streakSaves by repository.getStreakSaves().collectAsState(initial = 0)
    var showStreakRiskDialog by remember { mutableStateOf(false) }

    // Mostra il dialog quando la streak è a rischio
    LaunchedEffect(streakAtRisk) {
        if (streakAtRisk) {
            showStreakRiskDialog = true
        }
    }

    // Funzioni per il dialog di salvataggio streak
    val onSaveStreak: () -> Unit = {
        scope.launch {
            val saved = repository.saveStreakWithShield()
            if (saved) {
                showStreakRiskDialog = false
                homeViewModel.syncUserData()
                questsViewModel.syncUserData()
            }
        }
    }

    val onLoseStreak: () -> Unit = {
        scope.launch {
            repository.loseStreak()
            showStreakRiskDialog = false
            homeViewModel.syncUserData()
            questsViewModel.syncUserData()
        }
    }

    // ================================ UI PRINCIPALE ================================

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                mainScreens.forEachIndexed { index, screen ->
                    val icon = when (screen) {
                        MainScreen.HOME -> Icons.Filled.Home
                        MainScreen.QUESTS -> Icons.Filled.FitnessCenter
                        MainScreen.LEADERBOARD -> Icons.Filled.People
                        MainScreen.PROFILE -> Icons.Filled.Person
                        MainScreen.SETTINGS -> Icons.Filled.Settings
                        else -> Icons.Filled.Home
                    }
                    NavigationBarItem(
                        icon = { Icon(imageVector = icon, contentDescription = screen.route) },
                        label = { Text(screen.route) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "main_pager",
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                )
            }
        ) {
            // ---- Destinazione principale: HorizontalPager con le 5 schede ----
            composable("main_pager") {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> HomeScreen(viewModel = homeViewModel, isDarkTheme = isDarkTheme)
                        1 -> QuestsScreen(viewModel = questsViewModel, isDarkTheme = isDarkTheme)
                        2 -> {
                            val leaderboardViewModel: LeaderboardViewModel = viewModel(
                                factory = LeaderboardViewModel.factory(repository)
                            )
                            LeaderboardScreen(
                                viewModel = leaderboardViewModel,
                                isDarkTheme = isDarkTheme,
                                onUserClick = { userId ->
                                    navController.navigate("user_profile/$userId")
                                }
                            )
                        }
                        3 -> ProfileScreen(navController = navController, isDarkTheme = isDarkTheme)
                        4 -> SettingsScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = onToggleTheme,
                            onLogout = onLogout,
                            navController = navController
                        )
                    }
                }
            }

            // ---- Schermate secondarie (con slide) ----

            // Debug
            composable(MainScreen.DEBUG.route) {
                val context = LocalContext.current
                val prefsManager = remember { UserPreferencesManager(context) }
                DebugScreen(
                    navController = navController,
                    repository = repository,
                    preferencesManager = prefsManager,
                    questsViewModel = questsViewModel
                )
            }

            // Storia
            composable("story") {
                val storyViewModel: StoryViewModel = viewModel(
                    factory = StoryViewModel.factory(repository)
                )
                StoryScreen(viewModel = storyViewModel)
            }

            // Amici
            composable("friends") {
                val viewModel: FriendsViewModel = viewModel(
                    factory = FriendsViewModel.factory(repository)
                )
                FriendsScreen(
                    navController = navController,
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme
                )
            }

            // Aggiungi amico
            composable("add_friend") {
                val viewModel: AddFriendViewModel = viewModel(
                    factory = AddFriendViewModel.factory(repository)
                )
                AddFriendScreen(
                    navController = navController,
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme
                )
            }

            // Profilo pubblico
            composable("user_profile/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val context = LocalContext.current
                val repository = remember { CaresRepository(context) }
                PublicProfileScreen(
                    userId = userId,
                    repository = repository,
                    isDarkTheme = isDarkTheme,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // ---- Dialog celebrazione streak ----
        if (celebrationStreak != null) {
            Dialog(
                onDismissRequest = { /* Non permettere il dismiss con back press */ },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                StreakCelebrationScreen(
                    newStreak = celebrationStreak!!,
                    onDismiss = {
                        StreakCelebrationManager.dismissCelebration()
                    }
                )
            }
        }
    }

    // ================================ STREAK RISK DIALOG ================================

    /**
     * Dialog che appare quando la streak è in pericolo (l'utente ha saltato un giorno).
     *
     * **Opzioni:**
     * 1. Salva la streak usando uno scudo (se disponibile).
     * 2. Perde la streak (reset a 0).
     *
     * @see StreakRiskDialog per i dettagli dell'interfaccia.
     */
    if (showStreakRiskDialog) {
        StreakRiskDialog(
            currentStreak = if (streakRiskSavedStreak > 0) streakRiskSavedStreak else 1,
            hasShield = streakSaves > 0,
            onSaveStreak = onSaveStreak,
            onLoseStreak = onLoseStreak
        )
    }
}