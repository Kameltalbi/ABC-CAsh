package com.abccash.app.treasury.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TransactionVisibilityTest {

    @Test
    fun `unpaid invoice is forecast only`() {
        val invoice = Invoice(
            invoiceNumber = "F1",
            clientName = "Client",
            totalAmount = 100.0,
            dueDate = LocalDate.of(2026, 6, 15)
        )
        assertFalse(invoice.isRealizedTransaction())
        assertFalse(invoice.transactionDateIn(YearMonth.of(2026, 6)))
    }

    @Test
    fun `paid invoice appears in transactions by payment month`() {
        val invoice = Invoice(
            invoiceNumber = "F1",
            clientName = "Client",
            totalAmount = 100.0,
            paidAmount = 100.0,
            dueDate = LocalDate.of(2026, 6, 15),
            payments = listOf(
                Payment(
                    invoiceId = "x",
                    amount = 100.0,
                    date = LocalDate.of(2026, 6, 20),
                    method = PaymentMethod.CASH
                )
            )
        )
        assertTrue(invoice.isRealizedTransaction())
        assertTrue(invoice.transactionDateIn(YearMonth.of(2026, 6)))
        assertFalse(invoice.transactionDateIn(YearMonth.of(2026, 7)))
    }

    @Test
    fun `unpaid expense is forecast only`() {
        val expense = Expense(
            label = "Loyer",
            amount = 200.0,
            date = LocalDate.of(2026, 6, 5),
            isPaid = false
        )
        assertFalse(expense.isRealizedTransaction())
        assertFalse(expense.appearsInTransactions(YearMonth.of(2026, 6)))
    }

    @Test
    fun `paid expense appears in transactions`() {
        val expense = Expense(
            label = "Loyer",
            amount = 200.0,
            date = LocalDate.of(2026, 6, 5),
            isPaid = true,
            paymentMethod = PaymentMethod.TRANSFER
        )
        assertTrue(expense.isRealizedTransaction())
        assertTrue(expense.appearsInTransactions(YearMonth.of(2026, 6)))
    }
}
