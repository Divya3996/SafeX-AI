package com.sentinel.ai.features

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.data.local.SentinelDatabase
import com.sentinel.ai.core.model.*
import com.sentinel.ai.ml.PipelineEntryPoint
import com.sentinel.ai.protection.floating.CaptureSessionStore
import com.sentinel.ai.protection.intent.ImageContentReader
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FloatingPrivacyDeviceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository get() = EntryPointAccessors.fromApplication(context, PipelineEntryPoint::class.java).repository()
    @Test fun actualPrivatePipelinesDoNotWriteRoomHistory() = runBlocking {
        val results = listOf(repository.analyzeLinkPrivately("https://paypal.com@other.example/login"),
            repository.analyzeTextPrivately("Urgent bank support! Share your OTP and password immediately."),
            repository.analyzeQrPrivately("upi://pay?pa=shop@bank&am=50"))
        assertNotEquals(ProtectionDecision.ALLOW, results[1].decision)
        assertFalse(DecisionPolicy.canOpen(results[2]))
        val ids = results.map { it.id }.toSet()
        assertFalse(ThreatJournal.scanResults.value.any { it.id in ids })
        assertFalse(SentinelDatabase.getInstance(context).threatDao().getAllThreatRecords().any { it.id in ids })
    }
    @Test fun capturedPixelsAreHandedOffOnlyOnceAndCancelledFramesAreDiscarded() {
        CaptureSessionStore.clear()
        val first = CaptureSessionStore.begin()
        val stale = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        val next = CaptureSessionStore.begin()
        assertFalse(CaptureSessionStore.deliver(first, stale)); assertTrue(stale.isRecycled)
        val fresh = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        assertTrue(CaptureSessionStore.deliver(next, fresh))
        assertSame(fresh, CaptureSessionStore.take(next)); assertNull(CaptureSessionStore.take(next))
        fresh.recycle(); CaptureSessionStore.clear()
    }
    @Test fun blankImageCannotProduceAClaimOfSuccessfulExtraction() = runBlocking {
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLACK)
        try {
            val result = ImageContentReader(context).extract(bitmap)
            assertTrue(result.text.isBlank()); assertTrue(result.qrCodes.isEmpty())
        } finally { bitmap.recycle() }
    }
}
