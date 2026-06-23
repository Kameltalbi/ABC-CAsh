package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Test

class InvoiceNumberGeneratorTest {

    @Test
    fun `next number increments within year`() {
        val first = InvoiceNumberGenerator.nextNumber("FAC-", 2026, emptyList())
        assertEquals("FAC-2026-00001", first)

        val second = InvoiceNumberGenerator.nextNumber("FAC-", 2026, listOf(first))
        assertEquals("FAC-2026-00002", second)
    }

    @Test
    fun `next number resets sequence per year`() {
        val existing = listOf("FAC-2025-00012")
        val next = InvoiceNumberGenerator.nextNumber("FAC-", 2026, existing)
        assertEquals("FAC-2026-00001", next)
    }
}
