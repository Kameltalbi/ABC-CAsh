package com.abccash.app.treasury.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.abccash.app.treasury.data.AppCurrency
import com.abccash.app.treasury.data.AppCurrencyFormatter
import com.abccash.app.treasury.data.DocumentPdfTemplate
import com.abccash.app.treasury.data.Entreprise
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.InvoiceLineItem
import com.abccash.app.treasury.data.InvoiceStatus
import com.abccash.app.treasury.data.InvoiceTaxBreakdown
import com.abccash.app.treasury.data.Payment
import com.abccash.app.treasury.data.Quote
import com.abccash.app.treasury.data.QuoteStatus
import com.abccash.app.treasury.ui.otherTaxPdfLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class DocumentPdfKind { INVOICE, QUOTE }

data class DocumentPdfContent(
    val kind: DocumentPdfKind,
    val number: String,
    val isDraft: Boolean,
    val clientName: String,
    val primaryDate: LocalDate,
    val secondaryDateLabel: String,
    val secondaryDate: LocalDate,
    val statusLine: String?,
    val lineItems: List<InvoiceLineItem>,
    val taxBreakdown: InvoiceTaxBreakdown?,
    val totalAmount: Double,
    val paidAmount: Double? = null,
    val remainingAmount: Double? = null,
    val payments: List<Payment> = emptyList(),
    val notes: String? = null
)

object DocumentPdfRenderer {

    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val LEFT = 48f
    private const val RIGHT = 547f
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun renderPage(
        document: PdfDocument,
        content: DocumentPdfContent,
        entreprise: Entreprise?,
        currency: AppCurrency,
        template: DocumentPdfTemplate
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
        val page = document.startPage(pageInfo)
        drawContent(page.canvas, content, entreprise, currency, template)
        document.finishPage(page)
    }

    fun drawContent(
        canvas: Canvas,
        content: DocumentPdfContent,
        entreprise: Entreprise?,
        currency: AppCurrency,
        template: DocumentPdfTemplate
    ) {
        val palette = paletteFor(template)
        val paints = createPaints(palette)
        var y = when (template) {
            DocumentPdfTemplate.CLASSIC_BLUE -> drawClassicBlueHeader(canvas, content, entreprise, palette, paints)
            DocumentPdfTemplate.MODERN_GREEN -> drawModernGreenHeader(canvas, content, entreprise, palette, paints)
            DocumentPdfTemplate.ELEGANT_SLATE -> drawElegantSlateHeader(canvas, content, entreprise, palette, paints)
        }

        y = drawMetaBlock(canvas, content, y, palette, paints, template)
        y = drawLineItemsTable(canvas, content.lineItems, currency, y, palette, paints)
        y = drawTaxAndTotals(canvas, content, currency, y, palette, paints, template)
        y = drawPayments(canvas, content, currency, y, paints)
        drawNotes(canvas, content.notes, y, paints)
        drawFooter(canvas, paints, palette, template)
    }

    private data class TemplatePalette(
        val headerBg: Int,
        val headerText: Int,
        val accent: Int,
        val title: Int,
        val body: Int,
        val muted: Int,
        val line: Int,
        val tableHeaderBg: Int,
        val tableHeaderText: Int,
        val surfaceMuted: Int
    )

    private data class PdfPaints(
        val title: Paint,
        val header: Paint,
        val body: Paint,
        val muted: Paint,
        val line: Paint,
        val fill: Paint,
        val headerTitle: Paint,
        val tableHeader: Paint
    )

