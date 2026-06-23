package com.abccash.app.treasury.data

import java.time.LocalDate
import java.time.YearMonth

data class InvoiceKpis(
    val totalBilled: Double,
    val totalCollected: Double,
    val totalPending: Double,
    val invoiceCount: Int,
    val paidCount: Int,
    val dueCount: Int,
    val overdueCount: Int
)

object InvoiceKpisCalculations {

    fun compute(invoices: List<Invoice>, month: YearMonth, today: LocalDate = LocalDate.now()): InvoiceKpis {
        val monthInvoices = invoices.filter { YearMonth.from(it.dueDate) == month }
        val totalBilled = monthInvoices.sumOf { it.totalAmount }
        val totalCollected = monthInvoices.sumOf { it.paidAmount }
        val totalPending = monthInvoices.sumOf { it.remainingAmount }
        val paidCount = monthInvoices.count { it.status == InvoiceStatus.PAID }
        val dueCount = monthInvoices.count { it.status != InvoiceStatus.PAID }
        val overdueCount = monthInvoices.count {
            it.status != InvoiceStatus.PAID && it.dueDate.isBefore(today)
        }
        return InvoiceKpis(
            totalBilled = totalBilled,
            totalCollected = totalCollected,
            totalPending = totalPending,
            invoiceCount = monthInvoices.size,
            paidCount = paidCount,
            dueCount = dueCount,
            overdueCount = overdueCount
        )
    }
}
