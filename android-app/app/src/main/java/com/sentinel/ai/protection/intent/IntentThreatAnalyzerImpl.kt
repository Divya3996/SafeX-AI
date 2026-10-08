package com.sentinel.ai.protection.intent

import com.sentinel.ai.core.event.ThreatEvent
import com.sentinel.ai.core.event.ThreatEventBus
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.protection.intent.file.FileScanner
import com.sentinel.ai.protection.intent.link.LinkScanner
import com.sentinel.ai.protection.intent.model.FilePayload
import com.sentinel.ai.protection.intent.model.IntentPayload
import com.sentinel.ai.protection.intent.model.UrlPayload
import com.sentinel.ai.protection.intent.reputation.ReputationManager
import com.sentinel.ai.protection.intent.reputation.ReputationTarget
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of IntentThreatAnalyzer that coordinates scanning of links and files,
 * and integrates with the existing Threat Engine.
 */
@Singleton
class IntentThreatAnalyzerImpl @Inject constructor(
    private val linkScanner: LinkScanner,
    private val fileScanner: FileScanner,
    private val threatEventBus: ThreatEventBus,
    private val reputationManager: ReputationManager
) : IntentThreatAnalyzer {

    override suspend fun analyze(payload: IntentPayload): ScanResult {
        return when (payload) {
            is UrlPayload -> {
                val heuristicResult = linkScanner.scan(payload.url)
                reputationManager.enrich(
                    heuristicResult = heuristicResult,
                    target = ReputationTarget.Url(payload.url)
                )
            }
            is com.sentinel.ai.protection.intent.model.TextPayload -> throw IllegalArgumentException("Use scanText for messages")
            is FilePayload -> {
                val heuristicResult = fileScanner.scan(payload.uri)
                val finalResult = reputationManager.enrich(
                    heuristicResult = heuristicResult,
                    target = null
                )
                finalResult
            }
        }
    }
}
