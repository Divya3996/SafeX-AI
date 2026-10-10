package com.sentinel.ai.core.story

import com.sentinel.ai.core.data.ScanRepository
import com.sentinel.ai.core.model.ScanResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class StoryState(
    val case: StoryCase = StoryCase(), val analysis: StoryAnalysis = StoryAnalysis(StoryConcern.CONTEXT, emptyList()),
    val input: String = "", val source: StorySource = StorySource.MESSAGE, val edited: Boolean = false,
    val extractionNote: String? = null, val qrChoices: List<String> = emptyList(), val editingId: String? = null,
    val busy: Boolean = false, val saving: Boolean = false, val error: String? = null, val notice: String? = null,
    val dirty: Boolean = false, val listing: StoryListing = StoryListing(emptyList()),
    val demoSteps: List<String> = emptyList(), val demoIndex: Int = -1,
    val importedResult: ScanResult? = null
)

/** Application-owned temporary draft. No raw draft enters SavedStateHandle or scan history. */
@Singleton
class StoryController @Inject constructor(
    private val repository: ScanRepository, private val store: StoryStore, private val images: StoryImageReader
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutable = MutableStateFlow(StoryState())
    val state = mutable.asStateFlow()
    private var worker: Job? = null
    private var expiry: Job? = null
    private var generation = 0L
    private var backgroundAt = 0L
    private var foreground = false
    private fun now() = System.nanoTime() / 1000000
    private fun publish(value: StoryState) { mutable.value = value }
    private fun cancelWorker() { generation++; worker?.cancel(); worker = null }
    fun setForeground(value: Boolean) {
        expiry?.cancel()
        if (value && backgroundAt != 0L && now() - backgroundAt >= StoryLimits.IDLE_MS) clear("Private case expired. Saved cases remain encrypted on this device.")
        foreground = value; backgroundAt = if (value) 0 else now()
        if (!value) expiry = scope.launch { delay(StoryLimits.IDLE_MS); if (!foreground) clear("Private case expired. Saved cases remain encrypted on this device.") }
        if (value) refreshSaved()
    }
    fun clear(notice: String? = null) { cancelWorker(); publish(StoryState(notice = notice)); backgroundAt = 0 }
    fun lock() { clear("Private case cleared when the screen locked. Saved cases remain encrypted.") }
    fun rename(value: String) { if (!mutable.value.busy) publish(mutable.value.copy(case = mutable.value.case.copy(title = value.take(80)), dirty = true)) }
    fun selectSource(source: StorySource) { if (!mutable.value.busy && source != mutable.value.source) publish(mutable.value.copy(source = source, qrChoices = emptyList(), importedResult = null, error = null)) }
    fun editInput(value: String) { if (!mutable.value.busy && value != mutable.value.input) publish(mutable.value.copy(input = value.take(StoryLimits.ITEM_CHARS + 1), edited = mutable.value.edited || mutable.value.source in setOf(StorySource.IMAGE, StorySource.FLOATING), importedResult = null, error = null)) }
    fun chooseQr(value: String) { if (value in mutable.value.qrChoices) publish(mutable.value.copy(input = value, source = StorySource.QR, edited = false, importedResult = null, error = null)) }
    fun dismissMessage() { publish(mutable.value.copy(error = null, notice = null)) }
    fun cancel() { cancelWorker(); publish(mutable.value.copy(busy = false, saving = false, notice = "Analysis cancelled. Your reviewed input is still available.")) }
    fun editItem(id: String) {
        if (mutable.value.busy) return
        val item = mutable.value.case.items.firstOrNull { it.id == id } ?: return
        publish(mutable.value.copy(input = item.text, source = item.source, edited = item.edited, extractionNote = item.extractionNote, editingId = id, qrChoices = emptyList(), importedResult = null, error = null))
    }
    fun remove(id: String) {
        cancelWorker()
        val current = mutable.value
        val case = current.case.copy(items = current.case.items.filterNot { it.id == id }, updatedAt = System.currentTimeMillis(), savedAt = null)
        val removedEditing = current.editingId == id
        publish(current.copy(case = case, analysis = StoryAnalyzer.analyze(case), busy = false, saving = false, dirty = true,
            input = if (removedEditing) "" else current.input, editingId = if (removedEditing) null else current.editingId,
            importedResult = if (removedEditing) null else current.importedResult,
            extractionNote = if (removedEditing) null else current.extractionNote,
            qrChoices = if (removedEditing) emptyList() else current.qrChoices, error = null))
    }
    fun move(id: String, delta: Int) {
        if (mutable.value.busy) return
        val current = mutable.value; val items = current.case.items.toMutableList(); val index = items.indexOfFirst { it.id == id }
        if (index < 0 || index + delta !in items.indices) return
        val item = items.removeAt(index); items.add(index + delta, item)
        val case = current.case.copy(items = items, updatedAt = System.currentTimeMillis(), savedAt = null)
        publish(current.copy(case = case, analysis = StoryAnalyzer.analyze(case), dirty = true, notice = "Order updated. This is your chosen order, not a verified message timestamp."))
    }
    private fun start(message: String, timeout: Long = 20000, save: Boolean = false, work: suspend (Long) -> Unit) {
        if (mutable.value.busy) return
        cancelWorker(); val token = generation
        publish(mutable.value.copy(busy = true, saving = save, error = null, notice = null))
        worker = scope.launch {
            val running = coroutineContext.job
            val watchdog = scope.launch {
                delay(timeout)
                if (generation == token) {
                    generation++; running.cancel()
                    publish(mutable.value.copy(busy = false, saving = false, error = "Analysis timed out. Try a smaller item or paste the original text."))
                }
            }
            try { work(token) }
            catch (error: CancellationException) { throw error }
            catch (_: OutOfMemoryError) { if (generation == token) publish(mutable.value.copy(error = "Not enough memory for this image. Choose a smaller screenshot or paste content.")) }
            catch (_: Exception) { if (generation == token) publish(mutable.value.copy(error = message)) }
            finally { watchdog.cancel(); if (generation == token) publish(mutable.value.copy(busy = false, saving = false)) }
        }
    }
    fun importImage(uri: android.net.Uri, qrOnly: Boolean) {
        start("This image could not be read. Try a clearer screenshot or paste the original text.", 45000) { token ->
            val content = images.read(uri, qrOnly)
            if (generation != token) return@start
            if ((if (qrOnly) content.qr.isEmpty() else content.text.isBlank() && content.qr.isEmpty())) {
                publish(mutable.value.copy(error = "No readable content found. Try a clearer image or paste text.")); return@start
            }
            val input = if (qrOnly || content.text.isBlank()) content.qr.singleOrNull().orEmpty() else content.text
            publish(mutable.value.copy(input = input, source = if (qrOnly || content.text.isBlank()) StorySource.QR else StorySource.IMAGE,
                edited = false, editingId = null, qrChoices = content.qr, extractionNote = content.note, importedResult = null,
                notice = "Review the extracted content before adding it. Hidden link destinations are not visible in a screenshot."))
        }
    }
    fun addReviewed() {
        val current = mutable.value; val input = current.input.trim(); val others = current.case.items.filterNot { it.id == current.editingId }
        val problem = when {
            input.isBlank() -> "Add reviewed content first."
            input.length > StoryLimits.ITEM_CHARS -> "Choose readable content of at most 12,000 characters."
            current.source == StorySource.LINK && input.length > 8192 -> "Link is too long (maximum 8,192 characters)."
            others.size >= StoryLimits.ITEMS -> "A case can contain eight items. Remove an item or start another case."
            others.sumOf { it.text.length } + input.length > StoryLimits.TOTAL_CHARS -> "This case exceeds 48,000 characters. Shorten an item or start another case."
            others.any { StoryAnalyzer.duplicateKey(it.text) == StoryAnalyzer.duplicateKey(input) } -> "This content is already in the case. Duplicate evidence does not increase concern."
            else -> null
        }
        if (problem != null) { publish(current.copy(error = problem)); return }
        start("This item could not be analyzed. Your reviewed input is still available.") { token ->
            val result = current.importedResult?.takeIf { it.target?.trim() == input } ?: when (current.source) {
                StorySource.LINK -> repository.analyzeLinkPrivately(input)
                StorySource.QR -> repository.analyzeQrPrivately(input)
                else -> repository.analyzeTextPrivately(input)
            }
            if (generation != token) return@start
            val old = current.case.items.firstOrNull { it.id == current.editingId }
            val item = StoryItem(id = old?.id ?: java.util.UUID.randomUUID().toString(), source = current.source,
                text = input, addedAt = old?.addedAt ?: System.currentTimeMillis(), edited = current.edited || old != null,
                extractionNote = current.extractionNote, result = result.copy(isDemo = current.case.synthetic))
            val items = if (old == null) current.case.items + item else current.case.items.map { if (it.id == old.id) item else it }
            val case = current.case.copy(items = items, title = current.case.title.ifBlank { "Private situation" }, updatedAt = System.currentTimeMillis(), savedAt = null)
            val analysis = withContext(Dispatchers.Default) { StoryAnalyzer.analyze(case) }
            if (generation == token) publish(mutable.value.copy(case = case, analysis = analysis, input = "", editingId = null, edited = false,
                extractionNote = null, qrChoices = emptyList(), importedResult = null, dirty = true, notice = "Evidence added. Review the linked explanation below."))
        }
    }
    /** Explicit handoff only. It prepares a draft for review instead of silently grouping content. */
    fun prepareHandoff(text: String, source: StorySource, edited: Boolean = false, note: String? = null, result: ScanResult? = null): Boolean {
        if (mutable.value.busy || text.isBlank() || text.length > StoryLimits.ITEM_CHARS || mutable.value.input.isNotBlank()) return false
        publish(mutable.value.copy(input = text, source = source, edited = edited, extractionNote = note, editingId = null, importedResult = result,
            case = mutable.value.case.copy(synthetic = mutable.value.case.synthetic || result?.isDemo == true),
            notice = "Check that this belongs to the same situation, then add the reviewed evidence.", error = null))
        return true
    }
    fun loadDemo(steps: List<String>) {
        clear(); publish(StoryState(case = StoryCase(title = "Synthetic example", synthetic = true), input = steps.firstOrNull().orEmpty(), demoSteps = steps, demoIndex = 0))
    }
    fun nextDemo() {
        val current = mutable.value
        if (current.input.isNotBlank() || current.busy || current.demoIndex + 1 !in current.demoSteps.indices) return
        val next = current.demoIndex + 1
        publish(current.copy(input = current.demoSteps[next], source = StorySource.MESSAGE, demoIndex = next, edited = false, extractionNote = null))
    }
    fun refreshSaved() {
        val token = generation
        scope.launch { try {
            val listing = store.list()
            if (generation == token && foreground) publish(mutable.value.copy(listing = listing))
        } catch (_: Exception) { if (generation == token && foreground) publish(mutable.value.copy(error = "Saved cases could not be read. Try again or delete saved cases.")) } }
    }
    fun save() {
        val current = mutable.value
        if (current.case.items.isEmpty() || current.input.isNotBlank()) { publish(current.copy(error = "Finish reviewing your input before saving this case.")); return }
        start("This case could not be saved securely. The private draft is still available.", save = true) { token ->
            val saved = store.save(current.case.copy(title = current.case.title.ifBlank { "Private situation" }))
            if (generation == token) publish(mutable.value.copy(case = saved, dirty = false, notice = "Case saved with on-device encryption."))
        }
    }
    fun open(id: String) {
        start("This saved case could not be opened. It may be damaged or its encryption key may be unavailable.") { token ->
            val case = store.open(id)
            val analysis = withContext(Dispatchers.Default) { StoryAnalyzer.analyze(case) }
            if (generation == token) publish(StoryState(case = case, analysis = analysis, listing = mutable.value.listing, busy = true))
        }
    }
    fun deleteSaved(id: String? = null) {
        start("Saved cases could not be deleted. Try again.") { token ->
            if (id == null) store.deleteAll() else store.delete(id)
            val listing = store.list()
            if (generation == token) {
                val current = mutable.value
                publish(current.copy(listing = listing, case = if (id == null || id == current.case.id) current.case.copy(savedAt = null) else current.case,
                    dirty = current.dirty || id == null || id == current.case.id, notice = "Saved case data deleted. The open private draft is separate."))
            }
        }
    }
}
