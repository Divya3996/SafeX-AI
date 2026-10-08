package com.sentinel.ai.protection.intent

import com.sentinel.ai.protection.intent.file.readBounded
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import android.graphics.Bitmap
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

/** Bundled models are immediately available offline; images are never retained. */
class ImageContentReader(private val context: Context) {
    suspend fun read(uri: Uri, qrOnly: Boolean): String = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") { "Choose an image using the system picker." }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBounded(15 * 1024 * 1024) }
            ?: error("This image could not be opened.")
        require(bytes.size <= 15 * 1024 * 1024) { "Choose an image smaller than 15 MB." }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image format." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("This image could not be decoded.")
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            if (qrOnly) {
                val reader = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
                try {
                    val values = reader.process(image).await().mapNotNull { it.rawValue }.filter { it.isNotBlank() }.distinct()
                    require(values.size <= 8) { "This image has more than eight QR codes. Use the camera scanner to choose one code." }
                    val content = values.joinToString("\n")
                    require(content.length <= 12000) { "QR content is empty or too long to analyze." }
                    content
                }
                finally { reader.close() }
            } else {
                // Script coverage is independent of the selected interface language.
                val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                try {
                    val english = latin.process(image).await().text
                    val hindi = devanagari.process(image).await().text.takeIf { it.any { c -> c in '\u0900'..'\u097F' } }.orEmpty()
                    val gujarati = readGujarati(bitmap)
                    listOf(english, hindi, gujarati).filter { it.isNotBlank() }.distinct().joinToString("\n").take(12000)
                } finally { latin.close(); devanagari.close() }
            }
        } finally { bitmap.recycle() }
    }
    private suspend fun readGujarati(bitmap: Bitmap): String = gujaratiMutex.withLock {
        val directory = File(context.filesDir, "ocr").apply { mkdirs() }
        val tessdata = File(directory, "tessdata").apply { mkdirs() }
        val model = File(tessdata, "guj.traineddata")
        if (!model.exists() || model.length() == 0L) {
            val pending = File(tessdata, "guj.pending")
            try {
                context.assets.open("ocr/guj.traineddata").use { source -> pending.outputStream().use(source::copyTo) }
                check(pending.renameTo(model)) { "Offline language model could not be prepared." }
            } finally { pending.delete() }
        }
        val reader = TessBaseAPI()
        try {
            check(reader.init(directory.absolutePath, "guj", TessBaseAPI.OEM_LSTM_ONLY)) { "Offline language model could not be opened." }
            reader.setImage(bitmap)
            val text = reader.utF8Text.orEmpty()
            if (reader.meanConfidence() >= 35 && text.any { it in '\u0A80'..'\u0AFF' }) text else ""
        } finally { reader.recycle() }
    }
    companion object { private val gujaratiMutex = Mutex() }
}
