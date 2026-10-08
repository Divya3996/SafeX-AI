package com.sentinel.ai.agents.whatsapp

import com.sentinel.ai.core.event.schema.CaptureMethod
import com.sentinel.ai.core.event.schema.Channel
import com.sentinel.ai.core.event.schema.ChannelPayloadData
import com.sentinel.ai.core.event.schema.ContentBlock
import com.sentinel.ai.core.event.schema.EventSchemaConstants
import com.sentinel.ai.core.event.schema.EventType
import com.sentinel.ai.core.event.schema.GmailChannelPayload
import com.sentinel.ai.core.event.schema.IdentifierType
import com.sentinel.ai.core.event.schema.MediaType
import com.sentinel.ai.core.event.schema.MessageEvent
import com.sentinel.ai.core.event.schema.ProcessingStatus
import com.sentinel.ai.core.event.schema.SourceBlock
import com.sentinel.ai.core.event.schema.TelegramChannelPayload
import com.sentinel.ai.core.event.schema.TelegramChatType
import com.sentinel.ai.core.event.schema.ValidationResult
import com.sentinel.ai.core.event.schema.UrlAnalysisItem
import com.sentinel.ai.core.event.schema.UrlScheme
import com.sentinel.ai.core.event.schema.WhatsAppChannelPayload
import com.sentinel.ai.core.event.schema.WhatsAppMessageType
import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class NotificationEventBuilder @Inject constructor() {

    fun build(raw: WhatsAppRawNotificationData, isKnownContact: Boolean = false): MessageEvent? {
        if (raw.senderDisplayName.isNullOrBlank() || raw.messageText.isNullOrBlank()) return null

        val capturedAt = Instant.ofEpochMilli(raw.capturedAtMs).toString()
        val normalizedMessage = raw.messageText.trim()
        val truncated = normalizedMessage.take(EventSchemaConstants.MAX_BODY_LENGTH)
        val bodyTruncated = normalizedMessage.length > EventSchemaConstants.MAX_BODY_LENGTH
        val urls = extractUrls(truncated, capturedAt)
        val heuristics = WhatsAppContentHeuristics.analyze(truncated)
        val scamResult = ScamRuleEngine.evaluate(
            messageText = truncated,
            urls = urls,
            isKnownContact = isKnownContact
        )
        val senderHash = sha256Hex(raw.senderDisplayName)
        val chatHash = sha256Hex(
            buildString {
                append(raw.packageName)
                append(':')
                append(raw.conversationTitle ?: raw.senderDisplayName)
            }
        )

        // The pipeline is intentionally a single, universal parser/builder shared by every
        // supported app (WhatsApp, Telegram, Gmail, Google Messages, Instagram, Messenger,
        // Signal, Discord, Slack). Previously this method unconditionally tagged every event as
        // channel=WHATSAPP / eventType=WHATSAPP_MESSAGE_RECEIVED / identifierType=WHATSAPP_JID
        // regardless of the source package, which mislabeled every non-WhatsApp notification.
        // The mapping below routes each package to the correct existing schema channel where one
        // exists (WhatsApp, Telegram, Gmail); apps without a dedicated schema channel keep using
        // the generic message-shaped payload, which is a known schema limitation rather than a
        // code defect - see delivery notes.
        val mapping = resolveChannelMapping(raw.packageName)

        return MessageEvent.create(
            eventId = UUID.randomUUID().toString(),
            eventType = mapping.eventType,
            channel = mapping.channel,
            processingStatus = ProcessingStatus.CAPTURED,
            capturedAt = capturedAt,
            submittedAt = capturedAt,
            deviceId = sha256Hex(raw.packageName),
            appVersion = APP_VERSION,
            source = SourceBlock(
                identifierHash = senderHash,
                identifierType = mapping.identifierType,
                isKnownContact = isKnownContact
            ),
            content = ContentBlock(
                body = truncated,
                bodyTruncated = bodyTruncated,
                characterCount = truncated.length,
                containsUrls = urls.isNotEmpty(),
                containsAttachments = false,
                urlCount = urls.size.takeIf { it > 0 },
                hasUrgencyLanguage = heuristics.hasUrgencyLanguage,
                hasAuthorityClaim = heuristics.hasAuthorityClaim,
                hasFinancialMention = heuristics.hasFinancialMention,
                mediaType = MediaType.TEXT
            ),
            urls = urls.takeIf { it.isNotEmpty() },
            scamRiskScore = scamResult.riskScore,
            scamRiskLevel = scamResult.riskLevel,
            scamExplanations = scamResult.explanations,
            channelPayload = buildChannelPayload(mapping.channel, raw, senderHash, chatHash)
        )
    }

    fun validate(event: MessageEvent): ValidationResult = event.validate()

    private data class ChannelMapping(
        val channel: Channel,
        val eventType: EventType,
        val identifierType: IdentifierType
    )

    private fun resolveChannelMapping(packageName: String): ChannelMapping = when (packageName) {
        "com.whatsapp", "com.whatsapp.w4b" -> ChannelMapping(
            channel = Channel.WHATSAPP,
            eventType = EventType.WHATSAPP_MESSAGE_RECEIVED,
            identifierType = IdentifierType.WHATSAPP_JID
        )
        "org.telegram.messenger" -> ChannelMapping(
            channel = Channel.TELEGRAM,
            eventType = EventType.TELEGRAM_MESSAGE_RECEIVED,
            identifierType = IdentifierType.TELEGRAM_USER_ID
        )
        "com.google.android.gm" -> ChannelMapping(
            channel = Channel.GMAIL,
            eventType = EventType.EMAIL_RECEIVED,
            identifierType = IdentifierType.EMAIL_ADDRESS
        )
        // Google Messages, Instagram, Messenger, Signal, Discord and Slack are supported by the
        // notification pipeline but do not have a dedicated Channel/EventType/ChannelPayload in
        // the frozen event schema. They are normalized onto the generic WhatsApp-shaped message
        // payload (the schema's only generic "instant message" channel) rather than inventing new
        // schema types, which would go beyond a stabilization fix. This is a schema coverage gap,
        // not a parser defect - see delivery notes.
        else -> ChannelMapping(
            channel = Channel.WHATSAPP,
            eventType = EventType.WHATSAPP_MESSAGE_RECEIVED,
            identifierType = IdentifierType.ALPHA_SENDER_ID
        )
    }

    private fun buildChannelPayload(
        channel: Channel,
        raw: WhatsAppRawNotificationData,
        senderHash: String,
        chatHash: String
    ): ChannelPayloadData = when (channel) {
        Channel.TELEGRAM -> ChannelPayloadData.Telegram(
            TelegramChannelPayload(
                chatIdHash = chatHash,
                chatType = if (raw.isGroupChat) TelegramChatType.GROUP else TelegramChatType.PRIVATE,
                messageType = WhatsAppMessageType.TEXT,
                captureMethod = CaptureMethod.NOTIFICATION_LISTENER,
                senderUserIdHash = senderHash,
                // Intentionally left null: matches the existing WhatsApp payload's privacy
                // posture (see WhatsAppEventBuilderTest "sets group name to null for privacy
                // mode compliance") where conversation/group names are never embedded in the
                // channel payload.
                channelName = null
            )
        )
        Channel.GMAIL -> {
            val emailIdentifier = raw.senderDisplayName.orEmpty()
            val fromDomain = emailIdentifier.substringAfter('@', "").ifBlank { "unknown" }.lowercase()
            ChannelPayloadData.Gmail(
                GmailChannelPayload(
                    // Notification listeners do not expose Gmail's real message-id header; this
                    // is a locally generated capture id used only to correlate this event, not a
                    // claim about the underlying email's identity.
                    messageId = UUID.randomUUID().toString(),
                    fromAddressHash = senderHash,
                    fromDomain = fromDomain,
                    hasHtmlBody = false,
                    fromDisplayName = raw.senderDisplayName
                )
            )
        }
        else -> ChannelPayloadData.WhatsApp(
            WhatsAppChannelPayload(
                chatIdHash = chatHash,
                senderWaIdHash = senderHash,
                isGroupChat = raw.isGroupChat,
                messageType = WhatsAppMessageType.TEXT,
                captureMethod = CaptureMethod.NOTIFICATION_LISTENER,
                groupName = null,
                isForwarded = raw.isForwarded,
                forwardChainLength = raw.forwardChainLength,
                hasCallButton = raw.hasCallButton
            )
        )
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun extractUrls(body: String, analyzedAt: String): List<UrlAnalysisItem> {
        val rawUrls = LinkedHashSet<String>()
        val matches = (URL_REGEX.findAll(body) + WWW_REGEX.findAll(body))
            .sortedBy { it.range.first }

        matches.forEach { match ->
            rawUrls.add(match.value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}', '"', '\''))
        }

        return rawUrls.map { rawUrl ->
            val normalizedUrl = normalizeUrl(rawUrl)
            val parsed = URI(normalizedUrl)
            val host = parsed.host ?: normalizedUrl.removePrefix("www.").substringBefore('/')
            val domain = host.lowercase()
            val tld = domain.substringAfterLast('.', "")
            UrlAnalysisItem(
                urlId = UUID.randomUUID().toString(),
                rawUrl = rawUrl,
                normalizedUrl = normalizedUrl,
                domain = domain,
                tld = tld,
                urlScheme = when (parsed.scheme?.lowercase()) {
                    "https" -> UrlScheme.HTTPS
                    "http" -> UrlScheme.HTTP
                    else -> UrlScheme.OTHER
                },
                isShortened = SHORTENER_DOMAINS.any { shortened ->
                    domain == shortened || domain.endsWith(".$shortened")
                },
                isIpAddressUrl = IP_ADDRESS_REGEX.matches(host),
                brandImpersonationDetected = false,
                phishingFeedMatch = false,
                urlRiskScore = 0.0,
                analyzedAt = analyzedAt
            )
        }
    }

    private fun normalizeUrl(rawUrl: String): String =
        if (rawUrl.startsWith("www.", ignoreCase = true)) "https://$rawUrl" else rawUrl

    private object WhatsAppContentHeuristics {
        private val urgencyTerms = listOf(
            "arrest", "urgent", "deadline", "block", "suspend", "action required",
            "legal notice", "turant", "abhi"
        )
        private val authorityTerms = listOf(
            "cbi", "ed", "rbi", "trai", "customs", "police", "income tax", "ministry", "court", "tribunal"
        )
        private val financialTerms = listOf(
            "rs", "rupee", "lakh", "crore", "account", "payment", "refund", "transfer", "fine", "penalty"
        )

        fun analyze(body: String): ContentSignals {
            val lower = body.lowercase()
            return ContentSignals(
                hasUrgencyLanguage = urgencyTerms.any { lower.contains(it) },
                hasAuthorityClaim = authorityTerms.any { lower.contains(it) },
                hasFinancialMention = financialTerms.any { lower.contains(it) }
            )
        }
    }

    private data class ContentSignals(
        val hasUrgencyLanguage: Boolean,
        val hasAuthorityClaim: Boolean,
        val hasFinancialMention: Boolean
    )

    companion object {
        private const val APP_VERSION = "1.0.0"
        private val URL_REGEX = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
        private val WWW_REGEX = Regex("""(?<![A-Za-z0-9_./-])www\.[^\s<>"']+""", RegexOption.IGNORE_CASE)
        private val IP_ADDRESS_REGEX = Regex("""^\d{1,3}(?:\.\d{1,3}){3}$""")
        private val SHORTENER_DOMAINS = setOf(
            "bit.ly",
            "tinyurl.com",
            "t.co",
            "goo.gl",
            "is.gd",
            "buff.ly",
            "cutt.ly",
            "rebrand.ly"
        )
    }
}

/** Compatibility name for the existing WhatsApp event schema builder and tests. */
typealias WhatsAppEventBuilder = NotificationEventBuilder
