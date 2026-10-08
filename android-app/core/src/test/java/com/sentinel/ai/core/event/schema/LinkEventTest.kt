package com.sentinel.ai.core.event.schema

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkEventTest {

    @Test
    fun `link event requires url scan completed type`() {
        val link = LinkEvent.from(EventTestFixtures.minimalLinkBaseEvent())
        assertNotNull(link)
        assertEquals(ValidationResult.Valid, link!!.validate())
    }

    @Test
    fun `SMS event without urls is not a LinkEvent`() {
        assertNull(LinkEvent.from(EventTestFixtures.minimalSmsBaseEvent()))
    }

    @Test
    fun `create builds link event with urls enrichment`() {
        val link = LinkEvent.create(
            eventId = EventTestFixtures.EVENT_ID,
            channel = Channel.SMS,
            processingStatus = ProcessingStatus.ANALYZING,
            capturedAt = EventTestFixtures.CAPTURED_AT,
            submittedAt = EventTestFixtures.SUBMITTED_AT,
            deviceId = EventTestFixtures.DEVICE_ID,
            appVersion = EventTestFixtures.APP_VERSION,
            source = EventTestFixtures.minimalSource(),
            content = EventTestFixtures.minimalContent(),
            channelPayload = EventTestFixtures.minimalSmsPayload(),
            urls = listOf(EventTestFixtures.sampleUrlAnalysisItem())
        )
        assertEquals(EventType.URL_SCAN_COMPLETED, link.event.eventType)
        assertEquals(1, link.urls.size)
        assertEquals(ValidationResult.Valid, link.validate())
    }

    @Test
    fun `invalid url risk score fails validation`() {
        val badUrl = EventTestFixtures.sampleUrlAnalysisItem().copy(urlRiskScore = 1.5)
        val link = LinkEvent.create(
            eventId = EventTestFixtures.EVENT_ID,
            channel = Channel.SMS,
            processingStatus = ProcessingStatus.ANALYZING,
            capturedAt = EventTestFixtures.CAPTURED_AT,
            submittedAt = EventTestFixtures.SUBMITTED_AT,
            deviceId = EventTestFixtures.DEVICE_ID,
            appVersion = EventTestFixtures.APP_VERSION,
            source = EventTestFixtures.minimalSource(),
            content = EventTestFixtures.minimalContent(),
            channelPayload = EventTestFixtures.minimalSmsPayload(),
            urls = listOf(badUrl)
        )
        val result = link.validate()
        assertTrue(result is ValidationResult.Invalid)
    }

    private fun assertNotNull(value: Any?) {
        org.junit.Assert.assertNotNull(value)
    }
}
