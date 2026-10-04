package com.abccash.app.treasury.importer

import com.abccash.app.treasury.data.PilotCategory
import com.abccash.app.treasury.data.PilotEntry
import com.abccash.app.treasury.data.PilotEntryType
import com.abccash.app.treasury.data.PilotMoney
import com.abccash.app.treasury.data.PilotText
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

enum class PilotColumn {
    DATE,
    CATEGORY,
    AMOUNT,
    NOTE
}

data class PilotColumnMapping(
    val date: Int? = null,
    val category: Int? = null,
    val amount: Int? = null,
    val note: Int? = null
) {
    val isComplete: Boolean get() = date != null && category != null && amount != null

    fun indexOf(column: PilotColumn): Int? = when (column) {
        PilotColumn.DATE -> date
        PilotColumn.CATEGORY -> category
        PilotColumn.AMOUNT -> amount
        PilotColumn.NOTE -> note
    }

    fun with(column: PilotColumn, index: Int?): PilotColumnMapping {
        val cleared = copy(
            date = if (date == index) null else date,
            category = if (category == index) null else category,
            amount = if (amount == index) null else amount,
            note = if (note == index) null else note
        )
        return when (column) {
            PilotColumn.DATE -> cleared.copy(date = index)
            PilotColumn.CATEGORY -> cleared.copy(category = index)
            PilotColumn.AMOUNT -> cleared.copy(amount = index)
            PilotColumn.NOTE -> cleared.copy(note = index)
        }
    }
}

data class PilotTable(
    val hasHeader: Boolean,
    val headers: List<String>,
    val dataRows: List<List<String>>,
    val errorMessage: String? = null
)

enum class PilotLineIssue {
    INVALID_DATE,
    MISSING_CATEGORY,
    INVALID_AMOUNT,
    NON_POSITIVE_AMOUNT
}

data class PilotParsedLine(
    val sourceIndex: Int,
    val date: LocalDate?,
    val categoryName: String,
    val amount: Double?,
    val note: String,
    val issues: List<PilotLineIssue>
) {
    val isValid: Boolean get() = issues.isEmpty() && date != null && amount != null
}

enum class PilotDuplicateMode {
    IGNORE,
    IMPORT,
    REVIEW
}

data class PilotCategoryAction(
    val create: Boolean = true,
    val attachToCategoryId: String? = null
)

data class PilotImportPreview(
    val validLines: List<PilotParsedLine>,
    val invalidCount: Int,
    val total: Double,
    val periodStart: YearMonth?,
    val periodEnd: YearMonth?,
    val newCategoryNames: List<String>,
    val duplicateIndexes: Set<Int>,
    val linesToImport: List<PilotParsedLine>
)

