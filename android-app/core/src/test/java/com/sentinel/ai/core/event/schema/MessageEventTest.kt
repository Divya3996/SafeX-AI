package com.sentinel.ai.core.event.schema

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageEventTest {

    @Test
    fun `SMS received event is a valid MessageEvent`() {
        val message = MessageEvent.from(EventTestFixtures.minimalSmsBaseEvent())
        assertNotNull(message)
        assertEquals(ValidationResult.Valid, message!!.validate())
    }

    @Test
    fun `URL scan completed is not a MessageEvent`() {
        assertNull(MessageEvent.from(EventTestFixtures.minimalLinkBaseEvent()))
    }

    @Test
    fun `channel inconsistent with event type fails VAL-005`() {
        val event = MessageEvent.from(
            EventTestFixtures.minimalSmsBaseEvent().copy(
                channel = Channel.GMAIL,
                eventType = EventType.SMS_RECEIVED
            )
        )!!
        val result = event.validate()
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errors.any { it.contains("VAL-005") })
    }

    @Test
    fun `create builds captured message event`() {
        val message = MessageEvent.create(
            eventId = EventTestFixtures.EVENT_ID,
            eventType = EventType.WHATSAPP_MESSAGE_RECEIVED,
            channel = Channel.WHATSAPP,
            capturedAt = EventTestFixtures.CAPTURED_AT,
            submittedAt = EventTestFixtures.SUBMITTED_AT,
            deviceId = EventTestFixtures.DEVICE_ID,
            appVersion = EventTestFixtures.APP_VERSION,
            source = EventTestFixtures.minimalSource().copy(identifierType = IdentifierType.WHATSAPP_JID),
            content = EventTestFixtures.minimalContent("Hello"),
            channelPayload = ChannelPayloadData.WhatsApp(
                WhatsAppChannelPayload(
                    chatIdHash = "b94d27b9934d3e08a52e52d7da7dabfac484efe04294e576",
                    senderWaIdHash = "3c8d4e6f2a1b5c7d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4",
                    isGroupChat = false,
                    messageType = WhatsAppMessageType.TEXT,
                    captureMethod = CaptureMethod.NOTIFICATION_LISTENER
                )
            )
        )
        assertEquals(EventType.WHATSAPP_MESSAGE_RECEIVED, message.event.eventType)
        assertEquals(ValidationResult.Valid, message.validate())
    }

    private fun assertNotNull(value: Any?) {
        org.junit.Assert.assertNotNull(value)
    }
}
