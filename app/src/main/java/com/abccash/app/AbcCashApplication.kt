package com.abccash.app

import android.app.Application
import com.abccash.app.locale.LocaleHelper
import com.abccash.app.treasury.datastore.AppSettings
import com.abccash.app.treasury.notifications.OverdueNotificationScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AbcCashApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            runBlocking {
                val appSettings = AppSettings(this@AbcCashApplication)
                val tag = appSettings.getAppLanguageTag()
                LocaleHelper.apply(tag)
                if (appSettings.settingsFlow.first().notificationsEnabled) {
                    OverdueNotificationScheduler.schedule(this@AbcCashApplication)
                }
            }
        }
    }
}
