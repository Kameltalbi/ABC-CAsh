package com.abccash.app.treasury.importer

import com.abccash.app.treasury.data.Invoice
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipInputStream

data class InvoiceImportResult(
    val invoices: List<Invoice>,
    val errorMessage: String? = null,
    val detectedHeaders: List<String> = emptyList()
)

object InvoiceImportParser {
    fun parse(fileName: String, inputStream: InputStream, mimeType: String? = null): InvoiceImportResult {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) {
            return InvoiceImportResult(emptyList(), "Le fichier est vide.")
        }

        return when {
            isXlsx(fileName, mimeType, bytes) -> parseXlsx(bytes)
            fileName.lowercase(Locale.ROOT).endsWith(".xls") ->
                InvoiceImportResult(emptyList(), "Le format .xls (Excel ancien) n'est pas supporté. Enregistrez en .xlsx ou CSV.")
            else -> parseCsv(bytes)
        }
    }

    fun parse(fileName: String, inputStream: InputStream): List<Invoice> {
        return parse(fileName, inputStream, mimeType = null).invoices
    }

    private fun isXlsx(fileName: String, mimeType: String?, bytes: ByteArray): Boolean {
        if (fileName.lowercase(Locale.ROOT).endsWith(".xlsx")) return true
        if (mimeType?.contains("spreadsheetml", ignoreCase = true) == true) return true
        if (mimeType?.contains("openxmlformats", ignoreCase = true) == true) return true
        return bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte() &&
            (bytes[2] == 0x03.toByte() || bytes[2] == 0x05.toByte() || bytes[2] == 0x07.toByte())
    }

    private fun parseCsv(bytes: ByteArray): InvoiceImportResult {
        val lines = bytes.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return InvoiceImportResult(emptyList(), "Le fichier ne contient aucune ligne.")
        val delimiter = detectDelimiter(lines.first())
        val rawHeaderCells = splitCsvLine(lines.first(), delimiter)
        val header = rawHeaderCells.map { it.normalizedHeader() }
        val hasHeader = header.any { it in knownHeaders }
        val dataLines = if (hasHeader) lines.drop(1) else lines
        val dataRows = dataLines.map { splitCsvLine(it, delimiter) }
        return rowsToInvoices(header, dataRows, dataRows, if (hasHeader) rawHeaderCells else emptyList())
    }

    private fun parseXlsx(bytes: ByteArray): InvoiceImportResult {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    when {
                        entry.name == "xl/sharedStrings.xml" -> entries[entry.name] = zip.readBytes()
                        entry.name.startsWith("xl/worksheets/sheet") && !entries.containsKey("xl/worksheets/sheet1.xml") -> {
                            entries["xl/worksheets/sheet1.xml"] = zip.readBytes()
                        }
                    }
                }
                entry = zip.nextEntry
            }
        }

        val sheetBytes = entries["xl/worksheets/sheet1.xml"]
            ?: return InvoiceImportResult(emptyList(), "Feuille Excel introuvable dans le fichier.")

        val sharedStrings = entries["xl/sharedStrings.xml"]?.inputStream()?.let(::readSharedStrings).orEmpty()
        val rows = readSheetRows(sheetBytes.inputStream(), sharedStrings)
        if (rows.isEmpty()) {
            return InvoiceImportResult(emptyList(), "La feuille Excel est vide.")
        }

        val header = rows.first().map { it.normalizedHeader() }
        val hasHeader = header.any { it in knownHeaders }
        val dataRows = if (hasHeader) rows.drop(1) else rows
        return rowsToInvoices(header, dataRows, dataRows, if (hasHeader) rows.first() else emptyList())
    }

    private fun rowsToInvoices(
        header: List<String>,
        dataRows: List<List<String>>,
        allRows: List<List<String>>,
        rawHeader: List<String>
    ): InvoiceImportResult {
        val hasHeader = header.any { it in knownHeaders }
        val indexes = if (hasHeader) {
            headerIndexes(header) ?: return InvoiceImportResult(
                invoices = emptyList(),
                errorMessage = buildHeaderError(rawHeader),
                detectedHeaders = rawHeader
            )
        } else {
            defaultIndexes()
        }

        val invoices = dataRows.mapNotNull { invoiceFromCells(it, indexes) }
        if (invoices.isEmpty()) {
            return InvoiceImportResult(
                invoices = emptyList(),
                errorMessage = if (hasHeader) {
                    "Aucune ligne valide. Vérifiez montants et dates (ex. 30/06/2026)."
                } else {
                    buildHeaderError(rawHeader)
                },
                detectedHeaders = rawHeader
            )
        }
        return InvoiceImportResult(invoices)
    }

    private fun buildHeaderError(rawHeader: List<String>): String {
        val found = rawHeader.filter { it.isNotBlank() }.joinToString(", ")
        return "Colonnes non reconnues : $found. " +
            "Attendu : N° facture, Client, Montant, Date échéance (ou invoiceNumber, clientName, totalAmount, dueDate)."
    }

    private fun invoiceFromCells(cells: List<String>, indexes: Map<String, Int>): Invoice? {
        val number = cells.getOrNull(indexes.getValue("invoiceNumber"))?.trim().orEmpty()
        val client = cells.getOrNull(indexes.getValue("clientName"))?.trim().orEmpty()
        val amount = cells.getOrNull(indexes.getValue("totalAmount"))?.toAmount() ?: return null
        val dueDate = cells.getOrNull(indexes.getValue("dueDate"))?.toLocalDate() ?: return null
        if (number.isBlank() || client.isBlank()) return null
        return Invoice(
            invoiceNumber = number,
            clientName = client,
            totalAmount = amount,
            dueDate = dueDate
        )
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
        var inInlineString = false

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
                        inInlineString = cellType == "inlineStr"
                    }
                    "is" -> if (cellType == "inlineStr") inInlineString = true
                    "v", "t" -> { /* value collected via TEXT */ }
                }
                XmlPullParser.TEXT -> if (inCell) cellValue.append(parser.text.orEmpty())
                XmlPullParser.END_TAG -> when (parser.name) {
                    "is" -> inInlineString = false
                    "c" -> {
                        val value = when (cellType) {
                            "s" -> sharedStrings.getOrNull(cellValue.toString().toIntOrNull() ?: -1).orEmpty()
                            "inlineStr" -> cellValue.toString()
                            "str" -> cellValue.toString()
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

    private fun headerIndexes(header: List<String>): Map<String, Int>? {
        fun findByPriority(vararg keys: String): Int? {
            for (key in keys) {
                val index = header.indexOf(key)
                if (index >= 0) return index
            }
            return null
        }

        val invoiceNumber = findByPriority(
            "invoicenumber", "numfacture", "nfacture", "numerofacture",
            "numerodefacture", "numero", "facture", "ref", "reference"
        ) ?: return null
        val clientName = findByPriority(
            "clientname", "nomclient", "nomduclient", "raisonsociale",
            "client", "societe", "intitule", "nom"
        ) ?: return null
        val totalAmount = findByPriority(
            "totalamount", "montanttotal", "montantttc", "montantht",
            "montant", "total", "amount", "prix", "valeur"
        ) ?: return null
        val dueDate = findByPriority(
            "dateecheance", "datedecheance", "echeance", "duedate",
            "datelimite", "datefacture", "date"
        ) ?: return null

        return mapOf(
            "invoiceNumber" to invoiceNumber,
            "clientName" to clientName,
            "totalAmount" to totalAmount,
            "dueDate" to dueDate
        )
    }

    private fun defaultIndexes(): Map<String, Int> {
        return mapOf("invoiceNumber" to 0, "clientName" to 1, "totalAmount" to 2, "dueDate" to 3)
    }

    private fun String.normalizedHeader(): String {
        return lowercase(Locale.ROOT)
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e")
            .replace("ë", "e")
            .replace("à", "a")
            .replace("â", "a")
            .replace("ù", "u")
            .replace("û", "u")
            .replace("ô", "o")
            .replace("î", "i")
            .replace("ï", "i")
            .replace("ç", "c")
            .replace("°", "")
            .replace("'", "")
            .replace("’", "")
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
            .trim()
    }

    private fun String.toAmount(): Double? {
        return trim()
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace("DT", "", ignoreCase = true)
            .replace(",", ".")
            .toDoubleOrNull()
    }

    private fun String.toLocalDate(): LocalDate? {
        val value = trim().substringBefore(' ').substringBefore('T')
        value.toDoubleOrNull()?.let { serial ->
            return LocalDate.of(1899, 12, 30).plusDays(kotlin.math.round(serial).toLong())
        }
        return listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("d/M/yy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy")
        ).firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(value, formatter) }.getOrNull()
        }
    }

    private val knownHeaders = setOf(
        "invoicenumber", "numero", "numfacture", "nfacture", "numerofacture",
        "numerodefacture", "facture", "ref", "reference", "nfacture",
        "clientname", "client", "nomclient", "nom", "raisonsociale",
        "societe", "nomduclient", "intitule",
        "totalamount", "montant", "total", "amount", "montanttotal",
        "montantht", "montantttc", "prix", "valeur",
        "duedate", "echeance", "dateecheance", "date", "datefacture",
        "datelimite", "datedecheance"
    )
}
