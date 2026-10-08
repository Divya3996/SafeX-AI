package com.sentinel.ai.listeners

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentinelNotificationListenerTest {

    @Test
    fun `normalizes Indian country code spacing and punctuation`() {
        assertEquals("9876543210", normalizeNumber("+91 98765-43210"))
        assertEquals("9876543210", normalizeNumber("98765 43210"))
        assertEquals("9876543210", normalizeNumber("(+91) 98765.43210"))
    }

    @Test
    fun `matches WhatsApp sender title to contact display name`() {
        assertTrue(contactNamesMatch("Laksh", "laksh"))
        assertTrue(contactNamesMatch("  Laksh  ", "Laksh"))
        assertFalse(contactNamesMatch("Laksh", "Lakshmi"))
        assertFalse(contactNamesMatch(null, "Laksh"))
    }

    @Test
    fun `prevents substring false positives for short nicknames`() {
        assertFalse(contactNamesMatch("Bank Alert", "Al"))
        assertFalse(contactNamesMatch("Loan Officer", "Dan"))
        assertFalse(contactNamesMatch("Amazon Support", "Sam"))
        assertTrue(contactNamesMatch("John Doe", "John"))
    }
}
