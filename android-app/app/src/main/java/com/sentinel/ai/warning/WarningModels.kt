package com.sentinel.ai.warning

import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanResult

enum class WarningSeverity {
    NONE,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class WarningUiModel(
    val title: String,
    val riskLevelLabel: String,
    val riskScore: Float,
    val reasons: List<String>,
    val severity: WarningSeverity
)

fun ScanResult.toWarningUiModel(): WarningUiModel {
    val severity = when (riskLevel) {
        RiskLevel.GREEN -> WarningSeverity.NONE
        RiskLevel.YELLOW -> WarningSeverity.MEDIUM
        RiskLevel.RED -> WarningSeverity.HIGH
        RiskLevel.CRITICAL -> WarningSeverity.CRITICAL
    }

    return WarningUiModel(
        title = "Potential Scam Detected",
        riskLevelLabel = when (riskLevel) {
            RiskLevel.GREEN -> "LOW"
            RiskLevel.YELLOW -> "MEDIUM"
            RiskLevel.RED -> "HIGH"
            RiskLevel.CRITICAL -> "CRITICAL"
        },
        riskScore = riskScore,
        reasons = explanation
            .split(';', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() },
        severity = severity
    )
}
