package com.sentinel.ai.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ThreatDao {
    // Keep a stable SQLite snapshot while Android refills multi-window cursors.
    @androidx.room.Transaction
    @Query("SELECT * FROM threat_records ORDER BY timestamp DESC")
    suspend fun getAllThreatRecords(): List<ThreatRecordEntity>

    @androidx.room.Transaction
    @Query("SELECT * FROM threat_records ORDER BY timestamp DESC LIMIT 1000")
    fun observeRecords(): kotlinx.coroutines.flow.Flow<List<ThreatRecordEntity>>

    @Query("DELETE FROM threat_records")
    suspend fun deleteAll()

    @Query("DELETE FROM threat_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM threat_records WHERE timestamp < :before")
    suspend fun deleteBefore(before: Long)

    @Query("DELETE FROM threat_records WHERE rowid NOT IN (SELECT rowid FROM threat_records ORDER BY timestamp DESC LIMIT 1000)")
    suspend fun trimHistory()

    @androidx.room.Transaction
    suspend fun saveBounded(record: ThreatRecordEntity) { upsertThreatRecord(record); trimHistory() }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThreatRecord(record: ThreatRecordEntity)
}
