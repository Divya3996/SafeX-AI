package com.sentinel.ai.protection.intent.reputation

sealed interface ReputationTarget {
    data class Url(
        val url: String
    ) : ReputationTarget
}
