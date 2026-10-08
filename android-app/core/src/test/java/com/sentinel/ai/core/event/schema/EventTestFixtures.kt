package com.sentinel.ai.core.event.schema

object EventTestFixtures {

    const val EVENT_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
    const val DEVICE_ID = "a3f8c2d1e4b57690f1a2b3c4d5e6f708"
    const val IDENTIFIER_HASH = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"
    const val CAPTURED_AT = "2026-06-23T10:15:30.123Z"
    const val SUBMITTED_AT = "2026-06-23T10:15:30.891Z"
    const val APP_VERSION = "1.0.0"

    const val SMS_BODY = "Your account will be blocked. Verify at http://bit.ly/sbi-kyc"

    fun minimalSource(): SourceBlock = SourceBlock(
        identifierHash = IDENTIFIER_HASH,
        identifierType = IdentifierType.ALPHA_SENDER_ID,
        isKnownContact = false,
        alphaSenderId = "CBISEC"
    )

    fun minimalContent(body: String = SMS_BODY): ContentBlock = ContentBlock(
        body = body,
        bodyTruncated = false,
        characterCount = body.length,
        containsUrls = body.contains("http"),
        containsAttachments = false,
        urlCount = if (body.contains("http")) 1 else null
    )

    fun minimalSmsPayload(): ChannelPayloadData.Sms = ChannelPayloadData.Sms(
        SmsChannelPayload(
            smsType = SmsType.TRANSACTIONAL,
            messageParts = 1,
            alphaSenderId = "CBISEC"
        )
    )

    fun minimalSmsBaseEvent(): BaseEvent = BaseEvent(
        schemaVersion = EventSchemaConstants.SCHEMA_VERSION,
        eventId = EVENT_ID,
        eventType = EventType.SMS_RECEIVED,
        channel = Channel.SMS,
        processingStatus = ProcessingStatus.CAPTURED,
        capturedAt = CAPTURED_AT,
        submittedAt = SUBMITTED_AT,
        deviceId = DEVICE_ID,
        appVersion = APP_VERSION,
        source = minimalSource(),
        content = minimalContent(),
        channelPayload = minimalSmsPayload(),
        ttlSeconds = EventSchemaConstants.DEFAULT_TTL_SECONDS
    )

    fun sampleUrlAnalysisItem(): UrlAnalysisItem = UrlAnalysisItem(
        urlId = "a1b2c3d4-e5f6-4890-ab12-cd34ef567890",
        rawUrl = "http://bit.ly/sbi-kyc-update",
        normalizedUrl = "http://bit.ly/sbi-kyc-update",
        domain = "bit.ly",
        tld = "ly",
        urlScheme = UrlScheme.HTTP,
        isShortened = true,
        isIpAddressUrl = false,
        brandImpersonationDetected = true,
        phishingFeedMatch = false,
        urlRiskScore = 0.94,
        analyzedAt = "2026-06-23T10:15:33.201Z",
        impersonatedBrand = "SBI"
    )

    fun sampleAttachmentAnalysisItem(): AttachmentAnalysisItem = AttachmentAnalysisItem(
        attachmentId = "b2c3d4e5-f6a7-4901-bc23-de45fa678901",
        filename = "notice.pdf",
        fileExtension = "pdf",
        mimeType = "application/pdf",
        fileSizeBytes = 48_392,
        sha256Hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        fileCategory = FileCategory.PDF,
        isExecutable = false,
        malwareHashMatch = false,
        attachmentRiskScore = 0.72,
        analyzedAt = "2026-06-23T10:15:33.500Z"
    )

    fun minimalLinkBaseEvent(): BaseEvent = minimalSmsBaseEvent().copy(
        eventType = EventType.URL_SCAN_COMPLETED,
        processingStatus = ProcessingStatus.ANALYZING,
        urls = listOf(sampleUrlAnalysisItem())
    )

    fun minimalAttachmentBaseEvent(): BaseEvent = minimalSmsBaseEvent().copy(
        eventType = EventType.FILE_SCAN_COMPLETED,
        processingStatus = ProcessingStatus.ANALYZING,
        content = minimalContent("See attached document").copy(containsAttachments = true, attachmentCount = 1),
        attachments = listOf(sampleAttachmentAnalysisItem())
    )
}
