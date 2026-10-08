package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult
import com.sentinel.ai.protection.intent.link.ParsedUrl

class RepeatedHyphensRule : LinkHeuristicRule {
    override val id: String = "repeated_hyphens"
    override val name: String = "Repeated Hyphens"

    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val host = url.host.orEmpty()
        val weight = config.weights[id] ?: 0f
        
        val hasDoubleHyphen = host.contains("--") && !host.startsWith("xn--")
        val hyphenCount = host.count { it == '-' }
        
        val triggered = hasDoubleHyphen || hyphenCount > 2
        
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) weight else 0f,
            explanation = if (triggered) "Contains repeated or multiple hyphens in domain" else null,
            category = RuleCategory.DOMAIN
        )
    }
}
