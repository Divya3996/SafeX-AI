package com.sentinel.ai.core.model

object DecisionPolicy {
    fun level(score: Float): RiskLevel = when {
        score >= 90 -> RiskLevel.CRITICAL
        score >= 70 -> RiskLevel.RED
        score >= 30 -> RiskLevel.YELLOW
        else -> RiskLevel.GREEN
    }
    fun canOpen(result: ScanResult): Boolean = result.contentType == "link" &&
        result.decision != ProtectionDecision.BLOCK && !result.target.isNullOrBlank()
    fun floor(level: RiskLevel): Float = when (level) {
        RiskLevel.GREEN -> 0f
        RiskLevel.YELLOW -> 30f
        RiskLevel.RED -> 70f
        RiskLevel.CRITICAL -> 90f
    }
}
