package com.example.okane.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TEMPORARY discovery-phase table: raw financial-looking notifications,
 * captured so the per-app parsers can be written against real text without
 * the phone being tethered to Logcat.
 *
 * Deleted (table and all rows) once the 6 parsers are confirmed.
 */
@Entity(tableName = "raw_notifications")
data class RawNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    // Nullable: apps sometimes post notifications without one of these.
    val title: String?,
    val text: String?,
    val bigText: String?,
    val postTime: Long,
    val capturedAt: Long
)
