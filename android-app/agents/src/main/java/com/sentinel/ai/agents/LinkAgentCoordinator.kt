package com.sentinel.ai.agents

import com.sentinel.ai.agents.base.AgentInput
import com.sentinel.ai.agents.base.AgentResult
import com.sentinel.ai.core.event.ThreatEventBus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkAgentCoordinator @Inject constructor(
    private val scanRepository: com.sentinel.ai.core.data.ScanRepository
) {
    suspend fun process(url: String): AgentResult = AgentResult(scanResult = scanRepository.scanLink(url))

    suspend fun dispatch(input: AgentInput.Link) {
        process(input.url)
    }
}
