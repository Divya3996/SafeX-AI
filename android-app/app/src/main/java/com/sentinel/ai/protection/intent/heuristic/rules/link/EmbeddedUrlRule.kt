package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult
import com.sentinel.ai.protection.intent.link.ParsedUrl

class EmbeddedUrlRule : LinkHeuristicRule {
    override val id: String = "embedded_url"
    override val name: String = "Embedded URL"

    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val triggered = com.sentinel.ai.protection.intent.link.LinkDestinations.external(url).isNotEmpty()

        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "Embedded external URL detected" else null,
            category = RuleCategory.URL_STRUCTURE
        )
    }
}
