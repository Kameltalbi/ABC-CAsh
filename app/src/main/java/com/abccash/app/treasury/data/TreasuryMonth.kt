package com.abccash.app.treasury.data

import java.time.LocalDate
import java.time.YearMonth

data class TreasuryLine(
    val pilotEntryId: String?,
    val pilotType: PilotEntryType?,
    val date: LocalDate,
    val signedAmount: Double,
    val bankAccountId: String,
    val label: String
)

data class TreasuryAccountMonth(
    val accountId: String,
    val name: String,
    val opening: Double,
    val closing: Double
)

data class TreasuryMonthSnapshot(
    val month: YearMonth,
    val opening: Double,
    val inflows: Double,
    val outflows: Double,
    val closing: Double,
    val accounts: List<TreasuryAccountMonth>,
    val inflowLines: List<TreasuryLine>,
    val outflowLines: List<TreasuryLine>
)

object TreasuryMonth {
    fun snapshot(
        month: YearMonth,
        entrepriseId: String,
        accounts: List<BankAccount>,
        invoices: List<Invoice>,
        expenses: List<Expense>,
        pilotEntries: List<PilotEntry>,
        corrections: List<BalanceCorrection>
    ): TreasuryMonthSnapshot {
        val companyAccounts = accounts.filter { it.entrepriseId == entrepriseId }
        val flows = movements(entrepriseId, companyAccounts, invoices, expenses, pilotEntries, corrections)
        val start = month.atDay(1)
        val knownIds = companyAccounts.map { it.id }.toSet()
        val accountRows = companyAccounts.map { account ->
            val stock = openingStock(account, corrections)
            val before = signed(flows.filter { it.bankAccountId == account.id && it.date.isBefore(start) })
            val during = signed(flows.filter { it.bankAccountId == account.id && YearMonth.from(it.date) == month })
            val opening = PilotMoney.sum(listOf(stock, before))
            TreasuryAccountMonth(
                accountId = account.id,
                name = account.name,
                opening = opening,
                closing = PilotMoney.sum(listOf(opening, during))
            )
        }.sortedBy { it.name.lowercase() }
        val strayBefore = signed(flows.filter { it.bankAccountId !in knownIds && it.date.isBefore(start) })
        val opening = PilotMoney.sum(accountRows.map { it.opening } + strayBefore)
        val monthFlows = flows.filter { YearMonth.from(it.date) == month }
        val inflows = signed(monthFlows.filter { it.signedAmount > 0 })
        val outflows = signed(monthFlows.filter { it.signedAmount < 0 }.map { it.copy(signedAmount = -it.signedAmount) })
        val closing = PilotMoney.minus(PilotMoney.sum(listOf(opening, inflows)), outflows)
        return TreasuryMonthSnapshot(
            month = month,
            opening = opening,
            inflows = inflows,
            outflows = outflows,
            closing = closing,
            accounts = accountRows,
            inflowLines = monthFlows.filter { it.signedAmount > 0 }.sortedWith(lineOrder),
            outflowLines = monthFlows.filter { it.signedAmount < 0 }.sortedWith(lineOrder)
        )
    }

    private val lineOrder = compareByDescending<TreasuryLine> { it.date }.thenBy { it.label }

    private fun signed(lines: List<TreasuryLine>): Double =
        PilotMoney.sum(lines.map { it.signedAmount })

    private fun movements(
        entrepriseId: String,
        accounts: List<BankAccount>,
        invoices: List<Invoice>,
        expenses: List<Expense>,
        pilotEntries: List<PilotEntry>,
        corrections: List<BalanceCorrection>
    ): List<TreasuryLine> {
        val lines = mutableListOf<TreasuryLine>()
        pilotEntries.filter { it.entrepriseId == entrepriseId }.forEach { entry ->
            PilotCash.items(entry).forEach { item ->
                lines += TreasuryLine(
                    pilotEntryId = entry.id,
                    pilotType = entry.type,
                    date = item.date,
                    signedAmount = item.signedAmount,
                    bankAccountId = item.bankAccountId,
                    label = item.label
                )
            }
        }
        val seenPayments = mutableSetOf<String>()
        invoices.filter { it.entrepriseId == entrepriseId }.forEach { invoice ->
            invoice.payments.forEach { payment ->
                if (!PilotMoney.isPositive(payment.amount)) return@forEach
                if (!seenPayments.add(TransactionSignature.payment(invoice, payment))) return@forEach
                lines += TreasuryLine(
                    pilotEntryId = null,
                    pilotType = null,
                    date = payment.date,
                    signedAmount = PilotMoney.decimal(payment.amount).toDouble(),
                    bankAccountId = resolveAccount(payment.bankAccountId, payment.affectsBankTreasury(), accounts),
                    label = invoice.clientName.ifBlank { invoice.invoiceNumber }
                )
            }
        }
        val seenExpenses = mutableSetOf<String>()
        expenses.filter { it.entrepriseId == entrepriseId && it.isPaid && PilotMoney.isPositive(it.amount) }.forEach { expense ->
            if (!seenExpenses.add(TransactionSignature.expense(expense))) return@forEach
            lines += TreasuryLine(
                pilotEntryId = null,
                pilotType = null,
                date = expense.date,
                signedAmount = -PilotMoney.decimal(expense.amount).toDouble(),
                bankAccountId = resolveAccount(expense.bankAccountId, expense.affectsBankTreasury(), accounts),
                label = expense.label
            )
        }
        corrections.filter {
            it.entrepriseId == entrepriseId && it.type == BalanceCorrectionType.CORRECTION
        }.forEach { correction ->
            val delta = PilotMoney.minus(correction.newBalance, correction.oldBalance)
            if (delta == 0.0) return@forEach
            lines += TreasuryLine(
                pilotEntryId = null,
                pilotType = null,
                date = correction.correctionDate,
                signedAmount = delta,
                bankAccountId = correction.bankAccountId,
                label = correction.motif
            )
        }
        return lines
    }

    fun balanceAsOf(
        date: LocalDate,
        entrepriseId: String,
        accounts: List<BankAccount>,
        invoices: List<Invoice>,
        expenses: List<Expense>,
        pilotEntries: List<PilotEntry>,
        corrections: List<BalanceCorrection>
    ): Double {
        val companyAccounts = accounts.filter { it.entrepriseId == entrepriseId }
        val flows = movements(entrepriseId, companyAccounts, invoices, expenses, pilotEntries, corrections)
        val stock = PilotMoney.sum(companyAccounts.map { openingStock(it, corrections) })
        val moved = signed(flows.filter { !it.date.isAfter(date) })
        return PilotMoney.sum(listOf(stock, moved))
    }

    private fun openingStock(account: BankAccount, corrections: List<BalanceCorrection>): Double {
        if (account.openingBalance != 0.0) return account.openingBalance
        return corrections
            .filter {
                it.entrepriseId == account.entrepriseId &&
                    it.type == BalanceCorrectionType.INITIAL &&
                    it.bankAccountId == account.id
            }
            .maxWithOrNull(compareBy<BalanceCorrection> { it.correctionDate }.thenBy { it.createdAt })
            ?.newBalance
            ?: 0.0
    }

    private fun resolveAccount(explicitId: String?, bank: Boolean, accounts: List<BankAccount>): String {
        if (!explicitId.isNullOrBlank()) return explicitId
        val kind = if (bank) TreasuryAccountKind.BANK else TreasuryAccountKind.CASH
        val pool = accounts.filter { it.kind == kind }.ifEmpty { accounts }
        return pool.firstOrNull { it.isDefault }?.id ?: pool.firstOrNull()?.id.orEmpty()
    }
}
