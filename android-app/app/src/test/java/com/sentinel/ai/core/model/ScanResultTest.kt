package com.sentinel.ai.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanResultTest {

    @Test
    fun `legacy constructor remains source compatible and maps risk to decision`() {
        val green = legacyResult(RiskLevel.GREEN, 0f)
        val red = legacyResult(RiskLevel.RED, 75f)
        val critical = legacyResult(RiskLevel.CRITICAL, 90f)

        assertEquals(ProtectionDecision.ALLOW, green.decision)
        assertEquals(ProtectionDecision.WARN, red.decision)
        assertEquals(ProtectionDecision.BLOCK, critical.decision)
        assertTrue(green.providerFindings.isEmpty())
        assertEquals(null, green.localEvidence)
    }

    @Test
    fun `explicit explainable fields are retained as structured data`() {
        val local = LocalEvidence(
            score = 30f,
            riskLevel = RiskLevel.YELLOW,
            findings = listOf(
                LocalFinding("rule", "Rule", "DOMAIN", 30f, "Local reason")
            ),
            triggeredRuleCount = 1
        )
        val provider = ProviderFinding(
            providerName = "Provider",
            status = EvidenceSourceStatus.UNKNOWN,
            verdict = "UNKNOWN",
            reason = "No conclusive verdict."
        )
        val reason = ScanReason(
            source = ScanReasonSource.LOCAL_HEURISTIC,
            sourceName = "rule",
            message = "Local reason",
            riskLevel = RiskLevel.YELLOW
        )

        val result = legacyResult(RiskLevel.YELLOW, 30f).copy(
            decision = ProtectionDecision.WARN,
            confidence = 0.3f,
            headline = "Suspicious link evidence detected",
            summary = "Proceed with caution.",
            reasons = listOf(reason),
            providerFindings = listOf(provider),
            localEvidence = local,
            recommendedAction = ProtectionAction.PROCEED_WITH_CAUTION
        )

        assertEquals(local, result.localEvidence)
        assertEquals(provider, result.providerFindings.single())
        assertEquals(reason, result.reasons.single())
        assertEquals(0.3f, result.confidence, 0f)
    }

    @Test
    fun `default actions are separate from risk bands`() {
        assertEquals(ProtectionAction.CONTINUE, ProtectionDecision.ALLOW.defaultAction())
        assertEquals(ProtectionAction.PROCEED_WITH_CAUTION, ProtectionDecision.WARN.defaultAction())
        assertEquals(ProtectionAction.DO_NOT_CONTINUE, ProtectionDecision.BLOCK.defaultAction())
    }

    private fun legacyResult(level: RiskLevel, score: Float) = ScanResult(
        id = "id",
        source = "test",
        riskLevel = level,
        riskScore = score,
        explanation = "Legacy explanation",
        timestamp = 1L
    )
}
