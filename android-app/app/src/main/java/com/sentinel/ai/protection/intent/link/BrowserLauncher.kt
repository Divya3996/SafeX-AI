package com.sentinel.ai.protection.intent.link

import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

/**
 * Utility for launching a browser after a URL has been approved by future protection logic.
 */
class BrowserLauncher {

    /**
     * Opens an approved HTTP(S) URL in Chrome. If Chrome is unavailable, an external-only
     * chooser is shown instead.
     *
     * @return true when Android accepted a browser handoff, false when no other app can handle
     * the URL or the value is not a web URL.
     */
    fun launch(context: Context, url: String): Boolean {
        val uri = Uri.parse(url)

        if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) {
            Log.w("BrowserLauncher", "Invalid URL")
            return false
        }

        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.android.chrome")

        return try {
            Log.d(TAG, "Opening approved destination")
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Chrome unavailable; trying external browser chooser", e)
            launchExternalChooser(context, intent)
        }
    }

    private fun launchExternalChooser(context: Context, intent: Intent): Boolean {
        intent.setPackage(null)
        val handlers = context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val externalHandlers = handlers.filter { it.activityInfo.packageName != context.packageName }
        if (externalHandlers.isEmpty()) {
            Log.w(TAG, "No external browser available for approved URL")
            return false
        }
        val destinations = externalHandlers.map {
            Intent(intent).setComponent(ComponentName(it.activityInfo.packageName, it.activityInfo.name))
        }

        return runCatching {
            val chooser = Intent.createChooser(destinations.first(), com.sentinel.ai.core.i18n.I18n.translate(context, "Open with")).apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, destinations.drop(1).toTypedArray())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        }.getOrElse { error ->
            Log.w(TAG, "No browser available for approved URL", error)
            false
        }
    }

    private companion object {
        const val HTTP_PREFIX = "http://"
        const val HTTPS_PREFIX = "https://"
        const val CHROME_PACKAGE = "com.android.chrome"
        const val TAG = "BrowserLauncher"
    }
}
