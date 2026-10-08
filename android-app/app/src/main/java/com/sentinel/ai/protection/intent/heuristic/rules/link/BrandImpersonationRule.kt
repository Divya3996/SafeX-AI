package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.*
import com.sentinel.ai.protection.intent.link.LinkDestinations
import com.sentinel.ai.protection.intent.link.ParsedUrl
import java.net.IDN
import java.text.Normalizer

class BrandImpersonationRule : LinkHeuristicRule {
    override val id = "brand_impersonation"
    override val name = "Brand Impersonation"

    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val host = url.host?.removePrefix("www.").orEmpty()
        if (host.isBlank()) return clean()
        val registrable = LinkDestinations.site(host)
        val rawLabel = registrable.substringBefore('.')
        val unicodeLabel = IDN.toUnicode(rawLabel)
        val label = skeleton(unicodeLabel)
        val credentialContext = Regex("(?i)(login|signin|verify|password|account|otp|kyc|auth)")
            .containsMatchIn(url.path + url.query.orEmpty())
        config.brandOfficialDomains.forEach { (brand, domains) ->
            val official = domains.any { host == it || host.endsWith(".$it") }
            if (official) return@forEach
            if (domains.any { Regex("(?:^|\\.)${Regex.escape(it)}\\.").containsMatchIn(host) }) {
                return hit(config.weights["brand_impersonation"] ?: 30f,
                    "Trusted domain text is placed before a different actual domain")
            }
            if (label == brand || hasExtraWord(label, brand, config)) {
                return hit(config.weights[if (hasExtraWord(label, brand, config)) "brand_impersonation" else "brand_lookalike"] ?: 30f,
                    "Possible $brand brand impersonation")
            }
            val changedScript = unicodeLabel != label && label == brand
            val substituted = label.length == brand.length && label.zip(brand).all { (actual, expected) ->
                actual == expected || actual in config.lookAlikeReplacements[expected].orEmpty()
            } && label != brand
            val fuzzy = brand.length >= 5 && editDistance(label, brand) <= 1 &&
                (credentialContext || label.any(Char::isDigit) || unicodeLabel != rawLabel)
            if (changedScript || substituted || fuzzy) {
                return hit(config.weights["brand_lookalike"] ?: 30f, "Possible $brand look-alike domain")
            }
            // An exact brand in someone else's subdomain is meaningful with account-action cues.
            if (host.removeSuffix(".$registrable").split('.').any { skeleton(IDN.toUnicode(it)) == brand } &&
                (credentialContext || config.brandExtraWords.any { word -> host.split('.', '-').contains(word) })) {
                return hit(30f, "Possible $brand brand impersonation")
            }
        }
        return clean()
    }
    private fun hasExtraWord(label: String, brand: String, config: LinkHeuristicConfig) = config.brandExtraWords.any { word ->
        label.contains("$brand-$word") || label.contains("$word-$brand") || label.contains("$brand$word") || label.contains("$word$brand")
    }
    /** Small audited confusable set, not a claim to implement Unicode's complete confusable table. */
    private fun skeleton(text: String): String {
        val replacements = mapOf('а' to 'a','е' to 'e','о' to 'o','р' to 'p','с' to 'c','у' to 'y',
            'х' to 'x','і' to 'i','ј' to 'j','ӏ' to 'l','ѕ' to 's','α' to 'a','ο' to 'o','ρ' to 'p','ι' to 'i')
        return Normalizer.normalize(text.lowercase(java.util.Locale.ROOT), Normalizer.Form.NFKD)
            .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
            .map { replacements[it] ?: it }.joinToString("")
    }
    private fun editDistance(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 1 || a.length > 64) return 2
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            dp[i][j] = minOf(dp[i-1][j]+1, dp[i][j-1]+1, dp[i-1][j-1]+if (a[i-1]==b[j-1]) 0 else 1)
            if (i>1 && j>1 && a[i-1]==b[j-2] && a[i-2]==b[j-1]) dp[i][j]=minOf(dp[i][j],dp[i-2][j-2]+1)
        }
        return dp[a.length][b.length]
    }
    private fun hit(score: Float, reason: String) = RuleResult(true, score, reason, RuleCategory.BRAND_IMPERSONATION)
    private fun clean() = RuleResult(false, 0f, null, RuleCategory.BRAND_IMPERSONATION)
}
