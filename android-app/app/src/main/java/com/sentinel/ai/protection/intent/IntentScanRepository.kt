package com.sentinel.ai.protection.intent

import android.content.Context
import android.net.Uri
import com.sentinel.ai.core.data.ScanRepository
import com.sentinel.ai.core.event.ThreatEvent
import com.sentinel.ai.core.event.ThreatEventBus
import com.sentinel.ai.core.model.*
import com.sentinel.ai.ml.FeatureExtractor
import com.sentinel.ai.ml.MLInferenceManager
import com.sentinel.ai.ml.TextInferenceManager
import com.sentinel.ai.protection.intent.link.UrlNormalizer
import com.sentinel.ai.protection.intent.model.FilePayload
import com.sentinel.ai.protection.intent.model.UrlPayload
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CancellationException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/** One local analysis pipeline. Only final, complete results are durably published. */
@Singleton
class IntentScanRepository internal constructor(
    private val analyzer: IntentThreatAnalyzer,
    private val threatEventBus: ThreatEventBus,
    @ApplicationContext private val context: Context?,
    internal var mlPredictor: ((FloatArray) -> Float)?
) : ScanRepository {
    @Inject constructor(analyzer: IntentThreatAnalyzer, threatEventBus: ThreatEventBus,
        @ApplicationContext context: Context) : this(analyzer, threatEventBus, context, null)
    constructor(analyzer: IntentThreatAnalyzer, context: Context) : this(analyzer, ThreatEventBus(), context, null)
    internal constructor(analyzer: IntentThreatAnalyzer, threatEventBus: ThreatEventBus = ThreatEventBus(),
        mlPredictor: (FloatArray) -> Float) : this(analyzer, threatEventBus, null, mlPredictor)

    private val urlModel by lazy { runCatching { MLInferenceManager(checkNotNull(context)) } }
    private val textModel by lazy { runCatching { TextInferenceManager(checkNotNull(context)) } }
    private val images by lazy { ImageContentReader(checkNotNull(context)) }

    override suspend fun analyzeLinkPrivately(link: String): ScanResult = withContext(Dispatchers.Default) { analyzeLink(link) }
    override suspend fun analyzeTextPrivately(text: String): ScanResult = withContext(Dispatchers.Default) { analyzeText(text) }
    override suspend fun analyzeQrPrivately(content: String): ScanResult = withContext(Dispatchers.Default) { analyzeQrContent(content) }

    override suspend fun scanLink(link: String): ScanResult = withContext(Dispatchers.Default) {
        val start = System.nanoTime()
        val result = analyzeLink(link).copy(durationMs = elapsed(start))
        threatEventBus.emit(ThreatEvent.LinkThreatDetected(result))
        result
    }

    private suspend fun analyzeLink(link: String): ScanResult {
        require(link.length <= 8192) { "Link is too long (maximum 8,192 characters)." }
        val parsed = UrlNormalizer.parse(link.trim())
        require(parsed.isValid && parsed.scheme in listOf("http", "https")) { "Enter a valid web link." }
        val base = analyzer.analyze(UrlPayload(link.trim())).copy(target = parsed.normalized)
        val result = applyMlScore(base, parsed.normalized)
        val inspection = LinkInspection(host = parsed.host!!,
            registrableDomain = com.sentinel.ai.protection.intent.link.LinkDestinations.site(parsed.host),
            unicodeHost = runCatching { java.net.IDN.toUnicode(parsed.host) }.getOrDefault(parsed.host),
            usesHttps = parsed.scheme == "https", hasUserInfo = parsed.hasUserInfo,
            isNumericAddress = parsed.isIpv4 || parsed.isIpv6,
            embeddedHosts = com.sentinel.ai.protection.intent.link.LinkDestinations.external(parsed, depth = 4, includeBase64 = true).mapNotNull { it.host },
            signals = result.reasons.filter { it.source != ScanReasonSource.PROVIDER_STATUS }.map { it.message }.distinct())
        return result.copy(linkInspections = listOf(inspection))
    }

    override suspend fun scanFile(uri: Uri): ScanResult = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        val result = analyzer.analyze(FilePayload(uri)).copy(contentType = "file", durationMs = elapsed(start),
            coverage = "File metadata, header and bounded archive checks. Not a full malware scan.")
        threatEventBus.emit(ThreatEvent.FileThreatDetected(result))
        result
    }

    override suspend fun scanText(text: String): ScanResult = withContext(Dispatchers.Default) {
        val result = analyzeText(text)
        threatEventBus.emit(ThreatEvent.LinkThreatDetected(result))
        result
    }

    override suspend fun analyzeMessage(text: String, source: String, sender: String?, identifier: String?, timestamp: Long, id: String): ScanResult = withContext(Dispatchers.Default) {
        val result = analyzeText(text).copy(id = id, source = source, senderDisplayName = sender,
            senderIdentifier = identifier, timestamp = timestamp, isDemo = source == "com.sentinel.demo")
        threatEventBus.emit(ThreatEvent.WhatsAppThreatDetected(result))
        result
    }

    override suspend fun scanQrContent(content: String): ScanResult = withContext(Dispatchers.Default) {
        val result = analyzeQrContent(content).copy(source = "Live QR scan")
        threatEventBus.emit(ThreatEvent.FileThreatDetected(result))
        result
    }

    private suspend fun analyzeQrContent(content: String): ScanResult {
        require(content.isNotBlank() && content.length <= 12000) { "QR content is empty or too long to analyze." }
        val parsed = UrlNormalizer.parse(content.trim())
        return if (!content.contains('\n') && parsed.isValid && parsed.scheme in listOf("http", "https") &&
            content.trim().startsWith("http", true)) analyzeLink(content.trim())
        else analyzeText(content).copy(contentType = "qr")
    }

    override suspend fun scanImage(uri: Uri, qrOnly: Boolean): ScanResult = withContext(Dispatchers.Default) {
        val start = System.nanoTime()
        val content = withTimeout(15000) { images.read(uri, qrOnly) }
        require(content.isNotBlank()) { if (qrOnly) "No readable QR code found. Choose a clearer image." else "No readable text found. Choose a clearer screenshot." }
        val analyzed = if (qrOnly) analyzeQrContent(content) else analyzeText(content)
        val result = analyzed.copy(contentType = if (qrOnly && analyzed.contentType == "link") "link" else if (qrOnly) "qr" else "image",
            source = if (qrOnly) "QR image" else "Screenshot", durationMs = elapsed(start),
            coverage = "Extracted content only. Image quality can hide or change text.")
        threatEventBus.emit(ThreatEvent.FileThreatDetected(result))
        result
    }

    private suspend fun analyzeText(text: String): ScanResult {
        val start = System.nanoTime()
        require(text.isNotBlank()) { "Paste a message before scanning." }
        require(text.length <= 12000) { "Message is too long (maximum 12,000 characters)." }
        val signals = MessageSignals.analyze(text)
        var invalidLinks = 0
        val extractedLinks = MessageSignals.urls(text, limit = 9)
        val linkResults = extractedLinks.take(8).mapNotNull { link ->
            try { analyzeLink(link) } catch (e: CancellationException) { throw e } catch (_: IllegalArgumentException) { invalidLinks++; null }
        }
        val reasons = signals.reasons.toMutableList()
        linkResults.filter { it.decision != ProtectionDecision.ALLOW }.forEach { link ->
            reasons += "Embedded link: ${link.reasons.firstOrNull { it.source != ScanReasonSource.PROVIDER_STATUS }?.message ?: link.summary}"
        }
        var score = max(signals.score, linkResults.maxOfOrNull { it.riskScore } ?: 0f)
        if (extractedLinks.size > 8) {
            score = max(score, 30f)
            reasons += "Message has more than eight distinct links; additional destinations were not analyzed"
        }
        if (invalidLinks > 0) {
            score = max(score, 30f)
            reasons += "Message contains a malformed or unsupported web link; its destination could not be verified"
        }
        if (Regex("(?i)^(javascript:|data:text/html|intent:|file:)").containsMatchIn(text.trim())) {
            score = max(score, 30f)
            reasons += "Content requests executable data or an app handoff instead of an inspectable web link"
        }
        var modelStatus = "Text classifier unavailable; local rules active"
        textModel.getOrNull()?.let { model ->
            val probability = model.predict(text)
            modelStatus = "On-device text classifier • synthetic prototype"
            if (probability >= 0.60f) {
                score = max(score, 35f)
                reasons += "Local text classifier detected patterns associated with scam requests"
            }
        }
        val paymentReviews = PaymentQrParser.find(text)
        if (paymentReviews.isNotEmpty()) {
            score = max(score, 30f)
            reasons += "Payment QR: verify the recipient and amount in your payment app"
            reasons += paymentReviews.flatMap { it.issues }
        }
        val level = DecisionPolicy.level(score)
        val decision = if (linkResults.any { it.decision == ProtectionDecision.BLOCK }) ProtectionDecision.BLOCK else level.toProtectionDecision()
        val summary = if (decision == ProtectionDecision.ALLOW) "No strong scam signals found in the available content." else signals.advice
        val category = if (signals.category == "General" && linkResults.any { it.decision != ProtectionDecision.ALLOW }) "Suspicious link" else signals.category
        val structured = reasons.distinct().map { ScanReason(ScanReasonSource.LOCAL_HEURISTIC, "message_analysis", it, level) }
        return ScanResult(UUID.randomUUID().toString(), "Message scan", riskLevel = level,
            riskScore = score, explanation = (listOf(summary) + reasons.distinct()).joinToString("\n"),
            timestamp = System.currentTimeMillis(), decision = decision, summary = summary,
            reasons = structured, target = text.trim(), contentType = "message", category = category,
            durationMs = elapsed(start), modelStatus = modelStatus, guidance = signals.advice,
            coverage = "Local text and up to eight distinct links. No sender identity verification.",
            linkInspections = linkResults.flatMap { it.linkInspections.orEmpty() }.take(8),
            paymentReviews = paymentReviews)
    }

    private fun applyMlScore(base: ScanResult, url: String): ScanResult {
        return try {
            val probability = mlPredictor?.invoke(FeatureExtractor.extract(url))
                ?: urlModel.getOrThrow().predict(FeatureExtractor.extract(url))
            require(probability.isFinite() && probability in 0f..1f)
            val modelCoverage = com.sentinel.ai.ml.UrlModelCoverage.canWarnIndependently(UrlNormalizer.parse(url))
            val threshold = if (mlPredictor != null) 0.99f else urlModel.getOrThrow().warningThreshold
            val modelWarning = modelCoverage && probability >= threshold
            // Held-out structural model may WARN, never independently BLOCK or reduce evidence.
            val score = max(max(base.riskScore, DecisionPolicy.floor(base.riskLevel)),
                if (modelWarning) 35f else 0f).coerceIn(0f, 100f)
            val level = DecisionPolicy.level(score)
            val decision = if (base.decision == ProtectionDecision.BLOCK) ProtectionDecision.BLOCK else level.toProtectionDecision()
            val summary = if (decision != base.decision) "Local model and URL signals suggest caution. Verify this destination before opening." else base.summary
            val reasons = base.reasons + if (modelWarning) listOf(ScanReason(ScanReasonSource.LOCAL_HEURISTIC,
                "url_model", "On-device URL classifier detected phishing-like structural patterns", level)) else emptyList()
            base.copy(riskScore = score, riskLevel = level, decision = decision, headline = decision.defaultHeadline(),
                summary = summary, explanation = (listOf(summary) + reasons.map { it.message }).distinct().joinToString("\n"),
                reasons = reasons, recommendedAction = decision.defaultAction(), modelStatus = if (modelCoverage) "On-device URL model" else "On-device URL model • context limited",
                category = if (decision == ProtectionDecision.ALLOW) "Link check" else "Phishing risk",
                coverage = "URL structure, visible encoded destinations and local snapshot. No webpage fetch or redirect following; clean-looking links can still be fraudulent.",
                guidance = if (decision == ProtectionDecision.ALLOW) "Check the destination before entering sensitive information." else "Use the organization's official app or type its address yourself.")
        } catch (e: CancellationException) { throw e } catch (_: Exception) {
            base.copy(modelStatus = "URL model unavailable; local rules active")
        }
    }
    private fun elapsed(start: Long) = (System.nanoTime() - start) / 1_000_000
}
