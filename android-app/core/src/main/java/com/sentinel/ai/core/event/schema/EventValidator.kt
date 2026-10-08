package com.sentinel.ai.core.event.schema

import java.time.Instant
import java.util.UUID

object EventValidator {

    private val SCHEMA_VERSION_PATTERN = Regex("^\\d+\\.\\d+\\.\\d+$")
    private val DEVICE_ID_PATTERN = Regex("^[a-f0-9]{32,64}$")
    private val SHA256_PATTERN = Regex("^[a-f0-9]{64}$")
    private val MD5_PATTERN = Regex("^[a-f0-9]{32}$")
    private val E164_PATTERN = Regex("^\\+[1-9]\\d{6,14}$")
    private val COUNTRY_CODE_PATTERN = Regex("^[A-Z]{2}$")
    private val APP_VERSION_PATTERN = Regex("^\\d+\\.\\d+\\.\\d+$")
    private val LANGUAGE_PATTERN = Regex("^[a-z]{2,3}(-[A-Z]{2})?$")
    private val UUID_V4_PATTERN = Regex(
        "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
        RegexOption.IGNORE_CASE
    )

    fun validateBase(event: BaseEvent): ValidationResult {
        val errors = mutableListOf<String>()
        validateEnvelope(event, errors)
        validateSource(event.source, errors)
        validateContent(event.content, errors)
        validateChannelPayload(event.channel, event.channelPayload, errors)
        event.urls?.forEachIndexed { index, url -> validateUrlAnalysisItem(url, index, errors) }
        event.attachments?.forEachIndexed { index, attachment ->
            validateAttachmentAnalysisItem(attachment, index, errors)
        }
        event.riskAssessment?.let { validateRiskAssessment(it, errors) }
        event.investigationReport?.let { validateInvestigationReport(it, errors) }
        return toResult(errors)
    }

    fun validateCommunication(event: CommunicationEvent): ValidationResult {
        val errors = mutableListOf<String>()
        val baseResult = validateBase(event.event)
        if (baseResult is ValidationResult.Invalid) {
            errors.addAll(baseResult.errors)
        }
        return toResult(errors)
    }

    fun validateMessage(event: MessageEvent): ValidationResult {
        val errors = mutableListOf<String>()
        if (event.event.eventType !in EventSchemaConstants.MESSAGE_EVENT_TYPES) {
            errors.add("VAL-005: event_type ${event.event.eventType.value} is not an inbound message event type")
        }
        val baseResult = validateBase(event.event)
        if (baseResult is ValidationResult.Invalid) {
            errors.addAll(baseResult.errors)
        }
        return toResult(errors)
    }

    fun validateLink(event: LinkEvent): ValidationResult {
        val errors = mutableListOf<String>()
        if (event.event.eventType != EventType.URL_SCAN_COMPLETED) {
            errors.add("VAL-005: LinkEvent requires event_type sentinel.url.scan.completed")
        }
        if (event.event.urls.isNullOrEmpty()) {
            errors.add("LinkEvent requires at least one URL analysis item")
        }
        val baseResult = validateBase(event.event)
        if (baseResult is ValidationResult.Invalid) {
            errors.addAll(baseResult.errors)
        }
        return toResult(errors)
    }

    fun validateAttachment(event: AttachmentEvent): ValidationResult {
        val errors = mutableListOf<String>()
        if (event.event.eventType != EventType.FILE_SCAN_COMPLETED) {
            errors.add("VAL-005: AttachmentEvent requires event_type sentinel.file.scan.completed")
        }
        if (event.event.attachments.isNullOrEmpty()) {
            errors.add("AttachmentEvent requires at least one attachment analysis item")
        }
        val baseResult = validateBase(event.event)
        if (baseResult is ValidationResult.Invalid) {
            errors.addAll(baseResult.errors)
        }
        return toResult(errors)
    }

