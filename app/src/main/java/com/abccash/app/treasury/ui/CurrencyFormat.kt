package com.abccash.app.treasury.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.abccash.app.treasury.data.AppCurrencyFormatter
import com.abccash.app.treasury.data.LocalAppCurrency

@Composable
fun appCurrencySymbol(): String = LocalAppCurrency.current.symbol

@Composable
fun formatMoney(amount: Double): String =
    AppCurrencyFormatter.format(amount, LocalAppCurrency.current)

@Composable
fun rememberFormatMoney(): (Double) -> String {
    val currency = LocalAppCurrency.current
    return remember(currency) { { amount -> AppCurrencyFormatter.format(amount, currency) } }
}

@Composable
fun CurrencySuffix() {
    Text(appCurrencySymbol())
}
