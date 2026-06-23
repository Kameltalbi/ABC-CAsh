package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Test

class InvoiceTaxCalculationsTest {

    private val baseSettings = InvoiceSettings(
        tvaRate = 20.0,
        otherTaxRate = 10.0,
        otherTaxMode = OtherTaxMode.PERCENTAGE,
        otherTaxLabel = "Stamp"
    )

    @Test
    fun fromAmountExclTax_percentageMode() {
        val tax = InvoiceTaxCalculations.fromAmountExclTax(100.0, baseSettings)
        assertEquals(20.0, tax.tvaAmount, 0.001)
        assertEquals(10.0, tax.otherTaxAmount, 0.001)
        assertEquals(130.0, tax.totalInclTax, 0.001)
    }

    @Test
    fun fromAmountExclTax_absoluteMode() {
        val settings = baseSettings.copy(
            otherTaxRate = 5.0,
            otherTaxMode = OtherTaxMode.ABSOLUTE
        )
        val tax = InvoiceTaxCalculations.fromAmountExclTax(100.0, settings)
        assertEquals(20.0, tax.tvaAmount, 0.001)
        assertEquals(5.0, tax.otherTaxAmount, 0.001)
        assertEquals(125.0, tax.totalInclTax, 0.001)
    }

    @Test
    fun amountExclTaxFromTotal_percentageMode() {
        val ht = InvoiceTaxCalculations.amountExclTaxFromTotal(130.0, baseSettings)
        assertEquals(100.0, ht, 0.01)
    }

    @Test
    fun amountExclTaxFromTotal_absoluteMode() {
        val settings = baseSettings.copy(
            otherTaxRate = 5.0,
            otherTaxMode = OtherTaxMode.ABSOLUTE
        )
        val ht = InvoiceTaxCalculations.amountExclTaxFromTotal(125.0, settings)
        assertEquals(100.0, ht, 0.01)
    }
}
