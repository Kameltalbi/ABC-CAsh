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
    ): Double = monthlyTreasuryNet(invoices, expenses, month)

    fun monthlyEncaissements(invoices: List<Invoice>, month: YearMonth): Double =
        monthlyCollections(invoices, month) + pendingInvoiceAmount(invoices, month)

    fun monthlyDepenses(expenses: List<Expense>, month: YearMonth): Double =
        monthlyPaidExpenses(expenses, month) + monthlyUnpaidExpenses(expenses, month)

    fun monthlyTreasuryNet(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        month: YearMonth
    ): Double = monthlyEncaissements(invoices, month) - monthlyDepenses(expenses, month)

    fun openingBalanceAtYearStart(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        year: Int
    ): Double {
        val collectedBefore = invoices.flatMap { it.payments }
            .filter { it.date.year < year }
            .sumOf { it.amount }
        val paidBefore = expenses
            .filter { it.isPaid && it.date.year < year }
            .sumOf { it.amount }
        return collectedBefore - paidBefore
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

    fun yearlyRows(invoices: List<Invoice>, expenses: List<Expense>, year: Int): List<MonthlyTreasuryRow> {
        var cumulativeBalance = openingBalanceAtYearStart(invoices, expenses, year)
        return (1..12).map { monthNumber ->
            val month = YearMonth.of(year, monthNumber)
            val collected = monthlyCollections(invoices, month)
            val pendingIncome = pendingInvoiceAmount(invoices, month)
            val paidExpenses = monthlyPaidExpenses(expenses, month)
            val pendingExpenses = monthlyUnpaidExpenses(expenses, month)
            cumulativeBalance += (collected + pendingIncome) - (paidExpenses + pendingExpenses)
            MonthlyTreasuryRow(
                month = month,
                collected = collected,
                pendingIncome = pendingIncome,
                expenses = paidExpenses,
                pendingExpenses = pendingExpenses,
                balance = monthlyBalance(collected, paidExpenses),
                forecastBalance = cumulativeBalance
            )
        }
    }
}
