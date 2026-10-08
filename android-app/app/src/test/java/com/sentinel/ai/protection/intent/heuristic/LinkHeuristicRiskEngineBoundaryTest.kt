package com.sentinel.ai.protection.intent.heuristic

import com.sentinel.ai.core.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class LinkHeuristicRiskEngineBoundaryTest {

    private val engine = LinkHeuristicRiskEngine()

    @Test
    fun `score immediately below 30 is green and 30 is yellow`() {
        assertEquals(RiskLevel.GREEN, Math.nextDown(30f).toRiskLevel())
        assertEquals(RiskLevel.YELLOW, 30f.toRiskLevel())
    }

    @Test
    fun `score immediately below 70 is yellow and 70 is red`() {
        assertEquals(RiskLevel.YELLOW, Math.nextDown(70f).toRiskLevel())
        assertEquals(RiskLevel.RED, 70f.toRiskLevel())
    }

    @Test
    fun `score immediately below 90 is red and 90 is critical`() {
        assertEquals(RiskLevel.RED, Math.nextDown(90f).toRiskLevel())
        assertEquals(RiskLevel.CRITICAL, 90f.toRiskLevel())
    }

    @Test
    fun `score endpoints map to green and critical`() {
        assertEquals(RiskLevel.GREEN, 0f.toRiskLevel())
        assertEquals(RiskLevel.CRITICAL, 100f.toRiskLevel())
    }

    @Test
    fun `URL length threshold is exclusive at 150 characters`() {
        val atThreshold = "https://example.com/#" + "a".repeat(129)
        val overThreshold = "https://example.com/#" + "a".repeat(130)
        assertEquals(150, atThreshold.length)
        assertEquals(151, overThreshold.length)

        assertLinkAnalysis(engine, atThreshold, 0f, RiskLevel.GREEN)
        assertLinkAnalysis(
            engine,
            overThreshold,
            5f,
            RiskLevel.GREEN,
            "excessive_length" to expectedRule(
                5f,
                "URL is unusually long",
                RuleCategory.URL_STRUCTURE
            )
        )
    }

    @Test
    fun `path nesting threshold is exclusive at four segments`() {
        assertLinkAnalysis(engine, "https://example.com/a/b/c/d", 0f, RiskLevel.GREEN)
        assertLinkAnalysis(
            engine,
            "https://example.com/a/b/c/d/e",
            5f,
            RiskLevel.GREEN,
            "deep_nesting" to expectedRule(
                5f,
                "URL path is deeply nested",
                RuleCategory.URL_STRUCTURE
            )
        )
    }

    @Test
    fun `filename length threshold is exclusive at 30 characters`() {
        assertLinkAnalysis(
            engine,
            "https://example.com/${"a".repeat(30)}",
            0f,
            RiskLevel.GREEN
        )
        assertLinkAnalysis(
            engine,
            "https://example.com/${"a".repeat(31)}",
            5f,
            RiskLevel.GREEN,
            "long_filename" to expectedRule(
                5f,
                "URL contains an unusually long filename",
                RuleCategory.URL_STRUCTURE
            )
        )
    }

    @Test
    fun `query parameter threshold is exclusive at five parameters`() {
        assertLinkAnalysis(
            engine,
            "https://example.com/?a=1&b=2&c=3&d=4&e=5",
            0f,
            RiskLevel.GREEN
        )
        assertLinkAnalysis(
            engine,
            "https://example.com/?a=1&b=2&c=3&d=4&e=5&f=6",
            10f,
            RiskLevel.GREEN,
            "excessive_query" to expectedRule(
                10f,
                "URL has excessive query parameters",
                RuleCategory.URL_STRUCTURE
            )
        )
    }

    @Test
    fun `subdomain threshold is exclusive at three host dots`() {
        assertLinkAnalysis(engine, "https://a.b.example.com", 0f, RiskLevel.GREEN)
        assertLinkAnalysis(
            engine,
            "https://a.b.c.example.com",
            10f,
            RiskLevel.GREEN,
            "excessive_subdomains" to expectedRule(
                10f,
                "Contains excessive subdomains",
                RuleCategory.DOMAIN
            )
        )
    }

    @Test
    fun `digit ratio threshold is strict greater than point three`() {
        assertLinkAnalysis(engine, "https://abcdefg123.com", 0f, RiskLevel.GREEN)
        assertLinkAnalysis(
            engine,
            "https://abcdefg1234.com",
            10f,
            RiskLevel.GREEN,
            "excessive_digits" to expectedRule(
                10f,
                "Contains excessive digits in the domain",
                RuleCategory.DOMAIN
            )
        )
    }

    @Test
    fun `three host hyphens trigger while two do not`() {
        assertLinkAnalysis(engine, "https://a-b-c.example.com", 0f, RiskLevel.GREEN)
        assertLinkAnalysis(
            engine,
            "https://a-b-c-d.example.com",
            10f,
            RiskLevel.GREEN,
            "repeated_hyphens" to expectedRule(
                10f,
                "Contains repeated or multiple hyphens in domain",
                RuleCategory.DOMAIN
            )
        )
    }

    @Test
    fun `three encoded bytes trigger while two do not`() {
        assertLinkAnalysis(engine, "https://example.com/%41%42", 0f, RiskLevel.GREEN)
        assertLinkAnalysis(
            engine,
            "https://example.com/%41%42%43",
            10f,
            RiskLevel.GREEN,
            "encoded_chars" to expectedRule(
                10f,
                "URL contains many encoded characters",
                RuleCategory.URL_STRUCTURE
            )
        )
    }

    @Test
    fun `empty input does not throw and remains zero score`() {
        assertLinkAnalysis(engine, "", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `whitespace input does not throw and remains zero score`() {
        assertLinkAnalysis(engine, " ", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `plain malformed text does not throw and remains zero score`() {
        assertLinkAnalysis(engine, "not a url", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `bare domain is parsed through HTTPS fallback`() {
        assertLinkAnalysis(engine, "example.com", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `bare www domain is parsed through HTTPS fallback`() {
        assertLinkAnalysis(engine, "www.example.com", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `incomplete schemes and broken prefix do not throw`() {
        listOf("http://", "https://", "://broken").forEach { input ->
            assertLinkAnalysis(engine, input, 0f, RiskLevel.GREEN)
        }
    }

    @Test
    fun `trailing slash variants preserve zero score`() {
        listOf("https://example.com/", "https://example.com////").forEach { input ->
            assertLinkAnalysis(engine, input, 0f, RiskLevel.GREEN)
        }
    }

    @Test
    fun `uppercase scheme and host are parsed and matched case insensitively`() {
        assertLinkAnalysis(
            engine,
            "HTTPS://EXAMPLE.XYZ",
            15f,
            RiskLevel.GREEN,
            "suspicious_tld" to expectedRule(
                15f,
                "Uses .xyz domain",
                RuleCategory.DOMAIN
            )
        )
    }

    @Test
    fun `fragment keyword currently receives path query explanation and two points`() {
        assertLinkAnalysis(
            engine,
            "https://example.com/#login",
            2f,
            RiskLevel.GREEN,
            "social_engineering" to expectedRule(
                2f,
                "Uses social engineering keyword in path/query: login",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }

    @Test
    fun `query keyword currently receives discounted two point score`() {
        assertLinkAnalysis(
            engine,
            "https://example.com/?action=login",
            2f,
            RiskLevel.GREEN,
            "social_engineering" to expectedRule(
                2f,
                "Uses social engineering keyword in path/query: login",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }

    @Test
    fun `Unicode hostname is normalized and inspected as IDN`() {
        val analysis = engine.analyze("https://bücher.example")
        org.junit.Assert.assertEquals(25f, analysis.score, 0f)
        org.junit.Assert.assertEquals(RiskLevel.GREEN, analysis.riskLevel)
        org.junit.Assert.assertTrue(analysis.ruleResults.any { it.triggered && it.explanation == "Punycode domain name detected" })
    }

    @Test
    fun `IPv6 literal currently stacks IP and excessive digit signals`() {
        assertLinkAnalysis(
            engine,
            "https://[2001:db8::1]/",
            35f,
            RiskLevel.YELLOW,
            "ip_address" to expectedRule(
                25f,
                "Uses IP address instead of domain",
                RuleCategory.DOMAIN
            ),
            "excessive_digits" to expectedRule(
                10f,
                "Contains excessive digits in the domain",
                RuleCategory.DOMAIN
            )
        )
    }

    @Test
    fun `very long URL remains deterministic and only triggers raw length`() {
        val url = "https://example.com/#" + "a".repeat(1_000)
        assertLinkAnalysis(
            engine,
            url,
            5f,
            RiskLevel.GREEN,
            "excessive_length" to expectedRule(
                5f,
                "URL is unusually long",
                RuleCategory.URL_STRUCTURE
            )
        )
        assertEquals(engine.analyze(url), engine.analyze(url))
    }

    @Test
    fun `bare suspicious TLD is evaluated after fallback scheme insertion`() {
        assertLinkAnalysis(
            engine,
            "example.xyz",
            15f,
            RiskLevel.GREEN,
            "suspicious_tld" to expectedRule(
                15f,
                "Uses .xyz domain",
                RuleCategory.DOMAIN
            )
        )
    }

    @Test
    fun `unparseable text can still trigger raw social keyword rule`() {
        assertLinkAnalysis(
            engine,
            "login not a url",
            2f,
            RiskLevel.GREEN,
            "social_engineering" to expectedRule(
                2f,
                "Uses social engineering keyword in path/query: login",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }
}
