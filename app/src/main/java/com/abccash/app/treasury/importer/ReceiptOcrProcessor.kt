package com.abccash.app.treasury.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

object ReceiptOcrProcessor {

    suspend fun scanReceipt(context: Context, imageUri: Uri): ReceiptParseResult = withContext(Dispatchers.IO) {
        val bitmap = loadBitmap(context, imageUri)
            ?: throw IllegalArgumentException("Impossible de lire l'image")
        try {
            val text = recognizeText(bitmap)
            ReceiptOcrParser.parse(text)
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun recognizeText(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                cont.resume(result.text.orEmpty())
                recognizer.close()
            }
            .addOnFailureListener { error ->
                recognizer.close()
                cont.resumeWithException(error)
            }
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        val sampleSize = calculateSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, 1600)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        val largest = max(width, height)
        while (largest / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
