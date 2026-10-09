package com.sentinel.ai.core.model

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExtractionCoordinatorTest {
    @Test fun `QR and native script survive a Latin recognizer failure`() = runBlocking {
        val result = ExtractionCoordinator.collect(listOf(
            ExtractionEngine.LATIN to { throw IllegalStateException("unavailable") },
            ExtractionEngine.GUJARATI to { EngineContent(listOf(ExtractedLine("ઓટીપી મોકલો", null, "gujarati"))) },
            ExtractionEngine.QR to { EngineContent(qr = listOf("https://example.com")) }))
        assertEquals("ઓટીપી મોકલો", result.text)
        assertEquals(listOf("https://example.com"), result.qrCodes)
        assertTrue(result.limited)
        assertEquals(EvidenceSourceStatus.FAILED, result.outcomes.first().status)
    }
    @Test fun `text survives QR timeout and low quality reading remains explicit`() = runBlocking {
        val result = ExtractionCoordinator.collect(listOf(
            ExtractionEngine.QR to { throw ExtractionTimeoutException() },
            ExtractionEngine.LATIN to { EngineContent(listOf(ExtractedLine("Readable message", null, "latin"))) },
            ExtractionEngine.GUJARATI to { EngineContent(qualityLimited = true) }))
        assertEquals("Readable message", result.text)
        assertEquals(EvidenceSourceStatus.TIMED_OUT, result.outcomes.first().status)
        assertTrue(result.outcomes.last().qualityLimited)
        assertFalse(result.partial) // Bounds and recognizer coverage remain distinct.
        assertTrue(result.limited)
    }
}
