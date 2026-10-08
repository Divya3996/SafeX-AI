package com.sentinel.ai.core.model

/** Display-only identity facts from the same parser used by detection; no network verification. */
data class LinkInspection(
    val host: String,
    val registrableDomain: String,
    val unicodeHost: String,
    val usesHttps: Boolean,
    val hasUserInfo: Boolean,
    val isNumericAddress: Boolean,
    val embeddedHosts: List<String>,
    val signals: List<String>
)
