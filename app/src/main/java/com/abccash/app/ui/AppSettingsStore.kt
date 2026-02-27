package com.abccash.app.ui

import android.content.Context
import com.abccash.app.data.local.entity.CurrencyCode
import com.abccash.app.ui.theme.AppPalette

enum class DateFormatOption(val label: String, val pattern: String) {
    ISO("YYYY-MM-DD", "yyyy-MM-dd"),
    FR("DD/MM/YYYY", "dd/MM/yyyy"),
    US("MM/DD/YYYY", "MM/dd/yyyy")
}

class AppSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("abc_cash_settings", Context.MODE_PRIVATE)

    fun getDefaultCurrency(): CurrencyCode {
        return runCatching {
            CurrencyCode.valueOf(prefs.getString("default_currency", CurrencyCode.DT.name) ?: CurrencyCode.DT.name)
        }.getOrDefault(CurrencyCode.DT)
    }

    fun setDefaultCurrency(currency: CurrencyCode) {
        prefs.edit().putString("default_currency", currency.name).apply()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun getDateFormat(): DateFormatOption {
        val stored = prefs.getString("date_format", DateFormatOption.ISO.name) ?: DateFormatOption.ISO.name
        return runCatching { DateFormatOption.valueOf(stored) }.getOrDefault(DateFormatOption.ISO)
    }

    fun setDateFormat(format: DateFormatOption) {
        prefs.edit().putString("date_format", format.name).apply()
    }

    fun getPalette(): AppPalette {
        val stored = prefs.getString("palette", AppPalette.SUNSET.name) ?: AppPalette.SUNSET.name
        return runCatching { AppPalette.valueOf(stored) }.getOrDefault(AppPalette.SUNSET)
    }

    fun setPalette(palette: AppPalette) {
        prefs.edit().putString("palette", palette.name).apply()
    }

    fun getDriveSyncFolderUri(): String? = prefs.getString("drive_sync_folder_uri", null)

    fun setDriveSyncFolderUri(uri: String?) {
        prefs.edit().putString("drive_sync_folder_uri", uri).apply()
    }
}
