package com.example.okane.di

import com.example.okane.data.capture.CaptureProcessor
import com.example.okane.data.capture.RoomCaptureStore
import com.example.okane.data.prefs.SourcePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * One [CaptureProcessor] for the whole process: its lock is what keeps the
 * listener and the review sheet from recording the same money twice, so the
 * service and the ViewModel must share the instance.
 */
@Module
@InstallIn(SingletonComponent::class)
object CaptureModule {

    @Provides
    @Singleton
    fun provideCaptureProcessor(
        store: RoomCaptureStore,
        sourcePreferences: SourcePreferences,
    ): CaptureProcessor = CaptureProcessor(store = store, toggles = sourcePreferences)
}
