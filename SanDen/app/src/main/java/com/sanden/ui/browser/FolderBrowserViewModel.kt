package com.sanden.ui.browser

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanden.domain.model.VaultFile
import com.sanden.domain.repository.VaultRepository
import com.sanden.domain.usecase.CreateFolderUseCase
import com.sanden.domain.usecase.DeleteFilesUseCase
import com.sanden.domain.usecase.ExportFileUseCase
import com.sanden.domain.usecase.GetFolderContentsUseCase
import com.sanden.domain.usecase.ImportFilesUseCase
import com.sanden.domain.usecase.RenameFileUseCase
import com.sanden.domain.usecase.ScanVaultUseCase
import com.sanden.ui.components.BreadcrumbItem
import com.sanden.ui.components.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FolderBrowserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
    private val getFolderContentsUseCase: GetFolderContentsUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val renameFileUseCase: RenameFileUseCase,
    private val deleteFilesUseCase: DeleteFilesUseCase,
    private val exportFileUseCase: ExportFileUseCase,
    private val importFilesUseCase: ImportFilesUseCase,
    private val scanVaultUseCase: ScanVaultUseCase
) : ViewModel() {

    val rootPath: String = vaultRepository.getActiveVaultPath()

    private val _currentPath = MutableStateFlow(
        savedStateHandle.get<String>("path")?.takeIf { it.isNotBlank() } ?: rootPath
    )
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NAME_ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<VaultFile>>(emptySet())
    val selectedFiles: StateFlow<Set<VaultFile>> = _selectedFiles.asStateFlow()

    val breadcrumbs: StateFlow<List<BreadcrumbItem>> = combine(_currentPath) { paths ->
        val path = paths[0]
        val items = mutableListOf<BreadcrumbItem>()
        items.add(BreadcrumbItem(name = "Vault", path = rootPath))

        if (path != rootPath && path.startsWith(rootPath)) {
            val relative = path.removePrefix(rootPath).trimStart(File.separatorChar, '/')
            val segments = relative.split(File.separatorChar, '/').filter { it.isNotEmpty() }
            var accumulated = rootPath
            for (segment in segments) {
                accumulated = "$accumulated/$segment"
                items.add(BreadcrumbItem(name = segment, path = accumulated))
            }
        }
        items
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf(BreadcrumbItem("Vault", rootPath))
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val files: StateFlow<List<VaultFile>> = _currentPath
        .flatMapLatest { path ->
            combine(
                getFolderContentsUseCase(path),
                _sortOrder
            ) { allInFolder, sort ->
                val folders = allInFolder.filter { it.isDirectory }
                val filesOnly = allInFolder.filter { !it.isDirectory }

                val sortedFolders = when (sort) {
                    SortOrder.NAME_ASC -> folders.sortedBy { it.name.lowercase() }
                    SortOrder.NAME_DESC -> folders.sortedByDescending { it.name.lowercase() }
                    else -> folders.sortedBy { it.name.lowercase() }
                }

                val sortedFiles = when (sort) {
                    SortOrder.NAME_ASC -> filesOnly.sortedBy { it.name.lowercase() }
                    SortOrder.NAME_DESC -> filesOnly.sortedByDescending { it.name.lowercase() }
                    SortOrder.DATE_DESC -> filesOnly.sortedByDescending { it.lastModified }
                    SortOrder.DATE_ASC -> filesOnly.sortedBy { it.lastModified }
                    SortOrder.SIZE_DESC -> filesOnly.sortedByDescending { it.size }
                    SortOrder.SIZE_ASC -> filesOnly.sortedBy { it.size }
                }

                sortedFolders + sortedFiles
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun navigateTo(path: String) {
        _currentPath.value = path
        clearSelection()
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

    fun createFolder(name: String) {
        viewModelScope.launch {
            createFolderUseCase(_currentPath.value, name)
            scanVaultUseCase()
        }
    }

    fun renameFile(file: VaultFile, newName: String) {
        viewModelScope.launch {
            renameFileUseCase(file, newName)
            scanVaultUseCase()
        }
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

    fun importFiles(uris: List<Uri>) {
        viewModelScope.launch {
            importFilesUseCase(uris, _currentPath.value).collect { }
            scanVaultUseCase()
        }
    }
}
