package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult
import com.sentinel.ai.protection.intent.link.ParsedUrl

class EncodedCharactersRule : LinkHeuristicRule {
    override val id: String = "encoded_chars"
    override val name: String = "Encoded Characters"

    private val encodedCharacter = Regex("%[0-9a-fA-F]{2}")

    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val count = encodedCharacter.findAll(url.original).count()
        val triggered = count >= 3
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "URL contains many encoded characters" else null,
            category = RuleCategory.URL_STRUCTURE
        )
    }
}
