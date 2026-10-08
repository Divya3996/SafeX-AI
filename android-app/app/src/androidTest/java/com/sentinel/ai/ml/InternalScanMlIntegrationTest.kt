package com.sentinel.ai.ml

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.protection.intent.IntentScanRepository
import com.sentinel.ai.protection.intent.IntentThreatAnalyzer
import com.sentinel.ai.protection.intent.model.IntentPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InternalScanMlIntegrationTest {

    @Test
    fun builtInScanRepositoryRunsNativeMlInference() = runBlocking {
        val baseAnalyzer = object : IntentThreatAnalyzer {
            override suspend fun analyze(payload: IntentPayload) = ScanResult(
                id = "ml-instrumentation-test",
                source = "test",
                riskLevel = RiskLevel.GREEN,
                riskScore = 0f,
                explanation = "Base analyzer result",
                timestamp = 0L
            )
        }
        val repository = IntentScanRepository(
            analyzer = baseAnalyzer,
            context = ApplicationProvider.getApplicationContext()
        )

        val result = repository.scanLink("https://login.example.com/a1")

        assertTrue("Expected native inference, with no fallback", result.modelStatus == "On-device URL model")
    }
}
