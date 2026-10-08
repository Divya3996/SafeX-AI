package com.sentinel.ai.core.event

import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.core.model.Threat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ThreatEventBus {
    private val _events = MutableSharedFlow<ThreatEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val events: SharedFlow<ThreatEvent> = _events.asSharedFlow()

    suspend fun emit(event: ThreatEvent) {
        ThreatJournal.recordDurably(event)
        _events.emit(event)
    }
}

sealed interface ThreatEvent {
    data class SmsThreatDetected(val scanResult: ScanResult) : ThreatEvent
    data class CallThreatDetected(val scanResult: ScanResult) : ThreatEvent
    data class LinkThreatDetected(val scanResult: ScanResult) : ThreatEvent
    data class FileThreatDetected(val scanResult: ScanResult) : ThreatEvent
    data class WhatsAppThreatDetected(val scanResult: ScanResult) : ThreatEvent
    data class CriticalThreatAlert(val threat: Threat) : ThreatEvent
    data object GuardActivated : ThreatEvent
    data object GuardDeactivated : ThreatEvent
}
