package com.abccash.app.treasury.data

import java.time.LocalDate
import java.time.YearMonth

object TreasuryCalculations {

    // --- Activité (tous modes) — listes, graphiques encaissements/dépenses ---

    fun monthlyCollections(invoices: List<Invoice>, month: YearMonth): Double =
        distinctPaymentsInMonth(invoices, month).sumOf { it.amount }

    private data class RealizedPayment(val date: LocalDate, val amount: Double)

    /** Paiements uniques du mois (ignore les doublons d'import : même date, montant, libellé). */
    private fun distinctPaymentsInMonth(invoices: List<Invoice>, month: YearMonth): List<RealizedPayment> {
        val seen = mutableSetOf<String>()
        return invoices
            .flatMap { invoice ->
                invoice.payments.map { payment -> payment to invoice }
            }
            .filter { (payment, _) -> YearMonth.from(payment.date) == month }
            .sortedBy { (payment, _) -> payment.date }
            .mapNotNull { (payment, invoice) ->
                val signature = TransactionSignature.payment(invoice, payment)
                if (!seen.add(signature)) return@mapNotNull null
                RealizedPayment(payment.date, payment.amount)
            }
    }

    fun distinctPaidExpensesInMonth(
        expenses: List<Expense>,
        month: YearMonth,
        today: LocalDate = LocalDate.now()
    ): List<Expense> {
        val seen = mutableSetOf<String>()
        return expenses
            .filter { expensePaidInMonth(it, month, today) }
            .sortedBy { it.date }
            .filter { seen.add(TransactionSignature.expense(it)) }
    }

    /**
     * Dépenses payées réalisées dans un mois.
     * - Opérations ponctuelles (relevés bancaires) : date exacte du mouvement.
     * - Récurrentes payées : une occurrence par mois, uniquement si la date est passée.
     * Évite de compter 12 fois une charge récurrente sur toute l'année d'un coup.
     */
    fun monthlyPaidExpenses(
        expenses: List<Expense>,
        month: YearMonth,
        today: LocalDate = LocalDate.now()
    ): Double =
        distinctPaidExpensesInMonth(expenses, month, today)
            .sumOf { it.amount }

    private fun expensePaidInMonth(expense: Expense, month: YearMonth, today: LocalDate): Boolean {
        if (!expense.isPaid) return false
        if (!expense.isRecurring) return YearMonth.from(expense.date) == month
        val occurrence = expense.occurrenceDateIn(month) ?: return false
        return !occurrence.isAfter(today)
    }

    /** Somme des encaissements réalisés de janvier jusqu'au mois inclus. */
    fun ytdCollections(
        invoices: List<Invoice>,
        throughMonth: YearMonth = YearMonth.now()
    ): Double = monthsThroughYear(throughMonth).sumOf { monthlyCollections(invoices, it) }

    /** Somme des dépenses payées réalisées de janvier jusqu'au mois inclus. */
    fun ytdPaidExpenses(
        expenses: List<Expense>,
        throughMonth: YearMonth = YearMonth.now(),
        today: LocalDate = LocalDate.now()
    ): Double = monthsThroughYear(throughMonth).sumOf { monthlyPaidExpenses(expenses, it, today) }

    /** Solde réalisé cumulé = ouverture + encaissements YTD − dépenses YTD. */
    fun ytdRealizedBalance(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        openingBalance: Double,
        throughMonth: YearMonth = YearMonth.now(),
        today: LocalDate = LocalDate.now()
    ): Double =
        openingBalance +
            ytdCollections(invoices, throughMonth) -
            ytdPaidExpenses(expenses, throughMonth, today)

    private fun monthsThroughYear(throughMonth: YearMonth): List<YearMonth> =
        (1..throughMonth.monthValue).map { YearMonth.of(throughMonth.year, it) }

    /** Dépenses non payées avec échéance strictement future (vraies prévisions). */
    fun monthlyUnpaidExpenses(
        expenses: List<Expense>,
        month: YearMonth,
        today: LocalDate = LocalDate.now()
    ): Double =
        expenses.filter { expense ->
            if (expense.isPaid) return@filter false
            val due = expenseForecastDate(expense, month) ?: return@filter false
            due.isAfter(today)
        }.sumOf { it.amount }

    fun monthlyBalance(collections: Double, paidExpenses: Double): Double =
        collections - paidExpenses

    /** Encaissements prévisionnels : factures impayées dont l'échéance est strictement future. */
    fun pendingInvoiceAmount(
        invoices: List<Invoice>,
        month: YearMonth,
        today: LocalDate = LocalDate.now()
    ): Double =
        invoices.filter {
            it.status != InvoiceStatus.PAID &&
                YearMonth.from(it.dueDate) == month &&
                it.dueDate.isAfter(today)
        }.sumOf { it.remainingAmount }

