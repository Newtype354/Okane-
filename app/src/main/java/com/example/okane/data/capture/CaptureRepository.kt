package com.example.okane.data.capture

import com.example.okane.data.local.CaptureDao
import com.example.okane.data.local.CaptureSuggestionEntity
import com.example.okane.data.local.RawNotificationDao
import com.example.okane.domain.capture.CapturedSample
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** What the UI may do with captured notifications. */
@Singleton
class CaptureRepository @Inject constructor(
    private val dao: CaptureDao,
    private val rawDao: RawNotificationDao,
    private val processor: CaptureProcessor,
) {
    /** The "Perlu konfirmasi" inbox, newest first. */
    val pending: Flow<List<CaptureSuggestionEntity>> = dao.observePending()

    suspend fun accept(id: Long, confirmation: Confirmation): AcceptResult =
        processor.accept(id, confirmation)

    suspend fun dismiss(id: Long): Boolean = processor.dismiss(id)

    /**
     * Real notifications from the supported apps, newest first: the discovery
     * samples plus every suggestion ever made (accepted or dismissed), which
     * is what a new template is written from.
     */
    suspend fun samples(limit: Int = 200): List<CapturedSample> {
        val raw = rawDao.getAll().map {
            CapturedSample(it.packageName, it.title, it.bigText ?: it.text, it.postTime)
        }
        val suggested = dao.recentSuggestions(limit).map {
            CapturedSample(it.packageName, it.title, it.text, it.postedAt)
        }
        return (raw + suggested)
            .distinctBy { Triple(it.packageName, it.title, it.text) }
            .sortedByDescending { it.postedAt }
            .take(limit)
    }
}
