package com.sentinel.ai.protection.intent.heuristic

import com.sentinel.ai.protection.intent.link.ParsedUrl

interface LinkHeuristicRule {
    val id: String
    val name: String
    fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult
}
