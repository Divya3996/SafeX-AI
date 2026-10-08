package com.sentinel.ai.agents

import com.sentinel.ai.agents.base.AgentInput
import com.sentinel.ai.agents.base.AgentResult
import com.sentinel.ai.core.event.ThreatEventBus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsAgentCoordinator @Inject constructor(
    private val scanRepository: com.sentinel.ai.core.data.ScanRepository
) {
    suspend fun process(sender: String, body: String, timestamp: Long): AgentResult = AgentResult(scanResult = scanRepository.analyzeMessage(body, "SMS", sender, sender, timestamp, java.util.UUID.randomUUID().toString()))

    suspend fun dispatch(input: AgentInput.Sms) {
        process(input.sender, input.body, input.timestamp)
    }
}
