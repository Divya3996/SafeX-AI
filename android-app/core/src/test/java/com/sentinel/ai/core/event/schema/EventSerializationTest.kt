package com.sentinel.ai.core.event.schema

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventSerializationTest {

    @Test
    fun `round trip minimal SMS base event`() {
        val original = EventTestFixtures.minimalSmsBaseEvent()
        val json = EventSchemaGson.toJson(original)
        val restored = EventSchemaGson.fromJson(json)

        assertEquals(original, restored)
        assertEquals(ValidationResult.Valid, restored.validate())
    }

    @Test
    fun `round trip communication event`() {
        val original = CommunicationEvent.from(EventTestFixtures.minimalSmsBaseEvent())
        val json = EventSchemaGson.toJsonCommunication(original)
        val restored = EventSchemaGson.fromJsonCommunication(json)

        assertEquals(original.event, restored.event)
    }

    @Test
    fun `round trip message event`() {
        val original = MessageEvent.from(EventTestFixtures.minimalSmsBaseEvent())!!
        val json = EventSchemaGson.toJsonMessage(original)
        val restored = EventSchemaGson.fromJsonMessage(json)

        assertEquals(original.event, restored.event)
    }

    @Test
    fun `round trip link event`() {
        val original = LinkEvent.from(EventTestFixtures.minimalLinkBaseEvent())!!
        val json = EventSchemaGson.toJsonLink(original)
        val restored = EventSchemaGson.fromJsonLink(json)

        assertEquals(original.event, restored.event)
        assertEquals(original.urls, restored.urls)
    }

    @Test
    fun `round trip attachment event`() {
        val original = AttachmentEvent.from(EventTestFixtures.minimalAttachmentBaseEvent())!!
        val json = EventSchemaGson.toJsonAttachment(original)
        val restored = EventSchemaGson.fromJsonAttachment(json)

        assertEquals(original.event, restored.event)
        assertEquals(original.attachments, restored.attachments)
    }

    @Test
    fun `serialized JSON uses snake_case field names`() {
        val json = EventSchemaGson.toJson(EventTestFixtures.minimalSmsBaseEvent())
        assertTrue(json.contains("\"schema_version\""))
        assertTrue(json.contains("\"event_id\""))
        assertTrue(json.contains("\"channel_payload\""))
        assertTrue(json.contains("\"sentinel.sms.received\""))
    }

    @Test
    fun `event_type serializes as dotted string`() {
        val json = EventSchemaGson.toJson(EventTestFixtures.minimalSmsBaseEvent())
        assertTrue(json.contains("\"event_type\":\"sentinel.sms.received\""))
    }
}
