package com.sentinel.ai.protection.intent

import com.sentinel.ai.core.model.*
import com.sentinel.ai.protection.intent.model.IntentPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LinkPipelineSafetyTest {
    private fun repository(probability: Float) = IntentScanRepository(object : IntentThreatAnalyzer {
        override suspend fun analyze(payload: IntentPayload) = ScanResult("test", "Link", riskLevel = RiskLevel.GREEN,
            riskScore = 0f, explanation = "No structural evidence", timestamp = 0L, contentType = "link")
    }, mlPredictor = { probability })

    @Test fun modelOnlyRiskCannotBlock() = runBlocking {
        val high = repository(1f).scanLink("https://example.com/")
        assertEquals(ProtectionDecision.WARN, high.decision)
        assertTrue(DecisionPolicy.canOpen(high)); assertEquals(35f, high.riskScore)
        assertEquals(ProtectionDecision.ALLOW, repository(.6f).scanLink("https://example.com/").decision)
    }
    @Test fun malformedEmbeddedLinksAreNotSilentlyIgnored() = runBlocking {
        val result = repository(0f).scanText("Read https://example.com\\@attacker.example/")
        assertEquals(ProtectionDecision.WARN, result.decision)
        assertTrue(result.reasons.any { it.message.contains("malformed") })
    }
    @Test fun additionalLinksCannotSilentlyEscapeMessageInspection() = runBlocking {
        val text = (1..9).joinToString(" ") { "https://site$it.example/" }
        val result = repository(0f).scanText(text)
        assertEquals(ProtectionDecision.WARN, result.decision)
        assertTrue(result.reasons.any { it.message.contains("additional destinations") })
    }
    @Test fun executableQrPayloadReceivesCaution() = runBlocking {
        val result = repository(0f).scanText("javascript:alert(1)")
        assertEquals(ProtectionDecision.WARN, result.decision)
        assertFalse(DecisionPolicy.canOpen(result))
    }
}
