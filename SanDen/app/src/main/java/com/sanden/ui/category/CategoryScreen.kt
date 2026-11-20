package com.sanden.ui.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sanden.domain.model.VaultFile
import com.sanden.ui.components.EmptyState
import com.sanden.ui.components.AuroraGlassBackground
import com.sanden.ui.components.EmptyState
import com.sanden.ui.components.FileCard
import com.sanden.ui.components.SanDenSearchBar
import com.sanden.ui.components.SortMenu
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    onNavigateBack: () -> Unit,
    onFileClick: (VaultFile) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val files by viewModel.files.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()

    val isSelectionMode = selectedFiles.isNotEmpty()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                            text = viewModel.category.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
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
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SanDenSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = "Search ${viewModel.category.name.lowercase()}..."
            )

            if (files.isEmpty()) {
                EmptyState(
                    title = if (searchQuery.isNotEmpty()) "No Matching Files" else "No Files Found",
                    description = if (searchQuery.isNotEmpty()) "Try a different search query." else "Add some files to your vault to see them organized here.",
                    vaultPath = "",
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

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete ${selectedFiles.size} items?") },
            text = { Text("Are you sure you want to permanently delete these files from your vault? This cannot be undone.") },
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
