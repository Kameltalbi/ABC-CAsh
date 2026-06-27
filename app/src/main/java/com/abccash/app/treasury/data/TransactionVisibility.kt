package com.abccash.app.treasury.data

import java.time.YearMonth

/** Encaissement réalisé (au moins un paiement enregistré). */
fun Invoice.isRealizedTransaction(): Boolean = paidAmount > 0

fun Invoice.transactionDateIn(month: YearMonth): Boolean {
    // Encaissement réalisé uniquement : visible dans le mois du (des) paiement(s).
    return payments.any { YearMonth.from(it.date) == month }
}

fun Invoice.displayTransactionDate(): java.time.LocalDate =
    payments.maxByOrNull { it.date }?.date ?: dueDate

/** Dépense réglée. */
fun Expense.isRealizedTransaction(): Boolean = isPaid

fun Expense.appearsInTransactions(month: YearMonth): Boolean =
    isRealizedTransaction() && appliesToMonth(month)
