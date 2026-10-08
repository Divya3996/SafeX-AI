package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRule
import com.sentinel.ai.protection.intent.heuristic.RuleCategory
import com.sentinel.ai.protection.intent.heuristic.RuleResult
import com.sentinel.ai.protection.intent.link.ParsedUrl
import kotlin.math.log2

class RandomHostnameRule : LinkHeuristicRule {
    override val id: String = "random_hostname"
    override val name: String = "Random Hostname"

    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val host = url.originalHost.orEmpty()
        val weight = config.weights[id] ?: 0f
        
        // Remove standard dots so dots don't artificially reduce/increase entropy
        val cleanHost = host.replace(".", "")
        val entropy = calculateEntropy(cleanHost)
        val triggered = entropy > config.randomHostnameEntropyThreshold && cleanHost.length >= 8
        
        return RuleResult(
            triggered = triggered,
            scoreContribution = if (triggered) weight else 0f,
            explanation = if (triggered) "Uses a random-looking hostname" else null,
            category = RuleCategory.DOMAIN
        )
    }

    private fun calculateEntropy(str: String): Double {
        if (str.isEmpty()) return 0.0
        val freqs = str.groupingBy { it }.eachCount()
        val len = str.length.toDouble()
        var entropy = 0.0
        for (count in freqs.values) {
            val p = count / len
            entropy -= p * log2(p)
        }
        return entropy
    }
}
