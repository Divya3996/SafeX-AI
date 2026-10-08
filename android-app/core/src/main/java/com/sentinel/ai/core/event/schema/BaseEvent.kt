package com.sentinel.ai.core.event.schema

import com.google.gson.annotations.SerializedName

/**
 * Universal base envelope for all Sentinel AI events per EVENT_SCHEMA.md §3 and §8.1.
 */
data class BaseEvent(
    @SerializedName("schema_version") val schemaVersion: String,
    @SerializedName("event_id") val eventId: String,
    @SerializedName("event_type") val eventType: EventType,
    @SerializedName("channel") val channel: Channel,
    @SerializedName("processing_status") val processingStatus: ProcessingStatus,
    @SerializedName("captured_at") val capturedAt: String,
    @SerializedName("submitted_at") val submittedAt: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("source") val source: SourceBlock,
    @SerializedName("content") val content: ContentBlock,
    @SerializedName("channel_payload") val channelPayload: ChannelPayloadData,
    @SerializedName("processed_at") val processedAt: String? = null,
    @SerializedName("request_id") val requestId: String? = null,
    @SerializedName("ttl_seconds") val ttlSeconds: Int? = null,
    @SerializedName("urls") val urls: List<UrlAnalysisItem>? = null,
    @SerializedName("scam_risk_score") val scamRiskScore: Int? = null,
    @SerializedName("scam_risk_level") val scamRiskLevel: ScamRiskLevel? = null,
    @SerializedName("scam_explanations") val scamExplanations: List<String>? = null,
    @SerializedName("attachments") val attachments: List<AttachmentAnalysisItem>? = null,
    @SerializedName("risk_assessment") val riskAssessment: RiskAssessmentBlock? = null,
    @SerializedName("investigation_report") val investigationReport: InvestigationReportBlock? = null
) {
    fun validate(): ValidationResult = EventValidator.validateBase(this)
}
