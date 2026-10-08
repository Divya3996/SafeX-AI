package com.sentinel.ai.core.event.schema

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.lang.reflect.Type

object EventSchemaGson {

    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(EventType::class.java, EventTypeAdapter())
        .registerTypeAdapter(UrlScheme::class.java, UrlSchemeAdapter())
        .registerTypeAdapter(BaseEvent::class.java, BaseEventAdapter())
        .registerTypeAdapter(ChannelPayloadData::class.java, ChannelPayloadAdapter())
        .create()

    fun toJson(event: BaseEvent): String = gson.toJson(event)

    fun fromJson(json: String): BaseEvent = gson.fromJson(json, BaseEvent::class.java)

    fun toJsonCommunication(event: CommunicationEvent): String = toJson(event.event)

    fun fromJsonCommunication(json: String): CommunicationEvent =
        CommunicationEvent.from(fromJson(json))

    fun toJsonMessage(event: MessageEvent): String = toJson(event.event)

    fun fromJsonMessage(json: String): MessageEvent =
        MessageEvent.from(fromJson(json))
            ?: throw JsonParseException("JSON is not a valid MessageEvent")

    fun toJsonLink(event: LinkEvent): String = toJson(event.event)

    fun fromJsonLink(json: String): LinkEvent =
        LinkEvent.from(fromJson(json))
            ?: throw JsonParseException("JSON is not a valid LinkEvent")

    fun toJsonAttachment(event: AttachmentEvent): String = toJson(event.event)

    fun fromJsonAttachment(json: String): AttachmentEvent =
        AttachmentEvent.from(fromJson(json))
            ?: throw JsonParseException("JSON is not a valid AttachmentEvent")
}

private class EventTypeAdapter : TypeAdapter<EventType>() {
    override fun write(out: JsonWriter, value: EventType?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.value)
        }
    }

    override fun read(`in`: JsonReader): EventType? {
        val raw = `in`.nextString()
        return EventType.fromValue(raw)
            ?: throw JsonParseException("Unknown event_type: $raw")
    }
}

private class UrlSchemeAdapter : TypeAdapter<UrlScheme>() {
    override fun write(out: JsonWriter, value: UrlScheme?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.jsonValue)
        }
    }

    override fun read(`in`: JsonReader): UrlScheme? {
        return UrlScheme.fromJson(`in`.nextString())
    }
}

