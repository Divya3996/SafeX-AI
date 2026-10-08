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
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [VirusTotalReputationProvider].
 */
class VirusTotalReputationProviderTest {

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

    private fun serverLookupUrl(): String = server.url("/").toString()

    private fun enabledConfig(apiKey: String = "secret-key-123") = ReputationConfig(
        openPhishFeedUrl = "",
        openPhishApiKey = "",
        virusTotalApiKey = apiKey,
        virusTotalLookupUrl = serverLookupUrl(),
        lookupTimeoutMs = 5000L
    )

    private fun onlineWrapper(): HttpClientWrapper = HttpClientWrapper(
        okHttpClient = OkHttpClient(),
        connectivityChecker = object : ConnectivityChecker {
            override fun isConnected() = true
        },
        config = NetworkConfig(maxRetries = 0),
        dispatcherProvider = dispatcherProvider
    )

    private fun offlineWrapper(): HttpClientWrapper = HttpClientWrapper(
        okHttpClient = OkHttpClient(),
        connectivityChecker = object : ConnectivityChecker {
            override fun isConnected() = false
        },
        config = NetworkConfig(maxRetries = 0),
        dispatcherProvider = dispatcherProvider
    )

    private fun timeoutWrapper(): HttpClientWrapper = HttpClientWrapper(
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(50, TimeUnit.MILLISECONDS)
            .readTimeout(50, TimeUnit.MILLISECONDS)
            .writeTimeout(50, TimeUnit.MILLISECONDS)
            .build(),
        connectivityChecker = object : ConnectivityChecker {
            override fun isConnected() = true
        },
        config = NetworkConfig(maxRetries = 0),
        dispatcherProvider = dispatcherProvider
    )

    private fun provider(
        httpClient: HttpClientWrapper,
        config: ReputationConfig = enabledConfig()
    ) = VirusTotalReputationProvider(httpClient, jsonParser, config)

    private fun enqueueResponse(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    @Test
    fun supportsOnlyUrlTargets() {
        val p = provider(offlineWrapper())
        assertTrue(p.supports(ReputationTarget.Url("https://example.com")))
    }

    @Test
    fun providerNameIsVirusTotal() {
        assertEquals("VirusTotal", provider(offlineWrapper()).providerName)
    }

    @Test
    fun returnsNullWhenNoApiKey() = runTest(testDispatcher) {
        val p = provider(onlineWrapper(), enabledConfig(apiKey = ""))
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))
        assertNull(result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun successfulMaliciousLookup() = runTest(testDispatcher) {
        enqueueResponse(200, """{"data":{"id":"u-123"}}""")
        enqueueResponse(200, """
            {
              "data": {
                "attributes": {
                  "status": "completed",
                  "stats": {
                    "malicious": 4,
                    "suspicious": 1,
                    "harmless": 65,
                    "undetected": 2
                  }
                }
              }
            }
        """.trimIndent())

        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))

        assertNotNull(result)
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
        assertEquals(0.95f, result?.confidence ?: 0f, 0.001f)
        assertEquals("VirusTotal", result?.providerName)
        assertTrue(result?.reason?.contains("malicious=4") == true)

        val postRequest = server.takeRequest()
        assertEquals("POST", postRequest.method)
        assertEquals("/urls", postRequest.path)
        assertEquals("secret-key-123", postRequest.getHeader("x-apikey"))
        assertEquals("application/x-www-form-urlencoded; charset=utf-8", postRequest.getHeader("Content-Type"))
        assertEquals("url=https%3A%2F%2Fexample.com", postRequest.body.readUtf8())

