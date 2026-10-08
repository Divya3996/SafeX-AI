package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult
import com.sentinel.ai.protection.intent.link.ParsedUrl

class LongFilenameRule : LinkHeuristicRule {
    override val id: String = "long_filename"
    override val name: String = "Long Filename"

    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val filename = url.path.substringAfterLast('/', "")
        val triggered = filename.length > config.filenameLengthThreshold
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "URL contains an unusually long filename" else null,
            category = RuleCategory.URL_STRUCTURE
        )
    }
}
