package com.abccash.app.treasury.importer

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipInputStream

data class BankStatementEntry(
    val date: LocalDate,
    val label: String,
    val amount: Double,
    val isCredit: Boolean,
    val reference: String = ""
)

data class BankStatementImportResult(
    val entries: List<BankStatementEntry> = emptyList(),
    val errorMessage: String? = null,
    val detectedHeaders: List<String> = emptyList()
) {
    val creditCount: Int get() = entries.count { it.isCredit }
    val debitCount: Int get() = entries.count { !it.isCredit }
    val totalCredit: Double get() = entries.filter { it.isCredit }.sumOf { it.amount }
    val totalDebit: Double get() = entries.filter { !it.isCredit }.sumOf { it.amount }
}

/**
 * Parses a bank statement export (CSV or .xlsx) into a list of credit/debit entries.
 *
 * The parser is header driven and tolerant to accents/encoding so it adapts to the
 * column names used by Tunisian banks, e.g.:
 *   Date opération | Description | Débit/Crédit | Montant | Référence | Catégorie
 */
object BankStatementImportParser {

    fun parse(fileName: String, inputStream: InputStream, mimeType: String? = null): BankStatementImportResult {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) {
            return BankStatementImportResult(errorMessage = "Le fichier est vide.")
        }

        return when {
            isXlsx(fileName, mimeType, bytes) -> parseRows(readXlsxRows(bytes))
            fileName.lowercase(Locale.ROOT).endsWith(".xls") ->
                BankStatementImportResult(
                    errorMessage = "Le format .xls (Excel ancien) n'est pas supporté. Enregistrez en .xlsx ou CSV."
                )
            else -> parseRows(readCsvRows(bytes))
        }
    }

    private fun parseRows(rows: List<List<String>>): BankStatementImportResult {
        if (rows.isEmpty()) {
            return BankStatementImportResult(errorMessage = "Le fichier ne contient aucune ligne.")
        }

        val rawHeader = rows.first()
        val header = rawHeader.map { it.normalizedHeader() }
        val indexes = headerIndexes(header)
            ?: return BankStatementImportResult(
                errorMessage = buildHeaderError(rawHeader),
                detectedHeaders = rawHeader
            )

        val entries = rows.drop(1).mapNotNull { entryFromCells(it, indexes) }
        if (entries.isEmpty()) {
            return BankStatementImportResult(
                errorMessage = "Aucune opération valide. Vérifiez les colonnes montant et date.",
                detectedHeaders = rawHeader
            )
        }
        return BankStatementImportResult(entries = entries)
    }

    private data class ColumnIndexes(
        val date: Int,
        val label: Int,
        val amount: Int,
        val debit: Int,
        val credit: Int,
        val sens: Int,
        val reference: Int
    )

    private fun headerIndexes(header: List<String>): ColumnIndexes? {
        fun find(vararg keys: String): Int {
            for (key in keys) {
                val index = header.indexOf(key)
                if (index >= 0) return index
            }
            return -1
        }

        val date = find(
            "dateoperation", "datedoperation", "dateope", "datecomptable",
            "datevaleur", "datevalue", "transactiondate", "date"
        )
        val label = find(
            "description", "libelle", "libelleoperation", "libelleecriture",
            "intitule", "designation", "nature", "motif", "detail", "details"
        )
        val amount = find("montant", "montantoperation", "amount", "valeur", "value")
        val debit = find("debit", "debitamount", "montantdebit")
        val credit = find("credit", "creditamount", "montantcredit")
        val sens = find("debitcredit", "creditdebit", "sens", "sensoperation")
        val reference = find(
            "reference", "ref", "numerooperation", "numerodoperation",
            "numoperation", "numero"
        )

        val hasAmountSource = amount >= 0 || (debit >= 0 && credit >= 0)
        if (date < 0 || !hasAmountSource) return null

        return ColumnIndexes(
            date = date,
            label = label,
            amount = amount,
            debit = debit,
            credit = credit,
            sens = sens,
            reference = reference
        )
    }

    private fun entryFromCells(cells: List<String>, indexes: ColumnIndexes): BankStatementEntry? {
        val date = cells.getOrNull(indexes.date)?.toLocalDate() ?: return null

        var amount: Double
        val isCredit: Boolean

        if (indexes.debit >= 0 && indexes.credit >= 0) {
            val debitValue = cells.getOrNull(indexes.debit)?.toAmount() ?: 0.0
            val creditValue = cells.getOrNull(indexes.credit)?.toAmount() ?: 0.0
            when {
                kotlin.math.abs(creditValue) > 0.0 -> {
                    amount = kotlin.math.abs(creditValue)
                    isCredit = true
                }
                kotlin.math.abs(debitValue) > 0.0 -> {
                    amount = kotlin.math.abs(debitValue)
                    isCredit = false
                }
                else -> return null
            }
        } else {
            val raw = cells.getOrNull(indexes.amount)?.toAmount() ?: return null
            amount = kotlin.math.abs(raw)
            if (amount == 0.0) return null
            isCredit = when {
                indexes.sens >= 0 -> isCreditSens(cells.getOrNull(indexes.sens))
                else -> raw > 0.0
            }
        }

        if (amount == 0.0) return null

        val reference = cells.getOrNull(indexes.reference)?.trim().orEmpty()
        val label = listOfNotNull(
            cells.getOrNull(indexes.label)?.trim()?.takeIf { it.isNotBlank() },
            reference.takeIf { it.isNotBlank() }
        ).firstOrNull() ?: "Opération"

        return BankStatementEntry(
            date = date,
            label = label,
            amount = amount,
            isCredit = isCredit,
            reference = reference
        )
    }

    private fun isCreditSens(value: String?): Boolean {
        val normalized = value?.lowercase(Locale.ROOT)?.trim().orEmpty()
        return when {
            normalized.startsWith("c") -> true
            normalized.startsWith("d") -> false
            normalized.contains("cred") -> true
            else -> false
        }
    }

    private fun buildHeaderError(rawHeader: List<String>): String {
        val found = rawHeader.filter { it.isNotBlank() }.joinToString(", ")
        return "Colonnes non reconnues : $found. " +
            "Attendu : Date opération, Description, Montant (ou Débit/Crédit)."
    }

    // ---- CSV ----

    private fun readCsvRows(bytes: ByteArray): List<List<String>> {
        val lines = decodeCsvText(bytes).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val delimiter = detectDelimiter(lines.first())
        return lines.map { splitCsvLine(it, delimiter) }
    }

    private fun detectDelimiter(line: String): Char {
        return listOf(';', ',', '\t').maxBy { delimiter -> line.count { it == delimiter } }
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        line.forEachIndexed { index, char ->
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    cells += current.toString().trim().trim('"')
                    current.clear()
                }
                else -> current.append(char)
            }
            if (index == line.lastIndex) cells += current.toString().trim().trim('"')
        }
        return cells
    }

    private fun decodeCsvText(bytes: ByteArray): String {
        val payload = if (
            bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            bytes.copyOfRange(3, bytes.size)
        } else {
            bytes
        }

        val utf8 = payload.toString(Charsets.UTF_8)
        if (!looksLikeMojibake(utf8)) return utf8
        return String(payload, WINDOWS_1252)
    }

    private fun looksLikeMojibake(text: String): Boolean {
        return text.contains('\uFFFD') || text.contains("Ã") || text.contains("Â")
    }

    private val WINDOWS_1252: Charset = Charset.forName("Windows-1252")

    // ---- XLSX ----

    private fun isXlsx(fileName: String, mimeType: String?, bytes: ByteArray): Boolean {
        if (fileName.lowercase(Locale.ROOT).endsWith(".xlsx")) return true
        if (mimeType?.contains("spreadsheetml", ignoreCase = true) == true) return true
        if (mimeType?.contains("openxmlformats", ignoreCase = true) == true) return true
        return bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte() &&
            (bytes[2] == 0x03.toByte() || bytes[2] == 0x05.toByte() || bytes[2] == 0x07.toByte())
    }

    private fun readXlsxRows(bytes: ByteArray): List<List<String>> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    when {
                        entry.name == "xl/sharedStrings.xml" -> entries[entry.name] = zip.readBytes()
                        entry.name.startsWith("xl/worksheets/sheet") &&
                            !entries.containsKey("xl/worksheets/sheet1.xml") -> {
                            entries["xl/worksheets/sheet1.xml"] = zip.readBytes()
                        }
                    }
                }
                entry = zip.nextEntry
            }
        }

        val sheetBytes = entries["xl/worksheets/sheet1.xml"] ?: return emptyList()
        val sharedStrings = entries["xl/sharedStrings.xml"]?.inputStream()?.let(::readSharedStrings).orEmpty()
        return readSheetRows(sheetBytes.inputStream(), sharedStrings)
    }

    private fun readSharedStrings(inputStream: InputStream): List<String> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(inputStream, null)
        val values = mutableListOf<String>()
        var text = StringBuilder()
        var inStringItem = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> if (parser.name == "si") {
                    text = StringBuilder()
                    inStringItem = true
                }
                XmlPullParser.TEXT -> if (inStringItem) text.append(parser.text.orEmpty())
                XmlPullParser.END_TAG -> if (parser.name == "si") {
                    values += text.toString()
                    inStringItem = false
                }
            }
            event = parser.next()
        }
        return values
    }

    private fun readSheetRows(inputStream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(inputStream, null)
        val rows = mutableListOf<List<String>>()
        var sparseRow = mutableMapOf<Int, String>()
        var cellType: String? = null
        var cellCol = 0
        var cellValue = StringBuilder()
        var inCell = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> sparseRow = mutableMapOf()
                    "c" -> {
                        inCell = true
                        cellValue = StringBuilder()
                        cellType = parser.getAttributeValue(null, "t")
                        val ref = parser.getAttributeValue(null, "r")
                        cellCol = if (ref != null) columnRefToIndex(ref) else sparseRow.size
                    }
                }
                XmlPullParser.TEXT -> if (inCell) cellValue.append(parser.text.orEmpty())
                XmlPullParser.END_TAG -> when (parser.name) {
                    "c" -> {
                        val value = when (cellType) {
                            "s" -> sharedStrings.getOrNull(cellValue.toString().toIntOrNull() ?: -1).orEmpty()
                            else -> cellValue.toString()
                        }
                        sparseRow[cellCol] = value
                        inCell = false
                        cellType = null
                        cellValue = StringBuilder()
                    }
                    "row" -> if (sparseRow.isNotEmpty()) {
                        val maxCol = sparseRow.keys.max()
                        val row = (0..maxCol).map { sparseRow[it].orEmpty() }
                        if (row.any { it.isNotBlank() }) rows += row
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun columnRefToIndex(cellRef: String): Int {
        val letters = cellRef.takeWhile { it.isLetter() }.uppercase(Locale.ROOT)
        var index = 0
        for (char in letters) {
            index = index * 26 + (char.code - 'A'.code + 1)
        }
        return index - 1
    }

    // ---- Shared parsing helpers ----

    private fun String.normalizedHeader(): String {
        return lowercase(Locale.ROOT)
            .replace("é", "e").replace("è", "e").replace("ê", "e").replace("ë", "e")
            .replace("à", "a").replace("â", "a")
            .replace("ù", "u").replace("û", "u")
            .replace("ô", "o")
            .replace("î", "i").replace("ï", "i")
            .replace("ç", "c")
            .replace("°", "").replace("'", "").replace("’", "")
            .replace("/", "").replace("\\", "")
            .replace("(", "").replace(")", "")
            .replace(" ", "").replace("_", "").replace("-", "")
            .trim()
    }

    private fun String.toAmount(): Double? {
        var s = trim()
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace("DT", "", ignoreCase = true)
            .replace("TND", "", ignoreCase = true)
        if (s.isEmpty()) return null
        val hasComma = s.contains(',')
        val hasDot = s.contains('.')
        s = when {
            hasComma && hasDot ->
                if (s.lastIndexOf(',') > s.lastIndexOf('.')) {
                    s.replace(".", "").replace(",", ".")
                } else {
                    s.replace(",", "")
                }
            hasComma -> s.replace(",", ".")
            else -> s
        }
        return s.toDoubleOrNull()
    }

    private fun String.toLocalDate(): LocalDate? {
        val value = trim().substringBefore(' ').substringBefore('T')
        if (value.isBlank()) return null
        value.toDoubleOrNull()?.let { serial ->
            if (serial > 1000) {
                return LocalDate.of(1899, 12, 30).plusDays(kotlin.math.round(serial).toLong())
            }
        }
        return listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("d/M/yy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
        ).firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(value, formatter) }.getOrNull()
        }
    }
}
