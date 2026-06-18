package com.abccash.app.treasury.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReceiptOcrParserTest {

    @Test
    fun `parse receipt with total and numeric date`() {
        val text = """
            CARREFOUR MARKET
            12/06/2026
            TOTAL TTC 87,500 DT
        """.trimIndent()

        val result = ReceiptOcrParser.parse(text)

        assertEquals(87.5, result.amount!!, 0.001)
        assertEquals(2026, result.date!!.year)
        assertEquals(6, result.date!!.monthValue)
        assertEquals(12, result.date!!.dayOfMonth)
        assertEquals("CARREFOUR MARKET", result.merchantHint)
    }

    @Test
    fun `parse french date with month name`() {
        val text = "Ticket\n12 juin 2026\nMontant 45,000"

        val result = ReceiptOcrParser.parse(text)

        assertNotNull(result.date)
        assertEquals(12, result.date!!.dayOfMonth)
        assertEquals(6, result.date!!.monthValue)
        assertEquals(45.0, result.amount!!, 0.001)
    }

    @Test
    fun `prefers total line over smaller amounts`() {
        val text = """
            Article 12,000
            TVA 2,280
            TOTAL 14,280 DT
        """.trimIndent()

        assertEquals(14.28, ReceiptOcrParser.parseAmount(text)!!, 0.001)
    }
}
