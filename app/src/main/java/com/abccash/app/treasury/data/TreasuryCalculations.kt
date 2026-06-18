package com.abccash.app.treasury.data

import java.time.YearMonth

object TreasuryCalculations {

    fun monthlyCollections(invoices: List<Invoice>, month: YearMonth): Double =
        invoices.flatMap { it.payments }
            .filter { YearMonth.from(it.date) == month }
            .sumOf { it.amount }

    fun monthlyPaidExpenses(expenses: List<Expense>, month: YearMonth): Double =
        expenses.filter { it.isPaid && it.appliesToMonth(month) }
            .sumOf { it.amount }

    fun monthlyUnpaidExpenses(expenses: List<Expense>, month: YearMonth): Double =
        expenses.filter { !it.isPaid && it.appliesToMonth(month) }
            .sumOf { it.amount }

    fun monthlyBalance(collections: Double, paidExpenses: Double): Double =
        collections - paidExpenses

    fun pendingInvoiceAmount(invoices: List<Invoice>, month: YearMonth): Double =
        invoices.filter { it.status != InvoiceStatus.PAID && YearMonth.from(it.dueDate) == month }
            .sumOf { it.remainingAmount }

    fun forecastedBalance(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        month: YearMonth
    ): Double {
        val collections = monthlyCollections(invoices, month)
        val paidExpenses = monthlyPaidExpenses(expenses, month)
        val unpaidExpenses = monthlyUnpaidExpenses(expenses, month)
        val pendingInvoices = pendingInvoiceAmount(invoices, month)
        return monthlyBalance(collections, paidExpenses) + pendingInvoices - unpaidExpenses
    }
}