private class ChannelPayloadAdapter : JsonSerializer<ChannelPayloadData>, JsonDeserializer<ChannelPayloadData> {
    override fun serialize(
        src: ChannelPayloadData?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement = when (src) {
        null -> com.google.gson.JsonNull.INSTANCE
        is ChannelPayloadData.Sms -> context!!.serialize(src.payload)
        is ChannelPayloadData.Call -> context!!.serialize(src.payload)
        is ChannelPayloadData.WhatsApp -> context!!.serialize(src.payload)
        is ChannelPayloadData.Telegram -> context!!.serialize(src.payload)
        is ChannelPayloadData.Gmail -> context!!.serialize(src.payload)
        is ChannelPayloadData.Copilot -> context!!.serialize(src.payload)
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ChannelPayloadData = throw JsonParseException(
        "ChannelPayloadData must be deserialized via BaseEventAdapter"
    )
}

private class BaseEventAdapter : JsonSerializer<BaseEvent>, JsonDeserializer<BaseEvent> {
    override fun serialize(
        src: BaseEvent?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        if (src == null) return com.google.gson.JsonNull.INSTANCE
        val baseGson = GsonBuilder()
            .registerTypeAdapter(EventType::class.java, EventTypeAdapter())
            .registerTypeAdapter(UrlScheme::class.java, UrlSchemeAdapter())
            .create()
        val tree = baseGson.toJsonTree(src).asJsonObject
        tree.add("channel_payload", context!!.serialize(src.channelPayload, ChannelPayloadData::class.java))
        return tree
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfSrc: Type?,
        context: JsonDeserializationContext?
    ): BaseEvent {
        if (json == null || json.isJsonNull) {
            throw JsonParseException("BaseEvent JSON must not be null")
        }
        val obj = json.asJsonObject
        val channel = Channel.valueOf(obj.get("channel").asString)
        val payloadJson = obj.get("channel_payload")
            ?: throw JsonParseException("channel_payload is required")

        val payload = deserializeChannelPayload(channel, payloadJson, context!!)
        obj.remove("channel_payload")

        val baseGson = GsonBuilder()
            .registerTypeAdapter(EventType::class.java, EventTypeAdapter())
            .registerTypeAdapter(UrlScheme::class.java, UrlSchemeAdapter())
            .create()

        val partial = baseGson.fromJson(obj, BaseEventPartial::class.java)
        return BaseEvent(
            schemaVersion = partial.schemaVersion,
            eventId = partial.eventId,
            eventType = partial.eventType,
            channel = partial.channel,
            processingStatus = partial.processingStatus,
            capturedAt = partial.capturedAt,
            submittedAt = partial.submittedAt,
            deviceId = partial.deviceId,
            appVersion = partial.appVersion,
            source = partial.source,
            content = partial.content,
            channelPayload = payload,
            processedAt = partial.processedAt,
            requestId = partial.requestId,
            ttlSeconds = partial.ttlSeconds,
            urls = partial.urls,
            scamRiskScore = partial.scamRiskScore,
            scamRiskLevel = partial.scamRiskLevel,
            scamExplanations = partial.scamExplanations,
            attachments = partial.attachments,
            riskAssessment = partial.riskAssessment,
            investigationReport = partial.investigationReport
        )
    }

    private fun deserializeChannelPayload(
        channel: Channel,
        json: JsonElement,
        context: JsonDeserializationContext
    ): ChannelPayloadData = when (channel) {
        Channel.SMS -> ChannelPayloadData.Sms(
            context.deserialize(json, SmsChannelPayload::class.java)
        )
        Channel.CALL -> ChannelPayloadData.Call(
            context.deserialize(json, CallChannelPayload::class.java)
        )
        Channel.WHATSAPP -> ChannelPayloadData.WhatsApp(
            context.deserialize(json, WhatsAppChannelPayload::class.java)
        )
        Channel.TELEGRAM -> ChannelPayloadData.Telegram(
            context.deserialize(json, TelegramChannelPayload::class.java)
        )
        Channel.GMAIL -> ChannelPayloadData.Gmail(
            context.deserialize(json, GmailChannelPayload::class.java)
        )
        Channel.COPILOT -> ChannelPayloadData.Copilot(
            context.deserialize(json, CopilotChannelPayload::class.java)
        )
    }
}

/** Internal DTO used by [BaseEventAdapter] during deserialization. */
private data class BaseEventPartial(
    @com.google.gson.annotations.SerializedName("schema_version") val schemaVersion: String,
    @com.google.gson.annotations.SerializedName("event_id") val eventId: String,
    @com.google.gson.annotations.SerializedName("event_type") val eventType: EventType,
    @com.google.gson.annotations.SerializedName("channel") val channel: Channel,
    @com.google.gson.annotations.SerializedName("processing_status") val processingStatus: ProcessingStatus,
    @com.google.gson.annotations.SerializedName("captured_at") val capturedAt: String,
    @com.google.gson.annotations.SerializedName("submitted_at") val submittedAt: String,
    @com.google.gson.annotations.SerializedName("device_id") val deviceId: String,
    @com.google.gson.annotations.SerializedName("app_version") val appVersion: String,
    @com.google.gson.annotations.SerializedName("source") val source: SourceBlock,
    @com.google.gson.annotations.SerializedName("content") val content: ContentBlock,
    @com.google.gson.annotations.SerializedName("processed_at") val processedAt: String? = null,
    @com.google.gson.annotations.SerializedName("request_id") val requestId: String? = null,
    @com.google.gson.annotations.SerializedName("ttl_seconds") val ttlSeconds: Int? = null,
    @com.google.gson.annotations.SerializedName("urls") val urls: List<UrlAnalysisItem>? = null,
    @com.google.gson.annotations.SerializedName("scam_risk_score") val scamRiskScore: Int? = null,
    @com.google.gson.annotations.SerializedName("scam_risk_level") val scamRiskLevel: ScamRiskLevel? = null,
    @com.google.gson.annotations.SerializedName("scam_explanations") val scamExplanations: List<String>? = null,
    @com.google.gson.annotations.SerializedName("attachments") val attachments: List<AttachmentAnalysisItem>? = null,
    @com.google.gson.annotations.SerializedName("risk_assessment") val riskAssessment: RiskAssessmentBlock? = null,
    @com.google.gson.annotations.SerializedName("investigation_report") val investigationReport: InvestigationReportBlock? = null
)
