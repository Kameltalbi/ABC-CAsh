package com.abccash.app.treasury.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object OverdueNotificationScheduler {

    private const val WORK_NAME = "overdue_echeances_daily"
    private const val MORNING_HOUR = 8

    fun schedule(context: Context) {
        val now = ZonedDateTime.now()
        var nextRun = now.withHour(MORNING_HOUR).withMinute(0).withSecond(0).withNano(0)
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }
        val initialDelayMs = Duration.between(now, nextRun).toMillis()

        val request = PeriodicWorkRequestBuilder<OverdueNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
