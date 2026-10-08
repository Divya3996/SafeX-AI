package com.sentinel.ai.features

import android.content.Context
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.core.i18n.I18n
import com.sentinel.ai.core.model.*
import com.sentinel.ai.ui.screens.help.IncidentHelpScreen
import com.sentinel.ai.ui.screens.scanner.AnalysisResultContent
import com.sentinel.ai.ui.theme.SentinelTheme
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.runBlocking

class PracticalFeaturesUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val testName = org.junit.rules.TestName()
    private val recordId get() = "ui-feature-${testName.methodName}"
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun base() = ScanResult(recordId, "test", riskLevel = RiskLevel.GREEN, riskScore = 0f,
        explanation = "No strong signals", timestamp = System.currentTimeMillis(), target = "https://example.com/",
        linkInspections = listOf(LinkInspection("example.com", "example.com", "example.com", true, false, false, emptyList(), emptyList())))
    private fun screenshot(name: String) {
        val file = java.io.File(context.getExternalFilesDir(null), "feature-screenshots/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Before fun resetLanguage() { compose.runOnUiThread { DisplayPreferences.setLanguage(context, AppLanguage.ENGLISH); DisplayPreferences.setTextScale(1f) } }
    @After fun cleanup() = runBlocking {
        ThreatJournal.delete(recordId)
        compose.runOnUiThread { DisplayPreferences.setLanguage(context, AppLanguage.ENGLISH); DisplayPreferences.setTextScale(1f) }
    }

    @Test fun contextAnswersRaiseVisibleRiskAndAreSaved() {
        val opens = java.util.concurrent.atomic.AtomicInteger()
        val initial = base()
        compose.setContent { SentinelTheme { androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) { AnalysisResultContent(initial, {}, onOpen = { opens.incrementAndGet(); Unit }) } } }
        compose.onNodeWithText("Add context").performScrollTo().performClick()
        compose.onNodeWithText("Does it ask you to share an OTP, PIN or password?").performScrollTo().performClick()
        compose.onNodeWithText("Update risk review").performScrollTo().performClick()
        compose.waitUntil(5000) { ThreatJournal.scanResults.value.any { it.id == recordId && it.riskScore == 70f } }
        compose.onNodeWithText("Review before acting").performScrollTo().assertIsDisplayed()
        screenshot("context-review")
        compose.onNodeWithText("Review opening this link").performScrollTo().performClick()
        compose.onNodeWithText("Open a suspicious link?").assertExists()
        assertEquals(0, opens.get())
        compose.onNodeWithText("Stay here").performClick()
        assertEquals(0, opens.get())
        // AlertDialog owns a separate Android window; wait for dismissal before finding activity nodes.
        compose.waitUntil(5000) { compose.onAllNodesWithText("Review opening this link").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText("Review opening this link").performScrollTo().performClick()
        compose.onNodeWithText("Open anyway").performClick()
        assertEquals(1, opens.get())
        assertTrue(ThreatJournal.scanResults.value.first { it.id == recordId }.contextReview!!.asksForSecret)
    }

    @Test fun mismatchedUpiDetailsUpdateTheResultWithoutLaunchingPayment() {
        val payment = base().copy(contentType = "qr", linkInspections = null, paymentReviews = PaymentQrParser.find("upi://pay?pa=shop@bank&pn=Shop&am=50"))
        compose.setContent { SentinelTheme { androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) { AnalysisResultContent(payment, {}) } } }
        compose.onNodeWithText("Compare expected details").performScrollTo().performClick()
        compose.onNodeWithText("Expected UPI address").performScrollTo().performTextInput("different@bank")
        compose.onNodeWithText("Add mismatch to this result").performScrollTo().performClick()
        compose.waitUntil(5000) { ThreatJournal.scanResults.value.any { it.id == recordId && it.contextReview?.recipientMismatch == true } }
        compose.onNodeWithText("Review before acting").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Open in browser").assertDoesNotExist()
    }

    @Test fun incidentTypesSwitchToAppropriateOfflineSteps() {
        compose.setContent { SentinelTheme { androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) { IncidentHelpScreen {} } } }
        screenshot("incident-help")
        compose.onNodeWithText("I shared a password").performClick()
        compose.onNodeWithText("Change that password anywhere else you reused it. Review active sessions and sign out unknown devices.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("I sent money").performScrollTo().performClick()
        compose.onNodeWithText("In India, call 1930 for financial cyber fraud and submit a complaint at cybercrime.gov.in.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Open dialer • 1930").performScrollTo().assertHasClickAction()
    }

    @Test fun paymentReviewTranslationsRemainUsableAtLargestTextSize() {
        val payment = base().copy(contentType = "qr", linkInspections = null, paymentReviews = PaymentQrParser.find("upi://pay?pa=shop@bank&pn=દુકાન&am=50"))
        compose.setContent { SentinelTheme { androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) { AnalysisResultContent(payment, {}) } } }
        for (language in AppLanguage.entries) {
            compose.runOnUiThread { DisplayPreferences.setLanguage(context, language); DisplayPreferences.setTextScale(1.5f) }
            compose.waitForIdle()
            compose.onNodeWithText(I18n.translate(context, "Review this UPI payment", language)).performScrollTo().assertIsDisplayed()
            screenshot("upi-review-${language.tag}-150")
            compose.onNodeWithText("shop@bank").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("દુકાન").assertExists()
            compose.onNodeWithText(I18n.translate(context, "Currency", language)).performScrollTo().assertExists()
            compose.onNodeWithText(I18n.translate(context, "Compare expected details", language)).performScrollTo().assertHasClickAction()
        }
    }
}
