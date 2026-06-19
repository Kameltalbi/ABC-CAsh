package com.abccash.app.treasury.data

import java.time.YearMonth

/** Encaissement réalisé (au moins un paiement enregistré). */
fun Invoice.isRealizedTransaction(): Boolean = paidAmount > 0

fun Invoice.transactionDateIn(month: YearMonth): Boolean {
    if (!isRealizedTransaction()) return false
    if (payments.any { YearMonth.from(it.date) == month }) return true
    return payments.isEmpty() && YearMonth.from(dueDate) == month
}

fun Invoice.displayTransactionDate(): java.time.LocalDate =
    payments.maxByOrNull { it.date }?.date ?: dueDate

/** Dépense réglée. */
fun Expense.isRealizedTransaction(): Boolean = isPaid

fun Expense.appearsInTransactions(month: YearMonth): Boolean =
    isRealizedTransaction() && appliesToMonth(month)
