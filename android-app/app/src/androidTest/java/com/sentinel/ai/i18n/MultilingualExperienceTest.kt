package com.sentinel.ai.i18n

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.core.i18n.I18n
import com.sentinel.ai.ui.MainActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Before
import org.junit.Test

/** Tests visible selection/recreation and the same translations used by notification warnings. */
class MultilingualExperienceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun enterApp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        compose.runOnUiThread { DisplayPreferences.setLanguage(context, AppLanguage.ENGLISH); DisplayPreferences.setTextScale(1f) }
        compose.waitForIdle()
        if (compose.onAllNodesWithTag("welcome_skip").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("welcome_skip").performClick()
        }
        if (compose.onAllNodesWithText("Continue to SafeX AI").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Continue to SafeX AI").performScrollTo().performClick()
        }
    }

    @Test fun languagesAndReadingSizeUpdateAndSurviveActivityRecreation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        compose.runOnUiThread { DisplayPreferences.setLanguage(context, AppLanguage.ENGLISH); DisplayPreferences.setTextScale(1f) }
        compose.waitForIdle()
        if (compose.onAllNodesWithTag("language_hi").fetchSemanticsNodes().isEmpty()) {
            compose.onAllNodesWithText("Settings").onFirst().performClick()
        }
        compose.onNodeWithTag("language_hi").performScrollTo().performClick()
        compose.onNodeWithText("भाषा और पढ़ने की सुविधा").assertExists()
        assertEquals(AppLanguage.HINDI, DisplayPreferences.current.language)
        compose.onNodeWithTag("language_gu").performScrollTo().performClick()
        compose.onNodeWithText("ભાષા અને વાંચવાની સુવિધા").assertExists()
        compose.onNodeWithTag("text_size_slider").performScrollTo().performTouchInput {
            down(center)
            moveTo(androidx.compose.ui.geometry.Offset(width - 2f, height / 2f), delayMillis = 200)
        }
        compose.waitForIdle()
        // Applying the scale while the pointer is down would move the slider under the finger.
        assertEquals(1f, DisplayPreferences.current.textScale, 0f)
        compose.onNodeWithTag("text_size_slider").performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(1.5f, DisplayPreferences.current.textScale, .01f)
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        // Navigation state may return home after recreation; the persisted settings are independent.
        assertEquals(AppLanguage.GUJARATI, DisplayPreferences.current.language)
        assertEquals(1.5f, context.getSharedPreferences("safex_display", 0).getFloat("textScale", 0f), 0f)
        assertEquals("gu", context.getSharedPreferences("safex_display", 0).getString("language", ""))
        compose.runOnUiThread { DisplayPreferences.setTextScale(1f); DisplayPreferences.setLanguage(context, AppLanguage.ENGLISH) }
    }

    @Test fun dynamicRiskWarningsTranslateWhileUrlsAndNumbersStayIntact() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        for (language in listOf(AppLanguage.HINDI, AppLanguage.GUJARATI)) {
            val risk = I18n.translate(context, "Risk index 88 / 100", language)
            assertTrue(risk.contains("88")); assertFalse(risk.contains("Risk"))
            val reason = I18n.translate(context, "Possible paypal brand impersonation", language)
            assertTrue(reason.contains("paypal")); assertFalse(reason.contains("impersonation"))
            for (newReason in listOf(
                "Short link hides its final destination; it is not automatically fraudulent",
                "Numeric host uses an unusual IP address encoding",
                "Message has more than eight distinct links; additional destinations were not analyzed"
            )) assertNotEquals(newReason, I18n.translate(context, newReason, language))
            val destination = "https://example.com/private?token=abc"
            assertEquals(destination, I18n.translate(context, destination, language))
            val warning = I18n.translate(context, "Risk Level: Critical", language)
            assertFalse(warning.contains("Critical")); assertFalse(warning.contains("Risk"))
            val guidance = I18n.translate(context, "Do not share passwords, PINs or one-time codes. Contact the organization through its official app.", language)
            assertFalse(guidance.contains("share"))
        }
    }
}
