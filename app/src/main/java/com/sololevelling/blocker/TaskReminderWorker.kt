package com.sololevelling.blocker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TaskReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE).orEmpty().ifBlank { return Result.success() }
        val time = inputData.getString(KEY_TIME).orEmpty()

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Task reminders", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val contentText = if (time.isNotBlank()) "Task reminder at $time" else "Task reminder"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val id = inputData.getString(KEY_ID)?.hashCode() ?: title.hashCode()
        manager.notify(id, notification)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "task_title"
        const val KEY_TIME = "task_time"
        const val KEY_ID = "task_id"
        const val WORK_TAG = "task_reminder"
        private const val CHANNEL_ID = "task_reminder_channel"

        fun workName(taskId: String): String = "task_reminder_$taskId"
    }
}
