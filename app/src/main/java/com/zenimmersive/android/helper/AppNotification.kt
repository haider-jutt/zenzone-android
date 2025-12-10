package com.zenimmersive.android.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zenimmersive.android.R

object AppNotificationManager {
    const val CHANNEL_ID = "Zen Immersive Notifications"
    const val NOTIFICATION_ID = 10011

    private fun createNotificationChannel(context: Context) {

        var notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName: CharSequence = "Zen Immersive Notifications"
            val channelDescription = "Zen Immersive Notifications Channel"
            val channelImportance = NotificationManager.IMPORTANCE_NONE
            val notificationChannel =
                NotificationChannel(CHANNEL_ID, channelName, channelImportance)
            notificationChannel.description = channelDescription
            notificationChannel.importance = NotificationManager.IMPORTANCE_DEFAULT
            notificationChannel.enableVibration(false)

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    fun showNotification(context : Context, notificationId: Int = System.currentTimeMillis().toInt(), notification: Notification) {
        var notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    fun showNotification(context : Context, title : String, message : String, pendingIntent : PendingIntent) {
        var notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(context);
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Replace with your app icon
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    fun cancelNotification(context : Context, notificationId: Int) {
        var notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    fun notify(context : Context, notificationId: Int, notification: Notification) {
        var notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
