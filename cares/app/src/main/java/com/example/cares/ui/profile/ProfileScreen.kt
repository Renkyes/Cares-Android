// file: ui/profile/ProfileScreen.kt
package com.example.cares.ui.profile

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.util.Locale
import com.example.cares.data.manager.StoryManager
import com.example.cares.data.manager.UserPreferencesManager
import com.example.cares.data.manager.UserPreferencesManager.XpBoostData
import com.example.cares.data.repository.CaresRepository
import com.example.cares.ui.story.StoryViewModel
import com.example.cares.utils.Badge
import com.example.cares.utils.allBadges
import com.example.cares.ui.theme.Gradients
import com.example.cares.ui.theme.Neon
import com.example.cares.ui.theme.GlassCard
import com.example.cares.ui.animation.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ================================ PROFILE VIEW MODEL ================================

/**
 * ViewModel per la schermata del profilo utente.
 *
 * **Responsabilità:**
 * - Gestire i dati del profilo utente (avatar, livello, XP, streak, badge).
 * - Gestire l'inventario dell'utente (scudi streak, boost XP, frammenti di storia).
 * - Fornire azioni per utilizzare gli oggetti dell'inventario.
 *
 * @param repository Repository per l'accesso ai dati.
 */
class ProfileViewModel(
    private val repository: CaresRepository
) : ViewModel() {

    // ================================ STATO PROFILO ================================

    private val _avatar = MutableStateFlow("🧙")
    private val _level = MutableStateFlow("Non impostato")
    private val _goal = MutableStateFlow("Non impostato")
    private val _xp = MutableStateFlow(0)
    private val _userLevel = MutableStateFlow(1)
    private val _streak = MutableStateFlow(0)
    private val _unlockedBadges = MutableStateFlow<Set<String>>(emptySet())
    private val _completedQuestsCount = MutableStateFlow(0)
    private val _isLoading = MutableStateFlow(true)

    val avatar: StateFlow<String> = _avatar.asStateFlow()
    val level: StateFlow<String> = _level.asStateFlow()
    val goal: StateFlow<String> = _goal.asStateFlow()
    val xp: StateFlow<Int> = _xp.asStateFlow()
    val userLevel: StateFlow<Int> = _userLevel.asStateFlow()
    val streak: StateFlow<Int> = _streak.asStateFlow()
    val unlockedBadges: StateFlow<Set<String>> = _unlockedBadges.asStateFlow()
    val completedQuestsCount: StateFlow<Int> = _completedQuestsCount.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ================================ STATO INVENTARIO ================================

    private val _streakSaves = MutableStateFlow(0)
    private val _xpBoosts = MutableStateFlow<List<XpBoostData>>(emptyList())
    private val _storyFragments = MutableStateFlow<Set<String>>(emptySet())

    val streakSaves: StateFlow<Int> = _streakSaves.asStateFlow()
    val xpBoosts: StateFlow<List<XpBoostData>> = _xpBoosts.asStateFlow()
    val storyFragments: StateFlow<Set<String>> = _storyFragments.asStateFlow()

    // Callback per Snackbar
    private var showSnackbar: ((String) -> Unit)? = null

    // ================================ METODI PUBBLICI ================================

    /**
     * Imposta la funzione per mostrare snackbar.
     */
    fun setSnackbarFunction(onShowSnackbar: (String) -> Unit) {
        showSnackbar = onShowSnackbar
    }

    /**
     * Carica i dati utente dal repository.
     */
    fun loadUserData() {
        viewModelScope.launch {
            val prefs = repository.getAllPreferences().first()
            _avatar.value = prefs[UserPreferencesManager.SELECTED_AVATAR] ?: "🧙"
            _level.value = prefs[UserPreferencesManager.ACTIVITY_LEVEL] ?: "Non impostato"
            _goal.value = prefs[UserPreferencesManager.FITNESS_GOAL] ?: "Non impostato"
            _xp.value = prefs[UserPreferencesManager.USER_XP] ?: 0
            _userLevel.value = prefs[UserPreferencesManager.USER_LEVEL] ?: 1
            _streak.value = prefs[UserPreferencesManager.DAILY_STREAK] ?: 0
            _unlockedBadges.value = prefs[UserPreferencesManager.UNLOCKED_BADGES] ?: emptySet()
            _completedQuestsCount.value = prefs[UserPreferencesManager.TOTAL_COMPLETED_QUESTS] ?: 0

            // Carica inventario
            _streakSaves.value = repository.getStreakSaves().first()
            _xpBoosts.value = repository.getXpBoosts().first()
            _storyFragments.value = repository.getStoryFragments().first()

            _isLoading.value = false
        }
    }

    /**
     * Usa uno scudo streak per salvare la streak.
     */
    fun useStreakSave() {
        viewModelScope.launch {
            val used = repository.useStreakSave()
            if (used) {
                _streakSaves.value = repository.getStreakSaves().first()
                showSnackbar?.invoke("🛡️ Scudo Streak attivato! La tua streak è al sicuro.")
            } else {
                showSnackbar?.invoke("❌ Nessuno scudo disponibile.")
            }
        }
    }

    /**
     * Attiva un boost XP dall'inventario.
     * @param index Indice del boost nella lista.
     */
    fun activateXpBoost(index: Int) {
        viewModelScope.launch {
            val boost = repository.useXpBoost(index)
            if (boost != null) {
                repository.activateXpBoost(boost.multiplier, boost.days)
                _xpBoosts.value = repository.getXpBoosts().first()
                showSnackbar?.invoke("⚡ Boost XP x${boost.multiplier} attivato per ${boost.days} giorni!")
            } else {
                showSnackbar?.invoke("❌ Boost non disponibile.")
            }
        }
    }

    /**
     * Usa un frammento di storia per sbloccare un capitolo.
     * @param chapterId ID del capitolo da sbloccare.
     */
    fun useStoryFragment(chapterId: String) {
        viewModelScope.launch {
            val used = repository.useStoryFragment(chapterId)
            if (used) {
                _storyFragments.value = repository.getStoryFragments().first()
                showSnackbar?.invoke("📖 Capitolo sbloccato! Vai nella sezione Storia per leggerlo.")
            } else {
                showSnackbar?.invoke("❌ Frammento non disponibile.")
            }
        }
    }

    // ================================ FACTORY ================================

    companion object {
        fun factory(repository: CaresRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                        return ProfileViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

// ================================ PROFILE SCREEN ================================

/**
 * Schermata del profilo utente.
 *
 * **Sezioni:**
 * 1. Avatar e streak.
 * 2. Statistiche (livello, XP, missioni).
 * 3. Inventario (scudi streak, boost XP, frammenti di storia).
 * 4. Badge sbloccati (cliccabili per dettagli).
 * 5. Storia dell'avventura.
 * 6. Amici.
 *
 * @param navController Controller per la navigazione.
 * @param isDarkTheme Se `true`, applica il tema scuro.
 */
@Composable
fun ProfileScreen(
    navController: NavController,
    isDarkTheme: Boolean = true
) {
    // ================================ VIEWMODELS ================================
    val context = LocalContext.current
    val repository = remember { CaresRepository(context) }

    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.factory(repository)
    )

    val storyViewModel: StoryViewModel = viewModel(
        factory = StoryViewModel.factory(repository)
    )
    val unlockedIds by storyViewModel.unlockedChapters.collectAsState()

    // ================================ STATO ================================
    val avatar by viewModel.avatar.collectAsState()
    val level by viewModel.level.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val xp by viewModel.xp.collectAsState()
    val userLevel by viewModel.userLevel.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val unlockedBadges by viewModel.unlockedBadges.collectAsState()
    val completedQuestsCount by viewModel.completedQuestsCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val streakSaves by viewModel.streakSaves.collectAsState()
    val xpBoosts by viewModel.xpBoosts.collectAsState()
    val storyFragments by viewModel.storyFragments.collectAsState()

    // ================================ SNACKBAR ================================
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.setSnackbarFunction { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        }
    }

    // ================================ STATO DIALOG BADGE ================================
    var selectedBadge by remember { mutableStateOf<Badge?>(null) }
    var showBadgeDialog by remember { mutableStateOf(false) }

    // ================================ UI ================================
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Neon.Green, modifier = Modifier.size(48.dp))
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Gradients.mainBackground(isDarkTheme))
        ) {
            // ---- Effetti glow in background ----
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.TopStart)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Neon.Purple.copy(alpha = 0.1f), Color.Transparent),
                            radius = 350f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomEnd)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Neon.Green.copy(alpha = 0.08f), Color.Transparent),
                            radius = 300f
                        )
                    )
            )

            // ---- Contenuto principale ----
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .statusBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ---- Avatar e Streak ----
                    item {
                        ProfileHeader(
                            avatar = avatar,
                            streak = streak
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ---- Statistiche ----
                    item {
                        StatsSection(
                            userLevel = userLevel,
                            xp = xp,
                            completedQuestsCount = completedQuestsCount,
                            level = level,
                            goal = goal,
                            isDarkTheme = isDarkTheme
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // ---- Inventario ----
                    item {
                        InventorySection(
                            streakSaves = streakSaves,
                            xpBoosts = xpBoosts,
                            storyFragments = storyFragments,
                            onUseStreakSave = { viewModel.useStreakSave() },
                            onActivateXpBoost = { index -> viewModel.activateXpBoost(index) },
                            onUseStoryFragment = { chapterId -> viewModel.useStoryFragment(chapterId) },
                            isDarkTheme = isDarkTheme
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ---- Badge ----
                    item {
                        BadgesSection(
                            unlockedBadges = unlockedBadges,
                            onBadgeClick = { badge ->
                                selectedBadge = badge
                                showBadgeDialog = true
                            },
                            isDarkTheme = isDarkTheme
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ---- Storia ----
                    item {
                        StorySection(
                            unlockedIds = unlockedIds,
                            avatar = avatar,
                            navController = navController,
                            isDarkTheme = isDarkTheme
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ---- Amici ----
                    item {
                        FriendsSection(
                            navController = navController,
                            isDarkTheme = isDarkTheme
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // ---- Snackbar ----
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // ---- Badge Detail Dialog ----
            if (showBadgeDialog && selectedBadge != null) {
                val isUnlocked = unlockedBadges.contains(selectedBadge!!.id)
                BadgeDetailDialog(
                    badge = selectedBadge!!,
                    isUnlocked = isUnlocked,
                    onDismiss = {
                        showBadgeDialog = false
                        selectedBadge = null
                    },
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

// ================================ COMPONENTI SEZIONI ================================

/**
 * Header del profilo con avatar e streak.
 */
@Composable
fun ProfileHeader(
    avatar: String,
    streak: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Neon.Green.copy(alpha = 0.3f), Color.Transparent),
                        radius = 60f
                    )
                )
        ) {
            Text(
                text = avatar,
                fontSize = 48.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

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
            Text(
                "giorni",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Sezione statistiche.
 */
@Composable
fun StatsSection(
    userLevel: Int,
    xp: Int,
    completedQuestsCount: Int,
    level: String,
    goal: String,
    isDarkTheme: Boolean
) {
    Text(
        text = "Statistiche",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        darkTheme = isDarkTheme
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = "🏆",
                value = userLevel.toString(),
                label = "Livello"
            )
            StatItem(
                icon = "⭐",
                value = xp.toString(),
                label = "XP"
            )
            StatItem(
                icon = "📋",
                value = completedQuestsCount.toString(),
                label = "Missioni"
            )
        }
    }

    Text(
        text = "Livello attività: $level | Obiettivo: $goal",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * Sezione badge.
 */
@Composable
fun BadgesSection(
    unlockedBadges: Set<String>,
    onBadgeClick: (Badge) -> Unit,
    isDarkTheme: Boolean
) {
    val hasManyBadges = allBadges.size > 6

    Text(
        text = "🏅 Badge Sbloccati (${unlockedBadges.size}/${allBadges.size})",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    if (unlockedBadges.isEmpty()) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            darkTheme = isDarkTheme
        ) {
            Text(
                text = "Nessun badge ancora sbloccato. Completa missioni per guadagnarli! 💪",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (hasManyBadges) 180.dp else 130.dp)
            ) {
                items(allBadges) { badge ->
                    val isUnlocked = unlockedBadges.contains(badge.id)
                    BadgeItem(
                        badge = badge,
                        isUnlocked = isUnlocked,
                        onClick = { onBadgeClick(badge) }
                    )
                }
            }
        }

        if (hasManyBadges) {
            Text(
                text = "👆 Scorri per vedere tutti i badge",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Sezione storia.
 */
@Composable
fun StorySection(
    unlockedIds: Set<String>,
    avatar: String,
    navController: NavController,
    isDarkTheme: Boolean
) {
    Text(
        text = "📖 La Tua Avventura",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        darkTheme = isDarkTheme
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (unlockedIds.isEmpty()) {
                Text(
                    text = "Completa missioni per iniziare la tua avventura! 💪",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                val unlockedChapters = StoryManager.getUnlockedChapters(unlockedIds, avatar)
                unlockedChapters.takeLast(2).forEach { chapter ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chapter.icon,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (unlockedChapters.size > 2) {
                    Text(
                        text = "+ ${unlockedChapters.size - 2} altri capitoli...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { navController.navigate("story") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Neon.Green.copy(alpha = 0.3f),
                                    Neon.Green.copy(alpha = 0.1f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Neon.Green.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📖 Leggi tutta la storia",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Sezione amici.
 */
@Composable
fun FriendsSection(
    navController: NavController,
    isDarkTheme: Boolean
) {
    Text(
        text = "👥 Amici",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        darkTheme = isDarkTheme
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Gestisci le tue amicizie e le richieste in sospeso",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Button(
                onClick = { navController.navigate("friends") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Neon.Green.copy(alpha = 0.3f),
                                    Neon.Green.copy(alpha = 0.1f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Neon.Green.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👥 Vedi i tuoi amici",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

// ================================ INVENTORY SECTION ================================

/**
 * Sezione inventario del profilo.
 */
@Composable
fun InventorySection(
    streakSaves: Int,
    xpBoosts: List<XpBoostData>,
    storyFragments: Set<String>,
    onUseStreakSave: () -> Unit,
    onActivateXpBoost: (Int) -> Unit,
    onUseStoryFragment: (String) -> Unit,
    isDarkTheme: Boolean
) {
    val hasItems = streakSaves > 0 || xpBoosts.isNotEmpty() || storyFragments.isNotEmpty()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        darkTheme = isDarkTheme
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🎒 Inventario",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!hasItems) {
                Text(
                    text = "Nessun oggetto in inventario. Completa missioni speciali per ottenerne! 💪",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                // ---- Scudo Streak ----
                if (streakSaves > 0) {
                    InventoryItemRow(
                        icon = "🛡️",
                        name = "Scudo Streak",
                        description = "Salva la tua streak quando sta per scadere.",
                        quantity = streakSaves,
                        actionText = "Usa",
                        onAction = onUseStreakSave,
                        isDarkTheme = isDarkTheme
                    )
                }

                // ---- Boost XP ----
                xpBoosts.forEachIndexed { index, boost ->
                    val isLast = index == xpBoosts.size - 1 && storyFragments.isEmpty()
                    InventoryItemRow(
                        icon = "⚡",
                        name = "Boost x${boost.multiplier}",
                        description = "Moltiplica XP per ${boost.multiplier}x per ${boost.days} giorni.",
                        quantity = 1,
                        actionText = "Attiva",
                        onAction = { onActivateXpBoost(index) },
                        isDarkTheme = isDarkTheme,
                        isLast = isLast
                    )
                }

                // ---- Frammenti di Storia ----
                storyFragments.forEachIndexed { index, chapterId ->
                    val isLast = index == storyFragments.size - 1
                    val chapterTitle = chapterId
                        .replace("w_chapter_extra_", "Capitolo Extra ")
                        .replace("_", " ")
                        .trim()
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                    InventoryItemRow(
                        icon = "📖",
                        name = "Frammento: $chapterTitle",
                        description = "Sblocca un capitolo della storia.",
                        quantity = 1,
                        actionText = "Sblocca",
                        onAction = { onUseStoryFragment(chapterId) },
                        isDarkTheme = isDarkTheme,
                        isLast = isLast
                    )
                }
            }
        }
    }
}

// ================================ COMPONENTI RIUTILIZZABILI ================================

/**
 * Riga di un oggetto dell'inventario.
 */
@Composable
fun InventoryItemRow(
    icon: String,
    name: String,
    description: String,
    quantity: Int,
    actionText: String,
    onAction: () -> Unit,
    isDarkTheme: Boolean,
    isLast: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = icon, fontSize = 24.sp)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (quantity > 1) {
                            Text(
                                text = " ×$quantity",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Neon.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Button(
                onClick = onAction,
                modifier = Modifier
                    .height(32.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Neon.Green.copy(alpha = 0.3f),
                                Neon.Green.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Neon.Green.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = null,
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(
                    text = actionText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        if (!isLast) {
            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

/**
 * Item statistiche.
 */
@Composable
fun StatItem(
    icon: String,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(icon, fontSize = 24.sp)
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

/**
 * Item badge singolo.
 */
@Composable
fun BadgeItem(
    badge: Badge,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(isUnlocked) {
        if (isUnlocked) startAnimation = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.9f,
        animationSpec = Animations.springMedium(),
        label = "badgeScale"
    )

    val glow by animateFloatAsState(
        targetValue = if (isUnlocked && startAnimation) 1f else 0f,
        animationSpec = Animations.extraSlowTween(),
        label = "glow"
    )

    val iconSize by animateFloatAsState(
        targetValue = if (isUnlocked) 34f else 28f,
        animationSpec = Animations.springMedium(),
        label = "iconSize"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .scale(scale)
            .clickable { onClick() }
            .background(
                color = if (isUnlocked)
                    Neon.Green.copy(alpha = 0.15f + glow * 0.15f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isUnlocked) 1.5.dp else 1.dp,
                color = if (isUnlocked)
                    Neon.Green.copy(alpha = 0.4f + glow * 0.4f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isUnlocked) badge.icon else "🔒",
                fontSize = iconSize.sp
            )
            Text(
                text = badge.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal,
                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * Dialog dettaglio badge.
 */
@Composable
fun BadgeDetailDialog(
    badge: Badge,
    isUnlocked: Boolean,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color(0xFFF5F7FA),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isUnlocked) "🏅" else "🔒",
                    fontSize = 28.sp
                )
                Text(
                    text = badge.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Neon.Green else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked)
                                Brush.radialGradient(
                                    colors = listOf(Neon.Green.copy(alpha = 0.3f), Color.Transparent),
                                    radius = 60f
                                )
                            else
                                SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isUnlocked) badge.icon else "🔒",
                        fontSize = 48.sp
                    )
                }

                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUnlocked)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Divider(
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                )

                if (isUnlocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "✅",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Badge sbloccato!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Neon.Green
                        )
                    }
                    Text(
                        text = "Complimenti! Hai sbloccato questo badge completando i requisiti.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    val requirements = getBadgeRequirements(badge)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🔑 Come sbloccarlo:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = requirements,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Continua ad allenarti per sbloccarlo! 💪",
                            style = MaterialTheme.typography.bodySmall,
                            color = Neon.Cyan,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDarkTheme) Neon.Green else Color(0xFF2E7D32)
                )
            ) {
                Text("Capito! 👍")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Restituisce i requisiti per sbloccare un badge.
 */
fun getBadgeRequirements(badge: Badge): String {
    return when (badge.id) {
        "first_steps" -> "Completa la tua prima missione"
        "amateur" -> "Completa 10 missioni"
        "dedicated" -> "Completa 25 missioni"
        "marathoner" -> "Completa 50 missioni"
        "level_5" -> "Raggiungi il livello 5"
        "level_10" -> "Raggiungi il livello 10"
        "level_25" -> "Raggiungi il livello 25"
        "level_50" -> "Raggiungi il livello 50"
        "level_100" -> "Raggiungi il livello 100"
        "streak_7_g" -> "Mantieni uno streak di 7 giorni"
        "streak_14_g" -> "Mantieni uno streak di 14 giorni"
        "streak_1_m" -> "Mantieni uno streak di 30 giorni"
        "streak_3_m" -> "Mantieni uno streak di 3 mesi (90 giorni)"
        "streak_6_m" -> "Mantieni uno streak di 6 mesi (180 giorni)"
        "streak_1_y" -> "Mantieni uno streak di 1 anno (365 giorni)"
        else -> badge.description
    }
}