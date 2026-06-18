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

    fun yearlyCollections(invoices: List<Invoice>, year: Int): Double =
        invoices.flatMap { it.payments }
            .filter { it.date.year == year }
            .sumOf { it.amount }

    fun yearlyPaidExpenses(expenses: List<Expense>, year: Int): Double =
        (1..12).sumOf { month ->
            monthlyPaidExpenses(expenses, YearMonth.of(year, month))
        }

    fun yearlyBalance(invoices: List<Invoice>, expenses: List<Expense>, year: Int): Double =
        yearlyCollections(invoices, year) - yearlyPaidExpenses(expenses, year)

    fun yearlyPendingIncome(invoices: List<Invoice>, year: Int): Double =
        (1..12).sumOf { month ->
            pendingInvoiceAmount(invoices, YearMonth.of(year, month))
        }

    fun yearlyPendingExpenses(expenses: List<Expense>, year: Int): Double =
        (1..12).sumOf { month ->
            monthlyUnpaidExpenses(expenses, YearMonth.of(year, month))
        }

    fun yearlyForecastBalance(invoices: List<Invoice>, expenses: List<Expense>, year: Int): Double =
        yearlyCollections(invoices, year) +
            yearlyPendingIncome(invoices, year) -
            yearlyPaidExpenses(expenses, year) -
            yearlyPendingExpenses(expenses, year)

    data class MonthlyTreasuryRow(
        val month: YearMonth,
        val collected: Double,
        val pendingIncome: Double,
        val expenses: Double,
        val pendingExpenses: Double,
        val balance: Double,
        val forecastBalance: Double
    ) {
        val totalIncome: Double get() = collected + pendingIncome
        val totalExpenses: Double get() = expenses + pendingExpenses
    }

    fun yearlyRows(invoices: List<Invoice>, expenses: List<Expense>, year: Int): List<MonthlyTreasuryRow> =
        (1..12).map { monthNumber ->
            val month = YearMonth.of(year, monthNumber)
            val collected = monthlyCollections(invoices, month)
            val pendingIncome = pendingInvoiceAmount(invoices, month)
            val paidExpenses = monthlyPaidExpenses(expenses, month)
            val pendingExpenses = monthlyUnpaidExpenses(expenses, month)
            MonthlyTreasuryRow(
                month = month,
                collected = collected,
                pendingIncome = pendingIncome,
                expenses = paidExpenses,
                pendingExpenses = pendingExpenses,
                balance = monthlyBalance(collected, paidExpenses),
                forecastBalance = forecastedBalance(invoices, expenses, month)
            )
        }
}
