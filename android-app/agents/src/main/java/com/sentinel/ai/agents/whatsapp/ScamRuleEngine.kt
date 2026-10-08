package com.sentinel.ai.agents.whatsapp

import com.sentinel.ai.core.event.schema.ScamRiskLevel
import com.sentinel.ai.core.event.schema.UrlAnalysisItem
import com.sentinel.ai.core.model.MessageSignals

object ScamRuleEngine {
    fun evaluate(messageText: String, urls: List<UrlAnalysisItem>, isKnownContact: Boolean): ScamRuleResult {
        val analysis = MessageSignals.analyze(messageText)
        val explanations = analysis.reasons.toMutableList()
        var score = analysis.score.toInt()
        if (urls.any { it.isIpAddressUrl }) { score += 25; explanations += "Link uses a raw IP address" }
        if (urls.any { it.isShortened }) { score += 10; explanations += "Short link hides its final destination" }
        // A contact name is not an authenticated identity and never removes threat evidence.
        val capped = score.coerceIn(0, 100)
        return ScamRuleResult(capped, when {
            capped >= 90 -> ScamRiskLevel.CRITICAL
            capped >= 70 -> ScamRiskLevel.HIGH
            capped >= 30 -> ScamRiskLevel.MEDIUM
            else -> ScamRiskLevel.LOW
        }, explanations)
    }
}
