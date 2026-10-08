package com.sentinel.ai.protection.intent.file

import java.io.InputStream
import java.io.ByteArrayOutputStream

fun InputStream.readBounded(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (output.size() <= limit) {
        val count = read(buffer, 0, minOf(buffer.size, limit + 1 - output.size()))
        if (count < 0) break
        if (count == 0) continue
        output.write(buffer, 0, count)
    }
    require(output.size() <= limit) { "This item exceeds the size limit." }
    return output.toByteArray()
}
