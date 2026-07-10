package com.abccash.app.treasury.data

import java.time.YearMonth

/** Encaissement réalisé (au moins un paiement enregistré). */
fun Invoice.isRealizedTransaction(): Boolean = paidAmount > 0

/** Ajustement automatique de rapprochement bancaire. */
fun Invoice.isTreasuryAdjustment(): Boolean =
    invoiceNumber.startsWith("AJUST-") ||
        clientName == TreasuryAdjustmentLabels.INVOICE_CLIENT ||
        clientName == TreasuryAdjustmentLabels.LEGACY_LABEL ||
        clientName.startsWith("Ajustement")

fun Invoice.transactionDateIn(month: YearMonth): Boolean {
    // Encaissement réalisé uniquement : visible dans le mois du (des) paiement(s).
    return payments.any { YearMonth.from(it.date) == month }
}

fun Invoice.displayTransactionDate(): java.time.LocalDate =
    payments.maxByOrNull { it.date }?.date ?: dueDate

/** Dépense réglée. */
fun Expense.isRealizedTransaction(): Boolean = isPaid

/** Ajustement automatique de rapprochement bancaire. */
fun Expense.isTreasuryAdjustment(): Boolean =
    label == TreasuryAdjustmentLabels.EXPENSE ||
        label == TreasuryAdjustmentLabels.LEGACY_LABEL ||
        label.startsWith("Ajustement")

fun Expense.appearsInTransactions(month: YearMonth): Boolean =
    isRealizedTransaction() && appliesToMonth(month)
