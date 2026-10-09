package com.sentinel.ai.ml

import com.google.gson.JsonParser
import com.sentinel.ai.core.model.OfflineTextClassifier
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class BundledTextModelTest {
    private val assets = File("src/main/assets")
    private fun classifier(): OfflineTextClassifier {
        val json = JsonParser.parseString(File(assets, "text-model.json").readText()).asJsonObject
        return OfflineTextClassifier(json["vocabulary"].asJsonArray.map { it.asString },
            json["weights"].asJsonArray.map { it.asDouble }, json["intercept"].asDouble,
            json["threshold"].asFloat, json["feature_version"].asString)
    }
    @Test fun artifactMatchesCardAndMasksAddresses() {
        val bytes = File(assets, "text-model.json").readBytes()
        val card = JsonParser.parseString(File(assets, "text-model-card.json").readText()).asJsonObject
        assertEquals(card["sha256"].asString, MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) })
        val json = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
        assertFalse(json["vocabulary"].asJsonArray.any { it.asString.contains("@") || it.asString.contains("://") || it.asString.contains(Regex("[0-9]{4,}")) })
        assertTrue(bytes.size < 2 * 1024 * 1024)
    }
    @Test fun scorerMatchesPythonForAuthoredParityFixtures() {
        val fixture = JsonParser.parseString(File("src/androidTest/assets/text-model-parity.json").readText()).asJsonObject
        val model = classifier()
        assertEquals(fixture["threshold"].asFloat, model.warningThreshold, 0f)
        fixture["cases"].asJsonArray.forEach { entry ->
            val row = entry.asJsonObject
            assertEquals(row["text"].asString, row["predicted"].asFloat, model.predict(row["text"].asString), 0.00001f)
        }
    }
    @Test fun numericAndDestinationVariantsDoNotChangeTextEvidence() {
        val model = classifier()
        assertEquals(model.predict("Your code 123456 visit https://first.example/path"),
            model.predict("Your code 999988 visit https://second.example/other"), 0f)
    }
    @Test fun contextFreePolicyKeepsAmbiguousNeutralMessagesBelowThreshold() {
        val policy = JsonParser.parseString(File(assets, "text-warning-policy.json").readText()).asJsonObject
        val model = classifier()
        val threshold = policy["threshold"].asFloat
        assertTrue(threshold > model.warningThreshold)
        for (text in listOf("We received your registration for the free workshop",
            "Your interview is confirmed. No registration fee is required.",
            "Your verification code is private.")) {
            assertTrue(text, model.predict(text) < threshold)
        }
    }
}
