package com.sentinel.ai.protection.intent.heuristic

import com.sentinel.ai.core.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class LinkHeuristicRiskEngineLocalUrlRulesTest {

    private val engine = LinkHeuristicRiskEngine()

    @Test
    fun `explicit HTTP reaches warning while HTTPS and bare domains remain clean`() {
        assertLinkAnalysis(
            engine,
            "http://example.com",
            30f,
            RiskLevel.YELLOW,
            "insecure_http" to expectedRule(30f, "Uses insecure HTTP protocol", RuleCategory.URL_STRUCTURE)
        )
        assertLinkAnalysis(engine, "https://example.com", 0f, RiskLevel.GREEN)
        assertLinkAnalysis(engine, "example.com", 0f, RiskLevel.GREEN)
        assertLinkAnalysis(
            engine,
            "HTTP://EXAMPLE.COM",
            30f,
            RiskLevel.YELLOW,
            "insecure_http" to expectedRule(30f, "Uses insecure HTTP protocol", RuleCategory.URL_STRUCTURE)
        )
    }

    @Test
    fun `default ports remain clean and explicit non-default ports contribute ten points`() {
        assertLinkAnalysis(engine, "http://example.com:80", 30f, RiskLevel.YELLOW,
            "insecure_http" to expectedRule(30f, "Uses insecure HTTP protocol", RuleCategory.URL_STRUCTURE))
        assertLinkAnalysis(engine, "https://example.com:443", 0f, RiskLevel.GREEN)
        assertLinkAnalysis(engine, "http://example.com:8080", 40f, RiskLevel.YELLOW,
            "insecure_http" to expectedRule(30f, "Uses insecure HTTP protocol", RuleCategory.URL_STRUCTURE),
            "non_standard_port" to expectedRule(10f, "The URL uses a non-standard network port", RuleCategory.URL_STRUCTURE))
        assertLinkAnalysis(engine, "https://example.com:8443", 10f, RiskLevel.GREEN,
            "non_standard_port" to expectedRule(10f, "The URL uses a non-standard network port", RuleCategory.URL_STRUCTURE))
    }

    @Test
    fun `IPv6 ports are parsed without treating host colons as ports`() {
        assertLinkAnalysis(
            engine,
            "https://[2001:db8::1]:443",
            35f,
            RiskLevel.YELLOW,
            "ip_address" to expectedRule(25f, "Uses IP address instead of domain", RuleCategory.DOMAIN),
            "excessive_digits" to expectedRule(10f, "Contains excessive digits in the domain", RuleCategory.DOMAIN)
        )
        assertLinkAnalysis(
            engine,
            "https://[2001:db8::1]:8443",
            45f,
            RiskLevel.YELLOW,
            "ip_address" to expectedRule(25f, "Uses IP address instead of domain", RuleCategory.DOMAIN),
            "excessive_digits" to expectedRule(10f, "Contains excessive digits in the domain", RuleCategory.DOMAIN),
            "non_standard_port" to expectedRule(10f, "The URL uses a non-standard network port", RuleCategory.URL_STRUCTURE)
        )
    }

    @Test
    fun `malformed ports remain deterministic without throwing`() {
        listOf("http://", "https://example.com:", "https://example.com:99999").forEach { url ->
            assertEquals(engine.analyze(url), engine.analyze(url))
        }
    }

    @Test
    fun `userinfo before the host is a high severity signal`() {
        assertLinkAnalysis(
            engine,
            "https://google.com@evil.example/login",
            32f,
            RiskLevel.YELLOW,
            "social_engineering" to expectedRule(2f, "Uses social engineering keyword in path/query: login", RuleCategory.SOCIAL_ENGINEERING),
            "userinfo_deception" to expectedRule(30f, "The URL contains deceptive user information before the actual host", RuleCategory.URL_STRUCTURE)
        )
        assertLinkAnalysis(
            engine,
            "https://user:pass@evil.example",
            30f,
            RiskLevel.YELLOW,
            "userinfo_deception" to expectedRule(30f, "The URL contains deceptive user information before the actual host", RuleCategory.URL_STRUCTURE)
        )
    }

    @Test
    fun `query and fragment at signs are not mistaken for userinfo`() {
        assertLinkAnalysis(engine, "https://example.com/path?email=user@example.com", 0f, RiskLevel.GREEN)
        assertLinkAnalysis(engine, "https://example.com/#contact=user@example.com", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `rules after userinfo analyze the actual destination host`() {
        assertLinkAnalysis(
            engine,
            "https://google.com@paypal-secure.example",
            80f,
            RiskLevel.RED,
            "brand_impersonation" to expectedRule(30f, "Possible paypal brand impersonation", RuleCategory.BRAND_IMPERSONATION),
            "social_engineering" to expectedRule(20f, "Uses social engineering keyword in domain: secure", RuleCategory.SOCIAL_ENGINEERING),
            "userinfo_deception" to expectedRule(30f, "The URL contains deceptive user information before the actual host", RuleCategory.URL_STRUCTURE)
        )
    }

    @Test
    fun `plain and encoded embedded URLs are detected without matching ordinary HTTP text`() {
        assertLinkAnalysis(
            engine,
            "https://example.com/?url=https://evil.example",
            45f,
            RiskLevel.YELLOW,
            "suspicious_redirect" to expectedRule(15f, "URL uses a redirect parameter pointing to another destination", RuleCategory.URL_STRUCTURE),
            "embedded_url" to expectedRule(30f, "Embedded external URL detected", RuleCategory.URL_STRUCTURE)
        )
        assertLinkAnalysis(
            engine,
            "https://example.com/?url=http%3A%2F%2Fevil.example",
            55f,
            RiskLevel.YELLOW,
            "encoded_chars" to expectedRule(10f, "URL contains many encoded characters", RuleCategory.URL_STRUCTURE),
            "suspicious_redirect" to expectedRule(15f, "URL uses a redirect parameter pointing to another destination", RuleCategory.URL_STRUCTURE),
            "embedded_url" to expectedRule(30f, "Embedded external URL detected", RuleCategory.URL_STRUCTURE)
        )
        assertLinkAnalysis(
            engine,
            "https://example.com/?ref=http%3A%2F%2Fevil.example",
            40f,
            RiskLevel.YELLOW,
            "encoded_chars" to expectedRule(10f, "URL contains many encoded characters", RuleCategory.URL_STRUCTURE),
            "embedded_url" to expectedRule(30f, "Embedded external URL detected", RuleCategory.URL_STRUCTURE)
        )
        assertLinkAnalysis(engine, "https://example.com/path/https://evil.example", 30f, RiskLevel.YELLOW,
            "embedded_url" to expectedRule(30f, "Embedded external URL detected", RuleCategory.URL_STRUCTURE))
        assertLinkAnalysis(engine, "https://example.com/?text=http", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `redirect names are case insensitive and require plausible external destinations`() {
        assertLinkAnalysis(
            engine,
            "https://example.com/?Redirect=https://evil.example",
            45f,
            RiskLevel.YELLOW,
            "suspicious_redirect" to expectedRule(15f, "URL uses a redirect parameter pointing to another destination", RuleCategory.URL_STRUCTURE),
            "embedded_url" to expectedRule(30f, "Embedded external URL detected", RuleCategory.URL_STRUCTURE)
        )
        assertLinkAnalysis(engine, "https://example.com/?next=%2Fdashboard", 0f, RiskLevel.GREEN)
        assertLinkAnalysis(engine, "https://example.com/?next=page2", 0f, RiskLevel.GREEN)
        assertLinkAnalysis(
            engine,
            "https://example.com/?continue=https%3A%2F%2Fevil.example",
            55f,
            RiskLevel.YELLOW,
            "encoded_chars" to expectedRule(10f, "URL contains many encoded characters", RuleCategory.URL_STRUCTURE),
            "suspicious_redirect" to expectedRule(15f, "URL uses a redirect parameter pointing to another destination", RuleCategory.URL_STRUCTURE),
            "embedded_url" to expectedRule(30f, "Embedded external URL detected", RuleCategory.URL_STRUCTURE)
        )
    }

    @Test
    fun `redirect and embedded evidence both contribute`() {
        val analysis = assertLinkAnalysis(
            engine,
            "https://example.com/?redirect=https://evil.example",
            45f,
            RiskLevel.YELLOW,
            "suspicious_redirect" to expectedRule(15f, "URL uses a redirect parameter pointing to another destination", RuleCategory.URL_STRUCTURE),
            "embedded_url" to expectedRule(30f, "Embedded external URL detected", RuleCategory.URL_STRUCTURE)
        )
        assertEquals(analysis.ruleResults.size, EXPECTED_LINK_RULE_IDS.size)
        assertEquals(analysis, engine.analyze("https://example.com/?redirect=https://evil.example"))
    }

    @Test
    fun `combined HTTP and non-standard port reaches warning risk`() {
        assertLinkAnalysis(
            engine,
            "http://example.com:8080",
            40f,
            RiskLevel.YELLOW,
            "insecure_http" to expectedRule(30f, "Uses insecure HTTP protocol", RuleCategory.URL_STRUCTURE),
            "non_standard_port" to expectedRule(10f, "The URL uses a non-standard network port", RuleCategory.URL_STRUCTURE)
        )
    }

    @Test
    fun `encoded redirect and embedded URL both contribute with the encoding signal`() {
        assertLinkAnalysis(
            engine,
            "https://example.com/?redirect=https%3A%2F%2Fevil.example",
            55f,
            RiskLevel.YELLOW,
            "encoded_chars" to expectedRule(10f, "URL contains many encoded characters", RuleCategory.URL_STRUCTURE),
            "suspicious_redirect" to expectedRule(15f, "URL uses a redirect parameter pointing to another destination", RuleCategory.URL_STRUCTURE),
            "embedded_url" to expectedRule(30f, "Embedded external URL detected", RuleCategory.URL_STRUCTURE)
        )
    }

    @Test
    fun `HTTP userinfo and redirect combine deterministically`() {
        assertLinkAnalysis(
            engine,
            "http://google.com@evil.example/?redirect=https://evil.example",
            60f,
            RiskLevel.YELLOW,
            "insecure_http" to expectedRule(30f, "Uses insecure HTTP protocol", RuleCategory.URL_STRUCTURE),
            "userinfo_deception" to expectedRule(30f, "The URL contains deceptive user information before the actual host", RuleCategory.URL_STRUCTURE)
        )
    }

    @Test
    fun `malformed URL userinfo and query encoding inputs remain deterministic`() {
        listOf("https://user@@example.com", "https://example.com/?redirect=%ZZ").forEach { url ->
            assertEquals(engine.analyze(url), engine.analyze(url))
        }
    }
}
