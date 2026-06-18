package com.abccash.app.treasury.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DashboardBalancePoint(
    val date: LocalDate,
    val balance: Double,
    val isForecast: Boolean
)

data class CategorySlice(
    val label: String,
    val amount: Double
)

data class InnovativeDashboardData(
    val bankBalance: Double,
    val balanceHistory: List<DashboardBalancePoint>,
    val incomeByCategory: List<CategorySlice>,
    val incomeTotal: Double,
    val expenseByCategory: List<CategorySlice>,
    val expenseTotal: Double,
    val forecastIncome: Double,
    val forecastExpenses: Double
)

data class DashboardSnapshot(
    val bankBalance: Double,
    val accountUpToDate: Boolean,
    val balanceCurve: List<DashboardBalancePoint>,
    val monthIncome: Double,
    val monthExpenses: Double,
    val paidInvoicesCount: Int,
    val expenseEntriesCount: Int,
    val endOfMonthEstimate: Double,
    val isHealthy: Boolean
)

object DashboardCalculations {

    private const val HISTORY_DAYS = 30
    private const val FORECAST_DAYS = 30
    private const val FORECAST_HORIZON_DAYS = 30L

    fun buildInnovativeDashboard(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        bankBalance: Double?,
        today: LocalDate = LocalDate.now()
    ): InnovativeDashboardData {
        val balance = bankBalance ?: computedBalance(invoices, expenses)
        val month = YearMonth.from(today)
        val forecastItems = EcheanceForecast.buildItems(
            invoices = invoices,
            expenses = expenses,
            from = today,
            to = today.plusDays(FORECAST_HORIZON_DAYS)
        )

        val incomeByCategory = incomeCategoryBreakdown(invoices, month)
        val expenseByCategory = expenseCategoryBreakdown(expenses, month)

        return InnovativeDashboardData(
            bankBalance = balance,
            balanceHistory = buildHistoryCurve(invoices, expenses, balance, today),
            incomeByCategory = incomeByCategory,
            incomeTotal = incomeByCategory.sumOf { it.amount },
            expenseByCategory = expenseByCategory,
            expenseTotal = expenseByCategory.sumOf { it.amount },
            forecastIncome = forecastItems
                .filter { it.type == EcheanceType.INCOME }
                .sumOf { it.amount },
            forecastExpenses = forecastItems
                .filter { it.type == EcheanceType.EXPENSE }
                .sumOf { it.amount }
        )
    }

    fun buildHistoryCurve(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        anchorBalance: Double,
        today: LocalDate = LocalDate.now()
    ): List<DashboardBalancePoint> {
        val start = today.minusDays(HISTORY_DAYS.toLong())
        val allDays = generateSequence(start) { prev ->
            if (prev.isBefore(today)) prev.plusDays(1) else null
        }.toList() + today

        val dailyNet = allDays.associateWith { day -> netCashFlowOnDay(invoices, expenses, day) }

        return allDays.map { day ->
            val futureFromDay = dailyNet.filterKeys { it.isAfter(day) && !it.isAfter(today) }
                .values.sum()
            DashboardBalancePoint(day, anchorBalance - futureFromDay, isForecast = false)
        }
    }

