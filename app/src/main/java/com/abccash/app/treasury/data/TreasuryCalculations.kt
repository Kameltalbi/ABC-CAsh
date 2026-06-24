package com.abccash.app.treasury.data

import java.time.YearMonth

object TreasuryCalculations {

    // --- Activité (tous modes) — listes, graphiques encaissements/dépenses ---

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

    // --- Banque (hors espèces) — solde trésorerie bancaire ---

    fun monthlyBankCollections(invoices: List<Invoice>, month: YearMonth): Double =
        invoices.flatMap { it.payments }
            .filter { YearMonth.from(it.date) == month && it.affectsBankTreasury() }
            .sumOf { it.amount }

    fun monthlyBankPaidExpenses(expenses: List<Expense>, month: YearMonth): Double =
        expenses.filter { it.affectsBankTreasury() && it.appliesToMonth(month) }
            .sumOf { it.amount }

    fun monthlyBankUnpaidExpenses(expenses: List<Expense>, month: YearMonth): Double =
        expenses.filter { !it.isPaid && it.appliesToMonth(month) }
            .sumOf { it.amount }

    fun yearlyBankCollections(invoices: List<Invoice>, year: Int): Double =
        invoices.flatMap { it.payments }
            .filter { it.date.year == year && it.affectsBankTreasury() }
            .sumOf { it.amount }

    fun yearlyBankPaidExpenses(expenses: List<Expense>, year: Int): Double =
        (1..12).sumOf { month ->
            monthlyBankPaidExpenses(expenses, YearMonth.of(year, month))
        }

    fun yearlyBankBalance(invoices: List<Invoice>, expenses: List<Expense>, year: Int): Double =
        yearlyBankCollections(invoices, year) - yearlyBankPaidExpenses(expenses, year)

    fun openingBankBalanceAtYearStart(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        year: Int
    ): Double {
        val collectedBefore = invoices.flatMap { it.payments }
            .filter { it.date.year < year && it.affectsBankTreasury() }
            .sumOf { it.amount }
        val paidBefore = expenses
            .filter { it.affectsBankTreasury() && it.date.year < year }
            .sumOf { it.amount }
        return collectedBefore - paidBefore
    }

    fun computedBankBalance(invoices: List<Invoice>, expenses: List<Expense>): Double {
        val collected = invoices.flatMap { it.payments }
            .filter { it.affectsBankTreasury() }
            .sumOf { it.amount }
        val paid = expenses.filter { it.affectsBankTreasury() }.sumOf { it.amount }
        return collected - paid
    }

    fun computedCashBalance(invoices: List<Invoice>, expenses: List<Expense>): Double {
        val collected = invoices.flatMap { it.payments }
            .filter { it.affectsCashTreasury() }
            .sumOf { it.amount }
        val paid = expenses.filter { it.affectsCashTreasury() }.sumOf { it.amount }
        return collected - paid
    }

    // --- Legacy / cumul global ---

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

    fun yearlyBankForecastBalance(invoices: List<Invoice>, expenses: List<Expense>, year: Int): Double =
        yearlyBankCollections(invoices, year) +
            yearlyPendingIncome(invoices, year) -
            yearlyBankPaidExpenses(expenses, year) -
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

    fun openingBalanceBeforeMonth(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        month: YearMonth
    ): Double {
        val collectedBefore = invoices.flatMap { it.payments }
            .filter { YearMonth.from(it.date) < month }
            .sumOf { it.amount }
        val paidBefore = expenses
            .filter { it.isPaid && YearMonth.from(it.date) < month }
            .sumOf { it.amount }
        return collectedBefore - paidBefore
    }

    fun currentRealizedBalance(invoices: List<Invoice>, expenses: List<Expense>): Double {
        val collected = invoices.flatMap { it.payments }.sumOf { it.amount }
        val paid = expenses.filter { it.isPaid }.sumOf { it.amount }
        return collected - paid
    }

    data class RollingTreasuryRow(
        val month: YearMonth,
        val collected: Double,
        val pendingIncome: Double,
        val paidExpenses: Double,
        val pendingExpenses: Double,
        val realizedCumulative: Double,
        val forecastCumulative: Double
    )

