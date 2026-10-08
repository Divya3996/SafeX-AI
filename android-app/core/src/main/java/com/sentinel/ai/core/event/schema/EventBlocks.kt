package com.sentinel.ai.core.event.schema

import com.google.gson.annotations.SerializedName

data class SourceBlock(
    @SerializedName("identifier_hash") val identifierHash: String,
    @SerializedName("identifier_type") val identifierType: IdentifierType,
    @SerializedName("is_known_contact") val isKnownContact: Boolean,
    @SerializedName("raw_identifier") val rawIdentifier: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("country_code") val countryCode: String? = null,
    @SerializedName("e164_number") val e164Number: String? = null,
    @SerializedName("contact_type") val contactType: ContactType? = null,
    @SerializedName("platform_handle") val platformHandle: String? = null,
    @SerializedName("alpha_sender_id") val alphaSenderId: String? = null,
    @SerializedName("reported_scam_count") val reportedScamCount: Int? = null,
    @SerializedName("intelligence_match") val intelligenceMatch: IntelligenceMatch? = null
)

data class IntelligenceMatch(
    @SerializedName("is_known_fraudster") val isKnownFraudster: Boolean? = null,
    @SerializedName("associated_campaigns") val associatedCampaigns: List<String>? = null,
    @SerializedName("risk_score_from_graph") val riskScoreFromGraph: Double? = null
)

data class ContentBlock(
    @SerializedName("body") val body: String,
    @SerializedName("body_truncated") val bodyTruncated: Boolean,
    @SerializedName("character_count") val characterCount: Int,
    @SerializedName("contains_urls") val containsUrls: Boolean,
    @SerializedName("contains_attachments") val containsAttachments: Boolean,
    @SerializedName("original_length") val originalLength: Int? = null,
    @SerializedName("subject") val subject: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("language_confidence") val languageConfidence: Double? = null,
    @SerializedName("script") val script: String? = null,
    @SerializedName("word_count") val wordCount: Int? = null,
    @SerializedName("url_count") val urlCount: Int? = null,
    @SerializedName("attachment_count") val attachmentCount: Int? = null,
    @SerializedName("has_otp_pattern") val hasOtpPattern: Boolean? = null,
    @SerializedName("has_urgency_language") val hasUrgencyLanguage: Boolean? = null,
    @SerializedName("has_authority_claim") val hasAuthorityClaim: Boolean? = null,
    @SerializedName("has_financial_mention") val hasFinancialMention: Boolean? = null,
    @SerializedName("media_type") val mediaType: MediaType? = null,
    @SerializedName("call_transcript") val callTranscript: String? = null,
    @SerializedName("call_transcript_confidence") val callTranscriptConfidence: Double? = null
)

data class SmsChannelPayload(
    @SerializedName("sms_type") val smsType: SmsType,
    @SerializedName("message_parts") val messageParts: Int,
    @SerializedName("sender_number_raw") val senderNumberRaw: String? = null,
    @SerializedName("sender_number_e164") val senderNumberE164: String? = null,
    @SerializedName("alpha_sender_id") val alphaSenderId: String? = null,
    @SerializedName("sim_slot_index") val simSlotIndex: Int? = null,
    @SerializedName("carrier") val carrier: String? = null,
    @SerializedName("has_dlt_header") val hasDltHeader: Boolean? = null,
    @SerializedName("dlt_principal_entity_id") val dltPrincipalEntityId: String? = null,
    @SerializedName("dlt_template_id") val dltTemplateId: String? = null
)

data class CallChannelPayload(
    @SerializedName("call_direction") val callDirection: CallDirection,
    @SerializedName("call_state") val callState: CallState,
    @SerializedName("is_number_unknown") val isNumberUnknown: Boolean,
    @SerializedName("transcript_available") val transcriptAvailable: Boolean,
    @SerializedName("caller_number_raw") val callerNumberRaw: String? = null,
    @SerializedName("caller_number_e164") val callerNumberE164: String? = null,
    @SerializedName("duration_seconds") val durationSeconds: Int? = null,
    @SerializedName("voip_detected") val voipDetected: Boolean? = null,
    @SerializedName("call_type") val callType: CallType? = null,
    @SerializedName("carrier") val carrier: String? = null,
    @SerializedName("call_recording_reference") val callRecordingReference: String? = null
)

data class WhatsAppChannelPayload(
    @SerializedName("chat_id_hash") val chatIdHash: String,
    @SerializedName("sender_wa_id_hash") val senderWaIdHash: String,
    @SerializedName("is_group_chat") val isGroupChat: Boolean,
    @SerializedName("message_type") val messageType: WhatsAppMessageType,
    @SerializedName("capture_method") val captureMethod: CaptureMethod,
    @SerializedName("group_name") val groupName: String? = null,
    @SerializedName("group_member_count") val groupMemberCount: Int? = null,
    @SerializedName("is_forwarded") val isForwarded: Boolean? = null,
    @SerializedName("forward_chain_length") val forwardChainLength: Int? = null,
    @SerializedName("is_broadcast") val isBroadcast: Boolean? = null,
    @SerializedName("has_call_button") val hasCallButton: Boolean? = null
)

