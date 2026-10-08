package com.sentinel.ai.ml

import android.util.Log
import com.sentinel.ai.protection.intent.link.UrlNormalizer

object FeatureExtractor {

    const val FEATURE_COUNT = 15

    fun extract(url: String): FloatArray {


        return try {
            val parsedUrl = UrlNormalizer.parse(url)
            val host = parsedUrl.host.orEmpty()
            val normalizedUrl = parsedUrl.normalized
            val urlLength = normalizedUrl.length.coerceAtLeast(1).toFloat()
            val domainLength = host.length.coerceAtLeast(1).toFloat()
            val hasSuspiciousWords = SUSPICIOUS_WORDS.any {
                normalizedUrl.contains(it, ignoreCase = true)
            }
            val isKnownBrandDomain = KNOWN_BRAND_DOMAINS.values
                .flatten()
                .any { officialDomain ->
                    host == officialDomain || host.endsWith(".$officialDomain")
                }
            val brandImpersonationScore = (
                (if (!isKnownBrandDomain && KNOWN_BRAND_DOMAINS.keys.any(host::contains)) 0.5f else 0f) +
                    (if (hasSuspiciousWords) 0.25f else 0f) +
                    (if ('-' in host) 0.25f else 0f)
                ).coerceAtMost(1f)

            val features = floatArrayOf(
                normalizedUrl.length.toFloat(),
                host.length.toFloat(),
                if (parsedUrl.isIpv4) 1f else 0f,
                (host.split('.').size - 2).coerceAtLeast(0).toFloat(),
                if (parsedUrl.scheme.equals("https", ignoreCase = true)) 1f else 0f,
                if (hasSuspiciousWords) 1f else 0f,
                normalizedUrl.count { !it.isLetterOrDigit() }.toFloat() / urlLength,
                normalizedUrl.count(Char::isDigit).toFloat() / urlLength,
                if ('@' in normalizedUrl) 1f else 0f,
                if (host.substringAfterLast('.', "") in SUSPICIOUS_TLDS) 1f else 0f,
                brandImpersonationScore,
                normalizedUrl.count { it == '-' }.toFloat(),
                (parsedUrl.path.length + parsedUrl.query.orEmpty().length).toFloat(),
                if (isKnownBrandDomain) 1f else 0f,
                host.count { it.lowercaseChar() in VOWELS }.toFloat() / domainLength
            )

            check(features.size == FEATURE_COUNT && features.all(Float::isFinite)) {
                "Feature extraction produced invalid values"
            }

            features
        } catch (exception: Exception) {
            Log.e("ML_DEBUG", "Feature extraction failed")
            ZERO_FEATURES.copyOf()
        }
    }

    private val SUSPICIOUS_WORDS = listOf(
        "login",
        "verify",
        "secure",
        "account",
        "bank",
        "update",
        "crypto"
    )
    private val SUSPICIOUS_TLDS = setOf(
        "live", "click", "top", "xyz", "online", "info", "vip", "fit", "gq",
        "cf", "tk", "ml", "ga", "work", "club", "buzz", "support", "security",
        "update", "verify", "download", "bid", "loan", "men", "win", "stream"
    )
    private val KNOWN_BRAND_DOMAINS = com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig().brandOfficialDomains
    private val VOWELS = setOf('a', 'e', 'i', 'o', 'u')
    val FEATURE_NAMES = listOf(
        "URLLength",
        "DomainLength",
        "IsDomainIP",
        "NoOfSubDomain",
        "IsHTTPS",
        "HasSuspiciousWords",
        "SpecialCharRatio",
        "DigitRatio",
        "HasAtSymbol",
        "SuspiciousTLD",
        "BrandImpersonationScore",
        "HyphenCount",
        "PathQueryLength",
        "KnownBrandDomain",
        "DomainVowelRatio"
    )
    private val ZERO_FEATURES = FloatArray(FEATURE_COUNT)
}
