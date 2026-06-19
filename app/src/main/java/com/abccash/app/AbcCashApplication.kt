package com.abccash.app

import android.app.Application
import com.abccash.app.locale.LocaleHelper
import com.abccash.app.treasury.datastore.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AbcCashApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runBlocking {
            val tag = AppSettings(this@AbcCashApplication).getAppLanguageTag()
            LocaleHelper.apply(tag)
        }
    }
}
