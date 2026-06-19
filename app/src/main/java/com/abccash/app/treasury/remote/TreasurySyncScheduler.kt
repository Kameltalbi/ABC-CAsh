package com.abccash.app.treasury.remote

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.abccash.app.BuildConfig
import com.abccash.app.treasury.datastore.UserPreferences
import com.abccash.app.treasury.local.TreasuryDatabase
import com.abccash.app.treasury.repository.TreasuryRepository
import java.util.concurrent.TimeUnit

object TreasurySyncScheduler {

    private const val PERIODIC_WORK = "abc_cash_auto_sync"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<TreasurySyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_WORK)
    }

    fun createSyncService(context: Context): TreasurySyncService {
        val appContext = context.applicationContext
        val database = TreasuryDatabase.getInstance(appContext)
        val repository = TreasuryRepository(database.treasuryDao(), database)
        val userPreferences = UserPreferences(appContext)
        val apiClient = TreasuryApiClient(BuildConfig.API_BASE_URL)
        return TreasurySyncService(apiClient, repository, userPreferences)
    }
}
