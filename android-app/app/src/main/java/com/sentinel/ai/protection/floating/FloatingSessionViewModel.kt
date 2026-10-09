package com.sentinel.ai.protection.floating

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.sentinel.ai.core.data.ScanRepository
import com.sentinel.ai.core.event.ThreatEvent
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.feature.FloatingAssistantControl
import com.sentinel.ai.core.model.*
import com.sentinel.ai.core.validation.UrlInputValidator
import com.sentinel.ai.protection.intent.ImageContentReader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.IdentityHashMap
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class FloatingStage { SETUP, PASTE, WAITING, CROP, EXTRACTING, REVIEW, ANALYZING, RESULT }
enum class TextSelectionMode { ALL, SELECTED, EDITED }
enum class FloatingProblem { CAPTURE, IMAGE, EXTRACTION, ANALYSIS, EXPIRED, SAVE, INPUT }
data class FloatingSession(
    val id: String = UUID.randomUUID().toString(),
    val stage: FloatingStage = FloatingStage.SETUP,
    val bitmap: Bitmap? = null,
    val original: Bitmap? = null,
    val reviewBitmap: Bitmap? = null,
    val extraction: ExtractionResult? = null,
    val text: String = "",
    val editedDraft: String? = null,
    val userEdited: Boolean = false,
    val result: ScanResult? = null,
    val error: String? = null,
    val problem: FloatingProblem? = null,
    val saved: Boolean = false,
    val saving: Boolean = false,
    val fromScreen: Boolean = false,
    val origin: InputOrigin = InputOrigin.MANUAL,
    val selectedLines: Set<Int> = emptySet(),
    val textMode: TextSelectionMode = TextSelectionMode.ALL,
    val scope: ReviewScope = ReviewScope.MESSAGE,
    val selection: ScreenSelection = ScreenSelection.FULL,
    val reviewedSelection: ScreenSelection? = null,
    val extractionMs: Long? = null,
    val captureMs: Long? = null,
    val imageDownsampled: Boolean = false
) {
    val hasContent get() = original != null || extraction != null || text.isNotEmpty() || result != null
    val busy get() = stage in setOf(FloatingStage.WAITING, FloatingStage.EXTRACTING, FloatingStage.ANALYZING)
}

/** Application-owned, RAM-only session. Activity destruction never owns capture authorization.
 * A generation prevents stale workers changing new content; leases protect native bitmap consumers. */
