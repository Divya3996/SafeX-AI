package com.sentinel.ai.core.model

import org.junit.Assert.*
import org.junit.Test

class TextModelFeaturesTest {
    @Test fun masksIdentifiersAndKeepsNativeWords() {
        val terms = TextModelFeatures.extract("તાત્કાલિક ઓટીપી 123456 મોકલો https://secret.example/token?x=9988 person@example.com")
        assertTrue(terms.containsAll(listOf("તાત્કાલિક", "ઓટીપી", "મોકલો", "numbertoken", "urltoken", "emailtoken")))
        assertFalse(terms.any { it.contains("secret") || it.contains("123456") || it.contains("person") })
    }
    @Test fun invisibleCharactersAndDuplicateWordsMatch() {
        assertEquals(TextModelFeatures.extract("OTP OTP OTP"), TextModelFeatures.extract("O\u200bTP OTP OTP"))
        assertTrue(TextModelFeatures.extract("पासवर्ड").contains("पासवर्ड"))
        assertTrue(TextModelFeatures.extract("લોન").contains("લોન"))
    }
    @Test fun boundsLongInputsAndAddsCharacterEvidence() {
        assertEquals(TextModelFeatures.extract("send ".repeat(3000)), TextModelFeatures.extract("send ".repeat(512)))
        assertTrue(TextModelFeatures.extract("password").contains("~pas"))
    }
    @Test fun validatesModelAndUsesConfiguredThreshold() {
        val model = OfflineTextClassifier(listOf("otp"), listOf(2.0), -1.0, .7f, TextModelFeatures.VERSION)
        assertTrue(model.predict("OTP") >= model.warningThreshold)
        assertEquals(0f, model.predict(""), 0f)
        listOf(Double.NaN, Double.POSITIVE_INFINITY).forEach { bad ->
            assertThrows(IllegalArgumentException::class.java) { OfflineTextClassifier(listOf("otp"), listOf(bad), 0.0, .6f) }
        }
        assertThrows(IllegalArgumentException::class.java) { OfflineTextClassifier(listOf("otp"), listOf(1.0), 0.0, .6f, "unsupported") }
    }
}
