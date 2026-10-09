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
import kotlinx.coroutines.*
import com.sentinel.ai.core.model.*

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

    data class LoadedImage(val bitmap: Bitmap, val downsampled: Boolean)
    suspend fun load(uri: Uri, maxDimension: Int = 2048, maxPixels: Int = 6_000_000): Bitmap = loadWithInfo(uri, maxDimension, maxPixels).bitmap
    suspend fun loadWithInfo(uri: Uri, maxDimension: Int = 2048, maxPixels: Int = 6_000_000): LoadedImage = withContext(Dispatchers.IO) {
        require(maxPixels in 576..6_000_000)
        require(uri.scheme == "content") { "Choose an image using the system picker." }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBounded(15 * 1024 * 1024) } ?: error("This image could not be opened.")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image format." }
        val orientation = runCatching { android.media.ExifInterface(java.io.ByteArrayInputStream(bytes)).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
        // A transformed import briefly needs both decoded and rotated pixels; reserve within caller's budget.
        val decodeBudget = if (orientation in 2..8) maxOf(576, maxPixels / 2) else maxPixels
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDimension || bounds.outWidth.toLong() * bounds.outHeight / sample / sample > decodeBudget) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: error("This image could not be decoded.")
        val matrix = android.graphics.Matrix().apply {
            when (orientation) {
                2 -> setScale(-1f, 1f); 3 -> setRotate(180f); 4 -> setScale(1f, -1f)
                5 -> setValues(floatArrayOf(0f,1f,0f,1f,0f,0f,0f,0f,1f))
                6 -> setRotate(90f)
                7 -> setValues(floatArrayOf(0f,-1f,0f,-1f,0f,0f,0f,0f,1f))
                8 -> setRotate(270f)
            }
        }
        val bitmap = try { if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true) }
        catch (error: Throwable) { decoded.recycle(); throw error }
        if (bitmap !== decoded) decoded.recycle()
        LoadedImage(bitmap, sample > 1)
    }

    /** Cancellation clears the UI immediately; native consumers finish before their pixel lease ends. */
    suspend fun extract(bitmap: Bitmap, qrOnly: Boolean = false, includeQr: Boolean = true): ExtractionResult {
        val caller = currentCoroutineContext().job
        return withContext(Dispatchers.IO + NonCancellable) {
            require(!bitmap.isRecycled && bitmap.width.toLong() * bitmap.height <= 6_000_000)
            extractionMutex.withLock {
                val image = InputImage.fromBitmap(bitmap, 0)
                val readers = mutableListOf<Pair<ExtractionEngine, suspend () -> EngineContent>>()
                if (includeQr) readers += ExtractionEngine.QR to {
                    val reader = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
                    try { EngineContent(qr = reader.process(image).await().mapNotNull { it.rawValue }.filter { it.isNotBlank() }.distinct()) }
                    finally { reader.close() }
                }
                fun lines(text: com.google.mlkit.vision.text.Text, script: String) = text.textBlocks.flatMap { it.lines }.map { line ->
                    ExtractedLine(line.text, line.boundingBox?.let { PixelRegion(it.left, it.top, it.right, it.bottom) }, script)
                }
                if (!qrOnly) {
                    readers += ExtractionEngine.LATIN to {
                        val reader = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        try { EngineContent(lines(reader.process(image).await(), "latin")) } finally { reader.close() }
                    }
                    readers += ExtractionEngine.DEVANAGARI to {
                        val reader = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                        try { EngineContent(lines(reader.process(image).await(), "devanagari").filter { it.text.any { c -> c in '\u0900'..'\u097F' } }) } finally { reader.close() }
                    }
                    readers += ExtractionEngine.GUJARATI to { val found = readGujarati(bitmap, caller); EngineContent(found.lines, qualityLimited = found.limited || found.lines.any { (it.confidence ?: 1f) < .6f }) }
                }
                ExtractionCoordinator.collect(readers)
            }
        }
    }
    private data class GujaratiRead(val lines: List<ExtractedLine>, val limited: Boolean = false)
    private suspend fun readGujarati(bitmap: Bitmap, caller: Job): GujaratiRead = gujaratiMutex.withLock {
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
        val guard = Any()
        var finished = false
        var interrupted = false
        var stopTask: java.util.concurrent.ScheduledFuture<*>? = null
        try {
            check(reader.init(directory.absolutePath, "guj", TessBaseAPI.OEM_LSTM_ONLY)) { "Offline language model could not be opened." }
            reader.setImage(bitmap)
            val deadline = System.nanoTime() + 20_000_000_000L
            stopTask = stopExecutor.scheduleAtFixedRate({
                synchronized(guard) {
                    if (!finished && !interrupted && (!caller.isActive || System.nanoTime() >= deadline)) { interrupted = true; reader.stop() }
                }
            }, 100, 100, java.util.concurrent.TimeUnit.MILLISECONDS)
            // Tesseract documents getHOCRText as interruptible; plain getUTF8Text is not.
            reader.getHOCRText(0)
            if (synchronized(guard) { interrupted }) throw ExtractionTimeoutException()
            val text = reader.utF8Text.orEmpty()
            if (text.none { it in '\u0A80'..'\u0AFF' }) GujaratiRead(emptyList())
            else if (reader.meanConfidence() < 35) GujaratiRead(emptyList(), limited = true) else GujaratiRead(run {
                val iterator = reader.resultIterator
                if (iterator == null) listOf(ExtractedLine(text, null, "gujarati", reader.meanConfidence() / 100f)) else try {
                    val lines = mutableListOf<ExtractedLine>()
                    iterator.begin()
                    do {
                        val line = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE).orEmpty().trim()
                        val r = iterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
                        if (line.any { it in '\u0A80'..'\u0AFF' }) lines += ExtractedLine(line, r?.let { PixelRegion(it.left, it.top, it.right, it.bottom) }, "gujarati", iterator.confidence(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE) / 100f)
                    } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE))
                    lines
                } finally { iterator.delete() }
            })
        } finally { synchronized(guard) { finished = true; stopTask?.cancel(false) }; reader.recycle() }
    }
    companion object {
        private val gujaratiMutex = Mutex(); private val extractionMutex = Mutex()
        private val stopExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor { task -> Thread(task, "SafeX-OCR-stop").apply { isDaemon = true } }
    }
}
