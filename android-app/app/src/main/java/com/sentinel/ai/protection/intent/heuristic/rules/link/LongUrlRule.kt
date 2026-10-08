package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult
import com.sentinel.ai.protection.intent.link.ParsedUrl

class LongUrlRule : LinkHeuristicRule {
    override val id: String = "excessive_length"
    override val name: String = "Long URL"

    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val triggered = url.original.length > config.urlLengthThreshold
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "URL is unusually long" else null,
            category = RuleCategory.URL_STRUCTURE
        )
    }
}
