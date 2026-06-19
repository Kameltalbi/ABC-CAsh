package com.abccash.app.treasury.remote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abccash.app.treasury.datastore.UserPreferences
import kotlinx.coroutines.flow.first

class TreasurySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferences(applicationContext)
        if (!prefs.isSyncEnabled.first()) return Result.success()
        if (!prefs.isLoggedIn.first()) return Result.success()
        val entrepriseId = prefs.currentEntrepriseId.first() ?: return Result.success()
        if (prefs.getAuthToken().isNullOrBlank()) return Result.success()

        val syncService = TreasurySyncScheduler.createSyncService(applicationContext)
        val error = syncService.syncNow(entrepriseId)
        return if (error == null) Result.success() else Result.retry()
    }
}
