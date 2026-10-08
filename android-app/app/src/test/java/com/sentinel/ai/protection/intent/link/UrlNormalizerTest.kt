package com.sentinel.ai.protection.intent.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlNormalizerTest {

    @Test
    fun `parses a basic HTTPS URL`() {
        val parsed = UrlNormalizer.parse("https://example.com")

        assertEquals("https://example.com", parsed.original)
        assertEquals("https://example.com", parsed.normalized)
        assertEquals("https", parsed.scheme)
        assertEquals("example.com", parsed.host)
        assertEquals("example.com", parsed.originalHost)
        assertNull(parsed.port)
        assertFalse(parsed.hasExplicitPort)
        assertFalse(parsed.hasUserInfo)
        assertEquals("", parsed.path)
        assertNull(parsed.query)
        assertNull(parsed.fragment)
        assertTrue(parsed.isValid)
        assertFalse(parsed.schemeWasInferred)
    }

    @Test
    fun `normalizes scheme and host casing while preserving path case`() {
        val parsed = UrlNormalizer.parse("HTTPS://EXAMPLE.COM/Login")

        assertEquals("https://example.com/Login", parsed.normalized)
        assertEquals("https", parsed.scheme)
        assertEquals("example.com", parsed.host)
        assertEquals("EXAMPLE.COM", parsed.originalHost)
        assertEquals("/Login", parsed.path)
        assertTrue(parsed.isValid)
    }

    @Test
    fun `parses explicit port path query and fragment`() {
        val parsed = UrlNormalizer.parse("http://example.com:8080/path?x=1#section")

        assertEquals("http", parsed.scheme)
        assertEquals("example.com", parsed.host)
        assertEquals(8080, parsed.port)
        assertTrue(parsed.hasExplicitPort)
        assertEquals("/path", parsed.path)
        assertEquals("x=1", parsed.query)
        assertEquals("section", parsed.fragment)
        assertEquals(mapOf("x" to listOf("1")), parsed.decodedQueryParameters)
        assertTrue(parsed.isValid)
    }

    @Test
    fun `records userinfo presence without exposing credentials in diagnostics`() {
        val parsed = UrlNormalizer.parse("https://user:pass@example.com/path?token=secret")

        assertTrue(parsed.hasUserInfo)
        assertEquals("example.com", parsed.host)
        assertTrue(parsed.isValid)
        assertFalse(parsed.toString().contains("user:pass"))
        assertFalse(parsed.toString().contains("secret"))
    }

    @Test
    fun `classifies IPv4 hosts`() {
        val parsed = UrlNormalizer.parse("https://192.168.1.1/path")

        assertEquals("192.168.1.1", parsed.host)
        assertTrue(parsed.isIpv4)
        assertFalse(parsed.isIpv6)
        assertFalse(parsed.isPunycode)
        assertTrue(parsed.isValid)
    }

    @Test
    fun `parses bracketed IPv6 and explicit port`() {
        val parsed = UrlNormalizer.parse("https://[2001:db8::1]:8443/path")

        assertEquals("[2001:db8::1]", parsed.host)
        assertEquals(8443, parsed.port)
        assertTrue(parsed.hasExplicitPort)
        assertTrue(parsed.isIpv6)
        assertFalse(parsed.isIpv4)
        assertEquals("/path", parsed.path)
        assertTrue(parsed.isValid)
    }

    @Test
    fun `classifies punycode hosts`() {
        val parsed = UrlNormalizer.parse("https://xn--pple-43d.com")

        assertEquals("xn--pple-43d.com", parsed.host)
        assertTrue(parsed.isPunycode)
        assertFalse(parsed.isIpv4)
        assertFalse(parsed.isIpv6)
        assertTrue(parsed.isValid)
    }

    @Test
    fun `infers HTTPS for a bare domain using the existing policy`() {
        val parsed = UrlNormalizer.parse("example.com")

        assertEquals("https://example.com", parsed.normalized)
        assertEquals("https", parsed.scheme)
        assertEquals("example.com", parsed.host)
        assertTrue(parsed.schemeWasInferred)
        assertTrue(parsed.isValid)
    }

    @Test
    fun `infers HTTPS for a bare www domain with path`() {
        val parsed = UrlNormalizer.parse("www.example.com/path")

        assertEquals("https://www.example.com/path", parsed.normalized)
        assertEquals("www.example.com", parsed.host)
        assertEquals("/path", parsed.path)
        assertTrue(parsed.schemeWasInferred)
        assertTrue(parsed.isValid)
    }

    @Test
    fun `decodes an encoded query value once`() {
        val parsed = UrlNormalizer.parse(
            "https://example.com/?next=https%3A%2F%2Fevil.example"
        )
        val parameter = parsed.queryParameters.single()

        assertEquals("next", parameter.decodedName)
        assertEquals("https://evil.example", parameter.decodedValue)
        assertEquals("https://evil.example", parameter.twiceDecodedValue)
        assertEquals(
            mapOf("next" to listOf("https://evil.example")),
            parsed.decodedQueryParameters
        )
        assertTrue(parameter.isPlausibleHttpDestination)
        assertTrue(parameter.containsEmbeddedHttpUrl)
    }

    @Test
    fun `decodes a double-encoded query value at most twice`() {
        val parsed = UrlNormalizer.parse(
            "https://example.com/?next=https%253A%252F%252Fevil.example"
        )
        val parameter = parsed.queryParameters.single()

        assertEquals("https%3A%2F%2Fevil.example", parameter.decodedValue)
        assertEquals("https://evil.example", parameter.twiceDecodedValue)
        assertTrue(parameter.isPlausibleHttpDestination)

        val tripleEncoded = UrlNormalizer.parse(
            "https://example.com/?next=https%25253A%25252F%25252Fevil.example"
        ).queryParameters.single()
        assertEquals("https%3A%2F%2Fevil.example", tripleEncoded.twiceDecodedValue)
        assertFalse(tripleEncoded.isPlausibleHttpDestination)
    }

    @Test
    fun `preserves repeated query parameter values and order`() {
        val parsed = UrlNormalizer.parse("https://example.com/?url=a&url=b")

        assertEquals(2, parsed.rawQueryParameterCount)
        assertEquals(2, parsed.queryParameters.size)
        assertEquals(listOf("a", "b"), parsed.decodedQueryParameters["url"])
        assertEquals(listOf("a", "b"), parsed.queryParameters.map { it.decodedValue })
    }

    @Test
    fun `trims surrounding whitespace during normalization`() {
        val parsed = UrlNormalizer.parse("  HTTPS://EXAMPLE.COM/Login  ")

        assertEquals("  HTTPS://EXAMPLE.COM/Login  ", parsed.original)
        assertEquals("https://example.com/Login", parsed.normalized)
        assertEquals("example.com", parsed.host)
        assertTrue(parsed.isValid)
    }

    @Test
    fun `preserves trailing slash and encoded query semantics`() {
        assertEquals("https://example.com", UrlNormalizer.normalize("HTTPS://EXAMPLE.COM"))
        assertEquals("https://example.com/", UrlNormalizer.normalize("HTTPS://EXAMPLE.COM/"))
        assertEquals(
            "https://example.com/Path?Token=A+B%2fC",
            UrlNormalizer.normalize("HTTPS://EXAMPLE.COM/Path?Token=A+B%2fC")
        )
    }

    @Test
    fun `malformed inputs never throw and return deterministic invalid models`() {
        val malformedInputs = listOf(
            "",
            " ",
            "not a url",
            "http://",
            "https://",
            "://broken",
            "https://example.com:",
            "https://example.com:99999",
            "https://user@@example.com",
            "https://example.com/?redirect=%ZZ"
        )

        malformedInputs.forEach { input ->
            val first = UrlNormalizer.parse(input)
            val second = UrlNormalizer.parse(input)
            assertFalse("Expected invalid model for $input", first.isValid)
            assertEquals("Expected stable model for $input", first, second)
        }
    }

    @Test
    fun `malformed ports retain safe partial information`() {
        val emptyPort = UrlNormalizer.parse("https://example.com:")
        assertEquals("example.com", emptyPort.host)
        assertTrue(emptyPort.hasExplicitPort)
        assertNull(emptyPort.port)
        assertFalse(emptyPort.isValid)

        val oversizedPort = UrlNormalizer.parse("https://example.com:99999")
        assertEquals("example.com", oversizedPort.host)
        assertTrue(oversizedPort.hasExplicitPort)
        assertEquals(99999, oversizedPort.port)
        assertFalse(oversizedPort.isValid)
    }

    @Test
    fun `normalization is idempotent and parsing is stable`() {
        val inputs = listOf(
            "https://example.com",
            " HTTPS://EXAMPLE.COM/Login ",
            "example.com/path?x=1",
            "https://[2001:db8::1]:8443/path",
            "https://example.com/?url=a&url=b",
            "https://example.com/?redirect=%ZZ"
        )

        inputs.forEach { input ->
            val normalized = UrlNormalizer.normalize(input)
            assertEquals(normalized, UrlNormalizer.normalize(normalized))
            assertEquals(UrlNormalizer.parse(input), UrlNormalizer.parse(input))
        }
    }
}
