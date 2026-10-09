package com.sentinel.ai

import android.app.Application
import com.sentinel.ai.core.event.*
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.core.model.ProtectionDecision
import com.sentinel.ai.warning.WarningNotificationHelper
import com.sentinel.ai.ui.protection.ProtectionControl
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltAndroidApp
class SentinelApp : Application() {
    @Inject lateinit var events: ThreatEventBus
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun onCreate() {
        super.onCreate()
        // Application lifetime covers the interval when Android removes the capture gateway.
        androidx.core.content.ContextCompat.registerReceiver(this, object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF) {
                    com.sentinel.ai.protection.floating.CaptureSessionStore.clear()
                    stopService(android.content.Intent(this@SentinelApp, com.sentinel.ai.protection.floating.OneShotScreenCaptureService::class.java))
                }
            }
        }, android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        FeatureManager.init(this)
        DisplayPreferences.init(this)
        PrivacyPreferences.init(this)
        ThreatJournal.initialize(this)
        com.sentinel.ai.services.HistoryMaintenanceWorker.schedule(this)
        appScope.launch(start = CoroutineStart.UNDISPATCHED) {
            events.events.collect { event ->
                if (event is ThreatEvent.WhatsAppThreatDetected && ProtectionControl.isProtectionEnabled(this@SentinelApp)) {
                    runCatching { WarningNotificationHelper(this@SentinelApp).showWarning(event.scanResult,
                        highPriority = event.scanResult.riskScore >= 70f || event.scanResult.decision == ProtectionDecision.BLOCK) }
                }
            }
        }
        appScope.launch { ThreatJournal.applyRetention(PrivacyPreferences.retentionDays) }
    }
}
