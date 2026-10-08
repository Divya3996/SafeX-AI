package com.sentinel.ai.protection.intent.heuristic.rules.file

import com.sentinel.ai.protection.intent.heuristic.FileHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.FileHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult

class FakeDocumentRule : FileHeuristicRule {
    override val id: String = "fake_document"
    override val name: String = "Fake Document"

    override fun evaluate(filename: String, config: FileHeuristicConfig): RuleResult {
        val parts = filename.extensionParts()
        val apparentDocument = parts.dropLast(1).any { it in config.fakeDocumentExtensions }
        val finalDangerous = parts.lastOrNull().orEmpty() in config.dangerousExtensions
        val triggered = apparentDocument && finalDangerous
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "File disguises executable content as a document" else null,
            category = RuleCategory.FILE
        )
    }
}
