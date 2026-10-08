package com.sentinel.ai.ui

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionPolicyTest {

    @Test
    fun `requests permission on android 13 plus when missing`() {
        assertTrue(
            NotificationPermissionPolicy.shouldRequestPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                hasPermission = false
            )
        )
    }

    @Test
    fun `does not request permission on android 12 and below`() {
        assertFalse(
            NotificationPermissionPolicy.shouldRequestPermission(
                sdkInt = Build.VERSION_CODES.S,
                hasPermission = false
            )
        )
    }

    @Test
    fun `does not request permission when already granted`() {
        assertFalse(
            NotificationPermissionPolicy.shouldRequestPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                hasPermission = true
            )
        )
    }
}
