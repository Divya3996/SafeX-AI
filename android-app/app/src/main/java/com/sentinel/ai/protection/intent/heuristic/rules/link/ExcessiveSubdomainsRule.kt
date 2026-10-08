package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult
import com.sentinel.ai.protection.intent.link.ParsedUrl

class ExcessiveSubdomainsRule : LinkHeuristicRule {
    override val id: String = "excessive_subdomains"
    override val name: String = "Excessive Subdomains"

    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val weight = config.weights[id] ?: 0f
        
        val triggered = url.subdomainCount > config.subdomainThreshold
        
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) weight else 0f,
            explanation = if (triggered) "Contains excessive subdomains" else null,
            category = RuleCategory.DOMAIN
        )
    }
}
