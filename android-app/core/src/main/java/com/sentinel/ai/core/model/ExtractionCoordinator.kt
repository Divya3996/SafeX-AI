package com.sentinel.ai.core.model

/** Bounded outputs, independent failures. A recognizer's quality flag never becomes threat confidence. */
data class EngineContent(val lines: List<ExtractedLine> = emptyList(), val qr: List<String> = emptyList(), val qualityLimited: Boolean = false)
class ExtractionTimeoutException : Exception()
object ExtractionCoordinator {
    suspend fun collect(readers: List<Pair<ExtractionEngine, suspend () -> EngineContent>>): ExtractionResult {
        val lines = mutableListOf<ExtractedLine>(); val qr = mutableListOf<String>(); val outcomes = mutableListOf<ExtractionOutcome>()
        for ((engine, read) in readers) {
            val start = System.nanoTime()
            try {
                val content = read(); lines += content.lines; qr += content.qr
                outcomes += ExtractionOutcome(engine, if (content.qualityLimited) EvidenceSourceStatus.UNKNOWN else EvidenceSourceStatus.COMPLETED,
                    content.lines.size + content.qr.size, content.qualityLimited, (System.nanoTime() - start) / 1_000_000)
            } catch (_: ExtractionTimeoutException) { outcomes += ExtractionOutcome(engine, EvidenceSourceStatus.TIMED_OUT, elapsedMs = (System.nanoTime() - start) / 1_000_000) }
            catch (_: Exception) { outcomes += ExtractionOutcome(engine, EvidenceSourceStatus.FAILED, elapsedMs = (System.nanoTime() - start) / 1_000_000) }
        }
        val result = ExtractionResult.merge(lines, qr)
        val boundedQr = result.qrCodes.filter { it.length <= 12000 }
        return result.copy(qrCodes = boundedQr, partial = result.partial || boundedQr.size != result.qrCodes.size, qrTruncated = result.qrTruncated || boundedQr.size != result.qrCodes.size, outcomes = outcomes)
    }
}
