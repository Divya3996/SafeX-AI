package com.sentinel.ai.core.story

import com.sentinel.ai.core.model.*
import org.junit.Assert.*
import org.junit.Test

class StoryAnalyzerTest {
    private fun item(text: String, decision: ProtectionDecision = ProtectionDecision.ALLOW, level: RiskLevel = RiskLevel.GREEN) =
        StoryItem(source = StorySource.MESSAGE, text = text, result = ScanResult(id = "fixture", source = "Authored test", riskLevel = level,
            riskScore = 0f, explanation = "Authored baseline", timestamp = 0, decision = decision, target = text))
    private fun story(vararg text: String) = StoryCase(items = text.map { item(it) })
    private fun ids(case: StoryCase) = StoryAnalyzer.analyze(case).findings.map { it.id }
    @Test fun taskExamplesMatchInAllThreeScriptsWithExactEvidence() {
        for (language in listOf("en","hi","gu")) {
            val case = StoryCase(items = StoryExamples.task(language).map { item(it) })
            val finding = StoryAnalyzer.analyze(case).findings.single { it.id == "task_fee" }
            assertEquals(StoryConcern.HIGH, finding.concern)
            assertEquals(2, finding.evidence.map { it.itemId }.distinct().size)
            finding.evidence.forEach { signal ->
                val raw = case.items.single { it.id == signal.itemId }.text
                assertTrue(signal.start >= 0 && signal.end <= raw.length && signal.end > signal.start)
                assertTrue(raw.substring(signal.start, signal.end).isNotBlank())
            }
        }
    }
    @Test fun ordinaryFirstStageDoesNotBecomeVerifiedSafe() {
        assertEquals(StoryConcern.CONTEXT, StoryAnalyzer.analyze(story("Would you like details about evening work?")).concern)
    }
    @Test fun singleItemDoesNotFabricateASequence() {
        assertFalse(ids(story("Job earnings: deposit ₹500 to unlock them.")).contains("task_fee"))
    }
    @Test fun reversedStagesDoNotInventEarlierPromises() {
        assertFalse(ids(story("Deposit ₹500 to unlock funds.", "Your work starts Tuesday.")).contains("task_fee"))
    }
    @Test fun removalWithdrawsLinkedFindings() {
        val case = story("Your work commission is ready.", "Pay ₹500 to unlock your earnings.")
        assertTrue("task_fee" in ids(case))
        assertFalse("task_fee" in ids(case.copy(items = case.items.take(1))))
    }
    @Test fun correctionsWithdrawOldEvidence() {
        val case = story("Your work commission is ready.", "Pay ₹500 to unlock your earnings.")
        assertFalse("task_fee" in ids(case.copy(items = listOf(case.items[0], case.items[1].copy(text = "Your salary will arrive tomorrow.")))))
    }
    @Test fun duplicatedEvidenceDoesNotCreateNewStages() {
        val first = item("Your job begins Tuesday.")
        assertTrue(StoryAnalyzer.analyze(StoryCase(items = listOf(first, first.copy(id = java.util.UUID.randomUUID().toString())))).findings.isEmpty())
    }
    @Test fun normalizationDeduplicatesWhitespaceAndUnicode() {
        assertEquals(StoryAnalyzer.fingerprint("Work  details\n"), StoryAnalyzer.fingerprint("Ｗｏｒｋ details"))
    }
    @Test fun safetyAdviceIsNotASecretOrFeeRequestInAnyLanguage() {
        for (language in listOf("en","hi","gu")) {
            val findings = ids(StoryCase(items = StoryExamples.legitimate(language).map { item(it) }))
            assertTrue(findings.joinToString(), findings.isEmpty())
        }
    }
    @Test fun bankThreatAndSecretMatchAcrossEnglishItems() {
        assertTrue("authority_pressure" in ids(story("This is bank support.", "Your account will be frozen.", "Share your OTP with us.")))
    }
    @Test fun bankThreatAndSecretMatchAcrossHindiItems() {
        assertTrue("authority_pressure" in ids(story("हम बैंक से बोल रहे हैं।", "आपका खाता बंद हो जाएगा।", "अपना ओटीपी हमें बताएं।")))
    }
    @Test fun bankThreatAndSecretMatchAcrossGujaratiItems() {
        assertTrue("authority_pressure" in ids(story("અમે બેંકમાંથી બોલીએ છીએ.", "તમારું ખાતું બંધ થઈ જશે.", "તમારો ઓટીપી અમને આપો.")))
    }
    @Test fun bankInformationWithoutThreatDoesNotBecomeImpersonation() {
        assertFalse("authority_pressure" in ids(story("Bank support hours are 10 to 5.", "Your statement is ready.")))
    }
    @Test fun protectiveBankAdviceDoesNotRequestSecrets() {
        assertFalse("authority_pressure" in ids(story("Bank account frozen warning.", "Never share your OTP with callers.")))
    }
    @Test fun supportRemoteAndSecretMatchAcrossEnglishItems() {
        assertTrue("support_remote" in ids(story("This is technical support.", "Install AnyDesk and grant remote access.", "Share your bank OTP.")))
    }
    @Test fun supportRemoteAndSecretMatchAcrossHindiItems() {
        assertTrue("support_remote" in ids(story("यह तकनीकी सपोर्ट है।", "AnyDesk इंस्टॉल करें।", "अपना ओटीपी बताएं।")))
    }
    @Test fun supportRemoteAndSecretMatchAcrossGujaratiItems() {
        assertTrue("support_remote" in ids(story("આ ટેક્નિકલ સપોર્ટ છે.", "AnyDesk ઇન્સ્ટોલ કરો.", "તમારો ઓટીપી આપો.")))
    }
    @Test fun remoteSoftwareMentionDoesNotMeanAccessWasRequested() {
        assertFalse("support_remote" in ids(story("Technical support documentation.", "Our software supports TeamViewer.", "The annual fee is ₹500.")))
    }
    @Test fun paidSupportAloneIsReviewRatherThanCertainFraud() {
        val result = StoryAnalyzer.analyze(story("Technical support appointment.", "Install TeamViewer.", "Pay the service fee."))
        assertEquals(StoryConcern.REVIEW, result.concern)
    }
    @Test fun laterSecretRequestCannotBeHiddenByAnEarlierSupportPayment() {
        assertEquals(StoryConcern.HIGH, StoryAnalyzer.analyze(story("Technical support appointment.",
            "Install TeamViewer and pay the fee.", "Share your bank OTP.")).concern)
    }
    @Test fun normalPaymentBeforeThreatDoesNotHideALaterCoerciveSecretRequest() {
        assertTrue("authority_pressure" in ids(story("Bank support appointment.", "Pay the service invoice.",
            "Your account will be frozen.", "Share your OTP immediately.")))
    }
    @Test fun ordinaryQrPaymentDoesNotCreateAStoryPattern() {
        assertTrue(ids(story("upi://pay?pa=shop@invalid&am=500&cu=INR")).isEmpty())
    }
    @Test fun changedRecipientsAreCautionAndTheirEvidenceActuallyDiffers() {
        val case = story("upi://pay?pa=one@invalid&am=100", "upi://pay?pa=one@invalid&am=200", "upi://pay?pa=two@invalid&am=100")
        val finding = StoryAnalyzer.analyze(case).findings.single()
        assertEquals(StoryConcern.REVIEW, finding.concern)
        assertEquals(listOf(case.items[0].id,case.items[2].id), finding.evidence.map { it.itemId })
    }
    @Test fun matchingRecipientsDoNotVerifyOwnership() {
        assertTrue(ids(story("upi://pay?pa=one@invalid&am=100", "upi://pay?pa=one@invalid&am=200")).isEmpty())
    }
    @Test fun educationalInvoiceDoesNotBecomeJobFee() {
        assertFalse("task_fee" in ids(story("Your work-study timetable is ready.", "Pay your college tuition invoice to release enrollment.")))
    }
    @Test fun quotedExamplesRemainEducational() {
        assertFalse("task_fee" in ids(story("Your job starts soon.", "Example: Pay ₹500 to unlock earnings.")))
    }
    @Test fun detectorWarningsArePreservedWithoutSequence() {
        val case = StoryCase(items = listOf(item("Suspicious link", ProtectionDecision.BLOCK, RiskLevel.CRITICAL)))
        val result = StoryAnalyzer.analyze(case)
        assertEquals(StoryConcern.HIGH, result.concern)
        assertTrue(result.findings.single().id.startsWith("item_"))
    }
    @Test fun sequenceDeduplicationDoesNotHideAStrongerIndividualUnicodeWarning() {
        val case = StoryCase(items = listOf(item("https://example.com"), item("https://ｅｘａｍｐｌｅ.com", ProtectionDecision.BLOCK, RiskLevel.RED)))
        assertEquals(StoryConcern.HIGH, StoryAnalyzer.analyze(case).concern)
        assertEquals(case.items.last().id, StoryAnalyzer.analyze(case).findings.single().evidence.single().itemId)
    }
    @Test fun missingCoverageIsUnknownRatherThanSafe() {
        val case = StoryCase(items = listOf(StoryItem(source = StorySource.IMAGE, text = "partial", analysisFailed = true)))
        assertTrue(StoryAnalyzer.analyze(case).incomplete)
    }
    @Test fun deduplicationCannotHideMissingCoverage() {
        val case = StoryCase(items = listOf(item("Example text"), StoryItem(source = StorySource.IMAGE, text = "Ｅｘａｍｐｌｅ text", analysisFailed = true)))
        assertTrue(StoryAnalyzer.analyze(case).incomplete)
    }
    @Test fun secretFreeExportDoesNotContainRawValuesOrCaseTitle() {
        val case = story("PrivateName phone 9999988888 password SecretXYZ https://private.example/?token=SecretXYZ").copy(title = "PrivateName")
        val export = StoryExport.lines(case, StoryAnalyzer.analyze(case)).joinToString("\n")
        for (value in listOf("PrivateName","9999988888","SecretXYZ","private.example",case.id)) assertFalse(value, export.contains(value))
    }
    @Test fun limitsRejectInvalidIdentityAndTooMuchEvidence() {
        assertThrows(IllegalArgumentException::class.java) { StoryLimits.validate(story("example").copy(id = "../outside")) }
        assertThrows(IllegalArgumentException::class.java) { StoryLimits.validate(StoryCase(items = (1..9).map { item("Item $it") })) }
    }
}
