package com.sentinel.ai.core.event.schema

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseEventTest {

    @Test
    fun `minimal SMS event passes validation`() {
        val event = EventTestFixtures.minimalSmsBaseEvent()
        assertEquals(ValidationResult.Valid, event.validate())
    }

    @Test
    fun `invalid character count fails VAL-006`() {
        val event = EventTestFixtures.minimalSmsBaseEvent().copy(
            content = EventTestFixtures.minimalContent().copy(characterCount = 999)
        )
        val result = event.validate()
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errors.any { it.contains("VAL-006") })
    }

    @Test
    fun `invalid event_id fails VAL-001`() {
        val event = EventTestFixtures.minimalSmsBaseEvent().copy(eventId = "not-a-uuid")
        val result = event.validate()
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errors.any { it.contains("VAL-001") })
    }

    @Test
    fun `captured_at after submitted_at fails VAL-002`() {
        val event = EventTestFixtures.minimalSmsBaseEvent().copy(
            capturedAt = "2026-06-23T10:16:00.000Z",
            submittedAt = "2026-06-23T10:15:00.000Z"
        )
        val result = event.validate()
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errors.any { it.contains("VAL-002") })
    }

    @Test
    fun `channel payload type mismatch fails validation`() {
        val event = EventTestFixtures.minimalSmsBaseEvent().copy(
            channelPayload = ChannelPayloadData.Call(
                CallChannelPayload(
                    callDirection = CallDirection.INBOUND,
                    callState = CallState.RINGING,
                    isNumberUnknown = true,
                    transcriptAvailable = false
                )
            )
        )
        val result = event.validate()
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errors.any { it.contains("SMS payload") })
    }
}
