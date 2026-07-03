// file: ui/celebration/StreakCelebrationScreen.kt
package com.example.cares.ui.celebration

import android.view.View
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.cares.ui.theme.Neon
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Schermata di celebrazione per il raggiungimento di un nuovo streak.
 *
 * **Caratteristiche:**
 * - Animazione di conteggio progressivo dello streak.
 * - Effetto di rimbalzo sul numero durante il conteggio.
 * - Emoji e messaggi personalizzati in base al valore dello streak.
 * - Particelle animate che fluttuano sullo sfondo.
 * - Effetto glow pulsante attorno all'emoji.
 * - Barra di progresso che mostra l'avanzamento del conteggio.
 *
 * **Flusso di utilizzo:**
 * 1. [StreakCelebrationManager] attiva la celebrazione con [triggerCelebration].
 * 2. La schermata viene mostrata come overlay a schermo intero.
 * 3. L'animazione di conteggio parte da 0 fino al nuovo streak.
 * 4. L'utente può chiudere la celebrazione premendo il pulsante "Continua!".
 *
 * @param newStreak Il nuovo valore dello streak da celebrare.
 * @param onDismiss Callback eseguito quando la celebrazione viene chiusa.
 */
@Composable
fun StreakCelebrationScreen(
    newStreak: Int,
    onDismiss: () -> Unit
) {
    val view = LocalView.current

    // ================================ NASCONDI LE BARRE DI SISTEMA ================================

    DisposableEffect(Unit) {
        val window = (view.context as? androidx.activity.ComponentActivity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }

        // Nasconde le barre di sistema per un'esperienza immersiva
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            // Ripristina le barre di sistema quando la schermata viene chiusa
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // ================================ ANIMAZIONE DI CONTEGGIO ================================

    var displayedStreak by remember { mutableStateOf(0) }
    var isBouncing by remember { mutableStateOf(false) }

    // Anima il conteggio da 0 fino al nuovo streak
    LaunchedEffect(newStreak) {
        for (i in 0..newStreak) {
            displayedStreak = i
            isBouncing = true
            delay(100L)
            isBouncing = false
        }
    }

    // Animazione di scala per il numero (rimbalzo ad ogni incremento)
    val numberScale by animateFloatAsState(
        targetValue = if (isBouncing) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "numberScale"
    )

    // Animazione di respiro per l'emoji (scala che aumenta e diminuisce)
    val emojiScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emojiScale"
    )

    // Animazione del glow (intensità che pulsa)
    val glowAlpha by animateFloatAsState(
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // ================================ CONTENUTO PERSONALIZZATO ================================

    // Sceglie emoji, titolo e messaggio in base al valore dello streak
    val (emoji, title, message) = when {
        newStreak == 1 -> Triple("🚀", "Primo giorno!", "Inizia la leggenda!")
        newStreak < 5 -> Triple("🔥", "Stai andando forte!", "Continua così, ogni giorno conta!")
        newStreak < 10 -> Triple("⚡", "Guerriero della costanza!", "La tua dedizione è impressionante!")
        newStreak < 20 -> Triple("🏆", "Campione!", "La tua costanza ti sta portando lontano!")
        newStreak < 30 -> Triple("⭐", "Sei una stella!", "Stai per diventare una leggenda!")
        else -> Triple("👑", "LEGENDA!", "30 giorni di costanza! Sei un'icona!")
    }

    // ================================ PARTICHELLE ANIMATE ================================

    // Genera 30 particelle con proprietà casuali
    val particles = remember {
        List(30) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 6f + 2f,
                speed = Random.nextFloat() * 0.5f + 0.2f,
                phase = Random.nextFloat() * 360f
            )
        }
    }

    // ================================ UI ================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Neon.Green.copy(alpha = 0.15f + glowAlpha * 0.15f),
                        Color(0xFF1A1A2E),
                        Color(0xFF0A0A0A)
                    ),
                    radius = 800f,
                    center = Offset(300f, 300f)
                )
            )
    ) {
        // Sfondo con particelle animate
        particles.forEach { particle ->
            ParticleView(
                particle = particle,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Contenuto principale
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Emoji con glow
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(emojiScale),
                contentAlignment = Alignment.Center
            ) {
                // Glow pulsante
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(1f + glowAlpha * 0.2f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Neon.Green.copy(alpha = 0.3f * glowAlpha),
                                    Color.Transparent
                                ),
                                radius = 100f
                            ),
                            shape = CircleShape
                        )
                )
                Text(
                    text = emoji,
                    fontSize = 80.sp,
                    modifier = Modifier.scale(emojiScale)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Titolo della celebrazione
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Numero dello streak con animazione di conteggio
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayedStreak.toString(),
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    color = Neon.Green,
                    letterSpacing = 4.sp,
                    modifier = Modifier.scale(numberScale)
                )
                Text(
                    text = " giorni",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            // Barra di progresso del conteggio
            LinearProgressIndicator(
                progress = { displayedStreak.toFloat() / newStreak.toFloat() },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(4.dp),
                color = Neon.Green,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Messaggio motivazionale
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Pulsante per chiudere la celebrazione
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Neon.Green,
                    contentColor = Color.Black
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "✨ Continua!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ================================ MODELLO E COMPONENTE PARTICHELLA ================================

/**
 * Rappresenta una particella animata che fluttua nello sfondo.
 *
 * @property x Posizione orizzontale iniziale (0-1).
 * @property y Posizione verticale iniziale (0-1).
 * @property size Dimensione della particella in dp.
 * @property speed Velocità di movimento.
 * @property phase Fase iniziale per l'oscillazione.
 */
private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val phase: Float
)

/**
 * Componente che visualizza una particella con movimento oscillante.
 *
 * La particella si muove seguendo un pattern sinusoidale sia orizzontalmente
 * che verticalmente, creando un effetto di fluttuazione organica.
 *
 * @param particle I dati della particella.
 * @param modifier Modificatori da applicare al contenitore.
 */
@Composable
private fun ParticleView(
    particle: Particle,
    modifier: Modifier = Modifier
) {
    var time by remember { mutableStateOf(0f) }

    // Aggiorna il tempo ad ogni frame per l'animazione
    LaunchedEffect(Unit) {
        while (true) {
            delay(16L) // ~60 FPS
            time += 0.02f
        }
    }

    // Calcola lo spostamento sinusoidale
    val offsetX = sin(time * particle.speed + particle.phase) * 20f
    val offsetY = cos(time * particle.speed * 0.7f + particle.phase) * 20f
    val alpha = 0.3f + 0.5f * (sin(time * particle.speed + particle.phase) + 1f) / 2f

    Box(
        modifier = modifier
            .offset(
                x = (particle.x * 1000f + offsetX).dp,
                y = (particle.y * 2000f + offsetY).dp
            )
            .size(particle.size.dp)
            .background(
                color = Neon.Green.copy(alpha = alpha * 0.5f),
                shape = CircleShape
            )
    )
}