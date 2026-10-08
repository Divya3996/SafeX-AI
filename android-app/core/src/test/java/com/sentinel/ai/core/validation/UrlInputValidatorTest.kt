package com.sentinel.ai.core.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlInputValidatorTest {

    @Test
    fun `rejects plain text bare domains and malformed schemes`() {
        assertFalse(UrlInputValidator.isValid("hello"))
        assertFalse(UrlInputValidator.isValid("google.com"))
        assertFalse(UrlInputValidator.isValid("httpe://fake.com"))
        assertFalse(UrlInputValidator.isValid("https://"))
    }

    @Test
    fun `accepts complete HTTP and HTTPS URLs`() {
        assertTrue(UrlInputValidator.isValid("http://google.com"))
        assertTrue(UrlInputValidator.isValid("https://google.com"))
    }
}
