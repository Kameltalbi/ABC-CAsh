package com.abccash.app.treasury.data

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun nextOccurrenceAfter_advancesMonthlyRecurringExpense() {
        val expense = Expense(
            label = "Loyer",
            amount = 500.0,
            date = LocalDate.of(2026, 6, 24),
            isRecurring = true,
            recurrence = ExpenseRecurrence.MONTHLY,
            recurrenceEndDate = LocalDate.of(2026, 12, 31)
        )

        assertEquals(LocalDate.of(2026, 7, 24), expense.nextOccurrenceAfter(LocalDate.of(2026, 6, 24)))
        assertNull(expense.nextOccurrenceAfter(LocalDate.of(2026, 12, 24)))
    }

    @Test
    fun savesAsForecast_whenRecurringOrFutureOrForecastMode() {
        val today = LocalDate.of(2026, 6, 24)
        val recurring = Expense(
            label = "Loyer",
            amount = 500.0,
            date = today,
            isRecurring = true,
            recurrence = ExpenseRecurrence.MONTHLY
        )
        val oneOffToday = Expense(label = "Achat", amount = 10.0, date = today)

        assertTrue(recurring.savesAsForecast(forecastMode = false, today = today))
        assertTrue(oneOffToday.savesAsForecast(forecastMode = true, today = today))
        assertFalse(oneOffToday.savesAsForecast(forecastMode = false, today = today))
    }
}
