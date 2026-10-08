package com.sentinel.ai.protection.intent.reputation

import com.sentinel.ai.core.model.ScanResult

interface ReputationManager {
    suspend fun enrich(
        heuristicResult: ScanResult,
        target: ReputationTarget?
    ): ScanResult
}
