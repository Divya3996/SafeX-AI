package com.sentinel.ai.core.model

/** Nullable additions to ScanResult keep older saved JSON readable. No image or full draft is stored. */
enum class InputOrigin { SCREEN_CAPTURE, IMPORTED_IMAGE, CLIPBOARD, MANUAL }
enum class ReviewScope { MESSAGE, SELECTED_LINES, EDITED_TEXT, LINKS, QR }
enum class AssessmentCoverage { COMPLETE, LIMITED, UNSUPPORTED }
enum class ExtractionEngine { LATIN, DEVANAGARI, GUJARATI, QR }
data class ExtractionOutcome(val engine: ExtractionEngine, val status: EvidenceSourceStatus,
    val items: Int = 0, val qualityLimited: Boolean = false, val elapsedMs: Long = 0)
data class InputProvenance(val origin: InputOrigin, val scope: ReviewScope, val edited: Boolean = false,
    val selectedLineCount: Int = 0, val crop: ScreenSelection? = null)
data class ScanCoverageDetails(val assessment: AssessmentCoverage, val engines: List<ExtractionOutcome> = emptyList(),
    val visibleLinks: Int = 0, val analyzedLinks: Int = 0, val selectedQr: Boolean = false, val imageDownsampled: Boolean = false)
data class ScanTimings(val extractionMs: Long? = null, val analysisMs: Long? = null, val captureMs: Long? = null)

enum class QrPayloadKind { WEB_LINK, PAYMENT, TEXT, CONTACT, WIFI, EMAIL, SMS, PHONE, APP_ACTION }
object QrPayloadClassifier {
    fun classify(value: String): QrPayloadKind = when {
        value.startsWith("upi:", true) -> QrPayloadKind.PAYMENT
        value.startsWith("http://", true) || value.startsWith("https://", true) || value.startsWith("www.", true) -> QrPayloadKind.WEB_LINK
        value.startsWith("BEGIN:VCARD", true) || value.startsWith("MECARD:", true) -> QrPayloadKind.CONTACT
        value.startsWith("WIFI:", true) -> QrPayloadKind.WIFI
        value.startsWith("mailto:", true) || value.startsWith("MATMSG:", true) -> QrPayloadKind.EMAIL
        value.startsWith("sms:", true) || value.startsWith("smsto:", true) -> QrPayloadKind.SMS
        value.startsWith("tel:", true) -> QrPayloadKind.PHONE
        Regex("^[a-z][a-z0-9+.-]*:", RegexOption.IGNORE_CASE).containsMatchIn(value) -> QrPayloadKind.APP_ACTION
        else -> QrPayloadKind.TEXT
    }
    fun unsupported(value: String) = classify(value) in setOf(QrPayloadKind.CONTACT, QrPayloadKind.WIFI,
        QrPayloadKind.EMAIL, QrPayloadKind.SMS, QrPayloadKind.PHONE, QrPayloadKind.APP_ACTION)
}
