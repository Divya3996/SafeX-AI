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
import com.sentinel.ai.core.model.ExtractedLine
import com.sentinel.ai.core.model.ExtractionResult
import com.sentinel.ai.core.model.PixelRegion
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
        val bitmap = load(uri)
        try {
            val extracted = extract(bitmap, qrOnly, includeQr = qrOnly)
            require(!extracted.partial) { "Extracted content is too long. Select a smaller area or choose one QR code." }
            if (qrOnly) extracted.qrCodes.joinToString("\n") else extracted.text
        } finally { bitmap.recycle() }
    }

    suspend fun load(uri: Uri, maxDimension: Int = 2048): Bitmap = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") { "Choose an image using the system picker." }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBounded(15 * 1024 * 1024) }
            ?: error("This image could not be opened.")
        require(bytes.size <= 15 * 1024 * 1024) { "Choose an image smaller than 15 MB." }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image format." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDimension ||
            bounds.outWidth.toLong() * bounds.outHeight / sample / sample > 6_000_000) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("This image could not be decoded.")
        bitmap
    }

    /** Caller owns bitmap lifetime. Local OCR retains geometry for user-selected lines. */
    suspend fun extract(bitmap: Bitmap, qrOnly: Boolean = false, includeQr: Boolean = true): ExtractionResult = withContext(Dispatchers.IO) {
        require(!bitmap.isRecycled && bitmap.width.toLong() * bitmap.height <= 6_000_000) { "Select a smaller area of the captured image." }
            val image = InputImage.fromBitmap(bitmap, 0)
            var qr = emptyList<String>()
            if (includeQr) {
                val reader = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
                try {
                    qr = reader.process(image).await().mapNotNull { it.rawValue }.filter { it.isNotBlank() }.distinct()
                    if (qrOnly) {
                        require(qr.size <= 8) { "This image has more than eight QR codes. Use the camera scanner to choose one code." }
                        require(qr.sumOf { it.length + 1 } <= 12001) { "QR content is empty or too long to analyze." }
                    }
                }
                finally { reader.close() }
            }
            if (qrOnly) ExtractionResult.merge(emptyList(), qr) else {
                // Script coverage is independent of the selected interface language.
                val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                try {
                    fun lines(text: com.google.mlkit.vision.text.Text, script: String) = text.textBlocks.flatMap { it.lines }.map { line ->
                        val r = line.boundingBox
                        ExtractedLine(line.text, r?.let { PixelRegion(it.left, it.top, it.right, it.bottom) }, script)
                    }
                    val english = lines(latin.process(image).await(), "latin")
                    val hindi = lines(devanagari.process(image).await(), "devanagari").filter { it.text.any { c -> c in '\u0900'..'\u097F' } }
                    val gujarati = readGujarati(bitmap)
                    ExtractionResult.merge(english + hindi + gujarati, qr)
                } finally { latin.close(); devanagari.close() }
            }
    }
    private suspend fun readGujarati(bitmap: Bitmap): List<ExtractedLine> = gujaratiMutex.withLock {
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
            if (reader.meanConfidence() < 35 || text.none { it in '\u0A80'..'\u0AFF' }) emptyList() else {
                val iterator = reader.resultIterator
                if (iterator == null) listOf(ExtractedLine(text, null, "gujarati")) else try {
                    val lines = mutableListOf<ExtractedLine>()
                    iterator.begin()
                    do {
                        val line = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE).orEmpty().trim()
                        val r = iterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
                        if (line.any { it in '\u0A80'..'\u0AFF' }) lines += ExtractedLine(line, r?.let { PixelRegion(it.left, it.top, it.right, it.bottom) }, "gujarati")
                    } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE))
                    lines
                } finally { iterator.delete() }
            }
        } finally { reader.recycle() }
    }
    companion object { private val gujaratiMutex = Mutex() }
}
