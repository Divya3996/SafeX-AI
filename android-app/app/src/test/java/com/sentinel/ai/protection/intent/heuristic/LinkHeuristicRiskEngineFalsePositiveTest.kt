package com.sentinel.ai.protection.intent.heuristic

import com.sentinel.ai.core.model.RiskLevel
import org.junit.Test

class LinkHeuristicRiskEngineFalsePositiveTest {

    private val engine = LinkHeuristicRiskEngine()

    @Test
    fun `example dot com remains zero score`() {
        assertLinkAnalysis(engine, "https://example.com", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `www example dot com remains zero score`() {
        assertLinkAnalysis(engine, "https://www.example.com", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `GitHub home remains zero score`() {
        assertLinkAnalysis(engine, "https://github.com", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `GitHub login currently receives discounted path keyword score`() {
        assertLinkAnalysis(
            engine,
            "https://github.com/login",
            2f,
            RiskLevel.GREEN,
            "social_engineering" to expectedRule(
                2f,
                "Uses social engineering keyword in path/query: login",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }

    @Test
    fun `Google accounts host currently receives full account keyword score`() {
        assertLinkAnalysis(
            engine,
            "https://accounts.google.com",
            20f,
            RiskLevel.GREEN,
            "social_engineering" to expectedRule(
                20f,
                "Uses social engineering keyword in domain: account",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }

    @Test
    fun `Microsoft support remains zero score`() {
        assertLinkAnalysis(engine, "https://support.microsoft.com", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `Android developer home remains zero score`() {
        assertLinkAnalysis(engine, "https://developer.android.com", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `ServiceLogin cannot shadow account host evidence`() {
        assertLinkAnalysis(
            engine,
            "https://accounts.google.com/ServiceLogin",
            20f,
            RiskLevel.GREEN,
            "social_engineering" to expectedRule(
                20f,
                "Uses social engineering keyword in domain: account",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }

    @Test
    fun `support account recovery path currently receives two points`() {
        assertLinkAnalysis(
            engine,
            "https://support.example.com/account-recovery",
            2f,
            RiskLevel.GREEN,
            "social_engineering" to expectedRule(
                2f,
                "Uses social engineering keyword in path/query: account",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }

    @Test
    fun `bank account path currently receives two points`() {
        assertLinkAnalysis(
            engine,
            "https://bank.example.com/account",
            2f,
            RiskLevel.GREEN,
            "social_engineering" to expectedRule(
                2f,
                "Uses social engineering keyword in path/query: account",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }

    @Test
    fun `Android Studio page remains zero score`() {
        assertLinkAnalysis(engine, "https://developer.android.com/studio", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `zip download URL remains zero because link engine has no extension rule`() {
        assertLinkAnalysis(engine, "https://example.com/download/app.zip", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `trusted domain text before another domain triggers deception`() {
        assertLinkAnalysis(engine, "https://paypal.com.example.org", 30f, RiskLevel.YELLOW,
            "brand_impersonation" to expectedRule(30f, "Trusted domain text is placed before a different actual domain", RuleCategory.BRAND_IMPERSONATION))
    }

    @Test
    fun `brand substring no longer triggers while existing OTP keyword behavior remains characterized`() {
        assertLinkAnalysis(
            engine,
            "https://notpaypal.com",
            20f,
            RiskLevel.GREEN,
            "social_engineering" to expectedRule(
                20f,
                "Uses social engineering keyword in domain: otp",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }

    @Test
    fun `Google domain text before another domain triggers deception`() {
        assertLinkAnalysis(engine, "https://google.com.example.net", 30f, RiskLevel.YELLOW,
            "brand_impersonation" to expectedRule(30f, "Trusted domain text is placed before a different actual domain", RuleCategory.BRAND_IMPERSONATION))
    }

    @Test
    fun `secure hyphenated benign domain currently receives full keyword score`() {
        assertLinkAnalysis(
            engine,
            "https://secure-example.com",
            20f,
            RiskLevel.GREEN,
            "social_engineering" to expectedRule(
                20f,
                "Uses social engineering keyword in domain: secure",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }

    @Test
    fun `official PayPal domain remains zero score`() {
        assertLinkAnalysis(engine, "https://paypal.com", 0f, RiskLevel.GREEN)
    }

    @Test
    fun `official PayPal subdomain avoids brand rule but login keyword still scores`() {
        assertLinkAnalysis(
            engine,
            "https://login.paypal.com",
            20f,
            RiskLevel.GREEN,
            "social_engineering" to expectedRule(
                20f,
                "Uses social engineering keyword in domain: login",
                RuleCategory.SOCIAL_ENGINEERING
            )
        )
    }
}
