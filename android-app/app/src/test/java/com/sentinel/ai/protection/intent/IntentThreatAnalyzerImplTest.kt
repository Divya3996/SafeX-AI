package com.sentinel.ai.protection.intent

import android.net.Uri
import com.sentinel.ai.core.event.ThreatEventBus
import com.sentinel.ai.core.model.ProtectionDecision
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.protection.intent.file.FileScanner
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRiskEngine
import com.sentinel.ai.protection.intent.link.LinkProtectionAgent
import com.sentinel.ai.protection.intent.link.LinkScanner
import com.sentinel.ai.protection.intent.model.UrlPayload
import com.sentinel.ai.protection.intent.reputation.EvidenceCombiner
import com.sentinel.ai.protection.intent.reputation.ReputationEvidence
import com.sentinel.ai.protection.intent.reputation.ReputationManager
import com.sentinel.ai.protection.intent.reputation.ReputationResult
import com.sentinel.ai.protection.intent.reputation.ReputationTarget
import com.sentinel.ai.protection.intent.reputation.ReputationVerdict
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntentThreatAnalyzerImplTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `supported URL local result and target reach reputation manager`() = runTest(testDispatcher) {
        val local = scanResult(35f, RiskLevel.YELLOW)
        val final = local.copy(decision = ProtectionDecision.WARN, summary = "Combined")
        var scannedUrl: String? = null
        var receivedLocal: ScanResult? = null
        var receivedTarget: ReputationTarget? = null
        val analyzer = analyzer(
            linkScanner = object : LinkScanner {
                override suspend fun scan(url: String): ScanResult {
                    scannedUrl = url
                    return local
                }
            },
            reputationManager = object : ReputationManager {
                override suspend fun enrich(
                    heuristicResult: ScanResult,
                    target: ReputationTarget?
                ): ScanResult {
                    receivedLocal = heuristicResult
                    receivedTarget = target
                    return final
                }
            }
        )

        val result = analyzer.analyze(UrlPayload("https://example.xyz"))

        assertEquals("https://example.xyz", scannedUrl)
        assertSame(local, receivedLocal)
        assertEquals(ReputationTarget.Url("https://example.xyz"), receivedTarget)
        assertSame(final, result)
    }

    @Test
    fun `reputation evidence is combined and final block result is returned`() = runTest(testDispatcher) {
        val combiner = EvidenceCombiner()
        val local = scanResult(0f, RiskLevel.GREEN)
        var managerCalls = 0
        val analyzer = analyzer(
            linkScanner = fixedLinkScanner(local),
            reputationManager = object : ReputationManager {
                override suspend fun enrich(
                    heuristicResult: ScanResult,
                    target: ReputationTarget?
                ): ScanResult {
                    managerCalls += 1
                    return combiner.combine(
                        heuristicResult,
                        listOf(
                            ReputationEvidence.completed(
                                ReputationResult(
                                    providerName = "OpenPhish",
                                    confidence = 0.98f,
                                    reputation = ReputationVerdict.MALICIOUS,
                                    reason = "Matched feed.",
                                    timestamp = 1L
                                )
                            )
                        )
                    )
                }
            }
        )

        val result = analyzer.analyze(UrlPayload("https://malicious.example"))

        assertEquals(1, managerCalls)
        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertEquals("OpenPhish", result.providerFindings.single().providerName)
    }

    @Test
    fun `provider failure returns local-only fallback without crashing`() = runTest(testDispatcher) {
        val local = scanResult(35f, RiskLevel.YELLOW)
        val combiner = EvidenceCombiner()
        val analyzer = analyzer(
            linkScanner = fixedLinkScanner(local),
            reputationManager = object : ReputationManager {
                override suspend fun enrich(
                    heuristicResult: ScanResult,
                    target: ReputationTarget?
                ): ScanResult = combiner.combine(
                    heuristicResult,
                    listOf(ReputationEvidence.failed("OpenPhish"))
                )
            }
        )

        val result = analyzer.analyze(UrlPayload("https://example.xyz"))

        assertEquals(ProtectionDecision.WARN, result.decision)
        assertEquals(35f, result.riskScore, 0f)
        assertEquals("OpenPhish", result.providerFindings.single().providerName)
    }

    @Test
    fun `local-only URL fallback still receives a final decision`() = runTest(testDispatcher) {
        val combiner = EvidenceCombiner()
        val analyzer = analyzer(
            linkScanner = fixedLinkScanner(scanResult(0f, RiskLevel.GREEN)),
            reputationManager = object : ReputationManager {
                override suspend fun enrich(
                    heuristicResult: ScanResult,
                    target: ReputationTarget?
                ): ScanResult = combiner.combine(heuristicResult, emptyList())
            }
        )

        val result = analyzer.analyze(UrlPayload("https://example.com"))

        assertEquals(ProtectionDecision.ALLOW, result.decision)
        assertTrue(result.providerFindings.isEmpty())
    }

    @Test
    fun `invalid URL returns a controlled explainable result`() = runTest(testDispatcher) {
        val combiner = EvidenceCombiner()
        val analyzer = analyzer(
            linkScanner = LinkProtectionAgent(LinkHeuristicRiskEngine()),
            reputationManager = object : ReputationManager {
                override suspend fun enrich(
                    heuristicResult: ScanResult,
                    target: ReputationTarget?
                ): ScanResult = combiner.combine(heuristicResult, emptyList())
            }
        )

        val result = analyzer.analyze(UrlPayload("://broken"))

        assertEquals(ProtectionDecision.ALLOW, result.decision)
        assertTrue(result.riskScore in 0f..100f)
        assertNotNull(result.localEvidence)
        assertFalseSafetyClaim(result)
    }

    @Test
    fun `real local rule ids and explanations survive analyzer integration`() = runTest(testDispatcher) {
        val combiner = EvidenceCombiner()
        val analyzer = analyzer(
            linkScanner = LinkProtectionAgent(LinkHeuristicRiskEngine()),
            reputationManager = object : ReputationManager {
                override suspend fun enrich(
                    heuristicResult: ScanResult,
                    target: ReputationTarget?
                ): ScanResult = combiner.combine(heuristicResult, emptyList())
            }
        )

        val result = analyzer.analyze(
            UrlPayload("https://paypal-secure.xyz?next=https://destination.example")
        )

        val findings = result.localEvidence?.findings.orEmpty()
        assertTrue(findings.any { it.ruleId == "suspicious_tld" && it.reason == "Uses .xyz domain" })
        assertTrue(findings.any { it.ruleId == "suspicious_redirect" })
        assertTrue(result.reasons.any { it.sourceName == "suspicious_tld" })
    }

    @Test
    fun `cancellation during local scan is propagated`() = runTest(testDispatcher) {
        val analyzer = analyzer(
            linkScanner = object : LinkScanner {
                override suspend fun scan(url: String): ScanResult = awaitCancellation()
            },
            reputationManager = erroringManager()
        )
        val result = async {
            analyzer.analyze(UrlPayload("https://example.com"))
        }
        testDispatcher.scheduler.runCurrent()

        result.cancel()
        testDispatcher.scheduler.runCurrent()

        assertTrue(result.isCancelled)
    }

    @Test
    fun `cancellation during reputation lookup is propagated`() = runTest(testDispatcher) {
        val analyzer = analyzer(
            linkScanner = fixedLinkScanner(scanResult(0f, RiskLevel.GREEN)),
            reputationManager = object : ReputationManager {
                override suspend fun enrich(
                    heuristicResult: ScanResult,
                    target: ReputationTarget?
                ): ScanResult = awaitCancellation()
            }
        )
        val result = async {
            analyzer.analyze(UrlPayload("https://example.com"))
        }
        testDispatcher.scheduler.runCurrent()

        result.cancel()
        testDispatcher.scheduler.runCurrent()

        assertTrue(result.isCancelled)
    }

    private fun analyzer(
        linkScanner: LinkScanner,
        fileScanner: FileScanner = object : FileScanner {
            override suspend fun scan(uri: Uri): ScanResult = error("File scanner should not run")
        },
        reputationManager: ReputationManager
    ) = IntentThreatAnalyzerImpl(
        linkScanner = linkScanner,
        fileScanner = fileScanner,
        threatEventBus = ThreatEventBus(),
        reputationManager = reputationManager
    )

    private fun fixedLinkScanner(result: ScanResult) = object : LinkScanner {
        override suspend fun scan(url: String): ScanResult = result
    }

    private fun erroringManager() = object : ReputationManager {
        override suspend fun enrich(
            heuristicResult: ScanResult,
            target: ReputationTarget?
        ): ScanResult = error("Reputation manager should not run")
    }

    private fun scanResult(score: Float, level: RiskLevel) = ScanResult(
        id = "test-id",
        source = "Intent (Link)",
        riskLevel = level,
        riskScore = score,
        explanation = if (score == 0f) "No heuristic risk signals found." else "Local signal.",
        timestamp = 1L
    )

    private fun assertFalseSafetyClaim(result: ScanResult) {
        assertTrue(result.summary.contains("No strong threat evidence"))
        assertTrue(!result.summary.contains("safe", ignoreCase = true))
    }
}
