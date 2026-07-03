// file: ui/splash/LoadingScreen.kt
package com.example.cares.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cares.R
import kotlinx.coroutines.delay

// ================================ LOADING SCREEN ================================

/**
 * Schermata di caricamento con animazione di progresso.
 *
 * **Scopo:**
 * Mostra una schermata di caricamento mentre l'applicazione si prepara.
 * Include una barra di progresso, percentuale, frasi motivazionali e puntini animati.
 *
 * **Caratteristiche:**
 * - Immagine di sfondo full-screen con overlay scuro.
 * - Barra di progresso lineare che si riempie progressivamente.
 * - Percentuale di caricamento in tempo reale.
 * - Frasi motivazionali che cambiano ogni 1.5 secondi.
 * - Puntini animati che indicano il progresso del caricamento.
 * - Transition fluida al completamento.
 *
 * **Flusso di utilizzo:**
 * 1. La schermata viene mostrata dopo lo splash screen.
 * 2. La barra di progresso si riempie da 0 a 100% in circa 6.25 secondi.
 * 3. Le frasi motivazionali cambiano ogni 1.5 secondi.
 * 4. Al completamento, viene chiamato `onLoadingComplete()`.
 *
 * **Tempi di animazione:**
 * - Incremento progresso: 0.004 ogni 25ms → ~6.25 secondi per il completamento.
 * - Cambio frase: ogni 1500ms.
 * - Ritardo finale: 300ms prima di chiamare `onLoadingComplete()`.
 *
 * @param onLoadingComplete Callback eseguito quando il caricamento è completato.
 *                          Porta alla schermata successiva (Home o Onboarding).
 */
@Composable
fun LoadingScreen(
    onLoadingComplete: () -> Unit
) {
    // ================================ STATO ================================
    var progress by remember { mutableStateOf(0f) }
    var currentPhraseIndex by remember { mutableStateOf(0) }

    // ================================ FRASI MOTIVAZIONALI ================================
    val phrases = listOf(
        "Preparati a diventare la migliore versione di te stesso!",
        "Il viaggio verso il benessere inizia qui.",
        "Ogni passo conta, ogni allenamento ti avvicina al traguardo.",
        "La costanza è la chiave del successo.",
        "Sii più forte della tua scusa più forte.",
        "Non fermarti mai, il traguardo è più vicino di quanto pensi!",
        "Un piccolo passo oggi, un grande salto domani.",
        "Il tuo corpo è il tuo tempio, prenditene cura."
    )

    // ================================ EFFETTI ================================

    /**
     * Anima la barra di progresso da 0 a 1.
     * Il caricamento richiede circa 6.25 secondi (0.004 * 25ms = 0.1 al secondo).
     */
    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(25L)
            progress += 0.004f
            if (progress > 1f) progress = 1f
        }
        delay(300L) // Piccolo ritardo per permettere la transizione
        onLoadingComplete()
    }

    /**
     * Cambia la frase motivazionale ogni 1.5 secondi.
     */
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500L)
            currentPhraseIndex = (currentPhraseIndex + 1) % phrases.size
        }
    }

    // ================================ UI ================================

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // ---- Immagine di sfondo ----
            Image(
                painter = painterResource(id = R.drawable.ic_loading_screen),
                contentDescription = "Sfondo caricamento",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // ---- Overlay scuro per migliorare la leggibilità ----
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            // ---- Contenuto sovrapposto ----
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Spazio superiore per spingere il contenuto verso il basso
                Spacer(modifier = Modifier.height(700.dp))

                // ---- Barra di progresso ----
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.medium),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ---- Percentuale ----
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ---- Frase motivazionale ----
                Text(
                    text = phrases[currentPhraseIndex],
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(horizontal = 8.dp)
                        .height(60.dp) // Altezza fissa per evitare spostamenti
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ---- Puntini animati ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(3) { index ->
                        val dotScale by animateFloatAsState(
                            targetValue = if (progress > 0.3f + index * 0.2f) 1.2f else 0.8f,
                            animationSpec = tween(
                                durationMillis = 500,
                                delayMillis = (index * 150L).toInt()
                            ),
                            label = "dot_$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .padding(4.dp)
                                .background(
                                    color = Color.White.copy(
                                        alpha = if (progress > 0.3f + index * 0.2f) 1f else 0.4f
                                    ),
                                    shape = CircleShape
                                )
                                .scale(dotScale)
                        )
                    }
                }

                // Spazio inferiore per bilanciare il layout
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}