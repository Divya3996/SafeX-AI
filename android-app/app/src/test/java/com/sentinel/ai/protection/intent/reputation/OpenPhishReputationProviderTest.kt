package com.sentinel.ai.protection.intent.reputation

import com.google.gson.Gson
import com.sentinel.ai.core.coroutines.DispatcherProvider
import com.sentinel.ai.core.network.ConnectivityChecker
import com.sentinel.ai.core.network.HttpClientWrapper
import com.sentinel.ai.core.network.JsonParser
import com.sentinel.ai.core.network.NetworkConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [OpenPhishReputationProvider].
 *
 * ## Strategy
 * Each test spins up a [MockWebServer] to serve a controlled HTTP response.
 * The provider is wired to a real [HttpClientWrapper] that points at the local
 * server URL, so all provider branching (feed parsing, matching, error handling)
 * runs against a real HTTP stack with no network I/O beyond localhost.
 *
 * The offline path is covered by a separate [ConnectivityChecker] stub that
 * returns `false`, which short-circuits [HttpClientWrapper.execute] before any
 * socket is opened.
 *
 * ## URL matching semantics (normalization contract)
 * Both feed entry and target URL are normalized before comparison:
 *   1. Strip `https://`, `http://`, or `//` scheme prefix.
 *   2. Strip a leading `www.` host label.
 *   3. Strip a single trailing `/`.
 *   4. Lower-case.
 *
 * Matching rules (post-normalization):
 *   - **Exact**: normalized entry == normalized target -> MALICIOUS
 *   - **Prefix** (domain-only or path entry): target starts with entry AND next
 *     character is `/` or end-of-string -> MALICIOUS
 *
 * False-positive prevention: `evil.com` must NOT match `notevil.com` or
 * `evil.com.attacker.net`.
 */
class OpenPhishReputationProviderTest {

    // -------------------------------------------------------------------------
    // Test infrastructure
    // -------------------------------------------------------------------------

    private lateinit var server: MockWebServer
    private val testDispatcher = StandardTestDispatcher()

