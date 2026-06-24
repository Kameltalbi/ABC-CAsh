package com.abccash.app.treasury.data

import java.time.LocalDate

object BankAccountCalculations {

    fun defaultAccountId(accounts: List<BankAccount>, kind: TreasuryAccountKind): String? =
        accounts.firstOrNull { it.isDefault && it.kind == kind }?.id
            ?: accounts.firstOrNull { it.kind == kind }?.id

    fun belongsToAccount(
        bankAccountId: String?,
        account: BankAccount,
        defaultBankAccountId: String?,
        defaultCashAccountId: String?
    ): Boolean {
        if (bankAccountId != null) return bankAccountId == account.id
        val defaultId = when (account.kind) {
            TreasuryAccountKind.BANK -> defaultBankAccountId
            TreasuryAccountKind.CASH -> defaultCashAccountId
        }
        return account.isDefault || account.id == defaultId
    }

    private fun paymentMatchesAccount(
        payment: Payment,
        account: BankAccount,
        defaultBankAccountId: String?,
        defaultCashAccountId: String?
    ): Boolean {
        val matchesKind = when (account.kind) {
            TreasuryAccountKind.BANK -> payment.affectsBankTreasury()
            TreasuryAccountKind.CASH -> payment.affectsCashTreasury()
        }
        if (!matchesKind) return false
        return belongsToAccount(payment.bankAccountId, account, defaultBankAccountId, defaultCashAccountId)
    }

    private fun expenseMatchesAccount(
        expense: Expense,
        account: BankAccount,
        defaultBankAccountId: String?,
        defaultCashAccountId: String?
    ): Boolean {
        val matchesKind = when (account.kind) {
            TreasuryAccountKind.BANK -> expense.affectsBankTreasury()
            TreasuryAccountKind.CASH -> expense.affectsCashTreasury()
        }
        if (!matchesKind) return false
        return belongsToAccount(expense.bankAccountId, account, defaultBankAccountId, defaultCashAccountId)
    }

    fun balance(
        account: BankAccount,
        invoices: List<Invoice>,
        expenses: List<Expense>,
        defaultBankAccountId: String?,
        defaultCashAccountId: String?
    ): Double {
        var total = account.openingBalance
        invoices.flatMap { invoice ->
            invoice.payments
                .filter { paymentMatchesAccount(it, account, defaultBankAccountId, defaultCashAccountId) }
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
            .filter { expenseMatchesAccount(it, account, defaultBankAccountId, defaultCashAccountId) }
            .forEach { total -= it.amount }

        return total
    }

    fun movements(
        account: BankAccount,
        invoices: List<Invoice>,
        expenses: List<Expense>,
        defaultBankAccountId: String?,
        defaultCashAccountId: String?
    ): List<BankAccountMovement> {
        val income = invoices.flatMap { invoice ->
            invoice.payments
                .filter { paymentMatchesAccount(it, account, defaultBankAccountId, defaultCashAccountId) }
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
            .filter { expenseMatchesAccount(it, account, defaultBankAccountId, defaultCashAccountId) }
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
        val defaultBankId = defaultAccountId(accounts, TreasuryAccountKind.BANK)
        val defaultCashId = defaultAccountId(accounts, TreasuryAccountKind.CASH)
        return accounts.map { account ->
            val accountMovements = movements(account, invoices, expenses, defaultBankId, defaultCashId)
            val accountBalance = balance(account, invoices, expenses, defaultBankId, defaultCashId)
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
        defaultBankAccountId: String?,
        defaultCashAccountId: String?
    ): LocalDate? = movements(account, invoices, expenses, defaultBankAccountId, defaultCashAccountId)
        .firstOrNull()
        ?.date
}
