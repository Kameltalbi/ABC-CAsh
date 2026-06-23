package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BankAccountCalculationsTest {

    private val accountA = BankAccount(id = "a", entrepriseId = "e", name = "Main", openingBalance = 1000.0, isDefault = true)
    private val accountB = BankAccount(id = "b", entrepriseId = "e", name = "Secondary", openingBalance = 500.0)

    @Test
    fun balance_usesOpeningPlusBankPaymentsMinusBankExpenses() {
        val invoices = listOf(
            invoiceWithPayment(
                paymentId = "p1",
                amount = 200.0,
                method = PaymentMethod.TRANSFER,
                bankAccountId = "a"
            ),
            invoiceWithPayment(
                paymentId = "p2",
                amount = 50.0,
                method = PaymentMethod.CASH,
                bankAccountId = null
            )
        )
        val expenses = listOf(
            expense("x1", 80.0, PaymentMethod.TRANSFER, "a"),
            expense("x2", 30.0, PaymentMethod.CASH, null)
        )

        val balance = BankAccountCalculations.balance(accountA, invoices, expenses, defaultAccountId = "a")

        assertEquals(1120.0, balance, 0.001)
    }

    @Test
    fun unassignedBankMovements_goToDefaultAccount() {
        val invoices = listOf(
            invoiceWithPayment(
                paymentId = "p1",
                amount = 100.0,
                method = PaymentMethod.CHECK,
                bankAccountId = null
            )
        )
        val expenses = listOf(
            expense("x1", 40.0, PaymentMethod.CREDIT_CARD, null)
        )

        val defaultBalance = BankAccountCalculations.balance(accountA, invoices, expenses, defaultAccountId = "a")
        val secondaryBalance = BankAccountCalculations.balance(accountB, invoices, expenses, defaultAccountId = "a")

        assertEquals(1060.0, defaultBalance, 0.001)
        assertEquals(500.0, secondaryBalance, 0.001)
    }

    @Test
    fun summarize_flagsLowBalanceAlert() {
        val lowAlertAccount = accountA.copy(alertLowBalance = 2000.0)
        val summaries = BankAccountCalculations.summarize(
            accounts = listOf(lowAlertAccount),
            invoices = emptyList(),
            expenses = emptyList()
        )

        assertEquals(1, summaries.size)
        assertTrue(summaries.first().hasLowBalanceAlert)
    }

    private fun invoiceWithPayment(
        paymentId: String,
        amount: Double,
        method: PaymentMethod,
        bankAccountId: String?
    ): Invoice = Invoice(
        id = "inv-$paymentId",
        invoiceNumber = "F-$paymentId",
        clientName = "Client",
        totalAmount = amount,
        dueDate = LocalDate.of(2026, 1, 15),
        createdDate = LocalDate.of(2026, 1, 1),
        entrepriseId = "e",
        payments = listOf(
            Payment(
                id = paymentId,
                invoiceId = "inv-$paymentId",
                amount = amount,
                date = LocalDate.of(2026, 1, 10),
                method = method,
                bankAccountId = bankAccountId
            )
        )
    )

    private fun expense(
        id: String,
        amount: Double,
        method: PaymentMethod,
        bankAccountId: String?
    ): Expense = Expense(
        id = id,
        label = "Expense $id",
        amount = amount,
        date = LocalDate.of(2026, 1, 12),
        isPaid = true,
        paymentMethod = method,
        createdDate = LocalDate.of(2026, 1, 12),
        entrepriseId = "e",
        bankAccountId = bankAccountId
    )
}
