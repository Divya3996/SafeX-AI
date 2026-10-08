package com.sentinel.ai.protection.intent.file

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sentinel.ai.core.model.*
import com.sentinel.ai.protection.intent.heuristic.FileHeuristicRiskEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.zip.ZipInputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Bounded metadata/signature/archive checks. Never executes or installs a file. */
@Singleton
class FileProtectionAgent @Inject constructor(@ApplicationContext private val context: Context,
    private val riskEngine: FileHeuristicRiskEngine) : FileScanner {
    override suspend fun scan(uri: Uri): ScanResult {
        require(uri.scheme == "content") { "Choose a file using the system document picker." }
        var name = "Shared file"
        var size = -1L
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val n = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val s = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (n >= 0) name = cursor.getString(n).orEmpty().take(255)
                if (s >= 0 && !cursor.isNull(s)) size = cursor.getLong(s)
            }
        }
        require(size <= MAX_BYTES) { "Choose a file smaller than 10 MB." }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBounded(MAX_BYTES) }
            ?: error("File cannot be opened. Choose it again to grant read access.")
        val mime = context.contentResolver.getType(uri).orEmpty()
        val local = riskEngine.analyze(name)
        val reasons = local.ruleResults.filter { it.triggered }.mapNotNull { it.explanation }.toMutableList()
        var score = local.score
        val ext = name.substringAfterLast('.', "").lowercase()
        val pdf = bytes.size >= 5 && bytes.copyOfRange(0, 5).toString(Charsets.US_ASCII) == "%PDF-"
        val zip = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4b.toByte()
        val exe = bytes.size >= 2 && bytes[0] == 0x4d.toByte() && bytes[1] == 0x5a.toByte()
        if ((ext == "pdf" || mime == "application/pdf") && !pdf) {
            score = maxOf(score, 70f); reasons += "Claims to be a PDF but its file signature does not match"
        }
        if (exe) { score = maxOf(score, 70f); reasons += "Contains a Windows executable signature" }
        if (zip) {
            var count = 0
            var expanded = 0L
            var apk = false
            var limited = false
            ZipInputStream(bytes.inputStream()).use { archive ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val entry = archive.nextEntry ?: break
                    if (++count > 100) { limited = true; break }
                    val entryName = entry.name.lowercase()
                    if (entryName == "androidmanifest.xml") apk = true
                    if (entryName.startsWith('/') || entryName.split('/').contains("..")) { score = maxOf(score, 70f); reasons += "Archive contains unsafe paths" }
                    if (entryName.substringAfterLast('.', "") in setOf("exe", "js", "vbs", "bat", "cmd", "sh", "apk")) {
                        score = maxOf(score, 50f); reasons += "Archive contains executable or script content"
                    }
                    val buffer = ByteArray(8192)
                    var entryBytes = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = archive.read(buffer)
                        if (read < 0) break
                        entryBytes += read; expanded += read
                        if (entryBytes > 2 * 1024 * 1024 || expanded > 8 * 1024 * 1024) { limited = true; break }
                    }
                    if (limited) break
                }
            }
            if (apk) {
                score = maxOf(score, if (ext != "apk") 90f else 40f)
                reasons += if (ext != "apk") "Disguised Android installation package: filename does not end in .apk" else "Android installation package; verify its developer before installation"
            }
            if (limited) { score = maxOf(score, 30f); reasons += "Archive inspection stopped at safety limits; contents are partially checked" }
        }
        if (ext == "apk" && !zip) { score = maxOf(score, 70f); reasons += "Claims to be an APK but has no package archive signature" }
        val level = DecisionPolicy.level(score)
        val summary = if (score < 30) "No strong signals found in the supported file checks." else "This file has suspicious properties. Verify its source before opening or installing."
        return ScanResult(UUID.randomUUID().toString(), "File scan", riskLevel = level, riskScore = score,
            explanation = (listOf(summary) + reasons).distinct().joinToString("\n"), timestamp = System.currentTimeMillis(),
            summary = summary, target = name, contentType = "file", category = "File integrity", guidance = "Only open files from a verified source. These checks cannot certify a file as malware-free.",
            reasons = reasons.distinct().map { ScanReason(ScanReasonSource.LOCAL_HEURISTIC, "file_check", it, level) })
    }
    companion object { const val MAX_BYTES = 10 * 1024 * 1024 }
}
