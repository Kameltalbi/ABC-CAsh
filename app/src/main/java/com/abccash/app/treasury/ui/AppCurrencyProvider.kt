package com.abccash.app.treasury.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abccash.app.treasury.data.CurrencyConfig
import com.abccash.app.treasury.data.LocalAppCurrency
import com.abccash.app.treasury.datastore.AppSettings

@Composable
fun AppCurrencyProvider(
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val config by appSettings.currencyConfigFlow
        .collectAsStateWithLifecycle(initialValue = CurrencyConfig.DEFAULT)
    CompositionLocalProvider(LocalAppCurrency provides config.selectedCurrency) {
        content()
    }
}
