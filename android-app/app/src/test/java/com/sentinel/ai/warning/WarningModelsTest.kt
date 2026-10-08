package com.sentinel.ai.warning

import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Test

class WarningModelsTest {

    @Test
    fun `maps scan result to warning ui model`() {
        val model = ScanResult(
            id = "1",
            source = "com.whatsapp",
            riskLevel = RiskLevel.RED,
            riskScore = 78f,
            explanation = "URL shortener detected; Urgent language detected; Unknown sender",
            timestamp = 1L
        ).toWarningUiModel()

        assertEquals("HIGH", model.riskLevelLabel)
        assertEquals(WarningSeverity.HIGH, model.severity)
        assertEquals(listOf("URL shortener detected", "Urgent language detected", "Unknown sender"), model.reasons)
    }

    @Test
    fun `green risk maps to no warning`() {
        val model = ScanResult(
            id = "1",
            source = "com.google.android.apps.messaging",
            riskLevel = RiskLevel.GREEN,
            riskScore = 10f,
            explanation = "benign",
            timestamp = 1L
        ).toWarningUiModel()

        assertEquals(WarningSeverity.NONE, model.severity)
    }
}
