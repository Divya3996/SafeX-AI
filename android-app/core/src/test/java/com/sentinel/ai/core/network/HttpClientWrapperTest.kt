package com.sentinel.ai.core.network

import com.google.gson.Gson
import com.sentinel.ai.core.coroutines.DispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HttpClientWrapperTest {

    private lateinit var gson: Gson
    private lateinit var jsonParser: JsonParser
    private lateinit var testDispatcher: DispatcherProvider

    @Before
    fun setUp() {
        gson = Gson()
        jsonParser = JsonParser(gson)

        val dispatcher = StandardTestDispatcher()
        testDispatcher = object : DispatcherProvider() {
            override val io = dispatcher
            override val main = dispatcher
            override val default = dispatcher
        }
    }

    @Test
    fun jsonParserDeserializesValidJson() {
        val json = """{"name":"Sentinel","version":1}"""
        val result = jsonParser.parse<TestModel>(json)
        assertNotNull(result)
        assertEquals("Sentinel", result?.name)
        assertEquals(1, result?.version)
    }

    @Test
    fun jsonParserReturnsNullForMalformedJson() {
        val json = """{"name":"Sentinel", malformed}"""
        val result = jsonParser.parse<TestModel>(json)
        assertNull(result)
    }

    @Test
    fun jsonParserReturnsNullForEmptyJson() {
        val result = jsonParser.parse<TestModel>("")
        assertNull(result)
    }

    @Test
    fun httpClientWrapperReturnsUnavailableWhenOffline() = runTest {
        // Given an offline checker
        val offlineChecker = object : ConnectivityChecker {
            override fun isConnected(): Boolean = false
        }

        val wrapper = HttpClientWrapper(
            okHttpClient = OkHttpClient(),
            connectivityChecker = offlineChecker,
            config = NetworkConfig(),
            dispatcherProvider = testDispatcher
        )

        val request = NetworkRequest(
            url = "https://api.sentinel.ai/check",
            method = HttpMethod.GET
        )

        // When
        val response = wrapper.execute(request)

        // Then
        assertTrue(response is NetworkResponse.NetworkUnavailable)
    }

    @Test
    fun successResponseIncludesMetadata() {
        val headers = mapOf("X-RateLimit-Limit" to listOf("100"))
        val success = NetworkResponse.Success(
            data = "body",
            statusCode = 200,
            headers = headers
        )

        assertEquals("body", success.data)
        assertEquals(200, success.statusCode)
        assertEquals(headers, success.headers)
    }

    @Test
    fun httpErrorResponsePreservesCodeAndMessage() {
        val error = NetworkResponse.HttpError(404, "Not Found")
        assertEquals(404, error.code)
        assertEquals("Not Found", error.message)
    }

    @Test
    fun ioFailurePreservesCause() {
        val exception = IOException("Connection lost")
        val failure = NetworkResponse.IoFailure(exception)
        assertEquals(exception, failure.cause)
    }

    private data class TestModel(
        val name: String,
        val version: Int
    )
}
