package com.sentinel.ai.core.model

data class Alert(
    val id: String,
    val threatId: String,
    val title: String,
    val senderDisplayName: String? = null,
    val senderIdentifier: String? = null,
    val summary: String,
    val riskLevel: RiskLevel,
    val timestamp: Long,
    val isDismissed: Boolean = false,
    val isRead: Boolean = false
)
