package com.sentinel.ai.listeners

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import com.sentinel.ai.agents.whatsapp.NotificationAgentCoordinator
import com.sentinel.ai.agents.registry.SupportedAppRegistry
import com.sentinel.ai.core.feature.FeatureManager
import com.sentinel.ai.ui.protection.ProtectionControl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SentinelNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject lateinit var notificationAgentCoordinator: NotificationAgentCoordinator
    @Inject lateinit var supportedAppRegistry: SupportedAppRegistry

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!FeatureManager.isNotificationEnabled()) return
        if (!ProtectionControl.isProtectionEnabled(this)) {
            Log.d(TAG, "Notification ignored: protection_disabled, package=${sbn.packageName}")
            return
        }
        if (!com.sentinel.ai.core.feature.PrivacyPreferences.isAppEnabled(sbn.packageName)) return
        if (!supportedAppRegistry.isSupported(sbn.packageName)) {
            Log.d(TAG, "Notification ignored: unsupported package=${sbn.packageName}")
            return
        }

        val notification = sbn.notification ?: return

        if (notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0) {
            Log.d(TAG, "Notification ignored: group_summary")
            return
        }

        val extras = notification.extras ?: return

        val snapshot = com.sentinel.ai.agents.whatsapp.WhatsAppNotificationSnapshot(
            packageName = sbn.packageName,
            notificationKey = sbn.key,
            timestampMs = sbn.postTime,
            title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString(),
            bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString(),
            conversationTitle = extras.getCharSequence(android.app.Notification.EXTRA_CONVERSATION_TITLE)?.toString(),
            subText = extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString(),
            actionLabels = notification.actions?.mapNotNull { it.title?.toString() }.orEmpty()
        )

        val messages = if (android.os.Build.VERSION.SDK_INT >= 30) android.app.Notification.MessagingStyle.Message.getMessagesFromBundleArray(
            extras.getParcelableArray(android.app.Notification.EXTRA_MESSAGES)) else emptyList()
        val newest = messages.lastOrNull()
        val completeSnapshot = if (newest != null) snapshot.copy(
            text = newest.text?.toString(), bigText = newest.text?.toString(),
            title = (if (android.os.Build.VERSION.SDK_INT >= 28) newest.senderPerson?.name else newest.sender)?.toString() ?: snapshot.title,
            timestampMs = newest.timestamp
        ) else snapshot
        serviceScope.launch {
            notificationAgentCoordinator.onWhatsAppNotification(
                snapshot = completeSnapshot,
                isKnownContact = false
            )
        }
    }

    // ================= FIXED FUNCTION =================

    private fun isKnownContact(snapshot: com.sentinel.ai.agents.whatsapp.WhatsAppNotificationSnapshot): Boolean {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "READ_CONTACTS not granted")
            return false
        }

        val sender = snapshot.title?.trim().orEmpty()

        return runCatching {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->

                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

                val isSenderPhone = isPhoneNumber(sender)
                val normalizedSenderPhone = normalizeNumber(sender)

                while (cursor.moveToNext()) {

                    val contactNumber = if (numberIndex >= 0) cursor.getString(numberIndex).orEmpty() else ""
                    val contactName = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""

                    // 🔥 CASE 1: Sender is PHONE (WhatsApp unsaved)
                    if (isSenderPhone) {
                        val normalizedContact = normalizeNumber(contactNumber)

                        if (normalizedContact.isNotEmpty() &&
                            normalizedContact == normalizedSenderPhone
                        ) {
                            return@use true
                        }
                    }

                    // 🔥 CASE 2: Sender is NAME (WhatsApp saved)
                    else {
                        if (contactNamesMatch(sender, contactName)) {
                            return@use true
                        }
                    }
                }

                false
            } ?: false

        }.onFailure {
            Log.e(TAG, "Contact lookup failed", it)
        }.getOrDefault(false)
    }

    // ================= HELPERS =================

    private fun isPhoneNumber(text: String): Boolean {
        return text.matches(Regex("^[+0-9\\s()-]+$"))
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        com.sentinel.ai.core.feature.PrivacyPreferences.listenerConnected = true
    }

    override fun onListenerDisconnected() {
        com.sentinel.ai.core.feature.PrivacyPreferences.listenerConnected = false
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        com.sentinel.ai.core.feature.PrivacyPreferences.listenerConnected = false
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "SentinelNotification"
    }
}

internal fun normalizeNumber(number: String): String {
    return number
        .replace("\\s".toRegex(), "")
        .replace("-", "")
        .replace("(", "")
        .replace(")", "")
        .replace("+91", "")
        .filter(Char::isDigit)
        .takeLast(10)
}

internal fun normalizeName(name: String): String {
    return name.lowercase()
        .replace("[^a-z ]".toRegex(), "")
        .trim()
}

internal fun contactNamesMatch(senderName: String?, contactDisplayName: String?): Boolean {
    if (senderName.isNullOrBlank() || contactDisplayName.isNullOrBlank()) return false
    val na = normalizeName(senderName)
    val nb = normalizeName(contactDisplayName)
    if (na.isEmpty() || nb.isEmpty()) return false

    if (na == nb) return true

    val tokensA = na.split("\\s+".toRegex()).filter { it.isNotBlank() }
    val tokensB = nb.split("\\s+".toRegex()).filter { it.isNotBlank() }

    if (tokensA.isEmpty() || tokensB.isEmpty()) return false

    if (tokensA.size == 1 && tokensB.size == 1) {
        return tokensA[0] == tokensB[0]
    }

    val (shorter, longer) = if (tokensA.size <= tokensB.size) tokensA to tokensB else tokensB to tokensA
    return shorter.all { shortToken -> longer.contains(shortToken) }
}