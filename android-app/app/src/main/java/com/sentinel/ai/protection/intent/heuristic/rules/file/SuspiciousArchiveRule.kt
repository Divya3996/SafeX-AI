package com.sentinel.ai.protection.intent.heuristic.rules.file

import com.sentinel.ai.protection.intent.heuristic.FileHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.FileHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult

class SuspiciousArchiveRule : FileHeuristicRule {
    override val id: String = "suspicious_archive"
    override val name: String = "Suspicious Archive"

    override fun evaluate(filename: String, config: FileHeuristicConfig): RuleResult {
        val extension = filename.extensionOrEmpty()
        val triggered = extension in config.suspiciousArchiveExtensions
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "File is an archive that may hide unsafe content" else null,
            category = RuleCategory.FILE
        )
    }
}
