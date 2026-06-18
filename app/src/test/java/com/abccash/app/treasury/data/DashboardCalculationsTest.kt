package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DashboardCalculationsTest {

    @Test
    fun `computed balance subtracts paid expenses from collected invoices`() {
        val invoices = listOf(
            sampleInvoice(paid = 500.0, total = 500.0)
        )
        val expenses = listOf(
            sampleExpense(amount = 200.0, paid = true)
        )

        assertEquals(300.0, DashboardCalculations.computedBalance(invoices, expenses), 0.01)
    }

    @Test
    fun `innovative dashboard aggregates forecast totals`() {
        val today = LocalDate.of(2026, 6, 18)
        val invoices = listOf(
            sampleInvoice(paid = 0.0, total = 500.0, dueDate = today.plusDays(10))
        )
        val expenses = listOf(
            sampleExpense(amount = 120.0, date = today.plusDays(5), paid = false)
        )

        val data = DashboardCalculations.buildInnovativeDashboard(
            invoices = invoices,
            expenses = expenses,
            bankBalance = 2_000.0,
            today = today
        )

        assertEquals(2_000.0, data.bankBalance, 0.01)
        assertEquals(500.0, data.forecastIncome, 0.01)
        assertEquals(120.0, data.forecastExpenses, 0.01)
        assertTrue(data.balanceHistory.isNotEmpty())
    }

    @Test
    fun `snapshot uses bank balance when provided`() {
        val snapshot = DashboardCalculations.buildSnapshot(
            invoices = emptyList(),
            expenses = emptyList(),
            bankBalance = 12_450.0,
            today = LocalDate.of(2026, 6, 18)
        )

        assertEquals(12_450.0, snapshot.bankBalance, 0.01)
        assertTrue(snapshot.accountUpToDate)
        assertTrue(snapshot.balanceCurve.isNotEmpty())
    }

    @Test
    fun `monthly counts reflect current month activity`() {
        val month = YearMonth.of(2026, 6)
        val today = month.atDay(15)
        val invoices = listOf(
            sampleInvoice(
                paid = 100.0,
                total = 100.0,
                paymentDate = month.atDay(5)
            )
        )
        val expenses = listOf(
            sampleExpense(amount = 50.0, date = month.atDay(10), paid = true)
        )

        val snapshot = DashboardCalculations.buildSnapshot(
            invoices = invoices,
            expenses = expenses,
            bankBalance = 1_000.0,
            today = today
        )

        assertEquals(100.0, snapshot.monthIncome, 0.01)
        assertEquals(50.0, snapshot.monthExpenses, 0.01)
        assertEquals(1, snapshot.paidInvoicesCount)
        assertEquals(1, snapshot.expenseEntriesCount)
    }

    private fun sampleInvoice(
        paid: Double,
        total: Double,
        paymentDate: LocalDate = LocalDate.now(),
        dueDate: LocalDate = paymentDate
    ): Invoice {
        val id = "inv-test"
        return Invoice(
            id = id,
            invoiceNumber = "F-1",
            clientName = "Client",
            totalAmount = total,
            paidAmount = paid,
            dueDate = dueDate,
            payments = if (paid > 0) {
                listOf(
                    Payment(
                        invoiceId = id,
                        amount = paid,
                        date = paymentDate,
                        method = PaymentMethod.CASH
                    )
                )
            } else {
                emptyList()
            }
        )
    }

    private fun sampleExpense(
        amount: Double,
        date: LocalDate = LocalDate.now(),
        paid: Boolean = true
    ): Expense = Expense(
        label = "Charge",
        amount = amount,
        date = date,
        isPaid = paid
    )
}
