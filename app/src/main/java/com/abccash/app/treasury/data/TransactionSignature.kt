package com.abccash.app.treasury.data

import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Identifiant unique d'une opération bancaire (date + montant + libellé normalisé).
 * Utilisé pour détecter les doublons d'import et éviter de compter deux fois le même encaissement.
 */
object TransactionSignature {

    fun of(date: LocalDate, amount: Double, label: String): String {
        val normalizedAmount = (amount * 1000.0).roundToLong()
        val normalizedLabel = normalizeLabel(label)
        return "$date|$normalizedAmount|$normalizedLabel"
    }

    fun payment(invoice: Invoice, payment: Payment): String =
        of(payment.date, payment.amount, invoice.clientName)

    fun expense(expense: Expense): String =
        of(expense.date, expense.amount, expense.label)

    /**
     * Normalise les libellés bancaires : casse, espaces, références numériques longues.
     */
    fun normalizeLabel(label: String): String =
        label.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\d{6,}"), "")
            .trim()
}
