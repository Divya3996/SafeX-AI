package com.sentinel.ai.protection.intent.link

import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64

/** Bounded destination inspection. Never resolves or follows a redirect. */
object LinkDestinations {
    // Overlapping matches expose an inner destination even when an outer URL is same-site.
    private val http = Regex("(?i)(?=(https?://[^\\s<>\"']+))")
    private val relative = Regex("(?i)(?=(?:^|[?&=])(//[^\\s<>\"']+))")
    private val activeScheme = Regex("(?i)^(javascript:|data:text/html|intent:|file:)")
    fun site(host: String) = ("https://$host").toHttpUrlOrNull()?.topPrivateDomain() ?: host.trimEnd('.')
    fun sameSite(a: String?, b: String?) = a != null && b != null && site(a) == site(b)
    fun official(host: String?, config: LinkHeuristicConfig) = host != null && config.brandOfficialDomains.values
        .flatten().any { host == it || host.endsWith(".$it") }

    fun external(url: ParsedUrl, depth: Int = 2, includeBase64: Boolean = false): List<ParsedUrl> {
        if (!url.isValid) return emptyList()
        val values = mutableListOf<String>()
        values += url.rawPath
        values += url.fragment.orEmpty()
        url.queryParameters.forEach { parameter ->
            var current = parameter.rawValue
            values += current
            repeat(depth.coerceIn(0, 4)) {
                current = decode(current); values += current
            }
            if (includeBase64 && parameter.decodedName.lowercase() in LinkHeuristicConfig().redirectParameters) {
                decodeBase64(current)?.let { values += it }
            }
        }
        // Percent decoding in a path preserves '+': it is not form data.
        var path = url.rawPath
        repeat(depth.coerceIn(0, 4)) { path = decode(path.replace("+", "%2B")); values += path }
        return values.asSequence().flatMap { value ->
            val found = http.findAll(value).map { it.groupValues[1] }.toList().toMutableList()
            found += relative.findAll(value).map { "https:${it.groupValues[1]}" }
            found.asSequence()
        }.map(UrlNormalizer::parse).filter { it.isValid && it.scheme in listOf("http", "https") &&
            !sameSite(url.host, it.host) }.distinctBy { it.normalized }.take(8).toList()
    }

    fun suspiciousDeepEncoding(url: ParsedUrl, config: LinkHeuristicConfig): Boolean = url.queryParameters.any { p ->
        if (p.decodedName.lowercase() !in config.redirectParameters) false else {
            var value = p.rawValue
            repeat(4) { value = decode(value) }
            activeScheme.containsMatchIn(value) ||
                Regex("(?i)^https?%(?:25)*(?:3a|253a)").containsMatchIn(value) ||
                decodeBase64(value)?.let { activeScheme.containsMatchIn(it) } == true
        }
    }
    private fun decode(value: String) = runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }.getOrDefault(value)
    private fun decodeBase64(value: String): String? {
        if (value.length !in 12..4096 || !value.matches(Regex("[A-Za-z0-9+/_-]+={0,2}"))) return null
        val bytes = runCatching { Base64.getUrlDecoder().decode(value) }
            .recoverCatching { Base64.getDecoder().decode(value) }.getOrNull() ?: return null
        if (bytes.any { (it.toInt() and 255) !in 32..126 }) return null
        return bytes.toString(StandardCharsets.UTF_8).takeIf { it.startsWith("http", true) || it.startsWith("//") || activeScheme.containsMatchIn(it) }
    }
}
