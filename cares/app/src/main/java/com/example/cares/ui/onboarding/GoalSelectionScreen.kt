// file: ui/onboarding/GoalSelectionScreen.kt
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cares.ui.theme.Gradients
import com.example.cares.ui.theme.Neon
import kotlinx.coroutines.delay

// ================================ GOAL SELECTION SCREEN ================================

/**
 * Schermata di selezione dell'obiettivo quotidiano durante l'onboarding.
 *
 * **Scopo:**
 * Permette all'utente di scegliere un obiettivo giornaliero tra una lista di opzioni
 * predefinite o di crearne uno personalizzato.
 *
 * **Obiettivi predefiniti:**
 * - 🚶 5000 passi / 10000 passi
 * - 💪 50 push-up / 100 push-up
 * - 🏃 15 minuti di corsa / 30 minuti di corsa
 * - 🧘 10 minuti di yoga / 20 minuti di yoga
 * - 📚 Leggi 20 pagine
 * - 🎯 Personalizzato (libero)
 *
 * **Caratteristiche:**
 * - Visualizzazione a griglia con FlowRow.
 * - Animazione di ingresso con scala (spring).
 * - Evidenziazione dell'obiettivo selezionato con colore verde neon.
 * - Campo di testo per l'obiettivo personalizzato.
 * - Pulsante "Continua" abilitato solo dopo la selezione valida.
 *
 * **Flusso di utilizzo:**
 * 1. L'utente visualizza la lista degli obiettivi disponibili.
 * 2. Tocca un obiettivo per selezionarlo (evidenziato in verde).
 * 3. Se seleziona "🎯 Personalizzato", appare un campo di testo.
 * 4. Inserisce il proprio obiettivo personalizzato.
 * 5. Premere "Continua" per procedere allo step successivo dell'onboarding.
 *
 * @param onGoalSelected Callback eseguito quando l'utente preme "Continua".
 *                       Riceve la stringa dell'obiettivo selezionato (predefinito o personalizzato).
 */
@Composable
fun GoalSelectionScreen(
    onGoalSelected: (String) -> Unit
) {
    // ================================ STATO ================================
    var selectedGoal by remember { mutableStateOf<String?>(null) }
    var customGoal by remember { mutableStateOf("") }
    var startAnimation by remember { mutableStateOf(false) }

    // ================================ ANIMAZIONI ================================
    LaunchedEffect(Unit) {
        delay(200L) // Leggero ritardo per l'animazione di ingresso
        startAnimation = true
    }

    // ================================ DATI ================================
    val goals = listOf(
        "🚶 5000 passi",
        "🚶 10000 passi",
        "💪 50 push-up",
        "💪 100 push-up",
        "🏃 15 minuti di corsa",
        "🏃 30 minuti di corsa",
        "🧘 10 minuti di yoga",
        "🧘 20 minuti di yoga",
        "📚 Leggi 20 pagine",
        "🎯 Personalizzato"
    )

    // ================================ UI ================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Gradients.mainBackground(true))
    ) {
        // ---- Glow decorativo in background ----
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopStart)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Neon.Green.copy(alpha = 0.08f), Color.Transparent),
                        radius = 300f
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
                text = "Scegli il tuo obiettivo quotidiano",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cosa vuoi raggiungere ogni giorno?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // ---- Griglia di obiettivi ----
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                goals.forEach { goal ->
                    val isSelected = selectedGoal == goal
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
                            .clickable { selectedGoal = goal }
                            .scale(scale)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = goal,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ---- Campo per obiettivo personalizzato ----
            if (selectedGoal == "🎯 Personalizzato") {
                OutlinedTextField(
                    value = customGoal,
                    onValueChange = { customGoal = it },
                    label = { Text("Inserisci il tuo obiettivo") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Neon.Green,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = Neon.Green
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ---- Pulsante Continua ----
            Button(
                onClick = {
                    val finalGoal = if (selectedGoal == "🎯 Personalizzato") {
                        customGoal.ifBlank { "Obiettivo personalizzato" }
                    } else {
                        selectedGoal ?: return@Button
                    }
                    onGoalSelected(finalGoal)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = selectedGoal != null &&
                        (selectedGoal != "🎯 Personalizzato" || customGoal.isNotBlank()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Neon.Green,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continua",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}