package com.sentinel.ai.protection.intent.heuristic

import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.protection.intent.heuristic.rules.file.DangerousExtensionRule
import com.sentinel.ai.protection.intent.heuristic.rules.file.DoubleExtensionRule
import com.sentinel.ai.protection.intent.heuristic.rules.file.FakeDocumentRule
import com.sentinel.ai.protection.intent.heuristic.rules.file.MisleadingFilenameRule
import com.sentinel.ai.protection.intent.heuristic.rules.file.RandomFilenameRule
import com.sentinel.ai.protection.intent.heuristic.rules.file.SuspiciousArchiveRule
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileHeuristicRiskEngine @Inject constructor() {

    private val config: FileHeuristicConfig = FileHeuristicConfig()
    private val rules: Collection<FileHeuristicRule> = defaultRules()

    fun analyze(filename: String): FileHeuristicAnalysis {
        val results = rules.map { rule -> rule.evaluate(filename, config) }
        val score = results.sumOf { it.scoreContribution.toDouble() }.toFloat().coerceIn(0f, 100f)
        val triggered = results.filter { it.triggered }

        return FileHeuristicAnalysis(
            score = score,
            riskLevel = score.toRiskLevel(),
            ruleResults = results,
            explanation = buildExplanation(triggered),
            triggeredRuleCount = triggered.size
        )
    }

    fun toScanResult(filename: String, fileType: String): ScanResult {
        val analysis = analyze(filename)
        return ScanResult(
            id = UUID.randomUUID().toString(),
            source = "Intent (File)",
            senderDisplayName = null,
            senderIdentifier = null,
            riskLevel = analysis.riskLevel,
            riskScore = analysis.score,
            explanation = "${analysis.explanation} Type: $fileType",
            timestamp = System.currentTimeMillis()
        )
    }

    private fun buildExplanation(triggered: List<RuleResult>): String {
        if (triggered.isEmpty()) {
            return "No heuristic risk signals found. Only supported file properties were checked."
        }

        val reasons = triggered.mapNotNull { it.explanation }.take(4).joinToString("; ")
        return "Detected ${triggered.size} file risk signal(s): $reasons."
    }

    companion object {
        private const val CALIBRATION_LOG_TAG = "SentinelCalibration"

        fun defaultRules(): Collection<FileHeuristicRule> = listOf(
            DangerousExtensionRule(),
            DoubleExtensionRule(),
            FakeDocumentRule(),
            SuspiciousArchiveRule(),
            RandomFilenameRule(),
            MisleadingFilenameRule()
        )
    }
}

data class FileHeuristicAnalysis(
    val score: Float,
    val riskLevel: RiskLevel,
    val ruleResults: List<RuleResult>,
    val explanation: String,
    val triggeredRuleCount: Int
)