    fun validatePrivacyMode(event: BaseEvent, privacyMode: Boolean): ValidationResult {
        if (!privacyMode) {
            return ValidationResult.Valid
        }
        val errors = mutableListOf<String>()
        if (event.source.rawIdentifier != null) {
            errors.add("PRI-001: source.raw_identifier must be absent in privacy mode")
        }
        if (event.source.e164Number != null) {
            errors.add("PRI-001: source.e164_number must be absent in privacy mode")
        }
        if (event.source.displayName != null) {
            errors.add("PRI-001: source.display_name must be absent in privacy mode")
        }
        if (event.source.platformHandle != null) {
            errors.add("PRI-001: source.platform_handle must be absent in privacy mode")
        }
        when (val payload = event.channelPayload) {
            is ChannelPayloadData.Sms -> {
                if (payload.payload.senderNumberRaw != null) {
                    errors.add("PRI-001: channel_payload.sender_number_raw must be absent in privacy mode")
                }
            }
            is ChannelPayloadData.Call -> {
                if (payload.payload.callerNumberRaw != null) {
                    errors.add("PRI-001: channel_payload.caller_number_raw must be absent in privacy mode")
                }
            }
            is ChannelPayloadData.WhatsApp -> {
                if (payload.payload.groupName != null) {
                    errors.add("PRI-001: channel_payload.group_name must be absent in privacy mode")
                }
            }
            is ChannelPayloadData.Telegram -> {
                if (payload.payload.channelName != null) {
                    errors.add("PRI-001: channel_payload.channel_name must be absent in privacy mode")
                }
            }
            is ChannelPayloadData.Gmail -> {
                if (payload.payload.fromAddressRaw != null) {
                    errors.add("PRI-001: channel_payload.from_address_raw must be absent in privacy mode")
                }
            }
            is ChannelPayloadData.Copilot -> Unit
        }
        return toResult(errors)
    }

    private fun validateEnvelope(event: BaseEvent, errors: MutableList<String>) {
        if (!SCHEMA_VERSION_PATTERN.matches(event.schemaVersion)) {
            errors.add("VAL-004: schema_version must match semver pattern")
        } else if (event.schemaVersion !in EventSchemaConstants.RECOGNIZED_SCHEMA_VERSIONS) {
            errors.add("VAL-004: schema_version ${event.schemaVersion} is not a recognized published version")
        }

        if (!isValidUuidV4(event.eventId)) {
            errors.add("VAL-001: event_id must be a valid UUID v4")
        }

        if (!isValidIso8601(event.capturedAt)) {
            errors.add("captured_at must be a valid ISO 8601 timestamp")
        }
        if (!isValidIso8601(event.submittedAt)) {
            errors.add("submitted_at must be a valid ISO 8601 timestamp")
        }
        event.processedAt?.let {
            if (!isValidIso8601(it)) {
                errors.add("processed_at must be a valid ISO 8601 timestamp")
            }
        }

        if (isValidIso8601(event.capturedAt) && isValidIso8601(event.submittedAt)) {
            if (parseInstant(event.capturedAt) > parseInstant(event.submittedAt)) {
                errors.add("VAL-002: captured_at must be before or equal to submitted_at")
            }
        }
        if (event.processedAt != null &&
            isValidIso8601(event.submittedAt) &&
            isValidIso8601(event.processedAt)
        ) {
            if (parseInstant(event.submittedAt) > parseInstant(event.processedAt)) {
                errors.add("VAL-003: submitted_at must be before or equal to processed_at")
            }
        }

        val allowedTypes = EventSchemaConstants.CHANNEL_EVENT_TYPE_MAP[event.channel]
        if (allowedTypes == null || event.eventType !in allowedTypes) {
            if (event.eventType != EventType.URL_SCAN_COMPLETED &&
                event.eventType != EventType.FILE_SCAN_COMPLETED &&
                event.eventType != EventType.RISK_ASSESSED &&
                event.eventType != EventType.ALERT_TRIGGERED &&
                event.eventType != EventType.INVESTIGATION_COMPLETED
            ) {
                errors.add("VAL-005: channel ${event.channel} is inconsistent with event_type ${event.eventType.value}")
            }
        }

        if (!DEVICE_ID_PATTERN.matches(event.deviceId)) {
            errors.add("device_id must be 32-64 lowercase hex characters")
        }
        if (!APP_VERSION_PATTERN.matches(event.appVersion)) {
            errors.add("app_version must match semver pattern")
        }
        event.ttlSeconds?.let { ttl ->
            if (ttl !in 5..300) {
                errors.add("VAL-012: ttl_seconds must be between 5 and 300 inclusive")
            }
        }
    }