@Singleton
class FloatingSessionController @Inject constructor(private val repository: ScanRepository,
    @ApplicationContext private val context: Context) {
    private val mutable = MutableStateFlow(FloatingSession())
    val state = mutable.asStateFlow()
    private val jobs = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val images = ImageContentReader(context)
    private var operation: Job? = null
    private var generation = 0L
    private var recovery: FloatingSession? = null
    private var pastePrevious: FloatingSession? = null
    private var expiry: Job? = null
    private var backgroundAt = 0L
    private val owned = java.util.Collections.newSetFromMap(IdentityHashMap<Bitmap, Boolean>())
    private val leases = IdentityHashMap<Bitmap, Int>()
    val hasPrivateContent get() = mutable.value.hasContent || pastePrevious?.hasContent == true || recovery?.hasContent == true
    var requestId: String? = null
        private set
    var foreground = false
        set(value) {
            field = value
            expiry?.cancel()
            if (value && backgroundAt != 0L && android.os.SystemClock.elapsedRealtime() - backgroundAt >= 120000 && hasPrivateContent) expire()
            backgroundAt = if (value) 0 else android.os.SystemClock.elapsedRealtime()
            if (!value) expiry = jobs.launch { delay(120000); if (!foreground) expire() }
        }
    init {
        jobs.launch { mutable.collect { FloatingAssistantControl.privateReviewAvailable = hasPrivateContent } }
        jobs.launch {
            CaptureSessionStore.state.collect { update -> when (update) {
                is CaptureSessionStore.State.Ready -> if (requestId == update.id) {
                    val frame = CaptureSessionStore.take(update.id)
                    if (frame != null) {
                        requestId = null; cancelOperation(); recovery = null; pastePrevious = null
                        publish(FloatingSession(id = update.id, stage = FloatingStage.CROP, bitmap = frame, original = frame,
                            fromScreen = true, origin = InputOrigin.SCREEN_CAPTURE, captureMs = update.elapsedMs, imageDownsampled = update.downsampled))
                        context.getSystemService(android.app.NotificationManager::class.java).cancel(FloatingNotifications.READY_ID)
                        if (!foreground) showBubble()
                    }
                }
                is CaptureSessionStore.State.Failed -> if (requestId == update.id) {
                    requestId = null; val prior = recovery ?: FloatingSession(); recovery = null
                    publish(prior.copy(id = update.id, error = update.message, problem = FloatingProblem.CAPTURE))
                    if (!foreground) showBubble()
                }
                else -> Unit
            } }
        }
    }
    private fun publish(next: FloatingSession) {
        val retained = listOfNotNull(next.original, next.bitmap, next.reviewBitmap, recovery?.original, recovery?.bitmap, recovery?.reviewBitmap,
            pastePrevious?.original, pastePrevious?.bitmap, pastePrevious?.reviewBitmap).toSet()
        owned.filter { it !in retained }.toList().forEach { owned.remove(it); dispose(it) }
        retained.forEach { owned.add(it) }
        mutable.value = next
        FloatingAssistantControl.privateReviewAvailable = hasPrivateContent
    }
    private fun dispose(bitmap: Bitmap) {
        if ((leases[bitmap] ?: 0) == 0 && !owned.contains(bitmap)) {
            // The current AndroidView finishes its main-thread draw before these pixels are released.
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!owned.contains(bitmap) && (leases[bitmap] ?: 0) == 0 && !bitmap.isRecycled) bitmap.recycle()
            }, 32)
        }
    }
    private fun pixelBytes(): Long = (owned.toList() + leases.keys.toList()).distinct().filterNot { it.isRecycled }.sumOf { it.allocationByteCount.toLong() }
    private val pixelBudget = 72L * 1024 * 1024
    private fun retain(bitmap: Bitmap) { leases[bitmap] = (leases[bitmap] ?: 0) + 1 }
    private fun release(bitmap: Bitmap) { val count = (leases[bitmap] ?: 1) - 1; if (count == 0) leases.remove(bitmap) else leases[bitmap] = count; dispose(bitmap) }
    private fun cancelOperation() { generation++; operation?.cancel(); operation = null }
    private fun runOperation(before: FloatingSession, stage: FloatingStage, timeout: Long, problem: FloatingProblem,
        message: String, work: suspend (Long) -> Unit) {
        cancelOperation(); val token = generation; recovery = before
        publish(before.copy(stage = stage, error = null, problem = null))
        operation = jobs.launch {
            val worker = coroutineContext.job
            val watchdog = jobs.launch {
                delay(timeout)
                if (generation == token) {
                    generation++; worker.cancel(); recovery = null
                    publish(before.copy(error = message, problem = problem))
                }
            }
            try { work(token) }
            catch (e: CancellationException) { throw e }
            catch (_: OutOfMemoryError) { if (generation == token) { recovery = null; publish(before.copy(error = "Not enough memory for this image. Choose a smaller screenshot or paste content.", problem = FloatingProblem.IMAGE)) } }
            catch (e: IllegalArgumentException) { if (generation == token) { recovery = null; publish(before.copy(error = if (problem == FloatingProblem.EXTRACTION) "No readable content found. Try a larger crop, Share, or Paste." else message, problem = problem)) } }
            catch (_: Exception) { if (generation == token) { recovery = null; publish(before.copy(error = message, problem = problem)) } }
            finally { watchdog.cancel() }
        }
    }
    private fun expire() { if (hasPrivateContent || mutable.value.busy) { reset(); error("Private review expired while SafeX AI was in the background. Start a new scan.", FloatingProblem.EXPIRED) } }
    fun open(mode: String) {
        when (mode) {
            "paste" -> {
                if (mutable.value.stage == FloatingStage.PASTE) return
                cancelOperation(); pastePrevious = mutable.value.takeIf { it.hasContent }; recovery = null
                publish(FloatingSession(stage = FloatingStage.PASTE))
            }
            "setup" -> { if (!mutable.value.busy) publish(mutable.value.copy(stage = FloatingStage.SETUP)) }
            "review" -> resume()
            "result" -> if (mutable.value.result != null) publish(mutable.value.copy(stage = FloatingStage.RESULT))
        }
    }
    fun resume() {
        val current = mutable.value
        if (current.busy) return
        if (!current.hasContent && pastePrevious != null) { val prior = pastePrevious!!; pastePrevious = null; publish(prior); return }
        publish(current.copy(stage = when { current.result != null -> FloatingStage.RESULT; current.extraction != null -> FloatingStage.REVIEW; current.original != null -> FloatingStage.CROP; current.text.isNotBlank() -> FloatingStage.PASTE; else -> FloatingStage.SETUP }))
    }
    fun matches(id: String?) = id == null || id == mutable.value.id || id == requestId || (CaptureSessionStore.state.value as? CaptureSessionStore.State.Ready)?.id == id
    fun beginCapture(): String {
        cancelOperation(); recovery = mutable.value.takeIf { it.hasContent }; pastePrevious = null
        val id = CaptureSessionStore.begin(); requestId = id
        publish((recovery ?: FloatingSession()).copy(stage = FloatingStage.WAITING, error = null)); return id
    }
    fun captureDenied() { requestId = null; CaptureSessionStore.clear(); val prior = recovery ?: mutable.value; recovery = null; publish(prior.copy(stage = if (prior.stage == FloatingStage.WAITING) FloatingStage.SETUP else prior.stage, error = "Screen capture was not allowed. You can choose a screenshot or paste content instead.", problem = FloatingProblem.CAPTURE)) }
    fun error(message: String, problem: FloatingProblem = FloatingProblem.INPUT) { publish(mutable.value.copy(error = message, problem = problem)) }
    fun dismissError() { publish(mutable.value.copy(error = null, problem = null)) }
    fun selectCrop(selection: ScreenSelection) { publish(mutable.value.copy(selection = selection)) }
    fun changeArea() { if (mutable.value.original != null) { val s = mutable.value; publish(s.copy(stage = FloatingStage.CROP, bitmap = s.original, error = null)) } else error("Private image was released to free memory. Capture or choose the image again.", FloatingProblem.IMAGE) }
    fun setScope(scope: ReviewScope) { publish(mutable.value.copy(scope = scope)) }
    fun edit(text: String, clipboard: Boolean = false) {
        val current = mutable.value
        if (current.stage == FloatingStage.PASTE && pastePrevious != null) { pastePrevious = null; recovery = null }
        publish(current.copy(text = text, editedDraft = text, textMode = TextSelectionMode.EDITED,
            scope = if (current.extraction == null) ReviewScope.MESSAGE else ReviewScope.EDITED_TEXT, userEdited = !clipboard,
            origin = if (clipboard) InputOrigin.CLIPBOARD else current.origin, result = null, saved = false, error = null))
    }
    fun selectLine(index: Int) {
        val current = mutable.value; if (current.busy) return; val lines = current.extraction?.lines ?: return
        if (index !in lines.indices) return
        val selected = if (index in current.selectedLines) current.selectedLines - index else current.selectedLines + index
        publish(current.copy(selectedLines = selected, textMode = TextSelectionMode.SELECTED, scope = ReviewScope.SELECTED_LINES,
            text = selected.sorted().joinToString("\n") { lines[it].text }, result = null, saved = false))
    }
    fun useAllText() { publish(mutable.value.copy(text = mutable.value.extraction?.text.orEmpty(), textMode = TextSelectionMode.ALL, scope = ReviewScope.MESSAGE, result = null, saved = false)) }
    fun useEditedText() { publish(mutable.value.copy(text = mutable.value.editedDraft.orEmpty(), textMode = TextSelectionMode.EDITED, scope = ReviewScope.EDITED_TEXT, result = null, saved = false)) }
    fun useSelectedText() { val s = mutable.value; publish(s.copy(text = s.selectedLines.sorted().joinToString("\n") { s.extraction?.lines?.getOrNull(it)?.text.orEmpty() }, textMode = TextSelectionMode.SELECTED, scope = ReviewScope.SELECTED_LINES, result = null, saved = false)) }
    fun importImage(uri: Uri) {
        val before = mutable.value
        runOperation(before, FloatingStage.EXTRACTING, 15000, FloatingProblem.IMAGE, "This screenshot could not be opened. Choose another image.") { token ->
            val available = ((pixelBudget - pixelBytes()) / 4).coerceAtMost(6_000_000).toInt()
            if (available < 576) { recovery = null; publish(before.copy(error = "Image processing is still finishing. Try again shortly or paste content.", problem = FloatingProblem.IMAGE)); return@runOperation }
            val loaded = withContext(Dispatchers.IO + NonCancellable) { images.loadWithInfo(uri, 4096, available) }
            val bitmap = loaded.bitmap
            if (generation != token) { dispose(bitmap); return@runOperation }
            recovery = null; pastePrevious = null
            publish(FloatingSession(stage = FloatingStage.CROP, bitmap = bitmap, original = bitmap, fromScreen = true, origin = InputOrigin.IMPORTED_IMAGE, imageDownsampled = loaded.downsampled))
        }
    }
    fun crop(selection: ScreenSelection) {
        val before = mutable.value; val original = before.original ?: before.bitmap ?: return
        val region = runCatching { selection.pixels(original.width, original.height) }.getOrElse { error("Select an area of the captured image."); return }
        val bytes = if (selection == ScreenSelection.FULL) 0 else region.width.toLong() * region.height * 4
        if (pixelBytes() + bytes > pixelBudget) { error("Image processing is still finishing. Try again shortly or paste content."); return }
        retain(original)
        // Allow cold bundled-model setup as well as the bounded engine runs.
        // Cancel remains immediate; each recognizer retains its own deadline.
        runOperation(before, FloatingStage.EXTRACTING, 40000, FloatingProblem.EXTRACTION, "Text recognition timed out. Try a smaller crop or paste the original text.") { token ->
            var cropped: Bitmap? = null
            var cropLeased = false
            try {
                cropped = withContext(Dispatchers.Default + NonCancellable) {
                    val region = selection.pixels(original.width, original.height)
                    require(region.width >= 24 && region.height >= 24)
                    Bitmap.createBitmap(original, region.left, region.top, region.width, region.height)
                }
                if (generation != token) return@runOperation
                retain(cropped); cropLeased = true
                val start = System.nanoTime()
                val extraction = images.extract(cropped)
                if (generation != token) return@runOperation
                require(extraction.text.isNotBlank() || extraction.qrCodes.isNotEmpty())
                recovery = null
                publish(before.copy(stage = FloatingStage.REVIEW, bitmap = cropped, original = original, reviewBitmap = cropped, extraction = extraction,
                    text = extraction.text, editedDraft = null, result = null, saved = false, selectedLines = emptySet(), textMode = TextSelectionMode.ALL,
                    scope = if (extraction.text.isBlank()) ReviewScope.QR else ReviewScope.MESSAGE, selection = selection, reviewedSelection = selection,
                    extractionMs = (System.nanoTime() - start) / 1_000_000, userEdited = false, error = null))
            } finally { if (cropLeased) cropped?.let(::release) else cropped?.takeIf { it !== original }?.let(::dispose); release(original) }
        }
    }
    fun analyze(input: String = mutable.value.text, kind: String = "text", withContext: Boolean = false) {
        val current = mutable.value
        if (current.busy) return
        if (input.isBlank() || input.length > (if (kind == "link") 8192 else 12000)) { error(if (kind == "link") "Link is too long (maximum 8,192 characters)." else "Choose readable content of at most 12,000 characters."); return }
        if (withContext && current.text.length + input.length + 1 > 12000) { error("Message and link together exceed 12,000 characters. Shorten the text or analyze the link alone."); return }
        context.getSystemService(android.app.NotificationManager::class.java).cancel(915013)
        val start = System.nanoTime()
        runOperation(current, FloatingStage.ANALYZING, 15000, FloatingProblem.ANALYSIS, "This content could not be analyzed. Review the text and try again.") { token ->
            var result = when {
                kind == "qr" -> repository.analyzeQrPrivately(input)
                kind == "link" && withContext -> repository.analyzeTextPrivately(input + "\n" + current.text)
                kind == "link" || UrlInputValidator.isValid(input) -> repository.analyzeLinkPrivately(input)
                else -> repository.analyzeTextPrivately(input)
            }
            if (generation != token) return@runOperation
            val scope = when (kind) { "qr" -> ReviewScope.QR; "link" -> if (withContext) ReviewScope.MESSAGE else ReviewScope.LINKS; else -> current.scope }
            val note = when {
                kind == "link" && !withContext -> "Selected link only. Surrounding message text was not analyzed."
                kind == "qr" -> "Selected QR payload only. Surrounding image text was not analyzed."
                current.fromScreen -> "Selected visible content only. OCR can change text; hidden link destinations were not captured."
                else -> "User-provided text only."
            }
            val outcomes = current.extraction?.outcomes.orEmpty()
            val relevant = if (kind == "qr") outcomes.filter { it.engine == ExtractionEngine.QR } else outcomes.filter { it.engine != ExtractionEngine.QR }
            val existing = result.coverageDetails
            val assessment = when {
                existing?.assessment == AssessmentCoverage.UNSUPPORTED -> AssessmentCoverage.UNSUPPORTED
                existing?.assessment == AssessmentCoverage.LIMITED || (if (kind == "qr") current.extraction?.qrTruncated else current.extraction?.textTruncated) == true || relevant.any { it.status != EvidenceSourceStatus.COMPLETED } -> AssessmentCoverage.LIMITED
                else -> AssessmentCoverage.COMPLETE
            }
            val links = ContentCandidateExtractor.extract(input, 9).size
            result = CoveragePolicy.apply(result, ScanCoverageDetails(assessment, relevant, existing?.visibleLinks ?: links, existing?.analyzedLinks ?: links.coerceAtMost(8), kind == "qr", current.imageDownsampled))
                .copy(source = when (current.origin) { InputOrigin.SCREEN_CAPTURE -> "Floating screen crop"; InputOrigin.IMPORTED_IMAGE -> "Floating imported image"; InputOrigin.CLIPBOARD -> "Floating pasted content"; else -> "Floating manual content" },
                    coverage = note + "\n" + result.coverage, provenance = InputProvenance(current.origin, scope, if (kind == "qr") false else if (current.extraction != null) current.textMode == TextSelectionMode.EDITED else current.userEdited,
                        current.selectedLines.size, current.reviewedSelection),
                    durationMs = (System.nanoTime() - start) / 1_000_000,
                    timings = ScanTimings(current.extractionMs, (System.nanoTime() - start) / 1_000_000, current.captureMs))
            recovery = null
            publish(current.copy(stage = FloatingStage.RESULT, result = result, saved = false, error = null))
            if (!foreground && result.decision != ProtectionDecision.ALLOW) FloatingNotifications.warning(context, result.decision == ProtectionDecision.BLOCK, current.id)
        }
    }
    fun updateResult(result: ScanResult) { if (mutable.value.result?.id == result.id) publish(mutable.value.copy(result = result, saved = false)) }
    fun save() {
        val current = mutable.value; val result = current.result ?: return
        if (current.saving || current.saved) return
        publish(current.copy(saving = true, error = null))
        jobs.launch {
            try { ThreatJournal.recordDurably(ThreatEvent.LinkThreatDetected(result)); if (recovery?.result?.id == result.id) recovery = recovery?.copy(saved = recovery?.result == result, saving = false)
                if (mutable.value.result?.id == result.id) publish(mutable.value.copy(saved = mutable.value.result == result, saving = false)) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (mutable.value.id == current.id) publish(mutable.value.copy(saving = false, error = "Could not save this review. Try again.", problem = FloatingProblem.SAVE)) }
        }
    }
    fun cancel() {
        val prior = recovery ?: pastePrevious ?: mutable.value.copy(stage = FloatingStage.SETUP)
        cancelOperation(); requestId = null; recovery = null; pastePrevious = null
        context.stopService(android.content.Intent(context, OneShotScreenCaptureService::class.java)); CaptureSessionStore.clear()
        publish(prior.copy(error = null))
    }
    /** Returns false only at the root; Android may then dismiss the activity. */
    fun back(): Boolean {
        val s = mutable.value
        when (s.stage) {
            FloatingStage.RESULT -> publish(s.copy(stage = if (s.extraction != null) FloatingStage.REVIEW else FloatingStage.PASTE))
            FloatingStage.REVIEW -> if (s.original != null) changeArea() else publish(s.copy(stage = FloatingStage.PASTE))
            FloatingStage.CROP -> if (s.extraction != null) publish(s.copy(stage = FloatingStage.REVIEW, bitmap = s.reviewBitmap ?: s.original)) else publish(s.copy(stage = FloatingStage.SETUP))
            FloatingStage.PASTE -> { val prior = pastePrevious; pastePrevious = null; if (prior != null) publish(prior) else publish(s.copy(stage = FloatingStage.SETUP)) }
            FloatingStage.WAITING, FloatingStage.EXTRACTING, FloatingStage.ANALYZING -> cancel()
            FloatingStage.SETUP -> return false
        }
        return true
    }
    fun reset(mode: String = "setup") {
        cancelOperation(); expiry?.cancel(); requestId = null; recovery = null; pastePrevious = null
        context.stopService(android.content.Intent(context, OneShotScreenCaptureService::class.java)); CaptureSessionStore.clear()
        publish(FloatingSession(stage = if (mode == "paste") FloatingStage.PASTE else FloatingStage.SETUP))
        context.getSystemService(android.app.NotificationManager::class.java).cancel(FloatingNotifications.READY_ID)
        context.getSystemService(android.app.NotificationManager::class.java).cancel(915013)
    }
    fun releaseForMemoryPressure() {
        if (foreground || !hasPrivateContent) return
        val s = if (mutable.value.busy) recovery ?: mutable.value else mutable.value
        cancelOperation(); requestId = null; recovery = null
        context.stopService(android.content.Intent(context, OneShotScreenCaptureService::class.java)); CaptureSessionStore.clear()
        fun withoutPixels(value: FloatingSession): FloatingSession = value.copy(bitmap = null, original = null, reviewBitmap = null,
            stage = if (value.stage == FloatingStage.CROP || value.busy) {
                if (value.extraction != null) FloatingStage.REVIEW else if (value.text.isNotEmpty()) FloatingStage.PASTE else FloatingStage.SETUP
            } else value.stage)
        pastePrevious = pastePrevious?.let(::withoutPixels)
        publish(withoutPixels(s).copy(error = "Private image was released to free memory. Review text remains available.", problem = FloatingProblem.IMAGE))
    }
    private fun showBubble() { if (FloatingAssistantControl.running.value) FloatingAssistantControl.command(context, "show") }
}

