package com.sentinel.ai.protection.intent.heuristic.rules.file

import com.sentinel.ai.protection.intent.heuristic.FileHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.FileHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult

class DangerousExtensionRule : FileHeuristicRule {
    override val id: String = "dangerous_extension"
    override val name: String = "Dangerous Extension"

    override fun evaluate(filename: String, config: FileHeuristicConfig): RuleResult {
        val extension = filename.extensionOrEmpty()
        val triggered = extension in config.dangerousExtensions
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "File uses dangerous .$extension extension" else null,
            category = RuleCategory.FILE
        )
    }
}
