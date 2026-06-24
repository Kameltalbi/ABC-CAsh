package com.abccash.app.treasury.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abccash.app.treasury.datastore.AppSettings
import com.abccash.app.treasury.datastore.UserPreferences
import com.abccash.app.treasury.local.TreasuryDatabase
import com.abccash.app.treasury.repository.TreasuryRepository
import kotlinx.coroutines.flow.first

class OverdueNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val appSettings = AppSettings(applicationContext)
        val notificationsEnabled = appSettings.settingsFlow.first().notificationsEnabled
        if (!notificationsEnabled) return Result.success()

        val userPreferences = UserPreferences(applicationContext)
        if (!userPreferences.readLoggedIn()) return Result.success()

        val entrepriseId = userPreferences.readEntrepriseId() ?: return Result.success()

        val database = TreasuryDatabase.getInstance(applicationContext)
        val repository = TreasuryRepository(database.treasuryDao(), database, userPreferences)
        val overdueCount = repository.countOverdueEcheances(entrepriseId)
        if (overdueCount > 0) {
            OverdueNotificationHelper.showOverdueReminder(applicationContext, overdueCount)
        }
        return Result.success()
    }
}