    /** Total des prévisions futures sur l'année (encaissements). Zéro si tout est réalisé. */
    fun yearlyFuturePendingIncome(
        invoices: List<Invoice>,
        year: Int,
        today: LocalDate = LocalDate.now()
    ): Double =
        (1..12).sumOf { month ->
            pendingInvoiceAmount(invoices, YearMonth.of(year, month), today)
        }

    /** Total des prévisions futures sur l'année (dépenses). Zéro si tout est réalisé. */
    fun yearlyFuturePendingExpenses(
        expenses: List<Expense>,
        year: Int,
        today: LocalDate = LocalDate.now()
    ): Double =
        (1..12).sumOf { month ->
            monthlyUnpaidExpenses(expenses, YearMonth.of(year, month), today)
        }

    private fun expenseForecastDate(expense: Expense, month: YearMonth): LocalDate? =
        if (expense.isRecurring) expense.occurrenceDateIn(month) else expense.date

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

    fun monthlyBankCollections(invoices: List<Invoice>, month: YearMonth): Double {
        val seen = mutableSetOf<String>()
        return invoices
            .flatMap { invoice -> invoice.payments.map { it to invoice } }
            .filter { (payment, _) ->
                YearMonth.from(payment.date) == month && payment.affectsBankTreasury()
            }
            .sumOf { (payment, invoice) ->
                val signature = TransactionSignature.payment(invoice, payment)
                if (seen.add(signature)) payment.amount else 0.0
            }
    }

    fun monthlyBankPaidExpenses(
        expenses: List<Expense>,
        month: YearMonth,
        today: LocalDate = LocalDate.now()
    ): Double =
        distinctPaidExpensesInMonth(expenses, month, today)
            .filter { it.affectsBankTreasury() }
            .sumOf { it.amount }

    fun monthlyBankUnpaidExpenses(expenses: List<Expense>, month: YearMonth): Double =
        expenses.filter { !it.isPaid && it.appliesToMonth(month) }
            .sumOf { it.amount }

    fun yearlyBankCollections(invoices: List<Invoice>, year: Int): Double =
        (1..12).sumOf { month ->
            monthlyBankCollections(invoices, YearMonth.of(year, month))
        }

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

    /** Sum of opening balances entered when creating bank/cash accounts. */
    fun manualOpeningBalance(accounts: List<BankAccount>): Double =
        accounts.sumOf { it.openingBalance }

    @Deprecated(
        "Use manualOpeningBalance(bankAccounts) from account setup instead",
        ReplaceWith("manualOpeningBalance(accounts)")
    )
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
        (1..12).sumOf { month ->
            monthlyCollections(invoices, YearMonth.of(year, month))
        }

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

    fun yearlyForecastBalance(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        year: Int,
        openingBalance: Double = 0.0
    ): Double =
        yearlyRows(invoices, expenses, year, openingBalance).lastOrNull()?.forecastBalance ?: openingBalance

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

    fun yearlyRows(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        year: Int,
        openingBalance: Double = 0.0
    ): List<MonthlyTreasuryRow> {
        var cumulativeBalance = openingBalance
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

    fun MonthlyTreasuryRow.monthlyForecastNet(): Double = totalIncome - totalExpenses

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
        val seenPayments = mutableSetOf<String>()
        val collected = invoices
            .flatMap { invoice -> invoice.payments.map { it to invoice } }
            .sumOf { (payment, invoice) ->
                val signature = TransactionSignature.payment(invoice, payment)
                if (seenPayments.add(signature)) payment.amount else 0.0
            }
        val seenExpenses = mutableSetOf<String>()
        val paid = expenses
            .filter { it.isPaid }
            .sumOf { expense ->
                val signature = TransactionSignature.expense(expense)
                if (seenExpenses.add(signature)) expense.amount else 0.0
            }
        return collected - paid
    }

    /**
     * Solde de trésorerie actuel = solde d'ouverture + encaissements réalisés (espèces + banque)
     * − dépenses payées, sur tout l'historique. Source unique partagée par la page Trésorerie
     * et le Dashboard pour garantir un chiffre identique.
     */
    fun realizedBalance(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        opening: Double
    ): Double = opening + currentRealizedBalance(invoices, expenses)

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
        today: YearMonth = YearMonth.now(),
        openingBalance: Double = 0.0
    ): List<RollingTreasuryRow> {
        val realizedToday = currentRealizedBalance(invoices, expenses) + openingBalance
        var yearRealized = openingBalanceAtYearStart(invoices, expenses, year) + openingBalance

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
                year = year,
                openingBalance = openingBalance
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
        year: Int,
        openingBalance: Double = 0.0
    ): Double {
        if (month < today) {
            var running = openingBalanceAtYearStart(invoices, expenses, year) + openingBalance
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
            year = year,
            openingBalance = openingBalance
        )
        var cursor = today.plusMonths(1)
        while (cursor <= month) {
            balance += pendingInvoiceAmount(invoices, cursor) - monthlyUnpaidExpenses(expenses, cursor)
            cursor = cursor.plusMonths(1)
        }
        return balance
    }
}
