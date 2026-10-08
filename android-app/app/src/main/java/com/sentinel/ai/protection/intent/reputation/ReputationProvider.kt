package com.sentinel.ai.protection.intent.reputation

interface ReputationProvider {
    val providerName: String

    fun supports(target: ReputationTarget): Boolean = true

    suspend fun evaluate(target: ReputationTarget): ReputationResult?
}
