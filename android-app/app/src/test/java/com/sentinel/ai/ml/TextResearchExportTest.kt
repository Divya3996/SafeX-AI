package com.sentinel.ai.ml

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sentinel.ai.core.model.MessageSignals
import com.sentinel.ai.core.model.TextModelFeatures
import com.sentinel.ai.core.model.OfflineTextClassifier
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Opt-in local research: exports actual rule filtering and model eligibility. No network. */
class TextResearchExportTest {
    @Test fun exportMessagePolicy() {
        val input = System.getenv("SAFEX_MESSAGE_DATASET")
        val output = System.getenv("SAFEX_MESSAGE_FEATURES")
        assumeTrue("Optional local message policy export", input != null && output != null)
        val gson = Gson()
        val model = gson.fromJson(File("src/main/assets/text-model.json").readText(), JsonObject::class.java)
        val classifier = OfflineTextClassifier(model["vocabulary"].asJsonArray.map { it.asString },
            model["weights"].asJsonArray.map { it.asDouble }, model["intercept"].asDouble,
            model["threshold"].asFloat, model["feature_version"].asString)
        File(output!!).bufferedWriter().use { writer ->
            File(input!!).forEachLine { line ->
                val row = gson.fromJson(line, JsonObject::class.java)
                val signals = MessageSignals.analyze(row["text"].asString)
                row.addProperty("classifierInputRaw", signals.classifierInput)
                row.addProperty("classifierInput", TextModelFeatures.normalize(signals.classifierInput))
                row.addProperty("eligible", signals.modelWarningEligible)
                row.addProperty("contextFreeVocabularyEligible", classifier.supportsContextFreeWarning(signals.classifierInput))
                row.addProperty("ruleScore", signals.score)
                row.add("terms", gson.toJsonTree(TextModelFeatures.extract(signals.classifierInput).sorted()))
                writer.write(gson.toJson(row)); writer.newLine()
            }
        }
    }
}
