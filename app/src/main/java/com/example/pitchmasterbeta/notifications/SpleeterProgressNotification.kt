package com.example.pitchmasterbeta.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.pitchmasterbeta.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SpleeterProgressNotification(context: Context) {
    private var notificationManager: NotificationManager
    private var notificationBuilder: NotificationCompat.Builder
    private var lastProgress = 0
    private var tempJob: Job? = null

    init {
        // Create a notification manager
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Create a notification channel (required for Android 8.0 or higher)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Spleeter Channel", NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        lastProgress = 0
        // Create a notification builder
        notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Progress Notification").setContentText("Upload in progress")
            .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
            .setProgress(MAX_PROGRESS, lastProgress, true).setSound(null)
    }

    fun showNotification() {
        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    @JvmOverloads
    fun updateProgress(progress: Int, message: String? = null) {
        // Update the progress of the notification
        notificationBuilder.setProgress(MAX_PROGRESS, progress, false)
        lastProgress = progress
        if (message != null) {
            notificationBuilder.setContentText(message)
        }
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    @JvmOverloads
    fun updateProgress(finalProgress: Int, message: String? = null, duration: Double) {
        tempJob?.run { cancel() }
        tempJob = CoroutineScope(Dispatchers.IO).launch {
            val progressChunkDuration = 1000L // one second
            var currentDuration = 0L
            val oldProgress = lastProgress
            val progressStep = ((finalProgress - lastProgress) / (duration / progressChunkDuration))
            var numSteps = 1
            while (lastProgress <= finalProgress && currentDuration < duration) {
                lastProgress = oldProgress + (numSteps++ * progressStep).toInt()
                currentDuration += progressChunkDuration
                if (message != null) {
                    notificationBuilder.setContentText(message)
                }
                notificationBuilder.setProgress(MAX_PROGRESS, lastProgress, false)
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
                delay(progressChunkDuration)
                Log.d("Notification manager", "progress: $lastProgress%")
            }
        }
    }

    fun hideNotification() {
        tempJob?.run { cancel() }
        tempJob = null
        // Hide the notification
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        private const val CHANNEL_ID = "spleeter_service_process"
        private const val NOTIFICATION_ID = 1
        private const val MAX_PROGRESS = 100
    }
}