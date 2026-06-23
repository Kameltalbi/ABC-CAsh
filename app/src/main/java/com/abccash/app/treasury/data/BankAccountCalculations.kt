package com.abccash.app.treasury.data

import java.time.LocalDate

object BankAccountCalculations {

    fun belongsToAccount(
        bankAccountId: String?,
        account: BankAccount,
        defaultAccountId: String?
    ): Boolean {
        if (bankAccountId != null) return bankAccountId == account.id
        return account.isDefault || account.id == defaultAccountId
    }

    fun balance(
        account: BankAccount,
        invoices: List<Invoice>,
        expenses: List<Expense>,
        defaultAccountId: String?
    ): Double {
        var total = account.openingBalance
        invoices.flatMap { invoice ->
            invoice.payments
                .filter { it.affectsBankTreasury() && belongsToAccount(it.bankAccountId, account, defaultAccountId) }
                .map { payment ->
                    BankAccountMovement(
                        id = payment.id,
                        date = payment.date,
                        label = invoice.clientName,
                        amount = payment.amount,
                        method = payment.method,
                        type = BankAccountMovementType.INCOME
                    )
                }
        }.forEach { total += it.amount }

        expenses
            .filter { it.affectsBankTreasury() && belongsToAccount(it.bankAccountId, account, defaultAccountId) }
            .forEach { total -= it.amount }

        return total
    }

    fun movements(
        account: BankAccount,
        invoices: List<Invoice>,
        expenses: List<Expense>,
        defaultAccountId: String?
    ): List<BankAccountMovement> {
        val income = invoices.flatMap { invoice ->
            invoice.payments
                .filter { it.affectsBankTreasury() && belongsToAccount(it.bankAccountId, account, defaultAccountId) }
                .map { payment ->
                    BankAccountMovement(
                        id = payment.id,
                        date = payment.date,
                        label = invoice.clientName.ifBlank { invoice.invoiceNumber },
                        amount = payment.amount,
                        method = payment.method,
                        type = BankAccountMovementType.INCOME
                    )
                }
        }
        val outcome = expenses
            .filter { it.affectsBankTreasury() && belongsToAccount(it.bankAccountId, account, defaultAccountId) }
            .map { expense ->
                BankAccountMovement(
                    id = expense.id,
                    date = expense.date,
                    label = expense.label,
                    amount = expense.amount,
                    method = expense.paymentMethod ?: PaymentMethod.TRANSFER,
                    type = BankAccountMovementType.EXPENSE
                )
            }
        return (income + outcome).sortedByDescending { it.date }
    }

    fun summarize(
        accounts: List<BankAccount>,
        invoices: List<Invoice>,
        expenses: List<Expense>
    ): List<BankAccountSummary> {
        val defaultId = accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id
        return accounts.map { account ->
            val accountMovements = movements(account, invoices, expenses, defaultId)
            val accountBalance = balance(account, invoices, expenses, defaultId)
            BankAccountSummary(
                account = account,
                balance = accountBalance,
                movementCount = accountMovements.size,
                hasLowBalanceAlert = account.alertLowBalance?.let { accountBalance < it } == true
            )
        }
    }

    fun lastMovementDate(
        account: BankAccount,
        invoices: List<Invoice>,
        expenses: List<Expense>,
        defaultAccountId: String?
    ): LocalDate? = movements(account, invoices, expenses, defaultAccountId)
        .firstOrNull()
        ?.date
}
