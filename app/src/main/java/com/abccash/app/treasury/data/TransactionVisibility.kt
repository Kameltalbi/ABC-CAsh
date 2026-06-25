package com.abccash.app.treasury.data

import java.time.YearMonth

/** Encaissement réalisé (au moins un paiement enregistré). */
fun Invoice.isRealizedTransaction(): Boolean = paidAmount > 0

fun Invoice.transactionDateIn(month: YearMonth): Boolean {
    // Encaissement déjà réglé : visible dans le mois du (des) paiement(s).
    if (payments.any { YearMonth.from(it.date) == month }) return true
    // Encaissement non encore réglé (à encaisser) : visible dans le mois de l'échéance.
    if (payments.isEmpty()) return YearMonth.from(dueDate) == month
    return false
}

fun Invoice.displayTransactionDate(): java.time.LocalDate =
    payments.maxByOrNull { it.date }?.date ?: dueDate

/** Dépense réglée. */
fun Expense.isRealizedTransaction(): Boolean = isPaid

fun Expense.appearsInTransactions(month: YearMonth): Boolean =
    isRealizedTransaction() && appliesToMonth(month)
