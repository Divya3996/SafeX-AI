package com.sentinel.ai.features

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.protection.qr.LiveQrActivity
import org.junit.Rule
import org.junit.Test

class LiveQrCameraTest {
    @get:Rule val compose = createAndroidComposeRule<LiveQrActivity>()
    @Test fun foregroundCameraStartsAndCanBeClosedWithPermission() {
        val context = compose.activity
        compose.runOnUiThread { DisplayPreferences.setLanguage(context, AppLanguage.ENGLISH); DisplayPreferences.setTextScale(1f) }
        android.os.ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.CAMERA")).use { it.readBytes() }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Scan a QR code").assertIsDisplayed()
        // The deterministic emulator fixture can decode before the first ready label renders.
        compose.waitUntil(12000) { compose.onAllNodesWithText("Camera ready").fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Choose a QR code to analyze").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        compose.onNodeWithText("Camera could not start. Choose a QR image instead.").assertDoesNotExist()
        compose.onNodeWithText("Close camera").performScrollTo().performClick()
    }
}
