package com.sentinel.ai.protection.intent.reputation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockReputationProviderTest {

    private val provider = MockReputationProvider()

    @Test
    fun supportsOnlyUrlTargets() {
        assertTrue(provider.supports(ReputationTarget.Url("https://example.com")))
    }

    @Test
    fun maliciousUrlReturnsMaliciousVerdict() = runTest {
        val target = ReputationTarget.Url("https://malicious.com/phish")
        val result = provider.evaluate(target)

        assertNotNull(result)
        assertEquals("MockProvider", result?.providerName)
        assertEquals(ReputationVerdict.MALICIOUS, result?.reputation)
        assertEquals(0.95f, result?.confidence ?: 0f, 0.001f)
    }

    @Test
    fun suspiciousUrlReturnsSuspiciousVerdict() = runTest {
        val target = ReputationTarget.Url("https://suspicious.com/test")
        val result = provider.evaluate(target)

        assertNotNull(result)
        assertEquals(ReputationVerdict.SUSPICIOUS, result?.reputation)
        assertEquals(0.80f, result?.confidence ?: 0f, 0.001f)
    }

    @Test
    fun cleanUrlReturnsCleanVerdict() = runTest {
        val target = ReputationTarget.Url("https://clean.com/home")
        val result = provider.evaluate(target)

        assertNotNull(result)
        assertEquals(ReputationVerdict.CLEAN, result?.reputation)
        assertEquals(0.90f, result?.confidence ?: 0f, 0.001f)
    }

    @Test
    fun arbitraryUrlReturnsUnknownVerdict() = runTest {
        val target = ReputationTarget.Url("https://unknown-domain.org/something")
        val result = provider.evaluate(target)

        assertNotNull(result)
        assertEquals(ReputationVerdict.UNKNOWN, result?.reputation)
        assertEquals(0.50f, result?.confidence ?: 0f, 0.001f)
    }
}
