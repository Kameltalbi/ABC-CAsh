package com.abccash.app.treasury.data

import java.time.LocalDate
import java.util.UUID

data class Quote(
    val id: String = UUID.randomUUID().toString(),
    val quoteNumber: String = "",
    val clientName: String,
    val clientContactId: String? = null,
    val totalAmount: Double,
    val issueDate: LocalDate = LocalDate.now(),
    val validUntil: LocalDate,
    val createdDate: LocalDate = LocalDate.now(),
    val entrepriseId: String = "",
    val category: RevenueCategory = RevenueCategory.OTHER,
    val categoryLabel: String = "",
    val status: QuoteStatus = QuoteStatus.DRAFT,
    val amountExclTax: Double? = null,
    val tvaRate: Double = 0.0,
    val otherTaxRate: Double = 0.0,
    val otherTaxMode: OtherTaxMode = OtherTaxMode.PERCENTAGE,
    val otherTaxLabel: String = "",
    val lineItems: List<InvoiceLineItem> = emptyList(),
    val convertedInvoiceId: String? = null,
    val notes: String = ""
) {
    val isDraft: Boolean get() = status == QuoteStatus.DRAFT
    val canDelete: Boolean get() = status == QuoteStatus.DRAFT
    val canConvert: Boolean get() = status == QuoteStatus.ACCEPTED && convertedInvoiceId == null
    val displayNumber: String get() = if (isDraft) "" else quoteNumber

    val taxBreakdown: InvoiceTaxBreakdown?
        get() {
            val ht = amountExclTax ?: InvoiceLineItemCodec.totalExclTax(lineItems).takeIf { it > 0 }
            if (ht == null || ht <= 0) return null
            return InvoiceTaxCalculations.fromStoredTaxes(
                amountExclTax = ht,
                tvaRate = tvaRate,
                otherTaxValue = otherTaxRate,
                otherTaxMode = otherTaxMode,
                otherTaxLabel = otherTaxLabel,
                totalInclTax = totalAmount
            )
        }
}
