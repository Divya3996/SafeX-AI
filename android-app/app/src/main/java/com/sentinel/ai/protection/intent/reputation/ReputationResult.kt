package com.sentinel.ai.protection.intent.reputation

data class ReputationResult(
    val providerName: String,
    val confidence: Float,
    val reputation: ReputationVerdict,
    val reason: String,
    val timestamp: Long
)
