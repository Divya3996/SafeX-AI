package com.sentinel.ai.core.model

import org.junit.Assert.*
import org.junit.Test

class TextWarningCoverageTest {
    private val vocabulary = listOf("your", "mobile", "claim", "reward", "today")
    private fun model(version: String? = TextModelFeatures.VERSION) = OfflineTextClassifier(vocabulary, List(vocabulary.size) { 1.0 }, -1.0, .545f, version)

    @Test fun familiarWordsQualifyWithoutAnExplicitActionVerb() {
        assertTrue(model().supportsContextFreeWarning("Your mobile reward arrived today"))
    }
    @Test fun identifiersAndRepeatedWordsCannotCreateCoverage() {
        assertFalse(model().supportsContextFreeWarning("claim claim claim claim 123456 https://example.com person@example.com"))
        assertFalse(model().supportsContextFreeWarning("opaque unfamiliar madeup unrelated"))
    }
    @Test fun nativeScriptsKeepTheirExistingContextPath() {
        assertFalse(model().supportsContextFreeWarning("તમારી બેંકનો ગુપ્ત કોડ મોકલો"))
        assertFalse(model().supportsContextFreeWarning("अपना बैंक पासवर्ड तुरंत भेजें"))
        assertFalse(model().supportsContextFreeWarning("your mobile claim reward અજાણી માહિતી"))
    }
    @Test fun legacyPrototypeCannotWarnThroughTheResearchPath() {
        assertFalse(model(null).supportsContextFreeWarning("your mobile claim reward today"))
    }
}
