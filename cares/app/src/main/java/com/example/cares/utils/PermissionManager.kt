// file: utils/PermissionManager.kt
package com.example.cares.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

// ================================ PERMISSION MANAGER ================================

/**
 * Gestisce la richiesta di un permesso singolo in modo contestuale.
 *
 * **Scopo:**
 * Fornire un componente riutilizzabile che gestisca l'intero ciclo di vita di un permesso:
 * - **Permesso già concesso** → mostra il contenuto.
 * - **Permesso negato (non permanentemente)** → mostra una spiegazione e un pulsante per richiederlo.
 * - **Permesso negato permanentemente** → mostra un messaggio di errore e un pulsante per aprire le impostazioni.
 *
 * **Flusso di utilizzo:**
 * 1. Lo stato del permesso viene gestito tramite [rememberPermissionState] di Accompanist.
 * 2. A seconda dello stato, viene mostrata una UI diversa:
 *    - [PermissionRationaleView] per spiegare il motivo del permesso.
 *    - [PermissionPermanentlyDeniedView] per indirizzare l'utente alle impostazioni.
 * 3. Il contenuto principale ([content]) viene mostrato solo se il permesso è concesso.
 *
 * **Esempio di utilizzo:**
 * ```
 * PermissionManager(
 *     permission = Manifest.permission.CAMERA,
 *     rationaleMessage = "La fotocamera è necessaria per scattare foto durante le missioni."
 * ) {
 *     CameraPreview()
 * }
 * ```
 *
 * @param permission Il permesso da richiedere (es. [Manifest.permission.POST_NOTIFICATIONS]).
 * @param rationaleMessage Messaggio di spiegazione mostrato all'utente prima della richiesta.
 * @param onGranted Callback invocato quando il permesso viene concesso.
 * @param onDenied Callback invocato quando il permesso viene negato (non permanente).
 * @param content Contenuto da mostrare quando il permesso è già concesso.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionManager(
    permission: String,
    rationaleMessage: String,
    onGranted: () -> Unit = {},
    onDenied: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val permissionState = rememberPermissionState(permission)
    val context = LocalContext.current

    when {
        // ---- Caso 1: Permesso già concesso ----
        permissionState.status.isGranted -> {
            onGranted()
            content()
        }

        // ---- Caso 2: Permesso negato permanentemente ----
        permissionState.status.isPermanentlyDenied -> {
            PermissionPermanentlyDeniedView(
                permissionName = getPermissionDisplayName(permission),
                onGoToSettings = { openAppSettings(context) }
            )
        }

        // ---- Caso 3: Permesso negato (non permanentemente) ----
        else -> {
            PermissionRationaleView(
                permissionName = getPermissionDisplayName(permission),
                rationaleMessage = rationaleMessage,
                onRequestPermission = { permissionState.launchPermissionRequest() }
            )
        }
    }
}

// ================================ COMPONENTI UI ================================

/**
 * Schermata di spiegazione che mostra all'utente il motivo per cui il permesso è necessario.
 * Viene mostrata quando il permesso è stato negato ma non permanentemente.
 *
 * **Caratteristiche:**
 * - Card con sfondo surfaceVariant.
 * - Icona 🔒, titolo "NomePermesso necessario".
 * - Messaggio di spiegazione dettagliato.
 * - Pulsante "Concedi permesso" in verde (#4CAF50).
 *
 * @param permissionName Nome leggibile del permesso (es. "Notifiche").
 * @param rationaleMessage Messaggio di spiegazione dettagliato.
 * @param onRequestPermission Callback per richiedere il permesso.
 */
@Composable
fun PermissionRationaleView(
    permissionName: String,
    rationaleMessage: String,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔒 $permissionName necessario",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = rationaleMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50) // Verde per azione positiva
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Concedi permesso")
            }
        }
    }
}

/**
 * Schermata che mostra un messaggio di errore quando il permesso è stato negato permanentemente.
 *
 * **Causa:** L'utente ha spuntato "Non chiedere più" in precedenza.
 * **Soluzione:** L'utente deve abilitare il permesso manualmente dalle impostazioni del sistema.
 *
 * **Caratteristiche:**
 * - Card con sfondo errorContainer.
 * - Icona ⚠️, titolo "NomePermesso bloccato" in rosso.
 * - Messaggio informativo.
 * - Pulsante "Apri impostazioni" in rosso.
 *
 * @param permissionName Nome leggibile del permesso (es. "Notifiche").
 * @param onGoToSettings Callback per aprire le impostazioni del sistema.
 */
@Composable
fun PermissionPermanentlyDeniedView(
    permissionName: String,
    onGoToSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠️ $permissionName bloccato",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hai negato il permesso in modo permanente. Puoi abilitarlo dalle impostazioni del sistema.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGoToSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apri impostazioni")
            }
        }
    }
}

// ================================ FUNZIONI DI ESTENSIONE ================================

/**
 * Estensione per verificare se un permesso è stato negato permanentemente.
 *
 * **Condizione di negazione permanente:**
 * - Il permesso è in stato `Denied`.
 * - `shouldShowRationale` è `false` (l'utente ha spuntato "Non chiedere più").
 *
 * **Utilizzo:**
 * ```
 * if (permissionState.status.isPermanentlyDenied) {
 *     // Mostra schermata di errore con pulsante per impostazioni
 * }
 * ```
 *
 * @return `true` se il permesso è negato permanentemente, `false` altrimenti.
 */
@OptIn(ExperimentalPermissionsApi::class)
val PermissionStatus.isPermanentlyDenied: Boolean
    get() = this is PermissionStatus.Denied && !shouldShowRationale

// ================================ FUNZIONI UTILITY ================================

/**
 * Apre la schermata delle impostazioni dell'app nel sistema.
 * L'utente può abilitare manualmente i permessi negati permanentemente.
 *
 * **Utilizzo:**
 * ```
 * openAppSettings(context)
 * ```
 *
 * @param context Il contesto per avviare l'Intent.
 */
fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}")
    )
    context.startActivity(intent)
}

/**
 * Restituisce il nome leggibile (in italiano) di un permesso Android.
 *
 * **Mappatura dei permessi:**
 * | Permesso | Nome visualizzato |
 * |----------|-------------------|
 * | POST_NOTIFICATIONS | Notifiche |
 * | ACTIVITY_RECOGNITION | Riconoscimento attività |
 * | ACCESS_FINE_LOCATION | Posizione |
 * | ACCESS_COARSE_LOCATION | Posizione approssimativa |
 * | CAMERA | Fotocamera |
 * | RECORD_AUDIO | Microfono |
 * | Altro | Permesso (fallback) |
 *
 * @param permission Il permesso (es. [Manifest.permission.POST_NOTIFICATIONS]).
 * @return Il nome leggibile in italiano.
 */
fun getPermissionDisplayName(permission: String): String {
    return when (permission) {
        Manifest.permission.POST_NOTIFICATIONS -> "Notifiche"
        Manifest.permission.ACTIVITY_RECOGNITION -> "Riconoscimento attività"
        Manifest.permission.ACCESS_FINE_LOCATION -> "Posizione"
        Manifest.permission.ACCESS_COARSE_LOCATION -> "Posizione approssimativa"
        Manifest.permission.CAMERA -> "Fotocamera"
        Manifest.permission.RECORD_AUDIO -> "Microfono"
        else -> "Permesso"
    }
}