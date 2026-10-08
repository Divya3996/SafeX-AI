package com.sentinel.ai.core.model

data class Threat(
    val id: String,
    val source: String,
    val senderDisplayName: String? = null,
    val senderIdentifier: String? = null,
    val content: String,
    val riskLevel: RiskLevel,
    val riskScore: Float,
    val explanation: String,
    val recommendation: String,
    val timestamp: Long
)
