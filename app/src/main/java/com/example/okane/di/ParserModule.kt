package com.example.okane.di

import com.example.okane.domain.parser.NotificationParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * NotificationParser is a plain Kotlin class (no @Inject constructor,
 * because its constructor has a default parameter for testability),
 * so Hilt needs this recipe to provide it.
 */
@Module
@InstallIn(SingletonComponent::class)
object ParserModule {

    @Provides
    @Singleton
    fun provideNotificationParser(): NotificationParser = NotificationParser()
}
