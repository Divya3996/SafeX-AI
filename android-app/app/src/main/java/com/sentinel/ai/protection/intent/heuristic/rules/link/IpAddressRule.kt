package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult
import com.sentinel.ai.protection.intent.link.ParsedUrl

class IpAddressRule : LinkHeuristicRule {
    override val id: String = "ip_address"
    override val name: String = "IP Address Host"

    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val weight = config.weights[id] ?: 0f
        
        val triggered = url.isIpv4 || url.isIpv6
        
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) weight else 0f,
            explanation = if (triggered) "Uses IP address instead of domain" else null,
            category = RuleCategory.DOMAIN
        )
    }
}
