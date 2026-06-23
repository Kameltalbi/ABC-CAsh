package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PaymentMethodRoutingTest {

    private val month = YearMonth.of(2026, 6)

    @Test
    fun `cash payment affects caisse not bank`() {
        val invoices = listOf(
            invoiceWithPayment(100.0, PaymentMethod.CASH)
        )
        val expenses = emptyList<Expense>()

        assertEquals(0.0, TreasuryCalculations.computedBankBalance(invoices, expenses), 0.001)
        assertEquals(100.0, TreasuryCalculations.computedCashBalance(invoices, expenses), 0.001)
    }

    @Test
    fun `transfer payment affects bank not caisse`() {
        val invoices = listOf(
            invoiceWithPayment(250.0, PaymentMethod.TRANSFER)
        )

        assertEquals(250.0, TreasuryCalculations.computedBankBalance(invoices, emptyList()), 0.001)
        assertEquals(0.0, TreasuryCalculations.computedCashBalance(invoices, emptyList()), 0.001)
    }

    @Test
    fun `cash expense affects caisse only`() {
        val expenses = listOf(
            Expense(
                label = "Courses",
                amount = 30.0,
                date = LocalDate.of(2026, 6, 10),
                isPaid = true,
                paymentMethod = PaymentMethod.CASH
            )
        )

        assertEquals(0.0, TreasuryCalculations.computedBankBalance(emptyList(), expenses), 0.001)
        assertEquals(-30.0, TreasuryCalculations.computedCashBalance(emptyList(), expenses), 0.001)
    }

    @Test
    fun `activity totals still include all payment methods`() {
        val invoices = listOf(
            invoiceWithPayment(50.0, PaymentMethod.CASH),
            invoiceWithPayment(200.0, PaymentMethod.TRANSFER)
        )

        assertEquals(250.0, TreasuryCalculations.monthlyCollections(invoices, month), 0.001)
    }

    private fun invoiceWithPayment(amount: Double, method: PaymentMethod): Invoice {
        val id = "inv-$amount-$method"
        return Invoice(
            id = id,
            invoiceNumber = "F-$amount",
            clientName = "Client",
            totalAmount = amount,
            paidAmount = amount,
            dueDate = LocalDate.of(2026, 6, 15),
            payments = listOf(
                Payment(
                    invoiceId = id,
                    amount = amount,
                    date = LocalDate.of(2026, 6, 15),
                    method = method
                )
            )
        )
    }
}
