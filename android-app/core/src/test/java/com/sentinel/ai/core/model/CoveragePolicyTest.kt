package com.sentinel.ai.core.model

import org.junit.Assert.*
import org.junit.Test

class CoveragePolicyTest {
    private fun sample(decision: ProtectionDecision = ProtectionDecision.ALLOW) = ScanResult("x", "private",
        riskLevel = if (decision == ProtectionDecision.BLOCK) RiskLevel.CRITICAL else RiskLevel.GREEN,
        riskScore = if (decision == ProtectionDecision.BLOCK) 90f else 0f, explanation = "Evidence", timestamp = 0, decision = decision)
    @Test fun `incomplete recognizer prevents a clean assessment`() {
        val result = CoveragePolicy.apply(sample(), ScanCoverageDetails(AssessmentCoverage.LIMITED,
            listOf(ExtractionOutcome(ExtractionEngine.GUJARATI, EvidenceSourceStatus.FAILED))))
        assertEquals(ProtectionDecision.WARN, result.decision)
        assertEquals(RiskLevel.YELLOW, result.riskLevel)
        assertEquals(ProtectionAction.PROCEED_WITH_CAUTION, result.recommendedAction)
        assertTrue(result.summary.contains("limited"))
    }
    @Test fun `missing coverage never weakens existing threat evidence`() {
        val result = CoveragePolicy.apply(sample(ProtectionDecision.BLOCK), ScanCoverageDetails(AssessmentCoverage.UNSUPPORTED))
        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertEquals(90f, result.riskScore)
        assertEquals(ProtectionAction.DO_NOT_CONTINUE, result.recommendedAction)
    }
    @Test fun `action QR payloads are identified separately from web and payment`() {
        for (value in listOf("WIFI:T:WPA;S:demo;P:test;;", "BEGIN:VCARD\nN:Test\nEND:VCARD", "intent://app", "tel:123", "mailto:user@example.com", "smsto:123:hello"))
            assertTrue(QrPayloadClassifier.unsupported(value))
        for (value in listOf("https://example.com", "upi://pay?pa=shop@bank", "Ordinary text")) assertFalse(QrPayloadClassifier.unsupported(value))
    }
}
