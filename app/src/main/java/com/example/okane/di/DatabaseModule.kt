package com.example.okane.di

import android.content.Context
import androidx.room.Room
import com.example.okane.data.local.CaptureDao
import com.example.okane.data.local.CategoryDao
import com.example.okane.data.local.OKANE_MIGRATIONS
import com.example.okane.data.local.OkaneDatabase
import com.example.okane.data.local.RawNotificationDao
import com.example.okane.data.local.TransactionDao
import com.example.okane.data.local.WalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Teaches Hilt how to build the pieces it can't construct by itself.
 * SingletonComponent = these objects live as long as the app process.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // @Singleton: exactly one database connection for the whole app.
    // Multiple open connections to the same SQLite file cause subtle bugs.
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OkaneDatabase =
        Room.databaseBuilder(
            context,
            OkaneDatabase::class.java,
            "okane.db"
        )
            // No destructive fallback. The database holds data that cannot be
            // recreated: hand-entered transactions, and the raw_notifications
            // discovery samples captured on-device. A missing migration must
            // crash loudly in development rather than quietly delete them.
            .addMigrations(*OKANE_MIGRATIONS)
            // Nothing is seeded here. Onboarding owns seeding and is
            // idempotent, so a callback would only duplicate its work and
            // freeze users on whatever the defaults were at install time.
            .build()

    @Provides
    fun provideTransactionDao(database: OkaneDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideCategoryDao(database: OkaneDatabase): CategoryDao =
        database.categoryDao()

    @Provides
    fun provideWalletDao(database: OkaneDatabase): WalletDao =
        database.walletDao()

    @Provides
    fun provideRawNotificationDao(database: OkaneDatabase): RawNotificationDao =
        database.rawNotificationDao()

    @Provides
    fun provideCaptureDao(database: OkaneDatabase): CaptureDao =
        database.captureDao()
}
