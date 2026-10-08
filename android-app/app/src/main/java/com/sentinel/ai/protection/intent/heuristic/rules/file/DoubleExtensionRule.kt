package com.sentinel.ai.protection.intent.heuristic.rules.file

import com.sentinel.ai.protection.intent.heuristic.FileHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.FileHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult

class DoubleExtensionRule : FileHeuristicRule {
    override val id: String = "double_extension"
    override val name: String = "Double Extension"

    override fun evaluate(filename: String, config: FileHeuristicConfig): RuleResult {
        val parts = filename.extensionParts()
        val triggered = parts.size >= 3
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "File uses multiple extensions" else null,
            category = RuleCategory.FILE
        )
    }
}
