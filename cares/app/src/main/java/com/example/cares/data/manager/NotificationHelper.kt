// file: data/manager/NotificationHelper.kt
package com.example.cares.data.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cares.R
import com.example.cares.ui.main.MainActivity

/**
 * Helper per la creazione e l'invio di notifiche locali.
 *
 * Gestisce la creazione del canale di notifica (richiesto da Android 8+),
 * l'invio di notifiche per eventi specifici (promemoria, completamento missioni,
 * nuovo giorno, benvenuto) e la gestione automatica del permesso POST_NOTIFICATIONS
 * su Android 13+.
 *
 * **Note sulla gestione dei permessi:**
 * - Su Android 13+ (API 33), il permesso [android.Manifest.permission.POST_NOTIFICATIONS]
 *   deve essere concesso per inviare notifiche.
 * - Se il permesso non è concesso, i tentativi di invio vengono ignorati silenziosamente.
 * - La UI (SettingsScreen) si occupa di richiedere il permesso all'utente.
 *
 * @param context Il contesto necessario per accedere ai servizi di sistema.
 */
class NotificationHelper(private val context: Context) {

    // ================================ COSTANTI ================================

    companion object {
        /** ID univoco del canale di notifica. */
        const val CHANNEL_ID = "cares_channel"

        /** Nome visualizzato del canale. */
        const val CHANNEL_NAME = "Cares Notifiche"

        /** Descrizione del canale (mostrata nelle impostazioni di sistema). */
        const val CHANNEL_DESCRIPTION = "Promemoria missioni e aggiornamenti"

        /** ID per identificare le notifiche di promemoria. */
        const val NOTIFICATION_ID_REMINDER = 1001

        /** ID per identificare le notifiche di completamento missione. */
        const val NOTIFICATION_ID_COMPLETED = 1002

        /** ID per identificare le notifiche di nuovo giorno. */
        const val NOTIFICATION_ID_NEW_DAY = 1003
    }

    // ================================ API PUBBLICHE ================================

    /**
     * Crea il canale di notifica, necessario su Android 8+ (Oreo).
     *
     * Il canale viene configurato con priorità alta, vibrazione e luce LED.
     * Questa operazione deve essere eseguita una volta all'avvio dell'app,
     * preferibilmente prima di inviare qualsiasi notifica.
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Invia una notifica di promemoria quando mancano 5 minuti alla scadenza della missione.
     *
     * @param questTitle Il titolo della missione in scadenza.
     */
    fun sendReminderNotification(questTitle: String) {
        buildNotification(
            title = "⏳ Missione in scadenza!",
            message = "Hai solo 5 minuti per iniziare '$questTitle'! Corri! 🏃‍♂️",
            notificationId = NOTIFICATION_ID_REMINDER
        )
    }

    /**
     * Invia una notifica di completamento missione con il quantitativo di XP guadagnato.
     *
     * @param questTitle Il titolo della missione completata.
     * @param xpGained   L'esperienza guadagnata.
     */
    fun sendMissionCompletedNotification(questTitle: String, xpGained: Int) {
        buildNotification(
            title = "✅ Missione completata!",
            message = "Hai completato '$questTitle' e guadagnato $xpGained XP! ⭐",
            notificationId = NOTIFICATION_ID_COMPLETED
        )
    }

    /**
     * Invia una notifica per segnalare che sono disponibili nuove missioni giornaliere.
     */
    fun sendNewDayNotification() {
        buildNotification(
            title = "🌟 Nuove missioni disponibili!",
            message = "Le missioni di oggi ti aspettano! Sali di livello e guadagna XP! 🏆",
            notificationId = NOTIFICATION_ID_NEW_DAY
        )
    }

    // ================================ FUNZIONI PRIVATE ================================

    /**
     * Costruisce e invia una notifica.
     *
     * **Flusso:**
     * 1. Verifica il permesso POST_NOTIFICATIONS su Android 13+.
     * 2. Crea un PendingIntent che apre [MainActivity] al tap sulla notifica.
     * 3. Costruisce la notifica con titolo, messaggio e icona.
     * 4. Invia la notifica tramite [NotificationManagerCompat].
     *
     * **Note di sicurezza:**
     * - Viene usato [PendingIntent.FLAG_IMMUTABLE] per compatibilità con Android 12+.
     * - Le eccezioni di tipo [SecurityException] vengono ignorate silenziosamente
     *   poiché il permesso è già gestito a livello UI.
     *
     * @param title          Titolo della notifica.
     * @param message        Messaggio della notifica.
     * @param notificationId ID univoco per identificare la notifica.
     * @param autoCancel     Se true, la notifica viene cancellata al tap (default: true).
     */
    private fun buildNotification(
        title: String,
        message: String,
        notificationId: Int,
        autoCancel: Boolean = true
    ) {
        // Controllo permesso per Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Se il permesso non è concesso, la notifica non viene inviata
                return
            }
        }

        try {
            // Intent per aprire l'app quando l'utente tocca la notifica
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            // PendingIntent che viene eseguito al tap sulla notifica
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Costruzione della notifica
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(autoCancel)
                .build()

            // Invio della notifica
            NotificationManagerCompat.from(context).notify(notificationId, notification)

        } catch (_: SecurityException) {
            // Il permesso potrebbe essere stato revocato mentre l'app era in esecuzione.
            // Ignoriamo l'eccezione in modo silenzioso: la UI gestisce già la richiesta del permesso.
        }
    }
}