        val getRequest = server.takeRequest()
        assertEquals("GET", getRequest.method)
        assertEquals("/analyses/u-123", getRequest.path)
        assertEquals("secret-key-123", getRequest.getHeader("x-apikey"))
    }

    @Test
    fun successfulSuspiciousLookup() = runTest(testDispatcher) {
        enqueueResponse(200, """{"data":{"id":"u-123"}}""")
        enqueueResponse(200, """
            {
              "data": {
                "attributes": {
                  "status": "completed",
                  "stats": {
                    "malicious": 0,
                    "suspicious": 2,
                    "harmless": 65,
                    "undetected": 2
                  }
                }
              }
            }
        """.trimIndent())

        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))

        assertNotNull(result)
        assertEquals(ReputationVerdict.SUSPICIOUS, result?.reputation)
        assertEquals(0.75f, result?.confidence ?: 0f, 0.001f)
        assertTrue(result?.reason?.contains("suspicious=2") == true)
    }

    @Test
    fun unknownResult() = runTest(testDispatcher) {
        enqueueResponse(200, """{"data":{"id":"u-123"}}""")
        enqueueResponse(200, """
            {
              "data": {
                "attributes": {
                  "status": "completed",
                  "stats": {
                    "malicious": 0,
                    "suspicious": 0,
                    "harmless": 65,
                    "undetected": 2
                  }
                }
              }
            }
        """.trimIndent())

        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))

        assertNotNull(result)
        assertEquals(ReputationVerdict.UNKNOWN, result?.reputation)
        assertEquals(0.0f, result?.confidence ?: -1f, 0.001f)
        assertTrue(result?.reason?.contains("malicious=0") == true)
    }

    @Test
    fun returnsNullOnMalformedJsonInPost() = runTest(testDispatcher) {
        enqueueResponse(200, "{malformed}")
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))
        assertNull(result)
    }

    @Test
    fun returnsNullOnMalformedJsonInGet() = runTest(testDispatcher) {
        enqueueResponse(200, """{"data":{"id":"u-123"}}""")
        enqueueResponse(200, "{malformed}")
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))
        assertNull(result)
    }

    @Test
    fun returnsNullWhenNetworkUnavailable() = runTest(testDispatcher) {
        val p = provider(offlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))
        assertNull(result)
    }

    @Test
    fun returnsNullOnTimeout() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE))
        val p = provider(timeoutWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))
        assertNull(result)
    }

    @Test
    fun returnsNullOnHttpPostError() = runTest(testDispatcher) {
        enqueueResponse(500, "Internal Server Error")
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))
        assertNull(result)
    }

    @Test
    fun returnsNullOnHttpGetError() = runTest(testDispatcher) {
        enqueueResponse(200, """{"data":{"id":"u-123"}}""")
        enqueueResponse(500, "Internal Server Error")
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))
        assertNull(result)
    }

    @Test
    fun returnsNullOnMissingDataIdInPost() = runTest(testDispatcher) {
        enqueueResponse(200, """{"data":{}}""")
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))
        assertNull(result)
    }

    @Test
    fun returnsNullOnMissingStatsInGet() = runTest(testDispatcher) {
        enqueueResponse(200, """{"data":{"id":"u-123"}}""")
        enqueueResponse(200, """{"data":{"attributes":{"status":"completed"}}}""")
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))
        assertNull(result)
    }

    @Test
    fun returnsNullOnInvalidUrl() = runTest(testDispatcher) {
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("invalid-url-no-scheme"))
        assertNull(result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun returnsNullOnRateLimit() = runTest(testDispatcher) {
        enqueueResponse(429, "Too Many Requests")
        val p = provider(onlineWrapper())
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))
        assertNull(result)
    }

    @Test
    fun providerNeverThrowsExceptions() = runTest(testDispatcher) {
        val throwingOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { throw RuntimeException("Simulated network crash") }
            .build()

        val throwingWrapper = HttpClientWrapper(
            okHttpClient = throwingOkHttpClient,
            connectivityChecker = object : ConnectivityChecker {
                override fun isConnected() = true
            },
            config = NetworkConfig(maxRetries = 0),
            dispatcherProvider = dispatcherProvider
        )

        val p = provider(throwingWrapper)
        val result = p.evaluate(ReputationTarget.Url("https://example.com"))
        assertNull(result)
    }
}
