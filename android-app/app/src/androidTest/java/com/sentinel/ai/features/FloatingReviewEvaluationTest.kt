package com.sentinel.ai.features

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.ml.PipelineEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Authored development cases only. All calls use private installed-app pipelines and never open URLs. */
class FloatingReviewEvaluationTest {
    @Test fun completePrivatePipelineReportsLanguageCategoryCoverageAndTiming() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val fixture = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("floating-review-corpus.json").bufferedReader().use { it.readText() })
        val repository = EntryPointAccessors.fromApplication(context, PipelineEntryPoint::class.java).repository()
        val cases = fixture.getJSONArray("cases"); val outputs = JSONArray(); val violations = mutableListOf<String>()
        val before = ThreatJournal.scanResults.value.map { it.id }.toSet()
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i); val start = System.nanoTime()
            val result = when (case.getString("input_type")) {
                "qr" -> repository.analyzeQrPrivately(case.getString("content"))
                "link" -> repository.analyzeLinkPrivately(case.getString("content"))
                else -> repository.analyzeTextPrivately(case.getString("content"))
            }
            val expected = case.getString("expected")
            val coverage = result.coverageDetails?.assessment?.name ?: "COMPLETE"
            outputs.put(JSONObject().put("id", case.getString("id")).put("language", case.getString("language"))
                .put("category", case.getString("category")).put("label", case.getInt("label")).put("expected", expected)
                .put("decision", result.decision.name).put("coverage", coverage).put("risk_index", result.riskScore)
                .put("duration_ms", (System.nanoTime() - start) / 1_000_000))
            if (expected == "WARN" && result.decision.name == "ALLOW" || expected == "ALLOW" && result.decision.name != "ALLOW" || expected == "UNSUPPORTED" && coverage != "UNSUPPORTED") violations += case.getString("id")
        }
        val report = JSONObject().put("kind", fixture.getString("kind")).put("device", "${android.os.Build.MODEL}; API ${android.os.Build.VERSION.SDK_INT}").put("results", outputs)
        java.io.File(context.filesDir, "floating-review-evaluation.json").writeText(report.toString(2))
        assertEquals("Private evaluator must leave history unchanged", before, ThreatJournal.scanResults.value.map { it.id }.toSet())
        assertTrue("Authored functional contract mismatches: $violations", violations.isEmpty())
    }
}
