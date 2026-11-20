package com.sanden.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanden.domain.model.FileCategory
import com.sanden.domain.model.StorageMode
import com.sanden.domain.model.VaultFile
import com.sanden.domain.repository.VaultRepository
import com.sanden.domain.usecase.GetCategoryStatsUseCase
import com.sanden.domain.usecase.ImportFilesUseCase
import com.sanden.domain.usecase.ScanVaultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val categoryStats: Map<FileCategory, Pair<Int, Long>> = emptyMap(),
    val totalFiles: Int = 0,
    val totalSize: Long = 0L,
    val isScanning: Boolean = false,
    val vaultPath: String = "",
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCategoryStatsUseCase: GetCategoryStatsUseCase,
    private val scanVaultUseCase: ScanVaultUseCase,
    private val importFilesUseCase: ImportFilesUseCase,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val categoryStats: StateFlow<Map<FileCategory, Pair<Int, Long>>> =
        getCategoryStatsUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = FileCategory.entries.associateWith { 0 to 0L }
            )

    val vaultPath: String
        get() = vaultRepository.getVaultRootPath()

    val storageMode: StorageMode
        get() = vaultRepository.getStorageMode()

    init {
        rescan()
    }

    fun rescan() {
        viewModelScope.launch {
            _isScanning.value = true
            _error.value = null
            try {
                scanVaultUseCase()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Rescan failed", e)
                _error.value = e.message
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun importFiles(uris: List<Uri>) {
        viewModelScope.launch {
            _isScanning.value = true
            _error.value = null
            try {
                importFilesUseCase(uris, "").collect { op ->
                    if (op.error != null) {
                        _error.value = op.error
                    }
                }
                scanVaultUseCase()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isScanning.value = false
            }
        }
    }
}
