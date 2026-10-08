package com.sentinel.ai.protection.intent.file

import android.net.Uri
import com.sentinel.ai.core.model.ScanResult

/**
 * Reusable interface for scanning files.
 */
interface FileScanner {
    suspend fun scan(uri: Uri): ScanResult
}
