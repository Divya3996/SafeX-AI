package com.sentinel.ai.protection.intent.heuristic

import com.sentinel.ai.core.model.RiskLevel

internal fun Float.toRiskLevel(): RiskLevel = when {
    this >= 90f -> RiskLevel.CRITICAL
    this >= 70f -> RiskLevel.RED
    this >= 30f -> RiskLevel.YELLOW
    else -> RiskLevel.GREEN
}
