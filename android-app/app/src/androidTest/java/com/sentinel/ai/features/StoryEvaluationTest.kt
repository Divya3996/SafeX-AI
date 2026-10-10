package com.sentinel.ai.features

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.story.*
import com.sentinel.ai.ml.PipelineEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class StoryEvaluationTest {
    @Test fun twentyAuthoredStoriesComparePrivateItemChecksWithEvidenceGroundedSequences() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val fixture = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("story-corpus.json").bufferedReader().use { it.readText() })
        val repository = EntryPointAccessors.fromApplication(context, PipelineEntryPoint::class.java).repository()
        val cases = fixture.getJSONArray("cases"); val results = JSONArray(); val violations = mutableListOf<String>()
        val dao = com.sentinel.ai.core.data.local.SentinelDatabase.getInstance(context).threatDao()
        val records = dao.getAllThreatRecords()
        val persistedScans = records.filter { it.recordType == com.sentinel.ai.core.data.local.ThreatRecordEntity.TYPE_SCAN_RESULT }.map { it.id }.toSet()
        withTimeout(15000) { ThreatJournal.scanResults.first { scans -> scans.map { it.id }.toSet() == persistedScans } }
        val before = ThreatJournal.scanResults.value.map { it.id }.toSet()
        for (index in 0 until cases.length()) {
            val input = cases.getJSONObject(index); val raw = input.getJSONArray("items"); val baseline = JSONArray()
            val start = System.nanoTime()
            val items = (0 until raw.length()).map { position ->
                val text = raw.getString(position)
                val result = if (text.startsWith("upi://")) repository.analyzeQrPrivately(text) else repository.analyzeTextPrivately(text)
                baseline.put(JSONObject().put("decision", result.decision.name).put("coverage", result.coverageDetails?.assessment?.name ?: "COMPLETE"))
                StoryItem(source = if (text.startsWith("upi://")) StorySource.QR else StorySource.MESSAGE, text = text, result = result)
            }
            val correlationStart = System.nanoTime()
            val analysis = StoryAnalyzer.analyze(StoryCase(items = items))
            val correlationMicros = (System.nanoTime() - correlationStart) / 1000
            val patterns = analysis.findings.filterNot { it.id.startsWith("item_") }
            val expected = input.getString("pattern")
            if (expected.isEmpty() && patterns.isNotEmpty() || expected.isNotEmpty() && patterns.none { it.id == expected && it.concern.name == input.getString("concern") }) violations += input.getString("id")
            analysis.findings.flatMap { it.evidence }.forEach { signal ->
                assertTrue(items.any { it.id == signal.itemId && signal.start >= 0 && signal.end <= it.text.length && signal.end > signal.start })
            }
            results.put(JSONObject().put("id", input.getString("id")).put("language", input.getString("language"))
                .put("label", input.getString("label")).put("expected_pattern", expected).put("item_checks", baseline)
                .put("story_patterns", JSONArray(patterns.map { it.id })).put("overall_concern", analysis.concern.name)
                .put("correlation_us", correlationMicros).put("total_ms", (System.nanoTime() - start) / 1000000))
        }
        val report = JSONObject().put("kind", fixture.getString("kind")).put("rule_version", StoryAnalyzer.VERSION)
            .put("device", "${android.os.Build.MODEL}; API ${android.os.Build.VERSION.SDK_INT}").put("results", results).put("contract_mismatches", JSONArray(violations))
        java.io.File(context.filesDir, "story-evaluation.json").writeText(report.toString(2))
        assertEquals(before, ThreatJournal.scanResults.value.map { it.id }.toSet())
        assertEquals(records.map { it.id }.toSet(), dao.getAllThreatRecords().map { it.id }.toSet())
        assertTrue("Authored story contract mismatches: $violations", violations.isEmpty())
    }
}