    fun rollingRows(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        anchor: YearMonth = YearMonth.now(),
        monthsBefore: Int = 5,
        monthsAfter: Int = 6
    ): List<RollingTreasuryRow> {
        val start = anchor.minusMonths(monthsBefore.toLong())
        val totalMonths = monthsBefore + monthsAfter + 1
        var realizedCumulative = openingBalanceBeforeMonth(invoices, expenses, start)
        var forecastCumulative = realizedCumulative
        val now = YearMonth.now()

        return (0 until totalMonths).map { offset ->
            val month = start.plusMonths(offset.toLong())
            val collected = monthlyCollections(invoices, month)
            val pendingIncome = pendingInvoiceAmount(invoices, month)
            val paidExpenses = monthlyPaidExpenses(expenses, month)
            val pendingExpenses = monthlyUnpaidExpenses(expenses, month)

            if (month <= now) {
                realizedCumulative += collected - paidExpenses
            }
            forecastCumulative += (collected + pendingIncome) - (paidExpenses + pendingExpenses)

            RollingTreasuryRow(
                month = month,
                collected = collected,
                pendingIncome = pendingIncome,
                paidExpenses = paidExpenses,
                pendingExpenses = pendingExpenses,
                realizedCumulative = realizedCumulative,
                forecastCumulative = forecastCumulative
            )
        }
    }

    /**
     * Calendar-year chart: realized = actuals through [today], forecast = projection from today's
     * realized balance plus remaining due dates (not a full-year pending stack from January).
     */
    fun calendarYearChartRows(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        year: Int,
        today: YearMonth = YearMonth.now()
    ): List<RollingTreasuryRow> {
        val realizedToday = currentRealizedBalance(invoices, expenses)
        var yearRealized = openingBalanceAtYearStart(invoices, expenses, year)

        return (1..12).map { monthNumber ->
            val month = YearMonth.of(year, monthNumber)
            val collected = monthlyCollections(invoices, month)
            val pendingIncome = pendingInvoiceAmount(invoices, month)
            val paidExpenses = monthlyPaidExpenses(expenses, month)
            val pendingExpenses = monthlyUnpaidExpenses(expenses, month)

            if (month <= today) {
                yearRealized += collected - paidExpenses
            }

            val realizedPoint = when {
                month < today -> yearRealized
                else -> realizedToday
            }
            val forecastPoint = forecastEndOfMonthBalance(
                invoices = invoices,
                expenses = expenses,
                month = month,
                today = today,
                realizedToday = realizedToday,
                year = year
            )

            RollingTreasuryRow(
                month = month,
                collected = collected,
                pendingIncome = pendingIncome,
                paidExpenses = paidExpenses,
                pendingExpenses = pendingExpenses,
                realizedCumulative = realizedPoint,
                forecastCumulative = forecastPoint
            )
        }
    }

    private fun forecastEndOfMonthBalance(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        month: YearMonth,
        today: YearMonth,
        realizedToday: Double,
        year: Int
    ): Double {
        if (month < today) {
            var running = openingBalanceAtYearStart(invoices, expenses, year)
            for (monthNumber in 1..month.monthValue) {
                val cursor = YearMonth.of(year, monthNumber)
                running += monthlyCollections(invoices, cursor) - monthlyPaidExpenses(expenses, cursor)
            }
            return running
        }
        if (month == today) {
            return realizedToday +
                pendingInvoiceAmount(invoices, month) -
                monthlyUnpaidExpenses(expenses, month)
        }
        var balance = forecastEndOfMonthBalance(
            invoices = invoices,
            expenses = expenses,
            month = today,
            today = today,
            realizedToday = realizedToday,
            year = year
        )
        var cursor = today.plusMonths(1)
        while (cursor <= month) {
            balance += pendingInvoiceAmount(invoices, cursor) - monthlyUnpaidExpenses(expenses, cursor)
            cursor = cursor.plusMonths(1)
        }
        return balance
    }
}
