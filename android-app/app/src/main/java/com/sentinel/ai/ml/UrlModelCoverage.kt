package com.sentinel.ai.ml

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import com.sentinel.ai.protection.intent.link.LinkDestinations
import com.sentinel.ai.protection.intent.link.ParsedUrl

/** Historical URL-only training does not validate standalone warnings in these contexts.
 * This limits model evidence, never heuristic/snapshot evidence or a prior BLOCK.
 */
object UrlModelCoverage {
    private val config = LinkHeuristicConfig()
    fun canWarnIndependently(url: ParsedUrl): Boolean {
        val host = url.host ?: return false
        if (!url.isValid || url.isPunycode || LinkDestinations.official(host, config)) return false
        // The model's @ feature cannot distinguish an email query from deceptive userinfo.
        if (!url.hasUserInfo && url.queryParameters.any {
            it.decodedName.lowercase() in setOf("email", "login_hint") &&
                Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$").matches(it.decodedValue)
        }) return false
        return true
    }
}
