package com.sentinel.ai.core.event.schema

enum class EventType(val value: String) {
    SMS_RECEIVED("sentinel.sms.received"),
    CALL_INCOMING("sentinel.call.incoming"),
    CALL_ENDED("sentinel.call.ended"),
    WHATSAPP_MESSAGE_RECEIVED("sentinel.whatsapp.message.received"),
    WHATSAPP_FILE_SHARED("sentinel.whatsapp.file.shared"),
    TELEGRAM_MESSAGE_RECEIVED("sentinel.telegram.message.received"),
    TELEGRAM_FILE_SHARED("sentinel.telegram.file.shared"),
    EMAIL_RECEIVED("sentinel.email.received"),
    COPILOT_QUERY("sentinel.copilot.query"),
    URL_SCAN_COMPLETED("sentinel.url.scan.completed"),
    FILE_SCAN_COMPLETED("sentinel.file.scan.completed"),
    RISK_ASSESSED("sentinel.risk.assessed"),
    ALERT_TRIGGERED("sentinel.alert.triggered"),
    INVESTIGATION_COMPLETED("sentinel.investigation.completed");

    companion object {
        fun fromValue(value: String): EventType? = entries.find { it.value == value }
    }
}

enum class Channel {
    SMS,
    CALL,
    WHATSAPP,
    TELEGRAM,
    GMAIL,
    COPILOT
}

enum class ProcessingStatus {
    CAPTURED,
    QUEUED,
    ANALYZING,
    COMPLETED,
    FAILED,
    EXPIRED
}

enum class IdentifierType {
    PHONE_NUMBER,
    EMAIL_ADDRESS,
    WHATSAPP_JID,
    TELEGRAM_USER_ID,
    TELEGRAM_CHANNEL_ID,
    ALPHA_SENDER_ID,
    UNKNOWN
}

enum class ContactType {
    PERSONAL,
    BUSINESS,
    UNKNOWN
}

enum class MediaType {
    TEXT,
    IMAGE,
    AUDIO,
    VIDEO,
    DOCUMENT,
    STICKER,
    CONTACT_CARD,
    LOCATION,
    APK,
    VOICE_NOTE,
    UNKNOWN
}

enum class SmsType {
    TRANSACTIONAL,
    PROMOTIONAL,
    PERSONAL
}

enum class CallDirection {
    INBOUND,
    OUTBOUND
}

enum class CallState {
    RINGING,
    ACTIVE,
    ENDED,
    MISSED,
    REJECTED,
    BUSY
}

enum class CallType {
    VOICE,
    VIDEO
}

enum class WhatsAppMessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    STICKER,
    CONTACT_CARD,
    LOCATION,
    VOICE_NOTE,
    UNKNOWN
}

enum class CaptureMethod {
    NOTIFICATION_LISTENER,
    ACCESSIBILITY_SERVICE,
    USER_PASTE,
    SHARE_INTENT
}

enum class TelegramChatType {
    PRIVATE,
    GROUP,
    SUPERGROUP,
    CHANNEL,
    UNKNOWN
}

enum class EmailAuthResult {
    PASS,
    FAIL,
    SOFTFAIL,
    NEUTRAL,
    NONE,
    UNKNOWN
}

enum class UrlScheme(val jsonValue: String) {
    HTTPS("https"),
    HTTP("http"),
    FTP("ftp"),
    OTHER("other");

    companion object {
        fun fromJson(value: String): UrlScheme = when (value.lowercase()) {
            "https" -> HTTPS
            "http" -> HTTP
            "ftp" -> FTP
            else -> OTHER
        }
    }
}

enum class FileCategory {
    PDF,
    DOCUMENT,
    SPREADSHEET,
    IMAGE,
    AUDIO,
    VIDEO,
    APK,
    ARCHIVE,
    EXECUTABLE,
    UNKNOWN
}

enum class RiskLevel {
    GREEN,
    YELLOW,
    RED,
    CRITICAL
}

enum class AggregationMethod {
    WEIGHTED_AVERAGE,
    MAX_SCORE,
    BAYESIAN_COMBINATION,
    ENSEMBLE_VOTE
}

enum class ActionType {
    BLOCK_SENDER,
    DELETE_MESSAGE,
    REPORT_NCCRP,
    REPORT_TRAI,
    CALL_HELPLINE_1930,
    DO_NOT_SHARE_OTP,
    DO_NOT_CLICK_LINK,
    DO_NOT_DOWNLOAD_FILE,
    DO_NOT_MAKE_PAYMENT,
    DISCONNECT_CALL,
    CONTACT_BANK,
    CONTACT_POLICE,
    MARK_SPAM,
    FORWARD_TO_SENTINEL,
    CUSTOM
}

enum class EvidenceType {
    KEYWORD_MATCH,
    URL_RISK,
    DOMAIN_AGE,
    BRAND_IMPERSONATION,
    FEED_MATCH,
    GRAPH_MATCH,
    PERMISSION_RISK,
    MALWARE_HASH,
    AUTHORITY_CLAIM,
    COERCION_LANGUAGE,
    URGENCY_LANGUAGE,
    FAKE_NOTICE,
    DLT_ABSENT,
    AUTH_FAILURE,
    SENDER_MISMATCH,
    FORWARD_CHAIN,
    EXECUTABLE_ATTACHMENT
}

enum class EvidenceSeverity {
    INFO,
    WARNING,
    CRITICAL
}
