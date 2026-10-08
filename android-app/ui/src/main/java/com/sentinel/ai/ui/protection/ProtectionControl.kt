package com.sentinel.ai.ui.protection

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class ProtectionSnapshot(
    val protectionEnabled: Boolean = true,
    val guardServiceRunning: Boolean = false,
    val monitorServiceRunning: Boolean = false,
    val notificationListenerEnabled: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val overlayPermissionGranted: Boolean = false,
    val contactsPermissionGranted: Boolean = false,
    val missingPermissions: List<String> = emptyList()
)

object ProtectionControl {

    private const val PREFS_NAME = "sentinel_protection"
    private const val KEY_ENABLED = "protection_enabled"
    private const val SENTINEL_GUARD_SERVICE = "com.sentinel.ai.services.SentinelGuardService"
    private const val THREAT_MONITOR_SERVICE = "com.sentinel.ai.services.ThreatMonitorService"

    fun snapshot(context: Context): ProtectionSnapshot {
        val enabled = isProtectionEnabled(context)
        val guardRunning = com.sentinel.ai.core.feature.PrivacyPreferences.listenerConnected
        val monitorRunning = guardRunning
        val listenerEnabled = isNotificationListenerEnabled(context)
        val notificationPermissionGranted = hasPostNotificationsPermission(context)
        val overlayPermissionGranted = Settings.canDrawOverlays(context)
        val contactsPermissionGranted = hasContactsPermission(context)
        val missingPermissions = buildList {
            if (!notificationPermissionGranted) add("Notification permission")
            if (!listenerEnabled) add("Notification listener access")
        }

        return ProtectionSnapshot(
            protectionEnabled = enabled,
            guardServiceRunning = guardRunning,
            monitorServiceRunning = monitorRunning,
            notificationListenerEnabled = listenerEnabled,
            notificationPermissionGranted = notificationPermissionGranted,
            overlayPermissionGranted = overlayPermissionGranted,
            contactsPermissionGranted = contactsPermissionGranted,
            missingPermissions = missingPermissions
        )
    }

    fun setProtectionEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        sync(context)
    }

    fun isProtectionEnabled(context: Context): Boolean {
        return preferences(context).getBoolean(KEY_ENABLED, true)
    }

    fun sync(context: Context) {
        // NotificationListenerService is bound by Android; no always-running service is started.
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    private fun hasPostNotificationsPermission(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 33) return true
        return NotificationManagerCompat.from(context).areNotificationsEnabled() && ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun preferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
