package com.sentinel.ai.protection.intent.reputation

import com.sentinel.ai.core.model.EvidenceSourceStatus

data class ReputationEvidence(
    val providerName: String,
    val status: EvidenceSourceStatus,
    val result: ReputationResult? = null,
    val statusReason: String
) {
    companion object {
        fun completed(result: ReputationResult): ReputationEvidence {
            val status = if (result.reputation == ReputationVerdict.UNKNOWN) {
                EvidenceSourceStatus.UNKNOWN
            } else {
                EvidenceSourceStatus.COMPLETED
            }
            return ReputationEvidence(
                providerName = result.providerName,
                status = status,
                result = result,
                statusReason = result.reason
            )
        }

        fun unavailable(providerName: String): ReputationEvidence = ReputationEvidence(
            providerName = providerName,
            status = EvidenceSourceStatus.UNAVAILABLE,
            statusReason = "Provider returned no result."
        )

        fun failed(providerName: String): ReputationEvidence = ReputationEvidence(
            providerName = providerName,
            status = EvidenceSourceStatus.FAILED,
            statusReason = "Provider lookup failed."
        )

        fun timedOut(providerName: String): ReputationEvidence = ReputationEvidence(
            providerName = providerName,
            status = EvidenceSourceStatus.TIMED_OUT,
            statusReason = "Provider lookup timed out."
        )
    }
}
