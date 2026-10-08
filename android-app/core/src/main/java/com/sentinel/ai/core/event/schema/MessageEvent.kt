package com.sentinel.ai.core.event.schema

/**
 * Inbound channel message event (Android → Backend) per EVENT_SCHEMA.md §2.1.
 * Covers SMS, CALL, WHATSAPP, TELEGRAM, GMAIL, and COPILOT inbound event types.
 */
data class MessageEvent(
    val event: BaseEvent
) {
    fun validate(): ValidationResult = EventValidator.validateMessage(this)

    fun toBaseEvent(): BaseEvent = event

    companion object {
        fun from(event: BaseEvent): MessageEvent? {
            if (event.eventType !in EventSchemaConstants.MESSAGE_EVENT_TYPES) {
                return null
            }
            return MessageEvent(event)
        }

        fun create(
            schemaVersion: String = EventSchemaConstants.SCHEMA_VERSION,
            eventId: String,
            eventType: EventType,
            channel: Channel,
            processingStatus: ProcessingStatus = ProcessingStatus.CAPTURED,
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
            scamRiskScore: Int? = null,
            scamRiskLevel: ScamRiskLevel? = null,
            scamExplanations: List<String>? = null
        ): MessageEvent = MessageEvent(
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
                scamRiskScore = scamRiskScore,
                scamRiskLevel = scamRiskLevel,
                scamExplanations = scamExplanations
            )
        )
    }
}
