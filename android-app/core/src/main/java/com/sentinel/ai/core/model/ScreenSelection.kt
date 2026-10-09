package com.sentinel.ai.core.model

import kotlin.math.*

/** Normalized image coordinates, independent of preview density, padding and rotation. */
data class ScreenSelection(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun pixels(width: Int, height: Int): PixelRegion {
        require(width > 0 && height > 0)
        require(listOf(left, top, right, bottom).all { it.isFinite() })
        val l = floor(left.coerceIn(0f, 1f) * width).toInt().coerceIn(0, width - 1)
        val t = floor(top.coerceIn(0f, 1f) * height).toInt().coerceIn(0, height - 1)
        val r = ceil(right.coerceIn(0f, 1f) * width).toInt().coerceIn(l + 1, width)
        val b = ceil(bottom.coerceIn(0f, 1f) * height).toInt().coerceIn(t + 1, height)
        require(right > left && bottom > top) { "Select an area of the captured image." }
        return PixelRegion(l, t, r, b)
    }
    companion object { val FULL = ScreenSelection(0f, 0f, 1f, 1f) }
}
data class PixelRegion(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left
    val height get() = bottom - top
    fun overlaps(other: PixelRegion): Boolean {
        val area = max(0, min(right, other.right) - max(left, other.left)) * max(0, min(bottom, other.bottom) - max(top, other.top))
        return area.toFloat() / max(1, min(width * height, other.width * other.height)) > .65f
    }
}
data class ExtractedLine(val text: String, val region: PixelRegion?, val script: String)
data class ExtractionResult(val lines: List<ExtractedLine>, val qrCodes: List<String> = emptyList(), val partial: Boolean = false) {
    val text get() = lines.joinToString("\n") { it.text }
    companion object {
        private val latinTokens = Regex("[A-Za-z0-9][A-Za-z0-9._/:?&=@%+-]*")
        private fun covers(latin: ExtractedLine, native: ExtractedLine): Boolean {
            val tokens = latinTokens.findAll(latin.text).map { it.value }.toList()
            val nativeTokens = latinTokens.findAll(native.text).map { it.value }.toList()
            return tokens.isNotEmpty() && tokens.all { token -> nativeTokens.any { it.equals(token, ignoreCase = !token.contains('/')) } }
        }
        fun merge(input: List<ExtractedLine>, qr: List<String> = emptyList()): ExtractionResult {
            val unique = mutableListOf<ExtractedLine>()
            for (line in input) {
                if (line.text.isBlank()) continue
                val match = unique.indexOfFirst { prior ->
                    (prior.text == line.text && prior.region == line.region) ||
                        (prior.region != null && line.region != null && prior.region.overlaps(line.region) &&
                            ((prior.script == "latin" && line.script != "latin" && covers(prior, line)) ||
                                (line.script == "latin" && prior.script != "latin" && covers(line, prior))))
                }
                if (match < 0) unique += line
                else if (line.script != "latin" && unique[match].script == "latin") unique[match] = line
            }
            val ordered = unique.sortedWith(compareBy<ExtractedLine> { it.region?.top ?: Int.MAX_VALUE }.thenBy { it.region?.left ?: 0 })
            var length = 0
            var count = 0
            val bounded = ordered.take(250).takeWhile { line -> length += line.text.length + if (count++ > 0) 1 else 0; length <= 12000 }
            return ExtractionResult(bounded, qr.filter { it.isNotBlank() }.distinct().take(8), bounded.size < ordered.size || qr.distinct().size > 8)
        }
    }
}
