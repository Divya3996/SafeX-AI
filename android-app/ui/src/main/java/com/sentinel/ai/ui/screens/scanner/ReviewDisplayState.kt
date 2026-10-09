package com.sentinel.ai.ui.screens.scanner

import com.sentinel.ai.core.model.*

/** Older Room snapshots cannot roll back a context warning already shown to the user. */
object ReviewDisplayState {
    fun reconcile(prior: ScanResult, incoming: ScanResult): ScanResult {
        if (prior.id != incoming.id) return incoming
        val priorStrength = maxOf(prior.riskScore, DecisionPolicy.floor(prior.riskLevel))
        val incomingStrength = maxOf(incoming.riskScore, DecisionPolicy.floor(incoming.riskLevel))
        val keepPrior = prior.decision.ordinal > incoming.decision.ordinal ||
            (prior.decision == incoming.decision && priorStrength > incomingStrength)
        val score = maxOf(priorStrength, incomingStrength)
        val baseline = (if (keepPrior) prior else incoming).copy(
            riskScore = score, riskLevel = DecisionPolicy.level(score),
            reasons = (prior.reasons + incoming.reasons).distinctBy { it.sourceName to it.message })
        if (prior.contextReview == null && incoming.contextReview == null) return baseline
        val a = prior.contextReview ?: ContextAnswers()
        val b = incoming.contextReview ?: ContextAnswers()
        return ContextReview.apply(baseline, ContextAnswers(
            unexpected = a.unexpected || b.unexpected, asksForSecret = a.asksForSecret || b.asksForSecret,
            asksForPayment = a.asksForPayment || b.asksForPayment, asksToInstall = a.asksToInstall || b.asksToInstall,
            pressuresYou = a.pressuresYou || b.pressuresYou, recipientMismatch = a.recipientMismatch || b.recipientMismatch,
            amountMismatch = a.amountMismatch || b.amountMismatch))
    }
}
