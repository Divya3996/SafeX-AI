package com.sentinel.ai.core.event.schema

/**
 * Schema-compliant communication event emitted by Android listeners and consumed by the backend.
 * Wraps a [BaseEvent] with communication-specific validation per EVENT_SCHEMA.md §3.
 */
data class CommunicationEvent(
    val event: BaseEvent
) {
    fun validate(): ValidationResult = EventValidator.validateCommunication(this)

    fun toBaseEvent(): BaseEvent = event

    companion object {
        fun from(event: BaseEvent): CommunicationEvent = CommunicationEvent(event)

        fun create(
            schemaVersion: String = EventSchemaConstants.SCHEMA_VERSION,
            eventId: String,
            eventType: EventType,
            channel: Channel,
            processingStatus: ProcessingStatus,
            capturedAt: String,
            submittedAt: String,
            deviceId: String,
            appVersion: String,
            source: SourceBlock,
            content: ContentBlock,
            channelPayload: ChannelPayloadData,
            processedAt: String? = null,
            requestId: String? = null,
            ttlSeconds: Int? = EventSchemaConstants.DEFAULT_TTL_SECONDS,
            urls: List<UrlAnalysisItem>? = null,
            attachments: List<AttachmentAnalysisItem>? = null,
            riskAssessment: RiskAssessmentBlock? = null,
            investigationReport: InvestigationReportBlock? = null
        ): CommunicationEvent = CommunicationEvent(
            BaseEvent(
                schemaVersion = schemaVersion,
                eventId = eventId,
                eventType = eventType,
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
                urls = urls,
                attachments = attachments,
                riskAssessment = riskAssessment,
                investigationReport = investigationReport
            )
        )
    }
}
