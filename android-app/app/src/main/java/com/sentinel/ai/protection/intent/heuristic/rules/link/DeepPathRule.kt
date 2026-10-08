package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult
import com.sentinel.ai.protection.intent.link.ParsedUrl

class DeepPathRule : LinkHeuristicRule {
    override val id: String = "deep_nesting"
    override val name: String = "Deep Path"

    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val segments = url.path.split('/').filter { it.isNotBlank() }
        val triggered = segments.size > config.nestingThreshold
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "URL path is deeply nested" else null,
            category = RuleCategory.URL_STRUCTURE
        )
    }
}
