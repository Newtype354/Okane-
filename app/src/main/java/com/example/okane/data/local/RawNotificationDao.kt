package com.example.okane.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RawNotificationDao {

    @Insert
    suspend fun insert(raw: RawNotification): Long

    // One-shot read (not a Flow): used when dumping samples to Logcat.
    @Query("SELECT * FROM raw_notifications ORDER BY postTime DESC")
    suspend fun getAll(): List<RawNotification>

    // Live count shown on the setup screen so the user can see
    // collection working without a PC.
    @Query("SELECT COUNT(*) FROM raw_notifications")
    fun observeCount(): Flow<Int>

    // Cleanup once the parsers are confirmed.
    @Query("DELETE FROM raw_notifications")
    suspend fun deleteAll()
}
