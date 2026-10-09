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
    @Test fun `new provenance and coverage survive explicit save`() {
        val result = ScanResult("new", "private", riskLevel = RiskLevel.YELLOW, riskScore = 30f, explanation = "Coverage", timestamp = 0,
            provenance = InputProvenance(InputOrigin.IMPORTED_IMAGE, ReviewScope.SELECTED_LINES, selectedLineCount = 2, crop = ScreenSelection.FULL),
            coverageDetails = ScanCoverageDetails(AssessmentCoverage.LIMITED, listOf(ExtractionOutcome(ExtractionEngine.LATIN, EvidenceSourceStatus.COMPLETED, 2))),
            timings = ScanTimings(10, 20))
        assertEquals(result, result.toEntity().toScanResult())
    }
    @Test fun `old saved JSON has no invented provenance`() {
        val old = """{"id":"old","source":"scan","riskLevel":"GREEN","riskScore":0,"explanation":"old","timestamp":0}"""
        val result = com.google.gson.Gson().fromJson(old, ScanResult::class.java)
        assertNull(result.provenance); assertNull(result.coverageDetails); assertNull(result.timings)
    }
}
