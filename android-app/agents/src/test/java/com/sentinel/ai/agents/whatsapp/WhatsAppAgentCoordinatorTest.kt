package com.sentinel.ai.agents.whatsapp

import com.sentinel.ai.agents.registry.SupportedAppRegistry
import com.sentinel.ai.core.event.ThreatEvent
import com.sentinel.ai.core.event.ThreatEventBus
import com.sentinel.ai.core.model.RiskLevel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppAgentCoordinatorTest {

    @Test
    fun `publishes WhatsApp threat event for valid notification`() = runTest {
        val bus = ThreatEventBus()
        val coordinator = WhatsAppAgentCoordinator(
            parser = WhatsAppNotificationParser(SupportedAppRegistry()),
            builder = WhatsAppEventBuilder(),
            threatEventBus = bus
        )
        val emitted = async(start = CoroutineStart.UNDISPATCHED) { bus.events.first() }

        coordinator.onWhatsAppNotification(
            WhatsAppNotificationSnapshot(
                packageName = "com.whatsapp",
                notificationKey = "wa-key",
                sender = "John Doe",
                message = "Urgent payment request",
                timestampMs = 1_719_218_400_000L
            )
        )

        val event = emitted.await()
        assertTrue(event is ThreatEvent.WhatsAppThreatDetected)
        assertEquals("COMPLETED", coordinator.lastStatus.value)
        assertEquals("com.whatsapp", (event as ThreatEvent.WhatsAppThreatDetected).scanResult.source)
        assertEquals(RiskLevel.GREEN, event.scanResult.riskLevel)
    }

    @Test
    fun `ignores unsupported packages`() = runTest {
        val bus = ThreatEventBus()
        val coordinator = WhatsAppAgentCoordinator(
            parser = WhatsAppNotificationParser(SupportedAppRegistry()),
            builder = WhatsAppEventBuilder(),
            threatEventBus = bus
        )

        coordinator.onWhatsAppNotification(
            WhatsAppNotificationSnapshot(
                packageName = "com.example.fake",
                notificationKey = "other-key",
                sender = "John Doe",
                message = "Hello",
                timestampMs = 1_719_218_400_000L
            )
        )

        assertEquals("IGNORED", coordinator.lastStatus.value)
        assertTrue(bus.events.replayCache.isEmpty())
    }

    @Test
    fun `suppresses duplicate reposts for the same notification`() = runTest {
        val bus = ThreatEventBus()
        val coordinator = WhatsAppAgentCoordinator(
            parser = WhatsAppNotificationParser(SupportedAppRegistry()),
            builder = WhatsAppEventBuilder(),
            threatEventBus = bus
        )
        val events = mutableListOf<ThreatEvent>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            bus.events.collect { events += it }
        }
        val snapshot = WhatsAppNotificationSnapshot(
            packageName = "com.whatsapp",
            notificationKey = "wa-key",
            sender = "John Doe",
            message = "Urgent payment request",
            timestampMs = 1_719_218_400_000L
        )

        coordinator.onWhatsAppNotification(snapshot)
        coordinator.onWhatsAppNotification(snapshot)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals("IGNORED", coordinator.lastStatus.value)
        collector.cancel()
    }

    @Test
    fun `deduplicates critical reposts without flooding warnings`() = runTest {
        val bus = ThreatEventBus()
        val coordinator = WhatsAppAgentCoordinator(
            parser = WhatsAppNotificationParser(SupportedAppRegistry()),
            builder = WhatsAppEventBuilder(),
            threatEventBus = bus
        )
        val events = mutableListOf<ThreatEvent>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            bus.events.collect { events += it }
        }
        val snapshot = WhatsAppNotificationSnapshot(
            packageName = "com.whatsapp",
            notificationKey = "critical-key",
            sender = "Unknown Sender",
            message = "Bank support urgent share your OTP immediately",
            timestampMs = 1_719_218_400_000L
        )

        coordinator.onWhatsAppNotification(snapshot)
        coordinator.onWhatsAppNotification(snapshot)
        advanceUntilIdle()

        assertEquals(1, events.size)
        events.forEach { event ->
            assertEquals(
                RiskLevel.CRITICAL,
                (event as ThreatEvent.WhatsAppThreatDetected).scanResult.riskLevel
            )
        }
        assertEquals("IGNORED", coordinator.lastStatus.value)
        collector.cancel()
    }
}