    private val dispatcherProvider = object : DispatcherProvider() {
        override val io: CoroutineDispatcher = testDispatcher
        override val main: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    private val jsonParser = JsonParser(Gson())

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /** [HttpClientWrapper] connected to the [MockWebServer]; connectivity returns true. */
    private fun onlineWrapper(): HttpClientWrapper = HttpClientWrapper(
        okHttpClient = OkHttpClient(),
        connectivityChecker = object : ConnectivityChecker {
            override fun isConnected() = true
        },
        config = NetworkConfig(),
        dispatcherProvider = dispatcherProvider
    )

    /** [HttpClientWrapper] configured with an offline stub; no socket is ever opened. */
    private fun offlineWrapper(): HttpClientWrapper = HttpClientWrapper(
        okHttpClient = OkHttpClient(),
        connectivityChecker = object : ConnectivityChecker {
            override fun isConnected() = false
        },
        config = NetworkConfig(),
        dispatcherProvider = dispatcherProvider
    )

    /** Enqueue a 200 OK response with the given body on the MockWebServer. */
    private fun enqueueBody(body: String) {
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
    }

    /** Enqueue a non-2xx error response on the MockWebServer. */
    private fun enqueueError(code: Int, message: String = "Error") {
        server.enqueue(MockResponse().setResponseCode(code).setBody(message))
    }

    /** Feed URL pointing at the local MockWebServer. */
    private fun serverFeedUrl(): String = server.url("/feed.txt").toString()

    /** Enabled config – feed URL points at local MockWebServer, no API key. */
    private fun enabledConfig(apiKey: String = "") = ReputationConfig(
        openPhishFeedUrl = serverFeedUrl(),
        openPhishApiKey = apiKey,
        lookupTimeoutMs = 5000L
    )

    /** Disabled config – blank feed URL so [ReputationConfig.isOpenPhishEnabled] is false. */
    private fun disabledConfig() = ReputationConfig(
        openPhishFeedUrl = "",
        openPhishApiKey = "",
        lookupTimeoutMs = 5000L
    )

    private fun provider(
        httpClient: HttpClientWrapper,
        config: ReputationConfig = enabledConfig()
    ) = OpenPhishReputationProvider(httpClient, jsonParser, config)

    /** Joins [entries] with newlines to produce a realistic feed body. */
    private fun feed(vararg entries: String): String = entries.joinToString("\n")

    // -------------------------------------------------------------------------
    // supports()
    // -------------------------------------------------------------------------

    @Test
    fun supportsOnlyUrlTargets() {
        val p = provider(offlineWrapper())
        assertTrue(p.supports(ReputationTarget.Url("https://example.com")))
    }

    // -------------------------------------------------------------------------
    // providerName
    // -------------------------------------------------------------------------

    @Test
    fun providerNameIsOpenPhish() {
        assertEquals("OpenPhish", provider(offlineWrapper()).providerName)
    }

    // -------------------------------------------------------------------------
    // Provider disabled
    // -------------------------------------------------------------------------

    @Test
    fun returnsNullWhenProviderIsDisabled() = runTest(testDispatcher) {
        val p = provider(offlineWrapper(), disabledConfig())
        assertNull(p.evaluate(ReputationTarget.Url("https://phish.example.com/login")))
    }

    // -------------------------------------------------------------------------
    // Network failure paths
    // -------------------------------------------------------------------------

    @Test
    fun returnsNullWhenNetworkIsUnavailable() = runTest(testDispatcher) {
        val p = provider(offlineWrapper())
        assertNull(p.evaluate(ReputationTarget.Url("https://phish.example.com/login")))
    }

    @Test
    fun returnsNullOnHttpError() = runTest(testDispatcher) {
        enqueueError(503)
        val p = provider(onlineWrapper())
        assertNull(p.evaluate(ReputationTarget.Url("https://phish.example.com/login")))
    }

    @Test
    fun returnsNullOnHttp404() = runTest(testDispatcher) {
        enqueueError(404, "Not Found")
        val p = provider(onlineWrapper())
        assertNull(p.evaluate(ReputationTarget.Url("https://phish.example.com/login")))
    }

    // -------------------------------------------------------------------------
    // Empty vs malformed response body - treated differently
    // -------------------------------------------------------------------------

    @Test
    fun returnsNullOnEmptyResponseBody() = runTest(testDispatcher) {
        // Empty body: provider cannot evaluate; must not silently return UNKNOWN
        enqueueBody("")
        val p = provider(onlineWrapper())
        assertNull(
            "Empty body must return null, not UNKNOWN",
            p.evaluate(ReputationTarget.Url("https://phish.example.com"))
        )
    }

    @Test
    fun returnsNullOnHtmlDoctypeResponse() = runTest(testDispatcher) {
        enqueueBody("<!DOCTYPE html><html><body>Error</body></html>")
        val p = provider(onlineWrapper())
        assertNull(
            "HTML DOCTYPE response is malformed - must return null",
            p.evaluate(ReputationTarget.Url("https://phish.example.com"))
        )
    }

    @Test
    fun returnsNullOnHtmlTagResponseWithoutDoctype() = runTest(testDispatcher) {
        enqueueBody("<html><body><p>Login</p></body></html>")
        val p = provider(onlineWrapper())
        assertNull(
            "HTML response without DOCTYPE is also malformed - must return null",
            p.evaluate(ReputationTarget.Url("https://phish.example.com"))
        )
    }

    // -------------------------------------------------------------------------
    // No-match -> UNKNOWN
    // -------------------------------------------------------------------------

    @Test
    fun returnsUnknownVerdictWhenUrlNotInFeed() = runTest(testDispatcher) {
        enqueueBody(feed(
            "# OpenPhish Community Feed",
            "https://phish.example.com/steal",
            "http://otherbad.net/phishing"
        ))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://clean.example.com/home"))

        assertNotNull(result)
        assertEquals(ReputationVerdict.UNKNOWN, result?.reputation)
        assertEquals(0.0f, result?.confidence ?: -1f, 0.001f)
        assertEquals("OpenPhish", result?.providerName)
    }

    // -------------------------------------------------------------------------
    // Exact match -> MALICIOUS
    // -------------------------------------------------------------------------

    @Test
    fun returnsMaliciousVerdictOnExactMatch() = runTest(testDispatcher) {
        val target = "https://phish.example.com/steal-creds"
        enqueueBody(feed(target))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url(target))

        assertNotNull(result)
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
        assertEquals(0.98f, result?.confidence ?: 0f, 0.001f)
    }

