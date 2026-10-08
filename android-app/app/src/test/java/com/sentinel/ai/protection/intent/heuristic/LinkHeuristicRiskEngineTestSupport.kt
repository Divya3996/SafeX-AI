package com.sentinel.ai.protection.intent.heuristic

import com.sentinel.ai.core.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

internal val EXPECTED_LINK_RULE_IDS = listOf(
    "suspicious_tld",
    "ip_address",
    "excessive_subdomains",
    "random_hostname",
    "repeated_hyphens",
    "excessive_digits",
    "punycode",
    "excessive_length",
    "deep_nesting",
    "long_filename",
    "excessive_query",
    "encoded_chars",
    "tracking_parameters",
    "suspicious_redirect",
    "brand_impersonation",
    "social_engineering",
    "insecure_http",
    "non_standard_port",
    "userinfo_deception",
    "embedded_url",
    "shortened_destination",
    "numeric_host_encoding",
    "layered_destination",
    "executable_download",
    "sensitive_host_cues",
    "brand_camouflage"
)

internal data class ExpectedLinkRule(
    val score: Float,
    val explanation: String,
    val category: RuleCategory
)

internal fun expectedRule(
    score: Float,
    explanation: String,
    category: RuleCategory
): ExpectedLinkRule = ExpectedLinkRule(score, explanation, category)

internal fun assertLinkAnalysis(
    engine: LinkHeuristicRiskEngine,
    url: String,
    expectedScore: Float,
    expectedRiskLevel: RiskLevel,
    vararg expectedRuleEntries: Pair<String, ExpectedLinkRule>
): LinkHeuristicAnalysis {
    val expectedRules = linkedMapOf(*expectedRuleEntries)
    assertEquals("Expected rule IDs must be unique", expectedRuleEntries.size, expectedRules.size)

    val analysis = engine.analyze(url)
    assertEquals("score for $url", expectedScore, analysis.score, 0f)
    assertEquals("risk level for $url", expectedRiskLevel, analysis.riskLevel)
    assertEquals("rule result count for $url", EXPECTED_LINK_RULE_IDS.size, analysis.ruleResults.size)
    assertEquals("triggered rule count for $url", expectedRules.size, analysis.triggeredRuleCount)

    EXPECTED_LINK_RULE_IDS.zip(analysis.ruleResults).forEach { (ruleId, result) ->
        val expected = expectedRules[ruleId]
        if (expected == null) {
            assertFalse("$ruleId should not trigger for $url", result.triggered)
            assertEquals("$ruleId contribution for $url", 0f, result.scoreContribution, 0f)
            assertNull("$ruleId explanation for $url", result.explanation)
        } else {
            assertTrue("$ruleId should trigger for $url", result.triggered)
            assertEquals("$ruleId contribution for $url", expected.score, result.scoreContribution, 0f)
            assertEquals("$ruleId explanation for $url", expected.explanation, result.explanation)
            assertEquals("$ruleId category for $url", expected.category, result.category)
        }
    }

    val expectedExplanation = if (expectedRules.isEmpty()) {
        "No heuristic risk signals found. Page content was not inspected."
    } else {
        val reasons = EXPECTED_LINK_RULE_IDS
            .mapNotNull { expectedRules[it]?.explanation }
            .take(4)
            .joinToString("; ")
        "Detected ${expectedRules.size} link risk signal(s): $reasons."
    }
    assertEquals("combined explanation for $url", expectedExplanation, analysis.explanation)

    return analysis
}
