package com.sentinel.ai.core.event.schema

/**
 * URL scan enrichment event populated by LinkAgent per EVENT_SCHEMA.md §7.1.
 * Event type: sentinel.url.scan.completed
 */
data class LinkEvent(
    val event: BaseEvent
) {
    val urls: List<UrlAnalysisItem>
        get() = requireNotNull(event.urls) { "LinkEvent requires a non-null urls block" }

    fun validate(): ValidationResult = EventValidator.validateLink(this)

    fun toBaseEvent(): BaseEvent = event

    companion object {
        fun from(event: BaseEvent): LinkEvent? {
            if (event.eventType != EventType.URL_SCAN_COMPLETED) {
                return null
            }
            if (event.urls.isNullOrEmpty()) {
                return null
            }
            return LinkEvent(event)
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
            urls: List<UrlAnalysisItem>,
            processedAt: String? = null,
            requestId: String? = null,
            ttlSeconds: Int? = EventSchemaConstants.DEFAULT_TTL_SECONDS,
            riskAssessment: RiskAssessmentBlock? = null,
            investigationReport: InvestigationReportBlock? = null
        ): LinkEvent = LinkEvent(
            BaseEvent(
                schemaVersion = schemaVersion,
                eventId = eventId,
                eventType = EventType.URL_SCAN_COMPLETED,
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
                riskAssessment = riskAssessment,
                investigationReport = investigationReport
            )
        )
    }
}
