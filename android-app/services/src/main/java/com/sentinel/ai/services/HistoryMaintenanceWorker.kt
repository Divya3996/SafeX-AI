package com.sentinel.ai.services

import android.content.Context
import androidx.work.*
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.feature.PrivacyPreferences
import java.util.concurrent.TimeUnit

/** Daily, network-free retention maintenance. */
class HistoryMaintenanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            PrivacyPreferences.init(applicationContext)
            ThreatJournal.initialize(applicationContext)
            ThreatJournal.applyRetention(PrivacyPreferences.retentionDays)
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { Result.retry() }
    }
    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("history-retention", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<HistoryMaintenanceWorker>(24, TimeUnit.HOURS).build())
        }
    }
}
