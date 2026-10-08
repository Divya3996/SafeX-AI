package com.sentinel.ai.protection.intent.reputation

import com.sentinel.ai.core.coroutines.DispatcherProvider
import com.sentinel.ai.core.model.EvidenceSourceStatus
import com.sentinel.ai.core.model.ProtectionDecision
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ReputationManagerImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockDispatcherProvider = object : DispatcherProvider() {
        override val io: CoroutineDispatcher = testDispatcher
        override val main: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    private val combiner = EvidenceCombiner()
    private val baseResult = ScanResult(
        id = "test",
        source = "test",
        riskLevel = RiskLevel.GREEN,
        riskScore = 0f,
        explanation = "Clean.",
        timestamp = System.currentTimeMillis()
    )

    @Test
    fun returnsHeuristicResultIfTargetIsNull() = runTest(testDispatcher) {
        val manager = ReputationManagerImpl(
            providers = setOf(object : ReputationProvider {
                override val providerName = "FailProvider"
                override suspend fun evaluate(target: ReputationTarget) = error("Should not be called")
            }),
            combiner = combiner,
            dispatcherProvider = mockDispatcherProvider,
            config = ReputationConfig("", "", 5000L)
        )

        val result = manager.enrich(baseResult, null)
        assertEquals(baseResult, result)
    }

    @Test
    fun returnsHeuristicResultIfNoProvidersRegistered() = runTest(testDispatcher) {
        val manager = ReputationManagerImpl(
            providers = emptySet(),
            combiner = combiner,
            dispatcherProvider = mockDispatcherProvider,
            config = ReputationConfig("", "", 5000L)
        )

        val result = manager.enrich(baseResult, ReputationTarget.Url("https://malicious.com"))
        assertEquals(ProtectionDecision.ALLOW, result.decision)
        assertTrue(result.providerFindings.isEmpty())
        assertTrue(result.summary.contains("no online reputation result"))
    }

    @Test
    fun queriesActiveProvidersAndCombinesResults() = runTest(testDispatcher) {
        val provider = object : ReputationProvider {
            override val providerName = "ProviderA"
            override suspend fun evaluate(target: ReputationTarget): ReputationResult {
                return ReputationResult("ProviderA", 0.9f, ReputationVerdict.MALICIOUS, "Bad", 0L)
            }
        }

        val manager = ReputationManagerImpl(
            providers = setOf(provider),
            combiner = combiner,
            dispatcherProvider = mockDispatcherProvider,
            config = ReputationConfig("", "", 5000L)
        )

        val result = manager.enrich(baseResult, ReputationTarget.Url("https://malicious.com"))
        assertTrue(result.riskScore > 0f)
        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertEquals("ProviderA", result.providerFindings.single().providerName)
        assertEquals("MALICIOUS", result.providerFindings.single().verdict)
    }

    @Test
    fun ignoresProviderExceptionAndGracefullyFallsBack() = runTest(testDispatcher) {
        val buggyProvider = object : ReputationProvider {
            override val providerName = "BuggyProvider"
            override suspend fun evaluate(target: ReputationTarget): ReputationResult {
                throw IOException("Network error")
            }
        }
        val goodProvider = object : ReputationProvider {
            override val providerName = "GoodProvider"
            override suspend fun evaluate(target: ReputationTarget): ReputationResult {
                return ReputationResult("GoodProvider", 0.9f, ReputationVerdict.MALICIOUS, "Bad", 0L)
            }
        }

        val manager = ReputationManagerImpl(
            providers = setOf(buggyProvider, goodProvider),
            combiner = combiner,
            dispatcherProvider = mockDispatcherProvider,
            config = ReputationConfig("", "", 5000L)
        )

        val result = manager.enrich(baseResult, ReputationTarget.Url("https://malicious.com"))
        // The buggy provider fails but the good provider result is still integrated successfully
        assertTrue(result.riskScore > 0f)
        assertEquals(ProtectionDecision.BLOCK, result.decision)
        assertEquals(
            EvidenceSourceStatus.FAILED,
            result.providerFindings.single { it.providerName == "BuggyProvider" }.status
        )
        assertEquals(
            "MALICIOUS",
            result.providerFindings.single { it.providerName == "GoodProvider" }.verdict
        )
    }

    @Test
    fun respectsTimeoutAndExcludesSlowProviders() = runTest(testDispatcher) {
        val slowProvider = object : ReputationProvider {
            override val providerName = "SlowProvider"
            override suspend fun evaluate(target: ReputationTarget): ReputationResult {
                delay(2000L)
                return ReputationResult("SlowProvider", 0.9f, ReputationVerdict.MALICIOUS, "Bad", 0L)
            }
        }
        val fastProvider = object : ReputationProvider {
            override val providerName = "FastProvider"
            override suspend fun evaluate(target: ReputationTarget): ReputationResult {
                return ReputationResult("FastProvider", 0.8f, ReputationVerdict.SUSPICIOUS, "Suspect", 0L)
            }
        }

        val manager = ReputationManagerImpl(
            providers = setOf(slowProvider, fastProvider),
            combiner = combiner,
            dispatcherProvider = mockDispatcherProvider,
            config = ReputationConfig("", "", 500L) // 500ms timeout
        )

        // Run the task and advance the clock
        val resultDeferred = async {
            manager.enrich(baseResult, ReputationTarget.Url("https://malicious.com"))
        }

        // Fast forward time by 600ms (so timeout is hit)
        testDispatcher.scheduler.advanceTimeBy(600L)
        testDispatcher.scheduler.runCurrent()

        val result = resultDeferred.await()

        assertEquals(ProtectionDecision.WARN, result.decision)
        assertEquals(
            EvidenceSourceStatus.TIMED_OUT,
            result.providerFindings.single { it.providerName == "SlowProvider" }.status
        )
        assertEquals(
            "SUSPICIOUS",
            result.providerFindings.single { it.providerName == "FastProvider" }.verdict
        )
    }

    @Test
    fun recordsNullProviderResultAsUnavailable() = runTest(testDispatcher) {
        val provider = object : ReputationProvider {
            override val providerName = "UnavailableProvider"
            override suspend fun evaluate(target: ReputationTarget): ReputationResult? = null
        }
        val manager = manager(setOf(provider))

        val result = manager.enrich(baseResult, ReputationTarget.Url("https://example.com"))

        assertEquals(ProtectionDecision.ALLOW, result.decision)
        assertEquals(EvidenceSourceStatus.UNAVAILABLE, result.providerFindings.single().status)
        assertEquals("Limited online verification. Proceed with caution.", result.summary)
    }

    @Test
    fun recordsExplicitUnknownSeparatelyFromProviderFailure() = runTest(testDispatcher) {
        val provider = object : ReputationProvider {
            override val providerName = "UnknownProvider"
            override suspend fun evaluate(target: ReputationTarget) = ReputationResult(
                providerName = providerName,
                confidence = 0f,
                reputation = ReputationVerdict.UNKNOWN,
                reason = "No conclusive verdict.",
                timestamp = 1L
            )
        }
        val manager = manager(setOf(provider))

        val result = manager.enrich(baseResult, ReputationTarget.Url("https://example.com"))

        assertEquals(EvidenceSourceStatus.UNKNOWN, result.providerFindings.single().status)
        assertEquals("UNKNOWN", result.providerFindings.single().verdict)
    }

    @Test
    fun externalCancellationIsNotSwallowed() = runTest(testDispatcher) {
        val provider = object : ReputationProvider {
            override val providerName = "CancellableProvider"
            override suspend fun evaluate(target: ReputationTarget): ReputationResult? {
                awaitCancellation()
            }
        }
        val manager = manager(setOf(provider), timeoutMs = 60_000L)
        val result = async {
            manager.enrich(baseResult, ReputationTarget.Url("https://example.com"))
        }
        testDispatcher.scheduler.runCurrent()

        result.cancel()
        testDispatcher.scheduler.runCurrent()

        assertTrue(result.isCancelled)
    }

    private fun manager(
        providers: Set<ReputationProvider>,
        timeoutMs: Long = 5_000L
    ) = ReputationManagerImpl(
        providers = providers,
        combiner = combiner,
        dispatcherProvider = mockDispatcherProvider,
        config = ReputationConfig("", "", timeoutMs)
    )
}
