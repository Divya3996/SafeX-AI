package com.sentinel.ai.ml

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sentinel.ai.core.data.ScanRepository
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.model.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class OfflinePipelineTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository get() = EntryPointAccessors.fromApplication(context, PipelineEntryPoint::class.java).repository()
    private fun fixture(name: String, bytes: ByteArray): android.net.Uri {
        val dir = File(context.cacheDir, "test-fixtures").apply { mkdirs() }
        val file = File(dir, name).apply { writeBytes(bytes) }
        return FileProvider.getUriForFile(context, context.packageName + ".testfiles", file)
    }

    @Test fun installedAppHasNoInternetPermission() {
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.INTERNET))
    }

    @Test fun credentialScamAndBenignAdviceAreDistinctAndDurable() = runBlocking {
        val scam = repository.scanText("Bank support urgent: please you to share your OTP and password for KYC verification")
        assertEquals(ProtectionDecision.BLOCK, scam.decision)
        assertFalse(DecisionPolicy.canOpen(scam))
        assertTrue(scam.reasons.isNotEmpty())
        val restored = withTimeout(5000) { ThreatJournal.scanResults.first { rows -> rows.any { it.id == scam.id } } }.first { it.id == scam.id }
        assertEquals(scam.reasons, restored.reasons)
        assertEquals(scam.target, restored.target)
        val stored = com.sentinel.ai.core.data.local.SentinelDatabase.getInstance(context).threatDao().getAllThreatRecords().first { it.id == scam.id }
        assertNotNull(stored.resultJson)
        assertTrue(stored.resultJson!!.contains("Credential theft"))
        val advice = repository.scanText("Never share your OTP or password. The meeting starts at 10 tomorrow.")
        assertEquals(ProtectionDecision.ALLOW, advice.decision)
    }

    @Test fun deceptiveLinkCannotBecomeAnOpenableBlock() = runBlocking {
        val result = repository.scanLink("https://paypal-secure.example/verify?redirect=https://example.com")
        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertFalse(DecisionPolicy.canOpen(result))
        assertTrue(result.modelStatus.contains("URL", ignoreCase = true))
    }

    @Test fun disguisedApkDetectedFromContents() = runBlocking {
        val bytes = java.io.ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zip ->
                zip.putNextEntry(ZipEntry("AndroidManifest.xml")); zip.write(byteArrayOf(1,2,3)); zip.closeEntry()
            }
        }.toByteArray()
        val result = repository.scanFile(fixture("invoice.pdf", bytes))
        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertTrue(result.reasons.any { it.message.contains("Disguised Android") })
    }

    @Test fun bundledOcrReadsSyntheticScreenshotOffline() = runBlocking {
        val bitmap = Bitmap.createBitmap(1200, 500, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            val paint = Paint().apply { color = Color.BLACK; textSize = 52f; isAntiAlias = true }
            drawText("Urgent bank: Share your OTP", 35f, 150f, paint)
            drawText("and password for KYC verification", 35f, 250f, paint)
        }
        val bytes = java.io.ByteArrayOutputStream().apply { bitmap.compress(Bitmap.CompressFormat.PNG, 100, this) }.toByteArray()
        bitmap.recycle()
        val result = repository.scanImage(fixture("synthetic-screenshot.png", bytes), false)
        assertEquals("image", result.contentType)
        assertEquals(ProtectionDecision.BLOCK, result.decision)
    }

    @Test fun bundledQrReadsSyntheticLinkOffline() = runBlocking {
        val bytes = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("synthetic-qr.png").use { it.readBytes() }
        val result = repository.scanImage(fixture("synthetic-qr.png", bytes), true)
        assertEquals("link", result.contentType)
        assertTrue("QR web destinations retain domain review", result.linkInspections.orEmpty().isNotEmpty())
        assertEquals(ProtectionDecision.BLOCK, result.decision)
    }

    @Test fun prototypeModelMatchesPublishedValidationScores() {
        val model = TextInferenceManager(context)
        assertEquals(0.6022f, model.predict("To prevent suspension urgently send your bank password to support"), 0.01f)
        assertTrue(model.predict("Never give anyone your bank password or OTP") < 0.60f)
    }

    @Test fun nativeHindiAndGujaratiScamsAndAdviceWorkOffline() = runBlocking {
        listOf("बैंक सहायता तुरंत अपना ओटीपी और पासवर्ड भेजें", "બેંક સહાય હમણાં તમારો ઓટીપી અને પાસવર્ડ મોકલો").forEach { message ->
            val result = repository.scanText(message)
            assertEquals(message, ProtectionDecision.BLOCK, result.decision)
            assertEquals(message, result.target)
            assertEquals("Credential theft", result.category)
        }
        listOf("अपना ओटीपी या पासवर्ड किसी को न बताएं।", "તમારો ઓટીપી કે પાસવર્ડ ક્યારેય કોઈને આપશો નહીં.").forEach { message ->
            assertEquals(message, ProtectionDecision.ALLOW, repository.scanText(message).decision)
        }
    }

    @Test fun bundledHindiAndGujaratiOcrReadsNativeScriptsOffline() = runBlocking {
        for ((index, lines) in listOf(
            listOf("बैंक खाता बंद होगा", "तुरंत अपना ओटीपी भेजें", "और पासवर्ड बताएं"),
            listOf("બેંક ખાતું બંધ થશે", "હમણાં તમારો ઓટીપી મોકલો", "અને પાસવર્ડ આપો")
        ).withIndex()) {
            val bitmap = Bitmap.createBitmap(1800, 650, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).apply {
                drawColor(Color.WHITE)
                val paint = Paint().apply { color = Color.BLACK; textSize = 76f; isAntiAlias = true }
                lines.forEachIndexed { i, line -> drawText(line, 70f, 160f + i * 160f, paint) }
            }
            val bytes = java.io.ByteArrayOutputStream().apply { bitmap.compress(Bitmap.CompressFormat.PNG, 100, this) }.toByteArray()
            bitmap.recycle()
            val result = repository.scanImage(fixture("native-screenshot-$index.png", bytes), false)
            assertEquals("OCR script $index: ${result.target}", ProtectionDecision.BLOCK, result.decision)
            assertTrue(result.target!!.any { if (index == 0) it in '\u0900'..'\u097F' else it in '\u0A80'..'\u0AFF' })
        }
    }
}