    private fun paletteFor(template: DocumentPdfTemplate): TemplatePalette = when (template) {
        DocumentPdfTemplate.CLASSIC_BLUE -> TemplatePalette(
            headerBg = 0xFF2196F3.toInt(),
            headerText = 0xFFFFFFFF.toInt(),
            accent = 0xFF1976D2.toInt(),
            title = 0xFF1565C0.toInt(),
            body = 0xFF334155.toInt(),
            muted = 0xFF64748B.toInt(),
            line = 0xFFE2E8F0.toInt(),
            tableHeaderBg = 0xFFE3F2FD.toInt(),
            tableHeaderText = 0xFF1565C0.toInt(),
            surfaceMuted = 0xFFF8FAFC.toInt()
        )
        DocumentPdfTemplate.MODERN_GREEN -> TemplatePalette(
            headerBg = 0xFFFFFFFF.toInt(),
            headerText = 0xFF1B5E20.toInt(),
            accent = 0xFF4CAF50.toInt(),
            title = 0xFF2E7D32.toInt(),
            body = 0xFF374151.toInt(),
            muted = 0xFF6B7280.toInt(),
            line = 0xFFD1D5DB.toInt(),
            tableHeaderBg = 0xFFE8F5E9.toInt(),
            tableHeaderText = 0xFF2E7D32.toInt(),
            surfaceMuted = 0xFFF1F8F4.toInt()
        )
        DocumentPdfTemplate.ELEGANT_SLATE -> TemplatePalette(
            headerBg = 0xFF1E293B.toInt(),
            headerText = 0xFFFFFFFF.toInt(),
            accent = 0xFFF59E0B.toInt(),
            title = 0xFF0F172A.toInt(),
            body = 0xFF475569.toInt(),
            muted = 0xFF94A3B8.toInt(),
            line = 0xFFE2E8F0.toInt(),
            tableHeaderBg = 0xFFF1F5F9.toInt(),
            tableHeaderText = 0xFF1E293B.toInt(),
            surfaceMuted = 0xFFF8FAFC.toInt()
        )
    }

