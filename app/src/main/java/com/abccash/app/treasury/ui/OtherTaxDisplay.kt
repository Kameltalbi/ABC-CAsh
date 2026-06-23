package com.abccash.app.treasury.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.abccash.app.R
import com.abccash.app.treasury.data.InvoiceTaxBreakdown
import com.abccash.app.treasury.data.InvoiceTaxCalculations
import com.abccash.app.treasury.data.OtherTaxMode

@Composable
fun otherTaxLineLabel(tax: InvoiceTaxBreakdown): String {
    val defaultLabel = stringResource(R.string.invoice_other_tax)
    return when (tax.otherTaxMode) {
        OtherTaxMode.PERCENTAGE -> stringResource(
            R.string.invoice_other_tax_line,
            tax.otherTaxLabel.ifBlank { defaultLabel },
            tax.otherTaxRate
        )
        OtherTaxMode.ABSOLUTE -> tax.otherTaxLabel.ifBlank { defaultLabel }
    }
}

fun otherTaxPdfLabel(tax: InvoiceTaxBreakdown, defaultLabel: String): String =
    InvoiceTaxCalculations.otherTaxDisplayLabel(tax, defaultLabel)
