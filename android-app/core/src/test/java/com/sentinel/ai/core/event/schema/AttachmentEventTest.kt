package com.sentinel.ai.core.event.schema

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentEventTest {

    @Test
    fun `attachment event requires file scan completed type`() {
        val attachment = AttachmentEvent.from(EventTestFixtures.minimalAttachmentBaseEvent())
        assertNotNull(attachment)
        assertEquals(ValidationResult.Valid, attachment!!.validate())
    }

    @Test
    fun `SMS event without attachments is not an AttachmentEvent`() {
        assertNull(AttachmentEvent.from(EventTestFixtures.minimalSmsBaseEvent()))
    }

    @Test
    fun `create builds attachment event with enrichment`() {
        val attachment = AttachmentEvent.create(
            eventId = EventTestFixtures.EVENT_ID,
            channel = Channel.GMAIL,
            processingStatus = ProcessingStatus.ANALYZING,
            capturedAt = EventTestFixtures.CAPTURED_AT,
            submittedAt = EventTestFixtures.SUBMITTED_AT,
            deviceId = EventTestFixtures.DEVICE_ID,
            appVersion = EventTestFixtures.APP_VERSION,
            source = EventTestFixtures.minimalSource().copy(identifierType = IdentifierType.EMAIL_ADDRESS),
            content = EventTestFixtures.minimalContent("Invoice attached").copy(
                containsAttachments = true,
                attachmentCount = 1
            ),
            channelPayload = ChannelPayloadData.Gmail(
                GmailChannelPayload(
                    messageId = "<test@gmail.com>",
                    fromAddressHash = EventTestFixtures.IDENTIFIER_HASH,
                    fromDomain = "example.com",
                    hasHtmlBody = true
                )
            ),
            attachments = listOf(EventTestFixtures.sampleAttachmentAnalysisItem())
        )
        assertEquals(EventType.FILE_SCAN_COMPLETED, attachment.event.eventType)
        assertEquals(1, attachment.attachments.size)
        assertEquals(ValidationResult.Valid, attachment.validate())
    }

    @Test
    fun `invalid sha256 hash fails VAL-015`() {
        val badAttachment = EventTestFixtures.sampleAttachmentAnalysisItem().copy(sha256Hash = "abc")
        val attachment = AttachmentEvent.create(
            eventId = EventTestFixtures.EVENT_ID,
            channel = Channel.SMS,
            processingStatus = ProcessingStatus.ANALYZING,
            capturedAt = EventTestFixtures.CAPTURED_AT,
            submittedAt = EventTestFixtures.SUBMITTED_AT,
            deviceId = EventTestFixtures.DEVICE_ID,
            appVersion = EventTestFixtures.APP_VERSION,
            source = EventTestFixtures.minimalSource(),
            content = EventTestFixtures.minimalContent().copy(
                containsAttachments = true,
                attachmentCount = 1
            ),
            channelPayload = EventTestFixtures.minimalSmsPayload(),
            attachments = listOf(badAttachment)
        )
        val result = attachment.validate()
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errors.any { it.contains("VAL-015") })
    }

    private fun assertNotNull(value: Any?) {
        org.junit.Assert.assertNotNull(value)
    }
}
