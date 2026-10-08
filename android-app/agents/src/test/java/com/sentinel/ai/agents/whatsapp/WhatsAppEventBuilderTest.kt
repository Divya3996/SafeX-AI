package com.sentinel.ai.agents.whatsapp

import com.sentinel.ai.core.event.schema.ChannelPayloadData
import com.sentinel.ai.core.event.schema.EventSchemaConstants
import com.sentinel.ai.core.event.schema.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppEventBuilderTest {

    private val builder = WhatsAppEventBuilder()

    @Test
    fun `builds schema compliant WhatsApp message event`() {
        val event = builder.build(validRawNotification())

        assertNotNull(event)
        assertTrue(builder.validate(event!!) is ValidationResult.Valid)
        assertEquals("sentinel.whatsapp.message.received", event.event.eventType.value)
        assertEquals("WHATSAPP", event.event.channel.name)
        assertNull(event.event.source.displayName)
        assertNull(event.event.source.platformHandle)

        val payload = event.event.channelPayload as ChannelPayloadData.WhatsApp
        assertEquals(64, payload.payload.chatIdHash.length)
        assertEquals(64, payload.payload.senderWaIdHash.length)
        assertEquals("NOTIFICATION_LISTENER", payload.payload.captureMethod.name)
    }

    @Test
    fun `captures urls and truncates oversized messages`() {
        val longBody = buildString {
            append("http://example.com ")
            append("x".repeat(EventSchemaConstants.MAX_BODY_LENGTH))
        }
        val raw = validRawNotification(messageText = longBody)

        val event = builder.build(raw)

        assertNotNull(event)
        assertTrue(event!!.event.content.containsUrls)
        assertEquals(1, event.event.content.urlCount)
        assertTrue(event.event.content.bodyTruncated)
        assertEquals(EventSchemaConstants.MAX_BODY_LENGTH, event.event.content.body.length)
    }

    @Test
    fun `extracts plain and www urls into event enrichment`() {
        val raw = validRawNotification(
            messageText = "Check https://example.com and www.example.org then [www](http://www/)."
        )

        val event = builder.build(raw)

        assertNotNull(event)
        assertTrue(event!!.event.content.containsUrls)
        assertEquals(3, event.event.content.urlCount)
        assertEquals(3, event.event.urls?.size)
        assertEquals("https://example.com", event.event.urls?.get(0)?.normalizedUrl)
        assertEquals("https://www.example.org", event.event.urls?.get(1)?.normalizedUrl)
        assertEquals("http://www/", event.event.urls?.get(2)?.normalizedUrl)
    }

    @Test
    fun `leaves urls enrichment absent when no url is present`() {
        val event = builder.build(validRawNotification(messageText = "Hello there"))

        assertNotNull(event)
        assertFalse(event!!.event.content.containsUrls)
        assertNull(event.event.content.urlCount)
        assertNull(event.event.urls)
    }

    @Test
    fun `returns null when required sender or message is missing`() {
        assertNull(builder.build(validRawNotification(senderDisplayName = null)))
        assertNull(builder.build(validRawNotification(messageText = null)))
    }

    @Test
    fun `preserves resolved known contact status in the event`() {
        val event = builder.build(validRawNotification(), isKnownContact = true)

        assertNotNull(event)
        assertTrue(event!!.event.source.isKnownContact)
    }

    @Test
    fun `sets group name to null for privacy mode compliance`() {
        val event = builder.build(
            validRawNotification(
                conversationTitle = "Family",
                isGroupChat = true,
                groupName = "Family"
            )
        )

        val payload = event!!.event.channelPayload as ChannelPayloadData.WhatsApp
        assertTrue(payload.payload.isGroupChat)
        assertFalse(payload.payload.groupName != null)
    }

    private fun validRawNotification(
        senderDisplayName: String? = "John Doe",
        messageText: String? = "Transfer rs 5000 now http://example.com",
        conversationTitle: String? = null,
        isGroupChat: Boolean = false,
        groupName: String? = null
    ) = WhatsAppRawNotificationData(
        notificationKey = "wa-key",
        packageName = "com.whatsapp",
        senderDisplayName = senderDisplayName,
        messageText = messageText,
        subText = null,
        conversationTitle = conversationTitle,
        isGroupChat = isGroupChat,
        groupName = groupName,
        isForwarded = false,
        forwardChainLength = null,
        actionLabels = listOf("Reply"),
        hasCallButton = false,
        capturedAtMs = 1_719_218_400_000L
    )
}
