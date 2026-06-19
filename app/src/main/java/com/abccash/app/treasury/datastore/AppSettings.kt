package com.abccash.app.treasury.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.abccash.app.treasury.data.AppCurrencyFormatter
import com.abccash.app.treasury.data.BuiltInCurrencies
import com.abccash.app.treasury.data.CurrencyConfig
import com.abccash.app.treasury.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AppSettingsState(
    val currencyConfig: CurrencyConfig = CurrencyConfig.DEFAULT,
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val pinEnabled: Boolean = false,
    val hasPin: Boolean = false,
    val appLanguageTag: String? = null
) {
    fun requiresLock(): Boolean = biometricEnabled || (pinEnabled && hasPin)
}

class AppSettings(private val context: Context) {

    val currencyConfigFlow: Flow<CurrencyConfig> = context.userDataStore.data.map { prefs ->
        val custom = AppCurrencyFormatter.decodeCustomCurrencies(prefs[Keys.CUSTOM_CURRENCIES])
        val selectedId = prefs[Keys.SELECTED_CURRENCY_ID]
            ?: prefs[Keys.LEGACY_DEFAULT_CURRENCY]
            ?: BuiltInCurrencies.TND.id
        CurrencyConfig(
            selectedCurrencyId = selectedId,
            customCurrencies = custom
        )
    }

    val settingsFlow: Flow<AppSettingsState> = context.userDataStore.data.map { prefs ->
        val custom = AppCurrencyFormatter.decodeCustomCurrencies(prefs[Keys.CUSTOM_CURRENCIES])
        val selectedId = prefs[Keys.SELECTED_CURRENCY_ID]
            ?: prefs[Keys.LEGACY_DEFAULT_CURRENCY]
            ?: BuiltInCurrencies.TND.id
        AppSettingsState(
            currencyConfig = CurrencyConfig(
                selectedCurrencyId = selectedId,
                customCurrencies = custom
            ),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            pinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            hasPin = !prefs[Keys.PIN_HASH].isNullOrBlank(),
            appLanguageTag = prefs[Keys.APP_LANGUAGE]
        )
    }

    fun appLanguageFlow(): Flow<String?> = context.userDataStore.data.map { prefs ->
        prefs[Keys.APP_LANGUAGE]
    }

    suspend fun getAppLanguageTag(): String? =
        context.userDataStore.data.first()[Keys.APP_LANGUAGE]

    suspend fun setAppLanguage(languageTag: String?) {
        context.userDataStore.edit { prefs ->
            if (languageTag.isNullOrBlank()) {
                prefs.remove(Keys.APP_LANGUAGE)
            } else {
                prefs[Keys.APP_LANGUAGE] = languageTag
            }
        }
    }

    fun customIncomeCategories(entrepriseId: String): Flow<List<String>> =
        context.userDataStore.data.map { prefs ->
            decodeList(prefs[customIncomeKey(entrepriseId)])
        }

    fun customExpenseCategories(entrepriseId: String): Flow<List<String>> =
        context.userDataStore.data.map { prefs ->
            decodeList(prefs[customExpenseKey(entrepriseId)])
        }

    suspend fun setSelectedCurrency(currencyId: String) {
        context.userDataStore.edit { prefs ->
            prefs[Keys.SELECTED_CURRENCY_ID] = currencyId
            prefs.remove(Keys.LEGACY_DEFAULT_CURRENCY)
        }
    }

    suspend fun addCustomCurrency(label: String, symbol: String, decimalPlaces: Int): String? {
        val trimmedLabel = label.trim()
        val trimmedSymbol = symbol.trim()
        if (trimmedLabel.isBlank()) return "Le libellé est obligatoire"
        if (trimmedSymbol.isBlank()) return "Le symbole est obligatoire"
        if (decimalPlaces !in 0..4) return "Les décimales doivent être entre 0 et 4"

        val currency = runCatching {
            AppCurrencyFormatter.newCustomCurrency(trimmedLabel, trimmedSymbol, decimalPlaces)
        }.getOrElse { return it.message }

        val config = currencyConfigFlow.first()
        if (config.allCurrencies.any { it.symbol.equals(trimmedSymbol, ignoreCase = true) }) {
            return "Ce symbole existe déjà"
        }

        context.userDataStore.edit { prefs ->
            val current = AppCurrencyFormatter.decodeCustomCurrencies(prefs[Keys.CUSTOM_CURRENCIES])
            val updated = current + currency
            prefs[Keys.CUSTOM_CURRENCIES] = AppCurrencyFormatter.encodeCustomCurrencies(updated)
        }
        return null
    }

