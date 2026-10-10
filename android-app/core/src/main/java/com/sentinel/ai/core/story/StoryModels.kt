package com.sentinel.ai.core.story

import com.sentinel.ai.core.model.*
import java.util.UUID

enum class StorySource(val label: String) {
    MESSAGE("Message"), LINK("Link"), QR("QR content"), IMAGE("Screenshot"),
    FLOATING("Floating review"), RESULT("Scan result")
}
enum class StoryConcern(val label: String) {
    CONTEXT("More context needed"), REVIEW("Review this situation"), HIGH("High concern")
}
enum class StorySignalKind {
    WORK, EARNINGS, PAYMENT, FEE_RELEASE, AUTHORITY, THREAT, SECRET, SUPPORT, REMOTE
}
data class StorySignal(val itemId: String, val kind: StorySignalKind, val start: Int, val end: Int)
data class StoryFinding(
    val id: String, val title: String, val explanation: String, val action: String,
    val concern: StoryConcern, val evidence: List<StorySignal>, val watchFor: String? = null
)
data class StoryAnalysis(val concern: StoryConcern, val findings: List<StoryFinding>, val incomplete: Boolean = false)
data class StoryItem(
    val id: String = UUID.randomUUID().toString(), val source: StorySource,
    val text: String, val addedAt: Long = System.currentTimeMillis(),
    val edited: Boolean = false, val extractionNote: String? = null,
    val result: ScanResult? = null, val analysisFailed: Boolean = false
)
data class StoryCase(
    val id: String = UUID.randomUUID().toString(), val title: String = "Private situation",
    val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = createdAt,
    val items: List<StoryItem> = emptyList(), val synthetic: Boolean = false,
    val savedAt: Long? = null
)
data class SavedStory(val id: String, val title: String, val itemCount: Int, val savedAt: Long, val synthetic: Boolean)
data class StoryImageContent(val text: String, val qr: List<String>, val note: String)
interface StoryImageReader { suspend fun read(uri: android.net.Uri, qrOnly: Boolean): StoryImageContent }

object StoryLimits {
    const val ITEMS = 8
    const val ITEM_CHARS = 12000
    const val TOTAL_CHARS = 48000
    const val SAVED_CASES = 20
    const val FILE_BYTES = 1024 * 1024
    const val IDLE_MS = 120000L
    fun validate(case: StoryCase) {
        require(runCatching { UUID.fromString(case.id).toString() == case.id }.getOrDefault(false))
        require(case.title.isNotBlank() && case.title.length <= 80)
        require(case.items.size <= ITEMS && case.items.sumOf { it.text.length } <= TOTAL_CHARS)
        require(case.items.map { it.id }.distinct().size == case.items.size)
        case.items.forEach {
            require(runCatching { UUID.fromString(it.id).toString() == it.id }.getOrDefault(false))
            require(it.text.isNotBlank() && it.text.length <= ITEM_CHARS)
            require(it.result != null || it.analysisFailed)
        }
    }
}
