package com.sentinel.ai.agents.whatsapp

import com.sentinel.ai.agents.registry.SupportedAppRegistry
import java.util.Locale
import javax.inject.Inject

class NotificationParser @Inject constructor(
    private val supportedAppRegistry: SupportedAppRegistry
) {

    fun parse(snapshot: WhatsAppNotificationSnapshot): WhatsAppRawNotificationData? {
        if (!supportedAppRegistry.isSupported(snapshot.packageName)) return null

        // Android applications do not share a notification text layout. Prefer the standard
        // expanded text extra, then the regular text extra, and finally the legacy snapshot
        // value used by existing WhatsApp callers.
        val normalized = NormalizedNotificationData(
            senderTitle = snapshot.title.normalizedOrEmpty().ifEmpty { snapshot.sender.normalizedOrEmpty() },
            messageText = snapshot.bigText.normalizedOrEmpty()
                .ifEmpty { snapshot.text.normalizedOrEmpty() }
                .ifEmpty { snapshot.message.normalizedOrEmpty() },
            timestampMs = snapshot.timestampMs,
            packageName = snapshot.packageName.orEmpty()
        )
        val messageText = normalized.messageText.takeIf { it.isNotEmpty() }
        val sender = normalized.senderTitle.takeIf { it.isNotEmpty() }
        val conversationTitle = snapshot.conversationTitle?.trim().takeIf { !it.isNullOrBlank() }
        val subText = snapshot.subText?.trim().takeIf { !it.isNullOrBlank() }
        val isGroupChat = conversationTitle != null || (subText?.contains("@") == true)
        val groupName = if (isGroupChat) conversationTitle else null
        val normalizedMessage = messageText ?: ""
        val lowered = normalizedMessage.lowercase(Locale.ROOT).trimStart()
        val isForwarded = lowered.startsWith("forwarded")
        val forwardChainLength = when {
            lowered.startsWith("forwarded many times") -> 5
            isForwarded -> 1
            else -> null
        }
        val hasCallButton = snapshot.actionLabels.any {
            it.contains("call", ignoreCase = true) || it.contains("video", ignoreCase = true)
        }

        return WhatsAppRawNotificationData(
            notificationKey = snapshot.notificationKey,
            packageName = snapshot.packageName,
            senderDisplayName = sender,
            messageText = messageText,
            subText = subText,
            conversationTitle = conversationTitle,
            isGroupChat = isGroupChat,
            groupName = groupName,
            isForwarded = isForwarded,
            forwardChainLength = forwardChainLength,
            actionLabels = snapshot.actionLabels,
            hasCallButton = hasCallButton,
            capturedAtMs = snapshot.timestampMs,
            normalized = normalized
        )
    }

    private fun String?.normalizedOrEmpty(): String = this?.trim().orEmpty()
}

/** Compatibility name for the existing WhatsApp-only coordinator and tests. */
typealias WhatsAppNotificationParser = NotificationParser
