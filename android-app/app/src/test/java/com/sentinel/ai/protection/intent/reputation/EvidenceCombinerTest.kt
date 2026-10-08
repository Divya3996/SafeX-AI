package com.sentinel.ai.protection.intent.reputation

import com.sentinel.ai.core.model.EvidenceSourceStatus
import com.sentinel.ai.core.model.LocalEvidence
import com.sentinel.ai.core.model.LocalFinding
import com.sentinel.ai.core.model.ProtectionAction
import com.sentinel.ai.core.model.ProtectionDecision
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanReasonSource
import com.sentinel.ai.core.model.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceCombinerTest {

    private val combiner = EvidenceCombiner()

    @Test
    fun `local low risk allows without claiming guaranteed safety`() {
        val result = combine(localResult(10f))

        assertEquals(ProtectionDecision.ALLOW, result.decision)
        assertEquals(RiskLevel.GREEN, result.riskLevel)
        assertEquals(10f, result.riskScore, 0f)
        assertEquals(ProtectionAction.CONTINUE, result.recommendedAction)
        assertTrue(result.summary.contains("No strong threat evidence"))
        assertFalse(result.summary.contains("safe", ignoreCase = true))
    }

    @Test
    fun `local medium risk warns`() {
        val result = combine(localResult(30f))

        assertEquals(ProtectionDecision.WARN, result.decision)
        assertEquals(RiskLevel.YELLOW, result.riskLevel)
        assertEquals(30f, result.riskScore, 0f)
    }

    @Test
    fun `local high risk warns until the critical threshold`() {
        val result = combine(localResult(70f))

        assertEquals(ProtectionDecision.WARN, result.decision)
        assertEquals(RiskLevel.RED, result.riskLevel)
        assertEquals(70f, result.riskScore, 0f)
    }

    @Test
    fun `local critical risk blocks`() {
        val result = combine(localResult(90f))

        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
        assertEquals(ProtectionAction.DO_NOT_CONTINUE, result.recommendedAction)
    }

    @Test
    fun `local reasons and rule attribution are preserved`() {
        val result = combine(
            localResult(
                score = 35f,
                findings = listOf(
                    localFinding("suspicious_tld", 15f, "Uses .xyz domain"),
                    localFinding("social_engineering", 20f, "Uses a login keyword")
                )
            )
        )

        assertEquals(listOf("suspicious_tld", "social_engineering"), result.localEvidence?.findings?.map { it.ruleId })
        assertEquals(listOf("Uses .xyz domain", "Uses a login keyword"), result.reasons.map { it.message })
        assertTrue(result.reasons.all { it.source == ScanReasonSource.LOCAL_HEURISTIC })
    }

    @Test
    fun `local score is capped at one hundred`() {
        val result = combine(localResult(140f))

        assertEquals(100f, result.riskScore, 0f)
        assertEquals(100f, result.localEvidence?.score ?: -1f, 0f)
        assertEquals(ProtectionDecision.BLOCK, result.decision)
    }

    @Test
    fun `non-finite scores and confidence are normalized to bounded deterministic values`() {
        val infiniteLocal = combine(localResult(Float.POSITIVE_INFINITY))
        val nanLocal = combine(localResult(Float.NaN))
        val nanProvider = combine(
            localResult(0f),
            malicious("Provider", Float.NaN, "Malicious verdict with invalid confidence.")
        )

        assertEquals(100f, infiniteLocal.riskScore, 0f)
        assertEquals(ProtectionDecision.BLOCK, infiniteLocal.decision)
        assertEquals(0f, nanLocal.riskScore, 0f)
        assertEquals(ProtectionDecision.ALLOW, nanLocal.decision)
        assertEquals(EvidenceCombiner.BLOCK_THRESHOLD, nanProvider.riskScore, 0f)
        assertEquals(0f, nanProvider.confidence, 0f)
        assertEquals(0f, nanProvider.providerFindings.single().confidence, 0f)
    }

    @Test
    fun `score immediately below warning threshold allows`() {
        assertEquals(ProtectionDecision.ALLOW, combine(localResult(29.999f)).decision)
    }

    @Test
    fun `score at warning threshold warns`() {
        assertEquals(ProtectionDecision.WARN, combine(localResult(EvidenceCombiner.WARN_THRESHOLD)).decision)
    }

    @Test
    fun `score immediately below red risk boundary remains yellow and warns`() {
        val result = combine(localResult(69.999f))

        assertEquals(RiskLevel.YELLOW, result.riskLevel)
        assertEquals(ProtectionDecision.WARN, result.decision)
    }

    @Test
    fun `score at red risk boundary remains a warning decision`() {
        val result = combine(localResult(70f))

        assertEquals(RiskLevel.RED, result.riskLevel)
        assertEquals(ProtectionDecision.WARN, result.decision)
    }

    @Test
    fun `score immediately below block threshold warns`() {
        assertEquals(ProtectionDecision.WARN, combine(localResult(89.999f)).decision)
    }

    @Test
    fun `score at block threshold blocks`() {
        assertEquals(ProtectionDecision.BLOCK, combine(localResult(EvidenceCombiner.BLOCK_THRESHOLD)).decision)
    }

    @Test
    fun `OpenPhish malicious blocks clean local result`() {
        val result = combine(localResult(0f), malicious("OpenPhish", 0.98f, "Matched OpenPhish feed entry."))

        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertTrue(result.riskScore >= EvidenceCombiner.BLOCK_THRESHOLD)
        assertEquals("OpenPhish", result.providerFindings.single().providerName)
        assertEquals("MALICIOUS", result.providerFindings.single().verdict)
    }

    @Test
    fun `VirusTotal malicious blocks clean local result`() {
        val result = combine(localResult(0f), malicious("VirusTotal", 0.95f, "One engine detected malware."))

        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
    }

    @Test
    fun `VirusTotal suspicious produces at least warning`() {
        val result = combine(localResult(0f), suspicious("VirusTotal", 0.75f, "One engine was suspicious."))

        assertEquals(ProtectionDecision.WARN, result.decision)
        assertEquals(45f, result.riskScore, 0.001f)
    }

    @Test
    fun `provider unknown does not force warning or imply clean`() {
        val result = combine(localResult(0f), unknown("OpenPhish"))

        assertEquals(ProtectionDecision.ALLOW, result.decision)
        assertEquals(EvidenceSourceStatus.UNKNOWN, result.providerFindings.single().status)
        assertEquals("UNKNOWN", result.providerFindings.single().verdict)
        assertEquals(0f, result.confidence, 0f)
        assertTrue(result.summary.contains("no conclusive verdict"))
    }

    @Test
    fun `provider null or unavailable does not force warning`() {
        val result = combine(localResult(0f), ReputationEvidence.unavailable("OpenPhish"))

        assertEquals(ProtectionDecision.ALLOW, result.decision)
        assertEquals(EvidenceSourceStatus.UNAVAILABLE, result.providerFindings.single().status)
        assertEquals("Limited online verification. Proceed with caution.", result.summary)
        assertFalse(result.reasons.single().message.contains("malicious", ignoreCase = true))
    }

    @Test
    fun `multiple unknowns do not produce false safety wording`() {
        val result = combine(localResult(0f), unknown("VirusTotal"), unknown("OpenPhish"))

        assertEquals(ProtectionDecision.ALLOW, result.decision)
        assertEquals(2, result.providerFindings.size)
        assertFalse(result.summary.contains("safe", ignoreCase = true))
        assertEquals(0f, result.confidence, 0f)
    }

    @Test
    fun `local low plus VirusTotal suspicious warns`() {
        val result = combine(localResult(10f), suspicious("VirusTotal", 0.75f, "Suspicious engines."))

        assertEquals(ProtectionDecision.WARN, result.decision)
        assertTrue(result.riskScore >= EvidenceCombiner.WARN_THRESHOLD)
    }

    @Test
    fun `local high plus all providers unknown follows local warning`() {
        val result = combine(localResult(75f), unknown("OpenPhish"), unknown("VirusTotal"))

        assertEquals(ProtectionDecision.WARN, result.decision)
        assertEquals(75f, result.riskScore, 0f)
    }

    @Test
    fun `local medium plus malicious provider blocks`() {
        val result = combine(localResult(30f), malicious("OpenPhish", 0.98f, "Feed match."))

        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertTrue(result.riskScore >= EvidenceCombiner.BLOCK_THRESHOLD)
    }

    @Test
    fun `local critical plus provider failures remains blocked`() {
        val result = combine(
            localResult(90f),
            ReputationEvidence.failed("OpenPhish"),
            ReputationEvidence.timedOut("VirusTotal")
        )

        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertEquals(90f, result.riskScore, 0f)
    }

    @Test
    fun `local low plus all provider failures allows with limited coverage wording`() {
        val result = combine(
            localResult(0f),
            ReputationEvidence.failed("OpenPhish"),
            ReputationEvidence.timedOut("VirusTotal")
        )

        assertEquals(ProtectionDecision.ALLOW, result.decision)
        assertEquals(0f, result.confidence, 0f)
        assertEquals("Limited online verification. Proceed with caution.", result.summary)
    }

    @Test
    fun `local suspicious plus provider failure warns`() {
        val result = combine(localResult(35f), ReputationEvidence.failed("OpenPhish"))

        assertEquals(ProtectionDecision.WARN, result.decision)
        assertEquals(35f, result.riskScore, 0f)
    }

    @Test
    fun `malicious plus suspicious provider combination blocks`() {
        val result = combine(
            localResult(0f),
            suspicious("VirusTotal", 0.75f, "Suspicious engines."),
            malicious("OpenPhish", 0.98f, "Feed match.")
        )

        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertEquals("OpenPhish", result.reasons.first().sourceName)
    }

    @Test
    fun `clean result cannot cancel malicious evidence`() {
        val result = combine(
            localResult(0f),
            clean("CleanProvider", 1f),
            malicious("MaliciousProvider", 0.5f, "Confirmed malicious.")
        )

        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertEquals(EvidenceCombiner.BLOCK_THRESHOLD, result.riskScore, 0f)
    }

    @Test
    fun `clean result cannot discount local risk`() {
        val result = combine(localResult(80f), clean("CleanProvider", 1f))

        assertEquals(80f, result.riskScore, 0f)
        assertEquals(ProtectionDecision.WARN, result.decision)
    }

    @Test
    fun `multiple malicious providers increase confidence without exceeding one`() {
        val single = combine(localResult(0f), malicious("OpenPhish", 0.8f, "Feed match."))
        val multiple = combine(
            localResult(0f),
            malicious("OpenPhish", 0.8f, "Feed match."),
            malicious("VirusTotal", 0.8f, "Engine match.")
        )

        assertTrue(multiple.confidence > single.confidence)
        assertEquals(0.96f, multiple.confidence, 0.001f)
        assertTrue(multiple.confidence <= 1f)
        assertTrue(multiple.riskScore <= 100f)
    }

    @Test
    fun `duplicate provider evidence is counted once using strongest verdict`() {
        val result = combine(
            localResult(0f),
            unknown("VirusTotal"),
            malicious("VirusTotal", 0.8f, "Engine match."),
            malicious("VirusTotal", 0.8f, "Engine match.")
        )

        assertEquals(1, result.providerFindings.size)
        assertEquals(0.8f, result.confidence, 0f)
        assertEquals(ProtectionDecision.BLOCK, result.decision)
    }

    @Test
    fun `provider names reasons confidence and statuses are preserved`() {
        val result = combine(
            localResult(0f),
            malicious("OpenPhish", 0.98f, "Matched feed."),
            ReputationEvidence.timedOut("VirusTotal")
        )

        assertEquals(listOf("OpenPhish", "VirusTotal"), result.providerFindings.map { it.providerName })
        assertEquals(listOf("Matched feed.", "Provider lookup timed out."), result.providerFindings.map { it.reason })
        assertEquals(0.98f, result.providerFindings.first().confidence, 0f)
        assertEquals(EvidenceSourceStatus.TIMED_OUT, result.providerFindings.last().status)
    }

    @Test
    fun `reasons are deduplicated in stable severity order`() {
        val duplicate = "Same signal"
        val result = combine(
            localResult(35f, listOf(localFinding("local_duplicate", 35f, duplicate))),
            unknown("UnknownProvider"),
            suspicious("SuspiciousProvider", 0.75f, "Suspicious signal"),
            malicious("MaliciousProvider", 0.9f, duplicate),
            ReputationEvidence.failed("FailedProvider")
        )

        assertEquals(
            listOf("MaliciousProvider", "SuspiciousProvider", "UnknownProvider", "FailedProvider"),
            result.reasons.map { it.sourceName }
        )
        assertEquals(1, result.reasons.count { it.message == duplicate })
        assertFalse(result.reasons.last().message.contains("malicious", ignoreCase = true))
    }

    @Test
    fun `summary and action match each decision`() {
        val allow = combine(localResult(0f))
        val warn = combine(localResult(30f))
        val block = combine(localResult(90f))

        assertEquals(ProtectionAction.CONTINUE, allow.recommendedAction)
        assertTrue(allow.headline.contains("No strong"))
        assertEquals(ProtectionAction.PROCEED_WITH_CAUTION, warn.recommendedAction)
        assertTrue(warn.summary.contains("caution", ignoreCase = true))
        assertEquals(ProtectionAction.DO_NOT_CONTINUE, block.recommendedAction)
        assertTrue(block.summary.contains("Do not continue"))
    }

    @Test
    fun `same inputs always produce identical output`() {
        val local = localResult(
            35f,
            listOf(localFinding("suspicious_tld", 15f, "Uses .xyz domain"))
        )
        val evidence = arrayOf(
            suspicious("VirusTotal", 0.75f, "Suspicious engines."),
            unknown("OpenPhish")
        )

        val expected = combine(local, *evidence)
        repeat(20) {
            assertEquals(expected, combine(local, *evidence))
        }
    }

    private fun combine(
        local: ScanResult,
        vararg evidence: ReputationEvidence
    ): ScanResult = combiner.combine(local, evidence.toList())

    private fun localResult(
        score: Float,
        findings: List<LocalFinding> = if (score > 0f) {
            listOf(localFinding("test_rule", score, "Local risk signal."))
        } else {
            emptyList()
        }
    ): ScanResult {
        val boundedScore = score.coerceIn(0f, 100f)
        val level = riskLevel(boundedScore)
        return ScanResult(
            id = "test-id",
            source = "Intent (Link)",
            riskLevel = level,
            riskScore = score,
            explanation = if (findings.isEmpty()) "No heuristic risk signals found." else "Local evidence.",
            timestamp = 1L,
            localEvidence = LocalEvidence(
                score = score,
                riskLevel = level,
                findings = findings,
                triggeredRuleCount = findings.size
            )
        )
    }

    private fun localFinding(ruleId: String, score: Float, reason: String) = LocalFinding(
        ruleId = ruleId,
        ruleName = ruleId,
        category = "URL_STRUCTURE",
        scoreContribution = score,
        reason = reason
    )

    private fun malicious(
        provider: String,
        confidence: Float,
        reason: String
    ) = completed(provider, confidence, ReputationVerdict.MALICIOUS, reason)

    private fun suspicious(
        provider: String,
        confidence: Float,
        reason: String
    ) = completed(provider, confidence, ReputationVerdict.SUSPICIOUS, reason)

    private fun clean(provider: String, confidence: Float) = completed(
        provider,
        confidence,
        ReputationVerdict.CLEAN,
        "Provider reported clean."
    )

    private fun unknown(provider: String) = completed(
        provider,
        0f,
        ReputationVerdict.UNKNOWN,
        "No conclusive reputation verdict."
    )

    private fun completed(
        provider: String,
        confidence: Float,
        verdict: ReputationVerdict,
        reason: String
    ) = ReputationEvidence.completed(
        ReputationResult(
            providerName = provider,
            confidence = confidence,
            reputation = verdict,
            reason = reason,
            timestamp = 1L
        )
    )

    private fun riskLevel(score: Float): RiskLevel = when {
        score >= 90f -> RiskLevel.CRITICAL
        score >= 70f -> RiskLevel.RED
        score >= 30f -> RiskLevel.YELLOW
        else -> RiskLevel.GREEN
    }
}
