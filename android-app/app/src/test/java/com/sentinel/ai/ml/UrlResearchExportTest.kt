package com.sentinel.ai.ml

import com.google.gson.Gson
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRiskEngine
import com.sentinel.ai.protection.intent.link.UrlNormalizer
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Opt-in offline research export. Never performs DNS, HTTP or page inspection. */
class UrlResearchExportTest {
    private val gson = Gson()
    private val engine = LinkHeuristicRiskEngine()
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    @Test fun capturePatternAudit() {
        val output = System.getenv("SAFEX_PATTERN_AUDIT")
        assumeTrue("Optional audit export", output != null)
        val source = javaClass.classLoader!!.getResourceAsStream("url-pattern-corpus.json")!!.bufferedReader().readText()
        val root = gson.fromJson(source, com.google.gson.JsonObject::class.java)
        val rows = root.getAsJsonArray("cases").map { entry ->
            val row = entry.asJsonObject.deepCopy()
            val url = row["url"].asString
            val parsed = UrlNormalizer.parse(url)
            val result = engine.analyze(url)
            row.addProperty("valid", parsed.isValid && parsed.scheme in listOf("http", "https"))
            row.addProperty("score", result.score)
            row.addProperty("risk", result.riskLevel.name)
            row.add("features", gson.toJsonTree(FeatureExtractor.extract(url)))
            row.add("reasons", gson.toJsonTree(result.ruleResults.filter { it.triggered }.map { it.explanation }))
            row
        }
        File(output!!).apply { parentFile?.mkdirs(); writeText(gson.toJson(rows)) }
    }

    @Test fun exportModelCoverage() {
        val input = System.getenv("SAFEX_URL_DATASET")
        val output = System.getenv("SAFEX_URL_COVERAGE")
        assumeTrue("Optional model coverage export", input != null && output != null)
        File(output!!).bufferedWriter().use { writer ->
            File(input!!).forEachLine { line ->
                val row = gson.fromJson(line, com.google.gson.JsonObject::class.java)
                writer.write(gson.toJson(mapOf("index" to row["index"].asInt,
                    "eligible" to UrlModelCoverage.canWarnIndependently(UrlNormalizer.parse(row["url"].asString)))))
                writer.newLine()
            }
        }
    }

    @Test fun exportLicensedDatasetFeatures() {
        val input = System.getenv("SAFEX_URL_DATASET")
        val output = System.getenv("SAFEX_URL_FEATURES")
        assumeTrue("Optional licensed dataset export", input != null && output != null)
        File(output!!).parentFile?.mkdirs()
        var count = 0
        File(output).bufferedWriter().use { writer ->
            File(input!!).forEachLine { line ->
                val row = gson.fromJson(line, com.google.gson.JsonObject::class.java)
                val url = row["url"].asString
                val parsed = UrlNormalizer.parse(url)
                val valid = parsed.isValid && parsed.scheme in listOf("http", "https") && url.length <= 8192
                val host = parsed.host.orEmpty().trimEnd('.')
                val domain = ("https://$host").toHttpUrlOrNull()?.topPrivateDomain() ?: host
                val sourceHost = row.get("source_url")?.asString?.let { UrlNormalizer.parse(it).host }
                val sourceDomain = sourceHost?.let { ("https://$it").toHttpUrlOrNull()?.topPrivateDomain() ?: it }
                if (sourceDomain != null && sourceDomain != domain) return@forEachLine
                val group = hash("SafeX URL v1:$domain")
                val bucket = group.take(8).toLong(16) % 100
                val split = if (bucket < 70) "train" else if (bucket < 85) "validation" else "test"
                val exported = mapOf("index" to row["index"].asInt, "label" to row["label"].asInt,
                    "group" to group, "split" to split, "valid" to valid,
                    "augmentation" to (row.get("augmentation")?.asString ?: "original"),
                    "features" to FeatureExtractor.extract(url), "ruleScore" to engine.analyze(url).score)
                writer.write(gson.toJson(exported)); writer.newLine(); count++
            }
        }
        println("Exported $count offline URL rows using the Android production feature extractor and public-suffix domain grouping.")
    }
}
