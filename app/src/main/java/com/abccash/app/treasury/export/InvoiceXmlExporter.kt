package com.abccash.app.treasury.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.abccash.app.treasury.data.AppCurrency
import com.abccash.app.treasury.data.Entreprise
import com.abccash.app.treasury.data.Invoice
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter

object InvoiceXmlExporter {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun generateXml(context: Context, invoice: Invoice, entreprise: Entreprise?, currency: AppCurrency): File {
        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val safeName = invoice.invoiceNumber.ifBlank { "brouillon" }.replace("/", "-")
        val file = File(dir, "facture_$safeName.xml")
        file.writeText(buildXml(invoice, entreprise, currency), Charsets.UTF_8)
        return file
    }

    fun saveToDownloads(context: Context, file: File, invoice: Invoice): Uri? {
        val displayName = if (invoice.isDraft) {
            "facture-brouillon-${invoice.id.take(8)}.xml"
        } else {
            "facture-${invoice.invoiceNumber.replace("/", "-")}.xml"
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, "application/xml")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val target = File(downloads, displayName)
            file.inputStream().use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            Uri.fromFile(target)
        }
    }

    private fun buildXml(invoice: Invoice, entreprise: Entreprise?, currency: AppCurrency): String {
        val tax = invoice.taxBreakdown
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("<invoice>")
            appendLine("  <id>${esc(invoice.id)}</id>")
            appendLine("  <number>${esc(invoice.displayNumber)}</number>")
            appendLine("  <status>${esc(invoice.documentStatus.name)}</status>")
            appendLine("  <paymentStatus>${esc(invoice.status.name)}</paymentStatus>")
            appendLine("  <dueDate>${invoice.dueDate.format(dateFormatter)}</dueDate>")
            appendLine("  <createdDate>${invoice.createdDate.format(dateFormatter)}</createdDate>")
            appendLine("  <currency>${esc(currency.id)}</currency>")
            appendLine("  <client>")
            appendLine("    <name>${esc(invoice.clientName)}</name>")
            invoice.clientContactId?.let { appendLine("    <contactId>${esc(it)}</contactId>") }
            appendLine("  </client>")
            entreprise?.let { e ->
                appendLine("  <seller>")
                appendLine("    <name>${esc(e.nom)}</name>")
                e.email.takeIf { it.isNotBlank() }?.let { appendLine("    <email>${esc(it)}</email>") }
                appendLine("  </seller>")
            }
            tax?.let {
                appendLine("  <amountExclTax>${it.amountExclTax}</amountExclTax>")
                appendLine("  <tvaRate>${it.tvaRate}</tvaRate>")
                appendLine("  <tvaAmount>${it.tvaAmount}</tvaAmount>")
                if (it.hasOtherTax) {
                    appendLine("  <otherTaxLabel>${esc(it.otherTaxLabel)}</otherTaxLabel>")
                    appendLine("  <otherTaxAmount>${it.otherTaxAmount}</otherTaxAmount>")
                }
            }
            appendLine("  <totalAmount>${invoice.totalAmount}</totalAmount>")
            appendLine("  <paidAmount>${invoice.paidAmount}</paidAmount>")
            appendLine("  <remainingAmount>${invoice.remainingAmount}</remainingAmount>")
            if (invoice.lineItems.isNotEmpty()) {
                appendLine("  <lines>")
                invoice.lineItems.forEach { line ->
                    appendLine("    <line>")
                    appendLine("      <description>${esc(line.description)}</description>")
                    appendLine("      <quantity>${line.quantity}</quantity>")
                    appendLine("      <unitPriceExclTax>${line.unitPriceExclTax}</unitPriceExclTax>")
                    appendLine("      <lineTotalExclTax>${line.lineTotalExclTax}</lineTotalExclTax>")
                    appendLine("    </line>")
                }
                appendLine("  </lines>")
            }
            if (invoice.payments.isNotEmpty()) {
                appendLine("  <payments>")
                invoice.payments.forEach { payment ->
                    appendLine("    <payment>")
                    appendLine("      <date>${payment.date.format(dateFormatter)}</date>")
                    appendLine("      <amount>${payment.amount}</amount>")
                    appendLine("      <method>${esc(payment.method.name)}</method>")
                    appendLine("    </payment>")
                }
                appendLine("  </payments>")
            }
            appendLine("</invoice>")
        }
    }

    private fun esc(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
