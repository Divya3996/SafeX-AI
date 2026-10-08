package com.sentinel.ai.protection.intent.reputation

import com.sentinel.ai.core.coroutines.DispatcherProvider
import com.sentinel.ai.core.model.ScanResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.jvm.JvmSuppressWildcards

@Singleton
class ReputationManagerImpl @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards ReputationProvider>,
    private val combiner: EvidenceCombiner,
    private val dispatcherProvider: DispatcherProvider,
    private val config: ReputationConfig
) : ReputationManager {

    override suspend fun enrich(
        heuristicResult: ScanResult,
        target: ReputationTarget?
    ): ScanResult {
        if (target == null) {
            return heuristicResult
        }

        val supportedProviders = providers
            .filter { it.supports(target) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.providerName })

        val evidence = if (supportedProviders.isEmpty()) {
            emptyList()
        } else {
            coroutineScope {
                supportedProviders
                .map { provider ->
                    async(dispatcherProvider.io) {
                        evaluateProvider(provider, target)
                    }
                }
                .awaitAll()
            }
        }

        return combiner.combine(heuristicResult, evidence)
    }

    private suspend fun evaluateProvider(
        provider: ReputationProvider,
        target: ReputationTarget
    ): ReputationEvidence {
        return try {
            val completed = withTimeoutOrNull(config.lookupTimeoutMs) {
                CompletedProviderCall(provider.evaluate(target))
            } ?: return ReputationEvidence.timedOut(provider.providerName)

            completed.result
                ?.let(ReputationEvidence::completed)
                ?: ReputationEvidence.unavailable(provider.providerName)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            ReputationEvidence.failed(provider.providerName)
        }
    }
}

private data class CompletedProviderCall(
    val result: ReputationResult?
)
