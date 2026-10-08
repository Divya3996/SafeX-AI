package com.sentinel.ai.protection.intent.reputation

import com.sentinel.ai.core.model.EvidenceSourceStatus
import com.sentinel.ai.core.model.LocalEvidence
import com.sentinel.ai.core.model.LocalFinding
import com.sentinel.ai.core.model.ProtectionDecision
import com.sentinel.ai.core.model.ProviderFinding
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanReason
import com.sentinel.ai.core.model.ScanReasonSource
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.core.model.defaultAction
import com.sentinel.ai.protection.intent.heuristic.toRiskLevel
import javax.inject.Inject

class EvidenceCombiner @Inject constructor() {

    fun combine(
        heuristicResult: ScanResult,
        reputationEvidence: List<ReputationEvidence>
    ): ScanResult {
        val localScore = boundedScore(heuristicResult.riskScore)
        val localEvidence = normalizedLocalEvidence(heuristicResult, localScore)
        val providerEvidence = normalizeProviderEvidence(reputationEvidence)
        val providerFindings = providerEvidence.map(::toProviderFinding)

        val hasMaliciousProvider = providerEvidence.any {
            it.result?.reputation == ReputationVerdict.MALICIOUS
        }
        val hasSuspiciousProvider = providerEvidence.any {
            it.result?.reputation == ReputationVerdict.SUSPICIOUS
        }

        val decision = when {
            hasMaliciousProvider -> ProtectionDecision.BLOCK
            localScore >= BLOCK_THRESHOLD -> ProtectionDecision.BLOCK
            hasSuspiciousProvider -> ProtectionDecision.WARN
            localScore >= WARN_THRESHOLD -> ProtectionDecision.WARN
            else -> ProtectionDecision.ALLOW
        }

        val finalScore = calculateRiskScore(
            localScore = localScore,
            providerEvidence = providerEvidence,
            hasMaliciousProvider = hasMaliciousProvider,
            hasSuspiciousProvider = hasSuspiciousProvider
        )
        val finalRiskLevel = finalScore.toRiskLevel()
        val confidence = calculateConfidence(
            decision = decision,
            localScore = localScore,
            providerEvidence = providerEvidence
        )
        val reasons = aggregateReasons(localEvidence, providerEvidence)
        val headline = headlineFor(decision)
        val summary = summaryFor(decision, localScore, providerEvidence)
        val explanation = buildExplanation(summary, reasons)

        return heuristicResult.copy(
            riskLevel = finalRiskLevel,
            riskScore = finalScore,
            explanation = explanation,
            decision = decision,
            confidence = confidence,
            headline = headline,
            summary = summary,
            reasons = reasons,
            providerFindings = providerFindings,
            localEvidence = localEvidence,
            recommendedAction = decision.defaultAction()
        )
    }

    private fun normalizedLocalEvidence(
        heuristicResult: ScanResult,
        localScore: Float
    ): LocalEvidence {
        val riskLevel = localScore.toRiskLevel()
        val existing = heuristicResult.localEvidence
        if (existing != null) {
            return existing.copy(
                score = localScore,
                riskLevel = riskLevel,
                findings = existing.findings.map { finding ->
                    finding.copy(scoreContribution = boundedScore(finding.scoreContribution))
                }
            )
        }

        val fallbackFindings = if (localScore > MIN_SCORE && heuristicResult.explanation.isNotBlank()) {
            listOf(
                LocalFinding(
                    ruleId = "legacy_local_evidence",
                    ruleName = "Local heuristic analysis",
                    category = "LOCAL_HEURISTIC",
                    scoreContribution = localScore,
                    reason = heuristicResult.explanation.trim()
                )
            )
        } else {
            emptyList()
        }

        return LocalEvidence(
            score = localScore,
            riskLevel = riskLevel,
            findings = fallbackFindings,
            triggeredRuleCount = fallbackFindings.size
        )
    }

    private fun normalizeProviderEvidence(
        evidence: List<ReputationEvidence>
    ): List<ReputationEvidence> {
        return evidence
            .groupBy { it.providerName.trim().lowercase() }
            .values
            .map { duplicates ->
                duplicates.sortedWith(
                    compareByDescending<ReputationEvidence> { evidencePriority(it) }
                        .thenByDescending { it.result?.confidence?.let(::boundedConfidence) ?: 0f }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { evidenceReason(it) }
                        .thenBy { evidenceReason(it) }
                ).first()
            }
            .sortedWith(
                compareByDescending<ReputationEvidence> { evidencePriority(it) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.providerName }
                    .thenBy { it.providerName }
            )
    }

