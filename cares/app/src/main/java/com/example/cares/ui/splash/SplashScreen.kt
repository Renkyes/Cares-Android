// file: ui/splash/SplashScreen.kt
package com.example.cares.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cares.R
import kotlinx.coroutines.delay

// ================================ SPLASH SCREEN ================================

/**
 * Schermata iniziale (splash screen) dell'applicazione.
 *
 * **Scopo:**
 * Presenta il brand e il logo dell'app tramite un'immagine di sfondo personalizzata.
 * L'utente deve toccare lo schermo per proseguire verso la [LoadingScreen].
 *
 * **Caratteristiche:**
 * - Immagine di sfondo a tutta schermata con il logo integrato.
 * - Overlay scuro semitrasparente (25%) per garantire la leggibilità.
 * - Testo "Tocca per iniziare" con animazione lampeggiante in basso.
 * - Click su qualsiasi punto dello schermo per proseguire.
 *
 * **Flusso di utilizzo:**
 * 1. L'utente apre l'app e visualizza lo splash screen.
 * 2. Il testo "Tocca per iniziare" lampeggia per attirare l'attenzione.
 * 3. L'utente tocca lo schermo in qualsiasi punto.
 * 4. Viene chiamato `onTap()` che porta alla [LoadingScreen].
 *
 * **Animazione:**
 * - Il testo lampeggia con un'alternanza di alpha tra 1.0 e 0.3.
 * - Ogni ciclo dura 1600ms (800ms per stato).
 *
 * @param onTap Callback invocato quando l'utente tocca lo schermo.
 *              Porta alla schermata di caricamento successiva.
 */
@Composable
fun SplashScreen(
    onTap: () -> Unit
) {
    // ================================ ANIMAZIONE ================================

    /**
     * Stato che controlla il lampeggio del testo.
     * Alterna tra true e false ogni 800ms.
     */
    var isBlinking by remember { mutableStateOf(true) }

    /**
     * Alpha del testo che oscilla tra 1.0 e 0.3.
     * Usa un tween di 800ms per una transizione fluida.
     */
    val blinkAlpha by animateFloatAsState(
        targetValue = if (isBlinking) 1f else 0.3f,
        animationSpec = tween(800),
        label = "blink"
    )

    /**
     * Loop infinito che alterna lo stato di lampeggio ogni 800ms.
     */
    LaunchedEffect(Unit) {
        while (true) {
            delay(800L)
            isBlinking = !isBlinking
        }
    }

    // ================================ UI ================================

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onTap() }, // Click su tutta la superficie
        color = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // ---- Immagine di sfondo ----
            Image(
                painter = painterResource(id = R.drawable.ic_splash_screen),
                contentDescription = "Sfondo Splash Screen",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // ---- Overlay scuro per migliorare la leggibilità ----
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )

            // ---- Testo lampeggiante in basso ----
            Text(
                text = "Tocca per iniziare",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = blinkAlpha),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
            )
        }
    }
}