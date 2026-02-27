package com.abccash.app.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.abccash.app.data.local.entity.CurrencyCode
import com.abccash.app.data.local.entity.TransactionEntity
import com.abccash.app.data.local.entity.TransactionType
import com.abccash.app.domain.model.CumulativeForecastPoint
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import kotlin.math.absoluteValue

fun exportDashboardPdf(
    context: Context,
    openingMinor: Long,
    forecastRows: List<CumulativeForecastPoint>,
    currency: CurrencyCode
): File {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas

    val titlePaint = Paint().apply {
        textSize = 18f
        isFakeBoldText = true
        typeface = Typeface.DEFAULT_BOLD
    }
    val headerPaint = Paint().apply {
        textSize = 12f
        isFakeBoldText = true
    }
    val bodyPaint = Paint().apply { textSize = 11f }
    val redPaint = Paint().apply {
        textSize = 11f
        color = android.graphics.Color.parseColor("#B3261E")
    }
    val greenPaint = Paint().apply {
        textSize = 11f
        color = android.graphics.Color.parseColor("#1B8F3A")
    }

    var y = 40f
    canvas.drawText("ABC Cash - Rapport de tresorerie (12 mois)", 32f, y, titlePaint)
    y += 24f
    canvas.drawText("Date export: ${LocalDate.now()}", 32f, y, bodyPaint)
    y += 18f
    canvas.drawText("Solde actuel: ${formatAmountPdf(openingMinor, currency)}", 32f, y, bodyPaint)
    y += 24f

    val stressMonths = forecastRows.filter { it.cumulativeMinor < 0L }
    val worstStress = stressMonths.minByOrNull { it.cumulativeMinor }
    canvas.drawText("Mois en stress: ${stressMonths.size}", 32f, y, bodyPaint)
    y += 18f
    worstStress?.let {
        canvas.drawText(
            "Pire mois: ${it.yearMonth} - manque ${formatAmountPdf(it.cumulativeMinor.absoluteValue, currency)}",
            32f,
            y,
            redPaint
        )
        y += 18f
    }
    y += 8f

    canvas.drawText("Mois", 32f, y, headerPaint)
    canvas.drawText("Net", 170f, y, headerPaint)
    canvas.drawText("Cumul", 300f, y, headerPaint)
    canvas.drawText("Besoin / Excedent", 430f, y, headerPaint)
    y += 14f
    canvas.drawLine(32f, y, 563f, y, bodyPaint)
    y += 16f

    forecastRows.forEach { row ->
        canvas.drawText(row.yearMonth, 32f, y, bodyPaint)
        canvas.drawText(formatAmountPdf(row.netMinor, currency), 170f, y, bodyPaint)
        canvas.drawText(formatAmountPdf(row.cumulativeMinor, currency), 300f, y, bodyPaint)
        val statusText = if (row.cumulativeMinor < 0L) {
            "Manque ${formatAmountPdf(row.cumulativeMinor.absoluteValue, currency)}"
        } else {
            "Excedent ${formatAmountPdf(row.cumulativeMinor, currency)}"
        }
        val statusPaint = if (row.cumulativeMinor < 0L) redPaint else greenPaint
        canvas.drawText(statusText, 430f, y, statusPaint)
        y += 18f
    }

    document.finishPage(page)

    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "ABC_Cash_Dashboard_${LocalDate.now()}.pdf")
    FileOutputStream(file).use { out -> document.writeTo(out) }
    document.close()
    return file
}

fun exportTransactionsByMonthPdf(
    context: Context,
    transactions: List<TransactionEntity>,
    currency: CurrencyCode
): File {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas

    val titlePaint = Paint().apply {
        textSize = 17f
        isFakeBoldText = true
        typeface = Typeface.DEFAULT_BOLD
    }
    val headerPaint = Paint().apply {
        textSize = 12f
        isFakeBoldText = true
    }
    val bodyPaint = Paint().apply { textSize = 11f }
    val redPaint = Paint().apply {
        textSize = 11f
        color = android.graphics.Color.parseColor("#B3261E")
    }
    val greenPaint = Paint().apply {
        textSize = 11f
        color = android.graphics.Color.parseColor("#1B8F3A")
    }

    fun newPage() {
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
    }

    fun ensureSpace(y: Float, needed: Float): Float {
        return if (y + needed > pageHeight - 40f) {
            newPage()
            40f
        } else y
    }

    val grouped = transactions.groupBy { it.date.withDayOfMonth(1) }.toSortedMap()
    var y = 40f
    canvas.drawText("ABC Cash - Transactions par mois", 32f, y, titlePaint)
    y += 24f
    canvas.drawText("Date export: ${LocalDate.now()}", 32f, y, bodyPaint)
    y += 18f
    canvas.drawText("Nombre total de transactions: ${transactions.size}", 32f, y, bodyPaint)
    y += 20f

    grouped.forEach { (monthStart, monthTxs) ->
        y = ensureSpace(y, 80f)
        canvas.drawText("Mois: $monthStart", 32f, y, headerPaint)
        y += 16f

        val income = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
        val expense = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
        val net = income - expense
        canvas.drawText("Entrees: ${formatAmountPdf(income, currency)}", 32f, y, greenPaint)
        y += 14f
        canvas.drawText("Sorties: ${formatAmountPdf(expense, currency)}", 32f, y, redPaint)
        y += 14f
        canvas.drawText("Net: ${formatAmountPdf(net, currency)}", 32f, y, if (net >= 0) greenPaint else redPaint)
        y += 16f

        canvas.drawText("Date", 32f, y, headerPaint)
        canvas.drawText("Categorie", 120f, y, headerPaint)
        canvas.drawText("Type", 280f, y, headerPaint)
        canvas.drawText("Montant", 350f, y, headerPaint)
        canvas.drawText("Statut", 470f, y, headerPaint)
        y += 12f
        canvas.drawLine(32f, y, 563f, y, bodyPaint)
        y += 14f

        monthTxs.sortedBy { it.date }.forEach { tx ->
            y = ensureSpace(y, 20f)
            canvas.drawText(tx.date.toString(), 32f, y, bodyPaint)
            canvas.drawText(tx.category.take(20), 120f, y, bodyPaint)
            canvas.drawText(tx.type.name, 280f, y, bodyPaint)
            val amountPaint = if (tx.type == TransactionType.INCOME) greenPaint else redPaint
            canvas.drawText(formatAmountPdf(tx.amountMinor, currency), 350f, y, amountPaint)
            canvas.drawText(tx.status.name, 470f, y, bodyPaint)
            y += 16f
        }
        y += 10f
    }

    document.finishPage(page)
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "ABC_Cash_Transactions_Par_Mois_${LocalDate.now()}.pdf")
    FileOutputStream(file).use { out -> document.writeTo(out) }
    document.close()
    return file
}

private fun formatAmountPdf(amountMinor: Long, currency: CurrencyCode): String {
    val major = amountMinor / 100.0
    val symbol = when (currency) {
        CurrencyCode.DT -> "DT"
        CurrencyCode.USD -> "$"
        CurrencyCode.EUR -> "EUR"
    }
    return "%.2f %s".format(major, symbol)
}
