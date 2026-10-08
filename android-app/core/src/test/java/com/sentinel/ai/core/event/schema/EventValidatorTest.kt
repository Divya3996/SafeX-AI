package com.sentinel.ai.core.event.schema

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventValidatorTest {

    @Test
    fun `privacy mode strips PII fields validation`() {
        val event = EventTestFixtures.minimalSmsBaseEvent().copy(
            source = EventTestFixtures.minimalSource().copy(
                rawIdentifier = "+919876543210",
                displayName = "Scammer"
            )
        )
        val result = EventValidator.validatePrivacyMode(event, privacyMode = true)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errors.any { it.contains("PRI-001") })
    }

    @Test
    fun `privacy mode passes when PII absent`() {
        val event = EventTestFixtures.minimalSmsBaseEvent()
        assertEquals(ValidationResult.Valid, EventValidator.validatePrivacyMode(event, privacyMode = true))
    }

    @Test
    fun `risk assessment score range validation VAL-013`() {
        val assessment = RiskAssessmentBlock(
            riskLevel = RiskLevel.GREEN,
            overallScore = 0.96,
            confidence = 0.9,
            threatCategories = listOf("PHISHING"),
            isDigitalArrestScam = false,
            isAuthorityImpersonation = false,
            agentScores = listOf(
                AgentScore(
                    agentId = "link_agent",
                    agentVersion = "1.0.0",
                    score = 0.96,
                    confidence = 0.9
                )
            ),
            intelligenceFeedMatch = false,
            assessedAt = "2026-06-23T10:15:34.005Z"
        )
        val event = EventTestFixtures.minimalSmsBaseEvent().copy(riskAssessment = assessment)
        val result = event.validate()
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errors.any { it.contains("VAL-013") })
    }

    @Test
    fun `investigation report requires exactly one primary action VAL-014`() {
        val report = InvestigationReportBlock(
            reportId = "c1d2e3f4-a5b6-4890-8def-123456789abc",
            summary = "Scam detected",
            detailedExplanation = "This message impersonates a government agency.",
            whatHappened = "You received a fraudulent SMS.",
            whyItsRisky = "It requests immediate payment.",
            whatToDo = "Do not respond or pay.",
            recommendedActions = listOf(
                RecommendedAction(
                    actionId = "block-1",
                    actionType = ActionType.BLOCK_SENDER,
                    label = "Block Sender",
                    description = "Stop further messages.",
                    isPrimary = false
                ),
                RecommendedAction(
                    actionId = "report-1",
                    actionType = ActionType.REPORT_NCCRP,
                    label = "Report",
                    description = "Report to authorities.",
                    isPrimary = false
                )
            ),
            generatedAt = "2026-06-23T10:15:35.000Z"
        )
        val event = EventTestFixtures.minimalSmsBaseEvent().copy(investigationReport = report)
        val result = event.validate()
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errors.any { it.contains("VAL-014") })
    }
}