    private fun validateSource(source: SourceBlock, errors: MutableList<String>) {
        if (!SHA256_PATTERN.matches(source.identifierHash)) {
            errors.add("VAL-009: source.identifier_hash must be exactly 64 lowercase hex characters")
        }
        source.e164Number?.let { e164 ->
            if (!E164_PATTERN.matches(e164)) {
                errors.add("VAL-010: source.e164_number must match E.164 format")
            }
        }
        source.countryCode?.let { code ->
            if (!COUNTRY_CODE_PATTERN.matches(code)) {
                errors.add("VAL-011: source.country_code must be a valid ISO 3166-1 alpha-2 code")
            }
        }
        source.displayName?.let {
            if (it.length > 200) errors.add("source.display_name exceeds max length 200")
        }
        source.platformHandle?.let {
            if (it.length > 200) errors.add("source.platform_handle exceeds max length 200")
        }
        source.alphaSenderId?.let {
            if (it.length > 20) errors.add("source.alpha_sender_id exceeds max length 20")
        }
        source.reportedScamCount?.let {
            if (it < 0) errors.add("source.reported_scam_count must be >= 0")
        }
        source.intelligenceMatch?.riskScoreFromGraph?.let { score ->
            if (score !in 0.0..1.0) {
                errors.add("source.intelligence_match.risk_score_from_graph must be between 0.0 and 1.0")
            }
        }
    }

    private fun validateContent(content: ContentBlock, errors: MutableList<String>) {
        if (content.body.length > EventSchemaConstants.MAX_BODY_LENGTH) {
            errors.add("content.body exceeds maximum length of ${EventSchemaConstants.MAX_BODY_LENGTH}")
        }
        if (content.characterCount != content.body.length) {
            errors.add("VAL-006: content.character_count must equal len(content.body)")
        }
        if (content.containsUrls && content.urlCount != null && content.urlCount < 1) {
            errors.add("VAL-007: content.url_count must be >= 1 when content.contains_urls is true")
        }
        if (content.containsAttachments && content.attachmentCount != null && content.attachmentCount < 1) {
            errors.add("VAL-008: content.attachment_count must be >= 1 when content.contains_attachments is true")
        }
        content.language?.let { lang ->
            if (!LANGUAGE_PATTERN.matches(lang)) {
                errors.add("content.language must be a valid BCP 47 language tag")
            }
        }
        content.languageConfidence?.let {
            if (it !in 0.0..1.0) errors.add("content.language_confidence must be between 0.0 and 1.0")
        }
        content.callTranscriptConfidence?.let {
            if (it !in 0.0..1.0) errors.add("content.call_transcript_confidence must be between 0.0 and 1.0")
        }
        content.originalLength?.let { if (it < 0) errors.add("content.original_length must be >= 0") }
        content.wordCount?.let { if (it < 0) errors.add("content.word_count must be >= 0") }
        content.urlCount?.let { if (it < 0) errors.add("content.url_count must be >= 0") }
        content.attachmentCount?.let { if (it < 0) errors.add("content.attachment_count must be >= 0") }
        content.callTranscript?.let {
            if (it.length > 100_000) errors.add("content.call_transcript exceeds maximum length of 100000")
        }
        content.subject?.let {
            if (it.length > 500) errors.add("content.subject exceeds maximum length of 500")
        }
    }

