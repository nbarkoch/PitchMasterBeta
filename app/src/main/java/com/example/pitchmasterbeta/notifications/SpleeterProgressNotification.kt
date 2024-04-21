package com.example.pitchmasterbeta.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.pitchmasterbeta.MainActivity
import com.example.pitchmasterbeta.R
import com.example.pitchmasterbeta.model.StudioSharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SpleeterProgressNotification(private val context: Context, private val audioPath: String) {
    private var notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var notificationBuilder: NotificationCompat.Builder
    private var lastProgress = 0
    private var tempJob: Job? = null

    init {
        // Create a notification manager
        // Create a notification channel (required for Android 8.0 or higher)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(
                PROGRESS_CHANNEL_ID, "Spleeter Channel Progression", NotificationManager.IMPORTANCE_DEFAULT
            ))
            notificationManager.createNotificationChannel(NotificationChannel(
                COMPLETION_CHANNEL_ID, "Spleeter Channel Completion", NotificationManager.IMPORTANCE_DEFAULT
            ))
        }
        lastProgress = 0

        // Create a notification builder
        notificationBuilder = NotificationCompat.Builder(context, PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Progress Notification").setContentText("Upload in progress")
            .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
            .setProgress(MAX_PROGRESS, lastProgress, true).setSound(null)
            .setOngoing(false)
    }

//    fun showNotification() {
//        // Show the notification
//        notificationManager.notify(PROGRESS_NOTIFICATION_ID, notificationBuilder.build())
//    }

    @JvmOverloads
    fun updateProgress(progress: Int, message: String? = null) {
        // Update the progress of the notification
        tempJob?.run { cancel() }
        tempJob = CoroutineScope(Dispatchers.IO).launch {
            notificationBuilder.setContentIntent(createPendingIntent(progress, message))
            if (message != null) {
                notificationBuilder.setContentText(message)
            }
            notificationBuilder.setProgress(MAX_PROGRESS, progress, false)
            lastProgress = progress
            notificationManager.notify(PROGRESS_NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    private fun createPendingIntent(
        progress: Int,
        message: String? = null,
        duration: Double? = null
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("DL", "11")
            putExtra("message", message)
            putExtra("progress", progress)
            putExtra("duration", duration)
            putExtra("audioPath", audioPath)
        }
        return PendingIntent.getActivity(
            context, REQUEST_CODE_PROGRESS, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    @JvmOverloads
    fun updateProgress(finalProgress: Int, message: String? = null, duration: Double) {
        tempJob?.run { cancel() }
        tempJob = CoroutineScope(Dispatchers.IO).launch {
            val progressChunkDuration = 100L // milliseconds
            var currentDuration = 0L
            val oldProgress = lastProgress
            val progressStep = ((finalProgress - lastProgress) / (duration / progressChunkDuration))
            var numSteps = 1
            notificationBuilder.setContentIntent(createPendingIntent(finalProgress, message, duration))
            notificationManager.notify(PROGRESS_NOTIFICATION_ID, notificationBuilder.build())
            while (lastProgress <= finalProgress && currentDuration < duration) {
                lastProgress = oldProgress + (numSteps++ * progressStep).toInt()
                currentDuration += progressChunkDuration
                if (message != null) {
                    notificationBuilder.setContentText(message)
                }
                notificationBuilder.setProgress(MAX_PROGRESS, lastProgress, false)
                notificationManager.notify(PROGRESS_NOTIFICATION_ID, notificationBuilder.build())
                delay(progressChunkDuration)
                Log.d("Notification manager", "progress: $lastProgress%")
            }
        }
    }

    fun hideNotification() {
        tempJob?.run { cancel() }
        // Hide the notification
        notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
    }

    fun buildNotification(): Notification {
        return notificationBuilder.build()
    }

//    fun setCompletionIntent(karaokeRef: StudioSharedPreferences.KaraokeRef, deepLink: String) {
//        lastProgress = 100
//        val intent = Intent(context, MainActivity::class.java).apply {
//            val gson = Gson()
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            putExtra("ref", gson.toJson(karaokeRef))
//            putExtra("audioPath", audioPath)
//            putExtra("DL", deepLink)
//        }
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        val pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE_FINISH, intent,
//            PendingIntent.FLAG_IMMUTABLE)
//        notificationBuilder.setContentIntent(pendingIntent)
//        notificationBuilder.setAutoCancel(true)
//        notificationBuilder.setProgress(0, 0, false)
//        notificationBuilder.setContentText("Karaoke is ready!")
//        notificationManager.notify(PROGRESS_NOTIFICATION_ID, notificationBuilder.build())
//    }

    fun newNotificationIntent(karaokeRef: StudioSharedPreferences.KaraokeRef, deepLink: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            val gson = Gson()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("ref", gson.toJson(karaokeRef))
            putExtra("audioPath", audioPath)
            putExtra("DL", deepLink)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE_FINISH, intent,
            PendingIntent.FLAG_IMMUTABLE)

        notificationBuilder = NotificationCompat.Builder(context, COMPLETION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Progress Notification").setContentText("Karaoke is ready!")
            .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
            .setSound(null)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notificationBuilder.build())
    }

    companion object {
        const val PROGRESS_CHANNEL_ID = "spleeter_service_process"
        const val COMPLETION_CHANNEL_ID = "spleeter_service_completed"
        const val PROGRESS_NOTIFICATION_ID = 8172
        const val COMPLETION_NOTIFICATION_ID = 8173
        private const val MAX_PROGRESS = 100
        const val REQUEST_CODE_PROGRESS = 2112
        const val REQUEST_CODE_FINISH = 22323223
    }
}