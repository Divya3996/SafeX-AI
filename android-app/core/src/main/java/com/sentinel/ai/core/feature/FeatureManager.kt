package com.sentinel.ai.core.feature

import android.content.Context
import android.content.SharedPreferences

/** Persistent, process-wide feature switches. Getters remain safe before application startup. */
object FeatureManager {
    private const val PREFS_NAME = "sentinel_features"
    private const val KEY_NOTIFICATION_ENABLED = "notificationEnabled"
    private const val KEY_CLICK_PROTECTION_ENABLED = "clickProtectionEnabled"
    private const val KEY_TEXT_SELECTION_ENABLED = "textSelectionEnabled"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs == null) {
                prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    fun isNotificationEnabled(): Boolean =
        prefs?.getBoolean(KEY_NOTIFICATION_ENABLED, true) ?: true

    fun setNotificationEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_NOTIFICATION_ENABLED, enabled)?.apply()
    }

    fun isClickEnabled(): Boolean =
        prefs?.getBoolean(KEY_CLICK_PROTECTION_ENABLED, true) ?: true

    fun setClickEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_CLICK_PROTECTION_ENABLED, enabled)?.apply()
    }

    fun isTextEnabled(): Boolean =
        prefs?.getBoolean(KEY_TEXT_SELECTION_ENABLED, true) ?: true

    fun setTextEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_TEXT_SELECTION_ENABLED, enabled)?.apply()
    }
}