data class TelegramChannelPayload(
    @SerializedName("chat_id_hash") val chatIdHash: String,
    @SerializedName("chat_type") val chatType: TelegramChatType,
    @SerializedName("message_type") val messageType: WhatsAppMessageType,
    @SerializedName("capture_method") val captureMethod: CaptureMethod,
    @SerializedName("sender_user_id_hash") val senderUserIdHash: String? = null,
    @SerializedName("channel_name") val channelName: String? = null,
    @SerializedName("is_verified_channel") val isVerifiedChannel: Boolean? = null,
    @SerializedName("has_inline_keyboard") val hasInlineKeyboard: Boolean? = null,
    @SerializedName("bot_interaction") val botInteraction: Boolean? = null,
    @SerializedName("has_payment_button") val hasPaymentButton: Boolean? = null
)

data class GmailChannelPayload(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("from_address_hash") val fromAddressHash: String,
    @SerializedName("from_domain") val fromDomain: String,
    @SerializedName("has_html_body") val hasHtmlBody: Boolean,
    @SerializedName("thread_id") val threadId: String? = null,
    @SerializedName("from_address_raw") val fromAddressRaw: String? = null,
    @SerializedName("from_display_name") val fromDisplayName: String? = null,
    @SerializedName("reply_to_address_hash") val replyToAddressHash: String? = null,
    @SerializedName("return_path_domain") val returnPathDomain: String? = null,
    @SerializedName("subject") val subject: String? = null,
    @SerializedName("label_ids") val labelIds: List<String>? = null,
    @SerializedName("spam_label_present") val spamLabelPresent: Boolean? = null,
    @SerializedName("dkim_result") val dkimResult: EmailAuthResult? = null,
    @SerializedName("spf_result") val spfResult: EmailAuthResult? = null,
    @SerializedName("dmarc_result") val dmarcResult: EmailAuthResult? = null,
    @SerializedName("to_address_count") val toAddressCount: Int? = null,
    @SerializedName("cc_address_count") val ccAddressCount: Int? = null,
    @SerializedName("email_size_bytes") val emailSizeBytes: Int? = null,
    @SerializedName("received_at_server") val receivedAtServer: String? = null
)

data class CopilotChannelPayload(
    @SerializedName("query_type") val queryType: String? = null
)

data class UrlAnalysisItem(
    @SerializedName("url_id") val urlId: String,
    @SerializedName("raw_url") val rawUrl: String,
    @SerializedName("normalized_url") val normalizedUrl: String,
    @SerializedName("domain") val domain: String,
    @SerializedName("tld") val tld: String,
    @SerializedName("url_scheme") val urlScheme: UrlScheme,
    @SerializedName("is_shortened") val isShortened: Boolean,
    @SerializedName("is_ip_address_url") val isIpAddressUrl: Boolean,
    @SerializedName("brand_impersonation_detected") val brandImpersonationDetected: Boolean,
    @SerializedName("phishing_feed_match") val phishingFeedMatch: Boolean,
    @SerializedName("url_risk_score") val urlRiskScore: Double,
    @SerializedName("analyzed_at") val analyzedAt: String,
    @SerializedName("subdomain") val subdomain: String? = null,
    @SerializedName("final_url") val finalUrl: String? = null,
    @SerializedName("redirect_chain") val redirectChain: List<String>? = null,
    @SerializedName("redirect_depth") val redirectDepth: Int? = null,
    @SerializedName("domain_age_days") val domainAgeDays: Int? = null,
    @SerializedName("registrar") val registrar: String? = null,
    @SerializedName("registration_country") val registrationCountry: String? = null,
    @SerializedName("ip_address") val ipAddress: String? = null,
    @SerializedName("ssl_valid") val sslValid: Boolean? = null,
    @SerializedName("ssl_organization") val sslOrganization: String? = null,
    @SerializedName("impersonated_brand") val impersonatedBrand: String? = null,
    @SerializedName("phishing_feed_sources") val phishingFeedSources: List<String>? = null,
    @SerializedName("neo4j_domain_node_id") val neo4jDomainNodeId: String? = null,
    @SerializedName("url_risk_signals") val urlRiskSignals: List<String>? = null
)

data class PdfAnalysis(
    @SerializedName("page_count") val pageCount: Int? = null,
    @SerializedName("has_form_fields") val hasFormFields: Boolean? = null,
    @SerializedName("government_seal_detected") val governmentSealDetected: Boolean? = null,
    @SerializedName("fake_notice_probability") val fakeNoticeProbability: Double? = null
)

