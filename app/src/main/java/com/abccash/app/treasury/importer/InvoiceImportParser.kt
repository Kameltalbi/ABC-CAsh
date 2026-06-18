package com.abccash.app.treasury.importer

import com.abccash.app.treasury.data.Invoice
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipInputStream

object InvoiceImportParser {
    fun parse(fileName: String, inputStream: InputStream): List<Invoice> {
        val lowerName = fileName.lowercase(Locale.ROOT)
        return if (lowerName.endsWith(".xlsx")) {
            parseXlsx(inputStream)
        } else {
            parseCsv(inputStream)
        }
    }

    private fun parseCsv(inputStream: InputStream): List<Invoice> {
        val lines = inputStream.bufferedReader().readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val delimiter = detectDelimiter(lines.first())
        val header = splitCsvLine(lines.first(), delimiter).map { it.normalizedHeader() }
        val hasHeader = header.any { it in knownHeaders }
        val rows = if (hasHeader) lines.drop(1) else lines
        val indexes = if (hasHeader) headerIndexes(header) else defaultIndexes()
        return rows.mapNotNull { line ->
            val cells = splitCsvLine(line, delimiter)
            invoiceFromCells(cells, indexes)
        }
    }

    private fun parseXlsx(inputStream: InputStream): List<Invoice> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && (entry.name == "xl/sharedStrings.xml" || entry.name == "xl/worksheets/sheet1.xml")) {
                    entries[entry.name] = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }
        val sharedStrings = entries["xl/sharedStrings.xml"]?.inputStream()?.let(::readSharedStrings).orEmpty()
        val rows = entries["xl/worksheets/sheet1.xml"]?.inputStream()?.let { readSheetRows(it, sharedStrings) }.orEmpty()
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.normalizedHeader() }
        val hasHeader = header.any { it in knownHeaders }
        val dataRows = if (hasHeader) rows.drop(1) else rows
        val indexes = if (hasHeader) headerIndexes(header) else defaultIndexes()
        return dataRows.mapNotNull { invoiceFromCells(it, indexes) }
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
        var text = ""
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.TEXT) text += parser.text.orEmpty()
            if (event == XmlPullParser.END_TAG && parser.name == "si") {
                values += text
                text = ""
            }
            event = parser.next()
        }
        return values
    }

    private fun readSheetRows(inputStream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(inputStream, null)
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        var cellType: String? = null
        var currentValue = ""
        var inValue = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> row = mutableListOf()
                    "c" -> cellType = parser.getAttributeValue(null, "t")
                    "v" -> {
                        currentValue = ""
                        inValue = true
                    }
                }
                XmlPullParser.TEXT -> if (inValue) currentValue += parser.text.orEmpty()
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> inValue = false
                    "c" -> {
                        row += if (cellType == "s") sharedStrings.getOrNull(currentValue.toIntOrNull() ?: -1).orEmpty() else currentValue
                        cellType = null
                        currentValue = ""
                    }
                    "row" -> if (row.any { it.isNotBlank() }) rows += row
                }
            }
            event = parser.next()
        }
        return rows
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

    private fun headerIndexes(header: List<String>): Map<String, Int> {
        return mapOf(
            "invoiceNumber" to header.indexOfFirst { it in setOf("invoicenumber", "numero", "numfacture", "facture", "n facture") }.coerceAtLeast(0),
            "clientName" to header.indexOfFirst { it in setOf("clientname", "client", "nomclient") }.coerceAtLeast(1),
            "totalAmount" to header.indexOfFirst { it in setOf("totalamount", "montant", "total", "amount") }.coerceAtLeast(2),
            "dueDate" to header.indexOfFirst { it in setOf("duedate", "echeance", "dateecheance", "date") }.coerceAtLeast(3)
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
            .replace("à", "a")
            .replace("°", "")
            .replace("_", "")
            .replace("-", "")
            .trim()
    }

    private fun String.toAmount(): Double? {
        return trim()
            .replace(" ", "")
            .replace("DT", "", ignoreCase = true)
            .replace(",", ".")
            .toDoubleOrNull()
    }

    private fun String.toLocalDate(): LocalDate? {
        val value = trim()
        value.toDoubleOrNull()?.let { serial ->
            return LocalDate.of(1899, 12, 30).plusDays(serial.toLong())
        }
        return listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy")
        ).firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(value, formatter) }.getOrNull()
        }
    }

    private val knownHeaders = setOf(
        "invoicenumber",
        "numero",
        "numfacture",
        "facture",
        "clientname",
        "client",
        "nomclient",
        "totalamount",
        "montant",
        "total",
        "amount",
        "duedate",
        "echeance",
        "dateecheance",
        "date"
    )
}
