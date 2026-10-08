package com.sentinel.ai.ui.util

import android.content.Context

fun String.toAppLabel(context: Context): String {
    val packageName = trim()
    if (packageName.isEmpty()) return packageName

    FRIENDLY_APP_LABELS[packageName]?.let { return it }

    return runCatching {
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(applicationInfo).toString()
    }.getOrElse {
        packageName
            .substringAfterLast('.')
            .replace('_', ' ')
            .replaceFirstChar { char -> char.uppercase() }
    }
}

private val FRIENDLY_APP_LABELS = mapOf(
    "com.whatsapp" to "WhatsApp",
    "com.whatsapp.w4b" to "WhatsApp Business",
    "org.telegram.messenger" to "Telegram",
    "com.google.android.gm" to "Gmail",
    "com.google.android.apps.messaging" to "Google Messages",
    "com.instagram.android" to "Instagram",
    "com.facebook.orca" to "Messenger",
    "org.thoughtcrime.securesms" to "Signal",
    "com.discord" to "Discord",
    "com.Slack" to "Slack"
)