    // -------------------------------------------------------------------------
    // Scheme normalization
    // -------------------------------------------------------------------------

    @Test
    fun matchesHttpFeedEntryAgainstHttpsTarget() = runTest(testDispatcher) {
        enqueueBody(feed("http://phish.example.com/steal"))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://phish.example.com/steal"))
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
    }

    @Test
    fun matchesHttpsTargetAgainstHttpFeedEntry() = runTest(testDispatcher) {
        enqueueBody(feed("https://phish.example.com/steal"))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("http://phish.example.com/steal"))
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
    }

    // -------------------------------------------------------------------------
    // www. normalization
    // -------------------------------------------------------------------------

    @Test
    fun matchesWwwFeedEntryAgainstNonWwwTarget() = runTest(testDispatcher) {
        enqueueBody(feed("https://www.phish.example.com/login"))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://phish.example.com/login"))
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
    }

    @Test
    fun matchesNonWwwFeedEntryAgainstWwwTarget() = runTest(testDispatcher) {
        enqueueBody(feed("https://phish.example.com/login"))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://www.phish.example.com/login"))
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
    }

    // -------------------------------------------------------------------------
    // Trailing slash normalization
    // -------------------------------------------------------------------------

    @Test
    fun matchesFeedEntryWithTrailingSlashAgainstTargetWithout() = runTest(testDispatcher) {
        enqueueBody(feed("https://phish.example.com/page/"))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://phish.example.com/page"))
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
    }

    @Test
    fun matchesFeedEntryWithoutTrailingSlashAgainstTargetWith() = runTest(testDispatcher) {
        enqueueBody(feed("https://phish.example.com/page"))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://phish.example.com/page/"))
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
    }

    // -------------------------------------------------------------------------
    // Path-prefix matching
    // -------------------------------------------------------------------------

    @Test
    fun matchesTargetWithAdditionalPathSegmentBeyondFeedEntry() = runTest(testDispatcher) {
        // Feed: phish.example.com/phishing -> should match /phishing/step2
        enqueueBody(feed("https://phish.example.com/phishing"))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://phish.example.com/phishing/step2"))
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
    }

    // -------------------------------------------------------------------------
    // Domain-only feed entry
    // -------------------------------------------------------------------------

    @Test
    fun domainOnlyFeedEntryMatchesTargetWithPath() = runTest(testDispatcher) {
        enqueueBody(feed("phish.example.com"))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://phish.example.com/login?user=x"))
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
    }

    @Test
    fun domainOnlyFeedEntryMatchesBareTarget() = runTest(testDispatcher) {
        enqueueBody(feed("phish.example.com"))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://phish.example.com"))
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
    }

    // -------------------------------------------------------------------------
    // False-positive prevention
    // -------------------------------------------------------------------------

    @Test
    fun domainOnlyFeedEntryDoesNotMatchSuffixedDomain() = runTest(testDispatcher) {
        // evil.com must NOT match notevil.com
        enqueueBody(feed("evil.com"))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://notevil.com/page"))
        assertEquals(
            "evil.com must not match notevil.com",
            ReputationVerdict.UNKNOWN,
            result?.reputation
        )
    }

    @Test
    fun domainOnlyFeedEntryDoesNotMatchSubdomainOfDifferentRoot() = runTest(testDispatcher) {
        // evil.com must NOT match evil.com.attacker.net
        enqueueBody(feed("evil.com"))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://evil.com.attacker.net/page"))
        assertEquals(
            "evil.com must not match evil.com.attacker.net",
            ReputationVerdict.UNKNOWN,
            result?.reputation
        )
    }

    @Test
    fun pathPrefixEntryDoesNotMatchSiblingPathComponent() = runTest(testDispatcher) {
        // Feed: phish.example.com/phishing
        // Target: phish.example.com/phishingbutdifferent <- sibling, NOT a child
        enqueueBody(feed("https://phish.example.com/phishing"))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://phish.example.com/phishingbutdifferent"))
        assertEquals(
            "Prefix must not bleed into sibling path components",
            ReputationVerdict.UNKNOWN,
            result?.reputation
        )
    }

    // -------------------------------------------------------------------------
    // Feed comment lines
    // -------------------------------------------------------------------------

    @Test
    fun commentLinesInFeedAreSkipped() = runTest(testDispatcher) {
        // Each evaluate() call consumes one MockWebServer response
        enqueueBody(feed(
            "# This is a comment",
            "# Another comment",
            "https://phish.example.com/real-entry"
        ))
        val p = provider(onlineWrapper())
        // Comment content should not accidentally match any URL
        val commentResult = p.evaluate(ReputationTarget.Url("https://example.com"))
        assertEquals(ReputationVerdict.UNKNOWN, commentResult?.reputation)

        // Enqueue again for second evaluate call
        enqueueBody(feed(
            "# This is a comment",
            "https://phish.example.com/real-entry"
        ))
        val realResult = p.evaluate(ReputationTarget.Url("https://phish.example.com/real-entry"))
        assertEquals(ReputationVerdict.MALICIOUS, realResult?.reputation)
    }

    // -------------------------------------------------------------------------
    // Blank/whitespace lines in feed
    // -------------------------------------------------------------------------

    @Test
    fun blankLinesInFeedAreIgnored() = runTest(testDispatcher) {
        enqueueBody("https://phish.example.com/steal\n\n   \nhttps://other.bad/page")
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://phish.example.com/steal"))
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
    }

    // -------------------------------------------------------------------------
    // API key configuration
    // -------------------------------------------------------------------------

    @Test
    fun providerWorksCorrectlyWithNoApiKey() = runTest(testDispatcher) {
        enqueueBody(feed("https://phish.example.com/steal"))
        val p = provider(onlineWrapper(), enabledConfig(apiKey = ""))
        assertEquals(
            ReputationVerdict.MALICIOUS,
            p.evaluate(ReputationTarget.Url("https://phish.example.com/steal"))?.reputation
        )
    }

    @Test
    fun providerWorksCorrectlyWithApiKeyConfigured() = runTest(testDispatcher) {
        enqueueBody(feed("https://phish.example.com/steal"))
        val p = provider(onlineWrapper(), enabledConfig(apiKey = "secret-key-123"))
        assertEquals(
            ReputationVerdict.MALICIOUS,
            p.evaluate(ReputationTarget.Url("https://phish.example.com/steal"))?.reputation
        )
    }

    // -------------------------------------------------------------------------
    // Result metadata integrity
    // -------------------------------------------------------------------------

    @Test
    fun maliciousResultHasCorrectMetadata() = runTest(testDispatcher) {
        val target = "https://phish.example.com/credentials"
        enqueueBody(feed(target))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url(target))

        assertNotNull(result)
        assertEquals("OpenPhish", result?.providerName)
        assertEquals(0.98f, result?.confidence ?: 0f, 0.001f)
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
        assertTrue(
            "Reason must mention OpenPhish feed",
            result?.reason?.contains("OpenPhish") == true
        )
    }

    @Test
    fun unknownResultHasZeroConfidenceAndCorrectMetadata() = runTest(testDispatcher) {
        enqueueBody(feed("https://phish.example.com/steal"))
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://clean.example.com"))

        assertNotNull(result)
        assertEquals("OpenPhish", result?.providerName)
        assertEquals(0.0f, result?.confidence ?: -1f, 0.001f)
        assertEquals(ReputationVerdict.UNKNOWN, result?.reputation)
    }
}
