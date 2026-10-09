package com.sentinel.ai.core.data

import android.net.Uri
import com.sentinel.ai.core.model.ScanResult

/** Entry point used by UI clients to run the app's protection scan pipeline. */
interface ScanRepository {
    suspend fun scanLink(link: String): ScanResult
    suspend fun scanFile(uri: Uri): ScanResult
    suspend fun scanText(text: String): ScanResult = throw UnsupportedOperationException("Message scanning unavailable")
    suspend fun analyzeMessage(text: String, source: String, sender: String?, identifier: String?, timestamp: Long, id: String): ScanResult = scanText(text)
    suspend fun scanQrContent(content: String): ScanResult = scanText(content)
    suspend fun scanImage(uri: Uri, qrOnly: Boolean = false): ScanResult = throw UnsupportedOperationException("Image scanning unavailable")
    /** These paths must never emit an event or write history. Unsupported clients fail closed. */
    suspend fun analyzeLinkPrivately(link: String): ScanResult = throw UnsupportedOperationException("Private analysis unavailable")
    suspend fun analyzeTextPrivately(text: String): ScanResult = throw UnsupportedOperationException("Private analysis unavailable")
    suspend fun analyzeQrPrivately(content: String): ScanResult = throw UnsupportedOperationException("Private analysis unavailable")
}
