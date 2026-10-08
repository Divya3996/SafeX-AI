package com.sentinel.ai.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

enum class SentinelThemeMode {
    Dark,
    Neon,
    System
}

/** Stores one app-wide theme choice; screens never own their own theme state. */
object ThemePreferences {
    private const val PreferencesName = "sentinel_theme"
    private const val ThemeModeKey = "theme_mode"

    fun get(context: Context): SentinelThemeMode = runCatching {
        SentinelThemeMode.valueOf(preferences(context).getString(ThemeModeKey, SentinelThemeMode.Dark.name).orEmpty())
    }.getOrDefault(SentinelThemeMode.Dark)

    fun set(context: Context, mode: SentinelThemeMode) {
        preferences(context).edit().putString(ThemeModeKey, mode.name).apply()
    }

    private fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
}

@Composable
fun rememberThemeMode(context: Context): State<SentinelThemeMode> {
    val mode = remember { mutableStateOf(ThemePreferences.get(context)) }
    val preferences = remember(context) {
        context.getSharedPreferences("sentinel_theme", Context.MODE_PRIVATE)
    }

    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "theme_mode") mode.value = ThemePreferences.get(context)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return mode
}
