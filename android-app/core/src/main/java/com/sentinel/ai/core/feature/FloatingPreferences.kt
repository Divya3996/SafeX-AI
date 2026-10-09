package com.sentinel.ai.core.feature

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FloatingSettings(val sizeDp: Int = 60, val reducedMotion: Boolean = true)
object FloatingPreferences {
    private val mutable = MutableStateFlow(FloatingSettings())
    val settings = mutable.asStateFlow()
    fun init(context: Context) {
        val prefs = context.getSharedPreferences("safex_floating", Context.MODE_PRIVATE)
        mutable.value = FloatingSettings(prefs.getInt("size", 60).coerceIn(52, 72), prefs.getBoolean("reduced_motion", true))
    }
    fun setSize(context: Context, size: Int) {
        mutable.value = mutable.value.copy(sizeDp = size.coerceIn(52, 72))
        context.getSharedPreferences("safex_floating", Context.MODE_PRIVATE).edit().putInt("size", mutable.value.sizeDp).apply()
        if (FloatingAssistantControl.running.value) FloatingAssistantControl.command(context, "refresh")
    }
    fun setReducedMotion(context: Context, value: Boolean) {
        mutable.value = mutable.value.copy(reducedMotion = value)
        context.getSharedPreferences("safex_floating", Context.MODE_PRIVATE).edit().putBoolean("reduced_motion", value).apply()
    }
}
