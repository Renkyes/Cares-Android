// file: ui/streak/StreakRiskDialog.kt
package com.example.cares.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cares.ui.theme.Neon

// ================================ STREAK RISK DIALOG ================================

/**
 * Dialog che appare quando la streak dell'utente è in pericolo.
 *
 * **Scopo:**
 * Avvisa l'utente che ha saltato un giorno di attività e la sua streak
 * (serie di giorni consecutivi) è a rischio di essere persa.
 *
 * **Funzionalità:**
 * - Mostra la streak corrente a rischio.
 * - Indica se l'utente ha uno scudo disponibile.
 * - Offre due opzioni:
 *   1. **Salva streak**: consuma uno scudo per mantenere la streak.
 *   2. **Perdi streak**: la streak viene resettata a 0.
 *
 * **Stile Duolingo:**
 * Il dialog segue lo stile di Duolingo per il recupero della streak,
 * dando all'utente la possibilità di salvarla con uno scudo.
 *
 * **Flusso di utilizzo:**
 * 1. L'utente salta un giorno di attività.
 * 2. Al successivo avvio dell'app, viene rilevato il mancato completamento.
 * 3. Questo dialog viene mostrato automaticamente.
 * 4. L'utente decide se usare uno scudo o perdere la streak.
 *
 * @param currentStreak Il valore corrente della streak in pericolo.
 * @param hasShield Se `true`, l'utente ha uno scudo disponibile da usare.
 * @param onSaveStreak Callback eseguito quando l'utente sceglie di salvare la streak.
 *                     Consuma uno scudo e mantiene la streak.
 * @param onLoseStreak Callback eseguito quando l'utente sceglie di perdere la streak.
 *                     La streak viene resettata a 0.
 */
@Composable
fun StreakRiskDialog(
    currentStreak: Int,
    hasShield: Boolean,
    onSaveStreak: () -> Unit,
    onLoseStreak: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            // Non permettere il dismiss con back press o tap fuori
            // L'utente deve prendere una decisione obbligatoria
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E) // Blu scuro come sfondo
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ---- Icona di avviso ----
                Text(
                    text = "⚠️",
                    fontSize = 56.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // ---- Titolo ----
                Text(
                    text = "Hai saltato un giorno!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ---- Messaggio principale ----
                Text(
                    text = "La tua streak di $currentStreak giorni è a rischio! Usa uno scudo per salvarla.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ---- Stato degli scudi disponibili ----
                Text(
                    text = if (hasShield) "🛡️ Hai uno scudo disponibile" else "🛡️ Non hai scudi disponibili",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasShield) Neon.Green else Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ---- Pulsanti di azione ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ---- Pulsante "Salva streak" ----
                    Button(
                        onClick = onSaveStreak,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        enabled = hasShield,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasShield) Neon.Green else Color.White.copy(alpha = 0.1f),
                            contentColor = if (hasShield) Color.Black else Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (hasShield) "🛡️ Salva streak" else "🔒 Bloccato",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ---- Pulsante "Perdi streak" ----
                    Button(
                        onClick = onLoseStreak,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF1744).copy(alpha = 0.2f),
                            contentColor = Color(0xFFFF1744)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "💔 Perdi streak",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}