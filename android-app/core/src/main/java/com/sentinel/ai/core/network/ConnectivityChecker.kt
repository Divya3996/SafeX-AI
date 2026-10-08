package com.sentinel.ai.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks whether a usable network interface is currently available.
 *
 * Gated upfront by [HttpClientWrapper] to support offline-first behavior.
 */
interface ConnectivityChecker {
    /** Returns `true` if the device is connected to the Internet. */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    fun isConnected(): Boolean
}

/**
 * Production Android implementation of [ConnectivityChecker].
 *
 * Uses the [NetworkCapabilities] API (minSdk 26).
 * Provided by Hilt in [com.sentinel.ai.core.di.NetworkModule].
 */
@Singleton
class AndroidConnectivityChecker @Inject constructor(
    private val context: Context
) : ConnectivityChecker {

    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    override fun isConnected(): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_NETWORK_STATE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return false

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager
            ?: return false

        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
