package com.sololevelling.blocker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    fun scheduleCheckIns(context: Context, minutes: Int) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(minutes.toLong(), TimeUnit.MINUTES)
            .addTag(ReminderWorker.WORK_NAME)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun scheduleTaskReminders(context: Context, tasks: List<TaskEntry>) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(TaskReminderWorker.WORK_TAG)

        val now = LocalDateTime.now().withSecond(0).withNano(0)
        tasks.forEach { task ->
            val scheduled = LocalDateTime.of(task.date, task.time)
            if (scheduled.isBefore(now)) return@forEach

            val delay = Duration.between(now, scheduled)
            val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                .setInitialDelay(delay.seconds.coerceAtLeast(0), TimeUnit.SECONDS)
                .setInputData(
                    workDataOf(
                        TaskReminderWorker.KEY_TITLE to task.title,
                        TaskReminderWorker.KEY_TIME to task.time.toString(),
                        TaskReminderWorker.KEY_ID to task.id,
                    )
                )
                .addTag(TaskReminderWorker.WORK_TAG)
                .build()

            workManager.enqueueUniqueWork(
                TaskReminderWorker.workName(task.id),
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }

    fun rescheduleAll(context: Context) {
        val config = ConfigRepository(context).getConfig()
        scheduleCheckIns(context, config.checkInMinutes)
        scheduleTaskReminders(context, config.tasks)
    }
}