object PilotImportParser {
    fun read(fileName: String, inputStream: InputStream, mimeType: String? = null): PilotTable {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) return PilotTable(false, emptyList(), emptyList(), "Le fichier est vide.")
        return when {
            isXlsx(fileName, mimeType, bytes) -> readXlsx(bytes)
            fileName.lowercase(Locale.ROOT).endsWith(".xls") ->
                PilotTable(
                    false,
                    emptyList(),
                    emptyList(),
                    "Le format .xls (Excel ancien) n'est pas supporté. Enregistrez en .xlsx ou CSV."
                )
            else -> readCsv(bytes)
        }
    }

    fun suggestMapping(headers: List<String>): PilotColumnMapping {
        val normalized = headers.map { it.normalizedHeader() }
        return PilotColumnMapping(
            date = findColumn(normalized, dateAliases),
            category = findColumn(normalized, categoryAliases),
            amount = findColumn(normalized, amountAliases),
            note = findColumn(normalized, noteAliases)
        )
    }

    fun parseLines(table: PilotTable, mapping: PilotColumnMapping): List<PilotParsedLine> {
        if (!mapping.isComplete) return emptyList()
        return table.dataRows.mapIndexedNotNull { index, cells ->
            if (cells.all { it.isBlank() }) return@mapIndexedNotNull null
            lineFrom(index, cells, mapping)
        }
    }

    private fun lineFrom(index: Int, cells: List<String>, mapping: PilotColumnMapping): PilotParsedLine {
        val issues = mutableListOf<PilotLineIssue>()
        val date = cells.getOrNull(mapping.date ?: -1)?.let(::parseDate)
        if (date == null) issues += PilotLineIssue.INVALID_DATE
        val category = cells.getOrNull(mapping.category ?: -1)?.trim().orEmpty()
        if (category.isBlank()) issues += PilotLineIssue.MISSING_CATEGORY
        val amountRaw = cells.getOrNull(mapping.amount ?: -1).orEmpty()
        val amount = PilotMoney.parse(amountRaw)
        when {
            amount == null -> issues += PilotLineIssue.INVALID_AMOUNT
            !PilotMoney.isPositive(amount) -> issues += PilotLineIssue.NON_POSITIVE_AMOUNT
        }
        val note = mapping.note?.let { cells.getOrNull(it)?.trim().orEmpty() }.orEmpty()
        return PilotParsedLine(index, date, category, amount, note, issues)
    }

    fun parseDate(raw: String): LocalDate? {
        val value = raw.trim().substringBefore(' ').substringBefore('T')
        if (value.isBlank()) return null
        value.toDoubleOrNull()?.let { serial ->
            if (serial > 20_000) {
                return LocalDate.of(1899, 12, 30).plusDays(kotlin.math.round(serial).toLong())
            }
        }
        return listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("d/M/yy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy")
        ).firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(value, formatter) }.getOrNull()
        }
    }

    private fun findColumn(headers: List<String>, aliases: List<String>): Int? {
        for (alias in aliases) {
            val index = headers.indexOf(alias)
            if (index >= 0) return index
        }
        return null
    }

    private fun readCsv(bytes: ByteArray): PilotTable {
        val lines = decodeCsvText(bytes).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return PilotTable(false, emptyList(), emptyList(), "Le fichier ne contient aucune ligne.")
        val delimiter = detectDelimiter(lines.first())
        val rows = lines.map { splitCsvLine(it, delimiter) }
        return tableFromRows(rows)
    }

    private fun readXlsx(bytes: ByteArray): PilotTable {
        val files = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && isWorkbookPart(entry.name)) {
                    files[entry.name] = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }
        val sharedStrings = files["xl/sharedStrings.xml"]?.let(::readSharedStrings).orEmpty()
        val sheets = orderedSheetBytes(files)
        if (sheets.isEmpty()) {
            return PilotTable(false, emptyList(), emptyList(), "Feuille Excel introuvable dans le fichier.")
        }
        val blocks = sheets.map { readSheetRows(it, sharedStrings) }.filter { it.isNotEmpty() }
        if (blocks.isEmpty()) return PilotTable(false, emptyList(), emptyList(), "La feuille Excel est vide.")
        val merged = mutableListOf<List<String>>()
        blocks.forEachIndexed { index, rows ->
            if (index == 0) {
                merged += rows
            } else {
                val header = rows.first().map { it.normalizedHeader() }
                merged += if (header.any { it in knownHeaders }) rows.drop(1) else rows
            }
        }
        return tableFromRows(merged)
    }

    private fun isWorkbookPart(name: String): Boolean {
        return name == "xl/sharedStrings.xml" ||
            name == "xl/workbook.xml" ||
            name == "xl/_rels/workbook.xml.rels" ||
            (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml"))
    }

    private fun orderedSheetBytes(files: Map<String, ByteArray>): List<ByteArray> {
        val fromWorkbook = workbookSheetPaths(files)
        val paths = fromWorkbook.ifEmpty {
            files.keys
                .filter { it.substringAfterLast('/').matches(Regex("sheet\\d+\\.xml")) }
                .sortedBy { sheetNumber(it) }
        }
        return paths.mapNotNull { files[it] }
    }

    private fun workbookSheetPaths(files: Map<String, ByteArray>): List<String> {
        val workbook = files["xl/workbook.xml"] ?: return emptyList()
        val rels = files["xl/_rels/workbook.xml.rels"] ?: return emptyList()
        val targets = mutableMapOf<String, String>()
        val relationships = document(rels).getElementsByTagName("Relationship")
        for (index in 0 until relationships.length) {
            val node = relationships.item(index) as Element
            val target = node.getAttribute("Target")
            if (!target.contains("worksheets/")) continue
            val path = when {
                target.startsWith("/") -> target.removePrefix("/")
                target.startsWith("xl/") -> target
                else -> "xl/${target.removePrefix("./")}"
            }
            targets[node.getAttribute("Id")] = path
        }
        val sheets = document(workbook).getElementsByTagName("sheet")
        val paths = mutableListOf<String>()
        for (index in 0 until sheets.length) {
            val sheet = sheets.item(index) as Element
            val id = sheetAttribute(sheet, "id")
            targets[id]?.let { paths += it }
        }
        return paths
    }

    private fun sheetAttribute(sheet: Element, local: String): String {
        val direct = sheet.getAttribute(local).ifBlank { sheet.getAttribute("r:$local") }
        if (direct.isNotBlank()) return direct
        val attributes = sheet.attributes
        for (index in 0 until attributes.length) {
            val item = attributes.item(index)
            if (item.localName == local || item.nodeName.endsWith(":$local")) return item.nodeValue.orEmpty()
        }
        return ""
    }

    private fun sheetNumber(name: String): Int =
        Regex("sheet(\\d+)\\.xml").find(name)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE

    private fun tableFromRows(rows: List<List<String>>): PilotTable {
        if (rows.isEmpty()) return PilotTable(false, emptyList(), emptyList(), "Le fichier ne contient aucune ligne.")
        val width = rows.maxOf { it.size }
        val padded = rows.map { row -> row + List((width - row.size).coerceAtLeast(0)) { "" } }
        val headerCandidate = padded.first().map { it.normalizedHeader() }
        val hasHeader = headerCandidate.any { it in knownHeaders }
        val data = if (hasHeader) padded.drop(1) else padded
        val headers = if (hasHeader) {
            padded.first().map { it.trim().ifBlank { "Colonne" } }
        } else {
            List(width) { index -> "Colonne ${index + 1}" }
        }
        if (data.isEmpty() || data.all { row -> row.all { it.isBlank() } }) {
            return PilotTable(hasHeader, headers, emptyList(), "Le fichier ne contient aucune ligne à importer.")
        }
        return PilotTable(hasHeader, headers, data.filter { row -> row.any { it.isNotBlank() } })
    }

    private fun isXlsx(fileName: String, mimeType: String?, bytes: ByteArray): Boolean {
        if (fileName.lowercase(Locale.ROOT).endsWith(".xlsx")) return true
        if (mimeType?.contains("spreadsheetml", ignoreCase = true) == true) return true
        if (mimeType?.contains("openxmlformats", ignoreCase = true) == true) return true
        return bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte()
    }

    private fun readSharedStrings(bytes: ByteArray): List<String> {
        val items = document(bytes).getElementsByTagName("si")
        return (0 until items.length).map { index ->
            val texts = (items.item(index) as Element).getElementsByTagName("t")
            (0 until texts.length).joinToString("") { texts.item(it).textContent }
        }
    }

    private fun readSheetRows(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rowNodes = document(bytes).getElementsByTagName("row")
        val rows = mutableListOf<List<String>>()
        for (index in 0 until rowNodes.length) {
            val cells = (rowNodes.item(index) as Element).getElementsByTagName("c")
            val sparseRow = mutableMapOf<Int, String>()
            for (cellIndex in 0 until cells.length) {
                val cell = cells.item(cellIndex) as Element
                val ref = cell.getAttribute("r")
                val column = if (ref.isNotBlank()) columnRefToIndex(ref) else cellIndex
                val raw = when (cell.getAttribute("t")) {
                    "s" -> sharedStrings.getOrNull(directText(cell, "v").toIntOrNull() ?: -1).orEmpty()
                    "inlineStr" -> directText(cell, "t")
                    else -> directText(cell, "v")
                }
                sparseRow[column] = raw
            }
            if (sparseRow.isNotEmpty()) {
                val row = (0..sparseRow.keys.max()).map { sparseRow[it].orEmpty() }
                if (row.any { it.isNotBlank() }) rows += row
            }
        }
        return rows
    }

    private fun directText(cell: Element, tag: String): String {
        val nodes = cell.getElementsByTagName(tag)
        return (0 until nodes.length).joinToString("") { nodes.item(it).textContent }
    }

    private fun document(bytes: ByteArray): org.w3c.dom.Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
    }

    private fun columnRefToIndex(cellRef: String): Int {
        val letters = cellRef.takeWhile { it.isLetter() }.uppercase(Locale.ROOT)
        var index = 0
        for (char in letters) {
            index = index * 26 + (char.code - 'A'.code + 1)
        }
        return index - 1
    }

    private fun detectDelimiter(line: String): Char =
        listOf(';', ',', '\t').maxBy { delimiter -> line.count { it == delimiter } }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        line.forEach { char ->
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    cells += current.toString().trim().trim('"')
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        cells += current.toString().trim().trim('"')
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
        if (!utf8.contains('\uFFFD') && !utf8.contains("Ã")) return utf8
        return String(payload, Charset.forName("Windows-1252"))
    }

    private fun String.normalizedHeader(): String {
        return lowercase(Locale.ROOT)
            .replace('é', 'e').replace('è', 'e').replace('ê', 'e').replace('ë', 'e')
            .replace('à', 'a').replace('â', 'a')
            .replace('ù', 'u').replace('û', 'u')
            .replace('ô', 'o')
            .replace('î', 'i').replace('ï', 'i')
            .replace('ç', 'c')
            .replace("°", "")
            .replace("'", "")
            .replace("’", "")
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
            .trim()
    }

    private val dateAliases = listOf(
        "date", "datevente", "datedepense", "datefacture", "datecharge",
        "jour", "dateoperation"
    )
    private val categoryAliases = listOf(
        "categorie", "category", "type", "activite", "activity", "nature", "poste"
    )
    private val amountAliases = listOf(
        "montant", "total", "ca", "chiffreaffaires", "vente", "valeur",
        "amount", "montantht", "montantttc", "charge", "depense"
    )
    private val noteAliases = listOf(
        "note", "commentaire", "comment", "libelle", "description", "remarques", "remarque", "client"
    )
    private val knownHeaders = (dateAliases + categoryAliases + amountAliases + noteAliases).toSet()
}

