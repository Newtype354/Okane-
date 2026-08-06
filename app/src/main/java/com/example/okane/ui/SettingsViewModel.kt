package com.example.okane.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okane.data.prefs.SourcePreferences
import com.example.okane.data.repository.RawCaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sourcePreferences: SourcePreferences,
    private val rawCaptureRepository: RawCaptureRepository
) : ViewModel() {

    // SharedPreferences has no built-in Flow, so we mirror it in a
    // MutableStateFlow. Safe because this ViewModel is the only writer.
    private val _sourceToggles = MutableStateFlow(sourcePreferences.all())
    val sourceToggles: StateFlow<Map<String, Boolean>> = _sourceToggles.asStateFlow()

    fun setSourceEnabled(source: String, enabled: Boolean) {
        sourcePreferences.setEnabled(source, enabled)
        _sourceToggles.value = sourcePreferences.all()
    }

    // --- Discovery-phase helpers (removed with the raw_notifications table) ---

    val capturedSampleCount: StateFlow<Int> =
        rawCaptureRepository.observeCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Prints every captured sample to Logcat (tag "OkaneSamples") so they can
     * be copied out later without having been tethered when they arrived.
     */
    fun dumpCapturedSamplesToLog() {
        viewModelScope.launch {
            val samples = rawCaptureRepository.getAll()
            Log.i(SAMPLES_TAG, "===== ${samples.size} captured sample(s) =====")
            samples.forEach { s ->
                Log.i(
                    SAMPLES_TAG,
                    "pkg=${s.packageName} | postTime=${s.postTime}\n" +
                        "  title: ${s.title}\n  text: ${s.text}\n  bigText: ${s.bigText}"
                )
            }
        }
    }

    companion object {
        const val SAMPLES_TAG = "OkaneSamples"
    }
}
