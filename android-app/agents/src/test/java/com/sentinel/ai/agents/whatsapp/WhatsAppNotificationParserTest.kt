package com.sentinel.ai.agents.whatsapp

import com.sentinel.ai.agents.registry.SupportedAppRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppNotificationParserTest {

    private val parser = WhatsAppNotificationParser(SupportedAppRegistry())

    @Test
    fun `parses basic WhatsApp message fields`() {
        val parsed = parser.parse(
            WhatsAppNotificationSnapshot(
                packageName = "com.whatsapp",
                notificationKey = "wa-key",
                sender = "John Doe",
                message = "Hello there",
                timestampMs = 1_719_218_400_000L
            )
        )

        assertNotNull(parsed)
        assertEquals("John Doe", parsed?.senderDisplayName)
        assertEquals("Hello there", parsed?.messageText)
        assertFalse(parsed?.isGroupChat ?: true)
        assertFalse(parsed?.hasCallButton ?: true)
    }

    @Test
    fun `prefers conversation metadata for group and forwarded detection`() {
        val parsed = parser.parse(
            WhatsAppNotificationSnapshot(
                packageName = "com.whatsapp",
                notificationKey = "wa-group",
                sender = "Family Group",
                message = "Forwarded many times\nPay now",
                timestampMs = 1_719_218_400_000L,
                conversationTitle = "Family",
                subText = "Alice @ Family",
                actionLabels = listOf("Reply", "Video Call")
            )
        )

        assertNotNull(parsed)
        assertTrue(parsed?.isGroupChat == true)
        assertEquals("Family", parsed?.groupName)
        assertTrue(parsed?.isForwarded == true)
        assertEquals(5, parsed?.forwardChainLength)
        assertTrue(parsed?.hasCallButton == true)
    }
}
