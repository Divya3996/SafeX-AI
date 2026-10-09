package com.sentinel.ai.ui.screens.scanner

import com.sentinel.ai.core.model.*
import org.junit.Assert.*
import org.junit.Test

class ReviewDisplayStateTest {
    private fun base() = ScanResult("review", "test", riskLevel = RiskLevel.GREEN, riskScore = 0f, explanation = "Original", timestamp = 0L, target = "https://example.com/")
    @Test fun staleHistoryCannotRemoveAWarningOrRestoreDirectOpening() {
        val reviewed = ContextReview.apply(base(), ContextAnswers(asksForSecret = true))
        val shown = ReviewDisplayState.reconcile(reviewed, base())
        assertEquals(ProtectionDecision.WARN, shown.decision); assertEquals(70f, shown.riskScore)
        assertTrue(shown.contextReview!!.asksForSecret)
    }
    @Test fun olderContextKeepsAllPreviouslyDisclosedAnswers() {
        val current = ContextReview.apply(base(), ContextAnswers(asksForSecret = true, recipientMismatch = true))
        val incoming = ContextReview.apply(base(), ContextAnswers(unexpected = true))
        val shown = ReviewDisplayState.reconcile(current, incoming)
        assertTrue(shown.contextReview!!.asksForSecret); assertTrue(shown.contextReview!!.recipientMismatch)
        assertTrue(shown.contextReview!!.unexpected); assertEquals(70f, shown.riskScore)
    }
    @Test fun anExistingBlockRemainsBlocked() {
        val blocked = base().copy(riskScore = 100f, riskLevel = RiskLevel.CRITICAL, decision = ProtectionDecision.BLOCK)
        val shown = ReviewDisplayState.reconcile(blocked, base())
        assertEquals(ProtectionDecision.BLOCK, shown.decision); assertFalse(DecisionPolicy.canOpen(shown))
    }
    @Test fun olderBlockedScoreCannotLowerTheDisplayedScore() {
        val current = base().copy(riskScore = 100f, riskLevel = RiskLevel.CRITICAL, decision = ProtectionDecision.BLOCK)
        val older = current.copy(riskScore = 90f)
        val shown = ReviewDisplayState.reconcile(current, older)
        assertEquals(ProtectionDecision.BLOCK, shown.decision)
        assertEquals(100f, shown.riskScore)
        assertEquals(RiskLevel.CRITICAL, shown.riskLevel)
    }

}
