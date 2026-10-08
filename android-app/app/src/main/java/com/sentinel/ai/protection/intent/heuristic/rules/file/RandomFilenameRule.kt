package com.sentinel.ai.protection.intent.heuristic.rules.file

import com.sentinel.ai.protection.intent.heuristic.FileHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.FileHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult
import kotlin.math.log2

class RandomFilenameRule : FileHeuristicRule {
    override val id: String = "random_filename"
    override val name: String = "Random Filename"

    override fun evaluate(filename: String, config: FileHeuristicConfig): RuleResult {
        val stem = filename.cleanFilename().substringBeforeLast('.', "")
        val entropy = calculateEntropy(stem)
        val triggered = stem.length >= 8 && entropy > config.randomFilenameEntropyThreshold
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) config.weights[id] ?: 0f else 0f,
            explanation = if (triggered) "Filename appears randomly generated" else null,
            category = RuleCategory.FILE
        )
    }

    private fun calculateEntropy(value: String): Double {
        if (value.isEmpty()) return 0.0
        val length = value.length.toDouble()
        return value.groupingBy { it }.eachCount().values.sumOf { count ->
            val probability = count / length
            -probability * log2(probability)
        }
    }
}
