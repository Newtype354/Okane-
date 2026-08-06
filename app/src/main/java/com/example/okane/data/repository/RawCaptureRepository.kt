package com.example.okane.data.repository

import com.example.okane.data.local.RawNotification
import com.example.okane.data.local.RawNotificationDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Access point for the TEMPORARY discovery captures ([RawNotification]).
 * Removed together with that table once the 6 parsers are confirmed.
 */
@Singleton
class RawCaptureRepository @Inject constructor(
    private val dao: RawNotificationDao
) {
    suspend fun add(raw: RawNotification): Long = dao.insert(raw)

    suspend fun getAll(): List<RawNotification> = dao.getAll()

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun clear() = dao.deleteAll()
}
