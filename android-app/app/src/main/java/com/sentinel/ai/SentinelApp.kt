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
