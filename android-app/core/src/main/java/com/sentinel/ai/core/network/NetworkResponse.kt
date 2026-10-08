package com.sentinel.ai.core.network

import java.io.IOException

/**
 * Sealed result type returned by [HttpClientWrapper.execute].
 *
 * Every network outcome maps to exactly one variant. Callers use an
 * exhaustive `when` expression — no exception handling required outside
 * [HttpClientWrapper].
 *
 * @param T The type of the parsed response payload.
 */
sealed class NetworkResponse<out T> {

    /**
     * The request completed with a 2xx HTTP response.
     *
     * HTTP metadata is included so future providers (e.g. Google Safe
     * Browsing, VirusTotal) can inspect rate-limit and retry-after headers
     * without any change to this API.
     *
     * @property data       The response body, typed as [T].
     * @property statusCode HTTP status code, e.g. 200.
     * @property headers    All response headers. Values are lists because HTTP
     *                      allows multiple values for the same header name.
     */
    data class Success<T>(
        val data: T,
        val statusCode: Int,
        val headers: Map<String, List<String>>
    ) : NetworkResponse<T>()

    /**
     * The server returned a non-2xx HTTP response.
     *
     * HTTP 4xx errors are **never retried** — they indicate a permanent
     * client-side problem (bad URL, missing API key, etc.).
     * HTTP 5xx errors may be retried up to [NetworkConfig.maxRetries] times.
     *
     * @property code    HTTP status code, e.g. 404 or 503.
     * @property message HTTP status message from the server, e.g. "Not Found".
     */
    data class HttpError(
        val code: Int,
        val message: String
    ) : NetworkResponse<Nothing>()

    /**
     * No network interface was available when the request was attempted.
     *
     * The request is **never made** — [ConnectivityChecker] gates it before
     * any socket is opened. Offline heuristic detection continues normally.
     */
    data object NetworkUnavailable : NetworkResponse<Nothing>()

    /**
     * The request exceeded the configured socket or connection timeout.
     *
     * Retried up to [NetworkConfig.maxRetries] times before this variant
     * is returned to the caller.
     */
    data object Timeout : NetworkResponse<Nothing>()

    /**
     * A non-timeout I/O error occurred during the request.
     *
     * Retried up to [NetworkConfig.maxRetries] times. The original
     * [IOException] is preserved for logging but never propagated to callers.
     *
     * @property cause The underlying [IOException].
     */
    data class IoFailure(
        val cause: IOException
    ) : NetworkResponse<Nothing>()
}
