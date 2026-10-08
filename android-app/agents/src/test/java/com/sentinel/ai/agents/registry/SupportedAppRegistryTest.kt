package com.sentinel.ai.agents.registry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedAppRegistryTest {

    private val registry = SupportedAppRegistry()

    @Test
    fun `supports all configured communication applications`() {
        assertTrue(registry.isSupported("com.whatsapp"))
        assertTrue(registry.isSupported("com.whatsapp.w4b"))
        assertTrue(registry.isSupported("org.telegram.messenger"))
        assertTrue(registry.isSupported("com.google.android.apps.messaging"))
        assertTrue(registry.isSupported("com.google.android.gm"))
        assertTrue(registry.isSupported("com.instagram.android"))
        assertTrue(registry.isSupported("com.facebook.orca"))
        assertTrue(registry.isSupported("org.thoughtcrime.securesms"))
        assertTrue(registry.isSupported("com.discord"))
        assertTrue(registry.isSupported("com.Slack"))
        assertFalse(registry.isSupported("com.example.fake"))
        assertFalse(registry.isSupported(null))
    }
}
