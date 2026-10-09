package com.sentinel.ai.protection.floating

import java.nio.ByteBuffer

/** Handles padded strides, including devices that omit padding after the last row. */
object RgbaRows {
    fun packed(width: Int, height: Int, rowStride: Int, pixelStride: Int, pixels: ByteBuffer): ByteBuffer {
        require(width > 0 && height > 0 && width.toLong() * height <= 6_000_000)
        require(pixelStride == 4 && rowStride >= width * 4)
        require(rowStride.toLong() * height <= 32_000_000)
        val rowBytes = width * 4
        val last = (height - 1L) * rowStride + rowBytes
        require(last <= pixels.limit()) { "Incomplete captured pixel buffer." }
        val input = pixels.duplicate()
        if (rowStride == rowBytes) return input.apply { position(0); limit(rowBytes * height) }
        val output = ByteBuffer.allocate(rowBytes * height)
        repeat(height) { row ->
            input.limit(pixels.limit()); input.position(row * rowStride); input.limit(row * rowStride + rowBytes)
            output.put(input)
        }
        output.flip()
        return output
    }
}