    private fun validateChannelPayload(
        channel: Channel,
        payload: ChannelPayloadData,
        errors: MutableList<String>
    ) {
        when (channel) {
            Channel.SMS -> {
                if (payload !is ChannelPayloadData.Sms) {
                    errors.add("channel_payload must be SMS payload when channel is SMS")
                    return
                }
                if (payload.payload.messageParts < 1) {
                    errors.add("channel_payload.message_parts must be >= 1")
                }
            }
            Channel.CALL -> {
                if (payload !is ChannelPayloadData.Call) {
                    errors.add("channel_payload must be CALL payload when channel is CALL")
                    return
                }
                if (payload.payload.callState == CallState.ENDED && payload.payload.durationSeconds == null) {
                    // duration is optional per schema - no error
                }
            }
            Channel.WHATSAPP -> {
                if (payload !is ChannelPayloadData.WhatsApp) {
                    errors.add("channel_payload must be WHATSAPP payload when channel is WHATSAPP")
                }
            }
            Channel.TELEGRAM -> {
                if (payload !is ChannelPayloadData.Telegram) {
                    errors.add("channel_payload must be TELEGRAM payload when channel is TELEGRAM")
                }
            }
            Channel.GMAIL -> {
                if (payload !is ChannelPayloadData.Gmail) {
                    errors.add("channel_payload must be GMAIL payload when channel is GMAIL")
                }
            }
            Channel.COPILOT -> {
                if (payload !is ChannelPayloadData.Copilot) {
                    errors.add("channel_payload must be COPILOT payload when channel is COPILOT")
                }
            }
        }
    }

    private fun validateUrlAnalysisItem(item: UrlAnalysisItem, index: Int, errors: MutableList<String>) {
        val prefix = "urls[$index]"
        if (!isValidUuidV4(item.urlId)) {
            errors.add("$prefix.url_id must be a valid UUID v4")
        }
        if (item.rawUrl.length > 2048) errors.add("$prefix.raw_url exceeds max length 2048")
        if (item.normalizedUrl.length > 2048) errors.add("$prefix.normalized_url exceeds max length 2048")
        if (item.domain.length > 253) errors.add("$prefix.domain exceeds max length 253")
        if (item.urlRiskScore !in 0.0..1.0) {
            errors.add("$prefix.url_risk_score must be between 0.0 and 1.0")
        }
        item.registrationCountry?.let { country ->
            if (!COUNTRY_CODE_PATTERN.matches(country)) {
                errors.add("$prefix.registration_country must be ISO 3166-1 alpha-2")
            }
        }
        if (!isValidIso8601(item.analyzedAt)) {
            errors.add("$prefix.analyzed_at must be a valid ISO 8601 timestamp")
        }
    }

    private fun validateAttachmentAnalysisItem(
        item: AttachmentAnalysisItem,
        index: Int,
        errors: MutableList<String>
    ) {
        val prefix = "attachments[$index]"
        if (!isValidUuidV4(item.attachmentId)) {
            errors.add("$prefix.attachment_id must be a valid UUID v4")
        }
        if (item.filename.length > 255) errors.add("$prefix.filename exceeds max length 255")
        if (item.fileExtension.length > 20) errors.add("$prefix.file_extension exceeds max length 20")
        if (item.mimeType.length > 100) errors.add("$prefix.mime_type exceeds max length 100")
        if (!SHA256_PATTERN.matches(item.sha256Hash)) {
            errors.add("VAL-015: $prefix.sha256_hash must be exactly 64 lowercase hex characters")
        }
        item.md5Hash?.let { md5 ->
            if (!MD5_PATTERN.matches(md5)) {
                errors.add("$prefix.md5_hash must be exactly 32 lowercase hex characters")
            }
        }
        if (item.fileSizeBytes < 0) {
            errors.add("$prefix.file_size_bytes must be >= 0")
        } else if (item.fileSizeBytes > EventSchemaConstants.MAX_ATTACHMENT_SIZE_BYTES) {
            errors.add("VAL-016: $prefix.file_size_bytes must be <= ${EventSchemaConstants.MAX_ATTACHMENT_SIZE_BYTES}")
        }
        if (item.attachmentRiskScore !in 0.0..1.0) {
            errors.add("$prefix.attachment_risk_score must be between 0.0 and 1.0")
        }
        if (!isValidIso8601(item.analyzedAt)) {
            errors.add("$prefix.analyzed_at must be a valid ISO 8601 timestamp")
        }
    }

