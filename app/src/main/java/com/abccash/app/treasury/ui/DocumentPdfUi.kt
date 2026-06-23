package com.abccash.app.treasury.ui

import android.content.Context
import com.abccash.app.R
import com.abccash.app.treasury.data.AppCurrency
import com.abccash.app.treasury.data.DocumentPdfTemplate
import com.abccash.app.treasury.data.Entreprise
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.Quote
import com.abccash.app.treasury.export.InvoicePdfExporter
import com.abccash.app.treasury.export.InvoiceXmlExporter
import com.abccash.app.treasury.export.PdfShareHelper
import com.abccash.app.treasury.export.QuotePdfExporter
import java.time.format.DateTimeFormatter

object DocumentPdfUi {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun downloadInvoice(
        context: Context,
        invoice: Invoice,
        entreprise: Entreprise?,
        currency: AppCurrency,
        template: DocumentPdfTemplate
    ): Boolean {
        val file = InvoicePdfExporter.generatePdf(context, invoice, entreprise, currency, template)
        return InvoicePdfExporter.saveToDownloads(context, file, invoice) != null
    }

    fun downloadInvoiceXml(
        context: Context,
        invoice: Invoice,
        entreprise: Entreprise?,
        currency: AppCurrency
    ): Boolean {
        val file = InvoiceXmlExporter.generateXml(context, invoice, entreprise, currency)
        return InvoiceXmlExporter.saveToDownloads(context, file, invoice) != null
    }

    fun emailInvoice(
        context: Context,
        invoice: Invoice,
        entreprise: Entreprise?,
        clientEmail: String?,
        currency: AppCurrency,
        template: DocumentPdfTemplate,
        formatAmount: (Double) -> String
    ) {
        val file = InvoicePdfExporter.generatePdf(context, invoice, entreprise, currency, template)
        PdfShareHelper.shareViaEmail(
            context = context,
            file = file,
            recipient = clientEmail,
            subject = context.getString(R.string.invoice_email_subject, invoice.displayNumber),
            body = context.getString(
                R.string.invoice_email_body,
                invoice.clientName,
                formatAmount(invoice.totalAmount),
                invoice.dueDate.format(dateFormatter)
            )
        )
    }

    fun whatsAppInvoice(
        context: Context,
        invoice: Invoice,
        entreprise: Entreprise?,
        currency: AppCurrency,
        template: DocumentPdfTemplate,
        formatAmount: (Double) -> String
    ): Boolean {
        val file = InvoicePdfExporter.generatePdf(context, invoice, entreprise, currency, template)
        val message = context.getString(
            R.string.invoice_whatsapp_body,
            invoice.displayNumber.ifBlank { context.getString(R.string.invoice_status_draft) },
            invoice.clientName,
            formatAmount(invoice.totalAmount)
        )
        return PdfShareHelper.shareViaWhatsApp(context, file, message)
    }

    fun shareInvoice(
        context: Context,
        invoice: Invoice,
        entreprise: Entreprise?,
        currency: AppCurrency,
        template: DocumentPdfTemplate
    ) {
        val file = InvoicePdfExporter.generatePdf(context, invoice, entreprise, currency, template)
        PdfShareHelper.shareChooser(context, file, context.getString(R.string.invoice_share_pdf))
    }

    fun downloadQuote(
        context: Context,
        quote: Quote,
        entreprise: Entreprise?,
        currency: AppCurrency,
        template: DocumentPdfTemplate
    ): Boolean {
        val file = QuotePdfExporter.generatePdf(context, quote, entreprise, currency, template)
        return QuotePdfExporter.saveToDownloads(context, file, quote) != null
    }

    fun emailQuote(
        context: Context,
        quote: Quote,
        entreprise: Entreprise?,
        clientEmail: String?,
        currency: AppCurrency,
        template: DocumentPdfTemplate,
        formatAmount: (Double) -> String
    ) {
        val file = QuotePdfExporter.generatePdf(context, quote, entreprise, currency, template)
        PdfShareHelper.shareViaEmail(
            context = context,
            file = file,
            recipient = clientEmail,
            subject = context.getString(R.string.quote_email_subject, quote.displayNumber),
            body = context.getString(
                R.string.quote_email_body,
                quote.clientName,
                formatAmount(quote.totalAmount),
                quote.validUntil.format(dateFormatter)
            )
        )
    }

    fun whatsAppQuote(
        context: Context,
        quote: Quote,
        entreprise: Entreprise?,
        currency: AppCurrency,
        template: DocumentPdfTemplate,
        formatAmount: (Double) -> String
    ): Boolean {
        val file = QuotePdfExporter.generatePdf(context, quote, entreprise, currency, template)
        val message = context.getString(
            R.string.quote_whatsapp_body,
            quote.displayNumber.ifBlank { context.getString(R.string.quote_status_draft) },
            quote.clientName,
            formatAmount(quote.totalAmount)
        )
        return PdfShareHelper.shareViaWhatsApp(context, file, message)
    }

    fun shareQuote(
        context: Context,
        quote: Quote,
        entreprise: Entreprise?,
        currency: AppCurrency,
        template: DocumentPdfTemplate
    ) {
        val file = QuotePdfExporter.generatePdf(context, quote, entreprise, currency, template)
        PdfShareHelper.shareChooser(context, file, context.getString(R.string.quote_share_pdf))
    }
}
