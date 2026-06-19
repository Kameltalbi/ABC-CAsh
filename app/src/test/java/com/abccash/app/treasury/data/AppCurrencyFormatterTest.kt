package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCurrencyFormatterTest {

    @Test
    fun `format uses decimal places and symbol`() {
        val currency = AppCurrency("EUR", "Euro", "€", 2)
        val formatted = AppCurrencyFormatter.format(1234.56, currency)
        assertTrue(formatted.trimEnd().endsWith("€"))
        assertTrue(formatted.contains("56"))
        assertTrue(formatted.contains("1234") || formatted.contains("1") )
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
        val formatted = AppCurrencyFormatter.format(100.5, BuiltInCurrencies.TND)
        assertTrue(formatted.endsWith("DT"))
        assertTrue(formatted.contains("100"))
        assertTrue(formatted.contains("500"))
    }
}
