package com.sentinel.ai.protection.intent.heuristic

import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.protection.intent.heuristic.rules.link.BrandImpersonationRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.DeepPathRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.EmbeddedUrlRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.EncodedCharactersRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.ExcessiveDigitsRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.ExcessiveQueryParametersRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.ExcessiveSubdomainsRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.IpAddressRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.InsecureHttpRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.LongFilenameRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.LongUrlRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.NonStandardPortRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.PunycodeRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.RandomHostnameRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.RedirectParameterRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.RepeatedHyphensRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.SocialEngineeringRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.SuspiciousTldRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.TrackingParameterRule
import com.sentinel.ai.protection.intent.heuristic.rules.link.UserinfoDeceptionRule
import com.sentinel.ai.protection.intent.link.UrlNormalizer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkHeuristicRiskEngine @Inject constructor() {

    private val config: LinkHeuristicConfig = LinkHeuristicConfig()
    private val rules: Collection<LinkHeuristicRule> = defaultRules()

    fun analyze(url: String): LinkHeuristicAnalysis {
        val originalUrl = url.trim()
        val parsedUrl = UrlNormalizer.parse(originalUrl)
        val results = rules.map { rule -> rule.evaluate(parsedUrl, config) }
        val score = results.sumOf { it.scoreContribution.toDouble() }.toFloat().coerceIn(0f, 100f)
        val triggered = results.filter { it.triggered }

        return LinkHeuristicAnalysis(
            score = score,
            riskLevel = score.toRiskLevel(),
            ruleResults = results,
            explanation = buildExplanation(triggered),
            triggeredRuleCount = triggered.size
        )
    }

    fun toScanResult(url: String): ScanResult {
        val analysis = analyze(url)
        return ScanResult(
            id = UUID.randomUUID().toString(),
            source = "Intent (Link)",
            senderDisplayName = null,
            senderIdentifier = null,
            riskLevel = analysis.riskLevel,
            riskScore = analysis.score,
            explanation = analysis.explanation,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun buildExplanation(triggered: List<RuleResult>): String {
        if (triggered.isEmpty()) {
            return "No heuristic risk signals found. Page content was not inspected."
        }

        val reasons = triggered.mapNotNull { it.explanation }.take(4).joinToString("; ")
        return "Detected ${triggered.size} link risk signal(s): $reasons."
    }

    companion object {
        private const val CALIBRATION_LOG_TAG = "SentinelCalibration"

        fun defaultRules(): Collection<LinkHeuristicRule> = listOf(
            SuspiciousTldRule(),
            IpAddressRule(),
            ExcessiveSubdomainsRule(),
            RandomHostnameRule(),
            RepeatedHyphensRule(),
            ExcessiveDigitsRule(),
            PunycodeRule(),
            LongUrlRule(),
            DeepPathRule(),
            LongFilenameRule(),
            ExcessiveQueryParametersRule(),
            EncodedCharactersRule(),
            TrackingParameterRule(),
            RedirectParameterRule(),
            BrandImpersonationRule(),
            SocialEngineeringRule(),
            InsecureHttpRule(),
            NonStandardPortRule(),
            UserinfoDeceptionRule(),
            EmbeddedUrlRule(),
            com.sentinel.ai.protection.intent.heuristic.rules.link.ShortenedDestinationRule(),
            com.sentinel.ai.protection.intent.heuristic.rules.link.NumericHostEncodingRule(),
            com.sentinel.ai.protection.intent.heuristic.rules.link.LayeredDestinationRule(),
            com.sentinel.ai.protection.intent.heuristic.rules.link.ExecutableDownloadRule(),
            com.sentinel.ai.protection.intent.heuristic.rules.link.SensitiveHostCueRule(),
            com.sentinel.ai.protection.intent.heuristic.rules.link.BrandCamouflageRule()
        )
    }
}

data class LinkHeuristicAnalysis(
    val score: Float,
    val riskLevel: RiskLevel,
    val ruleResults: List<RuleResult>,
    val explanation: String,
    val triggeredRuleCount: Int
)
