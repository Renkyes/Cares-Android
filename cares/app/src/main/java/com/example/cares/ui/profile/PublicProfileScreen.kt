// file: ui/profile/PublicProfileScreen.kt
package com.example.cares.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cares.data.manager.StoryManager
import com.example.cares.data.repository.CaresRepository
import com.example.cares.ui.theme.Gradients
import com.example.cares.ui.theme.Neon
import com.example.cares.ui.theme.GlassCard
import com.example.cares.ui.animation.AnimatedEntrance
import com.example.cares.ui.animation.ShimmerLoader
import com.example.cares.utils.allBadges
import com.example.cares.viewmodel.PublicProfileViewModel

// ================================ PUBLIC PROFILE SCREEN ================================

/**
 * Schermata del profilo pubblico di un utente.
 *
 * **Scopo:**
 * Permette di visualizzare il profilo di un altro utente, mostrando:
 * - Avatar e streak
 * - Statistiche (livello, XP, missioni)
 * - Badge sbloccati
 * - Capitoli della storia sbloccati
 * - Lista degli amici
 *
 * **Caratteristiche:**
 * - Pulsante "←" per tornare indietro.
 * - Layout scrollabile.
 * - Stato di caricamento con shimmer.
 * - Gestione utente non trovato.
 *
 * @param userId ID dell'utente da visualizzare.
 * @param repository Repository per l'accesso ai dati.
 * @param isDarkTheme Se `true`, applica il tema scuro.
 * @param onBack Callback per tornare alla schermata precedente.
 */
@Composable
fun PublicProfileScreen(
    userId: String,
    repository: CaresRepository,
    isDarkTheme: Boolean = true,
    onBack: () -> Unit
) {
    // ================================ VIEWMODEL ================================
    val viewModel: PublicProfileViewModel = viewModel(
        factory = PublicProfileViewModel.factory(repository, userId)
    )

    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // ================================ UI ================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Gradients.mainBackground(isDarkTheme))
    ) {
        // ---- Glow decorativo in background ----
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Neon.Purple.copy(alpha = 0.08f), Color.Transparent),
                        radius = 300f
                    )
                )
        )

        // ---- Pulsante Back (fisso in alto) ----
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Text("←", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
        }

        // ---- Contenuto principale ----
        when {
            // Stato: caricamento
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ShimmerLoader(height = 400.dp)
                }
            }
            // Stato: utente non trovato
            profile == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Utente non trovato",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
            // Stato: profilo caricato
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 64.dp, bottom = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ---- Avatar e Streak ----
                    ProfileHeaderPublic(
                        avatar = profile!!.avatar,
                        streak = profile!!.streak
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ---- Statistiche ----
                    StatsSectionPublic(
                        level = profile!!.level,
                        xp = profile!!.xp,
                        completedQuests = profile!!.completedQuests,
                        isDarkTheme = isDarkTheme
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---- Badge ----
                    BadgesSectionPublic(
                        unlockedBadges = profile!!.unlockedBadges,
                        isDarkTheme = isDarkTheme
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---- Storia ----
                    StorySectionPublic(
                        unlockedChapters = profile!!.unlockedChapters,
                        avatar = profile!!.avatar,
                        isDarkTheme = isDarkTheme
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---- Amici ----
                    FriendsSectionPublic(
                        friends = profile!!.friends,
                        isDarkTheme = isDarkTheme
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ================================ COMPONENTI SEZIONI PUBBLICHE ================================

/**
 * Header del profilo pubblico con avatar e streak.
 */
@Composable
fun ProfileHeaderPublic(
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
 * Sezione statistiche del profilo pubblico.
 */
@Composable
fun StatsSectionPublic(
    level: Int,
    xp: Int,
    completedQuests: Int,
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
            StatItem("🏆", level.toString(), "Livello")
            StatItem("⭐", xp.toString(), "XP")
            StatItem("📋", completedQuests.toString(), "Missioni")
        }
    }
}

/**
 * Sezione badge del profilo pubblico (sola lettura).
 */
@Composable
fun BadgesSectionPublic(
    unlockedBadges: Set<String>,
    isDarkTheme: Boolean
) {
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
                text = "Nessun badge ancora sbloccato.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        val hasManyBadges = unlockedBadges.size > 6
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
                    onClick = {} // Disabilitato per profili altrui (sola lettura)
                )
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Sezione storia del profilo pubblico.
 */
@Composable
fun StorySectionPublic(
    unlockedChapters: Set<String>,
    avatar: String,
    isDarkTheme: Boolean
) {
    Text(
        text = "📖 La sua Avventura",
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
            if (unlockedChapters.isEmpty()) {
                Text(
                    text = "Ancora nessun capitolo sbloccato.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                val chapters = StoryManager.getUnlockedChapters(unlockedChapters, avatar)
                chapters.takeLast(2).forEach { chapter ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(chapter.icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            chapter.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (chapters.size > 2) {
                    Text(
                        text = "+ ${chapters.size - 2} altri capitoli...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Sezione amici del profilo pubblico.
 */
@Composable
fun FriendsSectionPublic(
    friends: List<com.example.cares.data.models.FriendInfo>,
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
        if (friends.isEmpty()) {
            Text(
                text = "Nessun amico.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(8.dp)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                friends.forEach { friend ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(friend.avatar, fontSize = 24.sp)
                        Text(
                            friend.username,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}