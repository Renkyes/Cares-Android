// file: ui/onboarding/CharacterCustomizationScreen.kt
package com.example.cares.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cares.R
import com.example.cares.ui.theme.Gradients
import com.example.cares.ui.theme.Neon
import kotlinx.coroutines.delay

// ================================ CHARACTER CUSTOMIZATION SCREEN ================================

/**
 * Schermata di personalizzazione del personaggio durante l'onboarding.
 *
 * **Scopo:**
 * Permette all'utente di scegliere il proprio avatar/personaggio tra tre opzioni:
 * - 🧙 Mago
 * - 🦸 Eroe
 * - 🧝 Elfo
 *
 * **Caratteristiche:**
 * - Visualizzazione di immagini (drawable) per ogni personaggio.
 * - Animazione di ingresso con scala (spring).
 * - Evidenziazione del personaggio selezionato con bordo verde neon.
 * - Pulsante "Continua" abilitato solo dopo la selezione.
 *
 * **Flusso di utilizzo:**
 * 1. L'utente visualizza i tre personaggi disponibili.
 * 2. Tocca un personaggio per selezionarlo (evidenziato con bordo verde).
 * 3. Premere "Continua" per procedere allo step successivo dell'onboarding.
 *
 * @param onContinue Callback eseguito quando l'utente preme "Continua".
 *                   Riceve l'emoji del personaggio selezionato (es. "🧙", "🦸", "🧝").
 */
@Composable
fun CharacterCustomizationScreen(
    onContinue: (String) -> Unit
) {
    // ================================ STATO ================================
    val selectedAvatar = remember { mutableStateOf<String?>(null) }

    // Lista degli avatar disponibili con emoji e nomi
    val avatars = listOf("🧙", "🦸", "🧝")
    val avatarNames = listOf("Mago", "Eroe", "Elfo")

    // Mappa per ottenere il drawable da un'emoji
    val avatarDrawables = mapOf(
        "🧙" to R.drawable.ic_mago,
        "🦸" to R.drawable.ic_eroe,
        "🧝" to R.drawable.ic_elfo
    )

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
                .size(400.dp)
                .align(Alignment.TopStart)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Neon.Purple.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        radius = 400f
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
                text = "Scegli il tuo Eroe!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Chi sarà il tuo alter ego?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // ---- Avatar ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                avatars.forEachIndexed { index, avatar ->
                    val isSelected = selectedAvatar.value == avatar
                    val scale by animateFloatAsState(
                        targetValue = if (startAnimation) 1f else 0.7f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "avatarScale_$index"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                    ) {
                        // ---- Contenitore dell'avatar ----
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Neon.Green.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Neon.Green else Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatar.value = avatar }
                                .scale(if (isSelected) 1.05f else 1f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Immagine del personaggio (drawable)
                            Image(
                                painter = painterResource(id = avatarDrawables[avatar] ?: R.drawable.ic_mago),
                                contentDescription = avatarNames[index],
                                modifier = Modifier.size(64.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ---- Nome del personaggio ----
                        Text(
                            text = avatarNames[index],
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )

                        // ---- Badge "Selezionato" ----
                        if (isSelected) {
                            Text(
                                text = "✅ Selezionato",
                                fontSize = 11.sp,
                                color = Neon.Green
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ---- Pulsante Continua ----
            Button(
                onClick = {
                    selectedAvatar.value?.let { avatar ->
                        onContinue(avatar)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = selectedAvatar.value != null,
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