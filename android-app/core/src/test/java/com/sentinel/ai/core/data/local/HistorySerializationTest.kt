package com.sentinel.ai.core.data.local

import com.sentinel.ai.core.model.*
import org.junit.Assert.*
import org.junit.Test

class HistorySerializationTest {
    @Test fun `complete evidence survives storage round trip`() {
        val result = ScanResult("a", "sample", riskLevel = RiskLevel.YELLOW, riskScore = 35f, explanation = "test", timestamp = 42,
            decision = ProtectionDecision.BLOCK, target = "https://example.com", reasons = listOf(ScanReason(ScanReasonSource.LOCAL_HEURISTIC, "model", "Reason")),
            category = "Credential theft", modelStatus = "Local model", durationMs = 123, contentType = "message", isDemo = true)
        assertEquals(result, result.toEntity().toScanResult())
    }
}
