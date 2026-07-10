package com.abccash.app.treasury.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object TreasuryAdjustmentLabels {
    const val EXPENSE = "Ajustement"
    const val INVOICE_CLIENT = "Ajustement"
    /** Ancien libellé conservé pour détecter les écritures déjà créées. */
    const val LEGACY_LABEL = "Ajustement trésorerie — écart compte bancaire"

    fun invoiceNumber(date: LocalDate = LocalDate.now()): String {
        val suffix = (System.currentTimeMillis() % 100_000).toString().padStart(5, '0')
        return "AJUST-${date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}-$suffix"
    }
}
