package com.sentinel.ai.protection.intent.heuristic

import com.sentinel.ai.core.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileHeuristicRiskEngineTest {

    private val engine = FileHeuristicRiskEngine()

    @Test
    fun simpleDocumentReturnsGreenRisk() {
        val analysis = engine.analyze("statement.pdf")

        assertEquals(RiskLevel.GREEN, analysis.riskLevel)
        assertEquals(0, analysis.triggeredRuleCount)
    }

    @Test
    fun fakeDocumentExecutableReturnsRedRisk() {
        val analysis = engine.analyze("invoice.pdf.exe")

        assertTrue(analysis.score >= 70f)
        assertTrue(analysis.ruleResults.any { it.triggered && it.category == RuleCategory.FILE })
    }
}
