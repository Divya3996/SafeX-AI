package com.sentinel.ai.agents

import com.sentinel.ai.agents.base.AgentInput
import com.sentinel.ai.agents.base.AgentResult
import com.sentinel.ai.core.event.ThreatEventBus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileAgentCoordinator @Inject constructor(
    private val scanRepository: com.sentinel.ai.core.data.ScanRepository
) {
    suspend fun process(path: String, mimeType: String): AgentResult = AgentResult(scanResult = scanRepository.scanFile(android.net.Uri.parse(path)))

    suspend fun dispatch(input: AgentInput.File) {
        process(input.path, input.mimeType)
    }
}
