package com.sentinel.ai.protection.intent.heuristic.rules.link

import com.sentinel.ai.protection.intent.heuristic.*
import com.sentinel.ai.protection.intent.link.LinkDestinations
import com.sentinel.ai.protection.intent.link.ParsedUrl
import java.net.URLDecoder

class ShortenedDestinationRule : LinkHeuristicRule {
    override val id = "shortened_destination"
    override val name = "Hidden Short-Link Destination"
    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val host = url.host?.removePrefix("www.")
        val hit = host in setOf("bit.ly", "tinyurl.com", "t.co", "cutt.ly", "is.gd", "ow.ly", "lnkd.in", "rebrand.ly", "tiny.cc", "rb.gy", "shorturl.at")
        return finding(hit, 30f, "Short link hides its final destination; it is not automatically fraudulent", RuleCategory.URL_STRUCTURE)
    }
}
class NumericHostEncodingRule : LinkHeuristicRule {
    override val id = "numeric_host_encoding"
    override val name = "Disguised Numeric Host"
    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig) = finding(url.isObfuscatedNumericHost,
        30f, "Numeric host uses an unusual IP address encoding", RuleCategory.DOMAIN)
}
class LayeredDestinationRule : LinkHeuristicRule {
    override val id = "layered_destination"
    override val name = "Layered or Encoded Destination"
    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        if (!url.isValid) return finding(false, 30f, "", RuleCategory.URL_STRUCTURE)
        val shallow = LinkDestinations.external(url).map { it.normalized }.toSet()
        val hit = LinkDestinations.external(url, 4, true).any { it.normalized !in shallow } ||
            LinkDestinations.suspiciousDeepEncoding(url, config)
        return finding(hit, 30f, "Redirect destination is deeply encoded, hidden in Base64, or requests an unsafe handoff", RuleCategory.URL_STRUCTURE)
    }
}
class ExecutableDownloadRule : LinkHeuristicRule {
    override val id = "executable_download"
    override val name = "Executable or Disguised Download"
    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val path = runCatching { URLDecoder.decode(url.rawPath.replace("+", "%2B"), "UTF-8") }.getOrDefault(url.rawPath)
        val file = path.substringAfterLast('/').lowercase().trim()
        val executable = Regex("\\.(apk|exe|msi|scr|bat|cmd|ps1|vbs|jar)$").containsMatchIn(file)
        val disguisedScript = Regex("\\.(pdf|docx?|xlsx?|jpe?g|png|txt)[ .]+(?:exe|apk|scr|js|vbs|bat|cmd|ps1)$").containsMatchIn(file)
        return finding(executable || disguisedScript, 30f,
            "Link downloads executable content or disguises a program as a document; file contents were not inspected", RuleCategory.URL_STRUCTURE)
    }
}
class SensitiveHostCueRule : LinkHeuristicRule {
    override val id = "sensitive_host_cues"
    override val name = "Combined Sensitive-Action Host Cues"
    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val cues = setOf("login", "verify", "verification", "account", "secure", "bank", "banking", "kyc", "otp", "password", "wallet", "connect", "refund", "gift", "claim", "reward", "update", "payment")
        val words = url.host.orEmpty().split(Regex("[^a-z0-9]+"))
        val hit = (words.toSet().count { it in cues } >= 2 ||
            url.subdomainCount >= 5 && Regex("(?i)(login|verify|password|otp|kyc)").containsMatchIn(url.path)) &&
            !LinkDestinations.official(url.host, config)
        return finding(hit, 30f, "Domain combines sensitive-action cues or deep nesting with an account-action path", RuleCategory.SOCIAL_ENGINEERING)
    }
}
class BrandCamouflageRule : LinkHeuristicRule {
    override val id = "brand_camouflage"
    override val name = "Trusted Brand Outside Actual Host"
    override fun evaluate(url: ParsedUrl, config: LinkHeuristicConfig): RuleResult {
        val text = (url.path + "?" + url.query.orEmpty() + "#" + url.fragment.orEmpty()).lowercase()
        val cue = Regex("(?:login|signin|verify|password|account|otp)").containsMatchIn(text)
        val hit = cue && config.brandOfficialDomains.values.flatten().any { domain ->
            text.contains(domain) && url.host != domain && url.host?.endsWith(".$domain") != true
        }
        // A link to documentation containing a brand name is not itself impersonation.
        return finding(hit, 30f, "Trusted brand text appears in the path, query, or fragment instead of the actual destination host", RuleCategory.BRAND_IMPERSONATION)
    }
}
private fun finding(hit: Boolean, score: Float, reason: String, category: RuleCategory) =
    RuleResult(hit, if (hit) score else 0f, if (hit) reason else null, category)
