package com.abccash.app.treasury.export

import android.content.Context
import android.net.Uri
import java.io.File

object ReceiptImageStorage {

    fun persistReceipt(context: Context, sourceUri: Uri, expenseId: String): String? = runCatching {
        val dir = File(context.filesDir, "receipts").apply { mkdirs() }
        val dest = File(dir, "$expenseId.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        dest.absolutePath
    }.getOrNull()

    fun deleteReceipt(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
