package com.sentinel.ai.core.story

import android.net.Uri
import com.sentinel.ai.core.data.ScanRepository
import com.sentinel.ai.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class StoryControllerTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var controller: StoryController
    private val store = MemoryStore()
    private val repository = PrivateRepository()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        controller = StoryController(repository, store, object : StoryImageReader {
            override suspend fun read(uri: Uri, qrOnly: Boolean): StoryImageContent = error("Not used")
        })
        controller.setForeground(true)
    }
    @After fun cleanup() { controller.clear(); Dispatchers.resetMain() }
    private fun awaitIdle() = runBlocking {
        withTimeout(5000) { controller.state.first { !it.busy } }
    }
    private fun add(text: String, source: StorySource = StorySource.MESSAGE) {
        controller.selectSource(source); controller.editInput(text); controller.addReviewed(); awaitIdle()
        assertNull(controller.state.value.error)
    }
    @Test fun onlyExplicitlyReviewedEvidenceIsAddedThroughPrivateAnalysis() {
        controller.prepareHandoff("Your work commission is ready.", StorySource.FLOATING)
        assertTrue(controller.state.value.case.items.isEmpty())
        controller.addReviewed(); awaitIdle()
        add("Pay ₹500 to unlock earnings.")
        assertTrue(controller.state.value.analysis.findings.any { it.id == "task_fee" })
        assertEquals(2, repository.calls)
        assertTrue(controller.state.value.input.isBlank())
    }
    @Test fun editsKeepIdentityAndRecomputeRatherThanAccumulatingOldFindings() {
        add("Your work commission is ready."); add("Pay ₹500 to unlock earnings.")
        val old = controller.state.value.case.items.last()
        controller.editItem(old.id); controller.editInput("The salary arrives tomorrow."); controller.addReviewed(); awaitIdle()
        val edited = controller.state.value.case.items.last()
        assertEquals(old.id, edited.id); assertEquals(old.addedAt, edited.addedAt); assertTrue(edited.edited)
        assertFalse(controller.state.value.analysis.findings.any { it.id == "task_fee" })
    }
    @Test fun reorderAndRemoveWithdrawUnsupportedSequence() {
        add("Your work commission is ready."); add("Pay ₹500 to unlock earnings.")
        val id = controller.state.value.case.items.last().id
        controller.move(id, -1)
        assertFalse(controller.state.value.analysis.findings.any { it.id == "task_fee" })
        controller.move(id, 1)
        assertTrue(controller.state.value.analysis.findings.any { it.id == "task_fee" })
        controller.remove(id)
        assertFalse(controller.state.value.analysis.findings.any { it.id == "task_fee" })
    }
    @Test fun duplicateWhitespaceContentIsRejectedBeforeAnotherScan() {
        add("Work details")
        controller.editInput("Work  details"); controller.addReviewed()
        assertEquals(1, repository.calls); assertEquals(1, controller.state.value.case.items.size)
        assertTrue(controller.state.value.error!!.contains("already"))
    }
    @Test fun visuallySimilarUnicodeInputsStillReceiveIndependentPrivateChecks() {
        add("https://example.com/path", StorySource.LINK)
        add("https://ｅｘａｍｐｌｅ.com/path", StorySource.LINK)
        assertEquals(2, repository.calls); assertEquals(2, controller.state.value.case.items.size)
    }
    @Test fun removingOtherEvidenceDoesNotLoseTheItemBeingEdited() {
        add("First item"); add("Second item")
        val items = controller.state.value.case.items
        controller.editItem(items[1].id); controller.editInput("Corrected second item"); controller.remove(items[0].id)
        assertEquals(items[1].id, controller.state.value.editingId)
        controller.addReviewed(); awaitIdle()
        assertEquals(1, controller.state.value.case.items.size)
        assertEquals(items[1].id, controller.state.value.case.items.single().id)
        assertEquals("Corrected second item", controller.state.value.case.items.single().text)
    }
    @Test fun eightItemAndCharacterLimitsAreEnforcedBeforeAnalysis() {
        repeat(8) { add("Distinct evidence $it") }
        controller.editInput("Ninth evidence"); controller.addReviewed()
        assertEquals(8, repository.calls); assertTrue(controller.state.value.error!!.contains("eight"))
        controller.clear(); controller.editInput("x".repeat(12001)); controller.addReviewed()
        assertEquals(8, repository.calls); assertTrue(controller.state.value.error!!.contains("12,000"))
    }
    @Test fun aggregateBudgetPreventsLargeCases() {
        repeat(4) { add("$it" + "x".repeat(11999)) }
        controller.editInput("overflow"); controller.addReviewed()
        assertEquals(4, repository.calls); assertTrue(controller.state.value.error!!.contains("48,000"))
    }
    @Test fun handoffDoesNotOverwritePendingInputAndCachedWarningsArePreserved() {
        val text = "Suspicious authored content"
        val result = repository.result(text).copy(decision = ProtectionDecision.BLOCK, riskLevel = RiskLevel.RED)
        assertTrue(controller.prepareHandoff(text, StorySource.RESULT, result = result))
        assertFalse(controller.prepareHandoff("another message", StorySource.MESSAGE))
        controller.addReviewed(); awaitIdle()
        assertEquals(0, repository.calls); assertEquals(StoryConcern.HIGH, controller.state.value.analysis.concern)
    }
    @Test fun editingAHandoffForcesFreshPrivateAnalysis() {
        controller.prepareHandoff("Original", StorySource.RESULT, result = repository.result("Original"))
        controller.editInput("Corrected"); controller.addReviewed(); awaitIdle()
        assertEquals(1, repository.calls); assertEquals("Corrected", controller.state.value.case.items.single().text)
    }
    @Test fun unchangedInputAndSourcePreserveTheOriginalHandoffWarning() {
        val text = "Reviewed authored handoff"
        val result = repository.result(text).copy(decision = ProtectionDecision.BLOCK, riskLevel = RiskLevel.RED)
        assertTrue(controller.prepareHandoff(text, StorySource.QR, result = result))
        controller.editInput(text)
        controller.selectSource(StorySource.QR)
        assertEquals(result, controller.state.value.importedResult)
        controller.addReviewed(); awaitIdle()
        assertEquals(0, repository.calls)
        assertEquals(result.id, controller.state.value.case.items.single().result!!.id)
        assertEquals(StoryConcern.HIGH, controller.state.value.analysis.concern)
    }
    @Test fun linksAndQrUseTheirOwnPrivatePipelines() {
        add("https://demo.example", StorySource.LINK)
        add("upi://pay?pa=demo@invalid&am=1", StorySource.QR)
        assertEquals(listOf("link", "qr"), repository.kinds)
    }
    @Test fun savingRequiresCompletedReviewAndStoresAnIndependentSnapshot() {
        add("Reviewed evidence"); controller.editInput("Not yet reviewed"); controller.save()
        assertTrue(store.values.isEmpty())
        controller.editInput(""); controller.save(); awaitIdle()
        val id = controller.state.value.case.id
        assertFalse(controller.state.value.dirty); assertNotNull(controller.state.value.case.savedAt)
        controller.remove(controller.state.value.case.items.single().id)
        assertEquals(1, store.values.getValue(id).items.size)
        controller.open(id); awaitIdle()
        assertEquals("Reviewed evidence", controller.state.value.case.items.single().text)
    }
    @Test fun saveFailureKeepsThePrivateDraft() {
        add("Reviewed evidence"); store.failSave = true; controller.save(); awaitIdle()
        assertEquals(1, controller.state.value.case.items.size); assertTrue(controller.state.value.dirty)
        assertNotNull(controller.state.value.error); assertNull(controller.state.value.case.savedAt)
    }
    @Test fun deletionRemovesSavedDataAndMarksOpenDraftUnsaved() {
        add("Reviewed evidence"); controller.save(); awaitIdle(); controller.deleteSaved(); awaitIdle()
        assertTrue(store.values.isEmpty()); assertEquals(1, controller.state.value.case.items.size)
        assertTrue(controller.state.value.dirty); assertNull(controller.state.value.case.savedAt)
    }
    @Test fun lockErasesAllDraftFieldsButKeepsAnExplicitSavedSnapshot() {
        add("Private evidence"); controller.save(); awaitIdle()
        controller.editInput("Private pending input"); controller.lock()
        assertTrue(controller.state.value.input.isBlank()); assertTrue(controller.state.value.case.items.isEmpty())
        assertNull(controller.state.value.importedResult); assertTrue(controller.state.value.listing.cases.isEmpty())
        assertEquals(1, store.values.size)
    }
    @Test fun awayExpiryClearsDraftAndReturningBeforeDeadlineCancelsIt() {
        add("Private evidence"); controller.setForeground(false)
        dispatcher.scheduler.advanceTimeBy(119999); dispatcher.scheduler.runCurrent()
        assertEquals(1, controller.state.value.case.items.size)
        controller.setForeground(true); dispatcher.scheduler.advanceTimeBy(120001); dispatcher.scheduler.runCurrent()
        assertEquals(1, controller.state.value.case.items.size)
        controller.setForeground(false); dispatcher.scheduler.advanceTimeBy(120000); dispatcher.scheduler.runCurrent()
        assertTrue(controller.state.value.case.items.isEmpty())
    }
    @Test fun timeoutDiscardsLateNativeResultsWithoutErasingReviewInput() {
        repository.nativeDelay = 30000
        controller.editInput("Private delayed content"); controller.addReviewed()
        assertTrue(controller.state.value.busy)
        dispatcher.scheduler.advanceTimeBy(20000); dispatcher.scheduler.runCurrent()
        assertFalse(controller.state.value.busy); assertTrue(controller.state.value.error!!.contains("timed out"))
        dispatcher.scheduler.advanceTimeBy(10000); dispatcher.scheduler.runCurrent()
        assertTrue(controller.state.value.case.items.isEmpty()); assertEquals("Private delayed content", controller.state.value.input)
    }
    @Test fun cancelAndClearCannotBeUndoneByUninterruptibleWork() {
        repository.nativeDelay = 30000
        controller.editInput("Private delayed content"); controller.addReviewed(); controller.cancel()
        assertFalse(controller.state.value.busy); controller.clear()
        dispatcher.scheduler.advanceTimeBy(30000); dispatcher.scheduler.runCurrent()
        assertTrue(controller.state.value.case.items.isEmpty()); assertTrue(controller.state.value.input.isEmpty())
    }
    @Test fun sourceChangeKeepsTextButInvalidatesAnImportedVerdict() {
        controller.prepareHandoff("https://demo.example", StorySource.MESSAGE, result = repository.result("https://demo.example"))
        controller.selectSource(StorySource.LINK)
        assertEquals("https://demo.example", controller.state.value.input); assertNull(controller.state.value.importedResult)
        controller.addReviewed(); awaitIdle(); assertEquals(listOf("link"), repository.kinds)
    }
    private class PrivateRepository : ScanRepository {
        var calls = 0; var nativeDelay = 0L; val kinds = mutableListOf<String>()
        fun result(text: String) = ScanResult(id = "fixture", source = "Private test", riskLevel = RiskLevel.GREEN,
            riskScore = 0f, explanation = "Authored test", timestamp = 0, decision = ProtectionDecision.ALLOW, target = text)
        override suspend fun scanLink(link: String): ScanResult = error("History-writing analysis is forbidden")
        override suspend fun scanFile(uri: Uri): ScanResult = error("History-writing analysis is forbidden")
        override suspend fun scanText(text: String): ScanResult = error("History-writing analysis is forbidden")
        private suspend fun analyze(text: String, kind: String): ScanResult {
            calls++; kinds += kind
            if (nativeDelay > 0) withContext(NonCancellable) { delay(nativeDelay) }
            return result(text)
        }
        override suspend fun analyzeTextPrivately(text: String) = analyze(text, "text")
        override suspend fun analyzeLinkPrivately(link: String) = analyze(link, "link")
        override suspend fun analyzeQrPrivately(content: String) = analyze(content, "qr")
    }
    private class MemoryStore : StoryStore {
        val values = linkedMapOf<String, StoryCase>(); var failSave = false
        override suspend fun list() = StoryListing(values.values.map { SavedStory(it.id, it.title, it.items.size, it.savedAt!!, it.synthetic) })
        override suspend fun save(case: StoryCase): StoryCase {
            check(!failSave)
            return case.copy(savedAt = System.currentTimeMillis()).also { values[it.id] = it }
        }
        override suspend fun open(id: String) = values.getValue(id)
        override suspend fun delete(id: String) { values.remove(id) }
        override suspend fun deleteAll() { values.clear() }
    }
}
