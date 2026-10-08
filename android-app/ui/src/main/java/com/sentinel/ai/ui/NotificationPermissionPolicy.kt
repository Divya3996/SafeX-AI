package com.sentinel.ai.ui

import android.os.Build

internal object NotificationPermissionPolicy {

    fun shouldRequestPermission(sdkInt: Int, hasPermission: Boolean): Boolean {
        return sdkInt >= Build.VERSION_CODES.TIRAMISU && !hasPermission
    }
}
