package com.abccash.app.treasury.data

import androidx.compose.runtime.compositionLocalOf
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

data class AppCurrency(
    val id: String,
    val label: String,
    val symbol: String,
    val decimalPlaces: Int,
    val isCustom: Boolean = false
) {
    init {
        require(label.isNotBlank()) { "Le libellé est obligatoire" }
        require(symbol.isNotBlank()) { "Le symbole est obligatoire" }
        require(decimalPlaces in 0..4) { "Les décimales doivent être entre 0 et 4" }
    }

    fun displayName(): String = "$label ($symbol)"
}

object BuiltInCurrencies {
    val TND = AppCurrency("TND", "Dinar tunisien", "DT", 3)
    val EUR = AppCurrency("EUR", "Euro", "€", 2)
    val USD = AppCurrency("USD", "Dollar US", "$", 2)

    val all = listOf(TND, EUR, USD)

    fun fromId(id: String?): AppCurrency =
        all.find { it.id == id } ?: TND
}

data class CurrencyConfig(
    val selectedCurrencyId: String = BuiltInCurrencies.TND.id,
    val customCurrencies: List<AppCurrency> = emptyList()
) {
    val allCurrencies: List<AppCurrency> = BuiltInCurrencies.all + customCurrencies

    val selectedCurrency: AppCurrency =
        allCurrencies.find { it.id == selectedCurrencyId } ?: BuiltInCurrencies.TND

    companion object {
        val DEFAULT = CurrencyConfig()
    }
}

object AppCurrencyFormatter {
    private val numberFormat = NumberFormat.getNumberInstance(Locale.FRENCH)

    fun formatNumber(amount: Double, decimalPlaces: Int): String =
        synchronized(numberFormat) {
            numberFormat.minimumFractionDigits = decimalPlaces
            numberFormat.maximumFractionDigits = decimalPlaces
            numberFormat.format(amount)
        }

    fun format(amount: Double, currency: AppCurrency): String =
        "${formatNumber(amount, currency.decimalPlaces)} ${currency.symbol}"

    fun encodeCustomCurrencies(currencies: List<AppCurrency>): String =
        currencies.joinToString("\u001F") { currency ->
            listOf(
                currency.id,
                currency.label,
                currency.symbol,
                currency.decimalPlaces.toString()
            ).joinToString("\u001E")
        }

    fun decodeCustomCurrencies(raw: String?): List<AppCurrency> =
        raw?.split('\u001F').orEmpty().mapNotNull { entry ->
            val parts = entry.split('\u001E')
            if (parts.size != 4) return@mapNotNull null
            val decimals = parts[3].toIntOrNull() ?: return@mapNotNull null
            if (decimals !in 0..4) return@mapNotNull null
            runCatching {
                AppCurrency(
                    id = parts[0],
                    label = parts[1],
                    symbol = parts[2],
                    decimalPlaces = decimals,
                    isCustom = true
                )
            }.getOrNull()
        }

    fun newCustomCurrency(label: String, symbol: String, decimalPlaces: Int): AppCurrency =
        AppCurrency(
            id = "custom_${UUID.randomUUID()}",
            label = label.trim(),
            symbol = symbol.trim(),
            decimalPlaces = decimalPlaces,
            isCustom = true
        )
}

val LocalAppCurrency = compositionLocalOf { BuiltInCurrencies.TND }
