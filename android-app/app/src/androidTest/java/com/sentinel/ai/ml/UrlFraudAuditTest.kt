package com.sentinel.ai.ml

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sentinel.ai.core.model.ProtectionDecision
import com.sentinel.ai.protection.intent.heuristic.LinkHeuristicRiskEngine
import com.sentinel.ai.protection.intent.link.UrlNormalizer
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

/** Input strings only: never opens, resolves or follows dataset/example URLs. */
@RunWith(AndroidJUnit4::class)
class UrlFraudAuditTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private fun asset(name: String) = JSONObject(testContext.assets.open(name).bufferedReader().use { it.readText() })
    private val repository get() = EntryPointAccessors.fromApplication(context, PipelineEntryPoint::class.java).repository()

    @Test fun heldOutFeaturesAndNativePredictionsMatchTraining() = runBlocking {
        val fixture = asset("native-url-evaluation.json")
        val cases = fixture.getJSONArray("cases")
        var maximumDifference = 0f
        var tp = 0; var fp = 0; var fn = 0; var tn = 0
        val durations = mutableListOf<Long>()
        MLInferenceManager(context).use { model ->
            assertEquals(fixture.getDouble("threshold").toFloat(), model.warningThreshold, 0.000001f)
            for (i in 0 until cases.length()) {
                val case = cases.getJSONObject(i)
                val normalized = UrlNormalizer.normalize(case.getString("url"))
                val features = FeatureExtractor.extract(normalized)
                val expected = case.getJSONArray("features")
                for (j in features.indices) assertEquals("Feature $j, case $i", expected.getDouble(j).toFloat(), features[j], 0.00001f)
                val score = model.predict(features)
                val difference = abs(score - case.getDouble("predicted").toFloat())
                maximumDifference = maxOf(maximumDifference, difference)
                assertTrue("Native TFLite differs on case $i by $difference", difference <= 0.0001f)
                val result = repository.scanLink(case.getString("url"))
                assertTrue("No model fallback, case $i", result.modelStatus.startsWith("On-device URL model"))
                assertTrue("Threshold must warn, case $i", !UrlModelCoverage.canWarnIndependently(UrlNormalizer.parse(normalized)) || score < model.warningThreshold || result.decision != ProtectionDecision.ALLOW)
                durations += result.durationMs
                val positive = result.decision != ProtectionDecision.ALLOW
                if (case.getInt("label") == 1) { if (positive) tp++ else fn++ }
                else { if (positive) fp++ else tn++ }
            }
        }
        durations.sort()
        val report = JSONObject().put("kind", "256 historical held-out native parity fixtures; not a representative accuracy benchmark")
            .put("cases", cases.length()).put("maximum_score_difference", maximumDifference)
            .put("pipeline_tp", tp).put("pipeline_fp", fp).put("pipeline_fn", fn).put("pipeline_tn", tn)
            .put("duration_ms_p50", durations[durations.size / 2])
            .put("duration_ms_p95", durations[(durations.size * .95).toInt()])
            .put("duration_ms_max", durations.last()).put("model_version", asset("native-url-evaluation.json").optString("model_version", "url-phiusiil-augmented-v2"))
        File(context.filesDir, "url-native-parity.json").writeText(report.toString(2))
    }

    @Test fun authoredAttackPatternsAndBenignControlsRunThroughInstalledApp() = runBlocking {
        val cases = asset("url-pattern-corpus.json").getJSONArray("cases")
        val results = JSONArray()
        var warned = 0; var unsupported = 0; var benignWarnings = 0
        val rules = LinkHeuristicRiskEngine()
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            val url = case.getString("url")
            val expected = case.getString("expected")
            var decision = "REJECT"; var modelStatus = ""
            try {
                val result = repository.scanLink(url)
                decision = result.decision.name; modelStatus = result.modelStatus
            } catch (_: IllegalArgumentException) { /* Explicit unsupported input, not a safe verdict. */ }
            when (expected) {
                "WARN" -> {
                    assertNotEquals(case.getString("id"), "ALLOW", decision)
                    if (decision == "REJECT") unsupported++ else warned++
                }
                "REJECT" -> assertEquals(case.getString("id"), "REJECT", decision)
                "ALLOW" -> {
                    assertTrue(case.getString("id"), UrlNormalizer.parse(url).isValid)
                    assertTrue("Benign structural control ${case.getString("id")}", rules.analyze(url).score < 30f)
                    assertNotEquals(case.getString("id"), "BLOCK", decision)
                    assertNotEquals(case.getString("id"), "REJECT", decision)
                    if (decision != "ALLOW") benignWarnings++
                }
                "UNKNOWN" -> assertTrue(case.getString("id"), case.getString("note").isNotBlank())
            }
            if (decision != "REJECT") assertTrue("No model fallback: ${case.getString("id")}", modelStatus.startsWith("On-device URL model"))
            results.put(JSONObject().put("id", case.getString("id")).put("family", case.getString("family"))
                .put("expected", expected).put("decision", decision))
        }
        File(context.filesDir, "url-native-patterns.json").writeText(JSONObject()
            .put("kind", "Authored regression examples, not independent fraud accuracy")
            .put("warn_cases_warned", warned).put("warn_cases_unsupported", unsupported)
            .put("benign_controls_warned_by_pipeline", benignWarnings).put("cases", results).toString(2))
    }
}
