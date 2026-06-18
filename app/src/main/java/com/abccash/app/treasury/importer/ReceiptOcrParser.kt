package com.abccash.app.treasury.importer

import com.abccash.app.treasury.data.parseFlexibleLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ReceiptParseResult(
    val amount: Double?,
    val date: LocalDate?,
    val merchantHint: String?
)

object ReceiptOcrParser {

    private val amountKeywordRegex = Regex(
        """(?i)(total|montant|ttc|net|a\s*payer|à\s*payer|amount|somme)"""
    )
    private val amountValueRegex = Regex(
        """(\d{1,3}(?:[ \u00A0]\d{3})*(?:[.,]\d{1,3})?)\s*(?:DT|TND|EUR|€|dinars?)?"""
    )
    private val numericDateRegex = Regex("""(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})""")
    private val frenchDateRegex = Regex(
        """(\d{1,2})\s+([a-zéûôîàèùç]+)\s+(\d{2,4})""",
        RegexOption.IGNORE_CASE
    )

    private val frenchMonths = mapOf(
        "janvier" to 1, "janv" to 1,
        "fevrier" to 2, "février" to 2, "fev" to 2, "fév" to 2,
        "mars" to 3,
        "avril" to 4, "avr" to 4,
        "mai" to 5,
        "juin" to 6,
        "juillet" to 7, "juil" to 7,
        "aout" to 8, "août" to 8,
        "septembre" to 9, "sept" to 9,
        "octobre" to 10, "oct" to 10,
        "novembre" to 11, "nov" to 11,
        "decembre" to 12, "décembre" to 12, "dec" to 12, "déc" to 12
    )

    fun parse(rawText: String): ReceiptParseResult {
        val normalized = rawText
            .replace('\u00A0', ' ')
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val joined = normalized.joinToString("\n")
        return ReceiptParseResult(
            amount = parseAmount(joined),
            date = parseDate(joined),
            merchantHint = parseMerchant(normalized)
        )
    }

    internal fun parseAmount(text: String): Double? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val keywordAmounts = lines
            .filter { amountKeywordRegex.containsMatchIn(it) }
            .mapNotNull { extractLargestAmount(it) }
        if (keywordAmounts.isNotEmpty()) {
            return keywordAmounts.maxOrNull()
        }
        return lines
            .mapNotNull { extractLargestAmount(it) }
            .filter { it in 0.01..999_999.0 }
            .maxOrNull()
    }

    internal fun parseDate(text: String): LocalDate? {
        frenchDateRegex.findAll(text).mapNotNull { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val monthName = match.groupValues[2].lowercase(Locale.FRENCH)
            val year = normalizeYear(match.groupValues[3].toIntOrNull() ?: return@mapNotNull null)
            val month = frenchMonths[monthName] ?: return@mapNotNull null
            runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }.firstOrNull()?.let { return it }

        numericDateRegex.findAll(text).forEach { match ->
            val a = match.groupValues[1].toIntOrNull() ?: return@forEach
            val b = match.groupValues[2].toIntOrNull() ?: return@forEach
            val year = normalizeYear(match.groupValues[3].toIntOrNull() ?: return@forEach)
            val candidates = listOf(
                runCatching { LocalDate.of(year, b, a) }.getOrNull(),
                runCatching { LocalDate.of(year, a, b) }.getOrNull()
            )
            candidates.filterNotNull()
                .firstOrNull { it.year in 2000..2100 }
                ?.let { return it }
        }

        return parseFlexibleLocalDate(text.lines().firstOrNull { numericDateRegex.containsMatchIn(it) }.orEmpty())
    }

    private fun parseMerchant(lines: List<String>): String? {
        return lines.firstOrNull { line ->
            !amountKeywordRegex.containsMatchIn(line) &&
                extractLargestAmount(line) == null &&
                parseDate(line) == null &&
                line.length in 3..40 &&
                line.any { it.isLetter() }
        }
    }

    private fun extractLargestAmount(line: String): Double? {
        return amountValueRegex.findAll(line)
            .mapNotNull { match -> parseAmountToken(match.groupValues[1]) }
            .maxOrNull()
    }

    private fun parseAmountToken(raw: String): Double? {
        val cleaned = raw.replace(" ", "").replace("\u00A0", "").replace(",", ".")
        return cleaned.toDoubleOrNull()
    }

    private fun normalizeYear(year: Int): Int = when {
        year < 100 -> 2000 + year
        else -> year
    }
}
