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
    fun formatNumber(amount: Double, decimalPlaces: Int): String {
        val format = NumberFormat.getNumberInstance(Locale.getDefault())
        format.minimumFractionDigits = decimalPlaces
        format.maximumFractionDigits = decimalPlaces
        return format.format(amount)
    }

    fun format(amount: Double, currency: AppCurrency): String =
        "${formatNumber(amount, currency.decimalPlaces)} ${currency.symbol}"

    fun formatCompact(amount: Double, currency: AppCurrency): String {
        val abs = kotlin.math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            abs >= 1_000_000 -> "$sign${compactValue(abs / 1_000_000)}M ${currency.symbol}"
            abs >= 1_000 -> "$sign${compactValue(abs / 1_000)}k ${currency.symbol}"
            else -> format(amount, currency)
        }
    }

    private fun compactValue(value: Double): String = when {
        value >= 100 -> String.format(Locale.US, "%.0f", value)
        value >= 10 -> String.format(Locale.US, "%.0f", value)
        else -> String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
    }

    fun formatTreasuryChartAmount(amount: Double, currency: AppCurrency): String {
        val sign = if (amount < 0) "-" else ""
        val abs = kotlin.math.abs(amount)
        return when {
            abs >= 1_000_000 -> "$sign${compactChartValue(abs / 1_000_000)}M"
            else -> "$sign${compactChartValue(abs / 1_000)}k"
        }
    }

    /** Compact chart label without locale thousand separators (avoids "4 740" clipping as "4 74"). */
    private fun compactChartValue(value: Double): String = when {
        value >= 100 -> String.format(Locale.US, "%.0f", value)
        value >= 10 -> String.format(Locale.US, "%.0f", value)
        else -> String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
    }

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
