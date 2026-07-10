package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TransactionSignatureTest {

    @Test
    fun normalizeLabel_ignoresLongReferenceNumbers() {
        val a = TransactionSignature.of(
            LocalDate.of(2026, 3, 10),
            500.0,
            "VIR SEPA CLIENT"
        )
        val b = TransactionSignature.of(
            LocalDate.of(2026, 3, 10),
            500.0,
            "VIR SEPA CLIENT 20260315001234"
        )
        assertEquals(a, b)
    }

    @Test
    fun monthlyCollections_ignoresDuplicateBankImports() {
        val month = YearMonth.of(2026, 3)
        val payment = Payment(
            invoiceId = "inv1",
            amount = 500.0,
            date = LocalDate.of(2026, 3, 10),
            method = PaymentMethod.TRANSFER
        )
        val invoices = listOf(
            Invoice(
                id = "inv1",
                invoiceNumber = "F1",
                clientName = "VIR SEPA CLIENT",
                totalAmount = 500.0,
                paidAmount = 500.0,
                dueDate = LocalDate.of(2026, 3, 10),
                payments = listOf(payment)
            ),
            Invoice(
                id = "inv2",
                invoiceNumber = "F2",
                clientName = "VIR SEPA CLIENT 20260315001234",
                totalAmount = 500.0,
                paidAmount = 500.0,
                dueDate = LocalDate.of(2026, 3, 10),
                payments = listOf(
                    payment.copy(id = "pay2", invoiceId = "inv2")
                )
            )
        )

        assertEquals(500.0, TreasuryCalculations.monthlyCollections(invoices, month), 0.001)
        assertEquals(500.0, TreasuryCalculations.yearlyCollections(invoices, 2026), 0.001)
    }

    @Test
    fun monthlyPaidExpenses_ignoresAdjustmentsAndDuplicates() {
        val month = YearMonth.of(2026, 3)
        val expenses = listOf(
            Expense(
                label = "LOYER",
                amount = 800.0,
                date = LocalDate.of(2026, 3, 5),
                isPaid = true
            ),
            Expense(
                label = TreasuryAdjustmentLabels.EXPENSE,
                amount = 150.0,
                date = LocalDate.of(2026, 3, 5),
                isPaid = true
            ),
            Expense(
                label = "LOYER",
                amount = 800.0,
                date = LocalDate.of(2026, 3, 5),
                isPaid = true
            )
        )
        assertEquals(800.0, TreasuryCalculations.monthlyPaidExpenses(expenses, month), 0.001)
    }
}
