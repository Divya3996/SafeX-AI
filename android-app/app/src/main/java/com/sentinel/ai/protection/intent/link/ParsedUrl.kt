package com.sentinel.ai.protection.intent.link

/**
 * Immutable, parser-independent URL information shared by click-time link heuristics.
 *
 * [original] and [normalized] are intentionally retained for behavior-compatible raw-string
 * rules and safe downstream handoff. Callers must not log either value because URLs can contain
 * credentials or sensitive query values. [toString] is redacted for the same reason.
 */
data class ParsedUrl(
    val original: String,
    val normalized: String,
    val scheme: String?,
    val host: String?,
    val originalHost: String?,
    val port: Int?,
    val hasExplicitPort: Boolean,
    val hasUserInfo: Boolean,
    val path: String,
    val rawPath: String,
    val percentDecodedPath: String,
    val pathContainsEmbeddedHttpUrl: Boolean,
    val query: String?,
    val fragment: String?,
    val isValid: Boolean,
    val isIpv4: Boolean,
    val isIpv6: Boolean,
    val isPunycode: Boolean,
    val subdomainCount: Int,
    val rawQueryParameterCount: Int,
    val queryParameters: List<ParsedUrlQueryParameter>,
    val decodedQueryParameters: Map<String, List<String>>,
    val schemeWasInferred: Boolean,
    val isObfuscatedNumericHost: Boolean = false
) {
    override fun toString(): String = buildString {
        append("ParsedUrl(")
        append("scheme=").append(scheme)
        append(", host=").append(host)
        append(", port=").append(port)
        append(", hasExplicitPort=").append(hasExplicitPort)
        append(", hasUserInfo=").append(hasUserInfo)
        append(", isValid=").append(isValid)
        append(", queryParameterCount=").append(queryParameters.size)
        append(", originalLength=").append(original.length)
        append(", normalizedLength=").append(normalized.length)
        append(')')
    }
}

/**
 * One query parameter with bounded decoding performed by [UrlNormalizer].
 *
 * Values are kept to support redirect and embedded-URL rules, but [toString] never reveals them.
 */
data class ParsedUrlQueryParameter(
    val rawName: String,
    val rawValue: String,
    val decodedName: String,
    val decodedValue: String,
    val twiceDecodedValue: String,
    val isPlausibleHttpDestination: Boolean,
    val containsEmbeddedHttpUrl: Boolean
) {
    override fun toString(): String =
        "ParsedUrlQueryParameter(nameLength=${rawName.length}, valueLength=${rawValue.length}, " +
            "isPlausibleHttpDestination=$isPlausibleHttpDestination, " +
            "containsEmbeddedHttpUrl=$containsEmbeddedHttpUrl)"
}
