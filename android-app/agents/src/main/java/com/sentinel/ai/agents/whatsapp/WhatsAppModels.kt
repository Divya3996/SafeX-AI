package com.sentinel.ai.agents.whatsapp

data class NotificationSnapshot(
    val packageName: String,
    val notificationKey: String,
    /**
     * Legacy aliases retained for callers that still construct WhatsApp snapshots directly.
     * New notification capture should populate [title], [text], and [bigText] from Android
     * notification extras.
     */
    val sender: String? = null,
    val message: String? = null,
    val timestampMs: Long,
    val conversationTitle: String? = null,
    val subText: String? = null,
    val actionLabels: List<String> = emptyList(),
    val title: String? = sender,
    val text: String? = message,
    val bigText: String? = null
)

/** Compatibility alias while the active registry still contains only WhatsApp packages. */
typealias WhatsAppNotificationSnapshot = NotificationSnapshot

/** Source-independent fields extracted from a notification. */
data class NormalizedNotificationData(
    val senderTitle: String,
    val messageText: String,
    val timestampMs: Long,
    val packageName: String
)

data class WhatsAppRawNotificationData(
    val notificationKey: String,
    val packageName: String,
    val senderDisplayName: String?,
    val messageText: String?,
    val subText: String?,
    val conversationTitle: String?,
    val isGroupChat: Boolean,
    val groupName: String?,
    val isForwarded: Boolean,
    val forwardChainLength: Int?,
    val actionLabels: List<String>,
    val hasCallButton: Boolean,
    val capturedAtMs: Long,
    val normalized: NormalizedNotificationData = NormalizedNotificationData(
        senderTitle = senderDisplayName.orEmpty(),
        messageText = messageText.orEmpty(),
        timestampMs = capturedAtMs,
        packageName = packageName
    )
)
