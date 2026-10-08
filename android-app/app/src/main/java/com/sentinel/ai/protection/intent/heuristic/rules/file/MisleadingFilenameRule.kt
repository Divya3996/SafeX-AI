package com.sentinel.ai.protection.intent.heuristic.rules.file

import com.sentinel.ai.protection.intent.heuristic.FileHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.FileHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult

class MisleadingFilenameRule : FileHeuristicRule {
    override val id: String = "misleading_filename"
    override val name: String = "Misleading Filename"

    override fun evaluate(filename: String, config: FileHeuristicConfig): RuleResult {
        val clean = filename.cleanFilename()
        val extension = clean.extensionOrEmpty()
        val matched = config.misleadingKeywords.firstOrNull { clean.contains(it) }
        val triggered = matched != null && (extension in config.dangerousExtensions || extension in config.suspiciousArchiveExtensions)
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "Filename uses sensitive lure word: $matched" else null,
            category = RuleCategory.FILE
        )
    }
}
