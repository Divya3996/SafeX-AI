package com.sentinel.ai.ml

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sentinel.ai.core.model.ProtectionDecision
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FraudRetrainingDeviceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private fun fixture(name: String) = JSONObject(testContext.assets.open(name).bufferedReader().use { it.readText() })
    private val repository get() = EntryPointAccessors.fromApplication(context, PipelineEntryPoint::class.java).repository()

    @Test fun nativeTextScorerMatchesPublishedAuthoredParity() {
        val source = fixture("text-model-parity.json")
        val model = TextInferenceManager(context)
        assertTrue(model.usesResearchFeatures)
        assertEquals(source.getDouble("threshold").toFloat(), model.warningThreshold, 0.000001f)
        val rows = source.getJSONArray("cases")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            assertEquals("Authored text parity $i", row.getDouble("predicted").toFloat(), model.predict(row.getString("text")), 0.00001f)
        }
    }

    @Test fun authoredThreeLanguageFraudAndProtectiveControlsUsePrivatePipeline() = runBlocking {
        val rows = fixture("fraud-app-contracts.json").getJSONArray("cases")
        val results = JSONArray()
        val mismatches = mutableListOf<String>()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val result = repository.analyzeTextPrivately(row.getString("text"))
            val positive = result.decision != ProtectionDecision.ALLOW
            val expected = row.getString("expected") == "WARN"
            if (positive != expected) mismatches += row.getString("id") + " → " + result.decision.name
            assertTrue("Research model loaded: ${row.getString("id")}", result.modelStatus.contains("limited research coverage"))
            results.put(JSONObject().put("id", row.getString("id")).put("language", row.getString("language"))
                .put("expected", row.getString("expected")).put("decision", result.decision.name).put("duration_ms", result.durationMs))
        }
        File(context.filesDir, "fraud-app-contract-results.json").writeText(JSONObject()
            .put("scope", "120 authored contracts, including training examples; not independent accuracy evidence")
            .put("cases", results).put("mismatches", JSONArray(mismatches)).toString(2))
        assertTrue(mismatches.joinToString("; "), mismatches.isEmpty())
    }

    @Test fun trainedModelsLoadOfflineWithoutInternetPermission() {
        val permissions = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse(permissions.contains("android.permission.INTERNET"))
        assertFalse(permissions.contains("android.permission.ACCESS_NETWORK_STATE"))
        MLInferenceManager(context).use { model ->
            val score = model.predict(FeatureExtractor.extract("https://example.com/help"))
            assertTrue(score.isFinite() && score in 0f..1f)
        }
        assertTrue(TextInferenceManager(context).predict("Bank support requests your OTP").isFinite())
    }

    @Test fun evaluateTransientObservedMessagesThroughInstalledPrivatePipeline() = runBlocking {
        val input = File(context.filesDir, "fraud-observed-evaluation.jsonl")
        assumeTrue("Optional transient local research input", input.exists())
        val started = android.os.SystemClock.elapsedRealtime()
        val cases = JSONArray()
        var tp = 0; var fp = 0; var fn = 0; var tn = 0; var errors = 0
        val durations = mutableListOf<Long>()
        input.forEachLine { line ->
            val row = JSONObject(line)
            try {
                val result = runBlocking { repository.analyzeTextPrivately(row.getString("text")) }
                val positive = result.decision != ProtectionDecision.ALLOW
                if (row.getInt("label") == 1) { if (positive) tp++ else fn++ }
                else { if (positive) fp++ else tn++ }
                durations += result.durationMs
                cases.put(JSONObject().put("id", row.getString("id")).put("label", row.getInt("label"))
                    .put("decision", result.decision.name).put("score", result.riskScore)
                    .put("reasons", JSONArray(result.reasons.map { it.sourceName })))
            } catch (error: Exception) {
                errors++
                cases.put(JSONObject().put("id", row.getString("id")).put("error", error.javaClass.simpleName))
            }
        }
        durations.sort()
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val report = JSONObject().put("scope", "Installed private text pipeline; inspected historical development evaluation, not independent current-world accuracy")
            .put("app_version", version).put("tp", tp).put("fp", fp).put("fn", fn).put("tn", tn).put("errors", errors)
            .put("input_sha256", java.security.MessageDigest.getInstance("SHA-256").digest(input.readBytes()).joinToString("") { "%02x".format(it) })
            .put("recall", tp.toDouble() / (tp + fn).coerceAtLeast(1))
            .put("false_positive_rate", fp.toDouble() / (fp + tn).coerceAtLeast(1))
            .put("duration_ms_p50", durations.getOrNull(durations.size / 2))
            .put("duration_ms_p95", durations.getOrNull((durations.size * .95).toInt()))
            .put("suite_elapsed_ms", android.os.SystemClock.elapsedRealtime() - started).put("cases", cases)
        File(context.filesDir, "fraud-observed-pipeline-results.json").writeText(report.toString(2))
        assertEquals("Every observed row completed", 0, errors)
        assertEquals(847, cases.length())
    }
}
