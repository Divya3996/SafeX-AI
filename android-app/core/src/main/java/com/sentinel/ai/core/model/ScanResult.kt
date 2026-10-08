package com.sentinel.ai.core.model

data class ScanResult(
    val id: String,
    val source: String,
    val senderDisplayName: String? = null,
    val senderIdentifier: String? = null,
    val riskLevel: RiskLevel,
    val riskScore: Float,
    val explanation: String,
    val timestamp: Long,
    val decision: ProtectionDecision = riskLevel.toProtectionDecision(),
    val confidence: Float = 0f,
    val headline: String = decision.defaultHeadline(),
    val summary: String = explanation,
    val reasons: List<ScanReason> = emptyList(),
    val providerFindings: List<ProviderFinding> = emptyList(),
    val localEvidence: LocalEvidence? = null,
    val recommendedAction: ProtectionAction = decision.defaultAction(),
    val target: String? = null,
    val contentType: String = "link",
    val category: String = "General",
    val durationMs: Long = 0,
    val modelStatus: String = "Not used",
    val coverage: String = "Local analysis",
    val isDemo: Boolean = false,
    val guidance: String = "Verify unexpected requests through a trusted channel.",
    val linkInspections: List<LinkInspection>? = null,
    val paymentReviews: List<PaymentQrReview>? = null,
    val contextReview: ContextAnswers? = null
)

enum class ProtectionDecision {
    ALLOW,
    WARN,
    BLOCK
}

enum class ProtectionAction {
    CONTINUE,
    PROCEED_WITH_CAUTION,
    DO_NOT_CONTINUE
}

enum class EvidenceSourceStatus {
    COMPLETED,
    UNKNOWN,
    UNAVAILABLE,
    FAILED,
    TIMED_OUT
}

enum class ScanReasonSource {
    REPUTATION_PROVIDER,
    LOCAL_HEURISTIC,
    PROVIDER_STATUS
}

data class ScanReason(
    val source: ScanReasonSource,
    val sourceName: String,
    val message: String,
    val riskLevel: RiskLevel? = null
)

data class ProviderFinding(
    val providerName: String,
    val status: EvidenceSourceStatus,
    val verdict: String? = null,
    val confidence: Float = 0f,
    val reason: String
)

data class LocalFinding(
    val ruleId: String,
    val ruleName: String,
    val category: String,
    val scoreContribution: Float,
    val reason: String
)

data class LocalEvidence(
    val score: Float,
    val riskLevel: RiskLevel,
    val findings: List<LocalFinding>,
    val triggeredRuleCount: Int
)

fun RiskLevel.toProtectionDecision(): ProtectionDecision = when (this) {
    RiskLevel.GREEN -> ProtectionDecision.ALLOW
    RiskLevel.YELLOW,
    RiskLevel.RED -> ProtectionDecision.WARN
    RiskLevel.CRITICAL -> ProtectionDecision.BLOCK
}

fun ProtectionDecision.defaultHeadline(): String = when (this) {
    ProtectionDecision.ALLOW -> "No strong threat evidence detected"
    ProtectionDecision.WARN -> "Suspicious content detected"
    ProtectionDecision.BLOCK -> "Critical threat evidence detected"
}

fun ProtectionDecision.defaultAction(): ProtectionAction = when (this) {
    ProtectionDecision.ALLOW -> ProtectionAction.CONTINUE
    ProtectionDecision.WARN -> ProtectionAction.PROCEED_WITH_CAUTION
    ProtectionDecision.BLOCK -> ProtectionAction.DO_NOT_CONTINUE
}
