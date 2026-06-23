package com.abccash.app.treasury.data

import java.util.Locale

object InvoiceNumberGenerator {

    fun nextNumber(
        prefix: String,
        year: Int,
        existingNumbers: List<String>
    ): String {
        val normalizedPrefix = prefix.trim().uppercase(Locale.ROOT)
        val yearToken = year.toString()
        val yearPrefix = "$normalizedPrefix$yearToken-"
        val maxSeq = existingNumbers
            .map { it.trim().uppercase(Locale.ROOT) }
            .filter { it.startsWith(yearPrefix) }
            .mapNotNull { number ->
                number.removePrefix(yearPrefix).toIntOrNull()
            }
            .maxOrNull() ?: 0
        return "$yearPrefix${(maxSeq + 1).toString().padStart(5, '0')}"
    }
}