    private fun evidencePriority(evidence: ReputationEvidence): Int = when (evidence.result?.reputation) {
        ReputationVerdict.MALICIOUS -> 5
        ReputationVerdict.SUSPICIOUS -> 4
        ReputationVerdict.CLEAN -> 3
        ReputationVerdict.UNKNOWN -> 2
        null -> when (evidence.status) {
            EvidenceSourceStatus.COMPLETED -> 2
            EvidenceSourceStatus.UNKNOWN -> 2
            EvidenceSourceStatus.TIMED_OUT -> 1
            EvidenceSourceStatus.FAILED -> 1
            EvidenceSourceStatus.UNAVAILABLE -> 1
        }
    }

    private fun evidenceReason(evidence: ReputationEvidence): String {
        return evidence.result?.reason?.trim().orEmpty().ifBlank { evidence.statusReason.trim() }
    }

    private fun toProviderFinding(evidence: ReputationEvidence): ProviderFinding {
        val result = evidence.result
        val status = if (result?.reputation == ReputationVerdict.UNKNOWN) {
            EvidenceSourceStatus.UNKNOWN
        } else {
            evidence.status
        }
        return ProviderFinding(
            providerName = evidence.providerName,
            status = status,
            verdict = result?.reputation?.name,
            confidence = result?.confidence?.let(::boundedConfidence) ?: 0f,
            reason = evidenceReason(evidence)
        )
    }

    private fun calculateRiskScore(
        localScore: Float,
        providerEvidence: List<ReputationEvidence>,
        hasMaliciousProvider: Boolean,
        hasSuspiciousProvider: Boolean
    ): Float {
        var complementaryProbability = 1f - (localScore / MAX_SCORE)
        providerEvidence.forEach { evidence ->
            val result = evidence.result ?: return@forEach
            val weightedProbability = when (result.reputation) {
                ReputationVerdict.MALICIOUS -> MALICIOUS_WEIGHT * boundedConfidence(result.confidence)
                ReputationVerdict.SUSPICIOUS -> SUSPICIOUS_WEIGHT * boundedConfidence(result.confidence)
                ReputationVerdict.CLEAN,
                ReputationVerdict.UNKNOWN -> 0f
            }
            complementaryProbability *= 1f - weightedProbability
        }

        val fusedScore = ((1f - complementaryProbability) * MAX_SCORE).coerceIn(MIN_SCORE, MAX_SCORE)
        return when {
            hasMaliciousProvider -> maxOf(fusedScore, BLOCK_THRESHOLD)
            hasSuspiciousProvider -> maxOf(fusedScore, WARN_THRESHOLD)
            else -> localScore
        }.coerceIn(MIN_SCORE, MAX_SCORE)
    }

    private fun calculateConfidence(
        decision: ProtectionDecision,
        localScore: Float,
        providerEvidence: List<ReputationEvidence>
    ): Float {
        val confidence = when (decision) {
            ProtectionDecision.BLOCK -> {
                val support = providerEvidence
                    .filter { it.result?.reputation == ReputationVerdict.MALICIOUS }
                    .mapNotNull { it.result?.confidence?.let(::boundedConfidence) }
                    .toMutableList()
                if (localScore >= BLOCK_THRESHOLD) {
                    support += localScore / MAX_SCORE
                }
                combineConfidence(support)
            }

            ProtectionDecision.WARN -> {
                val support = providerEvidence
                    .filter { it.result?.reputation == ReputationVerdict.SUSPICIOUS }
                    .mapNotNull { it.result?.confidence?.let(::boundedConfidence) }
                    .toMutableList()
                if (localScore in WARN_THRESHOLD..<BLOCK_THRESHOLD) {
                    support += localScore / MAX_SCORE
                }
                combineConfidence(support)
            }

            ProtectionDecision.ALLOW -> {
                val localMargin = ((WARN_THRESHOLD - localScore) / WARN_THRESHOLD).coerceIn(0f, 1f)
                if (providerEvidence.isEmpty()) {
                    localMargin
                } else {
                    val cleanConfidence = providerEvidence
                        .filter { it.result?.reputation == ReputationVerdict.CLEAN }
                        .mapNotNull { it.result?.confidence?.let(::boundedConfidence) }
                    val cleanCoverage = cleanConfidence.size.toFloat() / providerEvidence.size.toFloat()
                    localMargin * combineConfidence(cleanConfidence) * cleanCoverage
                }
            }
        }
        return boundedConfidence(confidence)
    }

    private fun combineConfidence(values: List<Float>): Float {
        if (values.isEmpty()) {
            return 0f
        }
        return (1f - values.fold(1f) { complement, value ->
            complement * (1f - boundedConfidence(value))
        }).let(::boundedConfidence)
    }

