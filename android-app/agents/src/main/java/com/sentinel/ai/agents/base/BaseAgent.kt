package com.sentinel.ai.agents.base

import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanResult

data class AgentResult(
    val scanResult: ScanResult?,
    val riskLevel: RiskLevel = RiskLevel.GREEN,
    val signals: List<String> = emptyList()
)

abstract class BaseAgent {
    abstract suspend fun process(input: AgentInput): AgentResult
}

sealed interface AgentInput {
    data class Sms(val sender: String, val body: String, val timestamp: Long) : AgentInput
    data class Call(val number: String, val timestamp: Long) : AgentInput
    data class Link(val url: String) : AgentInput
    data class File(val path: String, val mimeType: String) : AgentInput
    data class Message(val channel: String, val content: String, val timestamp: Long) : AgentInput
}
