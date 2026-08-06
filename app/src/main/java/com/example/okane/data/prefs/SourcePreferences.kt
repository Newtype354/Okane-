package com.example.okane.data.prefs

import android.content.Context
import com.example.okane.domain.Sources
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the per-app "record transactions from this source?" toggles.
 *
 * SharedPreferences instead of a database table on purpose: six booleans
 * don't justify a schema change (which would wipe the collected samples
 * during the discovery period), and it survives app restarts just fine.
 */
@Singleton
class SourcePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs =
        context.getSharedPreferences("okane_source_toggles", Context.MODE_PRIVATE)

    // Default true: a source the user never touched is tracked.
    fun isEnabled(source: String): Boolean = prefs.getBoolean(source, true)

    fun setEnabled(source: String, enabled: Boolean) {
        prefs.edit().putBoolean(source, enabled).apply()
    }

    fun all(): Map<String, Boolean> = Sources.ALL.associateWith { isEnabled(it) }
}
