package com.sentinel.ai.agents.whatsapp

import com.sentinel.ai.core.event.schema.ScamRiskLevel
import org.junit.Assert.*
import org.junit.Test

class ScamRuleEngineTest {
    @Test fun `credential request with authority and pressure is critical`() {
        val result = ScamRuleEngine.evaluate("Bank support urgent share your OTP immediately", emptyList(), false)
        assertEquals(100, result.riskScore)
        assertEquals(ScamRiskLevel.CRITICAL, result.riskLevel)
    }
    @Test fun `safety advice never becomes credential theft`() {
        val result = ScamRuleEngine.evaluate("Never share your OTP or password. Your payment was successful.", emptyList(), false)
        assertTrue(result.riskScore < 30)
    }
    @Test fun `known contact cannot downgrade an explicit request`() {
        val known = ScamRuleEngine.evaluate("Send your password to recover your account", emptyList(), true)
        val unknown = ScamRuleEngine.evaluate("Send your password to recover your account", emptyList(), false)
        assertEquals(known.riskScore, unknown.riskScore)
        assertEquals(ScamRiskLevel.HIGH, known.riskLevel)
    }
    @Test fun `ordinary offers are not scam proof`() {
        assertEquals(0, ScamRuleEngine.evaluate("Limited offer on shoes", emptyList(), false).riskScore)
    }
}
