package com.abccash.app.treasury.data

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseExtensionsTest {

    @Test
    fun nonRecurring_appliesOnlyToSameMonth() {
        val expense = Expense(
            label = "One-off",
            amount = 100.0,
            date = LocalDate.of(2026, 6, 15)
        )
        assertTrue(expense.appliesToMonth(YearMonth.of(2026, 6)))
        assertFalse(expense.appliesToMonth(YearMonth.of(2026, 7)))
    }

    @Test
    fun monthlyRecurring_appliesToFutureMonths() {
        val expense = Expense(
            label = "Loyer",
            amount = 500.0,
            date = LocalDate.of(2026, 3, 10),
            isRecurring = true,
            recurrence = ExpenseRecurrence.MONTHLY
        )
        assertTrue(expense.appliesToMonth(YearMonth.of(2026, 6)))
        assertFalse(expense.appliesToMonth(YearMonth.of(2026, 2)))
    }

    @Test
    fun weeklyRecurring_matchesWeeksInsideMonth() {
        val expense = Expense(
            label = "Hebdo",
            amount = 50.0,
            date = LocalDate.of(2026, 6, 3),
            isRecurring = true,
            recurrence = ExpenseRecurrence.WEEKLY
        )
        assertTrue(expense.appliesToMonth(YearMonth.of(2026, 6)))
        assertFalse(expense.appliesToMonth(YearMonth.of(2026, 5)))
    }

    @Test
    fun recurrenceEndDate_stopsAfterEnd() {
        val expense = Expense(
            label = "Temp",
            amount = 80.0,
            date = LocalDate.of(2026, 4, 1),
            isRecurring = true,
            recurrence = ExpenseRecurrence.MONTHLY,
            recurrenceEndDate = LocalDate.of(2026, 5, 31)
        )
        assertTrue(expense.appliesToMonth(YearMonth.of(2026, 5)))
        assertFalse(expense.appliesToMonth(YearMonth.of(2026, 6)))
    }
}
