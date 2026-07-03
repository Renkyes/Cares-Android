// file: ui/debug/DebugScreen.kt
package com.example.cares.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cares.data.manager.UserPreferencesManager
import com.example.cares.data.repository.CaresRepository
import com.example.cares.ui.quests.QuestsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Schermata di debug che espone strumenti per sviluppatori.
 *
 * **Scopo:** fornire un'interfaccia per eseguire operazioni di manutenzione,
 * reset dei dati e simulazioni utili durante lo sviluppo e il testing.
 *
 * **Sezioni:**
 * 1. **Reset**: pulsanti per resettare streak, badge, missioni e date.
 * 2. **Simulazione**: strumenti per avanzare il giorno artificialmente.
 * 3. **Leaderboard**: pulsante per forzare la sincronizzazione del profilo su Firebase.
 * 4. **Inventario**: pulsanti per aggiungere oggetti di test.
 *
 * **Accessibilità:** questa schermata è raggiungibile solo dalle impostazioni
 * (tramite il pulsante "Strumenti di Debug") ed è pensata esclusivamente per
 * sviluppatori e tester.
 *
 * **Nota di sicurezza:** tutte le operazioni modificano i dati persistenti.
 * Utilizzare con cautela, specialmente in ambienti di produzione.
 *
 * @param navController Controller di navigazione per tornare alla schermata precedente.
 * @param repository Repository per operazioni sui dati (es. sincronizzazione).
 * @param preferencesManager Manager delle preferenze per reset specifici.
 * @param questsViewModel ViewModel per la gestione delle missioni.
 */
@Composable
fun DebugScreen(
    navController: NavController,
    repository: CaresRepository,
    preferencesManager: UserPreferencesManager,
    questsViewModel: QuestsViewModel
) {
    val scrollState = rememberScrollState()
    val scope = CoroutineScope(Dispatchers.Main)
    var isProcessing by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // ================================ HEADER ================================

            Text(
                text = "🛠️ Strumenti di Debug",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ================================ AVVISO ================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "⚠️ Questi strumenti sono solo per sviluppo. Usali con cautela.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================================ SEZIONE RESET ================================

            Text(
                text = "🔁 Reset",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Resetta lo streak corrente (giorni consecutivi)
            DebugButton(
                text = "❌ Resetta Streak",
                color = Color(0xFFF44336),
                onClick = {
                    if (!isProcessing) {
                        isProcessing = true
                        scope.launch {
                            try {
                                preferencesManager.resetStreak()
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                }
            )

            // Resetta tutti i badge sbloccati
            DebugButton(
                text = "🏅 Resetta Badge",
                color = Color(0xFFFF9800),
                onClick = {
                    if (!isProcessing) {
                        isProcessing = true
                        scope.launch {
                            try {
                                preferencesManager.resetBadges()
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                }
            )

            // Resetta le missioni completate oggi
            DebugButton(
                text = "🔄 Resetta Missioni",
                color = Color(0xFF2196F3),
                onClick = {
                    if (!isProcessing) {
                        isProcessing = true
                        scope.launch {
                            try {
                                preferencesManager.resetQuests()
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                }
            )

            // Rigenera le missioni (debug)
            DebugButton(
                text = "🔄 Rigenera Missioni (DEBUG)",
                color = Color(0xFF2196F3),
                onClick = {
                    if (!isProcessing) {
                        isProcessing = true
                        scope.launch {
                            try {
                                questsViewModel.forceRefreshQuests()
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                }
            )

            // Resetta streak e date di completamento (debug avanzato)
            DebugButton(
                text = "📅 Reset Streak e Date (DEBUG)",
                color = Color(0xFF9C27B0),
                onClick = {
                    if (!isProcessing) {
                        isProcessing = true
                        scope.launch {
                            try {
                                preferencesManager.debugResetStreakAndDates()
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ================================ SEZIONE SIMULAZIONE ================================

            Text(
                text = "⏳ Simulazione",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Simula il passaggio a un nuovo giorno, utile per testare il reset giornaliero
            DebugButton(
                text = "⏩ Avanza Giorno (DEBUG)",
                color = Color(0xFF607D8B),
                onClick = {
                    if (!isProcessing) {
                        isProcessing = true
                        scope.launch {
                            try {
                                preferencesManager.debugAdvanceDay()
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ================================ SEZIONE LEADERBOARD ================================

            Text(
                text = "🏆 Leaderboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Sincronizza forzatamente i dati del profilo su Firebase
            DebugButton(
                text = "📤 Sincronizza Profilo su Firebase",
                color = Color(0xFF2196F3),
                onClick = {
                    if (!isProcessing) {
                        isProcessing = true
                        scope.launch {
                            try {
                                repository.syncUserData()
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ================================ SEZIONE INVENTARIO ================================

            Text(
                text = "🎒 Inventario (Debug)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Aggiunge uno scudo streak all'inventario
            DebugButton(
                text = "🛡️ Aggiungi Scudo Streak",
                color = Color(0xFF4CAF50),
                onClick = {
                    if (!isProcessing) {
                        isProcessing = true
                        scope.launch {
                            try {
                                repository.addStreakSave(1)
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                }
            )

            // Aggiunge un boost XP all'inventario
            DebugButton(
                text = "⚡ Aggiungi Boost XP x2 (3g)",
                color = Color(0xFFFFEB3B),
                onClick = {
                    if (!isProcessing) {
                        isProcessing = true
                        scope.launch {
                            try {
                                repository.addXpBoost(2.0f, 3)
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ================================ NAVIGAZIONE ================================

            Button(
                onClick = { navController.navigateUp() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("← Torna alle Impostazioni")
            }
        }
    }
}

// ================================ COMPONENTE PULSANTE DI DEBUG ================================

/**
 * Componente riutilizzabile per i pulsanti di debug.
 *
 * **Convenzione dei colori:**
 * - Rosso (#F44336): operazioni distruttive (reset completi)
 * - Arancione (#FF9800): operazioni parziali (reset badge)
 * - Blu (#2196F3): operazioni di sincronizzazione o reset di dati transitori
 * - Viola (#9C27B0): operazioni di debug avanzate
 * - Grigio (#607D8B): simulazioni
 * - Verde (#4CAF50): aggiunta di oggetti
 * - Giallo (#FFEB3B): aggiunta di boost
 *
 * @param text Testo del pulsante.
 * @param color Colore di sfondo del pulsante.
 * @param onClick Azione da eseguire al click.
 */
@Composable
private fun DebugButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text)
    }
    Spacer(modifier = Modifier.height(8.dp))
}