    private fun incomeCategoryBreakdown(
        invoices: List<Invoice>,
        month: YearMonth
    ): List<CategorySlice> =
        invoices.flatMap { invoice ->
            invoice.payments
                .filter { YearMonth.from(it.date) == month }
                .map { payment ->
                    CategorySelection.displayIncome(invoice.category, invoice.categoryLabel) to payment.amount
                }
        }
            .groupBy({ it.first }, { it.second })
            .map { (label, amounts) -> CategorySlice(label, amounts.sum()) }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }

    private fun expenseCategoryBreakdown(
        expenses: List<Expense>,
        month: YearMonth
    ): List<CategorySlice> =
        expenses
            .filter { it.isPaid && it.appliesToMonth(month) }
            .groupBy { CategorySelection.displayExpense(it.category, it.categoryLabel) }
            .map { (label, items) -> CategorySlice(label, items.sumOf { it.amount }) }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }

    fun buildSnapshot(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        bankBalance: Double?,
        today: LocalDate = LocalDate.now()
    ): DashboardSnapshot {
        val computed = computedBalance(invoices, expenses)
        val balance = bankBalance ?: computed
        val month = YearMonth.from(today)

        val monthIncome = TreasuryCalculations.monthlyCollections(invoices, month)
        val monthExpenses = TreasuryCalculations.monthlyPaidExpenses(expenses, month)
        val paidInvoicesCount = invoices.count { invoice ->
            invoice.payments.any { YearMonth.from(it.date) == month }
        }
        val expenseEntriesCount = expenses.count { it.appliesToMonth(month) }

        val remainingPendingIncome = invoices
            .filter { it.status != InvoiceStatus.PAID && YearMonth.from(it.dueDate) == month }
            .sumOf { it.remainingAmount }

        val remainingUnpaidExpenses = TreasuryCalculations.monthlyUnpaidExpenses(expenses, month)
        val monthEndEstimate = balance + remainingPendingIncome - remainingUnpaidExpenses

        return DashboardSnapshot(
            bankBalance = balance,
            accountUpToDate = bankBalance != null,
            balanceCurve = buildBalanceCurve(invoices, expenses, balance, today),
            monthIncome = monthIncome,
            monthExpenses = monthExpenses,
            paidInvoicesCount = paidInvoicesCount,
            expenseEntriesCount = expenseEntriesCount,
            endOfMonthEstimate = monthEndEstimate,
            isHealthy = monthEndEstimate >= 0
        )
    }

    fun computedBalance(invoices: List<Invoice>, expenses: List<Expense>): Double {
        val collected = invoices.sumOf { it.paidAmount }
        val paid = expenses.filter { it.isPaid }.sumOf { it.amount }
        return collected - paid
    }

    fun monthLabelsOnCurve(
        points: List<DashboardBalancePoint>,
        locale: Locale = Locale.FRENCH
    ): List<Pair<Float, String>> {
        if (points.isEmpty()) return emptyList()
        val formatter = DateTimeFormatter.ofPattern("MMM", locale)
        val months = points.map { YearMonth.from(it.date) }.distinct()
        return months.mapIndexed { index, ym ->
            val fraction = index.toFloat() / (months.size - 1).coerceAtLeast(1)
            fraction to ym.atDay(1).format(formatter).replaceFirstChar { it.uppercase() }
        }
    }

    private fun buildBalanceCurve(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        anchorBalance: Double,
        today: LocalDate
    ): List<DashboardBalancePoint> {
        val start = today.minusDays(HISTORY_DAYS.toLong())
        val end = today.plusDays(FORECAST_DAYS.toLong())
        val allDays = generateSequence(start) { prev ->
            if (prev.isBefore(end)) prev.plusDays(1) else null
        }.toList()

        val dailyNet = allDays.associateWith { day -> netCashFlowOnDay(invoices, expenses, day) }

        val historyPoints = allDays
            .filter { !it.isAfter(today) }
            .map { day ->
                val futureFromDay = dailyNet.filterKeys { it.isAfter(day) && !it.isAfter(today) }
                    .values.sum()
                DashboardBalancePoint(day, anchorBalance - futureFromDay, isForecast = false)
            }

        val forecastPoints = (1..FORECAST_DAYS).map { offset ->
            val day = today.plusDays(offset.toLong())
            val flowSinceToday = (1..offset).sumOf { o ->
                val d = today.plusDays(o.toLong())
                (dailyNet[d] ?: forecastFlowOnDay(invoices, expenses, d)).toDouble()
            }
            DashboardBalancePoint(day, anchorBalance + flowSinceToday, isForecast = true)
        }

        return historyPoints + forecastPoints
    }

    private fun netCashFlowOnDay(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        day: LocalDate
    ): Double {
        val income = invoices.flatMap { it.payments }
            .filter { it.date == day }
            .sumOf { it.amount }
        val outcome = expenses
            .filter { it.isPaid && expenseOccursOn(it, day) }
            .sumOf { it.amount }
        return income - outcome
    }

    private fun forecastFlowOnDay(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        day: LocalDate
    ): Double {
        val expectedIncome = invoices
            .filter { it.status != InvoiceStatus.PAID && it.dueDate == day }
            .sumOf { it.remainingAmount }
        val expectedExpense = expenses
            .filter { !it.isPaid && expenseOccursOn(it, day) }
            .sumOf { it.amount }
        return expectedIncome - expectedExpense
    }

    private fun expenseOccursOn(expense: Expense, day: LocalDate): Boolean {
        if (!expense.isRecurring) return expense.date == day
        return expense.occurrenceDateIn(YearMonth.from(day)) == day
    }
}