@HiltViewModel
class FloatingSessionViewModel @Inject constructor(val controller: FloatingSessionController) : ViewModel() {
    val state = controller.state
    val requestId get() = controller.requestId
    var foreground: Boolean get() = controller.foreground; set(value) { controller.foreground = value }
    fun open(mode: String) = controller.open(mode)
    fun beginCapture() = controller.beginCapture()
    fun captureDenied() = controller.captureDenied()
    fun error(message: String) = controller.error(message)
    fun dismissError() = controller.dismissError()
    fun selectCrop(selection: ScreenSelection) = controller.selectCrop(selection)
    fun edit(text: String) = controller.edit(text)
    fun paste(text: String) = controller.edit(text, clipboard = true)
    fun selectLine(index: Int) = controller.selectLine(index)
    fun useAllText() = controller.useAllText()
    fun useEditedText() = controller.useEditedText()
    fun useSelectedText() = controller.useSelectedText()
    fun setScope(scope: ReviewScope) = controller.setScope(scope)
    fun changeArea() = controller.changeArea()
    fun resume() = controller.resume()
    fun importImage(uri: Uri) = controller.importImage(uri)
    fun crop(selection: ScreenSelection) = controller.crop(selection)
    fun analyze(input: String = state.value.text, kind: String = "text", withContext: Boolean = false) = controller.analyze(input, kind, withContext)
    fun updateResult(result: ScanResult) = controller.updateResult(result)
    fun save() = controller.save()
    fun cancel() = controller.cancel()
    fun back() = controller.back()
    fun reset(mode: String = "setup") = controller.reset(mode)
}
