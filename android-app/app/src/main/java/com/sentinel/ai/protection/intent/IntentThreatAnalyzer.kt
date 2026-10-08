package com.sentinel.ai.protection.intent

import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.protection.intent.model.IntentPayload

/**
 * High-level coordinator interface for intent threat analysis.
 */
interface IntentThreatAnalyzer {
    suspend fun analyze(payload: IntentPayload): ScanResult
}
