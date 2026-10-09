package com.sentinel.ai.protection.floating

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.ai.core.data.ScanRepository
import com.sentinel.ai.core.event.ThreatEvent
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.model.*
import com.sentinel.ai.core.validation.UrlInputValidator
import com.sentinel.ai.protection.intent.ImageContentReader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class FloatingStage { SETUP, PASTE, WAITING, CROP, EXTRACTING, REVIEW, ANALYZING, RESULT }
data class FloatingSession(
    val stage: FloatingStage = FloatingStage.SETUP,
    val bitmap: Bitmap? = null,
    val extraction: ExtractionResult? = null,
    val text: String = "",
    val result: ScanResult? = null,
    val error: String? = null,
    val saved: Boolean = false,
    val saving: Boolean = false,
    val fromScreen: Boolean = false,
    val selectedLines: Set<Int> = emptySet(),
    val selection: ScreenSelection = ScreenSelection(.05f, .1f, .95f, .85f)
)

@HiltViewModel
class FloatingSessionViewModel @Inject constructor(private val repository: ScanRepository, @ApplicationContext private val context: Context) : ViewModel() {
    private val mutable = MutableStateFlow(FloatingSession())
    val state = mutable.asStateFlow()
    private val images = ImageContentReader(context)
    private var operation: Job? = null
    var requestId: String? = null
        private set
    private var expiry: Job? = null
    private var backgroundAt = 0L
    @Volatile var foreground = false
        set(value) {
            field = value
            expiry?.cancel()
            if (value && backgroundAt != 0L && android.os.SystemClock.elapsedRealtime() - backgroundAt >= 120000 && mutable.value.stage != FloatingStage.SETUP) {
                reset()
                error("Private review expired while SafeX AI was in the background. Start a new scan.")
            }
            backgroundAt = if (value) 0L else android.os.SystemClock.elapsedRealtime()
            if (!value) expiry = viewModelScope.launch {
                delay(120000)
                if (!foreground && mutable.value.stage != FloatingStage.SETUP) {
                    reset()
                    error("Private review expired while SafeX AI was in the background. Start a new scan.")
                }
            }
        }
    init {
        viewModelScope.launch {
            mutable.collect { com.sentinel.ai.core.feature.FloatingAssistantControl.privateReviewAvailable = it.stage in setOf(FloatingStage.CROP, FloatingStage.REVIEW, FloatingStage.RESULT) }
        }
        viewModelScope.launch {
            CaptureSessionStore.state.collect { update ->
                when (update) {
                    is CaptureSessionStore.State.Ready -> {
                        val bitmap = CaptureSessionStore.take(update.id)
                        if (bitmap != null) {
                            requestId = null
                            mutable.value = FloatingSession(FloatingStage.CROP, bitmap = bitmap, fromScreen = true)
                            context.getSystemService(android.app.NotificationManager::class.java).cancel(FloatingNotifications.READY_ID)
                            if (!foreground) showBubble()
                        }
                    }
                    is CaptureSessionStore.State.Failed -> if (requestId == update.id || (requestId == null && mutable.value.stage == FloatingStage.SETUP)) {
                        requestId = null
                        mutable.value = FloatingSession(error = update.message)
                        if (!foreground) showBubble()
                    }
                    else -> Unit
                }
            }
        }
    }
    fun open(mode: String) {
        if (mode == "review" && mutable.value.error != null) return
        if (mutable.value.stage in setOf(FloatingStage.WAITING, FloatingStage.CROP, FloatingStage.EXTRACTING, FloatingStage.REVIEW, FloatingStage.ANALYZING, FloatingStage.RESULT)) return
        mutable.value = FloatingSession(stage = if (mode == "paste") FloatingStage.PASTE else FloatingStage.SETUP)
    }
    fun beginCapture(): String {
        operation?.cancel()
        val id = CaptureSessionStore.begin()
        requestId = id
        mutable.value = FloatingSession(FloatingStage.WAITING)
        return id
    }
    fun captureDenied() { requestId = null; CaptureSessionStore.clear(); mutable.value = FloatingSession(error = "Screen capture was not allowed. You can choose a screenshot or paste content instead.") }
    fun error(message: String) { mutable.value = mutable.value.copy(error = message) }
    fun selectCrop(selection: ScreenSelection) { mutable.value = mutable.value.copy(selection = selection) }
    fun edit(text: String) { mutable.value = mutable.value.copy(text = text, selectedLines = emptySet(), error = null) }
    fun selectLine(index: Int) {
        val current = mutable.value
        val lines = current.extraction?.lines ?: return
        if (index !in lines.indices) return
        val selected = if (index in current.selectedLines) current.selectedLines - index else current.selectedLines + index
        mutable.value = current.copy(selectedLines = selected, text = if (selected.isEmpty()) current.extraction.text else selected.sorted().joinToString("\n") { lines[it].text })
    }
    fun useAllText() { mutable.value = mutable.value.copy(text = mutable.value.extraction?.text.orEmpty(), selectedLines = emptySet()) }
    fun importImage(uri: Uri) {
        operation?.cancel()
        mutable.value = FloatingSession(FloatingStage.EXTRACTING, fromScreen = true)
        operation = viewModelScope.launch {
            try {
                val bitmap = withTimeout(15000) { images.load(uri, 4096) }
                mutable.value = FloatingSession(FloatingStage.CROP, bitmap = bitmap, fromScreen = true)
            } catch (e: CancellationException) { if (e is TimeoutCancellationException) mutable.value = FloatingSession(error = "Image loading timed out. Choose a smaller screenshot.") else throw e }
            catch (_: OutOfMemoryError) { mutable.value = FloatingSession(error = "Not enough memory for this image. Choose a smaller screenshot or paste content.") }
            catch (_: Exception) { mutable.value = FloatingSession(error = "This screenshot could not be opened. Choose another image.") }
        }
    }
    fun crop(selection: ScreenSelection) {
        val before = mutable.value
        val original = before.bitmap ?: return
        operation?.cancel()
        mutable.value = mutable.value.copy(stage = FloatingStage.EXTRACTING, error = null)
        operation = viewModelScope.launch {
            try {
                val cropped = withContext(Dispatchers.Default) {
                    val region = selection.pixels(original.width, original.height)
                    require(region.width >= 24 && region.height >= 24) { "Select a larger area so the text can be read." }
                    Bitmap.createBitmap(original, region.left, region.top, region.width, region.height)
                }
                mutable.value = FloatingSession(FloatingStage.EXTRACTING, bitmap = cropped, fromScreen = true)
                val extraction = withTimeout(25000) { images.extract(cropped) }
                require(extraction.text.isNotBlank() || extraction.qrCodes.isNotEmpty()) { "No readable content found. Try a larger crop, Share, or Paste." }
                mutable.value = FloatingSession(FloatingStage.REVIEW, bitmap = cropped, extraction = extraction, text = extraction.text, fromScreen = true)
            } catch (e: CancellationException) { if (e is TimeoutCancellationException) mutable.value = before.copy(error = "Text recognition timed out. Try a smaller crop or paste the original text.") else throw e }
            catch (e: IllegalArgumentException) { mutable.value = before.copy(error = e.message ?: "No readable content found. Try a larger crop, Share, or Paste.") }
            catch (_: OutOfMemoryError) { mutable.value = FloatingSession(error = "Not enough memory for this image. Choose a smaller screenshot or paste content.") }
            catch (_: Exception) { mutable.value = before.copy(error = "Text recognition could not finish. Try again or paste the original text.") }
        }
    }
    fun analyze(input: String = mutable.value.text, kind: String = "text") {
        val current = mutable.value
        if (current.stage in setOf(FloatingStage.ANALYZING, FloatingStage.EXTRACTING, FloatingStage.WAITING)) return
        if (input.isBlank() || input.length > 12000) { error("Choose readable content of at most 12,000 characters."); return }
        operation?.cancel()
        mutable.value = current.copy(stage = FloatingStage.ANALYZING, error = null)
        operation = viewModelScope.launch {
            try {
                var result = withTimeout(15000) {
                    when {
                        kind == "qr" -> repository.analyzeQrPrivately(input)
                        kind == "link" || UrlInputValidator.isValid(input) -> repository.analyzeLinkPrivately(input)
                        else -> repository.analyzeTextPrivately(input)
                    }
                }
                result = result.copy(source = if (current.fromScreen) "Floating screen crop" else "Floating pasted content",
                    coverage = if (kind == "link") "Selected link only. Surrounding message text was not analyzed." else if (kind == "qr") "Selected QR payload only. Surrounding image text was not analyzed." else if (current.fromScreen) "Selected visible content only. OCR can change text; hidden link destinations were not captured." else "User-pasted content only.")
                if (current.extraction?.partial == true) result = result.copy(
                    riskScore = maxOf(30f, result.riskScore),
                    riskLevel = if (result.riskLevel == RiskLevel.GREEN) RiskLevel.YELLOW else result.riskLevel,
                    decision = if (result.decision == ProtectionDecision.ALLOW) ProtectionDecision.WARN else result.decision,
                    reasons = result.reasons + ScanReason(ScanReasonSource.LOCAL_HEURISTIC, "Extraction coverage", "Only part of the captured content was extracted. Choose a smaller area or verify the original text."))
                mutable.value = FloatingSession(FloatingStage.RESULT, result = result, fromScreen = current.fromScreen)
                if (!foreground && result.decision != ProtectionDecision.ALLOW) FloatingNotifications.warning(context, result.decision == ProtectionDecision.BLOCK)
            } catch (e: CancellationException) { if (e is TimeoutCancellationException) mutable.value = current.copy(error = "Analysis timed out. Try a shorter selection.") else throw e }
            catch (_: Exception) { mutable.value = current.copy(error = "This content could not be analyzed. Review the text and try again.") }
        }
    }
    fun updateResult(result: ScanResult) { mutable.value = mutable.value.copy(result = result, saved = false) }
    fun save() {
        val current = mutable.value
        val result = current.result ?: return
        if (current.saving || current.saved) return
        mutable.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                ThreatJournal.recordDurably(ThreatEvent.LinkThreatDetected(result))
                mutable.value = mutable.value.copy(saved = mutable.value.result == result, saving = false)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = mutable.value.copy(saving = false, error = "Could not save this review. Try again.") }
        }
    }
    fun reset(mode: String = "setup") {
        operation?.cancel(); expiry?.cancel(); requestId = null
        com.sentinel.ai.core.feature.FloatingAssistantControl.privateReviewAvailable = false
        context.stopService(android.content.Intent(context, OneShotScreenCaptureService::class.java)); CaptureSessionStore.clear()
        mutable.value = FloatingSession(stage = if (mode == "paste") FloatingStage.PASTE else FloatingStage.SETUP)
    }
    private fun showBubble() {
        if (com.sentinel.ai.core.feature.FloatingAssistantControl.running.value) com.sentinel.ai.core.feature.FloatingAssistantControl.command(context, "show")
    }
    override fun onCleared() {
        operation?.cancel(); expiry?.cancel()
        com.sentinel.ai.core.feature.FloatingAssistantControl.privateReviewAvailable = false
        // The one-shot service owns authorization across Android gateway destruction.
        // Explicit Close/Cancel resets it; otherwise its ten-second timeout applies.
        super.onCleared()
    }
}
