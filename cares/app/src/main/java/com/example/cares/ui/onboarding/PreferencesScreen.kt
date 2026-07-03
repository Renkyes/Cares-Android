// file: ui/onboarding/PreferencesScreen.kt
package com.example.cares.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cares.ui.theme.Gradients
import com.example.cares.ui.theme.Neon
import com.example.cares.ui.theme.GlassCard
import kotlinx.coroutines.delay

// ================================ PREFERENCES SCREEN ================================

/**
 * Schermata di impostazioni finali durante l'onboarding.
 *
 * **Scopo:**
 * Permette all'utente di completare il proprio profilo con:
 * - Nome utente (obbligatorio).
 * - Livello di attività (Sedentario, Moderato, Attivo).
 * - Obiettivo fitness principale (Perdere peso, Tonificare, Resistenza).
 *
 * **Caratteristiche:**
 * - Visualizzazione dell'avatar scelto.
 * - Riepilogo dell'obiettivo giornaliero selezionato.
 * - Validazione del nome utente.
 * - Animazione di ingresso con scala (spring).
 * - Evidenziazione delle opzioni selezionate con colore verde neon.
 * - Pulsante "Inizia l'avventura!" abilitato solo dopo il completamento.
 *
 * **Flusso di utilizzo:**
 * 1. L'utente inserisce il proprio nome utente.
 * 2. Seleziona il livello di attività tra tre opzioni.
 * 3. Seleziona l'obiettivo fitness principale tra tre opzioni.
 * 4. Visualizza il riepilogo dell'obiettivo giornaliero (se selezionato).
 * 5. Premere "Inizia l'avventura!" per completare l'onboarding.
 *
 * @param selectedAvatar Emoji dell'avatar scelto dall'utente.
 * @param dailyGoal Obiettivo giornaliero selezionato (opzionale).
 * @param onComplete Callback eseguito al completamento.
 *                    Riceve (level, goal, username, dailyGoal).
 */
@Composable
fun PreferencesScreen(
    selectedAvatar: String,
    dailyGoal: String = "",
    onComplete: (String, String, String, String) -> Unit
) {
    // ================================ STATO ================================
    var username by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }

    val selectedLevel = remember { mutableStateOf<String?>(null) }
    val selectedGoal = remember { mutableStateOf<String?>(null) }

    // ================================ DATI ================================
    val levels = listOf("🛋️ Sedentario", "🚶 Moderato", "🏃 Attivo")
    val goals = listOf("🎯 Perdere peso", "💪 Tonificare", "⚡ Resistenza")

    // ================================ ANIMAZIONI ================================
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200L) // Leggero ritardo per l'animazione di ingresso
        startAnimation = true
    }

    // ================================ UI ================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Gradients.mainBackground(true))
    ) {
        // ---- Glow decorativo in background ----
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.BottomEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Neon.Green.copy(alpha = 0.08f), Color.Transparent),
                        radius = 350f
                    )
                )
        )

        // ---- Contenuto principale ----
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ---- Titolo ----
            Text(
                text = "Personalizza il tuo piano",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hai scelto $selectedAvatar",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // ---- Nome utente ----
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                darkTheme = true
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scegli il tuo nome",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            usernameError = null
                        },
                        label = { Text("Nome utente") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = usernameError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Neon.Green,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = Neon.Green
                        ),
                        supportingText = {
                            if (usernameError != null) {
                                Text(
                                    text = usernameError!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Livello di attività ----
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                darkTheme = true
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Qual è il tuo livello di attività?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        levels.forEach { level ->
                            val isSelected = selectedLevel.value == level
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.05f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "levelScale_$level"
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Neon.Green else Color.White.copy(alpha = 0.05f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedLevel.value = level }
                                    .scale(scale)
                            ) {
                                Text(
                                    text = level,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Obiettivo fitness ----
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                darkTheme = true
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Qual è il tuo obiettivo principale?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        goals.forEach { goal ->
                            val isSelected = selectedGoal.value == goal
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.05f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "goalScale_$goal"
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Neon.Green else Color.White.copy(alpha = 0.05f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedGoal.value = goal }
                                    .scale(scale)
                            ) {
                                Text(
                                    text = goal,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // ---- Riepilogo obiettivo giornaliero ----
            if (dailyGoal.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    darkTheme = true
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Obiettivo giornaliero:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = dailyGoal,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Neon.Green
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---- Pulsante Inizia l'avventura! ----
            Button(
                onClick = {
                    if (username.isBlank()) {
                        usernameError = "Inserisci un nome"
                        return@Button
                    }
                    selectedLevel.value?.let { level ->
                        selectedGoal.value?.let { goal ->
                            onComplete(level, goal, username.trim(), dailyGoal)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = selectedLevel.value != null &&
                        selectedGoal.value != null &&
                        username.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Neon.Green,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Inizia l'avventura!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}