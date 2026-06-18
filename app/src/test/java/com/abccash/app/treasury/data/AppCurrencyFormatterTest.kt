package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCurrencyFormatterTest {

    @Test
    fun `format uses decimal places and symbol`() {
        val currency = AppCurrency("EUR", "Euro", "€", 2)
        val formatted = AppCurrencyFormatter.format(1234.56, currency)
        assert(formatted.contains("234,56"))
        assert(formatted.trimEnd().endsWith("€"))
    }

    @Test
    fun `encode and decode custom currencies`() {
        val custom = listOf(
            AppCurrency("custom_1", "Dinar algérien", "DZD", 2, isCustom = true)
        )
        val encoded = AppCurrencyFormatter.encodeCustomCurrencies(custom)
        val decoded = AppCurrencyFormatter.decodeCustomCurrencies(encoded)
        assertEquals(custom, decoded)
    }

    @Test
    fun `TND uses three decimals`() {
        assertEquals("100,500 DT", AppCurrencyFormatter.format(100.5, BuiltInCurrencies.TND))
    }
}