    private fun createPaints(palette: TemplatePalette): PdfPaints {
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = palette.title
        }
        val header = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = palette.title
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            color = palette.body
        }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = palette.muted
        }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1f
            color = palette.line
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val headerTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = palette.headerText
        }
        val tableHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = palette.tableHeaderText
        }
        return PdfPaints(title, header, body, muted, line, fill, headerTitle, tableHeader)
    }

    private fun drawClassicBlueHeader(
        canvas: Canvas,
        content: DocumentPdfContent,
        entreprise: Entreprise?,
        palette: TemplatePalette,
        paints: PdfPaints
    ): Float {
        paints.fill.color = palette.headerBg
        canvas.drawRect(0f, 0f, PAGE_WIDTH, 92f, paints.fill)
        val docTitle = documentTitle(content)
        canvas.drawText(docTitle, LEFT, 40f, paints.headerTitle)
        val number = if (content.isDraft) "BROUILLON" else content.number
        val numberPaint = Paint(paints.headerTitle).apply { textSize = 13f }
        canvas.drawText("N° $number", RIGHT - numberPaint.measureText("N° $number"), 40f, numberPaint)
        entreprise?.let { company ->
            var cy = 58f
            val companyPaint = Paint(paints.body).apply { color = palette.headerText; textSize = 11f }
            canvas.drawText(company.nom, LEFT, cy, companyPaint)
            cy += 14f
            listOfNotNull(
                company.adresse.takeIf { it.isNotBlank() },
                company.email.takeIf { it.isNotBlank() },
                company.telephone.takeIf { it.isNotBlank() }
            ).forEach { line ->
                canvas.drawText(line, LEFT, cy, companyPaint)
                cy += 13f
            }
        }
        return 118f
    }

    private fun drawModernGreenHeader(
        canvas: Canvas,
        content: DocumentPdfContent,
        entreprise: Entreprise?,
        palette: TemplatePalette,
        paints: PdfPaints
    ): Float {
        paints.fill.color = palette.accent
        canvas.drawRect(0f, 0f, 14f, PAGE_HEIGHT, paints.fill)
        var y = 52f
        entreprise?.let { company ->
            canvas.drawText(company.nom, LEFT + 8f, y, paints.title)
            y += 20f
            listOfNotNull(
                company.adresse.takeIf { it.isNotBlank() },
                company.email.takeIf { it.isNotBlank() },
                company.telephone.takeIf { it.isNotBlank() }
            ).forEach { line ->
                canvas.drawText(line, LEFT + 8f, y, paints.body)
                y += 15f
            }
            y += 8f
        }
        val docTitle = documentTitle(content)
        val titlePaint = Paint(paints.header).apply { color = palette.accent; textSize = 16f }
        canvas.drawText(docTitle, LEFT + 8f, y, titlePaint)
        y += 10f
        paints.fill.color = palette.accent
        canvas.drawRect(LEFT + 8f, y, LEFT + 180f, y + 3f, paints.fill)
        y += 22f
        val number = if (content.isDraft) "BROUILLON" else content.number
        canvas.drawText("N° $number", LEFT + 8f, y, paints.header)
        return y + 24f
    }

    private fun drawElegantSlateHeader(
        canvas: Canvas,
        content: DocumentPdfContent,
        entreprise: Entreprise?,
        palette: TemplatePalette,
        paints: PdfPaints
    ): Float {
        paints.fill.color = palette.headerBg
        canvas.drawRect(0f, 0f, PAGE_WIDTH, 108f, paints.fill)
        paints.fill.color = palette.accent
        canvas.drawRect(0f, 108f, PAGE_WIDTH, 112f, paints.fill)
        entreprise?.let { company ->
            canvas.drawText(company.nom, LEFT, 42f, paints.headerTitle)
            var cy = 62f
            val infoPaint = Paint(paints.body).apply { color = palette.headerText; textSize = 11f }
            listOfNotNull(
                company.adresse.takeIf { it.isNotBlank() },
                company.email.takeIf { it.isNotBlank() },
                company.telephone.takeIf { it.isNotBlank() }
            ).forEach { line ->
                canvas.drawText(line, LEFT, cy, infoPaint)
                cy += 14f
            }
        }
        val docTitle = documentTitle(content)
        val badgePaint = Paint(paints.headerTitle).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(docTitle, RIGHT, 48f, badgePaint)
        val number = if (content.isDraft) "BROUILLON" else content.number
        val numberPaint = Paint(paints.body).apply {
            color = palette.accent
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("N° $number", RIGHT, 68f, numberPaint)
        return 136f
    }

    private fun drawMetaBlock(
        canvas: Canvas,
        content: DocumentPdfContent,
        startY: Float,
        palette: TemplatePalette,
        paints: PdfPaints,
        template: DocumentPdfTemplate
    ): Float {
        var y = startY
        if (template == DocumentPdfTemplate.ELEGANT_SLATE) {
            paints.fill.color = palette.surfaceMuted
            canvas.drawRoundRect(RectF(LEFT, y, RIGHT, y + 72f), 8f, 8f, paints.fill)
        }
        val textLeft = if (template == DocumentPdfTemplate.MODERN_GREEN) LEFT + 8f else LEFT
        y += 18f
        canvas.drawText("Client : ${content.clientName}", textLeft, y, paints.body)
        y += 18f
        canvas.drawText("Date : ${content.primaryDate.format(dateFormatter)}", textLeft, y, paints.body)
        y += 18f
        canvas.drawText(
            "${content.secondaryDateLabel} : ${content.secondaryDate.format(dateFormatter)}",
            textLeft,
            y,
            paints.body
        )
        content.statusLine?.let {
            y += 18f
            canvas.drawText(it, textLeft, y, paints.body)
        }
        y += 20f
        canvas.drawLine(LEFT, y, RIGHT, y, paints.line)
        return y + 20f
    }

    private fun drawLineItemsTable(
        canvas: Canvas,
        lineItems: List<InvoiceLineItem>,
        currency: AppCurrency,
        startY: Float,
        palette: TemplatePalette,
        paints: PdfPaints
    ): Float {
        if (lineItems.isEmpty()) return startY
        var y = startY
        canvas.drawText("Détail", LEFT, y, paints.header)
        y += 16f
        paints.fill.color = palette.tableHeaderBg
        canvas.drawRect(LEFT, y - 12f, RIGHT, y + 8f, paints.fill)
        canvas.drawText("Description", LEFT + 4f, y, paints.tableHeader)
        canvas.drawText("Qté", 300f, y, paints.tableHeader)
        canvas.drawText("P.U. HT", 360f, y, paints.tableHeader)
        canvas.drawText("Total HT", RIGHT - 72f, y, paints.tableHeader)
        y += 14f
        canvas.drawLine(LEFT, y, RIGHT, y, paints.line)
        y += 16f
        lineItems.forEach { line ->
            canvas.drawText(line.description.take(42), LEFT, y, paints.body)
            canvas.drawText(formatQty(line.quantity), 300f, y, paints.body)
            canvas.drawText(AppCurrencyFormatter.format(line.unitPriceExclTax, currency), 360f, y, paints.body)
            val total = AppCurrencyFormatter.format(line.lineTotalExclTax, currency)
            canvas.drawText(total, RIGHT - paints.body.measureText(total), y, paints.body)
            y += 16f
        }
        y += 8f
        canvas.drawLine(LEFT, y, RIGHT, y, paints.line)
        return y + 18f
    }

    private fun drawTaxAndTotals(
        canvas: Canvas,
        content: DocumentPdfContent,
        currency: AppCurrency,
        startY: Float,
        palette: TemplatePalette,
        paints: PdfPaints,
        template: DocumentPdfTemplate
    ): Float {
        var y = startY
        content.taxBreakdown?.let { tax ->
            drawAmountRow(canvas, "Montant HT", AppCurrencyFormatter.format(tax.amountExclTax, currency), y, paints.body, paints.header)
            y += 20f
            if (tax.tvaRate > 0) {
                drawAmountRow(
                    canvas,
                    "TVA (${tax.tvaRate}%)",
                    AppCurrencyFormatter.format(tax.tvaAmount, currency),
                    y,
                    paints.body,
                    paints.body
                )
                y += 18f
            }
            if (tax.hasOtherTax) {
                val label = otherTaxPdfLabel(tax, "Autre taxe")
                drawAmountRow(
                    canvas,
                    label,
                    AppCurrencyFormatter.format(tax.otherTaxAmount, currency),
                    y,
                    paints.body,
                    paints.body
                )
                y += 18f
            }
            canvas.drawLine(LEFT, y, RIGHT, y, paints.line)
            y += 18f
        }
        if (template == DocumentPdfTemplate.ELEGANT_SLATE) {
            paints.fill.color = palette.headerBg
            canvas.drawRoundRect(RectF(LEFT, y - 10f, RIGHT, y + 58f), 8f, 8f, paints.fill)
            val totalPaint = Paint(paints.header).apply { color = palette.headerText; textSize = 15f }
            val valuePaint = Paint(totalPaint)
            drawAmountRow(
                canvas,
                "Total TTC",
                AppCurrencyFormatter.format(content.totalAmount, currency),
                y + 8f,
                totalPaint,
                valuePaint
            )
            y += 52f
        } else {
            val totalLabelPaint = if (template == DocumentPdfTemplate.MODERN_GREEN) {
                Paint(paints.header).apply { color = palette.accent }
            } else {
                paints.header
            }
            drawAmountRow(
                canvas,
                "Total TTC",
                AppCurrencyFormatter.format(content.totalAmount, currency),
                y,
                totalLabelPaint,
                totalLabelPaint
            )
            y += 22f
        }
        content.paidAmount?.let { paid ->
            drawAmountRow(canvas, "Encaissé", AppCurrencyFormatter.format(paid, currency), y, paints.body, paints.body)
            y += 18f
        }
        content.remainingAmount?.let { remaining ->
            drawAmountRow(canvas, "Reste à payer", AppCurrencyFormatter.format(remaining, currency), y, paints.body, paints.body)
            y += 18f
        }
        return y
    }

    private fun drawPayments(
        canvas: Canvas,
        content: DocumentPdfContent,
        currency: AppCurrency,
        startY: Float,
        paints: PdfPaints
    ): Float {
        if (content.payments.isEmpty()) return startY
        var y = startY + 12f
        canvas.drawLine(LEFT, y, RIGHT, y, paints.line)
        y += 22f
        canvas.drawText("Paiements reçus", LEFT, y, paints.header)
        y += 18f
        content.payments.sortedBy { it.date }.forEach { payment ->
            canvas.drawText(paymentLine(payment, currency), LEFT, y, paints.body)
            y += 16f
        }
        return y
    }

    private fun drawNotes(canvas: Canvas, notes: String?, startY: Float, paints: PdfPaints) {
        if (notes.isNullOrBlank()) return
        var y = startY + 20f
        canvas.drawText("Notes", LEFT, y, paints.header)
        y += 18f
        canvas.drawText(notes.take(140), LEFT, y, paints.body)
    }

    private fun drawFooter(canvas: Canvas, paints: PdfPaints, palette: TemplatePalette, template: DocumentPdfTemplate) {
        val footerPaint = Paint(paints.muted).apply {
            if (template == DocumentPdfTemplate.CLASSIC_BLUE) color = palette.accent
        }
        canvas.drawText(
            "Généré par ABC Cash — ${LocalDate.now().format(dateFormatter)}",
            LEFT,
            812f,
            footerPaint
        )
    }

    private fun drawAmountRow(
        canvas: Canvas,
        label: String,
        value: String,
        y: Float,
        labelPaint: Paint,
        valuePaint: Paint
    ) {
        canvas.drawText(label, LEFT, y, labelPaint)
        canvas.drawText(value, RIGHT - valuePaint.measureText(value), y, valuePaint)
    }

    private fun documentTitle(content: DocumentPdfContent): String = when (content.kind) {
        DocumentPdfKind.INVOICE -> "FACTURE"
        DocumentPdfKind.QUOTE -> "DEVIS"
    }

    private fun paymentLine(payment: Payment, currency: AppCurrency): String =
        "${payment.date.format(dateFormatter)} — ${AppCurrencyFormatter.format(payment.amount, currency)} (${payment.method.name})"

    private fun formatQty(qty: Double): String =
        if (qty == qty.toLong().toDouble()) qty.toLong().toString() else "%.2f".format(qty)
}

