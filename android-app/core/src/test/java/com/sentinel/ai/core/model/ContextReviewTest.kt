package com.sentinel.ai.core.model

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ContextReviewTest {
    private fun base(score: Float = 0f) = ScanResult("context-test", "test", riskLevel = DecisionPolicy.level(score),
        riskScore = score, explanation = "Initial", timestamp = 42, target = "https://example.com")
    @Test fun secretsAndMismatchedPaymentDetailsRaiseStrongWarning() {
        listOf(ContextAnswers(asksForSecret = true), ContextAnswers(recipientMismatch = true), ContextAnswers(amountMismatch = true)).forEach {
            val result = ContextReview.apply(base(), it)
            assertEquals(70f, result.riskScore)
            assertEquals(ProtectionDecision.WARN, result.decision)
            assertEquals(RiskLevel.RED, result.riskLevel)
            assertTrue(result.reasons.any { reason -> reason.sourceName == "user_context" })
        }
    }
    @Test fun ordinaryContextCanAddCautionButNeverIndependentlyBlocks() {
        listOf(ContextAnswers(unexpected = true), ContextAnswers(asksForPayment = true), ContextAnswers(asksToInstall = true), ContextAnswers(pressuresYou = true)).forEach {
            assertEquals(30f, ContextReview.apply(base(), it).riskScore)
        }
        assertEquals(ProtectionDecision.WARN, ContextReview.apply(base(), ContextAnswers(true, true, true, true, true, true, true)).decision)
    }
    @Test fun pressuredInstallationRaisesStrongWarning() {
        assertEquals(70f, ContextReview.apply(base(), ContextAnswers(asksToInstall = true, pressuresYou = true)).riskScore)
    }
    @Test fun reassuringAnswersCannotEraseExistingEvidence() {
        val initial = base(95f).copy(reasons = listOf(ScanReason(ScanReasonSource.REPUTATION_PROVIDER, "local", "Listed threat")))
        val result = ContextReview.apply(initial, ContextAnswers())
        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertEquals(95f, result.riskScore)
        assertEquals(initial.reasons, result.reasons)
        assertFalse(DecisionPolicy.canOpen(result))
    }
    @Test fun repeatedReviewDoesNotDuplicateReasonsOrChangeRecordIdentity() {
        val reviewed = ContextReview.apply(base(), ContextAnswers(asksForSecret = true))
        val repeated = ContextReview.apply(reviewed, ContextAnswers(asksForSecret = true))
        assertEquals(reviewed.reasons, repeated.reasons)
        assertEquals("context-test", repeated.id)
        assertEquals(42L, repeated.timestamp)
        assertEquals(reviewed.target, repeated.target)
    }
    @Test fun reviewFactsSurviveJsonHistoryAndOldRecordsStillRead() {
        val reviewed = ContextReview.apply(base(), ContextAnswers(asksForPayment = true)).copy(
            paymentReviews = PaymentQrParser.find("upi://pay?pa=shop@bank&am=50"),
            linkInspections = listOf(LinkInspection("www.example.com", "example.com", "www.example.com", true, false, false, emptyList(), emptyList())))
        assertEquals(reviewed, Gson().fromJson(Gson().toJson(reviewed), ScanResult::class.java))
        val legacy = Gson().fromJson(Gson().toJson(base()), ScanResult::class.java)
        assertNull(legacy.paymentReviews)
        assertNull(legacy.contextReview)
    }
}
