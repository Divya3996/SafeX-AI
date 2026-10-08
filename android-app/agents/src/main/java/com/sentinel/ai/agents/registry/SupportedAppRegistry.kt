package com.sentinel.ai.agents.registry

import javax.inject.Inject
import javax.inject.Singleton

/** Identifies notification sources handled by the notification pipeline. */
@Singleton
class SupportedAppRegistry @Inject constructor() {

    fun isSupported(packageName: String?): Boolean = packageName in supportedPackages || (com.sentinel.ai.agents.BuildConfig.DEBUG && packageName == "com.sentinel.demo")

    companion object {
        val supportedPackages = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "com.google.android.apps.messaging",
            "com.google.android.gm",
            "com.instagram.android",
            "com.facebook.orca",
            "org.thoughtcrime.securesms",
            "com.discord",
            "com.Slack"
        )
    }
}
