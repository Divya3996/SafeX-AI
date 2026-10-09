package com.sentinel.ai.features

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.core.model.*
import com.sentinel.ai.protection.floating.*
import org.junit.*
import org.junit.Assert.*

class FloatingSessionRecoveryTest {
    @get:Rule val compose = createAndroidComposeRule<FloatingAssistantActivity>()
    private val model get() = ViewModelProvider(compose.activity)[FloatingSessionViewModel::class.java]
    @Before fun reset() { compose.runOnUiThread { model.reset(); DisplayPreferences.setLanguage(compose.activity, AppLanguage.ENGLISH); DisplayPreferences.setTextScale(1f) } }
    @After fun clear() { compose.runOnUiThread { model.reset() } }
    private fun readFixture() {
        compose.runOnUiThread {
            val bitmap = Bitmap.createBitmap(1400, 650, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap); canvas.drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 52f }
            canvas.drawText("Never share your OTP or password.", 40f, 140f, paint)
            canvas.drawText("Visit https://example.com/help", 40f, 260f, paint)
            canvas.drawText("Call your bank using its official app.", 40f, 380f, paint)
            val id = model.beginCapture(); CaptureSessionStore.deliver(id, bitmap)
        }
        compose.waitUntil(5000) { model.state.value.stage == FloatingStage.CROP }
        compose.onNodeWithText("Read selected area").assertIsDisplayed().performClick()
        compose.waitUntil(45000) { model.state.value.stage == FloatingStage.REVIEW }
        assertTrue(model.state.value.extraction!!.lines.size >= 2)
    }
    @Test fun emptySelectionNeverRestoresAllTextAndEditedDraftSurvivesModeChanges() {
        readFixture()
        compose.runOnUiThread { model.selectLine(0) }
        assertTrue(model.state.value.text.isNotBlank())
        compose.runOnUiThread { model.selectLine(0) }
        assertEquals("", model.state.value.text)
        assertEquals(ReviewScope.SELECTED_LINES, model.state.value.scope)
        compose.onNodeWithText("Analyze selected text").assertIsDisplayed().assertIsNotEnabled()
        compose.runOnUiThread { model.edit("My corrected draft"); model.useAllText(); model.useSelectedText() }
        assertEquals("", model.state.value.text)
        compose.runOnUiThread { model.useEditedText() }
        assertEquals("My corrected draft", model.state.value.text)
    }
    @Test fun resultBackAndRecropKeepTextAndOriginalFrameUntilClose() {
        readFixture()
        val original = model.state.value.original!!
        compose.runOnUiThread { model.edit("Never share your OTP."); model.analyze() }
        compose.waitUntil(15000) { model.state.value.stage == FloatingStage.RESULT }
        assertEquals(InputOrigin.SCREEN_CAPTURE, model.state.value.result!!.provenance!!.origin)
        assertEquals(ReviewScope.EDITED_TEXT, model.state.value.result!!.provenance!!.scope)
        compose.runOnUiThread { model.back(); model.changeArea(); model.back() }
        assertEquals(FloatingStage.REVIEW, model.state.value.stage)
        assertEquals("Never share your OTP.", model.state.value.text)
        assertFalse(original.isRecycled)
        compose.runOnUiThread { model.reset() }
        compose.waitUntil(5000) { original.isRecycled }
    }
    @Test fun activityRecreationRetainsPrivateDraftWithoutPersistingItInSavedState() {
        compose.runOnUiThread { model.open("paste"); model.edit("Authored private draft"); model.analyze() }
        compose.waitUntil(15000) { model.state.value.stage == FloatingStage.RESULT }
        val id = model.state.value.id; val result = model.state.value.result
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        assertEquals(id, model.state.value.id)
        assertEquals(FloatingStage.RESULT, model.state.value.stage)
        assertEquals(result, model.state.value.result)
        compose.onNodeWithText("Edit and rescan").assertIsDisplayed().performClick()
        assertEquals("Authored private draft", model.state.value.text)
    }
    @Test fun invalidImportAndCancelledCapturePreserveExistingReview() {
        compose.runOnUiThread { model.open("paste"); model.edit("Keep this draft"); model.importImage(Uri.parse("content://missing-safex-fixture/image")) }
        compose.waitUntil(5000) { !model.state.value.busy }
        assertEquals("Keep this draft", model.state.value.text)
        assertEquals(FloatingProblem.IMAGE, model.state.value.problem)
        compose.runOnUiThread { model.beginCapture(); model.captureDenied() }
        assertEquals("Keep this draft", model.state.value.text)
        assertEquals(FloatingStage.PASTE, model.state.value.stage)
    }
    @Test fun startingAndCancellingPasteRestoresThePreviousInput() {
        readFixture()
        val original = model.state.value.original
        compose.runOnUiThread { model.open("paste"); model.back() }
        assertEquals(FloatingStage.REVIEW, model.state.value.stage)
        assertSame(original, model.state.value.original)
        assertTrue(model.state.value.text.isNotBlank())
    }
    @Test fun resetDiscardsLateAnalysisInsteadOfReplacingNewInput() {
        compose.runOnUiThread { model.open("paste"); model.edit("Share your OTP immediately."); model.analyze(); model.reset("paste"); model.edit("New draft") }
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        assertEquals("New draft", model.state.value.text)
        assertEquals(FloatingStage.PASTE, model.state.value.stage)
        assertNull(model.state.value.result)
    }
    @Test fun qrReviewKeepsSuccessfulQrCoverageIndependentOfOtherScripts() {
        compose.runOnUiThread {
            val bitmap = android.graphics.BitmapFactory.decodeStream(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("synthetic-qr.png"))
            val id = model.beginCapture(); CaptureSessionStore.deliver(id, bitmap)
        }
        compose.waitUntil(5000) { model.state.value.stage == FloatingStage.CROP }
        compose.onNodeWithText("Read selected area").assertIsDisplayed().performClick()
        compose.waitUntil(45000) { model.state.value.stage == FloatingStage.REVIEW }
        assertTrue(model.state.value.extraction!!.qrCodes.isNotEmpty())
        compose.runOnUiThread { model.setScope(ReviewScope.QR) }
        compose.onNodeWithText("Analyze this QR content").assertIsDisplayed().performClick()
        compose.waitUntil(15000) { model.state.value.stage == FloatingStage.RESULT }
        assertEquals(ReviewScope.QR, model.state.value.result!!.provenance!!.scope)
        assertEquals(AssessmentCoverage.COMPLETE, model.state.value.result!!.coverageDetails!!.assessment)
        assertEquals(listOf(ExtractionEngine.QR), model.state.value.result!!.coverageDetails!!.engines.map { it.engine })
    }
    @Test fun pastedOriginalAndItsCorrectionHaveDifferentEditProvenance() {
        compose.runOnUiThread { model.open("paste"); model.paste("Never share your OTP."); model.analyze() }
        compose.waitUntil(15000) { model.state.value.stage == FloatingStage.RESULT }
        assertEquals(InputOrigin.CLIPBOARD, model.state.value.result!!.provenance!!.origin)
        assertFalse(model.state.value.result!!.provenance!!.edited)
        compose.runOnUiThread { model.back(); model.edit("Never share your PIN."); model.analyze() }
        compose.waitUntil(15000) { model.state.value.stage == FloatingStage.RESULT }
        assertTrue(model.state.value.result!!.provenance!!.edited)
    }
    @Test fun memoryPressureReleasesPixelsWhileKeepingTheReviewedDraft() {
        readFixture(); val pixels = model.state.value.original!!
        compose.runOnUiThread { model.edit("Retained corrected draft"); model.foreground = false; model.controller.releaseForMemoryPressure(); model.foreground = true }
        assertEquals("Retained corrected draft", model.state.value.text)
        assertNull(model.state.value.original); assertNull(model.state.value.bitmap)
        compose.waitUntil(5000) { pixels.isRecycled }
        compose.onNodeWithText("Analyze reviewed message").assertIsDisplayed().performClick()
        compose.waitUntil(15000) { model.state.value.stage == FloatingStage.RESULT }
    }
    @Test fun captureFailureNoticeMatchesTheRestoredInput() {
        compose.runOnUiThread { model.open("paste"); model.edit("Retained input"); val id = model.beginCapture(); CaptureSessionStore.fail(id, "Screen capture stopped. Choose a new capture or paste the content.") }
        compose.waitUntil(5000) { model.state.value.problem == FloatingProblem.CAPTURE }
        assertEquals("Retained input", model.state.value.text)
        val failed = (CaptureSessionStore.state.value as CaptureSessionStore.State.Failed).id
        assertTrue(model.controller.matches(failed))
        compose.runOnUiThread { model.reset("paste"); model.edit("New input") }
        assertFalse(model.controller.matches(failed))
    }
}
