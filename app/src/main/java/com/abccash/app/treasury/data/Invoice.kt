package com.abccash.app.treasury.data

import java.time.LocalDate
import java.util.UUID

data class Invoice(
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val clientName: String,
    val clientContactId: String? = null,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val dueDate: LocalDate,
    val createdDate: LocalDate = LocalDate.now(),
    val entrepriseId: String = "",
    val payments: List<Payment> = emptyList(),
    val category: RevenueCategory = RevenueCategory.OTHER,
    val categoryLabel: String = "",
    val documentStatus: InvoiceDocumentStatus = InvoiceDocumentStatus.VALIDATED,
    val amountExclTax: Double? = null,
    val tvaRate: Double = 0.0,
    val otherTaxRate: Double = 0.0,
    val otherTaxMode: OtherTaxMode = OtherTaxMode.PERCENTAGE,
    val otherTaxLabel: String = "",
    val lineItems: List<InvoiceLineItem> = emptyList()
) {
    val remainingAmount: Double
        get() = totalAmount - paidAmount

    val isDraft: Boolean
        get() = documentStatus == InvoiceDocumentStatus.DRAFT

    val canDelete: Boolean
        get() = documentStatus == InvoiceDocumentStatus.DRAFT

    val displayNumber: String
        get() = if (isDraft) "" else invoiceNumber

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
    
    val status: InvoiceStatus
        get() = when {
            paidAmount >= totalAmount -> InvoiceStatus.PAID
            paidAmount > 0 -> InvoiceStatus.PARTIAL
            else -> InvoiceStatus.DUE
        }
    
    val progressPercentage: Float
        get() = if (totalAmount > 0) (paidAmount / totalAmount * 100).toFloat() else 0f
}
