package com.abccash.app.treasury.data

data class InvoiceSettings(
    val prefix: String = "FAC-",
    val quotePrefix: String = "DEV-",
    val tvaRate: Double = 19.0,
    val otherTaxRate: Double = 0.0,
    val otherTaxMode: OtherTaxMode = OtherTaxMode.PERCENTAGE,
    val otherTaxLabel: String = "",
    val pdfTemplate: DocumentPdfTemplate = DocumentPdfTemplate.CLASSIC_BLUE
)

data class InvoiceTaxBreakdown(
    val amountExclTax: Double,
    val tvaRate: Double,
    val tvaAmount: Double,
    val otherTaxRate: Double,
    val otherTaxMode: OtherTaxMode,
    val otherTaxAmount: Double,
    val otherTaxLabel: String,
    val totalInclTax: Double
) {
    val hasOtherTax: Boolean
        get() = otherTaxAmount > 0.0 || otherTaxRate > 0.0
}

object InvoiceTaxCalculations {

    fun otherTaxAmount(
        amountExclTax: Double,
        otherTaxValue: Double,
        mode: OtherTaxMode
    ): Double = when (mode) {
        OtherTaxMode.PERCENTAGE -> amountExclTax * otherTaxValue / 100.0
        OtherTaxMode.ABSOLUTE -> otherTaxValue.coerceAtLeast(0.0)
    }

    fun fromAmountExclTax(
        amountExclTax: Double,
        settings: InvoiceSettings
    ): InvoiceTaxBreakdown {
        val tvaAmount = amountExclTax * settings.tvaRate / 100.0
        val otherAmount = otherTaxAmount(amountExclTax, settings.otherTaxRate, settings.otherTaxMode)
        return InvoiceTaxBreakdown(
            amountExclTax = amountExclTax,
            tvaRate = settings.tvaRate,
            tvaAmount = tvaAmount,
            otherTaxRate = settings.otherTaxRate,
            otherTaxMode = settings.otherTaxMode,
            otherTaxAmount = otherAmount,
            otherTaxLabel = settings.otherTaxLabel,
            totalInclTax = amountExclTax + tvaAmount + otherAmount
        )
    }

    fun fromStoredTaxes(
        amountExclTax: Double,
        tvaRate: Double,
        otherTaxValue: Double,
        otherTaxMode: OtherTaxMode,
        otherTaxLabel: String,
        totalInclTax: Double
    ): InvoiceTaxBreakdown {
        val tvaAmount = amountExclTax * tvaRate / 100.0
        val otherAmount = otherTaxAmount(amountExclTax, otherTaxValue, otherTaxMode)
        return InvoiceTaxBreakdown(
            amountExclTax = amountExclTax,
            tvaRate = tvaRate,
            tvaAmount = tvaAmount,
            otherTaxRate = otherTaxValue,
            otherTaxMode = otherTaxMode,
            otherTaxAmount = otherAmount,
            otherTaxLabel = otherTaxLabel,
            totalInclTax = totalInclTax
        )
    }

    fun amountExclTaxFromTotal(
        totalInclTax: Double,
        settings: InvoiceSettings
    ): Double {
        return when (settings.otherTaxMode) {
            OtherTaxMode.PERCENTAGE -> {
                val divisor = 1.0 + (settings.tvaRate + settings.otherTaxRate) / 100.0
                if (divisor <= 0.0) totalInclTax else totalInclTax / divisor
            }
            OtherTaxMode.ABSOLUTE -> {
                val fixedTax = settings.otherTaxRate.coerceAtLeast(0.0)
                val divisor = 1.0 + settings.tvaRate / 100.0
                if (divisor <= 0.0) {
                    (totalInclTax - fixedTax).coerceAtLeast(0.0)
                } else {
                    ((totalInclTax - fixedTax) / divisor).coerceAtLeast(0.0)
                }
            }
        }
    }

    fun otherTaxDisplayLabel(
        breakdown: InvoiceTaxBreakdown,
        defaultLabel: String
    ): String {
        val label = breakdown.otherTaxLabel.ifBlank { defaultLabel }
        return when (breakdown.otherTaxMode) {
            OtherTaxMode.PERCENTAGE -> "$label (${breakdown.otherTaxRate}%)"
            OtherTaxMode.ABSOLUTE -> label
        }
    }
}
