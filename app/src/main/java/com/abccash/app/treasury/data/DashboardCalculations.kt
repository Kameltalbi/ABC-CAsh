package com.abccash.app.treasury.data

import com.abccash.app.locale.AppLocale
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MonthlyBarPoint(
    val month: YearMonth,
    val income: Double,
    val expenses: Double
)

data class DashboardBalancePoint(
    val date: LocalDate,
    val balance: Double,
    val isForecast: Boolean
)

data class CategorySlice(
    val label: String = "",
    val amount: Double,
    val revenueCategory: RevenueCategory? = null,
    val expenseCategory: ExpenseCategory? = null
)

data class DashboardData(
    val displayBalance: Double,
    val bankBalance: Double?,
    val calculatedBalance: Double,
    val balanceFromBank: Boolean,
    val monthLabel: String,
    val balanceHistory: List<DashboardBalancePoint>,
    val incomeByCategory: List<CategorySlice>,
    val incomeTotal: Double,
    val expenseByCategory: List<CategorySlice>,
    val expenseTotal: Double,
    val expensePaidTotal: Double,
    val expensePendingTotal: Double,
    val forecastIncome: Double,
    val forecastExpenses: Double,
    val forecastBalance30Days: Double = displayBalance
)

/** @deprecated use [DashboardData] */
typealias InnovativeDashboardData = DashboardData

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

    fun buildModernDashboardData(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        bankBalance: Double?,
        focusMonth: YearMonth = YearMonth.now(),
        viewMode: DashboardViewMode = DashboardViewMode.MONTH,
        today: LocalDate = LocalDate.now()
    ): DashboardData {
        val periodData = buildInnovativeDashboard(
            invoices = invoices,
            expenses = expenses,
            bankBalance = bankBalance,
            focusMonth = focusMonth,
            viewMode = viewMode,
            today = today
        )
        val balanceHistory = buildBalanceCurve(
            invoices = invoices,
            expenses = expenses,
            anchorBalance = periodData.displayBalance,
            today = today
        )
        val forecastBalance30Days = balanceHistory
            .lastOrNull { it.isForecast }
            ?.balance
            ?: periodData.displayBalance

        return periodData.copy(
            balanceHistory = balanceHistory,
            forecastBalance30Days = forecastBalance30Days
        )
    }

    fun isCurrentDashboardPeriod(
        focusMonth: YearMonth,
        viewMode: DashboardViewMode,
        today: LocalDate = LocalDate.now()
    ): Boolean = when (viewMode) {
        DashboardViewMode.YEAR -> focusMonth.year == today.year
        DashboardViewMode.MONTH -> focusMonth == YearMonth.from(today)
    }

    fun buildMonthlyBarChart(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        focusMonth: YearMonth,
        @Suppress("UNUSED_PARAMETER") viewMode: DashboardViewMode
    ): List<MonthlyBarPoint> {
        val months = (1..12).map { month -> YearMonth.of(focusMonth.year, month) }
        return months.map { month ->
            MonthlyBarPoint(
                month = month,
                income = TreasuryCalculations.monthlyCollections(invoices, month),
                expenses = TreasuryCalculations.monthlyPaidExpenses(expenses, month)
            )
        }
    }

    fun buildDashboardData(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        bankBalance: Double?,
        focusMonth: YearMonth = YearMonth.now(),
        viewMode: DashboardViewMode = DashboardViewMode.YEAR,
        today: LocalDate = LocalDate.now()
    ): DashboardData = buildInnovativeDashboard(
        invoices = invoices,
        expenses = expenses,
        bankBalance = bankBalance,
        focusMonth = focusMonth,
        viewMode = viewMode,
        today = today
    )

    fun buildInnovativeDashboard(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        bankBalance: Double?,
        focusMonth: YearMonth = YearMonth.now(),
        viewMode: DashboardViewMode = DashboardViewMode.YEAR,
        today: LocalDate = LocalDate.now()
    ): DashboardData {
        val referenceDate = resolveReferenceDate(focusMonth, viewMode, today)
        
        // Calculer le solde avec prévisions selon la période sélectionnée
        val calculated = when (viewMode) {
            DashboardViewMode.YEAR -> {
                val year = focusMonth.year
                TreasuryCalculations.yearlyForecastBalance(invoices, expenses, year)
            }
            DashboardViewMode.MONTH -> {
                monthlyForecastBalance(invoices, expenses, focusMonth)
            }
        }
        val displayBalance = bankBalance ?: calculated

        val periodLabel = when (viewMode) {
            DashboardViewMode.YEAR -> focusMonth.year.toString()
            DashboardViewMode.MONTH -> AppLocale.monthYear(focusMonth)
        }

        val incomeByCategory: List<CategorySlice>
        val expenseByCategory: List<CategorySlice>
        val incomeTotal: Double
        val expensePaidTotal: Double
        val expensePendingTotal: Double

        when (viewMode) {
            DashboardViewMode.YEAR -> {
                val year = focusMonth.year
                incomeByCategory = incomeCategoryBreakdownForYear(invoices, year)
                expenseByCategory = expenseCategoryBreakdownForYear(expenses, year)
                incomeTotal = TreasuryCalculations.yearlyCollections(invoices, year) + TreasuryCalculations.yearlyPendingIncome(invoices, year)
                expensePaidTotal = TreasuryCalculations.yearlyPaidExpenses(expenses, year)
                expensePendingTotal = TreasuryCalculations.yearlyPendingExpenses(expenses, year)
            }
            DashboardViewMode.MONTH -> {
                incomeByCategory = incomeCategoryBreakdown(invoices, focusMonth)
                expenseByCategory = expenseCategoryBreakdown(expenses, focusMonth)
                incomeTotal = TreasuryCalculations.monthlyCollections(invoices, focusMonth) + TreasuryCalculations.pendingInvoiceAmount(invoices, focusMonth)
                expensePaidTotal = TreasuryCalculations.monthlyPaidExpenses(expenses, focusMonth)
                expensePendingTotal = TreasuryCalculations.monthlyUnpaidExpenses(expenses, focusMonth)
            }
        }

        val forecastItems = if (referenceDate == today) {
            EcheanceForecast.buildItems(
                invoices = invoices,
                expenses = expenses,
                from = today,
                to = today.plusDays(FORECAST_HORIZON_DAYS)
            )
        } else {
            emptyList()
        }

        return DashboardData(
            displayBalance = displayBalance,
            bankBalance = bankBalance,
            calculatedBalance = calculated,
            balanceFromBank = bankBalance != null,
            monthLabel = periodLabel,
            balanceHistory = buildHistoryCurve(invoices, expenses, displayBalance, referenceDate),
            incomeByCategory = incomeByCategory,
            incomeTotal = incomeTotal,
            expenseByCategory = expenseByCategory,
            expenseTotal = expensePaidTotal + expensePendingTotal,
            expensePaidTotal = expensePaidTotal,
            expensePendingTotal = expensePendingTotal,
            forecastIncome = forecastItems
                .filter { it.type == EcheanceType.INCOME }
                .sumOf { it.amount },
            forecastExpenses = forecastItems
                .filter { it.type == EcheanceType.EXPENSE }
                .sumOf { it.amount }
        )
    }

    private fun resolveReferenceDate(
        focusMonth: YearMonth,
        viewMode: DashboardViewMode,
        today: LocalDate
    ): LocalDate = when (viewMode) {
        DashboardViewMode.YEAR -> {
            if (focusMonth.year == today.year) today else LocalDate.of(focusMonth.year, 12, 31)
        }
        DashboardViewMode.MONTH -> {
            when {
                focusMonth == YearMonth.from(today) -> today
                focusMonth.isBefore(YearMonth.from(today)) -> focusMonth.atEndOfMonth()
                else -> focusMonth.atDay(1)
            }
        }
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

    private fun incomeCategoryBreakdownForYear(
        invoices: List<Invoice>,
        year: Int
    ): List<CategorySlice> =
        invoices.flatMap { invoice ->
            invoice.payments
                .filter { it.date.year == year }
                .map { payment ->
                    CategorySelection.displayIncome(invoice.category, invoice.categoryLabel) to payment.amount
                }
        }
            .groupBy({ it.first }, { it.second })
            .map { (key, amounts) ->
                CategorySlice(
                    label = key.label,
                    amount = amounts.sum(),
                    revenueCategory = key.revenueCategory,
                    expenseCategory = key.expenseCategory
                )
            }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }

    private fun expenseCategoryBreakdownForYear(
        expenses: List<Expense>,
        year: Int
    ): List<CategorySlice> =
        (1..12).flatMap { monthNumber ->
            val month = YearMonth.of(year, monthNumber)
            expenses
                .filter { it.appliesToMonth(month) }
                .map { expense ->
                    CategorySelection.displayExpense(expense.category, expense.categoryLabel) to expense.amount
                }
        }
            .groupBy({ it.first }, { it.second })
            .map { (key, amounts) ->
                CategorySlice(
                    label = key.label,
                    amount = amounts.sum(),
                    revenueCategory = key.revenueCategory,
                    expenseCategory = key.expenseCategory
                )
            }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }

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
            .map { (key, amounts) ->
                CategorySlice(
                    label = key.label,
                    amount = amounts.sum(),
                    revenueCategory = key.revenueCategory,
                    expenseCategory = key.expenseCategory
                )
            }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }

    private fun expenseCategoryBreakdown(
        expenses: List<Expense>,
        month: YearMonth
    ): List<CategorySlice> =
        expenses
            .filter { it.appliesToMonth(month) }
            .groupBy { CategorySelection.displayExpense(it.category, it.categoryLabel) }
            .map { (key, items) ->
                CategorySlice(
                    label = key.label,
                    amount = items.sumOf { it.amount },
                    revenueCategory = key.revenueCategory,
                    expenseCategory = key.expenseCategory
                )
            }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }

    private fun incomeCategoryBreakdownLast30Days(
        invoices: List<Invoice>,
        today: LocalDate
    ): List<CategorySlice> {
        val from = today.minusDays(HISTORY_DAYS.toLong())
        return invoices.flatMap { invoice ->
            invoice.payments
                .filter { !it.date.isBefore(from) && !it.date.isAfter(today) }
                .map { payment ->
                    CategorySelection.displayIncome(invoice.category, invoice.categoryLabel) to payment.amount
                }
        }
            .groupBy({ it.first }, { it.second })
            .map { (key, amounts) ->
                CategorySlice(
                    label = key.label,
                    amount = amounts.sum(),
                    revenueCategory = key.revenueCategory,
                    expenseCategory = key.expenseCategory
                )
            }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }
    }

    private fun expenseCategoryBreakdownLast30Days(
        expenses: List<Expense>,
        today: LocalDate
    ): List<CategorySlice> {
        val from = today.minusDays(HISTORY_DAYS.toLong())
        val days = generateSequence(from) { prev ->
            if (prev.isBefore(today)) prev.plusDays(1) else null
        }.toList() + today
        return days.flatMap { day ->
            expenses
                .filter { it.isPaid && expenseOccursOn(it, day) }
                .map { expense ->
                    CategorySelection.displayExpense(expense.category, expense.categoryLabel) to expense.amount
                }
        }
            .groupBy({ it.first }, { it.second })
            .map { (key, amounts) ->
                CategorySlice(
                    label = key.label,
                    amount = amounts.sum(),
                    revenueCategory = key.revenueCategory,
                    expenseCategory = key.expenseCategory
                )
            }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }
    }

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

    fun computedBalanceAtDate(invoices: List<Invoice>, expenses: List<Expense>, date: LocalDate): Double {
        val collected = invoices
            .filter { it.paidAmount > 0 }
            .sumOf { invoice ->
                // Somme des paiements de la facture avant ou à la date
                // Pour simplifier, on utilise paidAmount (somme totale encaissée)
                // Pour être plus précis, il faudrait filtrer les paiements par date
                invoice.paidAmount
            }
        val paid = expenses
            .filter { it.isPaid && !it.date.isAfter(date) }
            .sumOf { it.amount }
        return collected - paid
    }

    fun monthlyForecastBalance(invoices: List<Invoice>, expenses: List<Expense>, month: YearMonth): Double {
        val year = month.year
        val cumulativeBalance = TreasuryCalculations.openingBalanceAtYearStart(invoices, expenses, year)
        
        // Ajouter les soldes cumulés pour tous les mois jusqu'au mois sélectionné
        var balance = cumulativeBalance
        for (m in 1..month.monthValue) {
            val currentMonth = YearMonth.of(year, m)
            balance += (TreasuryCalculations.monthlyCollections(invoices, currentMonth) +
                TreasuryCalculations.pendingInvoiceAmount(invoices, currentMonth) -
                TreasuryCalculations.monthlyPaidExpenses(expenses, currentMonth) -
                TreasuryCalculations.monthlyUnpaidExpenses(expenses, currentMonth))
        }
        return balance
    }

    fun expenseWeekTrendPercent(
        expenses: List<Expense>,
        today: LocalDate = LocalDate.now()
    ): Double? {
        val last7 = paidExpenseTotalBetween(expenses, today.minusDays(6), today)
        val prev7 = paidExpenseTotalBetween(expenses, today.minusDays(13), today.minusDays(7))
        if (prev7 <= 0.0) return null
        return ((last7 - prev7) / prev7) * 100.0
    }

    private fun paidExpenseTotalBetween(
        expenses: List<Expense>,
        from: LocalDate,
        to: LocalDate
    ): Double {
        val days = generateSequence(from) { prev ->
            if (prev.isBefore(to)) prev.plusDays(1) else null
        }.toList() + to
        return days.sumOf { day ->
            expenses
                .filter { it.isPaid && expenseOccursOn(it, day) }
                .sumOf { it.amount }
        }
    }

    fun monthLabelsOnCurve(
        points: List<DashboardBalancePoint>,
        locale: Locale = AppLocale.current()
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
