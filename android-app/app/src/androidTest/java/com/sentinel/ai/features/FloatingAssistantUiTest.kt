package com.sentinel.ai.features

import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.core.i18n.I18n
import com.sentinel.ai.protection.floating.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class FloatingAssistantUiTest {
    @get:Rule val compose = createAndroidComposeRule<FloatingAssistantActivity>()
    private fun screenshot(name: String) {
        compose.runOnUiThread { compose.activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
        val file = java.io.File(compose.activity.getExternalFilesDir(null), "floating-screenshots/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Before fun resetLanguage() { compose.runOnUiThread { DisplayPreferences.setLanguage(compose.activity, AppLanguage.ENGLISH); DisplayPreferences.setTextScale(1f) } }
    @After fun cleanup() {
        compose.runOnUiThread { DisplayPreferences.setLanguage(compose.activity, AppLanguage.ENGLISH); DisplayPreferences.setTextScale(1f); FloatingAssistantControl.stop(compose.activity) }
    }
    @Test fun privatePasteAndContextStayUnsavedUntilExplicitSave() {
        compose.onNodeWithText("Paste link or message").performScrollTo().performClick()
        compose.onNodeWithText("Text to analyze").performTextInput("https://example.com/")
        compose.onNodeWithText("Analyze privately").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Private review • not saved").fetchSemanticsNodes().isNotEmpty() }
        lateinit var model: FloatingSessionViewModel
        compose.runOnUiThread { model = ViewModelProvider(compose.activity)[FloatingSessionViewModel::class.java] }
        val id = model.state.value.result!!.id
        try {
            assertFalse(ThreatJournal.scanResults.value.any { it.id == id })
            compose.onNodeWithText("Add context").performScrollTo().performClick()
            compose.onNodeWithText("Does it ask you to share an OTP, PIN or password?").performScrollTo().performClick()
            compose.onNodeWithText("Update risk review").performScrollTo().performClick()
            compose.waitUntil(5000) { model.state.value.result?.contextReview?.asksForSecret == true }
            assertFalse(ThreatJournal.scanResults.value.any { it.id == id })
            compose.onNodeWithText("Save result").performClick()
            compose.waitUntil(5000) { ThreatJournal.scanResults.value.any { it.id == id && it.contextReview?.asksForSecret == true } }
            compose.waitUntil(5000) { model.state.value.saved }
            compose.waitUntil(5000) { runCatching { compose.onNodeWithText("Saved to local history").assertIsDisplayed() }.isSuccess }
            assertEquals(1, ThreatJournal.scanResults.value.count { it.id == id })
        } finally { runBlocking { ThreatJournal.delete(id) } }
    }
    @Test fun largestTextScaleSetupAndPasteWorkInAllThreeLanguages() {
        for (language in AppLanguage.entries) {
            compose.runOnUiThread {
                val model = ViewModelProvider(compose.activity)[FloatingSessionViewModel::class.java]
                model.reset()
                DisplayPreferences.setLanguage(compose.activity, language); DisplayPreferences.setTextScale(1.5f)
            }
            compose.waitForIdle()
            val paste = I18n.translate(compose.activity, "Paste link or message", language)
            screenshot("setup-${language.tag}-150")
            compose.onNodeWithText(paste).performScrollTo().performClick()
            compose.onNodeWithText(I18n.translate(compose.activity, "Text to analyze", language)).performTextInput("https://example.com/")
            compose.onNodeWithText(I18n.translate(compose.activity, "Analyze privately", language)).performScrollTo().assertHasClickAction()
            screenshot("paste-${language.tag}-150")
        }
    }

    @Test fun clipboardIsReadOnlyWhenTheUserChoosesPaste() {
        val value = "https://example.com/clipboard-fixture"
        lateinit var model: FloatingSessionViewModel
        compose.runOnUiThread {
            model = ViewModelProvider(compose.activity)[FloatingSessionViewModel::class.java]
            compose.activity.getSystemService(android.content.ClipboardManager::class.java)
                .setPrimaryClip(android.content.ClipData.newPlainText("Authored fixture", value))
        }
        compose.onNodeWithText("Paste link or message").performScrollTo().performClick()
        assertEquals("", model.state.value.text)
        compose.onNodeWithText("Paste", substring = false).performScrollTo().performClick()
        compose.waitUntil(5000) { model.state.value.text == value }
    }
    @Test fun lockingTheScreenClearsPrivateContent() {
        compose.onNodeWithText("Paste link or message").performScrollTo().performClick()
        compose.onNodeWithText("Text to analyze").performTextInput("Authored private message")
        lateinit var model: FloatingSessionViewModel
        compose.runOnUiThread { model = ViewModelProvider(compose.activity)[FloatingSessionViewModel::class.java] }
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val device = androidx.test.uiautomator.UiDevice.getInstance(instrumentation)
        try {
            device.sleep()
            compose.waitUntil(5000) { model.state.value.text.isEmpty() && model.state.value.stage == FloatingStage.SETUP }
            assertFalse(FloatingAssistantControl.privateReviewAvailable)
        } finally {
            device.wakeUp()
            android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard")).use { it.readBytes() }
        }
    }
    @Test fun blankAndOversizedTextCannotStartAnalysis() {
        compose.onNodeWithText("Paste link or message").performScrollTo().performClick()
        compose.onNodeWithText("Analyze privately").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Text to analyze").performTextInput("x".repeat(12001))
        compose.onNodeWithText("Analyze privately").performScrollTo().assertIsNotEnabled()
        assertFalse(ThreatJournal.scanResults.value.any { it.source == "Floating pasted content" && it.target == "x".repeat(12001) })
    }
}
