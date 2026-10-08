package com.sentinel.ai.protection.intent.link

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRiskEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FraudPatternRegressionTest(private val example: JsonObject) {
    @Test fun authoredAttackAndBenignControl() {
        val url = example["url"].asString
        val parsed = UrlNormalizer.parse(url)
        val supported = parsed.isValid && parsed.scheme in listOf("http", "https")
        val analysis = LinkHeuristicRiskEngine().analyze(url)
        when (example["expected"].asString) {
            "WARN" -> assertTrue(example["id"].asString, !supported || analysis.score >= 30f)
            "ALLOW" -> { assertTrue(supported); assertTrue(example["id"].asString, analysis.score < 30f) }
            "REJECT" -> assertFalse(example["id"].asString, supported)
            "UNKNOWN" -> assertTrue("Blind spots must stay documented", example["note"].asString.isNotBlank())
        }
    }
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "pattern {index}") fun examples(): List<Array<JsonObject>> {
            val json = FraudPatternRegressionTest::class.java.classLoader!!
                .getResourceAsStream("url-pattern-corpus.json")!!.bufferedReader().readText()
            return Gson().fromJson(json, JsonObject::class.java).getAsJsonArray("cases").map { arrayOf(it.asJsonObject) }
        }
    }
}
