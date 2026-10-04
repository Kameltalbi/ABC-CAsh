package com.abccash.app.treasury.data

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

data class DashboardKpi(
    val amount: Double,
    val previous: Double,
    val changePercent: Double?
)

data class DashboardTreasuryPoint(
    val month: YearMonth,
    val closing: Double,
    val forecast: Boolean
)

data class DashboardYearSnapshot(
    val year: Int,
    val sales: DashboardKpi,
    val expenses: DashboardKpi,
    val result: DashboardKpi,
    val treasury: DashboardKpi,
    val selected: List<DashboardTreasuryPoint>,
    val previousYear: List<DashboardTreasuryPoint>
)

object DashboardYear {
    fun snapshot(
        year: Int,
        today: LocalDate,
        entrepriseId: String,
        accounts: List<BankAccount>,
        invoices: List<Invoice>,
        expenses: List<Expense>,
        pilotEntries: List<PilotEntry>,
        corrections: List<BalanceCorrection>
    ): DashboardYearSnapshot {
        val activityEnd = activityEnd(year, today)
        val comparisonEnd = comparisonEnd(year, today)
        val salesNow = activity(pilotEntries, entrepriseId, PilotEntryType.SALE, year, activityEnd)
        val salesThen = activity(pilotEntries, entrepriseId, PilotEntryType.SALE, year - 1, comparisonEnd)
        val expensesNow = activity(pilotEntries, entrepriseId, PilotEntryType.EXPENSE, year, activityEnd)
        val expensesThen = activity(pilotEntries, entrepriseId, PilotEntryType.EXPENSE, year - 1, comparisonEnd)
        val treasuryDate = treasuryDate(year, today)
        val treasuryThenDate = treasuryComparisonDate(year, today)
        val treasuryNow = cash(treasuryDate, entrepriseId, accounts, invoices, expenses, pilotEntries, corrections)
        val treasuryThen = cash(treasuryThenDate, entrepriseId, accounts, invoices, expenses, pilotEntries, corrections)
        return DashboardYearSnapshot(
            year = year,
            sales = kpi(salesNow, salesThen),
            expenses = kpi(expensesNow, expensesThen),
            result = kpi(PilotMoney.minus(salesNow, expensesNow), PilotMoney.minus(salesThen, expensesThen)),
            treasury = kpi(treasuryNow, treasuryThen),
            selected = curve(year, today, entrepriseId, accounts, invoices, expenses, pilotEntries, corrections),
            previousYear = curve(year - 1, today, entrepriseId, accounts, invoices, expenses, pilotEntries, corrections)
        )
    }

    private fun activityEnd(year: Int, today: LocalDate): LocalDate = when {
        year == today.year -> YearMonth.from(today).atEndOfMonth()
        else -> LocalDate.of(year, 12, 31)
    }

    private fun comparisonEnd(year: Int, today: LocalDate): LocalDate =
        if (year == today.year) activityEnd(year, today).minusYears(1) else LocalDate.of(year - 1, 12, 31)

    private fun treasuryDate(year: Int, today: LocalDate): LocalDate = when {
        year == today.year -> today
        else -> LocalDate.of(year, 12, 31)
    }

    private fun treasuryComparisonDate(year: Int, today: LocalDate): LocalDate =
        if (year == today.year) today.minusYears(1) else LocalDate.of(year - 1, 12, 31)

    private fun activity(
        entries: List<PilotEntry>,
        entrepriseId: String,
        type: PilotEntryType,
        year: Int,
        end: LocalDate
    ): Double {
        val start = LocalDate.of(year, 1, 1)
        return PilotMoney.sum(
            entries.filter {
                it.entrepriseId == entrepriseId &&
                    it.type == type &&
                    !it.date.isBefore(start) &&
                    !it.date.isAfter(end)
            }.map { it.amount }
        )
    }

    private fun cash(
        date: LocalDate,
        entrepriseId: String,
        accounts: List<BankAccount>,
        invoices: List<Invoice>,
        expenses: List<Expense>,
        pilotEntries: List<PilotEntry>,
        corrections: List<BalanceCorrection>
    ): Double = TreasuryMonth.balanceAsOf(
        date, entrepriseId, accounts, invoices, expenses, pilotEntries, corrections
    )

    private fun curve(
        year: Int,
        today: LocalDate,
        entrepriseId: String,
        accounts: List<BankAccount>,
        invoices: List<Invoice>,
        expenses: List<Expense>,
        pilotEntries: List<PilotEntry>,
        corrections: List<BalanceCorrection>
    ): List<DashboardTreasuryPoint> {
        val currentMonth = YearMonth.from(today)
        return (1..12).map { monthNumber ->
            val month = YearMonth.of(year, monthNumber)
            DashboardTreasuryPoint(
                month = month,
                closing = TreasuryMonth.snapshot(
                    month, entrepriseId, accounts, invoices, expenses, pilotEntries, corrections
                ).closing,
                forecast = month > currentMonth
            )
        }
    }

    private fun kpi(amount: Double, previous: Double): DashboardKpi =
        DashboardKpi(amount, previous, variation(amount, previous))

    private fun variation(current: Double, previous: Double): Double? {
        if (PilotMoney.same(previous, 0.0)) return null
        val ratio = PilotMoney.ratio(PilotMoney.minus(current, previous), abs(previous)) ?: return null
        return PilotMoney.percent(ratio)
    }
}
