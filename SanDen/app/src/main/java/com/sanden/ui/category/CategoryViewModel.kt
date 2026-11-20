package com.sanden.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanden.domain.model.FileCategory
import com.sanden.domain.model.VaultFile
import com.sanden.domain.usecase.DeleteFilesUseCase
import com.sanden.domain.usecase.ExportFileUseCase
import com.sanden.domain.usecase.GetFilesByCategoryUseCase
import com.sanden.domain.usecase.MoveFilesUseCase
import com.sanden.domain.usecase.ScanVaultUseCase
import com.sanden.ui.components.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getFilesByCategoryUseCase: GetFilesByCategoryUseCase,
    private val deleteFilesUseCase: DeleteFilesUseCase,
    private val exportFileUseCase: ExportFileUseCase,
    private val moveFilesUseCase: MoveFilesUseCase,
    private val scanVaultUseCase: ScanVaultUseCase
) : ViewModel() {

    val category: FileCategory = run {
        val catName = savedStateHandle.get<String>("categoryName") ?: FileCategory.OTHER.name
        try {
            FileCategory.valueOf(catName)
        } catch (_: Exception) {
            FileCategory.OTHER
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<VaultFile>>(emptySet())
    val selectedFiles: StateFlow<Set<VaultFile>> = _selectedFiles.asStateFlow()

    val files: StateFlow<List<VaultFile>> = combine(
        getFilesByCategoryUseCase(category),
        _searchQuery,
        _sortOrder
    ) { rawFiles, query, sort ->
        val filtered = if (query.isBlank()) {
            rawFiles
        } else {
            rawFiles.filter { it.name.contains(query, ignoreCase = true) }
        }

        when (sort) {
            SortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.lastModified }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.lastModified }
            SortOrder.SIZE_DESC -> filtered.sortedByDescending { it.size }
            SortOrder.SIZE_ASC -> filtered.sortedBy { it.size }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSortOrderChanged(newSort: SortOrder) {
        _sortOrder.value = newSort
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun toggleSelection(file: VaultFile) {
        val current = _selectedFiles.value.toMutableSet()
        if (current.contains(file)) {
            current.remove(file)
        } else {
            current.add(file)
        }
        _selectedFiles.value = current
    }

    fun selectAll(allFiles: List<VaultFile>) {
        _selectedFiles.value = allFiles.toSet()
    }

    fun clearSelection() {
        _selectedFiles.value = emptySet()
    }

    fun deleteSelected() {
        val toDelete = _selectedFiles.value.toList()
        if (toDelete.isEmpty()) return
        viewModelScope.launch {
            deleteFilesUseCase(toDelete).collect { }
            clearSelection()
            scanVaultUseCase()
        }
    }

    fun exportSelected() {
        val toExport = _selectedFiles.value.toList()
        if (toExport.isEmpty()) return
        viewModelScope.launch {
            for (file in toExport) {
                exportFileUseCase(file).collect { }
            }
            clearSelection()
        }
    }
}
