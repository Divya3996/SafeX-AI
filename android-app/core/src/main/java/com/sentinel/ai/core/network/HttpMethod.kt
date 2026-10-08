package com.sentinel.ai.core.network

/**
 * Supported HTTP methods for [NetworkRequest].
 *
 * Adding a new verb (e.g. HEAD, PUT, DELETE) only requires adding a new
 * value here and handling it inside [HttpClientWrapper] — the public
 * `execute(NetworkRequest)` API remains unchanged.
 */
enum class HttpMethod {
    GET,
    POST
}
