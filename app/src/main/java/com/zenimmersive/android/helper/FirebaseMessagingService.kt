package com.zenimmersive.android.helper

import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.media3.common.util.UnstableApi
import com.zenimmersive.android.ui.DashboardActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class FirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "Logger"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token: $token")
        // Send token to your server if necessary
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Received Message: ${message.data}")

        val title = message.notification?.title ?: "Zen Immersive"
        val body = message.notification?.body ?: "You have a new notification"

        // Handle sound URI
        message.data["sound"]?.let { soundUriString ->
            try {
                val soundUri = Uri.parse(soundUriString)
                val ringtone = RingtoneManager.getRingtone(applicationContext, soundUri)
                ringtone?.play()
            } catch (e: Exception) {
                LogSystem.e(TAG, "Error playing sound: ${e.message}")
            }
        }

        // Open DashboardActivity on notification click
        sendNotification(title, body)
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun sendNotification(title: String, message: String) {

        val intent = Intent(this, DashboardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        AppNotificationManager.showNotification(applicationContext, title, message, pendingIntent)

    }
}
