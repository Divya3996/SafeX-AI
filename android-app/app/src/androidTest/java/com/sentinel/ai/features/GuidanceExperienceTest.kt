package com.sentinel.ai.features

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.core.i18n.I18n
import com.sentinel.ai.ui.MainActivity
import com.sentinel.ai.ui.guidance.*
import com.sentinel.ai.ui.screens.help.OfficialIncidentHelp
import org.junit.*
import org.junit.Assert.*

class GuidanceExperienceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: androidx.test.core.app.ActivityScenario<MainActivity>
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun waitTag(tag: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun permissionFinish() {
        compose.onNodeWithText(I18n.translate(context, "Continue to SafeX AI")).performScrollTo().performClick()
    }
    private fun screenshot(name: String) {
        val file = java.io.File(context.getExternalFilesDir(null), "guidance-screenshots/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Before fun firstLaunch() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            DisplayPreferences.setLanguage(context, AppLanguage.ENGLISH)
            DisplayPreferences.setTextScale(1f)
            context.getSharedPreferences("safex_guidance", 0).edit().clear().commit()
            context.getSharedPreferences("sentinel_onboarding", 0).edit().putBoolean("first_launch", true).commit()
        }
        // Start a fresh Activity rather than restoring a prior navigation back stack.
        scenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
        waitTag("welcome_next")
    }
    @After fun restore() {
        scenario.close()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            DisplayPreferences.setLanguage(context, AppLanguage.ENGLISH); DisplayPreferences.setTextScale(1f)
            GuidancePreferences.finishIntro(context, false)
            context.getSharedPreferences("sentinel_onboarding", 0).edit().putBoolean("first_launch", false).commit()
        }
    }
    @Test fun introductionLanguagesAndLargeReadingSizeRemainUsable() {
        compose.onNodeWithTag("welcome_next").performClick()
        for (language in AppLanguage.entries) {
            compose.onNodeWithTag("language_${language.tag}").performScrollTo().performClick()
            compose.onNodeWithTag("welcome_title").assertTextEquals(I18n.translate(context, "Your language. Your reading size.", language))
            InstrumentationRegistry.getInstrumentation().runOnMainSync { DisplayPreferences.setTextScale(1.5f) }
            compose.onNodeWithTag("welcome_next").assertIsDisplayed()
            compose.onNodeWithTag("welcome_incident_help").assertIsDisplayed()
            screenshot("introduction-${language.tag}-150")
        }
        compose.onNodeWithTag("welcome_skip").performClick()
        permissionFinish()
        assertFalse(GuidancePreferences.needsIntro(context))
        scenario.recreate()
        compose.onNodeWithTag("welcome_next").assertDoesNotExist()
        compose.onNodeWithTag("feature_tour").assertDoesNotExist()
    }
    @Test fun tourControlsWorkInEveryLanguageAtLargestTextSize() {
        repeat(4) { compose.onNodeWithTag("welcome_next").performClick() }
        permissionFinish()
        waitTag("tour_next")
        for (language in AppLanguage.entries) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { DisplayPreferences.setLanguage(context, language); DisplayPreferences.setTextScale(1.5f) }
            compose.onNodeWithText(I18n.translate(context, AppGuideSteps[0].title, language)).assertExists()
            compose.onNodeWithTag("tour_next").assertIsDisplayed()
            compose.onNodeWithTag("tour_skip").assertIsDisplayed()
            compose.onNodeWithTag("tour_incident_help").assertIsDisplayed()
            screenshot("tour-${language.tag}-150")
            compose.onNodeWithTag("tour_next").performClick()
            compose.onNodeWithTag("tour_back").performClick()
        }
        compose.onNodeWithTag("tour_incident_help").performClick()
        compose.onNodeWithTag("help_dial_1930").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("feature_tour").assertDoesNotExist()
    }
    @Test fun fullTourVisitsEveryRealControlAndCanBeReplayed() {
        repeat(4) { compose.onNodeWithTag("welcome_next").performClick() }
        permissionFinish()
        for ((index, step) in AppGuideSteps.withIndex()) {
            waitTag("guide_target_${step.target}")
            compose.waitForIdle()
            compose.onNodeWithTag("feature_tour").assertExists()
            compose.onNodeWithText(step.title).assertExists()
            compose.waitUntil(5000) { runCatching { compose.onNodeWithTag("guide_target_${step.target}").assertIsDisplayed() }.isSuccess }
            if (index == 4) {
                compose.onNodeWithTag("tour_back").performClick()
                compose.onNodeWithText(AppGuideSteps[3].title).assertExists()
                compose.onNodeWithTag("tour_next").performClick()
            }
            compose.onNodeWithTag("tour_next").performClick()
        }
        compose.onNodeWithTag("feature_tour").assertDoesNotExist()
        compose.onAllNodesWithText("Settings").onFirst().performClick()
        compose.onNodeWithText("Open feature guide").performScrollTo().performClick()
        compose.onNodeWithTag("replay_full_tour").performClick()
        waitTag("tour_skip")
        compose.onNodeWithTag("tour_skip").performClick()
        compose.onNodeWithTag("feature_tour").assertDoesNotExist()
    }
    @Test fun urgentHelpIsReachableWithoutFinishingIntroAndDialingRequiresConfirmation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(IntentFilter(Intent.ACTION_DIAL).apply { addDataScheme("tel") }, Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        try {
            compose.onNodeWithTag("welcome_incident_help").performClick()
            compose.onNodeWithTag("help_dial_1930").performScrollTo().performClick()
            assertEquals(0, monitor.hits)
            compose.onNodeWithText("Cancel").performClick()
            assertEquals(0, monitor.hits)
            for (number in listOf("1930", "112")) {
                compose.onNodeWithTag("help_dial_$number").performScrollTo().performClick()
                compose.onNodeWithTag("confirm_help_dial").performClick()
                assertEquals(Intent.ACTION_DIAL, OfficialIncidentHelp.dialIntent(number).action)
                assertEquals("tel:$number", OfficialIncidentHelp.dialIntent(number).dataString)
            }
            assertEquals(2, monitor.hits)
            compose.onNodeWithTag("help_other_country").performScrollTo().performClick()
            compose.onNodeWithTag("help_dial_1930").assertDoesNotExist()
            compose.onNodeWithTag("help_dial_112").assertDoesNotExist()
            assertTrue(GuidancePreferences.needsIntro(context))
        } finally { instrumentation.removeMonitor(monitor) }
    }
}
