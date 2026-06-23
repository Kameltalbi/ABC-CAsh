package com.abccash.app.treasury.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.abccash.app.treasury.data.AppCurrency
import com.abccash.app.treasury.data.DocumentPdfTemplate
import com.abccash.app.treasury.data.Entreprise
import com.abccash.app.treasury.data.Invoice
import java.io.File
import java.io.FileOutputStream

object InvoicePdfExporter {

    fun generatePdf(
        context: Context,
        invoice: Invoice,
        entreprise: Entreprise?,
        currency: AppCurrency,
        template: DocumentPdfTemplate = DocumentPdfTemplate.CLASSIC_BLUE
    ): File {
        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val safeName = invoice.invoiceNumber.ifBlank { "brouillon" }.replace("/", "-")
        val file = File(dir, "facture_$safeName.pdf")

        val document = PdfDocument()
        DocumentPdfRenderer.renderPage(
            document = document,
            content = invoice.toPdfContent(),
            entreprise = entreprise,
            currency = currency,
            template = template
        )
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    fun saveToDownloads(context: Context, file: File, invoice: Invoice): Uri? {
        val displayName = if (invoice.isDraft) {
            "facture-brouillon-${invoice.id.take(8)}.pdf"
        } else {
            "facture-${invoice.invoiceNumber.replace("/", "-")}.pdf"
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
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

    fun sharePdf(context: Context, file: File, chooserTitle: String) {
        val uri = fileUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun emailPdf(
        context: Context,
        file: File,
        recipient: String?,
        subject: String,
        body: String
    ) {
        val uri = fileUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            recipient?.takeIf { it.isNotBlank() }?.let { putExtra(Intent.EXTRA_EMAIL, arrayOf(it)) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }

    private fun fileUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
