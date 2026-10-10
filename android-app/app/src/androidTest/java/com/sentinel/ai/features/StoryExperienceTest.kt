package com.sentinel.ai.features

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.sentinel.ai.core.data.local.SentinelDatabase
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.core.i18n.I18n
import com.sentinel.ai.core.story.*
import com.sentinel.ai.ml.PipelineEntryPoint
import com.sentinel.ai.ui.MainActivity
import com.sentinel.ai.ui.guidance.GuidancePreferences
import com.sentinel.ai.ui.screens.story.*
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class StoryExperienceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val controller get() = EntryPointAccessors.fromApplication(context, StoryEntryPoint::class.java).story()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun idle() { compose.waitUntil(45000) { !controller.state.value.busy }; compose.waitForIdle() }
    private fun tag(name: String) = compose.onNodeWithTag(name)
    private fun add(text: String) {
        main { controller.editInput(text) }
        tag("story_add").assertIsDisplayed().performClick(); idle()
        assertNull(controller.state.value.error)
    }
    private fun screenshot(name: String) {
        // Test-only authored fixtures. Dialogs have their own secure windows;
        // clear both for capture, then restore every window's original flags.
        if (android.os.Build.VERSION.SDK_INT < 29) return
        val secured = mutableListOf<Pair<android.view.View, Int>>()
        main {
            android.view.inspector.WindowInspector.getGlobalWindowViews().forEach { view ->
                val params = view.layoutParams as? android.view.WindowManager.LayoutParams
                if (params != null && params.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0) {
                    secured += view to params.flags
                    params.flags = params.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE.inv()
                    view.context.getSystemService(android.view.WindowManager::class.java).updateViewLayout(view, params)
                }
            }
        }
        val file = java.io.File(context.getExternalFilesDir(null), "story-screenshots/$name.png")
        file.parentFile!!.mkdirs()
        try {
            compose.waitForIdle()
            val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("Authored screenshot unavailable")
            try { file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } }
            finally { bitmap.recycle() }
        } finally {
            main { secured.forEach { (view, flags) ->
                val params = view.layoutParams as android.view.WindowManager.LayoutParams
                params.flags = flags
                view.context.getSystemService(android.view.WindowManager::class.java).updateViewLayout(view, params)
            } }
        }
    }
    @Before fun launch() {
        main {
            DisplayPreferences.setLanguage(context, AppLanguage.ENGLISH); DisplayPreferences.setTextScale(1f)
            GuidancePreferences.finishIntro(context, false)
            context.getSharedPreferences("sentinel_onboarding", 0).edit().putBoolean("first_launch", false).commit()
            context.getSharedPreferences("safex_story", 0).edit().putBoolean("guide_seen", true).commit()
            controller.clear()
        }
        scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java).putExtra(StoryNavigation.REQUEST, true))
        compose.waitUntil(15000) { compose.onAllNodesWithTag("guide_target_story.summary").fetchSemanticsNodes().isNotEmpty() }
        scenario.onActivity { assertTrue(it.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0) }
    }
    @After fun cleanup() {
        scenario.close()
        main { controller.clear(); DisplayPreferences.setLanguage(context, AppLanguage.ENGLISH); DisplayPreferences.setTextScale(1f) }
        runBlocking { StoryVault(context).deleteAll() }
    }
    @Test fun threeLanguageStoriesRunActualDetectorAtLargestTextSizeAndExplainExactEvidence() {
        for (language in AppLanguage.entries) {
            main { controller.clear(); DisplayPreferences.setLanguage(context, language); DisplayPreferences.setTextScale(1.5f) }
            StoryExamples.task(language.tag).forEach(::add)
            tag("story_add").assertIsDisplayed(); tag("story_help").assertIsDisplayed(); tag("story_saved").assertIsDisplayed()
            tag("guide_target_story.summary").performScrollTo(); screenshot("${language.tag}-150-summary")
            tag("story_finding_task_fee").performScrollTo().performClick()
            val finding = controller.state.value.analysis.findings.single { it.id == "task_fee" }
            for (signal in finding.evidence) {
                val text = controller.state.value.case.items.single { it.id == signal.itemId }.text.substring(signal.start, signal.end)
                compose.onNode(hasText(text) and hasAnyAncestor(isDialog())).performScrollTo().assertIsDisplayed()
            }
            screenshot("${language.tag}-150-evidence")
            compose.onNodeWithText(I18n.translate(context, "Done", language)).performClick()
        }
    }
    @Test fun timelineReorderCorrectionAndRemovalRecomputeActualFindings() {
        StoryExamples.task("en").forEach(::add)
        tag("story_up_2").performScrollTo().performClick()
        tag("story_up_1").performScrollTo().performClick()
        assertFalse(controller.state.value.analysis.findings.any { it.id == "task_fee" })
        tag("story_down_0").performScrollTo().performClick()
        assertTrue(controller.state.value.analysis.findings.any { it.id == "task_fee" })
        tag("story_edit_1").performScrollTo().performClick()
        main { controller.editInput("Your salary will arrive tomorrow.") }
        tag("story_add").performClick(); idle()
        assertFalse(controller.state.value.analysis.findings.any { it.id == "task_fee" })
        assertTrue(controller.state.value.case.items[1].edited)
        tag("story_remove_1").performScrollTo().performClick(); tag("story_confirm").performClick()
        assertEquals(2, controller.state.value.case.items.size)
    }
    @Test fun legitimateComparisonsRemainFreeOfInventedSequenceFindingsAndPrivateHistoryWrites() = runBlocking {
        val db = SentinelDatabase.getInstance(context).threatDao()
        val records = db.getAllThreatRecords()
        val beforeDb = records.map { it.id }.toSet()
        val persistedScans = records.filter { it.recordType == com.sentinel.ai.core.data.local.ThreatRecordEntity.TYPE_SCAN_RESULT }.map { it.id }.toSet()
        // Journal restoration is asynchronous, including when the app already has history.
        compose.waitUntil(15000) { ThreatJournal.scanResults.value.map { it.id }.toSet() == persistedScans }
        val before = ThreatJournal.scanResults.value.map { it.id }.toSet()
        for (language in listOf("en", "hi", "gu")) {
            main { controller.clear() }
            StoryExamples.legitimate(language).forEach(::add)
            assertFalse(controller.state.value.analysis.findings.any { !it.id.startsWith("item_") })
        }
        assertEquals(before, ThreatJournal.scanResults.value.map { it.id }.toSet())
        assertEquals(beforeDb, db.getAllThreatRecords().map { it.id }.toSet())
    }
    @Test fun savedCaseReopensAndRedactedPreviewDoesNotIncludeRawContent() {
        add("PrivateName 9999988888 SecretXYZ https://private.example/?token=SecretXYZ")
        tag("story_save").performScrollTo().performClick(); idle()
        val id = controller.state.value.case.id
        assertNotNull(controller.state.value.case.savedAt)
        tag("story_export").performScrollTo().performClick()
        val preview = tag("story_export_preview").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString()
        for (value in listOf("PrivateName", "9999988888", "SecretXYZ", "private.example", id)) assertFalse(preview.contains(value))
        compose.onNodeWithText("Cancel").performClick()
        tag("story_clear").performScrollTo().performClick()
        assertTrue(controller.state.value.case.items.isEmpty())
        tag("story_saved").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("story_open_$id").fetchSemanticsNodes().isNotEmpty() }
        tag("story_open_$id").performClick(); idle()
        assertEquals(id, controller.state.value.case.id); assertEquals(1, controller.state.value.case.items.size)
    }
    @Test fun lockClearsOpenDraftAndDialogWhileExplicitSavedSnapshotRemains() {
        add("Private evidence"); tag("story_save").performScrollTo().performClick(); idle()
        val id = controller.state.value.case.id
        tag("story_export").performScrollTo().performClick(); tag("story_export_preview").assertExists()
        val device = androidx.test.uiautomator.UiDevice.getInstance(instrumentation)
        try {
            device.sleep()
            compose.waitUntil(5000) { controller.state.value.case.items.isEmpty() }
        } finally {
            device.wakeUp()
            android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard")).use { it.readBytes() }
        }
        compose.waitForIdle()
        tag("story_export_preview").assertDoesNotExist(); assertTrue(controller.state.value.case.items.isEmpty())
        assertEquals(1, runBlocking { StoryVault(context).open(id).items.size })
    }
    @Test fun floatingResultHandoffRequiresReviewAndKeepsExistingVerdict() = runBlocking {
        val repo = EntryPointAccessors.fromApplication(context, PipelineEntryPoint::class.java).repository()
        val result = repo.analyzeTextPrivately("Urgent bank support. Share your OTP immediately.")
        main { StoryNavigation.offer(context, result, true) }; compose.waitForIdle()
        assertEquals(result.target, controller.state.value.input); assertTrue(controller.state.value.case.items.isEmpty())
        assertEquals(StorySource.FLOATING, controller.state.value.source)
        main { context.getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(android.content.ClipData.newPlainText("Authored empty clipboard", "")) }
        compose.onNodeWithText("Paste").performScrollTo().performClick(); compose.waitForIdle()
        assertEquals(result.target, controller.state.value.input)
        assertEquals(result, controller.state.value.importedResult)
        tag("story_add").performClick(); idle()
        assertEquals(result.decision, controller.state.value.case.items.single().result!!.decision)
        assertEquals(result.id, controller.state.value.case.items.single().result!!.id)
    }
    @Test fun qrImageImportsIntoReviewBeforeAnyEvidenceIsAddedAndBadUriRecovers() {
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "safex-story-authored-qr.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SafeX-test")
        })!!
        try {
            context.contentResolver.openOutputStream(uri)!!.use { output -> instrumentation.context.assets.open("synthetic-qr.png").use { it.copyTo(output) } }
            main { repeat(8) { controller.importImage(uri, true); controller.cancel() } }
            main { controller.importImage(uri, true) }; idle()
            assertNull(controller.state.value.error); assertTrue(controller.state.value.case.items.isEmpty())
            assertEquals(StorySource.QR, controller.state.value.source)
            assertTrue(controller.state.value.input.isNotBlank())
            tag("story_add").performClick(); idle(); assertEquals(1, controller.state.value.case.items.size)
            main { controller.importImage(android.net.Uri.parse("content://missing.safex.example/absent"), false) }; idle()
            assertNotNull(controller.state.value.error); assertFalse(controller.state.value.busy)
            assertEquals(1, controller.state.value.case.items.size)
        } finally { context.contentResolver.delete(uri, null, null) }
    }
    @Test fun recreationKeepsRamDraftAndHelpIsReachable() {
        add("Reviewed evidence"); scenario.recreate()
        tag("guide_target_story.summary").assertExists(); assertEquals(1, controller.state.value.case.items.size)
        tag("story_help").performClick()
        compose.onNodeWithTag("help_dial_1930").performScrollTo().assertIsDisplayed()
    }
    @Test fun homeEntryAndKeyboardKeepThePrimaryActionUsable() {
        androidx.test.uiautomator.UiDevice.getInstance(instrumentation).pressBack()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Review the whole situation").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Review the whole situation").performScrollTo().performClick()
        tag("story_input").performScrollTo().performClick().performTextInput("Authored message from keyboard")
        tag("story_add").assertIsDisplayed().performClick(); idle()
        assertEquals("Authored message from keyboard", controller.state.value.case.items.single().text)
    }
    @Test fun screenshotRecognitionStaysEditableUntilReviewedAndAdded() {
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "safex-story-authored-text.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SafeX-test")
        })!!
        try {
            val bitmap = android.graphics.Bitmap.createBitmap(1000, 360, android.graphics.Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(android.graphics.Color.WHITE)
                val canvas = android.graphics.Canvas(bitmap)
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 48f }
                canvas.drawText("Your job interview is on Tuesday.", 35f, 110f, paint)
                canvas.drawText("There is no fee for this job.", 35f, 220f, paint)
                context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            } finally { bitmap.recycle() }
            main { controller.importImage(uri, false) }; idle()
            assertNull(controller.state.value.error); assertEquals(StorySource.IMAGE, controller.state.value.source)
            assertTrue(controller.state.value.input.contains("interview", true)); assertTrue(controller.state.value.case.items.isEmpty())
            main { controller.editInput(controller.state.value.input + "\nReviewed correction.") }
            tag("story_add").performClick(); idle()
            assertTrue(controller.state.value.case.items.single().edited)
            assertFalse(controller.state.value.analysis.findings.any { it.id == "task_fee" })
        } finally { context.contentResolver.delete(uri, null, null) }
    }
}
