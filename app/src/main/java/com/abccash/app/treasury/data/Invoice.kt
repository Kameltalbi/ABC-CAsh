package com.abccash.app.treasury.data

import java.time.LocalDate
import java.util.UUID

data class Invoice(
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val clientName: String,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val dueDate: LocalDate,
    val createdDate: LocalDate = LocalDate.now(),
    val entrepriseId: String = "",
    val payments: List<Payment> = emptyList()
) {
    val remainingAmount: Double
        get() = totalAmount - paidAmount
    
    val status: InvoiceStatus
        get() = when {
            paidAmount >= totalAmount -> InvoiceStatus.PAID
            paidAmount > 0 -> InvoiceStatus.PARTIAL
            else -> InvoiceStatus.DUE
        }
    
    val progressPercentage: Float
        get() = if (totalAmount > 0) (paidAmount / totalAmount * 100).toFloat() else 0f
}
