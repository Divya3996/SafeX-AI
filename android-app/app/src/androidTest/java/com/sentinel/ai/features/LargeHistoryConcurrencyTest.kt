package com.sentinel.ai.features

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.sentinel.ai.core.data.local.SentinelDatabase
import com.sentinel.ai.core.data.local.ThreatRecordEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.junit.Assert.*
import org.junit.Test

/** Regression for history exceeding Android's 2 MB cursor window during concurrent edits. */
class LargeHistoryConcurrencyTest {
    @Test fun multiWindowHistoryRemainsConsistentDuringConcurrentWrites() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val name = "history-window-regression.db"
        context.deleteDatabase(name)
        val db = Room.databaseBuilder(context, SentinelDatabase::class.java, name)
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING).build()
        val dao = db.threatDao()
        fun record(i: Int, time: Long) = ThreatRecordEntity("window-$i", "SCAN_RESULT", "test", null, null,
            "x".repeat(8000), "GREEN", 0f, "Synthetic cursor-window regression", null, time)
        try {
            withTimeout(30000) {
                db.withTransaction { repeat(400) { dao.upsertThreatRecord(record(it, it.toLong())) } }
                val snapshots = java.util.concurrent.atomic.AtomicInteger()
                val observer = launch {
                    dao.observeRecords().collect { rows ->
                        assertEquals(400, rows.size)
                        assertEquals(400, rows.map { it.id }.distinct().size)
                        snapshots.incrementAndGet()
                    }
                }
                try {
                    val reads = async {
                        repeat(12) {
                            val rows = dao.getAllThreatRecords()
                            assertEquals(400, rows.size)
                            assertEquals(400, rows.map { it.id }.distinct().size)
                        }
                    }
                    val writes = async {
                        repeat(24) { i ->
                            db.withTransaction {
                                dao.deleteById("window-$i")
                                dao.upsertThreatRecord(record(i, 1000L + i))
                            }
                            delay(20)
                        }
                    }
                    reads.await(); writes.await()
                    assertTrue(snapshots.get() > 0)
                } finally { observer.cancelAndJoin() }
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
