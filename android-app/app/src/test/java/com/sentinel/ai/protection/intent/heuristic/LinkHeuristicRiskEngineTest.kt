package com.sentinel.ai.protection.intent.heuristic

import com.sentinel.ai.core.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class LinkHeuristicRiskEngineTest {

    private val engine = LinkHeuristicRiskEngine()

    @Test
    fun `path keyword cannot hide a stronger host keyword`() {
        val result = engine.analyze("https://paypal-secure.example/verify?redirect=https://example.com")
        val hostReason = result.ruleResults.first { it.explanation?.contains("keyword in domain") == true }
        org.junit.Assert.assertEquals(20f, hostReason.scoreContribution, 0f)
        org.junit.Assert.assertEquals(95f, result.score, 0f)
    }

    @Test
    fun `default rule inventory and evaluation order remain stable`() {
        val rules = LinkHeuristicRiskEngine.defaultRules().toList()

        assertEquals(26, rules.size)
        assertEquals(EXPECTED_LINK_RULE_IDS, rules.map { it.id })
        assertEquals(26, rules.map { it.id }.toSet().size)
    }

    @Test
    fun `suspicious TLD contributes 15 points with stable reason`() {
        assertLinkAnalysis(
            engine,
            "https://example.xyz",
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
    fun `raw IPv4 host contributes 25 points without numeric host stacking`() {
        assertLinkAnalysis(
            engine,
            "https://1.2.3.4/",
            25f,
            RiskLevel.GREEN,
            "ip_address" to expectedRule(
                25f,
                "Uses IP address instead of domain",
                RuleCategory.DOMAIN
            )
        )
    }

    @Test
    fun `more than three host dots contributes excessive subdomain score`() {
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
    fun `high entropy hostname contributes random hostname score`() {
        assertLinkAnalysis(
            engine,
            "https://abcdefghijklpq.com",
            10f,
            RiskLevel.GREEN,
            "random_hostname" to expectedRule(
                10f,
                "Uses a random-looking hostname",
                RuleCategory.DOMAIN
            )
        )
    }

    @Test
    fun `double hyphen contributes repeated hyphen score`() {
        assertLinkAnalysis(
            engine,
            "https://alpha--beta.com",
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
    fun `numeric heavy hostname contributes excessive digit score`() {
        assertLinkAnalysis(
            engine,
            "https://abc12.com",
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
    fun `punycode hostname contributes 15 points in isolation`() {
        assertLinkAnalysis(
            engine,
            "https://xn--fiqs8s.com",
            15f,
            RiskLevel.GREEN,
            "punycode" to expectedRule(
                15f,
                "Punycode domain name detected",
                RuleCategory.DOMAIN
            )
        )
    }

    @Test
    fun `punycode hostname currently stacks repeated hyphen score when it has a third hyphen`() {
        assertLinkAnalysis(
            engine,
            "https://xn--bcher-kva.example",
            25f,
            RiskLevel.GREEN,
            "repeated_hyphens" to expectedRule(
                10f,
                "Contains repeated or multiple hyphens in domain",
                RuleCategory.DOMAIN
            ),
            "punycode" to expectedRule(
                15f,
                "Punycode domain name detected",
                RuleCategory.DOMAIN
            )
        )
    }

    @Test
    fun `raw URL longer than 150 characters contributes five points`() {
        val url = "https://example.com/#" + "a".repeat(130)
        assertEquals(151, url.length)

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
    }

    @Test
    fun `more than four path segments contributes deep path score`() {
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
    fun `last path segment longer than 30 characters contributes filename score`() {
        val url = "https://example.com/${"a".repeat(31)}"

        assertLinkAnalysis(
            engine,
            url,
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
    fun `more than five query parameters contributes ten points`() {
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
    fun `three percent encoded bytes contributes encoded character score`() {
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
    fun `tracking parameter triggers evidence but contributes zero points`() {
        assertLinkAnalysis(
            engine,
            "https://example.com/?utm_source=newsletter",
            0f,
            RiskLevel.GREEN,
            "tracking_parameters" to expectedRule(
                0f,
                "URL contains tracking parameters",
                RuleCategory.URL_STRUCTURE
            )
        )
    }

    @Test
    fun `unencoded redirect target and embedded URL contribute 45 points`() {
        assertLinkAnalysis(
            engine,
            "https://example.com/?next=https://destination.test",
            45f,
            RiskLevel.YELLOW,
            "suspicious_redirect" to expectedRule(
                15f,
                "URL uses a redirect parameter pointing to another destination",
                RuleCategory.URL_STRUCTURE
            ),
            "embedded_url" to expectedRule(
                30f,
                "Embedded external URL detected",
                RuleCategory.URL_STRUCTURE
            )
        )
    }

    @Test
    fun `digit brand look alike contributes 30 points`() {
        assertLinkAnalysis(
            engine,
            "https://paypa1.example",
            30f,
            RiskLevel.YELLOW,
            "brand_impersonation" to expectedRule(
                30f,
                "Possible paypal look-alike domain",
                RuleCategory.BRAND_IMPERSONATION
            )
        )
    }

    @Test
    fun `brand plus extra word contributes 30 points and currently stacks with keyword score`() {
        assertLinkAnalysis(
            engine,
            "https://paypal-secure.example",
            50f,
            RiskLevel.YELLOW,
            "brand_impersonation" to expectedRule(
                30f,
                "Possible paypal brand impersonation",
                RuleCategory.BRAND_IMPERSONATION
            ),
            "social_engineering" to expectedRule(
                20f,
                "Uses social engineering keyword in domain: secure",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }

    @Test
    fun `social engineering term in host contributes 20 points`() {
        assertLinkAnalysis(
            engine,
            "https://secure-example.com",
            20f,
            RiskLevel.GREEN,
            "social_engineering" to expectedRule(
                20f,
                "Uses social engineering keyword in domain: secure",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }

    @Test
    fun `social engineering term outside host contributes two points`() {
        assertLinkAnalysis(
            engine,
            "https://example.com/login",
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
    fun `two weak structural signals add deterministically`() {
        val url = "https://example.com/a/b/c/d/e#" + "a".repeat(121)
        assertLinkAnalysis(
            engine,
            url,
            10f,
            RiskLevel.GREEN,
            "excessive_length" to expectedRule(
                5f,
                "URL is unusually long",
                RuleCategory.URL_STRUCTURE
            ),
            "deep_nesting" to expectedRule(
                5f,
                "URL path is deeply nested",
                RuleCategory.URL_STRUCTURE
            )
        )
    }

    @Test
    fun `one strong and one weak signal reach yellow boundary`() {
        assertLinkAnalysis(
            engine,
            "https://paypa1.example/a/b/c/d/e",
            35f,
            RiskLevel.YELLOW,
            "deep_nesting" to expectedRule(
                5f,
                "URL path is deeply nested",
                RuleCategory.URL_STRUCTURE
            ),
            "brand_impersonation" to expectedRule(
                30f,
                "Possible paypal look-alike domain",
                RuleCategory.BRAND_IMPERSONATION
            )
        )
    }

    @Test
    fun `several medium domain signals combine to yellow`() {
        assertLinkAnalysis(
            engine,
            "https://a.a.a.a.alpha--beta.xyz",
            35f,
            RiskLevel.YELLOW,
            "suspicious_tld" to expectedRule(
                15f,
                "Uses .xyz domain",
                RuleCategory.DOMAIN
            ),
            "excessive_subdomains" to expectedRule(
                10f,
                "Contains excessive subdomains",
                RuleCategory.DOMAIN
            ),
            "repeated_hyphens" to expectedRule(
                10f,
                "Contains repeated or multiple hyphens in domain",
                RuleCategory.DOMAIN
            )
        )
    }

    @Test
    fun `several strong signals combine to red`() {
        assertLinkAnalysis(
            engine,
            "https://paypal-secure.xyz/?next=https://evil.test",
            100f,
            RiskLevel.CRITICAL,
            "suspicious_tld" to expectedRule(
                15f,
                "Uses .xyz domain",
                RuleCategory.DOMAIN
            ),
            "suspicious_redirect" to expectedRule(
                15f,
                "URL uses a redirect parameter pointing to another destination",
                RuleCategory.URL_STRUCTURE
            ),
            "brand_impersonation" to expectedRule(
                30f,
                "Possible paypal brand impersonation",
                RuleCategory.BRAND_IMPERSONATION
            ),
            "social_engineering" to expectedRule(
                20f,
                "Uses social engineering keyword in domain: secure",
                RuleCategory.SOCIAL_ENGINEERING
            ),
            "embedded_url" to expectedRule(
                30f,
                "Embedded external URL detected",
                RuleCategory.URL_STRUCTURE
            )
        )
    }

    @Test
    fun `maximum realistic combination caps score and keeps first four explanations`() {
        val url = buildString {
            append("https://a.b.c.d.xn--paypal-secure-123456.xyz/a/b/c/d/")
            append("f".repeat(31))
            append("?utm_source=x&a=1&b=2&c=3&d=4&next=https%3A%2F%2Fevil.test#")
            append("z".repeat(80))
        }

        val analysis = assertLinkAnalysis(
            engine,
            url,
            100f,
            RiskLevel.CRITICAL,
            "suspicious_tld" to expectedRule(15f, "Uses .xyz domain", RuleCategory.DOMAIN),
            "excessive_subdomains" to expectedRule(10f, "Contains excessive subdomains", RuleCategory.DOMAIN),
            "random_hostname" to expectedRule(10f, "Uses a random-looking hostname", RuleCategory.DOMAIN),
            "repeated_hyphens" to expectedRule(10f, "Contains repeated or multiple hyphens in domain", RuleCategory.DOMAIN),
            "excessive_digits" to expectedRule(10f, "Contains excessive digits in the domain", RuleCategory.DOMAIN),
            "punycode" to expectedRule(30f, "Punycode domain could not be decoded by the local IDN parser", RuleCategory.DOMAIN),
            "excessive_length" to expectedRule(5f, "URL is unusually long", RuleCategory.URL_STRUCTURE),
            "deep_nesting" to expectedRule(5f, "URL path is deeply nested", RuleCategory.URL_STRUCTURE),
            "long_filename" to expectedRule(5f, "URL contains an unusually long filename", RuleCategory.URL_STRUCTURE),
            "excessive_query" to expectedRule(10f, "URL has excessive query parameters", RuleCategory.URL_STRUCTURE),
            "encoded_chars" to expectedRule(10f, "URL contains many encoded characters", RuleCategory.URL_STRUCTURE),
            "tracking_parameters" to expectedRule(0f, "URL contains tracking parameters", RuleCategory.URL_STRUCTURE),
            "suspicious_redirect" to expectedRule(15f, "URL uses a redirect parameter pointing to another destination", RuleCategory.URL_STRUCTURE),
            "brand_impersonation" to expectedRule(30f, "Possible paypal brand impersonation", RuleCategory.BRAND_IMPERSONATION),
            "social_engineering" to expectedRule(20f, "Uses social engineering keyword in domain: secure", RuleCategory.SOCIAL_ENGINEERING),
            "embedded_url" to expectedRule(30f, "Embedded external URL detected", RuleCategory.URL_STRUCTURE)
        )

        assertEquals(215f, analysis.ruleResults.sumOf { it.scoreContribution.toDouble() }.toFloat(), 0f)
        assertEquals(
            "Detected 16 link risk signal(s): Uses .xyz domain; Contains excessive subdomains; " +
                "Uses a random-looking hostname; Contains repeated or multiple hyphens in domain.",
            analysis.explanation
        )
    }

    @Test
    fun `repeated login terms trigger social engineering only once`() {
        assertLinkAnalysis(
            engine,
            "https://example.com/login/login/login",
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
    fun `repeated shortener text warns once about an unknown destination`() {
        assertLinkAnalysis(
            engine,
            "https://bit.ly/bit.ly/bit.ly",
            30f,
            RiskLevel.YELLOW,
            "shortened_destination" to expectedRule(30f, "Short link hides its final destination; it is not automatically fraudulent", RuleCategory.URL_STRUCTURE)
        )
    }

    @Test
    fun `repeated verify terms in host contribute only once`() {
        assertLinkAnalysis(
            engine,
            "https://verify-verify-verify.example.com",
            20f,
            RiskLevel.GREEN,
            "social_engineering" to expectedRule(
                20f,
                "Uses social engineering keyword in domain: verify",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }

    @Test
    fun `repeated redirect parameters contribute only once`() {
        assertLinkAnalysis(
            engine,
            "https://example.com/?next=https://one.test&next=https://two.test",
            45f,
            RiskLevel.YELLOW,
            "suspicious_redirect" to expectedRule(
                15f,
                "URL uses a redirect parameter pointing to another destination",
                RuleCategory.URL_STRUCTURE
            ),
            "embedded_url" to expectedRule(
                30f,
                "Embedded external URL detected",
                RuleCategory.URL_STRUCTURE
            )
        )
    }

    @Test
    fun `analysis is deterministic for safe malformed combined and capped inputs`() {
        val urls = listOf(
            "https://example.com",
            "not a url",
            "https://paypal-secure.xyz/?next=https://evil.test",
            buildString {
                append("https://a.b.c.d.xn--paypal-secure-123456.xyz/a/b/c/d/")
                append("f".repeat(31))
                append("?utm_source=x&a=1&b=2&c=3&d=4&next=https%3A%2F%2Fevil.test#")
                append("z".repeat(80))
            }
        )

        urls.forEach { url ->
            val first = engine.analyze(url)
            repeat(10) {
                assertEquals("analysis changed for $url", first, engine.analyze(url))
            }
        }
    }
}
