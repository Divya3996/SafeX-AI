package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult
import com.sentinel.ai.protection.intent.link.ParsedUrl

class UserinfoDeceptionRule : LinkHeuristicRule {
    override val id: String = "userinfo_deception"
    override val name: String = "Userinfo Deception"

    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val triggered = url.hasUserInfo
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "The URL contains deceptive user information before the actual host" else null,
            category = RuleCategory.URL_STRUCTURE
        )
    }
}
