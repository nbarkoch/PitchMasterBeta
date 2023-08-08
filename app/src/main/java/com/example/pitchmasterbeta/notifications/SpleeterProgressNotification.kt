package com.example.pitchmasterbeta.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pitchmasterbeta.R

class SpleeterProgressNotification(context: Context) {
    private var notificationManager: NotificationManager
    private var notificationBuilder: NotificationCompat.Builder

    init {
        // Create a notification manager
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel (required for Android 8.0 or higher)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "My Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Create a notification builder
        notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Progress Notification")
            .setContentText("Upload in progress")
            .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
            .setProgress(100, 0, true).setSound(null)
    }

    fun showNotification() {
        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    @JvmOverloads
    fun updateProgress(progress: Int, message: String? = null) {
        // Update the progress of the notification
        notificationBuilder.setProgress(100, progress, false)
        if (message != null) {
            notificationBuilder.setContentText(message)
        }
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    fun hideNotification() {
        // Hide the notification
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        private const val CHANNEL_ID = "my_channel"
        private const val NOTIFICATION_ID = 1
    }
}