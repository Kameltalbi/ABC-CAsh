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

data class MonthFinancialSummary(
    val month: YearMonth,
    val label: String,
    val income: Double,
    val expenses: Double
) {
    val total: Double get() = income - expenses
}

data class UpcomingPaymentBar(
    val dueDate: LocalDate,
    val amount: Double,
    val label: String
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

data class AnnualTreasuryPoint(
    val month: YearMonth,
    val label: String,
    val forecastBalance: Double,
    val income: Double,
    val expenses: Double
)

data class TreasuryRecommendation(
    val title: String,
    val description: String,
    val severity: RecommendationSeverity,
    val actionLabel: String? = null,
    val estimateImpact: String? = null
)

data class BreakEvenSummary(
    val targetRevenue: Double,
    val achievedRevenue: Double,
    val remainingRevenue: Double,
    val projectedExpenses: Double,
    val previousMonthBalance: Double,
    val additionalMargin: Double,
    val isAchieved: Boolean
)

enum class RecommendationSeverity {
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL
}

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
            today = today,
            bankOnly = true
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

    fun buildTreasuryRecommendations(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        bankAccounts: List<BankAccount>,
        focusMonth: YearMonth = YearMonth.now(),
        today: LocalDate = LocalDate.now()
    ): List<TreasuryRecommendation> {
        val openingBalance = TreasuryCalculations.manualOpeningBalance(bankAccounts)
        val currentMonthNet = TreasuryCalculations.monthlyTreasuryNet(invoices, expenses, focusMonth)
        val currentMonthExpenses = TreasuryCalculations.monthlyDepenses(expenses, focusMonth)
        val deficit = -currentMonthNet.coerceAtMost(0.0)
        val deficitRatio = if (currentMonthExpenses > 0) deficit / currentMonthExpenses else 0.0
        val overdueInvoices = invoices.filter { it.status != InvoiceStatus.PAID && it.dueDate.isBefore(today) }
        val overdueAmount = overdueInvoices.sumOf { it.remainingAmount }
        val upcoming7Days = EcheanceForecast.buildItems(invoices, expenses, from = today, to = today.plusDays(7))
        val upcomingExpenses7Days = upcoming7Days.filter { it.type == EcheanceType.EXPENSE }.sumOf { it.amount }
        val annualForecast = buildAnnualTreasuryForecast(invoices, expenses, bankAccounts, focusYear = focusMonth.year, today = today)
        val negativeMonths = annualForecast.count { it.forecastBalance < 0 }
        val yearlyRows = TreasuryCalculations.yearlyRows(invoices, expenses, focusMonth.year, openingBalance)
        val currentForecastBalance = yearlyRows.find { it.month == focusMonth }?.forecastBalance ?: (openingBalance + currentMonthNet)

        val recommendations = mutableListOf<TreasuryRecommendation>()

        // Niveau critique : solde prévisionnel négatif + plusieurs mois déficitaires
        if (currentForecastBalance < 0 && negativeMonths >= 3) {
            recommendations.add(
                TreasuryRecommendation(
                    title = "credit_financing",
                    description = "multiple_negative_months",
                    severity = RecommendationSeverity.CRITICAL,
                    actionLabel = "contact_bank",
                    estimateImpact = "évite le découvert"
                )
            )
            recommendations.add(
                TreasuryRecommendation(
                    title = "partner_contribution",
                    description = "urgent_cash_injection",
                    severity = RecommendationSeverity.CRITICAL,
                    actionLabel = "ask_associates",
                    estimateImpact = "apport rapide"
                )
            )
            recommendations.add(
                TreasuryRecommendation(
                    title = "cut_expenses",
                    description = "cancel_postpone_expenses",
                    severity = RecommendationSeverity.CRITICAL,
                    actionLabel = "review_expenses",
                    estimateImpact = "réduit le besoin"
                )
            )
            return recommendations
        }

        // Niveau grave : solde négatif ou déficit > 50%
        if (currentForecastBalance < 0 || deficitRatio > 0.5) {
            recommendations.add(
                TreasuryRecommendation(
                    title = "accelerate_collection",
                    description = "call_overdue_clients",
                    severity = RecommendationSeverity.SEVERE,
                    actionLabel = "view_overdue",
                    estimateImpact = "+ ${formatDouble(overdueAmount)} DT potentiel"
                )
            )
            recommendations.add(
                TreasuryRecommendation(
                    title = "cash_sale",
                    description = "propose_cash_payment",
                    severity = RecommendationSeverity.SEVERE,
                    actionLabel = "add_income",
                    estimateImpact = "entrée immédiate"
                )
            )
            recommendations.add(
                TreasuryRecommendation(
                    title = "postpone_expenses",
                    description = "postpone_non_urgent",
                    severity = RecommendationSeverity.SEVERE,
                    actionLabel = "view_upcoming",
                    estimateImpact = "-${formatDouble(upcomingExpenses7Days)} DT reportable"
                )
            )
            return recommendations
        }

        // Niveau modéré : déficit 10-50%
        if (deficitRatio in 0.1..0.5) {
            recommendations.add(
                TreasuryRecommendation(
                    title = "send_reminders",
                    description = "remind_overdue_clients",
                    severity = RecommendationSeverity.MODERATE,
                    actionLabel = "view_overdue",
                    estimateImpact = "+ ${formatDouble(overdueAmount)} DT"
                )
            )
            recommendations.add(
                TreasuryRecommendation(
                    title = "advance_invoice",
                    description = "ask_client_advance",
                    severity = RecommendationSeverity.MODERATE,
                    actionLabel = "view_invoices",
                    estimateImpact = "accélère l'encaissement"
                )
            )
            recommendations.add(
                TreasuryRecommendation(
                    title = "partner_advance",
                    description = "ask_partner_contribution",
                    severity = RecommendationSeverity.MODERATE,
                    actionLabel = "ask_associates",
                    estimateImpact = "solution interne"
                )
            )
            return recommendations
        }

        // Niveau léger : déficit < 10% ou tout va bien
        if (deficitRatio > 0.0 && deficitRatio < 0.1) {
            recommendations.add(
                TreasuryRecommendation(
                    title = "light_reminder",
                    description = "send_friendly_reminder",
                    severity = RecommendationSeverity.LIGHT,
                    actionLabel = "view_overdue",
                    estimateImpact = "+ ${formatDouble(overdueAmount)} DT"
                )
            )
        }
        if (currentForecastBalance >= 0 && negativeMonths == 0) {
            recommendations.add(
                TreasuryRecommendation(
                    title = "healthy_treasury",
                    description = "build_reserve_or_invest",
                    severity = RecommendationSeverity.LIGHT,
                    actionLabel = "view_forecasts",
                    estimateImpact = "sécurise l'avenir"
                )
            )
        }

        return recommendations.take(3)
    }

    private fun formatDouble(value: Double): String =
        java.text.NumberFormat.getInstance(Locale.getDefault()).apply { maximumFractionDigits = 0 }.format(value)

    fun buildBreakEvenSummary(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        bankAccounts: List<BankAccount>,
        focusMonth: YearMonth = YearMonth.now(),
        today: LocalDate = LocalDate.now()
    ): BreakEvenSummary {
        val openingBalance = TreasuryCalculations.manualOpeningBalance(bankAccounts)
        val previousMonth = focusMonth.minusMonths(1)
        val previousMonthBalance = TreasuryCalculations.calendarYearChartRows(
            invoices = invoices,
            expenses = expenses,
            year = previousMonth.year,
            today = YearMonth.from(today),
            openingBalance = openingBalance
        ).find { it.month == previousMonth }?.forecastCumulative
            ?: TreasuryCalculations.yearlyForecastBalance(invoices, expenses, previousMonth.year, openingBalance)

        val projectedExpenses = TreasuryCalculations.monthlyDepenses(expenses, focusMonth)
        val historicalAverageExpenses = if (projectedExpenses > 0) {
            projectedExpenses
        } else {
            val pastMonths = (1..6).map { focusMonth.minusMonths(it.toLong()) }
            val pastTotals = pastMonths.map { TreasuryCalculations.monthlyDepenses(expenses, it) }
            val nonZero = pastTotals.filter { it > 0 }
            if (nonZero.isNotEmpty()) nonZero.average() else 0.0
        }

        val expensesToCover = projectedExpenses.coerceAtLeast(historicalAverageExpenses)
        val targetRevenue = (expensesToCover - previousMonthBalance).coerceAtLeast(0.0)
        val achievedRevenue = TreasuryCalculations.monthlyEncaissements(invoices, focusMonth)
        val remainingRevenue = (targetRevenue - achievedRevenue).coerceAtLeast(0.0)
        val isAchieved = achievedRevenue >= targetRevenue
        val additionalMargin = if (isAchieved) achievedRevenue - targetRevenue else 0.0

        return BreakEvenSummary(
            targetRevenue = targetRevenue,
            achievedRevenue = achievedRevenue,
            remainingRevenue = remainingRevenue,
            projectedExpenses = expensesToCover,
            previousMonthBalance = previousMonthBalance,
            additionalMargin = additionalMargin,
            isAchieved = isAchieved
        )
    }

    fun buildAnnualTreasuryForecast(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        bankAccounts: List<BankAccount>,
        focusYear: Int = YearMonth.now().year,
        today: LocalDate = LocalDate.now()
    ): List<AnnualTreasuryPoint> {
        val openingBalance = TreasuryCalculations.manualOpeningBalance(bankAccounts)
        val todayMonth = YearMonth.from(today)
        val year = if (focusYear == todayMonth.year) todayMonth.year else focusYear
        val rows = TreasuryCalculations.yearlyRows(
            invoices = invoices,
            expenses = expenses,
            year = year,
            openingBalance = openingBalance
        )
        return rows.map { row ->
            val isPastOrCurrent = !row.month.isAfter(todayMonth)
            val net = if (isPastOrCurrent) {
                row.balance
            } else {
                row.totalIncome - row.totalExpenses
            }
            AnnualTreasuryPoint(
                month = row.month,
                label = row.month.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()),
                forecastBalance = net,
                income = row.collected + row.pendingIncome,
                expenses = row.expenses + row.pendingExpenses
            )
        }
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

    /** Revenus / dépenses payées par mois, fenêtre glissante (transactions uniquement). */
    fun buildRollingMonthlyBarChart(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        focusMonth: YearMonth = YearMonth.now(),
        monthCount: Int = 6
    ): List<MonthlyBarPoint> {
        val months = (monthCount - 1 downTo 0).map { offset ->
            focusMonth.minusMonths(offset.toLong())
        }
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
        bankAccounts: List<BankAccount> = emptyList(),
        focusMonth: YearMonth = YearMonth.now(),
        viewMode: DashboardViewMode = DashboardViewMode.YEAR,
        today: LocalDate = LocalDate.now()
    ): DashboardData = buildInnovativeDashboard(
        invoices = invoices,
        expenses = expenses,
        bankBalance = bankBalance,
        bankAccounts = bankAccounts,
        focusMonth = focusMonth,
        viewMode = viewMode,
        today = today
    )

    fun buildInnovativeDashboard(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        bankBalance: Double?,
        bankAccounts: List<BankAccount> = emptyList(),
        focusMonth: YearMonth = YearMonth.now(),
        viewMode: DashboardViewMode = DashboardViewMode.YEAR,
        today: LocalDate = LocalDate.now()
    ): DashboardData {
        val referenceDate = resolveReferenceDate(focusMonth, viewMode, today)
        val openingBalance = TreasuryCalculations.manualOpeningBalance(bankAccounts)

        val calculatedBank = when (viewMode) {
            DashboardViewMode.YEAR -> {
                TreasuryCalculations.yearlyForecastBalance(invoices, expenses, focusMonth.year, openingBalance)
            }
            DashboardViewMode.MONTH -> {
                val year = focusMonth.year
                var balance = openingBalance
                for (m in 1..focusMonth.monthValue) {
                    val currentMonth = YearMonth.of(year, m)
                    balance += (TreasuryCalculations.monthlyCollections(invoices, currentMonth) +
                        TreasuryCalculations.pendingInvoiceAmount(invoices, currentMonth) -
                        TreasuryCalculations.monthlyPaidExpenses(expenses, currentMonth) -
                        TreasuryCalculations.monthlyUnpaidExpenses(expenses, currentMonth))
                }
                balance
            }
        }
        val displayBalance = bankBalance ?: calculatedBank

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
            calculatedBalance = calculatedBank,
            balanceFromBank = bankBalance != null,
            monthLabel = periodLabel,
            balanceHistory = buildHistoryCurve(invoices, expenses, displayBalance, referenceDate, bankOnly = true),
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
        today: LocalDate = LocalDate.now(),
        bankOnly: Boolean = false
    ): List<DashboardBalancePoint> {
        val start = today.minusDays(HISTORY_DAYS.toLong())
        val allDays = generateSequence(start) { prev ->
            if (prev.isBefore(today)) prev.plusDays(1) else null
        }.toList() + today

        val dailyNet = allDays.associateWith { day ->
            netCashFlowOnDay(invoices, expenses, day, bankOnly = bankOnly)
        }

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
            val paidInMonth = invoice.payments
                .filter { YearMonth.from(it.date) == month }
                .sumOf { it.amount }
            val pendingInMonth = if (YearMonth.from(invoice.dueDate) == month && invoice.paidAmount < invoice.totalAmount) {
                invoice.totalAmount - invoice.paidAmount
            } else {
                0.0
            }
            if (paidInMonth > 0 || pendingInMonth > 0) {
                listOf(CategorySelection.displayIncome(invoice.category, invoice.categoryLabel) to (paidInMonth + pendingInMonth))
            } else {
                emptyList()
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
        val computed = computedBankBalance(invoices, expenses)
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

    fun computedBalance(invoices: List<Invoice>, expenses: List<Expense>): Double =
        computedBankBalance(invoices, expenses)

    fun computedBankBalance(invoices: List<Invoice>, expenses: List<Expense>): Double =
        TreasuryCalculations.computedBankBalance(invoices, expenses)

    fun computedBalanceAtDate(invoices: List<Invoice>, expenses: List<Expense>, date: LocalDate): Double {
        val collected = invoices.flatMap { it.payments }
            .filter { !it.date.isAfter(date) && it.affectsBankTreasury() }
            .sumOf { it.amount }
        val paid = expenses
            .filter { it.affectsBankTreasury() && !it.date.isAfter(date) }
            .sumOf { it.amount }
        return collected - paid
    }

    fun monthlyBankForecastBalance(invoices: List<Invoice>, expenses: List<Expense>, month: YearMonth): Double {
        val year = month.year
        var balance = TreasuryCalculations.openingBankBalanceAtYearStart(invoices, expenses, year)
        for (m in 1..month.monthValue) {
            val currentMonth = YearMonth.of(year, m)
            balance += (TreasuryCalculations.monthlyBankCollections(invoices, currentMonth) +
                TreasuryCalculations.pendingInvoiceAmount(invoices, currentMonth) -
                TreasuryCalculations.monthlyBankPaidExpenses(expenses, currentMonth) -
                TreasuryCalculations.monthlyUnpaidExpenses(expenses, currentMonth))
        }
        return balance
    }

    @Deprecated("Use monthlyBankForecastBalance", ReplaceWith("monthlyBankForecastBalance(invoices, expenses, month)"))
    fun monthlyForecastBalance(invoices: List<Invoice>, expenses: List<Expense>, month: YearMonth): Double =
        monthlyBankForecastBalance(invoices, expenses, month)

    fun buildMonthComparison(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        focusMonth: YearMonth = YearMonth.now()
    ): Pair<MonthFinancialSummary, MonthFinancialSummary> {
        val current = buildMonthSummary(invoices, expenses, focusMonth)
        val previous = buildMonthSummary(invoices, expenses, focusMonth.minusMonths(1))
        return current to previous
    }

    fun buildMonthSummary(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        month: YearMonth
    ): MonthFinancialSummary = MonthFinancialSummary(
        month = month,
        label = AppLocale.monthYear(month),
        income = TreasuryCalculations.monthlyCollections(invoices, month),
        expenses = TreasuryCalculations.monthlyPaidExpenses(expenses, month)
    )

    fun buildUpcomingExpensePayments(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        today: LocalDate = LocalDate.now(),
        limit: Int = 10,
        locale: Locale = AppLocale.current()
    ): List<UpcomingPaymentBar> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM", locale)
        return EcheanceForecast.buildItems(
            invoices = invoices,
            expenses = expenses,
            from = today,
            to = today.plusYears(2)
        )
            .asSequence()
            .filter { it.type == EcheanceType.EXPENSE && !it.dueDate.isBefore(today) }
            .sortedBy { it.dueDate }
            .take(limit)
            .map { item ->
                UpcomingPaymentBar(
                    dueDate = item.dueDate,
                    amount = item.amount,
                    label = item.dueDate.format(formatter)
                )
            }
            .toList()
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
        today: LocalDate,
        bankOnly: Boolean = false
    ): List<DashboardBalancePoint> {
        val start = today.minusDays(HISTORY_DAYS.toLong())
        val end = today.plusDays(FORECAST_DAYS.toLong())
        val allDays = generateSequence(start) { prev ->
            if (prev.isBefore(end)) prev.plusDays(1) else null
        }.toList()

        val dailyNet = allDays.associateWith { day ->
            netCashFlowOnDay(invoices, expenses, day, bankOnly = bankOnly)
        }

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
                (dailyNet[d] ?: forecastFlowOnDay(invoices, expenses, d, bankOnly = bankOnly)).toDouble()
            }
            DashboardBalancePoint(day, anchorBalance + flowSinceToday, isForecast = true)
        }

        return historyPoints + forecastPoints
    }

    private fun netCashFlowOnDay(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        day: LocalDate,
        bankOnly: Boolean = false
    ): Double {
        val income = invoices.flatMap { it.payments }
            .filter { it.date == day && (!bankOnly || it.affectsBankTreasury()) }
            .sumOf { it.amount }
        val outcome = expenses
            .filter { it.isPaid && expenseOccursOn(it, day) && (!bankOnly || it.affectsBankTreasury()) }
            .sumOf { it.amount }
        return income - outcome
    }

    private fun forecastFlowOnDay(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        day: LocalDate,
        bankOnly: Boolean = false
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
