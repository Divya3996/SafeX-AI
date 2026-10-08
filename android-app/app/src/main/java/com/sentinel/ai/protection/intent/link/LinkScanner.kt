package com.sentinel.ai.protection.intent.link

import com.sentinel.ai.core.model.ScanResult

/**
 * Reusable interface for scanning web URLs.
 */
interface LinkScanner {
    suspend fun scan(url: String): ScanResult
}
