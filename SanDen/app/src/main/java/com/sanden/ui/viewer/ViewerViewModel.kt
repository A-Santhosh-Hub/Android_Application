package com.sanden.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanden.domain.model.VaultFile
import com.sanden.domain.repository.VaultRepository
import com.sanden.domain.usecase.DeleteFilesUseCase
import com.sanden.domain.usecase.ExportFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
    private val deleteFilesUseCase: DeleteFilesUseCase,
    private val exportFileUseCase: ExportFileUseCase
) : ViewModel() {

    val fileId: Long = savedStateHandle.get<Long>("fileId") ?: 0L

    private val _currentFile = MutableStateFlow<VaultFile?>(null)
    val currentFile: StateFlow<VaultFile?> = _currentFile.asStateFlow()

    private val _adjacentFiles = MutableStateFlow<List<VaultFile>>(emptyList())
    val adjacentFiles: StateFlow<List<VaultFile>> = _adjacentFiles.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val file = vaultRepository.getFileById(fileId)
            _currentFile.value = file
            if (file != null) {
                vaultRepository.getFilesByCategory(file.category).collect { allInCat ->
                    _adjacentFiles.value = allInCat
                }
            }
        }
    }

    fun deleteFile(file: VaultFile, onComplete: () -> Unit) {
        viewModelScope.launch {
            deleteFilesUseCase(listOf(file)).collect { }
            onComplete()
        }
    }

    fun exportFile(file: VaultFile) {
        viewModelScope.launch {
            exportFileUseCase(file).collect { }
        }
    }
}
