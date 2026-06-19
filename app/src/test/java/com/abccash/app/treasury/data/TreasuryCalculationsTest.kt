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
    fun `yearly balance aggregates all months`() {
        val year = 2026
        val invoices = listOf(
            Invoice(
                invoiceNumber = "F1",
                clientName = "Client",
                totalAmount = 1000.0,
                paidAmount = 1000.0,
                dueDate = java.time.LocalDate.of(2026, 3, 1),
                payments = listOf(
                    Payment(
                        invoiceId = "i1",
                        amount = 400.0,
                        date = java.time.LocalDate.of(2026, 2, 10),
                        method = PaymentMethod.CASH
                    ),
                    Payment(
                        invoiceId = "i1",
                        amount = 600.0,
                        date = java.time.LocalDate.of(2026, 8, 5),
                        method = PaymentMethod.CASH
                    )
                )
            )
        )
        val expenses = listOf(
            Expense(
                label = "Loyer",
                amount = 150.0,
                date = java.time.LocalDate.of(2026, 1, 1),
                isPaid = true
            )
        )

        assertEquals(1000.0, TreasuryCalculations.yearlyCollections(invoices, year), 0.001)
        assertEquals(150.0, TreasuryCalculations.yearlyPaidExpenses(expenses, year), 0.001)
        assertEquals(850.0, TreasuryCalculations.yearlyBalance(invoices, expenses, year), 0.001)
        assertEquals(12, TreasuryCalculations.yearlyRows(invoices, expenses, year).size)
    }

    @Test
    fun `yearly rows include pending amounts`() {
        val year = 2026
        val invoices = listOf(
            Invoice(
                invoiceNumber = "F1",
                clientName = "Client",
                totalAmount = 400.0,
                dueDate = LocalDate.of(2026, 4, 10)
            )
        )
        val expenses = listOf(
            Expense(
                label = "Charge",
                amount = 100.0,
                date = LocalDate.of(2026, 4, 5),
                isPaid = false
            )
        )
        val april = TreasuryCalculations.yearlyRows(invoices, expenses, year)[3]

        assertEquals(400.0, april.pendingIncome, 0.001)
        assertEquals(100.0, april.pendingExpenses, 0.001)
        assertEquals(300.0, april.forecastBalance, 0.001)
        assertEquals(400.0, april.totalIncome, 0.001)
        assertEquals(100.0, april.totalExpenses, 0.001)
    }

    @Test
    fun `yearly rows accumulate month by month balance`() {
        val year = 2026
        val invoices = listOf(
            sampleInvoiceWithPayment(500.0, LocalDate.of(2026, 1, 10)),
            sampleInvoiceWithPayment(200.0, LocalDate.of(2026, 2, 5))
        )
        val expenses = listOf(
            Expense(
                label = "Charge",
                amount = 150.0,
                date = LocalDate.of(2026, 2, 20),
                isPaid = true
            )
        )

        val rows = TreasuryCalculations.yearlyRows(invoices, expenses, year)

        assertEquals(500.0, rows[0].forecastBalance, 0.001)
        assertEquals(550.0, rows[1].forecastBalance, 0.001)
        assertEquals(550.0, rows[11].forecastBalance, 0.001)
    }

    private fun sampleInvoiceWithPayment(amount: Double, date: LocalDate): Invoice =
        Invoice(
            invoiceNumber = "F1",
            clientName = "Client",
            totalAmount = amount,
            paidAmount = amount,
            dueDate = date,
            payments = listOf(
                Payment(
                    invoiceId = "inv",
                    amount = amount,
                    date = date,
                    method = PaymentMethod.CASH
                )
            )
        )
}
