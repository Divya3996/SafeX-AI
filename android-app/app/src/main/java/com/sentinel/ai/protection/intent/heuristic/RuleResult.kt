package com.sentinel.ai.protection.intent.heuristic

data class RuleResult(
    val triggered: Boolean,
    val scoreContribution: Float,
    val explanation: String?,
    val category: RuleCategory
)
