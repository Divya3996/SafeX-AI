package com.sentinel.ai.core.model

data class ContextAnswers(
    val unexpected: Boolean = false,
    val asksForSecret: Boolean = false,
    val asksForPayment: Boolean = false,
    val asksToInstall: Boolean = false,
    val pressuresYou: Boolean = false,
    val recipientMismatch: Boolean = false,
    val amountMismatch: Boolean = false
)

/** User context can raise caution, but never erase detector evidence or independently block. */
object ContextReview {
    fun apply(base: ScanResult, answers: ContextAnswers): ScanResult {
        val messages = buildList {
            if (answers.unexpected) add("You were not expecting this communication. Verify the sender independently.")
            if (answers.asksForSecret) add("You reported a request for an OTP, PIN or password. Do not share these secrets.")
            if (answers.asksForPayment) add("You reported a payment request. Confirm its purpose and recipient independently.")
            if (answers.asksToInstall) add("You reported an app installation or remote-access request. Verify it before proceeding.")
            if (answers.pressuresYou) add("You reported pressure to act quickly. Pause and verify the request.")
            if (answers.recipientMismatch) add("The QR payment address does not match the recipient you expected.")
            if (answers.amountMismatch) add("The QR payment amount is missing or does not match the amount you expected.")
        }
        val contextScore = when {
            answers.asksForSecret || answers.recipientMismatch || answers.amountMismatch -> 70f
            answers.asksToInstall && (answers.unexpected || answers.pressuresYou) -> 70f
            answers.asksToInstall || answers.asksForPayment || answers.unexpected || answers.pressuresYou -> 30f
            else -> 0f
        }
        val score = maxOf(base.riskScore, DecisionPolicy.floor(base.riskLevel), contextScore)
        val level = DecisionPolicy.level(score)
        val decision = if (base.decision == ProtectionDecision.BLOCK) ProtectionDecision.BLOCK else level.toProtectionDecision()
        val reasons = (base.reasons + messages.map {
            ScanReason(ScanReasonSource.LOCAL_HEURISTIC, "user_context", it, level)
        }).distinctBy { it.sourceName to it.message }
        val summary = if (decision != base.decision) "Your answers add risk signals. Verify this request before acting." else base.summary
        return base.copy(riskScore = score, riskLevel = level, decision = decision,
            headline = decision.defaultHeadline(), recommendedAction = decision.defaultAction(),
            summary = summary, explanation = (listOf(summary) + reasons.map { it.message }).distinct().joinToString("\n"),
            reasons = reasons, contextReview = answers,
            guidance = if (contextScore > 0) "Pause. Contact the person or organization using a number or app you already trust." else base.guidance)
    }
}
