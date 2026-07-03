// file: ui/leaderboard/LeaderboardScreen.kt
package com.example.cares.ui.leaderboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cares.data.models.LeaderboardEntry
import com.example.cares.viewmodel.LeaderboardViewModel
import com.example.cares.ui.theme.Gradients
import com.example.cares.ui.theme.Neon
import com.example.cares.ui.theme.GlassCard
import com.example.cares.ui.animation.*

// ================================ LEADERBOARD SCREEN ================================

/**
 * Schermata della classifica (Leaderboard).
 *
 * **Caratteristiche:**
 * - Visualizzazione della classifica globale o solo degli amici.
 * - Ordinamento per XP, Streak, Livello o Missioni completate.
 * - Evidenziazione dell'utente corrente con bordo verde neon.
 * - Clic su un utente per visualizzarne il profilo.
 * - Animazione di entrata per ogni elemento della lista.
 *
 * **Flusso di utilizzo:**
 * 1. Il ViewModel carica la classifica da Firestore.
 * 2. L'utente può cambiare modalità di visualizzazione (Globale/Amici).
 * 3. L'utente può cambiare il criterio di ordinamento.
 * 4. L'utente può cliccare su un elemento per vedere il profilo.
 *
 * @param viewModel Il ViewModel che gestisce i dati e la logica della classifica.
 * @param isDarkTheme Se `true` applica il tema scuro, altrimenti il tema chiaro.
 * @param onUserClick Callback invocato quando l'utente clicca su un elemento della classifica.
 *                    Riceve l'ID dell'utente selezionato.
 */
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    isDarkTheme: Boolean = true,
    onUserClick: (String) -> Unit = {}
) {
    // ================================ STATO ================================
    val leaderboard by viewModel.leaderboard.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserPosition by viewModel.currentUserPosition.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()
    val sortLabel by viewModel.sortLabel.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    // ================================ UI ================================
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Neon.Green, modifier = Modifier.size(48.dp))
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
                    .statusBarsPadding()
            ) {
                // ---- Header ----
                Text(
                    text = "Classifica",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // ---- Toggle Globale / Amici ----
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassChip(
                        text = "Globale",
                        isSelected = viewMode == LeaderboardViewModel.ViewMode.GLOBAL,
                        isDarkTheme = isDarkTheme,
                        onClick = { viewModel.setViewMode(LeaderboardViewModel.ViewMode.GLOBAL) },
                        modifier = Modifier.weight(1f)
                    )
                    GlassChip(
                        text = "Amici",
                        isSelected = viewMode == LeaderboardViewModel.ViewMode.FRIENDS,
                        isDarkTheme = isDarkTheme,
                        onClick = { viewModel.setViewMode(LeaderboardViewModel.ViewMode.FRIENDS) },
                        color = Neon.Green,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ---- Filtri di ordinamento ----
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassChip(
                        text = "XP",
                        isSelected = currentSort == LeaderboardViewModel.SortType.XP,
                        isDarkTheme = isDarkTheme,
                        onClick = { viewModel.setSortType(LeaderboardViewModel.SortType.XP) },
                        modifier = Modifier.weight(1f)
                    )
                    GlassChip(
                        text = "Streak",
                        isSelected = currentSort == LeaderboardViewModel.SortType.STREAK,
                        isDarkTheme = isDarkTheme,
                        onClick = { viewModel.setSortType(LeaderboardViewModel.SortType.STREAK) },
                        color = Neon.Orange,
                        modifier = Modifier.weight(1f)
                    )
                    GlassChip(
                        text = "Level",
                        isSelected = currentSort == LeaderboardViewModel.SortType.LEVEL,
                        isDarkTheme = isDarkTheme,
                        onClick = { viewModel.setSortType(LeaderboardViewModel.SortType.LEVEL) },
                        color = Neon.Purple,
                        modifier = Modifier.weight(1f)
                    )
                    GlassChip(
                        text = "Quest",
                        isSelected = currentSort == LeaderboardViewModel.SortType.QUESTS,
                        isDarkTheme = isDarkTheme,
                        onClick = { viewModel.setSortType(LeaderboardViewModel.SortType.QUESTS) },
                        color = Neon.Cyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ---- Posizione utente corrente ----
                if (currentUserPosition != -1) {
                    Text(
                        text = "La tua posizione: #$currentUserPosition",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDarkTheme) Neon.Green else Color(0xFF2E7D32),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // ---- Lista classifica ----
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(
                        items = leaderboard,
                        key = { _, entry -> entry.userId }
                    ) { index, entry ->
                        AnimatedEntrance(delay = index * 50) {
                            LeaderboardItem(
                                position = index + 1,
                                entry = entry,
                                isCurrentUser = entry.isCurrentUser,
                                sortType = currentSort,
                                isDarkTheme = isDarkTheme,
                                onClick = { onUserClick(entry.userId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================================ LEADERBOARD ITEM ================================

/**
 * Singola riga della classifica.
 *
 * **Caratteristiche:**
 * - Mostra posizione (con medaglia per i primi 3 posti).
 * - Avatar e nome utente.
 * - Valore principale in base al criterio di ordinamento.
 * - Evidenziazione dell'utente corrente con bordo verde neon.
 * - Animazione di ingresso con [AnimatedEntrance].
 * - Clic sulla riga per visualizzare il profilo utente.
 *
 * @param position Posizione in classifica (1-based).
 * @param entry Dati dell'utente da visualizzare.
 * @param isCurrentUser Se `true`, la riga viene evidenziata.
 * @param sortType Criterio di ordinamento corrente, determina il valore principale mostrato.
 * @param isDarkTheme Se `true` applica il tema scuro.
 * @param onClick Callback invocato quando l'utente clicca sulla riga.
 */
@Composable
fun LeaderboardItem(
    position: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    sortType: LeaderboardViewModel.SortType,
    isDarkTheme: Boolean,
    onClick: () -> Unit = {}
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isCurrentUser) 1f else 0.85f)
            .then(
                if (isCurrentUser) {
                    Modifier.border(
                        width = 2.dp,
                        color = if (isDarkTheme) Neon.Green.copy(alpha = 0.6f) else Color(0xFF2E7D32).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            )
            .clickable { onClick() },
        darkTheme = isDarkTheme
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ---- Posizione e Avatar ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Medaglia o numero posizione
                if (position <= 3) {
                    Text(
                        text = when (position) {
                            1 -> "🥇"
                            2 -> "🥈"
                            else -> "🥉"
                        },
                        fontSize = 28.sp
                    )
                } else {
                    Text(
                        text = "#$position",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentUser) {
                            if (isDarkTheme) Neon.Green else Color(0xFF2E7D32)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                }

                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrentUser) Neon.Green.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                ) {
                    Text(
                        text = entry.avatar,
                        fontSize = 24.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // ---- Nome e Valore Principale ----
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrentUser) {
                        if (isDarkTheme) Neon.Green else Color(0xFF2E7D32)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                // Valore in base al tipo di ordinamento
                val mainValue = when (sortType) {
                    LeaderboardViewModel.SortType.XP -> "⭐ ${entry.xp} XP"
                    LeaderboardViewModel.SortType.STREAK -> "🔥 ${entry.streak} giorni"
                    LeaderboardViewModel.SortType.LEVEL -> "🏆 Lv.${entry.level}"
                    LeaderboardViewModel.SortType.QUESTS -> "📋 ${entry.completedQuests} missioni"
                }
                val mainColor = when (sortType) {
                    LeaderboardViewModel.SortType.XP -> if (isDarkTheme) Neon.Green else Color(0xFF2E7D32)
                    LeaderboardViewModel.SortType.STREAK -> if (isDarkTheme) Neon.Orange else Color(0xFFE65100)
                    LeaderboardViewModel.SortType.LEVEL -> if (isDarkTheme) Neon.Purple else Color(0xFF6A1B9A)
                    LeaderboardViewModel.SortType.QUESTS -> if (isDarkTheme) Neon.Cyan else Color(0xFF00838F)
                }

                Text(
                    text = mainValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = mainColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ================================ GLASS CHIP ================================

/**
 * Chip in stile glassmorphism per filtri e toggle.
 *
 * **Utilizzo:**
 * - Selezionare la modalità di visualizzazione (Globale/Amici).
 * - Selezionare i criteri di ordinamento (XP/Streak/Livello/Missioni).
 *
 * **Caratteristiche:**
 * - Effetto glassmorphism quando selezionato.
 * - Colore di accento personalizzabile.
 * - Adattamento al tema scuro/chiaro.
 *
 * @param text Testo del chip.
 * @param isSelected Se `true`, il chip viene evidenziato con il colore specificato.
 * @param onClick Azione al click.
 * @param color Colore di accento quando selezionato (default: Neon.Cyan).
 * @param isDarkTheme Se `true` applica il tema scuro.
 * @param modifier Modificatori da applicare.
 */
@Composable
fun GlassChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color = Neon.Cyan,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isSelected) {
        Color.White
    } else {
        if (isDarkTheme) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .background(
                brush = if (isSelected) {
                    Brush.horizontalGradient(
                        colors = listOf(color.copy(alpha = 0.5f), color.copy(alpha = 0.3f))
                    )
                } else {
                    val color1 = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
                    val color2 = if (isDarkTheme) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.02f)
                    Brush.horizontalGradient(colors = listOf(color1, color2))
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) color.copy(alpha = 0.8f) else {
                    if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(12.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1
        )
    }
}