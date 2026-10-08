package com.sentinel.ai.protection.intent.reputation

import com.sentinel.ai.core.network.HttpClientWrapper
import com.sentinel.ai.core.network.HttpMethod
import com.sentinel.ai.core.network.JsonParser
import com.sentinel.ai.core.network.NetworkRequest
import com.sentinel.ai.core.network.NetworkResponse
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenPhishReputationProvider @Inject constructor(
    httpClient: HttpClientWrapper,
    jsonParser: JsonParser,
    private val config: ReputationConfig
) : BaseNetworkReputationProvider(httpClient, jsonParser) {

    override val providerName: String = "OpenPhish"
    private var cachedFeed: String? = null
    private var cachedAt: Long = 0
    private val cacheMutex = kotlinx.coroutines.sync.Mutex()

    override fun supports(target: ReputationTarget): Boolean = target is ReputationTarget.Url

    override suspend fun evaluate(target: ReputationTarget): ReputationResult? {
        val urlTarget = target as? ReputationTarget.Url ?: run {
            Timber.w("OpenPhish: Unsupported target type: $target")
            return null
        }

        if (!config.isOpenPhishEnabled) {
            Timber.d("OpenPhish: Provider disabled (feed URL is blank)")
            return null
        }

        val feedUrl = buildRequestUrl(config) ?: run {
            Timber.e("OpenPhish: Failed to build feed URL")
            return null
        }

        Timber.d("OpenPhish: Starting evaluation for target: ${urlTarget.url}")

        val request = NetworkRequest(
            url = feedUrl,
            method = HttpMethod.GET,
            headers = buildRequestHeaders(config)
        )

        val response = cacheMutex.withLock {
            val cached = cachedFeed
            if (cached != null && System.currentTimeMillis() - cachedAt < 6 * 60 * 60 * 1000L) {
                NetworkResponse.Success(cached, 200, emptyMap())
            } else {
                val fresh = execute(request)
                if (fresh is NetworkResponse.Success && fresh.data.isNotBlank() && !isHtmlContent(fresh.data)) {
                    cachedFeed = fresh.data; cachedAt = System.currentTimeMillis()
                }
                fresh
            }
        }

        return when (response) {
            is NetworkResponse.Success -> {
                val feedBody = response.data
                if (feedBody.isEmpty()) {
                    Timber.e("OpenPhish: Empty response feed")
                    return null
                }

                if (isHtmlContent(feedBody)) {
                    Timber.e("OpenPhish: Malformed response format")
                    return null
                }

                val targetUrl = urlTarget.url
                var isThreat = false
                try {
                    isThreat = feedBody.lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") }
                        .any { entry -> matchesTarget(entry, targetUrl) }
                } catch (e: Exception) {
                    Timber.e(e, "OpenPhish: Parse failure - exception while parsing line sequence")
                    return null
                }

                val confidence = if (isThreat) 0.98f else 0.0f
                val verdict = if (isThreat) ReputationVerdict.MALICIOUS else ReputationVerdict.UNKNOWN
                val reason = if (isThreat) {
                    "Matched OpenPhish feed entry."
                } else {
                    "No OpenPhish feed match detected."
                }

                val result = ReputationResult(
                    providerName = providerName,
                    confidence = confidence,
                    reputation = verdict,
                    reason = reason,
                    timestamp = System.currentTimeMillis()
                )

                Timber.d("OpenPhish: Success - Target: ${urlTarget.url}, Verdict: $verdict, Reason: $reason")
                result
            }
            is NetworkResponse.NetworkUnavailable -> {
                Timber.w("OpenPhish: Offline - network unavailable")
                null
            }
            is NetworkResponse.Timeout -> {
                Timber.w("OpenPhish: Timeout - request timed out")
                null
            }
            is NetworkResponse.HttpError -> {
                Timber.e("OpenPhish: Provider failure - HTTP error code: ${response.code}, message: ${response.message}")
                null
            }
            is NetworkResponse.IoFailure -> {
                Timber.e(response.cause, "OpenPhish: Provider failure - IO exception occurred")
                null
            }
        }
    }

    companion object {
        private fun buildRequestUrl(config: ReputationConfig): String? {
            val rawUrl = config.openPhishFeedUrl.trim()
            if (rawUrl.isBlank()) {
                return null
            }

            val key = config.openPhishApiKey.trim()
            if (key.isBlank()) {
                return rawUrl
            }

            val httpUrl = rawUrl.toHttpUrlOrNull()
            return httpUrl?.newBuilder()
                ?.addQueryParameter("api_key", key)
                ?.build()
                ?.toString()
                ?: if (rawUrl.contains("?")) "$rawUrl&api_key=$key" else "$rawUrl?api_key=$key"
        }

        private fun buildRequestHeaders(config: ReputationConfig): Map<String, String> {
            val key = config.openPhishApiKey.trim()
            if (key.isBlank()) {
                return emptyMap()
            }
            return mapOf("Authorization" to "Bearer $key")
        }

        private fun isHtmlContent(content: String): Boolean {
            val trimmed = content.trim().lowercase()
            return trimmed.startsWith("<!doctype") ||
                    trimmed.startsWith("<html") ||
                    trimmed.contains("</html>") ||
                    trimmed.contains("</body>")
        }

        private fun normalizeUrl(url: String): String {
            var u = url.trim().lowercase()
            if (u.startsWith("https://")) {
                u = u.substring(8)
            } else if (u.startsWith("http://")) {
                u = u.substring(7)
            } else if (u.startsWith("//")) {
                u = u.substring(2)
            }
            if (u.startsWith("www.")) {
                u = u.substring(4)
            }
            if (u.endsWith("/")) {
                u = u.substring(0, u.length - 1)
            }
            return u
        }

        private fun matchesTarget(entry: String, targetUrl: String): Boolean {
            val entryNormalized = normalizeUrl(entry)
            val targetNormalized = normalizeUrl(targetUrl)

            if (entryNormalized == targetNormalized) {
                return true
            }

            val slashIndex = entryNormalized.indexOf('/')
            if (slashIndex == -1) {
                if (targetNormalized.startsWith(entryNormalized)) {
                    val nextCharIndex = entryNormalized.length
                    if (targetNormalized.length == nextCharIndex || targetNormalized[nextCharIndex] == '/') {
                        return true
                    }
                }
            } else {
                if (targetNormalized.startsWith(entryNormalized)) {
                    val nextCharIndex = entryNormalized.length
                    if (targetNormalized.length == nextCharIndex || targetNormalized[nextCharIndex] == '/') {
                        return true
                    }
                }
            }

            return false
        }
    }
}