object PilotImportAnalysis {
    fun preview(
        lines: List<PilotParsedLine>,
        entries: List<PilotEntry>,
        categories: List<PilotCategory>,
        entrepriseId: String,
        type: PilotEntryType,
        duplicateMode: PilotDuplicateMode,
        reviewedIndexes: Set<Int>
    ): PilotImportPreview {
        val valid = lines.filter { it.isValid }
        val namesById = categories
            .filter { it.entrepriseId == entrepriseId && it.type == type }
            .associate { it.id to PilotText.categoryKey(it.name) }
        val existingKeys = entries
            .filter { it.entrepriseId == entrepriseId && it.type == type }
            .mapNotNull { entry ->
                val name = namesById[entry.categoryId] ?: return@mapNotNull null
                duplicateKey(entry.date, name, entry.amount)
            }
            .toMutableSet()
        val duplicateIndexes = mutableSetOf<Int>()
        valid.forEach { line ->
            val key = duplicateKey(line.date!!, PilotText.categoryKey(line.categoryName), line.amount!!)
            if (key in existingKeys) {
                duplicateIndexes += line.sourceIndex
            } else {
                existingKeys += key
            }
        }
        val knownNames = namesById.values.toSet()
        val newNames = valid
            .map { it.categoryName.trim() }
            .distinctBy { PilotText.categoryKey(it) }
            .filter { PilotText.categoryKey(it) !in knownNames }
        val accepted = when (duplicateMode) {
            PilotDuplicateMode.IMPORT -> valid
            PilotDuplicateMode.IGNORE -> valid.filter { it.sourceIndex !in duplicateIndexes }
            PilotDuplicateMode.REVIEW -> valid.filter { line ->
                line.sourceIndex !in duplicateIndexes || line.sourceIndex in reviewedIndexes
            }
        }
        val dates = valid.mapNotNull { it.date }
        return PilotImportPreview(
            validLines = valid,
            invalidCount = lines.count { !it.isValid },
            total = PilotMoney.sum(valid.mapNotNull { it.amount }),
            periodStart = dates.minOrNull()?.let(YearMonth::from),
            periodEnd = dates.maxOrNull()?.let(YearMonth::from),
            newCategoryNames = newNames,
            duplicateIndexes = duplicateIndexes,
            linesToImport = accepted
        )
    }

    private fun duplicateKey(date: LocalDate, categoryKey: String, amount: Double): String =
        "$date|$categoryKey|${PilotMoney.decimal(amount).toPlainString()}"
}
