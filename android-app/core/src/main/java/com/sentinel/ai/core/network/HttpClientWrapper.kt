package com.sentinel.ai.core.network

import com.sentinel.ai.core.coroutines.DispatcherProvider
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

/**
 * Coroutine-friendly HTTP execution layer shared by all reputation providers.
 *
 * ## Single entry point
 * All outgoing HTTP calls go through [execute]. Providers never interact with
 * OkHttp directly.
 *
 * ## Error handling contract
 * | Scenario                        | Outcome                           | Retried?                   |
 * |---------------------------------|-----------------------------------|----------------------------|
 * | [CancellationException]         | **Re-thrown immediately**         | Never — coroutine contract |
 * | [SocketTimeoutException]        | [NetworkResponse.Timeout]         | Yes, up to [NetworkConfig.maxRetries] |
 * | Other [IOException]             | [NetworkResponse.IoFailure]       | Yes, up to [NetworkConfig.maxRetries] |
 * | HTTP 4xx                        | [NetworkResponse.HttpError]       | Never — client error       |
 * | HTTP 5xx                        | [NetworkResponse.HttpError]       | Yes, up to [NetworkConfig.maxRetries] |
 * | No connectivity                 | [NetworkResponse.NetworkUnavailable] | Never — checked upfront |
 * | HTTP 2xx                        | [NetworkResponse.Success]         | —                          |
 *
 * ## Offline-first
 * [ConnectivityChecker] is consulted before any socket is opened. If offline,
 * [NetworkResponse.NetworkUnavailable] is returned immediately and no timeout
 * is consumed.
 *
 * Provided as a Hilt singleton by [com.sentinel.ai.core.di.NetworkModule].
 */
@Singleton
class HttpClientWrapper @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val connectivityChecker: ConnectivityChecker,
    private val config: NetworkConfig,
    private val dispatcherProvider: DispatcherProvider
) {

    /**
     * Executes [request] and returns a [NetworkResponse] describing the
     * outcome. Never throws — all failure paths are represented as sealed
     * variants.
     *
     * **[CancellationException] is the only exception that may escape**: it
     * is re-thrown immediately to preserve coroutine cancellation semantics.
     */
    suspend fun execute(request: NetworkRequest): NetworkResponse<String> {
        if (!connectivityChecker.isConnected()) {
            Timber.d("HttpClientWrapper: no network connectivity, skipping request to ${request.url}")
            return NetworkResponse.NetworkUnavailable
        }

        return withContext(dispatcherProvider.io) {
            executeWithRetry(request)
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Attempts the request and retries on transient failures.
     *
     * Retry policy:
     * - [SocketTimeoutException] → retry, then [NetworkResponse.Timeout]
     * - Other [IOException] → retry, then [NetworkResponse.IoFailure]
     * - HTTP 5xx → retry, then [NetworkResponse.HttpError]
     * - HTTP 4xx → **no retry**, return [NetworkResponse.HttpError] immediately
     * - [CancellationException] → **re-thrown immediately**, never caught here
     */
    private suspend fun executeWithRetry(request: NetworkRequest): NetworkResponse<String> {
        var lastResponse: NetworkResponse<String> = NetworkResponse.NetworkUnavailable
        var attempt = 0

        while (attempt <= config.maxRetries) {
            if (attempt > 0) {
                Timber.w("HttpClientWrapper: retry attempt $attempt/${config.maxRetries} for ${request.url}")
                delay(config.retryDelayMs)
            }

            lastResponse = attemptOnce(request)

            when {
                lastResponse is NetworkResponse.Success -> return lastResponse
                lastResponse is NetworkResponse.HttpError && lastResponse.code in 400..499 -> {
                    // 4xx: permanent client error, never retry
                    Timber.w("HttpClientWrapper: HTTP ${lastResponse.code} for ${request.url} — not retrying (client error)")
                    return lastResponse
                }
                else -> {
                    // Timeout, IoFailure, or HTTP 5xx — eligible for retry
                    attempt++
                }
            }
        }

        Timber.e("HttpClientWrapper: all ${ config.maxRetries} retry attempts exhausted for ${request.url}, last result: $lastResponse")
        return lastResponse
    }

    /**
     * Makes a single HTTP call. Never retries.
     *
     * [CancellationException] is **never caught** here — it propagates up
     * through [executeWithRetry] and out of [execute], which correctly
     * cancels the coroutine.
     */
    private fun attemptOnce(request: NetworkRequest): NetworkResponse<String> {
        return try {
            val okRequest = buildOkHttpRequest(request)
            okHttpClient.newCall(okRequest).execute().use { response ->
                val code = response.code
                val bodyString = response.body?.string() ?: ""
                val headers = response.headers.toMultimap()

                if (response.isSuccessful) {
                    Timber.d("HttpClientWrapper: HTTP $code success for ${request.url}")
                    NetworkResponse.Success(
                        data = bodyString,
                        statusCode = code,
                        headers = headers
                    )
                } else {
                    Timber.w("HttpClientWrapper: HTTP $code error for ${request.url} — ${response.message}")
                    NetworkResponse.HttpError(code = code, message = response.message)
                }
            }
        } catch (e: CancellationException) {
            // CancellationException must NEVER be swallowed. Re-throw immediately.
            throw e
        } catch (e: SocketTimeoutException) {
            Timber.w(e, "HttpClientWrapper: timeout for ${request.url}")
            NetworkResponse.Timeout
        } catch (e: IOException) {
            Timber.w(e, "HttpClientWrapper: IO failure for ${request.url}")
            NetworkResponse.IoFailure(cause = e)
        }
    }

    /**
     * Translates a [NetworkRequest] into an OkHttp [Request].
     */
    private fun buildOkHttpRequest(request: NetworkRequest): Request {
        val builder = Request.Builder().url(request.url)

        // Apply provider-supplied headers (User-Agent here may override the
        // interceptor-level default if the provider sets it explicitly)
        request.headers.forEach { (name, value) -> builder.header(name, value) }

        when (request.method) {
            HttpMethod.GET -> builder.get()
            HttpMethod.POST -> {
                val mediaType = (request.contentType ?: "application/json").toMediaType()
                val body = (request.body ?: "").toRequestBody(mediaType)
                builder.post(body)
            }
        }

        return builder.build()
    }
}
