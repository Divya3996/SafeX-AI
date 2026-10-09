package com.sentinel.ai.protection.floating

import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class RgbaRowsTest {
    @Test fun lastRowWithoutTrailingPaddingRetainsExactRgbaPixels() {
        val input = ByteBuffer.wrap(byteArrayOf(1,2,3,4,5,6,7,8,99,99,99,99,9,10,11,12,13,14,15,16))
        input.position(3)
        val output = RgbaRows.packed(2,2,12,4,input)
        val bytes = ByteArray(output.remaining()); output.get(bytes)
        assertArrayEquals((1..16).map { it.toByte() }.toByteArray(), bytes)
        assertEquals(3, input.position())
    }
    @Test fun tightlyPackedPixelsNeedNoExtraCopy() {
        val input = ByteBuffer.allocate(16)
        val output = RgbaRows.packed(2,2,8,4,input)
        assertSame(input.array(), output.array()); assertEquals(16, output.remaining())
    }
    @Test(expected = IllegalArgumentException::class) fun incompleteLastRowIsRejected() {
        RgbaRows.packed(2,2,12,4,ByteBuffer.allocate(19))
    }
}
