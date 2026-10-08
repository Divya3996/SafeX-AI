package com.sentinel.ai.agents

import com.sentinel.ai.agents.base.AgentInput
import com.sentinel.ai.agents.base.AgentResult
import com.sentinel.ai.core.event.ThreatEventBus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageAgentCoordinator @Inject constructor(
    private val scanRepository: com.sentinel.ai.core.data.ScanRepository
) {
    suspend fun process(channel: String, content: String, timestamp: Long): AgentResult = AgentResult(scanResult = scanRepository.analyzeMessage(content, channel, null, null, timestamp, java.util.UUID.randomUUID().toString()))

    suspend fun dispatch(input: AgentInput.Message) {
        process(input.channel, input.content, input.timestamp)
    }
}
