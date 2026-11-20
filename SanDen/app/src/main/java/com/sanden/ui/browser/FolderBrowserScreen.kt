package com.sanden.ui.browser

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanden.domain.model.VaultFile
import com.sanden.ui.components.BreadcrumbBar
import com.sanden.ui.components.EmptyState
import com.sanden.ui.components.AuroraGlassBackground
import com.sanden.ui.components.FileCard
import com.sanden.ui.components.SortMenu
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    onNavigateBack: () -> Unit,
    onFolderClick: (String) -> Unit,
    onFileClick: (VaultFile) -> Unit,
    viewModel: FolderBrowserViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val currentPath by viewModel.currentPath.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val files by viewModel.files.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()

    val isSelectionMode = selectedFiles.isNotEmpty()

    BackHandler {
        if (isSelectionMode) {
            viewModel.clearSelection()
        } else if (currentPath != viewModel.rootPath) {
            val parent = File(currentPath).parent ?: viewModel.rootPath
            viewModel.navigateTo(parent)
        } else {
            onNavigateBack()
        }
    }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameFileName by remember { mutableStateOf("") }
    var fileToRename by remember { mutableStateOf<VaultFile?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris)
        }
    }

    val currentFolderDisplayName = if (currentPath == viewModel.rootPath) {
        "SanDen Root"
    } else {
        File(currentPath).name
    }

    AuroraGlassBackground(modifier = modifier) {
        Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedFiles.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close Selection")
                        }
                    },
                    actions = {
                        if (selectedFiles.size == 1) {
                            IconButton(onClick = {
                                fileToRename = selectedFiles.first()
                                renameFileName = selectedFiles.first().name
                                showRenameDialog = true
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Rename")
                            }
                        }
                        IconButton(onClick = { viewModel.selectAll(files) }) {
                            Icon(Icons.Rounded.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { viewModel.exportSelected() }) {
                            Icon(Icons.Rounded.IosShare, contentDescription = "Export")
                        }
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = currentFolderDisplayName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (currentPath != viewModel.rootPath) {
                                    val parent = File(currentPath).parent ?: viewModel.rootPath
                                    viewModel.navigateTo(parent)
                                } else {
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(Icons.Rounded.CreateNewFolder, contentDescription = "New Folder")
                        }
                        SortMenu(
                            selectedSort = sortOrder,
                            onSortSelected = { viewModel.onSortOrderChanged(it) },
                            isGridView = isGridView,
                            onViewToggle = { viewModel.toggleViewMode() }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Rounded.FileUpload,
                    contentDescription = "Import Here",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BreadcrumbBar(
                breadcrumbs = breadcrumbs,
                onItemClick = { viewModel.navigateTo(it.path) }
            )

            if (files.isEmpty()) {
                EmptyState(
                    title = "This Folder is Empty",
                    description = "Import files using the button below or drag and drop files from your computer.",
                    vaultPath = currentPath,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(files, key = { it.path }) { file ->
                        FileCard(
                            file = file,
                            isSelected = selectedFiles.contains(file),
                            isSelectionMode = isSelectionMode,
                            isGridView = true,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(file)
                                } else if (file.isDirectory) {
                                    viewModel.navigateTo(file.path)
                                } else {
                                    onFileClick(file)
                                }
                            },
                            onLongClick = { viewModel.toggleSelection(file) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(files, key = { it.path }) { file ->
                        FileCard(
                            file = file,
                            isSelected = selectedFiles.contains(file),
                            isSelectionMode = isSelectionMode,
                            isGridView = false,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(file)
                                } else if (file.isDirectory) {
                                    viewModel.navigateTo(file.path)
                                } else {
                                    onFileClick(file)
                                }
                            },
                            onLongClick = { viewModel.toggleSelection(file) }
                        )
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateFolderDialog = false
                newFolderName = ""
            },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName.trim())
                            showCreateFolderDialog = false
                            newFolderName = ""
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateFolderDialog = false
                        newFolderName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog && fileToRename != null) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                fileToRename = null
            },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameFileName,
                    onValueChange = { renameFileName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameFileName.isNotBlank()) {
                            viewModel.renameFile(fileToRename!!, renameFileName.trim())
                            showRenameDialog = false
                            fileToRename = null
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        fileToRename = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete ${selectedFiles.size} items?") },
            text = { Text("Are you sure you want to delete these files and folders? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelected()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    }
}
