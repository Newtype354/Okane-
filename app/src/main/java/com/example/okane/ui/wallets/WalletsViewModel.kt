package com.example.okane.ui.wallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okane.data.local.WalletBalance
import com.example.okane.data.repository.ArchiveResult
import com.example.okane.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WalletsViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : ViewModel() {

    private val content = walletRepository.balances.map { rows ->
        WalletContent(
            isLoading = false,
            rows = rows,
            active = WalletsPresenter.active(rows),
            archived = WalletsPresenter.archived(rows),
            total = WalletsPresenter.total(rows),
            archivedContributes = WalletsPresenter.archivedContributes(rows),
        )
    }

    private val _navigation = MutableStateFlow(WalletNavigation())
    private val _editor = MutableStateFlow<WalletEditorState?>(null)
    private val _operation = MutableStateFlow(WalletOperation())

    val uiState: StateFlow<WalletsUiState> = combine(
        content,
        _navigation,
        _editor,
        _operation,
    ) { walletContent, navigation, editor, operation ->
        WalletsUiState(
            isLoading = walletContent.isLoading,
            totalMoney = walletContent.total,
            activeWallets = walletContent.active,
            archivedWallets = walletContent.archived,
            archivedContributesToTotal = walletContent.archivedContributes,
            surface = navigation.surface,
            selectedWallet = walletContent.rows.firstOrNull {
                it.walletId == navigation.selectedWalletId
            },
            editor = editor,
            archiveCandidate = walletContent.rows.firstOrNull {
                it.walletId == navigation.archiveCandidateId
            },
            operationInProgress = operation.inProgress,
            feedbackMessage = operation.feedback,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WalletsUiState(),
    )

    fun openAddWallet() {
        if (_operation.value.inProgress) return
        _editor.value = WalletEditorState(mode = WalletEditorMode.ADD)
    }

    fun openDetail(walletId: Long, fromArchived: Boolean = false) {
        _navigation.value = WalletNavigation(
            surface = WalletSurface.DETAIL,
            selectedWalletId = walletId,
            detailOrigin = if (fromArchived) WalletSurface.ARCHIVED else WalletSurface.ROOT,
        )
    }

    fun openArchived() {
        if (_editor.value != null || _operation.value.inProgress) return
        _navigation.value = WalletNavigation(surface = WalletSurface.ARCHIVED)
    }

    fun navigateBack() {
        if (_operation.value.inProgress) return
        if (_navigation.value.archiveCandidateId != null) {
            dismissArchiveConfirmation()
            return
        }
        if (_editor.value != null) {
            dismissEditor()
            return
        }
        val navigation = _navigation.value
        _navigation.value = when (navigation.surface) {
            WalletSurface.DETAIL -> WalletNavigation(surface = navigation.detailOrigin)
            WalletSurface.ARCHIVED -> WalletNavigation()
            WalletSurface.ROOT -> navigation
        }
    }

    fun openEditWallet() {
        val wallet = uiState.value.selectedWallet ?: return
        if (wallet.isArchived || _operation.value.inProgress) return
        _editor.value = WalletEditorState(
            mode = WalletEditorMode.EDIT,
            walletId = wallet.walletId,
            name = wallet.name,
            initialBalanceInput = wallet.initialBalance.takeIf { it != 0L }?.toString().orEmpty(),
            monthlyFeeInput = wallet.monthlyFee.takeIf { it != 0L }?.toString().orEmpty(),
            makeDefault = wallet.isDefault,
            defaultLocked = wallet.isDefault,
        )
    }

    fun dismissEditor() {
        if (_operation.value.inProgress) return
        _editor.value = null
    }

    fun updateEditorName(value: String) = updateEditor { copy(name = value, errorMessage = null) }

    fun updateEditorInitialBalance(value: String) = updateEditor {
        copy(initialBalanceInput = sanitizeWalletAmountInput(value), errorMessage = null)
    }

    fun updateEditorMonthlyFee(value: String) = updateEditor {
        copy(monthlyFeeInput = sanitizeWalletAmountInput(value), errorMessage = null)
    }

    fun updateEditorDefault(value: Boolean) = updateEditor {
        if (defaultLocked) this else copy(makeDefault = value, errorMessage = null)
    }

    fun saveEditor() {
        val editor = _editor.value ?: return
        if (_operation.value.inProgress) return
        if (editor.name.trim().isEmpty()) {
            _editor.update { it?.copy(errorMessage = "Nama dompet wajib diisi.") }
            return
        }

        setOperation(inProgress = true)
        viewModelScope.launch {
            try {
                val saved = when (editor.mode) {
                    WalletEditorMode.ADD -> {
                        walletRepository.create(
                            name = editor.name,
                            initialBalance = editor.initialBalance,
                            monthlyFee = editor.monthlyFee,
                            makeDefault = editor.makeDefault,
                        )
                        true
                    }

                    WalletEditorMode.EDIT -> {
                        val walletId = editor.walletId ?: return@launch
                        val updated = walletRepository.updateDetails(
                            id = walletId,
                            name = editor.name,
                            initialBalance = editor.initialBalance,
                            monthlyFee = editor.monthlyFee,
                        )
                        if (updated && editor.makeDefault && !editor.defaultLocked) {
                            walletRepository.setDefault(walletId)
                        }
                        updated
                    }
                }
                if (saved) {
                    _editor.value = null
                    setOperation(
                        feedback = if (editor.mode == WalletEditorMode.ADD) {
                            "Dompet berhasil ditambahkan."
                        } else {
                            "Informasi dompet diperbarui."
                        }
                    )
                } else {
                    setOperation(feedback = "Dompet tidak ditemukan. Muat ulang dan coba lagi.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IllegalArgumentException) {
                _editor.update { it?.copy(errorMessage = error.message) }
                setOperation()
            } catch (_: Exception) {
                setOperation(feedback = "Perubahan dompet gagal disimpan. Coba lagi.")
            } finally {
                _operation.update { it.copy(inProgress = false) }
            }
        }
    }

    fun makeSelectedDefault() {
        val wallet = uiState.value.selectedWallet ?: return
        if (wallet.isArchived || wallet.isDefault || _operation.value.inProgress) return
        setOperation(inProgress = true)
        viewModelScope.launch {
            try {
                val changed = walletRepository.setDefault(wallet.walletId)
                setOperation(
                    feedback = if (changed) {
                        "${wallet.name} sekarang menjadi dompet utama."
                    } else {
                        "Dompet ini tidak dapat dijadikan utama."
                    }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                setOperation(feedback = "Dompet utama gagal diperbarui. Coba lagi.")
            } finally {
                _operation.update { it.copy(inProgress = false) }
            }
        }
    }

    fun requestArchiveSelected() {
        val wallet = uiState.value.selectedWallet ?: return
        if (wallet.isArchived || _operation.value.inProgress) return
        _navigation.update { it.copy(archiveCandidateId = wallet.walletId) }
    }

    fun dismissArchiveConfirmation() {
        if (_operation.value.inProgress) return
        _navigation.update { it.copy(archiveCandidateId = null) }
    }

    fun confirmArchive() {
        val wallet = uiState.value.archiveCandidate ?: return
        if (_operation.value.inProgress) return
        setOperation(inProgress = true)
        viewModelScope.launch {
            try {
                when (walletRepository.archive(wallet.walletId)) {
                    ArchiveResult.ARCHIVED -> {
                        _navigation.value = WalletNavigation()
                        setOperation(feedback = "${wallet.name} telah diarsipkan.")
                    }
                    ArchiveResult.REFUSED_LAST_WALLET -> {
                        _navigation.update { it.copy(archiveCandidateId = null) }
                        setOperation(
                            feedback = "Dompet aktif terakhir tidak dapat diarsipkan."
                        )
                    }
                    ArchiveResult.NOT_FOUND -> {
                        _navigation.update { it.copy(archiveCandidateId = null) }
                        setOperation(feedback = "Dompet tidak ditemukan.")
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                setOperation(feedback = "Dompet gagal diarsipkan. Coba lagi.")
            } finally {
                _operation.update { it.copy(inProgress = false) }
            }
        }
    }

    fun restoreSelected() {
        val wallet = uiState.value.selectedWallet ?: return
        if (!wallet.isArchived || _operation.value.inProgress) return
        setOperation(inProgress = true)
        viewModelScope.launch {
            try {
                if (walletRepository.unarchive(wallet.walletId)) {
                    _navigation.value = WalletNavigation(surface = WalletSurface.ARCHIVED)
                    setOperation(feedback = "${wallet.name} diaktifkan kembali.")
                } else {
                    setOperation(feedback = "Dompet tidak ditemukan.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                setOperation(feedback = "Dompet gagal diaktifkan kembali. Coba lagi.")
            } finally {
                _operation.update { it.copy(inProgress = false) }
            }
        }
    }

    fun consumeFeedback() {
        _operation.update { it.copy(feedback = null) }
    }

    private fun updateEditor(transform: WalletEditorState.() -> WalletEditorState) {
        if (_operation.value.inProgress) return
        _editor.update { editor -> editor?.transform() }
    }

    private fun setOperation(
        inProgress: Boolean = false,
        feedback: String? = null,
    ) {
        _operation.value = WalletOperation(inProgress, feedback)
    }

    private data class WalletContent(
        val isLoading: Boolean = true,
        val rows: List<WalletBalance> = emptyList(),
        val active: List<WalletBalance> = emptyList(),
        val archived: List<WalletBalance> = emptyList(),
        val total: Long = 0,
        val archivedContributes: Boolean = false,
    )

    private data class WalletNavigation(
        val surface: WalletSurface = WalletSurface.ROOT,
        val selectedWalletId: Long? = null,
        val detailOrigin: WalletSurface = WalletSurface.ROOT,
        val archiveCandidateId: Long? = null,
    )

    private data class WalletOperation(
        val inProgress: Boolean = false,
        val feedback: String? = null,
    )
}

