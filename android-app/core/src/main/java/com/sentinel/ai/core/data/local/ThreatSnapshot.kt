package com.sentinel.ai.core.data.local

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

/** Import a user-chosen URL threat list. No network access or submission of scan content. */
object ThreatSnapshot {
    suspend fun import(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val lines = mutableSetOf<String>()
        val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open this threat list.")
        val text = input.use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                require(output.size() + count <= 2_000_000) { "Threat list is too large." }
                output.write(buffer, 0, count)
            }
            output.toString("UTF-8")
        }
        text.lineSequence().forEach { line ->
            require(line.length <= 8192) { "Threat list line is too long." }
            val value = line.trim()
            if (value.startsWith('#') || value.isBlank()) return@forEach
            val parsed = runCatching { URI(value) }.getOrNull()
            require(parsed?.scheme in listOf("http", "https") && !parsed?.host.isNullOrBlank()) { "Threat lists must contain one complete HTTP(S) URL per line." }
            lines += value
            require(lines.size <= 20000) { "Threat list has too many entries." }
        }
        require(lines.isNotEmpty()) { "The threat list contains no URLs." }
        val atomic = AtomicFile(File(context.filesDir, "threat-feed.txt"))
        val output = atomic.startWrite()
        try { output.write(lines.joinToString("\n").toByteArray()); atomic.finishWrite(output) }
        catch (e: Exception) { atomic.failWrite(output); throw e }
        lines.size
    }
    fun clear(context: Context) { AtomicFile(File(context.filesDir, "threat-feed.txt")).delete() }
    fun count(context: Context): Int = runCatching { File(context.filesDir, "threat-feed.txt").useLines { it.count() } }.getOrDefault(0)
}
