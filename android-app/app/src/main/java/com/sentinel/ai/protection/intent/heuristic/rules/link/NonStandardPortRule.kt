package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult
import com.sentinel.ai.protection.intent.link.ParsedUrl

class NonStandardPortRule : LinkHeuristicRule {
    override val id: String = "non_standard_port"
    override val name: String = "Non-Standard Port"

    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val port = url.port ?: -1
        val defaultPort = when {
            url.scheme.equals("http", ignoreCase = true) -> 80
            url.scheme.equals("https", ignoreCase = true) -> 443
            else -> null
        }
        val triggered = port >= 0 && defaultPort != null && port != defaultPort
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "The URL uses a non-standard network port" else null,
            category = RuleCategory.URL_STRUCTURE
        )
    }
}