    suspend fun removeCustomCurrency(currencyId: String) {
        context.userDataStore.edit { prefs ->
            val current = AppCurrencyFormatter.decodeCustomCurrencies(prefs[Keys.CUSTOM_CURRENCIES])
            val updated = current.filterNot { it.id == currencyId }
            prefs[Keys.CUSTOM_CURRENCIES] = AppCurrencyFormatter.encodeCustomCurrencies(updated)
            val selected = prefs[Keys.SELECTED_CURRENCY_ID]
                ?: prefs[Keys.LEGACY_DEFAULT_CURRENCY]
            if (selected == currencyId) {
                prefs[Keys.SELECTED_CURRENCY_ID] = BuiltInCurrencies.TND.id
            }
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.userDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.userDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setPinEnabled(enabled: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[Keys.PIN_ENABLED] = enabled
            if (!enabled) prefs.remove(Keys.PIN_HASH)
        }
    }

    suspend fun setPin(pin: String) {
        context.userDataStore.edit { prefs ->
            prefs[Keys.PIN_HASH] = PasswordHasher.hash(pin)
            prefs[Keys.PIN_ENABLED] = true
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = context.userDataStore.data.first()[Keys.PIN_HASH] ?: return false
        return PasswordHasher.verify(pin, stored)
    }

    suspend fun addCustomIncomeCategory(entrepriseId: String, label: String) {
        val trimmed = label.trim()
        if (trimmed.isBlank()) return
        context.userDataStore.edit { prefs ->
            val key = customIncomeKey(entrepriseId)
            val current = decodeList(prefs[key]).toMutableList()
            if (current.none { it.equals(trimmed, ignoreCase = true) }) {
                current.add(trimmed)
                prefs[key] = encodeList(current)
            }
        }
    }

    suspend fun removeCustomIncomeCategory(entrepriseId: String, label: String) {
        context.userDataStore.edit { prefs ->
            val key = customIncomeKey(entrepriseId)
            val current = decodeList(prefs[key]).filterNot { it.equals(label, ignoreCase = true) }
            prefs[key] = encodeList(current)
        }
    }

    suspend fun renameCustomIncomeCategory(
        entrepriseId: String,
        oldLabel: String,
        newLabel: String
    ): String? {
        val trimmed = newLabel.trim()
        if (trimmed.isBlank()) return "label_required"
        val key = customIncomeKey(entrepriseId)
        val current = decodeList(context.userDataStore.data.first()[key])
        if (current.none { it.equals(oldLabel, ignoreCase = true) }) return null
        if (current.any { it.equals(trimmed, ignoreCase = true) && !it.equals(oldLabel, ignoreCase = true) }) {
            return "category_exists"
        }
        context.userDataStore.edit { prefs ->
            val items = decodeList(prefs[key]).toMutableList()
            val index = items.indexOfFirst { it.equals(oldLabel, ignoreCase = true) }
            if (index >= 0) {
                items[index] = trimmed
                prefs[key] = encodeList(items)
            }
        }
        return null
    }

    suspend fun addCustomExpenseCategory(entrepriseId: String, label: String) {
        val trimmed = label.trim()
        if (trimmed.isBlank()) return
        context.userDataStore.edit { prefs ->
            val key = customExpenseKey(entrepriseId)
            val current = decodeList(prefs[key]).toMutableList()
            if (current.none { it.equals(trimmed, ignoreCase = true) }) {
                current.add(trimmed)
                prefs[key] = encodeList(current)
            }
        }
    }

    suspend fun removeCustomExpenseCategory(entrepriseId: String, label: String) {
        context.userDataStore.edit { prefs ->
            val key = customExpenseKey(entrepriseId)
            val current = decodeList(prefs[key]).filterNot { it.equals(label, ignoreCase = true) }
            prefs[key] = encodeList(current)
        }
    }

    suspend fun renameCustomExpenseCategory(
        entrepriseId: String,
        oldLabel: String,
        newLabel: String
    ): String? {
        val trimmed = newLabel.trim()
        if (trimmed.isBlank()) return "label_required"
        val key = customExpenseKey(entrepriseId)
        val current = decodeList(context.userDataStore.data.first()[key])
        if (current.none { it.equals(oldLabel, ignoreCase = true) }) return null
        if (current.any { it.equals(trimmed, ignoreCase = true) && !it.equals(oldLabel, ignoreCase = true) }) {
            return "category_exists"
        }
        context.userDataStore.edit { prefs ->
            val items = decodeList(prefs[key]).toMutableList()
            val index = items.indexOfFirst { it.equals(oldLabel, ignoreCase = true) }
            if (index >= 0) {
                items[index] = trimmed
                prefs[key] = encodeList(items)
            }
        }
        return null
    }

    private object Keys {
        val SELECTED_CURRENCY_ID = stringPreferencesKey("selected_currency_id")
        val LEGACY_DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val CUSTOM_CURRENCIES = stringPreferencesKey("custom_currencies")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("app_pin_hash")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    private fun customIncomeKey(entrepriseId: String) =
        stringPreferencesKey("custom_income_categories_$entrepriseId")

    private fun customExpenseKey(entrepriseId: String) =
        stringPreferencesKey("custom_expense_categories_$entrepriseId")

    private fun encodeList(items: List<String>): String =
        items.joinToString("\u001F")

    private fun decodeList(raw: String?): List<String> =
        raw?.split('\u001F')?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
}
