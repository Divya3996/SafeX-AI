package com.sentinel.ai.core.network

/**
 * Immutable description of a single outgoing HTTP request.
 *
 * Providers construct a [NetworkRequest] and hand it to
 * [HttpClientWrapper.execute]. They never interact with OkHttp directly.
 *
 * @property url         Fully-qualified URL including scheme, host, path, and
 *                       any query parameters.
 * @property method      HTTP verb to use (see [HttpMethod]).
 * @property headers     Additional request headers. The `User-Agent` and any
 *                       authentication headers set by the provider go here.
 *                       The infrastructure adds its own `User-Agent` header
 *                       automatically; provider-supplied values override it.
 * @property body        Raw request body string. Ignored for [HttpMethod.GET].
 * @property contentType MIME type for [body], e.g. `"application/json"`.
 *                       Ignored when [body] is null.
 */
data class NetworkRequest(
    val url: String,
    val method: HttpMethod,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val contentType: String? = null
)
