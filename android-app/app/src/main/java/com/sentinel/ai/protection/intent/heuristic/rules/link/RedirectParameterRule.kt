package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult
import com.sentinel.ai.protection.intent.link.ParsedUrl

class RedirectParameterRule : LinkHeuristicRule {
    override val id: String = "suspicious_redirect"
    override val name: String = "Redirect Parameter"

    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val triggered = url.queryParameters.any { parameter ->
            parameter.decodedName.lowercase() in config.redirectParameters &&
                com.sentinel.ai.protection.intent.link.LinkDestinations.external(
                    url.copy(rawPath = "", fragment = null, queryParameters = listOf(parameter))
                ).isNotEmpty()
        }
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "URL uses a redirect parameter pointing to another destination" else null,
            category = RuleCategory.URL_STRUCTURE
        )
    }
}
