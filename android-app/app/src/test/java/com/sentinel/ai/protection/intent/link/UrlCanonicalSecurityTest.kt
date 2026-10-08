package com.sentinel.ai.protection.intent.link

import org.junit.Assert.*
import org.junit.Test

class UrlCanonicalSecurityTest {
    @Test fun sameSiteWrappersCannotConcealInnerCrossSiteDestinations() {
        listOf("https://example.com/?next=https://example.com/redirect?next=https://attacker.example/login",
            "https://example.com/?next=https://example.com/redirect?next=//attacker.example/login",
            "https://example.com/?next=//example.com/redirect?next=//attacker.example/login").forEach {
            assertTrue(it, LinkDestinations.external(UrlNormalizer.parse(it)).any { destination -> destination.host == "attacker.example" })
            assertTrue(it, com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRiskEngine().analyze(it).score >= 30f)
        }
    }
    @Test fun numericBrowserFormsHaveOneDestination() {
        listOf("2130706433", "0x7f000001", "0177.0.0.1", "127.1").forEach { raw ->
            val parsed = UrlNormalizer.parse("https://$raw/login")
            assertTrue(parsed.isValid); assertTrue(parsed.isIpv4); assertTrue(parsed.isObfuscatedNumericHost)
            assertEquals("127.0.0.1", parsed.host); assertEquals("https://127.0.0.1/login", parsed.normalized)
            assertEquals(parsed.normalized, UrlNormalizer.normalize(parsed.normalized))
        }
    }
    @Test fun impossibleNumericAddressesAreRejected() {
        listOf("https://4294967296/", "https://999.1.2.3/", "https://08.0.0.1/").forEach {
            assertFalse(it, UrlNormalizer.parse(it).isValid)
        }
    }
    @Test fun invisibleAndDirectionalControlsAreRejected() {
        listOf("https://exam\u200bple.com/", "https://example.com/\u202eexe.pdf", "https://example.com\\@evil.example/").forEach {
            assertFalse(UrlNormalizer.parse(it).isValid)
        }
    }
    @Test fun legitimateUnicodeAndPathCaseArePreserved() {
        val parsed = UrlNormalizer.parse("https://bücher.example/Path+A?Token=A+B")
        assertTrue(parsed.isValid); assertTrue(parsed.isPunycode)
        assertTrue(parsed.normalized.endsWith("/Path+A?Token=A+B"))
        assertEquals("paypal.com", UrlNormalizer.parse("https://paypal.com./").host)
    }
    @Test fun repeatedSameSiteParametersCannotHideALaterExternalDestination() {
        val benign = (1..40).joinToString("&") { "p$it=https://example.com/dashboard" }
        val parsed = UrlNormalizer.parse("https://example.com/?$benign&next=https://attacker.example/login")
        assertTrue(LinkDestinations.external(parsed).any { it.host == "attacker.example" })
        assertTrue(com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRiskEngine().analyze(parsed.original).score >= 30f)
    }
    @Test fun emptyAndSignedNumericLabelsAreNotAddresses() {
        listOf("https://+1/", "https://1..2/", "https://:/").forEach { assertFalse(it, UrlNormalizer.parse(it).isValid) }
    }
    @Test fun privateSuffixTenantsAreDifferentSites() {
        assertFalse(LinkDestinations.sameSite("tenant-a.github.io", "tenant-b.github.io"))
        assertTrue(LinkDestinations.sameSite("www.paypal.com", "login.paypal.com"))
    }
}
