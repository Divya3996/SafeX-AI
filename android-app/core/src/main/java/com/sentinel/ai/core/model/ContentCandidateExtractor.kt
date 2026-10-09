package com.sentinel.ai.core.model

import java.net.URI
import java.net.URLDecoder

enum class CandidateChange { ASSUMED_HTTPS, DEFANGED, INVISIBLE_CHARACTERS, WRAPPED, LINE_BREAK }
data class ContentCandidate(val original: String, val inspectionValue: String, val changes: Set<CandidateChange> = emptySet()) {
    val needsConfirmation get() = changes.any { it != CandidateChange.ASSUMED_HTTPS }
}

/** Candidates are evidence to review, never navigable commands. No guessed character repairs. */
object ContentCandidateExtractor {
    private val invisible = Regex("[\\u200B-\\u200D\\uFEFF]")
    private val explicit = Regex("(?:https?://|hxxps?://|www\\.)[^\\s<>]+", RegexOption.IGNORE_CASE)
    private val bare = Regex("(?<![\\p{L}\\p{N}_@/.-])(?:[\\p{L}\\p{N}](?:[\\p{L}\\p{M}\\p{N}-]{0,62})\\.)+[\\p{L}][\\p{L}\\p{M}]{1,23}(?::[0-9]{1,5})?(?:[/?#][^\\s<>]*)?", RegexOption.IGNORE_CASE)
    private val token = Regex("[^\\s<>]+")
    private fun trim(value: String) = value.trimEnd('.', ',', ';', ')', ']', '}', '!', '?', '\'', '"', '।', '॥', '“', '”')
    fun extract(text: String, limit: Int = 8): List<ContentCandidate> {
        val output = mutableListOf<ContentCandidate>()
        val bounded = text.take(12000)
        fun add(candidate: ContentCandidate) {
            if (candidate.inspectionValue.length in 1..8192 && output.none { it.inspectionValue == candidate.inspectionValue } && output.size < limit.coerceIn(1, 32)) output += candidate
        }
        for (part in token.findAll(bounded)) {
            val raw = part.value
            val normalized = invisible.replace(raw, "").replace("[.]", ".")
                .replace(Regex("hxxp(?=s?://)", RegexOption.IGNORE_CASE), "http")
            val alterations = buildSet {
                if (invisible.containsMatchIn(raw)) add(CandidateChange.INVISIBLE_CHARACTERS)
                if (raw.contains("[.]") || Regex("hxxps?://", RegexOption.IGNORE_CASE).containsMatchIn(raw)) add(CandidateChange.DEFANGED)
            }
            val matches = explicit.findAll(normalized).toList().ifEmpty { bare.findAll(normalized).toList() }
            for (match in matches) {
                // Email addresses and executable scheme payloads are not bare web hosts.
                if (match.range.first > 0 && normalized[match.range.first - 1] == '@') continue
                val value = trim(match.value)
                val assumed = !value.startsWith("http://", true) && !value.startsWith("https://", true)
                val inspection = if (assumed) "https://$value" else value
                add(ContentCandidate(if (alterations.isEmpty()) value else trim(raw), inspection,
                    alterations + if (assumed) setOf(CandidateChange.ASSUMED_HTTPS) else emptySet()))
                if (value.lastOrNull() in listOf('/', '?', '=', '&', '-', '%')) {
                    val end = part.range.last + 1
                    val after = bounded.substring(end).take(600)
                    if (after.startsWith("\n") || after.startsWith("\r\n")) {
                        val continuation = trim(after.trimStart('\r', '\n').takeWhile { !it.isWhitespace() })
                        if (continuation.length in 2..512 && continuation.all { it.isLetterOrDigit() || it in "/?&=_%#.+-" } && !continuation.contains("://"))
                            add(ContentCandidate(trim(raw) + "\n" + continuation, inspection + continuation, alterations + CandidateChange.LINE_BREAK))
                    }
                }
                // A wrapped destination is only an additional candidate; it never replaces the outer link.
                val query = runCatching { URI(inspection).rawQuery }.getOrNull().orEmpty()
                query.split('&').take(32).forEach { entry ->
                    val key = entry.substringBefore('=').lowercase()
                    if (key in setOf("url", "u", "target", "redirect", "redirect_uri", "next", "continue", "destination")) {
                        val inner = runCatching { URLDecoder.decode(entry.substringAfter('=', ""), "UTF-8") }.getOrNull().orEmpty()
                        if (inner.startsWith("https://", true) || inner.startsWith("http://", true))
                            add(ContentCandidate(trim(raw), trim(inner), alterations + CandidateChange.WRAPPED))
                    }
                }
            }
        }
        return output
    }
}
