package com.sentinel.ai.protection.intent

import com.sentinel.ai.core.model.*
import com.sentinel.ai.protection.intent.model.IntentPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PracticalReviewPipelineTest {
    private val repository = IntentScanRepository(object : IntentThreatAnalyzer {
        override suspend fun analyze(payload: IntentPayload) = ScanResult("review-test", "Link", riskLevel = RiskLevel.GREEN,
            riskScore = 0f, explanation = "Base", timestamp = 0L)
    }, mlPredictor = { 0f })
    @Test fun domainExplanationUsesActualDomainRatherThanBrandInUserInfo() = runBlocking {
        val facts = repository.scanLink("https://paypal.com@sub.attacker.example/login").linkInspections!!.single()
        assertEquals("attacker.example", facts.registrableDomain)
        assertEquals("sub.attacker.example", facts.host)
        assertTrue(facts.hasUserInfo)
    }
    @Test fun privateHostingSuffixKeepsTenantAsWebsiteDomain() = runBlocking {
        val facts = repository.scanLink("https://my-project.pages.dev/").linkInspections!!.single()
        assertEquals("my-project.pages.dev", facts.registrableDomain)
    }
    @Test fun embeddedEncodedDestinationsAreExplainedWithoutFollowingThem() = runBlocking {
        val facts = repository.scanLink("https://example.com/?redirect=https%3A%2F%2Fother.example%2Flogin").linkInspections!!.single()
        assertEquals(listOf("other.example"), facts.embeddedHosts)
    }
    @Test fun liveWebQrUsesLinkPolicyAndUpiCannotBeOpenedAsWebLink() = runBlocking {
        val web = repository.scanQrContent("https://example.com/")
        assertEquals("link", web.contentType)
        assertEquals("Live QR scan", web.source)
        assertTrue(DecisionPolicy.canOpen(web))
        val payment = repository.scanQrContent("upi://pay?pa=shop@bank&pn=Unverified&am=50")
        assertEquals("qr", payment.contentType)
        assertEquals(ProtectionDecision.WARN, payment.decision)
        assertEquals("shop@bank", payment.paymentReviews!!.single().address)
        assertFalse(DecisionPolicy.canOpen(payment))
    }
    @Test fun textWithMixedLinksAndPaymentCodesRetainsAllReviewFacts() = runBlocking {
        val result = repository.scanText("Visit https://example.com and inspect upi://pay?pa=shop@bank&am=50")
        assertEquals(1, result.linkInspections!!.size)
        assertEquals(1, result.paymentReviews!!.size)
    }
    @Test fun oversizedQrContentIsRejectedInsteadOfTruncated() = runBlocking {
        try { repository.scanQrContent("a".repeat(12001)); fail("Oversized QR accepted") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun malformedUpiCodeAddsExplicitCaution() = runBlocking {
        val result = repository.scanQrContent("upi://pay?pa=first@bank&pa=other@bank")
        assertEquals(ProtectionDecision.WARN, result.decision)
        assertFalse(result.paymentReviews!!.single().isInspectable)
        assertTrue(result.reasons.any { it.message.contains("reliably") })
    }
}
