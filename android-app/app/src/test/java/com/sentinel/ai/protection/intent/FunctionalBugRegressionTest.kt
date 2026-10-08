package com.sentinel.ai.protection.intent

import com.sentinel.ai.core.event.ThreatEvent
import com.sentinel.ai.core.event.ThreatEventBus
import com.sentinel.ai.core.model.ProtectionAction
import com.sentinel.ai.core.model.ProtectionDecision
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRiskEngine
import com.sentinel.ai.protection.intent.model.IntentPayload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionalBugRegressionTest {

    private val engine = LinkHeuristicRiskEngine()

    @Test
    fun `HTTP warns while HTTPS remains allowed`() {
        assertEquals(ProtectionDecision.WARN, engine.toScanResult("http://google.com").decision)
        assertEquals(ProtectionDecision.ALLOW, engine.toScanResult("https://google.com").decision)
    }

    @Test
    fun `embedded redirect URL reaches suspicious decision`() {
        val result = engine.toScanResult("https://example.com/?redirect=https://evil.com")

        assertEquals(45f, result.riskScore, 0f)
        assertEquals(ProtectionDecision.WARN, result.decision)
    }

    @Test
    fun `continue button follows protection decision`() {
        assertEquals("Continue", continueButtonText(ProtectionDecision.ALLOW))
        assertEquals("Continue Anyway", continueButtonText(ProtectionDecision.WARN))
        assertNull(continueButtonText(ProtectionDecision.BLOCK))
    }

    @Test
    fun `high confidence model elevates risk level and decision from ALLOW to WARN`() = runBlocking {
        val baseAnalyzer = object : IntentThreatAnalyzer {
            override suspend fun analyze(payload: IntentPayload) = ScanResult(
                id = "test-1",
                source = "Intent (Link)",
                riskLevel = RiskLevel.GREEN,
                riskScore = 0f,
                explanation = "Clean heuristics",
                timestamp = 0L,
                decision = ProtectionDecision.ALLOW
            )
        }
        val bus = ThreatEventBus()
        val repository = IntentScanRepository(
            analyzer = baseAnalyzer,
            threatEventBus = bus,
            mlPredictor = { 1.0f } // High-confidence model may warn, never independently block.
        )

        val result = repository.scanLink("https://example.com/login")

        assertEquals(35f, result.riskScore, 0.01f)
        assertEquals(RiskLevel.YELLOW, result.riskLevel)
        assertEquals(ProtectionDecision.WARN, result.decision)
        assertEquals(ProtectionAction.PROCEED_WITH_CAUTION, result.recommendedAction)
    }

    @Test
    fun `ml blending cannot downgrade BLOCK decision from reputation provider`() = runBlocking {
        val baseAnalyzer = object : IntentThreatAnalyzer {
            override suspend fun analyze(payload: IntentPayload) = ScanResult(
                id = "test-2",
                source = "Intent (Link)",
                riskLevel = RiskLevel.CRITICAL,
                riskScore = 100f,
                explanation = "Known malicious domain",
                timestamp = 0L,
                decision = ProtectionDecision.BLOCK
            )
        }
        val repository = IntentScanRepository(
            analyzer = baseAnalyzer,
            threatEventBus = ThreatEventBus(),
            mlPredictor = { 0.0f } // 0% ML -> combined = 70f
        )

        val result = repository.scanLink("https://known-phish.com")

        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
    }

    @Test
    fun `ml scan result is emitted to threat event bus with updated score and decision`() = runBlocking {
        val baseAnalyzer = object : IntentThreatAnalyzer {
            override suspend fun analyze(payload: IntentPayload) = ScanResult(
                id = "test-3",
                source = "Intent (Link)",
                riskLevel = RiskLevel.GREEN,
                riskScore = 0f,
                explanation = "Clean heuristics",
                timestamp = 0L,
                decision = ProtectionDecision.ALLOW
            )
        }
        val bus = ThreatEventBus()
        val repository = IntentScanRepository(
            analyzer = baseAnalyzer,
            threatEventBus = bus,
            mlPredictor = { 1.0f }
        )

        val waiting = async(start = CoroutineStart.UNDISPATCHED) { bus.events.first() }
        repository.scanLink("https://suspicious-site.com")

        val event = waiting.await()
        assertTrue(event is ThreatEvent.LinkThreatDetected)
        val emittedScan = (event as ThreatEvent.LinkThreatDetected).scanResult
        assertEquals(ProtectionDecision.WARN, emittedScan.decision)
        assertEquals(35f, emittedScan.riskScore, 0.01f)
    }
}
