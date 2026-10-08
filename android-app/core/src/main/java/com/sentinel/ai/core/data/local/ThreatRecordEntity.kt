package com.sentinel.ai.core.data.local

import androidx.room.Entity
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.core.model.Threat

@Entity(
    tableName = "threat_records",
    primaryKeys = ["id", "recordType"]
)
data class ThreatRecordEntity(
    val id: String,
    val recordType: String,
    val source: String,
    val senderDisplayName: String?,
    val senderIdentifier: String?,
    val content: String?,
    val riskLevel: String,
    val riskScore: Float,
    val explanation: String,
    val recommendation: String?,
    val timestamp: Long,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val resultJson: String? = null
) {
    companion object {
        const val TYPE_SCAN_RESULT = "SCAN_RESULT"
        const val TYPE_THREAT = "THREAT"
    }
}

internal fun ScanResult.toEntity(): ThreatRecordEntity {
    return ThreatRecordEntity(
        id = id,
        recordType = ThreatRecordEntity.TYPE_SCAN_RESULT,
        source = source,
        senderDisplayName = senderDisplayName,
        senderIdentifier = senderIdentifier,
        content = target,
        riskLevel = riskLevel.name,
        riskScore = riskScore,
        explanation = explanation,
        recommendation = null,
        timestamp = timestamp,
        resultJson = com.google.gson.Gson().toJson(this)
    )
}

internal fun Threat.toEntity(): ThreatRecordEntity {
    return ThreatRecordEntity(
        id = id,
        recordType = ThreatRecordEntity.TYPE_THREAT,
        source = source,
        senderDisplayName = senderDisplayName,
        senderIdentifier = senderIdentifier,
        content = content,
        riskLevel = riskLevel.name,
        riskScore = riskScore,
        explanation = explanation,
        recommendation = recommendation,
        timestamp = timestamp
    )
}

internal fun ThreatRecordEntity.toScanResult(): ScanResult {
    resultJson?.let { json ->
        runCatching { com.google.gson.Gson().fromJson(json, ScanResult::class.java) }.getOrNull()?.let { return it }
    }
    return ScanResult(
        id = id,
        source = source,
        target = content,
        senderDisplayName = senderDisplayName,
        senderIdentifier = senderIdentifier,
        riskLevel = RiskLevel.valueOf(riskLevel),
        riskScore = riskScore,
        explanation = explanation,
        timestamp = timestamp
    )
}

internal fun ThreatRecordEntity.toThreat(): Threat {
    val resolvedRiskLevel = RiskLevel.valueOf(riskLevel)
    return Threat(
        id = id,
        source = source,
        senderDisplayName = senderDisplayName,
        senderIdentifier = senderIdentifier,
        content = content ?: explanation,
        riskLevel = resolvedRiskLevel,
        riskScore = riskScore,
        explanation = explanation,
        recommendation = recommendation ?: defaultRecommendation(resolvedRiskLevel),
        timestamp = timestamp
    )
}

private fun defaultRecommendation(riskLevel: RiskLevel): String {
    return when (riskLevel) {
        RiskLevel.GREEN -> "No immediate action is required."
        RiskLevel.YELLOW -> "Verify the sender through a trusted channel before responding."
        RiskLevel.RED -> "Do not engage with the message and block the sender if needed."
        RiskLevel.CRITICAL -> "Block the sender, preserve evidence, and report the threat immediately."
    }
}
