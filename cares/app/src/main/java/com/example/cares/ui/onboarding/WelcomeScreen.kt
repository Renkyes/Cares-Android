// file: ui/onboarding/WelcomeScreen.kt
package com.example.cares.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cares.R
import com.example.cares.ui.theme.Neon

// ================================ WELCOME SCREEN ================================

/**
 * Schermata di benvenuto dell'onboarding.
 *
 * **Scopo:**
 * Accoglie l'utente con un'immagine di sfondo personalizzata e un messaggio di benvenuto.
 * L'utente deve premere il pulsante "Inizia" per proseguire nel flusso di onboarding.
 *
 * **Caratteristiche:**
 * - Immagine di sfondo full-screen con overlay scuro per migliorare la leggibilità.
 * - Titolo e sottotitolo in bianco su sfondo scuro.
 * - Pulsante "Inizia" in stile neon verde.
 * - Layout centrato con spaziatura bilanciata.
 *
 * **Flusso di utilizzo:**
 * 1. L'utente visualizza la schermata di benvenuto.
 * 2. Legge il messaggio introduttivo.
 * 3. Premere "Inizia" per procedere alla schermata successiva dell'onboarding.
 *
 * @param onStartClicked Callback invocato quando l'utente preme il pulsante "Inizia".
 *                       Porta alla schermata successiva del flusso di onboarding.
 */
@Composable
fun WelcomeScreen(
    onStartClicked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // ---- Immagine di sfondo ----
            Image(
                painter = painterResource(id = R.drawable.ic_welcome_screen),
                contentDescription = "Sfondo Welcome Screen",
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
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Spazio superiore per centrare meglio il contenuto
                Spacer(modifier = Modifier.weight(0.3f))

                // ---- Titolo ----
                Text(
                    text = "Benvenuto su Cares!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // ---- Sottotitolo ----
                Text(
                    text = "Il tuo viaggio verso una vita più sana inizia qui.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 48.dp)
                )

                // ---- Pulsante Inizia ----
                Button(
                    onClick = onStartClicked,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Neon.Green,
                        contentColor = Color.Black
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = "🚀 Inizia",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Spazio inferiore per bilanciare il layout
                Spacer(modifier = Modifier.weight(0.3f))
            }
        }
    }
}