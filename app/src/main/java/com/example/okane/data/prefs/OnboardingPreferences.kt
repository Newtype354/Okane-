package com.example.okane.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers that onboarding has run.
 *
 * SharedPreferences rather than a database row: it is one boolean, it is read
 * synchronously on the very first frame to decide what to show, and a Room
 * read there would mean a flash of the wrong screen.
 *
 * A user upgrading from an older version has no flag, so onboarding runs for
 * them too. That is safe because every write it performs is idempotent — see
 * WalletRepository.createIfAbsent and CategoryRepository.create.
 */
@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs =
        context.getSharedPreferences("okane_onboarding", Context.MODE_PRIVATE)

    fun isCompleted(): Boolean = prefs.getBoolean(KEY_COMPLETED, false)

    fun markCompleted() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    private companion object {
        const val KEY_COMPLETED = "completed"
    }
}