data class ApkAnalysis(
    @SerializedName("package_name") val packageName: String? = null,
    @SerializedName("declared_permissions") val declaredPermissions: List<String>? = null,
    @SerializedName("is_signed") val isSigned: Boolean? = null,
    @SerializedName("signing_certificate_hash") val signingCertificateHash: String? = null,
    @SerializedName("requests_sms_permission") val requestsSmsPermission: Boolean? = null,
    @SerializedName("requests_call_log_permission") val requestsCallLogPermission: Boolean? = null,
    @SerializedName("requests_overlay_permission") val requestsOverlayPermission: Boolean? = null
)

data class AttachmentAnalysisItem(
    @SerializedName("attachment_id") val attachmentId: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("file_extension") val fileExtension: String,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("file_size_bytes") val fileSizeBytes: Int,
    @SerializedName("sha256_hash") val sha256Hash: String,
    @SerializedName("file_category") val fileCategory: FileCategory,
    @SerializedName("is_executable") val isExecutable: Boolean,
    @SerializedName("malware_hash_match") val malwareHashMatch: Boolean,
    @SerializedName("attachment_risk_score") val attachmentRiskScore: Double,
    @SerializedName("analyzed_at") val analyzedAt: String,
    @SerializedName("md5_hash") val md5Hash: String? = null,
    @SerializedName("backend_storage_key") val backendStorageKey: String? = null,
    @SerializedName("malware_hash_sources") val malwareHashSources: List<String>? = null,
    @SerializedName("embedded_urls") val embeddedUrls: List<String>? = null,
    @SerializedName("embedded_url_count") val embeddedUrlCount: Int? = null,
    @SerializedName("has_macro") val hasMacro: Boolean? = null,
    @SerializedName("has_javascript") val hasJavascript: Boolean? = null,
    @SerializedName("pdf_analysis") val pdfAnalysis: PdfAnalysis? = null,
    @SerializedName("apk_analysis") val apkAnalysis: ApkAnalysis? = null,
    @SerializedName("attachment_risk_signals") val attachmentRiskSignals: List<String>? = null
)

data class AgentScore(
    @SerializedName("agent_id") val agentId: String,
    @SerializedName("agent_version") val agentVersion: String,
    @SerializedName("score") val score: Double,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("signals") val signals: List<String>? = null,
    @SerializedName("threat_categories") val threatCategories: List<String>? = null,
    @SerializedName("latency_ms") val latencyMs: Int? = null
)

data class RiskAssessmentBlock(
    @SerializedName("risk_level") val riskLevel: RiskLevel,
    @SerializedName("overall_score") val overallScore: Double,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("threat_categories") val threatCategories: List<String>,
    @SerializedName("is_digital_arrest_scam") val isDigitalArrestScam: Boolean,
    @SerializedName("is_authority_impersonation") val isAuthorityImpersonation: Boolean,
    @SerializedName("agent_scores") val agentScores: List<AgentScore>,
    @SerializedName("intelligence_feed_match") val intelligenceFeedMatch: Boolean,
    @SerializedName("assessed_at") val assessedAt: String,
    @SerializedName("primary_threat_category") val primaryThreatCategory: String? = null,
    @SerializedName("aggregation_method") val aggregationMethod: AggregationMethod? = null,
    @SerializedName("neo4j_context_score") val neo4jContextScore: Double? = null,
    @SerializedName("false_positive_probability") val falsePositiveProbability: Double? = null,
    @SerializedName("model_versions") val modelVersions: Map<String, String>? = null
)

data class RecommendedAction(
    @SerializedName("action_id") val actionId: String,
    @SerializedName("action_type") val actionType: ActionType,
    @SerializedName("label") val label: String,
    @SerializedName("description") val description: String,
    @SerializedName("is_primary") val isPrimary: Boolean,
    @SerializedName("deep_link") val deepLink: String? = null
)

data class EvidenceItem(
    @SerializedName("evidence_type") val evidenceType: EvidenceType,
    @SerializedName("description") val description: String,
    @SerializedName("severity") val severity: EvidenceSeverity,
    @SerializedName("source_agent") val sourceAgent: String? = null
)

data class Neo4jRelationship(
    @SerializedName("node_type") val nodeType: String,
    @SerializedName("node_id") val nodeId: String,
    @SerializedName("relationship_type") val relationshipType: String,
    @SerializedName("related_entity") val relatedEntity: String? = null
)

data class InvestigationReportBlock(
    @SerializedName("report_id") val reportId: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("detailed_explanation") val detailedExplanation: String,
    @SerializedName("what_happened") val whatHappened: String,
    @SerializedName("why_its_risky") val whyItsRisky: String,
    @SerializedName("what_to_do") val whatToDo: String,
    @SerializedName("recommended_actions") val recommendedActions: List<RecommendedAction>,
    @SerializedName("generated_at") val generatedAt: String,
    @SerializedName("evidence") val evidence: List<EvidenceItem>? = null,
    @SerializedName("neo4j_relationships") val neo4jRelationships: List<Neo4jRelationship>? = null,
    @SerializedName("language") val language: String? = null
)
