package com.sentinel.ai.protection.intent.reputation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Generic feed snapshot only. Scanned URLs are never submitted to a server. */
@Singleton
class LocalReputationProvider @Inject constructor(@ApplicationContext private val context: Context): ReputationProvider {
    override val providerName = "Local threat snapshot"
    private var modified = Long.MIN_VALUE
    private var entries = emptySet<String>()
    override fun supports(target: ReputationTarget) = target is ReputationTarget.Url
    override suspend fun evaluate(target: ReputationTarget): ReputationResult? = lookup(target)
    @Synchronized private fun lookup(target: ReputationTarget): ReputationResult? {
        val url = (target as? ReputationTarget.Url)?.url ?: return null
        val file = File(context.filesDir, "threat-feed.txt")
        val stamp = if (file.exists()) file.lastModified() else 0L
        if (stamp != modified) {
            val text = if (file.exists()) file.readText() else context.assets.open("threat-feed.txt").bufferedReader().use { it.readText() }
            entries = text.lineSequence().filter { it.startsWith("https://") || it.startsWith("http://") }.take(20000).toSet()
            modified = stamp
        }
        val match = url.trim() in entries
        return ReputationResult(providerName, if (match) 0.98f else 0f,
            if (match) ReputationVerdict.MALICIOUS else ReputationVerdict.UNKNOWN,
            if (match) "Matches a known phishing URL in the on-device snapshot" else "No local snapshot match; this is not a guarantee of safety", System.currentTimeMillis())
    }
}
