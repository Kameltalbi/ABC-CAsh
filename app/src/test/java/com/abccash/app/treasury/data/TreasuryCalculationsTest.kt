package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TreasuryCalculationsTest {

    private val march = YearMonth.of(2026, 3)

    @Test
    fun `forecast does not double count unpaid expenses`() {
        val invoices = listOf(
            Invoice(
                invoiceNumber = "F1",
                clientName = "Client",
                totalAmount = 1000.0,
                paidAmount = 1000.0,
                dueDate = LocalDate.of(2026, 3, 15),
                payments = listOf(
                    Payment(
                        invoiceId = "inv1",
                        amount = 1000.0,
                        date = LocalDate.of(2026, 3, 10),
                        method = PaymentMethod.CASH
                    )
                )
            )
        )
        val expenses = listOf(
            Expense(
                label = "Loyer",
                amount = 200.0,
                date = LocalDate.of(2026, 3, 1),
                isPaid = false
            )
        )

        val forecast = TreasuryCalculations.forecastedBalance(invoices, expenses, march)

        assertEquals(800.0, forecast, 0.001)
    }

    @Test
    fun `forecast includes pending invoices and subtracts unpaid expenses`() {
        val invoices = listOf(
            Invoice(
                invoiceNumber = "F1",
                clientName = "Client",
                totalAmount = 500.0,
                dueDate = LocalDate.of(2026, 3, 20)
            )
        )
        val expenses = listOf(
            Expense(
                label = "Salaire",
                amount = 100.0,
                date = LocalDate.of(2026, 3, 5),
                isPaid = true
            ),
            Expense(
                label = "Fournisseur",
                amount = 50.0,
                date = LocalDate.of(2026, 3, 8),
                isPaid = false
            )
        )

        val forecast = TreasuryCalculations.forecastedBalance(invoices, expenses, march)

        assertEquals(350.0, forecast, 0.001)
    }

    @Test
    fun `monthly balance uses only paid expenses`() {
        val expenses = listOf(
            Expense(
                label = "Payée",
                amount = 100.0,
                date = LocalDate.of(2026, 3, 1),
                isPaid = true
            ),
            Expense(
                label = "À venir",
                amount = 300.0,
                date = LocalDate.of(2026, 3, 1),
                isPaid = false
            )
        )

        val paid = TreasuryCalculations.monthlyPaidExpenses(expenses, march)
        val balance = TreasuryCalculations.monthlyBalance(1000.0, paid)

        assertEquals(100.0, paid, 0.001)
        assertEquals(900.0, balance, 0.001)
    }
}