    private fun validateRiskAssessment(assessment: RiskAssessmentBlock, errors: MutableList<String>) {
        if (assessment.overallScore !in 0.0..1.0) {
            errors.add("risk_assessment.overall_score must be between 0.0 and 1.0")
        }
        if (assessment.confidence !in 0.0..1.0) {
            errors.add("risk_assessment.confidence must be between 0.0 and 1.0")
        }
        if (assessment.agentScores.isEmpty()) {
            errors.add("VAL-020: risk_assessment.agent_scores must contain at least one entry")
        }
        val scoreRange = EventSchemaConstants.RISK_LEVEL_SCORE_RANGES[assessment.riskLevel]
        if (scoreRange != null && assessment.overallScore !in scoreRange) {
            errors.add("VAL-013: risk_assessment.overall_score is inconsistent with risk_assessment.risk_level")
        }
        assessment.agentScores.forEachIndexed { index, score ->
            if (score.score !in 0.0..1.0) {
                errors.add("risk_assessment.agent_scores[$index].score must be between 0.0 and 1.0")
            }
            if (score.confidence !in 0.0..1.0) {
                errors.add("risk_assessment.agent_scores[$index].confidence must be between 0.0 and 1.0")
            }
            score.latencyMs?.let {
                if (it < 0) errors.add("risk_assessment.agent_scores[$index].latency_ms must be >= 0")
            }
        }
        assessment.neo4jContextScore?.let {
            if (it !in 0.0..1.0) errors.add("risk_assessment.neo4j_context_score must be between 0.0 and 1.0")
        }
        assessment.falsePositiveProbability?.let {
            if (it !in 0.0..1.0) errors.add("risk_assessment.false_positive_probability must be between 0.0 and 1.0")
        }
        if (!isValidIso8601(assessment.assessedAt)) {
            errors.add("risk_assessment.assessed_at must be a valid ISO 8601 timestamp")
        }
    }

    private fun validateInvestigationReport(report: InvestigationReportBlock, errors: MutableList<String>) {
        if (!isValidUuidV4(report.reportId)) {
            errors.add("investigation_report.report_id must be a valid UUID v4")
        }
        if (report.summary.length > 150) {
            errors.add("investigation_report.summary exceeds maximum length of 150")
        }
        if (report.detailedExplanation.length > 1000) {
            errors.add("investigation_report.detailed_explanation exceeds maximum length of 1000")
        }
        if (report.whatHappened.length > 500) {
            errors.add("investigation_report.what_happened exceeds maximum length of 500")
        }
        if (report.whyItsRisky.length > 500) {
            errors.add("investigation_report.why_its_risky exceeds maximum length of 500")
        }
        if (report.whatToDo.length > 200) {
            errors.add("investigation_report.what_to_do exceeds maximum length of 200")
        }
        if (report.recommendedActions.isEmpty()) {
            errors.add("investigation_report.recommended_actions must contain at least one entry")
        }
        val primaryCount = report.recommendedActions.count { it.isPrimary }
        if (primaryCount != 1) {
            errors.add("VAL-014: exactly one recommended_actions[n].is_primary must be true")
        }
        report.recommendedActions.forEachIndexed { index, action ->
            if (action.label.length > 50) {
                errors.add("investigation_report.recommended_actions[$index].label exceeds max length 50")
            }
            if (action.description.length > 200) {
                errors.add("investigation_report.recommended_actions[$index].description exceeds max length 200")
            }
        }
        report.evidence?.forEachIndexed { index, evidence ->
            if (evidence.description.length > 300) {
                errors.add("investigation_report.evidence[$index].description exceeds max length 300")
            }
        }
        if (!isValidIso8601(report.generatedAt)) {
            errors.add("investigation_report.generated_at must be a valid ISO 8601 timestamp")
        }
    }

    private fun isValidUuidV4(value: String): Boolean {
        if (!UUID_V4_PATTERN.matches(value)) return false
        return try {
            UUID.fromString(value).version() == 4
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun isValidIso8601(value: String): Boolean = try {
        Instant.parse(value)
        true
    } catch (_: Exception) {
        false
    }

    private fun parseInstant(value: String): Instant = Instant.parse(value)

    private fun toResult(errors: List<String>): ValidationResult =
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
}
