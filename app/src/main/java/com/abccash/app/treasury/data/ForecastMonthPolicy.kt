package com.abccash.app.treasury.data

import com.abccash.app.locale.AppLocale
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class ClosedMonthForecastPending(
    val month: YearMonth,
    val expenseCount: Int,
    val invoiceCount: Int,
    val purgeDate: LocalDate,
    val daysUntilPurge: Int
) {
    val monthLabel: String get() = AppLocale.monthYear(month)
}

object ForecastMonthPolicy {
    const val GRACE_DAYS = 7

    fun earliestSelectableMonth(today: LocalDate = LocalDate.now()): YearMonth =
        YearMonth.from(today)

    fun clampMonth(month: YearMonth, today: LocalDate = LocalDate.now()): YearMonth {
        val earliest = earliestSelectableMonth(today)
        return if (month.isBefore(earliest)) earliest else month
    }

    fun canSelectMonth(month: YearMonth, today: LocalDate = LocalDate.now()): Boolean =
        !month.isBefore(earliestSelectableMonth(today))

    fun purgeDateForMonth(closedMonth: YearMonth): LocalDate =
        closedMonth.plusMonths(1).atDay(1).plusDays(GRACE_DAYS.toLong())

    fun daysUntilPurge(closedMonth: YearMonth, today: LocalDate = LocalDate.now()): Int {
        val purgeDate = purgeDateForMonth(closedMonth)
        return ChronoUnit.DAYS.between(today, purgeDate).toInt().coerceAtLeast(0)
    }

    fun isInGracePeriod(closedMonth: YearMonth, today: LocalDate = LocalDate.now()): Boolean {
        val currentMonth = YearMonth.from(today)
        if (!closedMonth.isBefore(currentMonth)) return false
        return today.isBefore(purgeDateForMonth(closedMonth))
    }

    fun shouldPurgeExpense(expense: Expense, today: LocalDate = LocalDate.now()): Boolean {
        if (expense.isPaid || expense.isRecurring) return false
        val month = YearMonth.from(expense.date)
        if (!month.isBefore(YearMonth.from(today))) return false
        return !today.isBefore(purgeDateForMonth(month))
    }

    fun pendingClosedMonths(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        today: LocalDate = LocalDate.now()
    ): List<ClosedMonthForecastPending> {
        val currentMonth = YearMonth.from(today)
        val closedMonths = buildSet {
            invoices.filter { it.status != InvoiceStatus.PAID && it.remainingAmount > 0 }
                .map { YearMonth.from(it.dueDate) }
                .filter { it.isBefore(currentMonth) }
                .forEach { add(it) }
            expenses.filter { !it.isPaid && !it.isRecurring }
                .map { YearMonth.from(it.date) }
                .filter { it.isBefore(currentMonth) }
                .forEach { add(it) }
        }

        return closedMonths
            .filter { isInGracePeriod(it, today) }
            .sortedDescending()
            .mapNotNull { month ->
                val expenseCount = expenses.count { expense ->
                    !expense.isPaid &&
                        !expense.isRecurring &&
                        YearMonth.from(expense.date) == month
                }
                val invoiceCount = invoices.count { invoice ->
                    invoice.status != InvoiceStatus.PAID &&
                        invoice.remainingAmount > 0 &&
                        YearMonth.from(invoice.dueDate) == month
                }
                if (expenseCount == 0 && invoiceCount == 0) return@mapNotNull null
                ClosedMonthForecastPending(
                    month = month,
                    expenseCount = expenseCount,
                    invoiceCount = invoiceCount,
                    purgeDate = purgeDateForMonth(month),
                    daysUntilPurge = daysUntilPurge(month, today)
                )
            }
    }

    fun expensesToPurge(
        expenses: List<Expense>,
        today: LocalDate = LocalDate.now()
    ): List<Expense> = expenses.filter { shouldPurgeExpense(it, today) }
}
