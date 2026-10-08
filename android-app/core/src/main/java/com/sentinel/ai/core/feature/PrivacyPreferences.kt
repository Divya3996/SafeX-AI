package com.sentinel.ai.core.feature

import android.content.Context

object PrivacyPreferences {
    private var prefs: android.content.SharedPreferences? = null
    fun init(context: Context) { prefs = context.getSharedPreferences("sentinel_privacy", Context.MODE_PRIVATE) }
    var retentionDays: Int
        get() = prefs?.getInt("retention", 30) ?: 30
        set(value) { prefs?.edit()?.putInt("retention", value)?.apply() }
    fun isAppEnabled(packageName: String): Boolean = prefs?.getBoolean("app_$packageName", true) ?: true
    fun setAppEnabled(packageName: String, enabled: Boolean) { prefs?.edit()?.putBoolean("app_$packageName", enabled)?.apply() }
    var listenerConnected = false
}
