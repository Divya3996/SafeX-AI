package com.sentinel.ai.core.event.schema

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationEventTest {

    @Test
    fun `create wraps valid base event`() {
        val communication = CommunicationEvent.create(
            eventId = EventTestFixtures.EVENT_ID,
            eventType = EventType.SMS_RECEIVED,
            channel = Channel.SMS,
            processingStatus = ProcessingStatus.CAPTURED,
            capturedAt = EventTestFixtures.CAPTURED_AT,
            submittedAt = EventTestFixtures.SUBMITTED_AT,
            deviceId = EventTestFixtures.DEVICE_ID,
            appVersion = EventTestFixtures.APP_VERSION,
            source = EventTestFixtures.minimalSource(),
            content = EventTestFixtures.minimalContent(),
            channelPayload = EventTestFixtures.minimalSmsPayload()
        )
        assertEquals(ValidationResult.Valid, communication.validate())
    }

    @Test
    fun `from converts base event`() {
        val base = EventTestFixtures.minimalSmsBaseEvent()
        val communication = CommunicationEvent.from(base)
        assertEquals(base, communication.toBaseEvent())
    }

    @Test
    fun `invalid ttl fails validation`() {
        val event = CommunicationEvent.from(
            EventTestFixtures.minimalSmsBaseEvent().copy(ttlSeconds = 2)
        )
        val result = event.validate()
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errors.any { it.contains("VAL-012") })
    }
}
