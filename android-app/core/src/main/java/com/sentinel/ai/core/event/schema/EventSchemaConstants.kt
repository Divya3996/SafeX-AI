package com.sentinel.ai.core.event.schema

sealed class ChannelPayloadData {
    data class Sms(val payload: SmsChannelPayload) : ChannelPayloadData()
    data class Call(val payload: CallChannelPayload) : ChannelPayloadData()
    data class WhatsApp(val payload: WhatsAppChannelPayload) : ChannelPayloadData()
    data class Telegram(val payload: TelegramChannelPayload) : ChannelPayloadData()
    data class Gmail(val payload: GmailChannelPayload) : ChannelPayloadData()
    data class Copilot(val payload: CopilotChannelPayload = CopilotChannelPayload()) : ChannelPayloadData()
}

object EventSchemaConstants {
    const val SCHEMA_VERSION = "1.0.0"
    const val DEFAULT_TTL_SECONDS = 30
    const val MAX_BODY_LENGTH = 50_000
    const val MAX_ATTACHMENT_SIZE_BYTES = 104_857_600

    val RECOGNIZED_SCHEMA_VERSIONS = setOf(SCHEMA_VERSION)

    val MESSAGE_EVENT_TYPES = setOf(
        EventType.SMS_RECEIVED,
        EventType.CALL_INCOMING,
        EventType.CALL_ENDED,
        EventType.WHATSAPP_MESSAGE_RECEIVED,
        EventType.WHATSAPP_FILE_SHARED,
        EventType.TELEGRAM_MESSAGE_RECEIVED,
        EventType.TELEGRAM_FILE_SHARED,
        EventType.EMAIL_RECEIVED,
        EventType.COPILOT_QUERY
    )

    val CHANNEL_EVENT_TYPE_MAP: Map<Channel, Set<EventType>> = mapOf(
        Channel.SMS to setOf(EventType.SMS_RECEIVED),
        Channel.CALL to setOf(EventType.CALL_INCOMING, EventType.CALL_ENDED),
        Channel.WHATSAPP to setOf(EventType.WHATSAPP_MESSAGE_RECEIVED, EventType.WHATSAPP_FILE_SHARED),
        Channel.TELEGRAM to setOf(EventType.TELEGRAM_MESSAGE_RECEIVED, EventType.TELEGRAM_FILE_SHARED),
        Channel.GMAIL to setOf(EventType.EMAIL_RECEIVED),
        Channel.COPILOT to setOf(EventType.COPILOT_QUERY)
    )

    val RISK_LEVEL_SCORE_RANGES: Map<RiskLevel, ClosedFloatingPointRange<Double>> = mapOf(
        RiskLevel.GREEN to 0.0..0.24,
        RiskLevel.YELLOW to 0.25..0.59,
        RiskLevel.RED to 0.60..0.84,
        RiskLevel.CRITICAL to 0.85..1.0
    )
}
