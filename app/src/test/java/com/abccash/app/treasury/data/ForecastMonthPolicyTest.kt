package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ForecastMonthPolicyTest {
    private val today = LocalDate.of(2026, 7, 10)
    private val currentMonth = YearMonth.of(2026, 7)
    private val closedMonth = YearMonth.of(2026, 6)

    @Test
    fun clampMonth_movesPastMonthsToCurrent() {
        assertEquals(currentMonth, ForecastMonthPolicy.clampMonth(YearMonth.of(2026, 3), today))
        assertEquals(currentMonth, ForecastMonthPolicy.clampMonth(currentMonth, today))
        assertEquals(YearMonth.of(2026, 8), ForecastMonthPolicy.clampMonth(YearMonth.of(2026, 8), today))
    }

    @Test
    fun gracePeriod_lastsSevenDaysAfterMonthEnd() {
        assertTrue(ForecastMonthPolicy.isInGracePeriod(closedMonth, LocalDate.of(2026, 7, 1)))
        assertTrue(ForecastMonthPolicy.isInGracePeriod(closedMonth, LocalDate.of(2026, 7, 7)))
        assertFalse(ForecastMonthPolicy.isInGracePeriod(closedMonth, LocalDate.of(2026, 7, 8)))
    }

    @Test
    fun shouldPurgeExpense_onlyOneOffUnpaidAfterGrace() {
        val expense = Expense(
            label = "Loyer",
            amount = 1000.0,
            date = LocalDate.of(2026, 6, 15),
            isPaid = false,
            isRecurring = false
        )
        assertFalse(ForecastMonthPolicy.shouldPurgeExpense(expense, LocalDate.of(2026, 7, 5)))
        assertTrue(ForecastMonthPolicy.shouldPurgeExpense(expense, LocalDate.of(2026, 7, 8)))
    }

    @Test
    fun shouldPurgeExpense_skipsRecurringAndPaid() {
        val recurring = Expense(
            label = "Abonnement",
            amount = 50.0,
            date = LocalDate.of(2026, 6, 1),
            isPaid = false,
            isRecurring = true,
            recurrence = ExpenseRecurrence.MONTHLY
        )
        assertFalse(ForecastMonthPolicy.shouldPurgeExpense(recurring, LocalDate.of(2026, 7, 10)))

        val paid = Expense(
            label = "Done",
            amount = 10.0,
            date = LocalDate.of(2026, 6, 1),
            isPaid = true,
            isRecurring = false
        )
        assertFalse(ForecastMonthPolicy.shouldPurgeExpense(paid, LocalDate.of(2026, 7, 10)))
    }

    @Test
    fun pendingClosedMonths_listsGracePeriodItems() {
        val expenses = listOf(
            Expense(
                label = "Prévision",
                amount = 200.0,
                date = LocalDate.of(2026, 6, 20),
                isPaid = false,
                isRecurring = false
            )
        )
        val invoices = listOf(
            Invoice(
                invoiceNumber = "FAC-001",
                clientName = "Client",
                totalAmount = 500.0,
                paidAmount = 0.0,
                dueDate = LocalDate.of(2026, 6, 10)
            )
        )

        val pending = ForecastMonthPolicy.pendingClosedMonths(
            invoices,
            expenses,
            LocalDate.of(2026, 7, 3)
        )
        assertEquals(1, pending.size)
        assertEquals(closedMonth, pending.first().month)
        assertEquals(1, pending.first().expenseCount)
        assertEquals(1, pending.first().invoiceCount)
    }
}
