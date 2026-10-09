package com.sentinel.ai.core.feature

import android.content.Context
import android.content.Intent
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared entry points without introducing an app-module dependency into UI. */
object FloatingAssistantControl {
    private const val ACTIVITY = "com.sentinel.ai.protection.floating.FloatingAssistantActivity"
    private const val SERVICE = "com.sentinel.ai.protection.floating.FloatingAssistantService"
    private val state = MutableStateFlow(false)
    val running = state.asStateFlow()
    @Volatile var helperVisible = false
    @Volatile var privateReviewAvailable = false
    fun setRunning(value: Boolean) { state.value = value }
    fun open(context: Context, mode: String = "setup") {
        context.startActivity(Intent().setClassName(context.packageName, ACTIVITY).putExtra("mode", mode).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun start(context: Context) {
        check(Settings.canDrawOverlays(context)) { "Allow display over other apps first." }
        context.startForegroundService(Intent().setClassName(context.packageName, SERVICE))
    }
    fun command(context: Context, action: String) {
        if (!state.value) return
        runCatching { context.startService(Intent().setClassName(context.packageName, SERVICE).setAction(action)) }
    }
    fun stop(context: Context) {
        context.stopService(Intent().setClassName(context.packageName, SERVICE))
        state.value = false
    }
}
