package com.sentinel.ai.protection.intent.reputation

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockReputationProvider @Inject constructor() : ReputationProvider {
    override val providerName: String = "MockProvider"

    override fun supports(target: ReputationTarget): Boolean = target is ReputationTarget.Url

    override suspend fun evaluate(target: ReputationTarget): ReputationResult? {
        val urlTarget = target as? ReputationTarget.Url ?: return null
        val url = urlTarget.url

        return when {
            url.contains("malicious.com", ignoreCase = true) -> {
                ReputationResult(
                    providerName = providerName,
                    confidence = 0.95f,
                    reputation = ReputationVerdict.MALICIOUS,
                    reason = "Deterministic mock match: malicious target.",
                    timestamp = System.currentTimeMillis()
                )
            }
            url.contains("suspicious.com", ignoreCase = true) -> {
                ReputationResult(
                    providerName = providerName,
                    confidence = 0.80f,
                    reputation = ReputationVerdict.SUSPICIOUS,
                    reason = "Deterministic mock match: suspicious target.",
                    timestamp = System.currentTimeMillis()
                )
            }
            url.contains("clean.com", ignoreCase = true) -> {
                ReputationResult(
                    providerName = providerName,
                    confidence = 0.90f,
                    reputation = ReputationVerdict.CLEAN,
                    reason = "Deterministic mock match: clean target.",
                    timestamp = System.currentTimeMillis()
                )
            }
            else -> {
                ReputationResult(
                    providerName = providerName,
                    confidence = 0.50f,
                    reputation = ReputationVerdict.UNKNOWN,
                    reason = "Mock provider fallback: unknown target.",
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }
}
