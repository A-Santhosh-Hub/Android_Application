package com.sanden.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanden.domain.model.StorageMode
import com.sanden.domain.repository.VaultRepository
import com.sanden.domain.usecase.ScanVaultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val scanVaultUseCase: ScanVaultUseCase
) : ViewModel() {

    private val _storageMode = MutableStateFlow(vaultRepository.getStorageMode())
    val storageMode: StateFlow<StorageMode> = _storageMode.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _rescanMessage = MutableStateFlow<String?>(null)
    val rescanMessage: StateFlow<String?> = _rescanMessage.asStateFlow()

    val vaultPath: String = vaultRepository.getVaultRootPath()

    fun setStorageMode(mode: StorageMode) {
        vaultRepository.setStorageMode(mode)
        _storageMode.value = mode
        rescan()
    }

    fun rescan() {
        viewModelScope.launch {
            _isScanning.value = true
            _rescanMessage.value = null
            try {
                val result = scanVaultUseCase()
                _rescanMessage.value = "Scan complete: ${result.totalFiles} files indexed (${result.added.size} added, ${result.removed.size} removed)"
            } catch (e: Exception) {
                _rescanMessage.value = "Scan failed: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearRescanMessage() {
        _rescanMessage.value = null
    }
}
