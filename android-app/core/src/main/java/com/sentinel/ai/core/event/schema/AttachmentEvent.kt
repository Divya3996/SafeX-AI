package com.sentinel.ai.core.event.schema

/**
 * Attachment scan enrichment event populated by FileAgent per EVENT_SCHEMA.md §7.2.
 * Event type: sentinel.file.scan.completed
 */
data class AttachmentEvent(
    val event: BaseEvent
) {
    val attachments: List<AttachmentAnalysisItem>
        get() = requireNotNull(event.attachments) { "AttachmentEvent requires a non-null attachments block" }

    fun validate(): ValidationResult = EventValidator.validateAttachment(this)

    fun toBaseEvent(): BaseEvent = event

    companion object {
        fun from(event: BaseEvent): AttachmentEvent? {
            if (event.eventType != EventType.FILE_SCAN_COMPLETED) {
                return null
            }
            if (event.attachments.isNullOrEmpty()) {
                return null
            }
            return AttachmentEvent(event)
        }

        fun create(
            schemaVersion: String = EventSchemaConstants.SCHEMA_VERSION,
            eventId: String,
            channel: Channel,
            processingStatus: ProcessingStatus,
            capturedAt: String,
            submittedAt: String,
            deviceId: String,
            appVersion: String,
            source: SourceBlock,
            content: ContentBlock,
            channelPayload: ChannelPayloadData,
            attachments: List<AttachmentAnalysisItem>,
            processedAt: String? = null,
            requestId: String? = null,
            ttlSeconds: Int? = EventSchemaConstants.DEFAULT_TTL_SECONDS,
            riskAssessment: RiskAssessmentBlock? = null,
            investigationReport: InvestigationReportBlock? = null
        ): AttachmentEvent = AttachmentEvent(
            BaseEvent(
                schemaVersion = schemaVersion,
                eventId = eventId,
                eventType = EventType.FILE_SCAN_COMPLETED,
                channel = channel,
                processingStatus = processingStatus,
                capturedAt = capturedAt,
                submittedAt = submittedAt,
                deviceId = deviceId,
                appVersion = appVersion,
                source = source,
                content = content,
                channelPayload = channelPayload,
                processedAt = processedAt,
                requestId = requestId,
                ttlSeconds = ttlSeconds,
                attachments = attachments,
                riskAssessment = riskAssessment,
                investigationReport = investigationReport
            )
        )
    }
}
