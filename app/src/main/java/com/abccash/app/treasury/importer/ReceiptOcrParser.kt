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
        """(?i)(total|montant|ttc|net|a\s*payer|à\s*payer|amount|somme|payer|reglement|règlement)"""
    )
    private val strongTotalRegex = Regex("""(?i)\b(total|ttc|net\s*a\s*payer)\b""")
    private val currencyRegex = Regex("""(?i)\b(DT|TND|DR|Dinars?|EUR|€)\b""")
    private val phoneLineRegex = Regex(
        """(?i)(t[ée]l\.?|phone|mobile|gsm|fax|whatsapp|contact|\+216|\b216[\s.-]?\d{2})"""
    )
    private val skipLineRegex = Regex(
        """(?i)(siret|matricule|tva\s*n[°o]?|ice\b|caisse|table\s*n|serveur|ticket\s*n|heure|merci|www\.|@|n°\s*caisse)"""
    )
    private val amountValueRegex = Regex(
        """(\d{1,3}(?:[ \u00A0]\d{3})+(?:[.,]\d{1,3})?|\d{1,7}[., ]\d{3}|\d+[.,]\d{1,2}|\d{1,7})\s*(?:DT|TND|DR|EUR|€|dinars?)?""",
        RegexOption.IGNORE_CASE
    )
    private val numericDateRegex = Regex("""(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})""")
    private val frenchDateRegex = Regex(
        """(\d{1,2})\s+([a-zéûôîàèùç]+)\s+(\d{2,4})""",
        RegexOption.IGNORE_CASE
    )
    private val tunisianMillimesRegex = Regex("""^(\d{1,7})[,. ](\d{3})$""")
    private val tunisianGroupedMillimesRegex = Regex("""^(\d{1,3}(?:[ ]\d{3})*)[,.](\d{3})$""")

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
        val candidates = mutableListOf<ScoredAmount>()

        lines.forEachIndexed { index, line ->
            if (isIgnoredLine(line)) return@forEachIndexed

            val hasKeyword = amountKeywordRegex.containsMatchIn(line)
            val hasStrongTotal = strongTotalRegex.containsMatchIn(line)
            val lineHasCurrency = currencyRegex.containsMatchIn(line)

            extractAmounts(line).forEach { candidate ->
                val score = scoreAmount(
                    amount = candidate.value,
                    hasKeyword = hasKeyword,
                    hasStrongTotal = hasStrongTotal,
                    hasCurrency = lineHasCurrency || candidate.hasCurrency,
                    lineIndex = index,
                    lineCount = lines.size
                )
                candidates += ScoredAmount(candidate.value, score)
            }
        }

        return candidates
            .filter { it.value in 0.001..999_999.0 }
            .maxByOrNull { it.score }
            ?.value
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
                !isIgnoredLine(line) &&
                extractAmounts(line).isEmpty() &&
                parseDate(line) == null &&
                line.length in 3..40 &&
                line.any { it.isLetter() }
        }
    }

    private data class ScoredAmount(val value: Double, val score: Int)
    private data class AmountCandidate(val value: Double, val hasCurrency: Boolean)

    private fun isIgnoredLine(line: String): Boolean {
        if (phoneLineRegex.containsMatchIn(line)) return true
        if (skipLineRegex.containsMatchIn(line)) return true
        val digitsOnly = line.filter { it.isDigit() }
        if (digitsOnly.length >= 8 && !currencyRegex.containsMatchIn(line) && !amountKeywordRegex.containsMatchIn(line)) {
            return true
        }
        return false
    }

    private fun scoreAmount(
        amount: Double,
        hasKeyword: Boolean,
        hasStrongTotal: Boolean,
        hasCurrency: Boolean,
        lineIndex: Int,
        lineCount: Int
    ): Int {
        var score = 0
        if (hasStrongTotal) score += 200
        if (hasKeyword) score += 120
        if (hasCurrency) score += 80
        // Totals are usually in the bottom third of the ticket.
        if (lineIndex >= lineCount * 2 / 3) score += 20
        score += (amount * 1000).toInt().coerceAtMost(50)
        return score
    }

    private fun extractAmounts(line: String): List<AmountCandidate> {
        return amountValueRegex.findAll(line)
            .mapNotNull { match ->
                val rawToken = match.groupValues[1]
                val hasCurrency = currencyRegex.containsMatchIn(match.value)
                parseAmountToken(rawToken, hasKeywordLine = amountKeywordRegex.containsMatchIn(line))
                    ?.let { AmountCandidate(it, hasCurrency) }
            }
            .toList()
    }

    internal fun parseAmountToken(raw: String, hasKeywordLine: Boolean = false): Double? {
        val token = raw.trim().replace("\u00A0", " ")

        tunisianMillimesRegex.matchEntire(token)?.let { match ->
            return "${match.groupValues[1]}.${match.groupValues[2]}".toDoubleOrNull()
        }

        tunisianGroupedMillimesRegex.matchEntire(token)?.let { match ->
            val dinars = match.groupValues[1].replace(" ", "")
            return "$dinars.${match.groupValues[2]}".toDoubleOrNull()
        }

        Regex("""^(\d+)[,.](\d{1,2})$""").matchEntire(token.replace(" ", ""))?.let { match ->
            return "${match.groupValues[1]}.${match.groupValues[2]}".toDoubleOrNull()
        }

        val digitsOnly = token.replace(" ", "").replace(",", "").replace(".", "")
        if (digitsOnly.matches(Regex("""^\d+$"""))) {
            if (digitsOnly.length >= 4) {
                val dinars = digitsOnly.dropLast(3).ifEmpty { "0" }
                val millimes = digitsOnly.takeLast(3)
                val tunisian = "$dinars.$millimes".toDoubleOrNull()
                if (tunisian != null && (hasKeywordLine || digitsOnly.length >= 5)) {
                    return tunisian
                }
            }
            val plain = digitsOnly.toDoubleOrNull() ?: return null
            if (plain in 100.0..999.0 && !hasKeywordLine) {
                return null
            }
            return plain
        }

        return null
    }

    private fun normalizeYear(year: Int): Int = when {
        year < 100 -> 2000 + year
        else -> year
    }
}