fun Invoice.toPdfContent(): DocumentPdfContent = DocumentPdfContent(
    kind = DocumentPdfKind.INVOICE,
    number = invoiceNumber,
    isDraft = isDraft,
    clientName = clientName,
    primaryDate = createdDate,
    secondaryDateLabel = "Échéance",
    secondaryDate = dueDate,
    statusLine = "Statut paiement : ${invoicePaymentStatusLabel(status)}",
    lineItems = lineItems,
    taxBreakdown = taxBreakdown,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    remainingAmount = remainingAmount,
    payments = payments
)

fun Quote.toPdfContent(): DocumentPdfContent = DocumentPdfContent(
    kind = DocumentPdfKind.QUOTE,
    number = quoteNumber,
    isDraft = isDraft,
    clientName = clientName,
    primaryDate = issueDate,
    secondaryDateLabel = "Valable jusqu'au",
    secondaryDate = validUntil,
    statusLine = "Statut : ${quoteStatusLabel(status)}",
    lineItems = lineItems,
    taxBreakdown = taxBreakdown,
    totalAmount = totalAmount,
    notes = notes.takeIf { it.isNotBlank() }
)

private fun invoicePaymentStatusLabel(status: InvoiceStatus): String = when (status) {
    InvoiceStatus.PAID -> "Payée"
    InvoiceStatus.PARTIAL -> "Partielle"
    InvoiceStatus.DUE -> "À payer"
}

private fun quoteStatusLabel(status: QuoteStatus): String = when (status) {
    QuoteStatus.DRAFT -> "Brouillon"
    QuoteStatus.SENT -> "Envoyé"
    QuoteStatus.ACCEPTED -> "Accepté"
    QuoteStatus.REFUSED -> "Refusé"
    QuoteStatus.CONVERTED -> "Converti"
}
