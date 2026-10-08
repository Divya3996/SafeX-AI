package com.sentinel.ai.agents.whatsapp

import com.sentinel.ai.core.event.schema.ScamRiskLevel

data class ScamRuleResult(
    val riskScore: Int,
    val riskLevel: ScamRiskLevel,
    val explanations: List<String>
)