    private fun aggregateReasons(
        localEvidence: LocalEvidence,
        providerEvidence: List<ReputationEvidence>
    ): List<ScanReason> {
        val maliciousReasons = providerEvidence
            .filter { it.result?.reputation == ReputationVerdict.MALICIOUS }
            .map { providerReason(it, RiskLevel.CRITICAL) }
        val suspiciousReasons = providerEvidence
            .filter { it.result?.reputation == ReputationVerdict.SUSPICIOUS }
            .map { providerReason(it, RiskLevel.YELLOW) }
        val localReasons = localEvidence.findings.map { finding ->
            ScanReason(
                source = ScanReasonSource.LOCAL_HEURISTIC,
                sourceName = finding.ruleId,
                message = finding.reason,
                riskLevel = localEvidence.riskLevel
            )
        }
        val informationalReasons = providerEvidence
            .filter {
                it.result?.reputation != ReputationVerdict.MALICIOUS &&
                    it.result?.reputation != ReputationVerdict.SUSPICIOUS
            }
            .map { evidence ->
                val isProviderResult = evidence.result != null
                ScanReason(
                    source = if (isProviderResult) {
                        ScanReasonSource.REPUTATION_PROVIDER
                    } else {
                        ScanReasonSource.PROVIDER_STATUS
                    },
                    sourceName = evidence.providerName,
                    message = evidenceReason(evidence),
                    riskLevel = null
                )
            }

        val seenMessages = mutableSetOf<String>()
        return (maliciousReasons + suspiciousReasons + localReasons + informationalReasons)
            .filter { reason -> seenMessages.add(reason.message.trim().lowercase()) }
    }

    private fun providerReason(
        evidence: ReputationEvidence,
        riskLevel: RiskLevel
    ): ScanReason = ScanReason(
        source = ScanReasonSource.REPUTATION_PROVIDER,
        sourceName = evidence.providerName,
        message = evidenceReason(evidence),
        riskLevel = riskLevel
    )

    private fun headlineFor(decision: ProtectionDecision): String = when (decision) {
        ProtectionDecision.ALLOW -> "No strong threat evidence detected"
        ProtectionDecision.WARN -> "Suspicious link evidence detected"
        ProtectionDecision.BLOCK -> "Critical threat evidence detected"
    }

    private fun summaryFor(
        decision: ProtectionDecision,
        localScore: Float,
        providerEvidence: List<ReputationEvidence>
    ): String {
        val incompleteLookup = providerEvidence.any {
            it.status == EvidenceSourceStatus.UNAVAILABLE ||
                it.status == EvidenceSourceStatus.FAILED ||
                it.status == EvidenceSourceStatus.TIMED_OUT
        }
        val hasUnknown = providerEvidence.any {
            it.result?.reputation == ReputationVerdict.UNKNOWN ||
                it.status == EvidenceSourceStatus.UNKNOWN
        }
        return when (decision) {
            ProtectionDecision.BLOCK -> {
                if (providerEvidence.any { it.result?.reputation == ReputationVerdict.MALICIOUS }) {
                    "A reputation provider identified this destination as malicious. Do not continue."
                } else {
                    "Critical local URL evidence was detected. Do not continue."
                }
            }

            ProtectionDecision.WARN -> {
                if (providerEvidence.any { it.result?.reputation == ReputationVerdict.SUSPICIOUS }) {
                    "A reputation provider reported suspicious evidence. Proceed only after verification."
                } else if (localScore >= WARN_THRESHOLD) {
                    "Local URL analysis found meaningful risk signals. Proceed with caution."
                } else {
                    "Suspicious evidence was detected. Proceed with caution."
                }
            }

            ProtectionDecision.ALLOW -> when {
                incompleteLookup ->
                    "Limited online verification. Proceed with caution."
                hasUnknown ->
                    "No strong threat evidence was detected; reputation providers returned no conclusive verdict."
                providerEvidence.isEmpty() ->
                    "No strong threat evidence was detected by local analysis; no online reputation result was available."
                else ->
                    "No strong threat evidence was detected."
            }
        }
    }

    private fun buildExplanation(summary: String, reasons: List<ScanReason>): String {
        if (reasons.isEmpty()) {
            return summary
        }
        val detail = reasons.joinToString("; ") { reason ->
            "${reason.sourceName}: ${reason.message.trim().trimEnd('.')}"
        }
        return "$summary Evidence: $detail."
    }

    private fun boundedScore(value: Float): Float = when {
        value.isNaN() -> MIN_SCORE
        value == Float.POSITIVE_INFINITY -> MAX_SCORE
        value == Float.NEGATIVE_INFINITY -> MIN_SCORE
        else -> value.coerceIn(MIN_SCORE, MAX_SCORE)
    }

    private fun boundedConfidence(value: Float): Float = when {
        value.isNaN() -> 0f
        value == Float.POSITIVE_INFINITY -> 1f
        value == Float.NEGATIVE_INFINITY -> 0f
        else -> value.coerceIn(0f, 1f)
    }

    companion object {
        const val WARN_THRESHOLD = 30f
        const val BLOCK_THRESHOLD = 90f
        const val MIN_SCORE = 0f
        const val MAX_SCORE = 100f
        const val MALICIOUS_WEIGHT = 0.90f
        const val SUSPICIOUS_WEIGHT = 0.60f
    }
}
