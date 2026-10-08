package com.sentinel.ai.protection.intent.reputation

import com.sentinel.ai.core.network.HttpClientWrapper
import com.sentinel.ai.core.network.JsonParser
import com.sentinel.ai.core.network.NetworkRequest
import com.sentinel.ai.core.network.NetworkResponse

/**
 * Abstract base class for reputation providers that perform network lookups.
 *
 * ## Purpose
 * Eliminates networking boilerplate from every future provider. A new
 * provider only needs to:
 * 1. Extend this class
 * 2. Declare [providerName]
 * 3. Implement [evaluate] — build a [NetworkRequest], call [execute],
 *    handle the [NetworkResponse]
 *
 * ## Example future provider
 * ```kotlin
 * @Singleton
 * class OpenPhishProvider @Inject constructor(
 *     httpClient: HttpClientWrapper,
 *     jsonParser: JsonParser
 * ) : BaseNetworkReputationProvider(httpClient, jsonParser) {
 *
 *     override val providerName = "OpenPhish"
 *
 *     override fun supports(target: ReputationTarget) =
 *         target is ReputationTarget.Url
 *
 *     override suspend fun evaluate(target: ReputationTarget): ReputationResult? {
 *         val url = (target as? ReputationTarget.Url)?.url ?: return null
 *         val request = NetworkRequest(
 *             url = FEED_URL,
 *             method = HttpMethod.GET
 *         )
 *         return when (val response = execute(request)) {
 *             is NetworkResponse.Success -> parseAndMatch(response.data, url)
 *             else -> null   // offline / error → ReputationManager ignores null
 *         }
 *     }
 * }
 * ```
 *
 * ## What is NOT in scope for Phase 3.8.1
 * Concrete provider implementations (OpenPhish, Google Safe Browsing,
 * VirusTotal, PhishTank) are not written here. This base class is the
 * sole deliverable for the `app` module in this phase.
 *
 * @param httpClient The shared HTTP execution abstraction. Injected by Hilt.
 * @param jsonParser Thin Gson wrapper for deserialising response bodies.
 *                   Injected by Hilt.
 */
abstract class BaseNetworkReputationProvider(
    private val httpClient: HttpClientWrapper,
    @PublishedApi internal val jsonParser: JsonParser
) : ReputationProvider {

    /**
     * Executes [request] via the shared [HttpClientWrapper].
     *
     * Providers call this instead of interacting with OkHttp directly.
     * The return type is a sealed [NetworkResponse] — no exception handling
     * is needed in the provider.
     *
     * Error cases ([NetworkResponse.NetworkUnavailable], [NetworkResponse.Timeout],
     * [NetworkResponse.IoFailure], [NetworkResponse.HttpError]) should all
     * be handled by returning `null` from [evaluate], which
     * [com.sentinel.ai.protection.intent.reputation.ReputationManagerImpl]
     * safely filters out.
     */
    protected suspend fun execute(request: NetworkRequest): NetworkResponse<String> =
        httpClient.execute(request)

    /**
     * Deserialises [json] into type [T] using the shared [JsonParser].
     *
     * Returns `null` if [json] is blank or malformed. Providers should treat
     * a `null` result as an inability to evaluate and return `null` from
     * [evaluate].
     */
    protected inline fun <reified T> parseJson(json: String): T? =
        jsonParser.parse(json)
}
