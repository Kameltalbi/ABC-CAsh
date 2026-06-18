package com.abccash.app.treasury.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

fun Expense.appliesToMonth(month: YearMonth): Boolean {
    if (!isRecurring) {
        return YearMonth.from(date) == month
    }

    val startMonth = YearMonth.from(date)
    if (month < startMonth) return false

    val recurrenceType = recurrence ?: return YearMonth.from(date) == month

    val matchesPattern = when (recurrenceType) {
        ExpenseRecurrence.WEEKLY -> {
            val monthStart = month.atDay(1)
            val monthEnd = month.atEndOfMonth()
            if (monthEnd.isBefore(date)) return false
            var cursor = date
            while (!cursor.isAfter(monthEnd)) {
                if (!cursor.isBefore(monthStart)) return true
                cursor = cursor.plusWeeks(1)
            }
            false
        }
        else -> {
            val interval = recurrenceType.monthsInterval ?: 1
            ChronoUnit.MONTHS.between(startMonth, month) % interval == 0L
        }
    }
    if (!matchesPattern) return false

    val day = minOf(date.dayOfMonth, month.lengthOfMonth())
    val occurrence = month.atDay(day)
    return recurrenceEndDate == null || !occurrence.isAfter(recurrenceEndDate)
}

fun Expense.occurrenceDateIn(month: YearMonth): LocalDate? {
    if (!appliesToMonth(month)) return null
    val day = minOf(date.dayOfMonth, month.lengthOfMonth())
    return month.atDay(day)
}

fun List<Expense>.forMonth(month: YearMonth): List<Expense> {
    return filter { it.appliesToMonth(month) }
        .sortedByDescending { it.occurrenceDateIn(month) }
}
