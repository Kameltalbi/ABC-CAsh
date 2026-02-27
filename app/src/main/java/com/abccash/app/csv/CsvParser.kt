package com.abccash.app.csv

import com.abccash.app.data.local.entity.CurrencyCode
import com.abccash.app.data.local.entity.TransactionStatus
import com.abccash.app.data.local.entity.TransactionType
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class CsvTransaction(
    val date: LocalDate,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val suggestedCategory: String,
    val currency: CurrencyCode = CurrencyCode.EUR
)

object CsvParser {
    private val dateFormatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    )

    fun parseCsv(inputStream: InputStream): Result<List<CsvTransaction>> {
        return try {
            val lines = inputStream.bufferedReader().readLines()
            if (lines.isEmpty()) {
                return Result.failure(Exception("Fichier CSV vide"))
            }

            val header = lines.first().lowercase()
            val transactions = lines.drop(1).mapNotNull { line ->
                parseLine(line, header)
            }

            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseLine(line: String, header: String): CsvTransaction? {
        if (line.isBlank()) return null

        val parts = line.split(",", ";").map { it.trim().replace("\"", "") }
        if (parts.size < 3) return null

        return try {
            val dateIndex = findColumnIndex(header, listOf("date", "datum", "fecha"))
            val descIndex = findColumnIndex(header, listOf("libelle", "libellé", "description", "label", "desc"))
            val amountIndex = findColumnIndex(header, listOf("montant", "amount", "betrag", "importe"))

            val date = parseDate(parts.getOrNull(dateIndex) ?: parts[0])
            val description = parts.getOrNull(descIndex) ?: parts.getOrNull(1) ?: "Transaction"
            val amountStr = parts.getOrNull(amountIndex) ?: parts.getOrNull(2) ?: "0"
            
            val amount = parseAmount(amountStr)
            val type = if (amount >= 0) TransactionType.INCOME else TransactionType.EXPENSE
            val category = categorizeTransaction(description, type)

            CsvTransaction(
                date = date,
                description = description,
                amount = kotlin.math.abs(amount),
                type = type,
                suggestedCategory = category
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun findColumnIndex(header: String, keywords: List<String>): Int {
        val columns = header.split(",", ";")
        keywords.forEach { keyword ->
            val index = columns.indexOfFirst { it.contains(keyword) }
            if (index != -1) return index
        }
        return -1
    }

    private fun parseDate(dateStr: String): LocalDate {
        for (formatter in dateFormatters) {
            try {
                return LocalDate.parse(dateStr, formatter)
            } catch (e: DateTimeParseException) {
                continue
            }
        }
        return LocalDate.now()
    }

    private fun parseAmount(amountStr: String): Double {
        val cleaned = amountStr
            .replace(" ", "")
            .replace("€", "")
            .replace("EUR", "")
            .replace("$", "")
            .replace("USD", "")
            .trim()

        val normalized = if (cleaned.contains(",") && cleaned.contains(".")) {
            cleaned.replace(",", "")
        } else if (cleaned.contains(",")) {
            cleaned.replace(",", ".")
        } else {
            cleaned
        }

        return normalized.toDoubleOrNull() ?: 0.0
    }

    private fun categorizeTransaction(description: String, type: TransactionType): String {
        val desc = description.lowercase()
        
        if (type == TransactionType.INCOME) {
            return when {
                desc.contains("salaire") || desc.contains("salary") -> "Salaire"
                desc.contains("freelance") || desc.contains("consultant") -> "Freelance"
                else -> "Autres revenus"
            }
        }

        return when {
            desc.contains("loyer") || desc.contains("rent") -> "Loyer"
            desc.contains("carrefour") || desc.contains("auchan") || desc.contains("leclerc") 
                || desc.contains("courses") || desc.contains("supermarche") -> "Alimentation"
            desc.contains("restaurant") || desc.contains("mcdo") || desc.contains("burger") -> "Restaurant"
            desc.contains("essence") || desc.contains("station") || desc.contains("total") 
                || desc.contains("shell") -> "Transport"
            desc.contains("sncf") || desc.contains("ratp") || desc.contains("uber") -> "Transport"
            desc.contains("edf") || desc.contains("engie") || desc.contains("electricite") -> "Factures"
            desc.contains("orange") || desc.contains("sfr") || desc.contains("free") 
                || desc.contains("bouygues") -> "Factures"
            desc.contains("netflix") || desc.contains("spotify") || desc.contains("amazon") -> "Loisirs"
            desc.contains("pharmacie") || desc.contains("medecin") || desc.contains("hopital") -> "Santé"
            else -> "Divers"
        }
    }
}
