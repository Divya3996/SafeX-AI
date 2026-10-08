package com.sentinel.ai.features

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.core.model.ProtectionDecision
import com.sentinel.ai.ui.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Run with the emulator's imagefile camera set to androidTest/assets/camera-qr.png. */
class LiveQrEndToEndTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun actualCameraFrameReturnsToScannerAndProducesABlockedResultOffline() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("safexCameraFixture") == "true")
        val context = compose.activity
        compose.runOnUiThread {
            DisplayPreferences.setLanguage(context, AppLanguage.ENGLISH)
            DisplayPreferences.setTextScale(1f)
            context.getSharedPreferences("sentinel_onboarding", 0).edit().putBoolean("first_launch", false).commit()
        }
        android.os.ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.CAMERA")).use { it.readBytes() }
        compose.activityRule.scenario.recreate()
        val existingIds = ThreatJournal.scanResults.value.map { it.id }.toSet()
        compose.onAllNodesWithText("Scan").onFirst().performClick()
        compose.onNodeWithText("QR code").performClick()
        compose.onNodeWithText("Scan with camera").performScrollTo().performClick()
        compose.waitUntil(20000) { compose.onAllNodesWithText("Choose a QR code to analyze").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Analyze privately").performScrollTo().performClick()
        compose.waitUntil(20000) { compose.onAllNodesWithText("High-risk content").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("High-risk content").assertIsDisplayed()
        compose.onNodeWithText("Open in browser").assertDoesNotExist()
        compose.onNodeWithText("Review opening this link").assertDoesNotExist()
        val result = ThreatJournal.scanResults.value.first { it.id !in existingIds && it.source == "Live QR scan" }
        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertTrue(result.linkInspections.orEmpty().isNotEmpty())
        runBlocking { ThreatJournal.delete(result.id) }
    }
}
