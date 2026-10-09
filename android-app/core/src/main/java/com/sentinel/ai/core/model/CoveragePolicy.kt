package com.sentinel.ai.core.model

/** Incomplete evidence may increase caution, never erase threat evidence or imply a clean scan. */
object CoveragePolicy {
    fun apply(result: ScanResult, details: ScanCoverageDetails): ScanResult {
        if (details.assessment == AssessmentCoverage.COMPLETE) return result.copy(coverageDetails = details)
        val decision = if (result.decision == ProtectionDecision.ALLOW) ProtectionDecision.WARN else result.decision
        val score = maxOf(result.riskScore, 30f)
        val level = if (result.riskLevel == RiskLevel.GREEN) RiskLevel.YELLOW else result.riskLevel
        val reason = if (details.assessment == AssessmentCoverage.UNSUPPORTED)
            "This payload type cannot be fully assessed. Nothing was opened or executed."
        else "Assessment is limited. Some selected content or destinations could not be checked."
        val reasons = (result.reasons + ScanReason(ScanReasonSource.LOCAL_HEURISTIC, "Assessment coverage", reason)).distinct()
        return result.copy(coverageDetails = details, decision = decision, riskScore = score, riskLevel = level,
            headline = decision.defaultHeadline(), recommendedAction = decision.defaultAction(), reasons = reasons,
            summary = if (result.decision == ProtectionDecision.ALLOW) reason else result.summary,
            explanation = (listOf(result.explanation, reason)).distinct().joinToString("\n"))
    }
}
