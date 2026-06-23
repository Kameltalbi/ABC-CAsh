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
import com.abccash.app.treasury.data.Quote
import java.io.File
import java.io.FileOutputStream

object QuotePdfExporter {

    fun generatePdf(
        context: Context,
        quote: Quote,
        entreprise: Entreprise?,
        currency: AppCurrency,
        template: DocumentPdfTemplate = DocumentPdfTemplate.CLASSIC_BLUE
    ): File {
        val dir = File(context.cacheDir, "quotes").apply { mkdirs() }
        val safeName = quote.quoteNumber.ifBlank { "brouillon" }.replace("/", "-")
        val file = File(dir, "devis_$safeName.pdf")

        val document = PdfDocument()
        DocumentPdfRenderer.renderPage(
            document = document,
            content = quote.toPdfContent(),
            entreprise = entreprise,
            currency = currency,
            template = template
        )
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    fun saveToDownloads(context: Context, file: File, quote: Quote): Uri? {
        val displayName = if (quote.isDraft) {
            "devis-brouillon-${quote.id.take(8)}.pdf"
        } else {
            "devis-${quote.quoteNumber.replace("/", "-")}.pdf"
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

    private fun fileUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
