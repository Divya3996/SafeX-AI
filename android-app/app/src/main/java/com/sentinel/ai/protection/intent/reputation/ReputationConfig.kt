package com.sentinel.ai.protection.intent.reputation

data class ReputationConfig(
    val openPhishFeedUrl: String,
    val openPhishApiKey: String,
    val lookupTimeoutMs: Long,
    val virusTotalApiKey: String = "",
    val virusTotalLookupUrl: String = ""
) {
    val isOpenPhishEnabled: Boolean
        get() = openPhishFeedUrl.isNotBlank()
}
