package com.example.okane.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okane.data.capture.CaptureRepository
import com.example.okane.data.prefs.CaptureHealth
import com.example.okane.data.prefs.SourcePreferences
import com.example.okane.data.repository.RawCaptureRepository
import com.example.okane.domain.capture.CaptureHealthState
import com.example.okane.domain.capture.SampleExport
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
    rawCaptureRepository: RawCaptureRepository,
    private val captureRepository: CaptureRepository,
    captureHealth: CaptureHealth,
) : ViewModel() {

    // SharedPreferences has no built-in Flow, so we mirror it in a
    // MutableStateFlow. Safe because this ViewModel is the only writer.
    private val _sourceToggles = MutableStateFlow(sourcePreferences.all())
    val sourceToggles: StateFlow<Map<String, Boolean>> = _sourceToggles.asStateFlow()

    /** Live: the listener updates it from the same process. */
    val health: StateFlow<CaptureHealthState> = captureHealth.state

    fun setSourceEnabled(source: String, enabled: Boolean) {
        sourcePreferences.setEnabled(source, enabled)
        _sourceToggles.value = sourcePreferences.all()
    }

    val capturedSampleCount: StateFlow<Int> =
        rawCaptureRepository.observeCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Builds the masked "Bagikan sampel" text and hands it to [share], which
     * opens the system share sheet. The user sees and sends it themselves.
     */
    fun shareSamples(share: (String) -> Unit) {
        viewModelScope.launch {
            share(SampleExport.format(captureRepository.samples()))
        }
    }
}
