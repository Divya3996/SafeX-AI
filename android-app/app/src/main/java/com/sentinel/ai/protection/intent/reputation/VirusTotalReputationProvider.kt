package com.sentinel.ai.protection.intent.reputation

import com.sentinel.ai.core.network.HttpClientWrapper
import com.sentinel.ai.core.network.HttpMethod
import com.sentinel.ai.core.network.JsonParser
import com.sentinel.ai.core.network.NetworkRequest
import com.sentinel.ai.core.network.NetworkResponse
import kotlinx.coroutines.delay
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reputation provider that evaluates URLs using the official VirusTotal v3 API.
 *
 * This provider executes a two-step lookup workflow:
 * 1. POST the target URL to `/urls` to initiate or retrieve an analysis.
 * 2. Poll the analyses endpoint GET `/analyses/{id}` until the analysis is completed.
 *
 * Verification results are mapped to [ReputationVerdict] and [ReputationResult] based on the
 * malicious and suspicious counts in the final analysis statistics.
 */
@Singleton
class VirusTotalReputationProvider @Inject constructor(
    httpClient: HttpClientWrapper,
    jsonParser: JsonParser,
    private val config: ReputationConfig
) : BaseNetworkReputationProvider(httpClient, jsonParser) {

    override val providerName: String = "VirusTotal"

    override fun supports(target: ReputationTarget): Boolean = target is ReputationTarget.Url

    override suspend fun evaluate(target: ReputationTarget): ReputationResult? {
        val urlTarget = target as? ReputationTarget.Url ?: run {
            Timber.w("VirusTotal: Unsupported target type: $target")
            return null
        }

        if (config.virusTotalApiKey.isBlank()) {
            Timber.d("VirusTotal: Provider disabled (API key is blank)")
            return null
        }

        val targetUrl = urlTarget.url
        if (targetUrl.isBlank()) {
            Timber.w("VirusTotal: Empty/blank URL target")
            return null
        }

        if (targetUrl.toHttpUrlOrNull() == null) {
            Timber.w("VirusTotal: Invalid URL: $targetUrl")
            return null
        }

        return try {
            performEvaluation(targetUrl)
        } catch (e: Exception) {
            Timber.e(e, "VirusTotal: Unexpected exception during evaluation of $targetUrl")
            null
        }
    }

    private suspend fun performEvaluation(targetUrl: String): ReputationResult? {
        val baseUrl = config.virusTotalLookupUrl.trim()
        val postUrl = if (baseUrl.endsWith("/")) "${baseUrl}urls" else "$baseUrl/urls"
        val encodedUrl = URLEncoder.encode(targetUrl, "UTF-8")
        val postBody = "url=$encodedUrl"

        val postRequest = NetworkRequest(
            url = postUrl,
            method = HttpMethod.POST,
            headers = mapOf(
                "x-apikey" to config.virusTotalApiKey.trim(),
                "Content-Type" to "application/x-www-form-urlencoded"
            ),
            body = postBody,
            contentType = "application/x-www-form-urlencoded"
        )

        val postResponse = execute(postRequest)
        if (postResponse !is NetworkResponse.Success) {
            Timber.w("VirusTotal: POST urls failed with response: $postResponse")
            return null
        }

        val postData = parseJson<UrlLookupResponse>(postResponse.data)
        val analysisId = postData?.data?.id
        if (analysisId.isNullOrBlank()) {
            Timber.w("VirusTotal: Failed to parse analysis ID from POST response")
            return null
        }

        val getUrl = if (baseUrl.endsWith("/")) "${baseUrl}analyses/$analysisId" else "$baseUrl/analyses/$analysisId"
        var attempts = 0
        var stats: AnalysisStats? = null

        while (attempts < 5) {
            val getRequest = NetworkRequest(
                url = getUrl,
                method = HttpMethod.GET,
                headers = mapOf("x-apikey" to config.virusTotalApiKey.trim())
            )

            when (val getResponse = execute(getRequest)) {
                is NetworkResponse.Success -> {
                    val analysisResponse = parseJson<AnalysisResponse>(getResponse.data)
                    val status = analysisResponse?.data?.attributes?.status
                    if (status == "completed") {
                        stats = analysisResponse.data.attributes.stats
                        break
                    }
                }
                is NetworkResponse.HttpError -> {
                    Timber.e("VirusTotal: GET analyses failed with HTTP error code: ${getResponse.code}")
                    return null
                }
                else -> {
                    Timber.w("VirusTotal: GET analysis attempt ${attempts + 1} failed with response: $getResponse")
                    return null
                }
            }

            attempts++
            if (attempts < 5) {
                delay(1000L)
            }
        }

        if (stats == null) {
            Timber.w("VirusTotal: Analysis did not complete or stats are missing after max attempts")
            return null
        }

        val malicious = stats.malicious ?: 0
        val suspicious = stats.suspicious ?: 0
        val harmless = stats.harmless ?: 0
        val undetected = stats.undetected ?: 0

        val verdict = when {
            malicious > 0 -> ReputationVerdict.MALICIOUS
            suspicious > 0 -> ReputationVerdict.SUSPICIOUS
            else -> ReputationVerdict.UNKNOWN
        }

        val confidence = when (verdict) {
            ReputationVerdict.MALICIOUS -> 0.95f
            ReputationVerdict.SUSPICIOUS -> 0.75f
            else -> 0.0f
        }

        val reason = "VirusTotal stats: malicious=$malicious, suspicious=$suspicious, harmless=$harmless, undetected=$undetected"

        return ReputationResult(
            providerName = providerName,
            confidence = confidence,
            reputation = verdict,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
    }
}

private data class UrlLookupResponse(
    val data: UrlLookupData?
)

private data class UrlLookupData(
    val id: String?
)

private data class AnalysisResponse(
    val data: AnalysisData?
)

private data class AnalysisData(
    val attributes: AnalysisAttributes?
)

private data class AnalysisAttributes(
    val status: String?,
    val stats: AnalysisStats?
)

private data class AnalysisStats(
    val malicious: Int?,
    val suspicious: Int?,
    val harmless: Int?,
    val undetected: Int?
)
