package com.sentinel.ai.protection.intent

import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.model.*
import com.sentinel.ai.protection.intent.model.IntentPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class PrivateAnalysisTest {
    private val repository = IntentScanRepository(object : IntentThreatAnalyzer {
        override suspend fun analyze(payload: IntentPayload) = ScanResult(UUID.randomUUID().toString(), "private", riskLevel = RiskLevel.GREEN,
            riskScore = 0f, explanation = "Base", timestamp = 0L)
    }, mlPredictor = { 0f })
    @Test fun privateLinkRetainsDomainChecksWithoutSaving() = runBlocking {
        val before = ThreatJournal.scanResults.value.map { it.id }
        val result = repository.analyzeLinkPrivately("https://paypal.com@other.example/login")
        assertEquals("other.example", result.linkInspections!!.single().host)
        assertEquals(before, ThreatJournal.scanResults.value.map { it.id })
        assertFalse(ThreatJournal.scanResults.value.any { it.id == result.id })
    }
    @Test fun privateMessageKeepsScamContextAndLeavesHistoryUntouched() = runBlocking {
        val before = ThreatJournal.scanResults.value.map { it.id }
        val result = repository.analyzeTextPrivately("Urgent! Share your OTP now to avoid account suspension. https://example.com")
        assertNotEquals(ProtectionDecision.ALLOW, result.decision)
        assertTrue(result.linkInspections.orEmpty().isNotEmpty())
        assertEquals(before, ThreatJournal.scanResults.value.map { it.id })
    }
    @Test fun privatePaymentReviewDoesNotLaunchOrSave() = runBlocking {
        val before = ThreatJournal.scanResults.value.map { it.id }
        val result = repository.analyzeQrPrivately("upi://pay?pa=shop@bank&am=50")
        assertEquals("shop@bank", result.paymentReviews!!.single().address)
        assertFalse(DecisionPolicy.canOpen(result))
        assertEquals(before, ThreatJournal.scanResults.value.map { it.id })
    }
    @Test(expected = IllegalArgumentException::class) fun privateOversizedMessageFailsWithoutTruncation() = runBlocking { repository.analyzeTextPrivately("x".repeat(12001)); Unit }
    @Test(expected = IllegalArgumentException::class) fun executableUrlCannotBecomeBrowserAction() = runBlocking { repository.analyzeLinkPrivately("javascript:alert(1)"); Unit }
}
