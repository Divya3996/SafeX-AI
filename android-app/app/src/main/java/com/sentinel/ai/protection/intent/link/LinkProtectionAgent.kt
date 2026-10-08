package com.sentinel.ai.protection.intent.link

import com.sentinel.ai.core.model.LocalEvidence
import com.sentinel.ai.core.model.LocalFinding
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRiskEngine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * URL protection entry point for incoming link payloads.
 *
 * This class normalizes URLs, evaluates threat metrics, and returns a scan result.
 */
@Singleton
class LinkProtectionAgent @Inject constructor(
    private val riskEngine: LinkHeuristicRiskEngine
) : LinkScanner {

    private val ruleMetadata = LinkHeuristicRiskEngine.defaultRules().map { rule ->
        rule.id to rule.name
    }

    override suspend fun scan(url: String): ScanResult {
        val analysis = riskEngine.analyze(url)
        val findings = analysis.ruleResults.mapIndexedNotNull { index, result ->
            if (!result.triggered || result.explanation == null) {
                return@mapIndexedNotNull null
            }

            val (ruleId, ruleName) = ruleMetadata.getOrNull(index)
                ?: ("local_rule_${index + 1}" to "Local heuristic rule ${index + 1}")
            LocalFinding(
                ruleId = ruleId,
                ruleName = ruleName,
                category = result.category.name,
                scoreContribution = result.scoreContribution.coerceAtLeast(0f),
                reason = result.explanation
            )
        }

        return ScanResult(
            id = UUID.randomUUID().toString(),
            source = "Intent (Link)",
            target = url.trim(),
            senderDisplayName = null,
            senderIdentifier = null,
            riskLevel = analysis.riskLevel,
            riskScore = analysis.score,
            explanation = analysis.explanation,
            timestamp = System.currentTimeMillis(),
            localEvidence = LocalEvidence(
                score = analysis.score,
                riskLevel = analysis.riskLevel,
                findings = findings,
                triggeredRuleCount = analysis.triggeredRuleCount
            )
        )
    }
}
