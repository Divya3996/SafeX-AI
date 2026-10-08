package com.sentinel.ai.features

import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.sentinel.ai.core.feature.WarningChannels
import com.sentinel.ai.core.event.ThreatEvent
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.model.*
import com.sentinel.ai.ml.PipelineEntryPoint
import com.sentinel.ai.warning.WarningNotificationHelper
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PracticalFeaturesDeviceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository get() = EntryPointAccessors.fromApplication(context, PipelineEntryPoint::class.java).repository()

    @Test fun liveQrContentAndContextPersistThroughRealRoomPipeline() = runBlocking {
        val qr = repository.scanQrContent("upi://pay?pa=localshop@bank&pn=Local%20Shop&am=125.50&cu=INR")
        assertEquals("localshop@bank", qr.paymentReviews!!.single().address)
        assertEquals(ProtectionDecision.WARN, qr.decision)
        assertFalse(DecisionPolicy.canOpen(qr))
        val reviewed = ContextReview.apply(qr, ContextAnswers(recipientMismatch = true))
        ThreatJournal.recordDurably(ThreatEvent.FileThreatDetected(reviewed))
        val entity = com.sentinel.ai.core.data.local.SentinelDatabase.getInstance(context).threatDao().getAllThreatRecords().first { it.id == qr.id }
        val restored = com.google.gson.Gson().fromJson(entity.resultJson, ScanResult::class.java)
        assertEquals(reviewed, restored)
        assertEquals(70f, restored.riskScore)
        ThreatJournal.delete(qr.id)
    }

    @Test fun identityExplainsRealHostAndVisibleRedirectDestination() = runBlocking {
        val result = repository.scanQrContent("https://paypal.com@other.example/?redirect=https%3A%2F%2Fnext.example%2F")
        val details = result.linkInspections!!.single()
        assertEquals("other.example", details.registrableDomain)
        assertTrue(details.hasUserInfo)
        assertEquals(listOf("next.example"), details.embeddedHosts)
        ThreatJournal.delete(result.id)
    }

    @Test fun warningToneIsAPlayableLocalAssetAndChannelsUseIt() {
        WarningChannels.ensure(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        for (high in listOf(false, true)) {
            val channel = manager.getNotificationChannel(WarningChannels.id(high))
            assertNotNull(channel)
            assertEquals(WarningChannels.tone(context), channel.sound)
            assertFalse(channel.canBypassDnd())
        }
        val player = MediaPlayer.create(context, WarningChannels.tone(context))
        assertNotNull("Bundled tune must decode on Android", player)
        try { assertTrue(player.duration in 700..1300) } finally { player.release() }
    }

    @Test fun soundTestAndThreatWarningsPostButBenignResultsDoNot() = runBlocking {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")).use { it.readBytes() }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
        assertTrue(WarningChannels.test(context))
        val deadline = System.currentTimeMillis() + 5000
        while (manager.activeNotifications.isEmpty() && System.currentTimeMillis() < deadline) kotlinx.coroutines.delay(50)
        assertTrue(manager.activeNotifications.any { it.notification.channelId == WarningChannels.REVIEW })
        manager.cancelAll()
        val helper = WarningNotificationHelper(context)
        helper.showWarning(ScanResult("benign-sound-test", "test", riskLevel = RiskLevel.GREEN, riskScore = 0f, explanation = "Benign", timestamp = 0), false)
        assertFalse(manager.activeNotifications.any { it.id == "benign-sound-test".hashCode() })
        val scam = repository.scanText("Urgent bank support: send your OTP and password immediately for KYC")
        helper.showWarning(scam, true)
        val postedDeadline = System.currentTimeMillis() + 5000
        while (manager.activeNotifications.none { it.id == scam.id.hashCode() } && System.currentTimeMillis() < postedDeadline) kotlinx.coroutines.delay(50)
        val posted = manager.activeNotifications.first { it.id == scam.id.hashCode() }.notification
        assertEquals(WarningChannels.URGENT, posted.channelId)
        assertEquals(android.app.Notification.VISIBILITY_PRIVATE, posted.visibility)
        assertNotNull(posted.publicVersion)
        manager.cancelAll()
        ThreatJournal.delete(scam.id)
    }
}
