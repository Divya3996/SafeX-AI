package com.sentinel.ai.core.validation

import android.util.Patterns
import java.util.regex.Pattern

/** Validates user-supplied web URLs before they enter the scan pipeline. */
object UrlInputValidator {
    fun isValid(input: String): Boolean {
        val value = input.trim()
        val hasSupportedScheme = value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
        val webUrlPattern = Patterns.WEB_URL ?: JVM_WEB_URL_PATTERN
        return hasSupportedScheme && webUrlPattern.matcher(value).matches()
    }

    // android.util.Patterns is unavailable in local JVM tests, so use the same strict input shape.
    private val JVM_WEB_URL_PATTERN: Pattern = Pattern.compile(
        "(?i)^https?://(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}" +
            "(?::\\d{1,5})?(?:[/?#][^\\s]*)?$"
    )
}
