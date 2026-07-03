// file: ui/settings/SettingsScreen.kt
package com.example.cares.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.cares.data.manager.FirebaseAuthManager
import com.example.cares.data.manager.MusicPlayerManager
import com.example.cares.data.manager.UserPreferencesManager
import com.example.cares.data.repository.CaresRepository
import com.example.cares.ui.main.MainScreen
import com.example.cares.ui.theme.Gradients
import com.example.cares.ui.theme.Neon
import com.example.cares.ui.theme.GlassCard
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ================================ SETTINGS SCREEN ================================

/**
 * Schermata delle impostazioni dell'applicazione.
 *
 * **Sezioni:**
 * 1. **Tema**: alternanza tema scuro/chiaro.
 * 2. **Musica**: attivazione/disattivazione musica di sottofondo.
 * 3. **Permessi**: stato e richiesta dei permessi (notifiche, attività fisica, fotocamera, posizione).
 * 4. **Debug**: accesso agli strumenti di debug.
 * 5. **Account**: logout e reset completo dei dati.
 *
 * **Caratteristiche:**
 * - Layout scrollabile con LazyColumn.
 * - Effetti glow in background per estetica neon.
 * - Card glassmorphism per ogni sezione.
 * - Gestione permessi con Accompanist.
 * - Persistenza delle preferenze (tema, musica).
 *
 * @param isDarkTheme Se `true`, applica il tema scuro.
 * @param onToggleTheme Callback per alternare il tema.
 * @param onLogout Callback per il logout.
 * @param navController Controller per la navigazione verso DebugScreen.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val preferencesManager = remember { UserPreferencesManager(context) }

    // ================================ PERMESSI ================================

    // Notifiche (Android 13+)
    val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // Android < 13: permesso concesso di default
    }

    // Attività fisica (Android 10+)
    val activityRecognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // Android < 10: permesso non richiesto
    }

    // Fotocamera (Accompanist)
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Posizione (Accompanist)
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)

    // ================================ FUNZIONI ================================

    /**
     * Esegue il logout dell'utente.
     * Pulisce i dati e sincronizza lo stato.
     */
    fun performLogout() {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = CaresRepository(context)
            repository.logout()
            onLogout()
        }
    }

    /**
     * Resetta completamente tutti i dati dell'applicazione.
     * Cancella preferenze, dati utente e logout.
     */
    fun performFullReset() {
        CoroutineScope(Dispatchers.IO).launch {
            preferencesManager.clearAll()
            FirebaseAuthManager.signOut()
            onLogout()
        }
    }

    // ================================ UI ================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Gradients.mainBackground(isDarkTheme))
    ) {
        // ---- Effetti glow in background ----
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Neon.Cyan.copy(alpha = 0.08f), Color.Transparent),
                        radius = 300f
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomStart)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Neon.Purple.copy(alpha = 0.08f), Color.Transparent),
                        radius = 250f
                    )
                )
        )

        // ---- Contenuto scrollabile ----
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- Header ----
            item {
                Text(
                    text = "Impostazioni",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // ---- Tema Scuro/Chiaro ----
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), darkTheme = isDarkTheme) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDarkTheme) "Tema Scuro" else "Tema Chiaro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = !isDarkTheme,
                            onCheckedChange = { onToggleTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Neon.Green,
                                checkedTrackColor = Neon.Green.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ---- Musica di sottofondo ----
            item {
                val musicEnabled = remember { mutableStateOf(MusicPlayerManager.isMusicEnabled()) }

                GlassCard(modifier = Modifier.fillMaxWidth(), darkTheme = isDarkTheme) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Musica di sottofondo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = musicEnabled.value,
                            onCheckedChange = { checked ->
                                musicEnabled.value = checked
                                MusicPlayerManager.setEnabled(context, checked)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ---- Permessi ----
            item {
                Text(
                    text = "Permessi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            // Notifiche
            item {
                PermissionStatusRow(
                    icon = "🔔",
                    name = "Notifiche",
                    isGranted = notificationsGranted,
                    isDarkTheme = isDarkTheme
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Attività fisica
            item {
                PermissionStatusRow(
                    icon = "👟",
                    name = "Attività fisica",
                    isGranted = activityRecognitionGranted,
                    isDarkTheme = isDarkTheme
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Fotocamera
            item {
                PermissionStatusRow(
                    icon = "📷",
                    name = "Fotocamera",
                    isGranted = cameraPermissionState.status.isGranted,
                    isDarkTheme = isDarkTheme,
                    onRequest = { cameraPermissionState.launchPermissionRequest() }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Posizione
            item {
                PermissionStatusRow(
                    icon = "📍",
                    name = "Posizione",
                    isGranted = locationPermissionState.status.isGranted,
                    isDarkTheme = isDarkTheme,
                    onRequest = { locationPermissionState.launchPermissionRequest() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ---- Debug ----
            item {
                Text(
                    text = "Debug",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                GlassButton(
                    text = "Strumenti di Debug",
                    onClick = { navController.navigate(MainScreen.DEBUG.route) },
                    modifier = Modifier.fillMaxWidth(),
                    color = Neon.Cyan
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ---- Account ----
            item {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                GlassButton(
                    text = "Logout",
                    onClick = { performLogout() },
                    modifier = Modifier.fillMaxWidth(),
                    color = Neon.Orange
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ---- Reset Completo ----
            item {
                GlassButton(
                    text = "Reset Completo (Cancella tutti i dati)",
                    onClick = { performFullReset() },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFF1744)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ---- Versione ----
            item {
                Text(
                    text = "Versione 1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ================================ PERMISSION STATUS ROW ================================

/**
 * Riga che mostra lo stato di un permesso.
 *
 * **Caratteristiche:**
 * - Icona e nome del permesso.
 * - Stato "✅ Concesso" o "🔴 Negato".
 * - Se il permesso non è concesso e `onRequest` è fornito, mostra un indicatore "👆" per richiederlo.
 * - Click sulla riga richiede il permesso (se `onRequest` è fornito).
 *
 * @param icon Emoji rappresentativa del permesso.
 * @param name Nome del permesso.
 * @param isGranted Se `true`, il permesso è concesso.
 * @param isDarkTheme Se `true`, applica il tema scuro.
 * @param onRequest Callback opzionale per richiedere il permesso.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionStatusRow(
    icon: String,
    name: String,
    isGranted: Boolean,
    isDarkTheme: Boolean,
    onRequest: (() -> Unit)? = null
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onRequest != null) Modifier.clickable { onRequest() }
                else Modifier
            ),
        darkTheme = isDarkTheme
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = icon, fontSize = 20.sp)
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isGranted) "✅ Concesso" else "🔴 Negato",
                    fontSize = 13.sp,
                    color = if (isGranted) Neon.Green else Color(0xFFFF1744)
                )
                if (onRequest != null && !isGranted) {
                    Text(
                        text = "👆",
                        fontSize = 14.sp,
                        color = Neon.Cyan
                    )
                }
            }
        }
    }
}

// ================================ GLASS BUTTON ================================

/**
 * Pulsante in stile glassmorphism per le azioni delle impostazioni.
 *
 * @param text Testo del pulsante.
 * @param onClick Callback al click.
 * @param modifier Modificatori da applicare.
 * @param color Colore di accento (default: Neon.Cyan).
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Neon.Cyan
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = null
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            letterSpacing = 0.3.sp
        )
    }
}