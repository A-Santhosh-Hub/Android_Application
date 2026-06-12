package com.example.ui

import android.graphics.Color as AndroidColor
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChecklistItem
import com.example.data.DrawingPoint
import com.example.data.DrawingStroke
import com.example.data.Label
import com.example.data.Note
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepApp(viewModel: NoteViewModel) {
    val context = LocalContext.current
    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val trashedNotes by viewModel.trashedNotes.collectAsStateWithLifecycle()
    val allLabels by viewModel.allLabels.collectAsStateWithLifecycle()
    
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilterType by viewModel.selectedFilterType.collectAsStateWithLifecycle()
    val selectedFilterColor by viewModel.selectedFilterColor.collectAsStateWithLifecycle()
    val editingNote by viewModel.editingNote.collectAsStateWithLifecycle()
    val isCanvasOpen by viewModel.isCanvasOpen.collectAsStateWithLifecycle()
    val currentCanvasStrokes by viewModel.currentCanvasStrokes.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showLabelCreatorDialog by remember { mutableStateOf(false) }

    // Combine section + query filters
    val notesFeed = remember(currentSection, activeNotes, archivedNotes, trashedNotes) {
        when (currentSection) {
            is NoteSection.Notes -> activeNotes.filter { it.title != "Global Tasks List" }
            is NoteSection.ToDoList -> activeNotes
            is NoteSection.Archive -> archivedNotes
            is NoteSection.Trash -> trashedNotes
            is NoteSection.LabelFilter -> {
                val filter = (currentSection as NoteSection.LabelFilter).labelName
                (activeNotes + archivedNotes).filter { note ->
                    note.title != "Global Tasks List" &&
                    note.labels.split(",").filter { it.isNotBlank() }.contains(filter)
                }
            }
        }
    }

    val filteredNotes = remember(notesFeed, searchQuery, selectedFilterType, selectedFilterColor) {
        notesFeed.filter { note ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                note.title.contains(searchQuery, ignoreCase = true) ||
                (if (note.isChecklist) {
                    note.getChecklistItems().any { it.text.contains(searchQuery, ignoreCase = true) }
                } else {
                    note.content.contains(searchQuery, ignoreCase = true)
                })
            }
            
            val matchesType = when (selectedFilterType) {
                SearchFilterType.CHECKLIST -> note.isChecklist
                SearchFilterType.DRAWING -> note.drawingData != null
                SearchFilterType.REMINDER -> note.reminderTime != null
                null -> true
            }

            val matchesColor = if (selectedFilterColor == null) true else note.colorIndex == selectedFilterColor

            matchesQuery && matchesType && matchesColor
        }
    }

    // Split filtered notes into Pinned and Unpinned for Notes screen
    val (pinnedNotes, unpinnedNotes) = remember(filteredNotes, currentSection) {
        if (currentSection is NoteSection.Notes && searchQuery.isBlank() && selectedFilterType == null && selectedFilterColor == null) {
            filteredNotes.partition { it.isPinned }
        } else {
            Pair(emptyList(), filteredNotes)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = "San Sync Icon",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "San Sync",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                NavigationDrawerItem(
                    label = { Text("Notes") },
                    selected = currentSection is NoteSection.Notes,
                    onClick = {
                        viewModel.setSection(NoteSection.Notes)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Lightbulb, contentDescription = "Active Notes") },
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier.padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("To-Do List") },
                    selected = currentSection is NoteSection.ToDoList,
                    onClick = {
                        viewModel.setSection(NoteSection.ToDoList)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.CheckBox, contentDescription = "To-Do Tasks") },
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier.padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Archive") },
                    selected = currentSection is NoteSection.Archive,
                    onClick = {
                        viewModel.setSection(NoteSection.Archive)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Archive, contentDescription = "Archive") },
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier.padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Trash") },
                    selected = currentSection is NoteSection.Trash,
                    onClick = {
                        viewModel.setSection(NoteSection.Trash)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = "Trash Bin") },
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier.padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

                // Labels Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "LABELS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    TextButton(
                        onClick = { showLabelCreatorDialog = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Edit", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Scrollable label links
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allLabels) { label ->
                        NavigationDrawerItem(
                            label = { Text(label.name) },
                            selected = currentSection is NoteSection.LabelFilter && (currentSection as NoteSection.LabelFilter).labelName == label.name,
                            onClick = {
                                viewModel.setSection(NoteSection.LabelFilter(label.name))
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Outlined.Label, contentDescription = "Label: ${label.name}") },
                            shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                            modifier = Modifier.padding(end = 12.dp, top = 2.dp, bottom = 2.dp)
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

                var showThemeDialog by remember { mutableStateOf(false) }
                val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

                val (themeLabel, themeIcon) = when (themeMode) {
                    ThemeMode.SYSTEM -> Pair("Theme: System Default", Icons.Outlined.Settings)
                    ThemeMode.LIGHT -> Pair("Theme: Light Theme", Icons.Outlined.LightMode)
                    ThemeMode.DARK -> Pair("Theme: Dark Theme", Icons.Outlined.DarkMode)
                }

                NavigationDrawerItem(
                    label = { Text(themeLabel) },
                    selected = false,
                    onClick = {
                        showThemeDialog = true
                    },
                    icon = { Icon(themeIcon, contentDescription = "Choose App Theme") },
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier.padding(end = 12.dp, top = 2.dp, bottom = 2.dp)
                )

                var showSoundSettingsDialog by remember { mutableStateOf(false) }
                val currentSoundType by viewModel.soundType.collectAsStateWithLifecycle()
                val currentSoundDuration by viewModel.soundDurationSeconds.collectAsStateWithLifecycle()

                NavigationDrawerItem(
                    label = { Text("Alarm & Playback Settings") },
                    selected = false,
                    onClick = {
                        showSoundSettingsDialog = true
                    },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Alarm Sound Settings") },
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier.padding(end = 12.dp, top = 2.dp, bottom = 2.dp)
                )

                if (showSoundSettingsDialog) {
                    var previewingType by remember { mutableStateOf<String?>(null) }
                    val previewScope = rememberCoroutineScope()

                    AlertDialog(
                        onDismissRequest = { 
                            AlarmSoundManager.stopSound()
                            showSoundSettingsDialog = false 
                        },
                        title = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Alarm & Sounds", fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Choose your custom alert music tone and configure how long it plays when a task reminder goes off.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider()

                                Text(
                                    text = "1. ALARM MUSIC TONE",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                val soundOptions = listOf(
                                    "System Default",
                                    "Sweet Harp Alarm",
                                    "Melodic Synth Chime",
                                    "Classic Beepy Trio",
                                    "Cosmic Radar Alert"
                                )

                                soundOptions.forEach { option ->
                                    val isSelected = currentSoundType == option
                                    val isCurrentlyPreviewing = previewingType == option

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                viewModel.setSoundType(option)
                                                previewingType = option
                                                AlarmSoundManager.playSound(context, option, currentSoundDuration, previewScope)
                                            }
                                            .padding(vertical = 8.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    viewModel.setSoundType(option)
                                                    previewingType = option
                                                    AlarmSoundManager.playSound(context, option, currentSoundDuration, previewScope)
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = option,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = when (option) {
                                                        "System Default" -> "Your standard phone notify sound"
                                                        "Sweet Harp Alarm" -> "Melodic rising acoustic scale"
                                                        "Melodic Synth Chime" -> "Arpeggiating bright synth sweeps"
                                                        "Classic Beepy Trio" -> "Standard sharp alert sound"
                                                        "Cosmic Radar Alert" -> "Space-like wave beep pulse"
                                                        else -> ""
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                if (isCurrentlyPreviewing) {
                                                    AlarmSoundManager.stopSound()
                                                    previewingType = null
                                                } else {
                                                    previewingType = option
                                                    AlarmSoundManager.playSound(context, option, currentSoundDuration, previewScope)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isCurrentlyPreviewing) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                                contentDescription = "Preview Tone",
                                                tint = if (isCurrentlyPreviewing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider()

                                Text(
                                    text = "2. PLAYBACK TIME DURATION",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                val durationOptions = listOf(5, 10, 15, 30)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    durationOptions.forEach { secs ->
                                        val isDurSelected = currentSoundDuration == secs
                                        FilterChip(
                                            selected = isDurSelected,
                                            onClick = {
                                                viewModel.setSoundDurationSeconds(secs)
                                                if (previewingType != null) {
                                                    AlarmSoundManager.playSound(context, previewingType!!, secs, previewScope)
                                                }
                                            },
                                            label = { Text("$secs seconds") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    AlarmSoundManager.stopSound()
                                    showSoundSettingsDialog = false
                                }
                            ) {
                                Text("Save & Apply")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    AlarmSoundManager.stopSound()
                                    showSoundSettingsDialog = false
                                }
                            ) {
                                Text("Close")
                            }
                        }
                    )
                }

                if (showThemeDialog) {
                    AlertDialog(
                        onDismissRequest = { showThemeDialog = false },
                        title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setThemeMode(ThemeMode.LIGHT)
                                            showThemeDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = themeMode == ThemeMode.LIGHT,
                                        onClick = {
                                            viewModel.setThemeMode(ThemeMode.LIGHT)
                                            showThemeDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Light (White) Mode", style = MaterialTheme.typography.bodyLarge)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setThemeMode(ThemeMode.DARK)
                                            showThemeDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = themeMode == ThemeMode.DARK,
                                        onClick = {
                                            viewModel.setThemeMode(ThemeMode.DARK)
                                            showThemeDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setThemeMode(ThemeMode.SYSTEM)
                                            showThemeDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = themeMode == ThemeMode.SYSTEM,
                                        onClick = {
                                            viewModel.setThemeMode(ThemeMode.SYSTEM)
                                            showThemeDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("System Default", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemeDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                // Author Tag & Sync Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Cloud-Sync Ready • College Project",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clickable {
                                try {
                                    uriHandler.openUri("https://a-santhosh-hub.github.io/in/")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "Developed By ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Santhosh",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                            modifier = Modifier.testTag("developed_by_santhosh_link")
                        )
                    }
                }
            }
        }
    ) {
        val isTrashActive = currentSection is NoteSection.Trash

        Scaffold(
            topBar = {
                Column(modifier = Modifier.statusBarsPadding()) {
                    // Google Keep style custom search header
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 12.dp, end = 12.dp, bottom = 4.dp)
                            .testTag("keep_search_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("drawer_menu_button")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = when (currentSection) {
                                            is NoteSection.Notes -> "Search college notes..."
                                            is NoteSection.ToDoList -> "Search tasks..."
                                            is NoteSection.Archive -> "Search archived notes..."
                                            is NoteSection.Trash -> "Search deleted notes..."
                                            is NoteSection.LabelFilter -> "Search Label: ${(currentSection as NoteSection.LabelFilter).labelName}..."
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 8.dp)
                                        .testTag("search_text_input")
                                )
                            }

                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearFilters() }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                                }
                            }

                            IconButton(
                                onClick = { viewModel.toggleGridView() },
                                modifier = Modifier.testTag("layout_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle Grid/List layout"
                                )
                            }
                        }
                    }

                    // Search Category Filter Row
                    AnimatedVisibility(
                        visible = currentSection !is NoteSection.Trash && currentSection !is NoteSection.ToDoList,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedFilterType == SearchFilterType.CHECKLIST,
                                    onClick = { viewModel.selectFilterType(SearchFilterType.CHECKLIST) },
                                    label = { Text("Lists") },
                                    leadingIcon = { Icon(Icons.Default.CheckBox, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedFilterType == SearchFilterType.DRAWING,
                                    onClick = { viewModel.selectFilterType(SearchFilterType.DRAWING) },
                                    label = { Text("Drawings") },
                                    leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedFilterType == SearchFilterType.REMINDER,
                                    onClick = { viewModel.selectFilterType(SearchFilterType.REMINDER) },
                                    label = { Text("Reminders") },
                                    leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            
                            // Color chips to filter
                            items(12) { idx ->
                                val mColors = getFilterColor(idx)
                                val isSelected = selectedFilterColor == idx
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(mColors.first)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else mColors.second,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.selectFilterColor(idx) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected color fitter",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (currentSection is NoteSection.Trash) {
                    // Empty trash prompt
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Notes in Trash are deleted forever.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp
                            )
                            Button(
                                onClick = { viewModel.emptyTrash() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Empty Bin", fontSize = 11.sp)
                            }
                        }
                    }
                } else if (currentSection !is NoteSection.ToDoList) {
                    // Take a Note Action Bar
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(12.dp)
                            .testTag("action_bar_take_note"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clickable { viewModel.startNewNote(false) }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Take a note...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.startNewNote(isChecklist = true) },
                                modifier = Modifier.testTag("take_checklist_button")
                            ) {
                                Icon(Icons.Default.CheckBox, contentDescription = "New Checklist Note")
                            }
                            IconButton(
                                onClick = { viewModel.startNewNote(hasDrawing = true) },
                                modifier = Modifier.testTag("take_sketch_button")
                            ) {
                                Icon(Icons.Default.Brush, contentDescription = "New Drawing Note")
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (currentSection is NoteSection.ToDoList) {
                    ToDoListScreen(viewModel = viewModel)
                } else if (filteredNotes.isEmpty()) {
                    // Modern styled empty state placeholders
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (currentSection) {
                                is NoteSection.Notes -> Icons.Outlined.NoteAlt
                                is NoteSection.ToDoList -> Icons.Outlined.CheckBox
                                is NoteSection.Archive -> Icons.Outlined.Archive
                                is NoteSection.Trash -> Icons.Outlined.Delete
                                is NoteSection.LabelFilter -> Icons.Outlined.Label
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (currentSection) {
                                is NoteSection.Notes -> "Capture college lectures, lists and sketches!"
                                is NoteSection.ToDoList -> "To-Do tasks list"
                                is NoteSection.Archive -> "Archived notes appear here"
                                is NoteSection.Trash -> "Bin is empty"
                                is NoteSection.LabelFilter -> "No notes have this label applied"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Clean styling with local Room Database storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    // Render Notes Feed either as Staggered Grid or Single Scroll List
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isGridView) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                            ) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                        Text(
                                            "PINNED",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                                        )
                                    }
                                    items(pinnedNotes, key = { it.id }) { note ->
                                        NoteCard(
                                            note = note,
                                            onClick = { viewModel.editNote(note) },
                                            onPinClick = { viewModel.togglePinNote(note) },
                                            onArchiveClick = { viewModel.moveNoteToArchive(note, !note.isArchived) },
                                            onTrashClick = { viewModel.moveNoteToTrash(note, true) },
                                            onRestoreClick = { viewModel.restoreNoteFromTrash(note) },
                                            onDeletePermanentClick = { viewModel.deleteNotePermanently(note) },
                                            isTrashSection = isTrashActive,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                        Text(
                                            "OTHERS",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp)
                                        )
                                    }
                                }

                                items(unpinnedNotes, key = { it.id }) { note ->
                                    NoteCard(
                                        note = note,
                                        onClick = { viewModel.editNote(note) },
                                        onPinClick = { viewModel.togglePinNote(note) },
                                        onArchiveClick = { viewModel.moveNoteToArchive(note, !note.isArchived) },
                                        onTrashClick = { viewModel.moveNoteToTrash(note, true) },
                                        onRestoreClick = { viewModel.restoreNoteFromTrash(note) },
                                        onDeletePermanentClick = { viewModel.deleteNotePermanently(note) },
                                        isTrashSection = isTrashActive,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                            ) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item {
                                        Text(
                                            "PINNED",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                                        )
                                    }
                                    items(pinnedNotes, key = { it.id }) { note ->
                                        NoteCard(
                                            note = note,
                                            onClick = { viewModel.editNote(note) },
                                            onPinClick = { viewModel.togglePinNote(note) },
                                            onArchiveClick = { viewModel.moveNoteToArchive(note, !note.isArchived) },
                                            onTrashClick = { viewModel.moveNoteToTrash(note, true) },
                                            onRestoreClick = { viewModel.restoreNoteFromTrash(note) },
                                            onDeletePermanentClick = { viewModel.deleteNotePermanently(note) },
                                            isTrashSection = isTrashActive,
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                        )
                                    }
                                    item {
                                        Text(
                                            "OTHERS",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp)
                                        )
                                    }
                                }

                                items(unpinnedNotes, key = { it.id }) { note ->
                                    NoteCard(
                                        note = note,
                                        onClick = { viewModel.editNote(note) },
                                        onPinClick = { viewModel.togglePinNote(note) },
                                        onArchiveClick = { viewModel.moveNoteToArchive(note, !note.isArchived) },
                                        onTrashClick = { viewModel.moveNoteToTrash(note, true) },
                                        onRestoreClick = { viewModel.restoreNoteFromTrash(note) },
                                        onDeletePermanentClick = { viewModel.deleteNotePermanently(note) },
                                        isTrashSection = isTrashActive,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Label Creator Dialog
    if (showLabelCreatorDialog) {
        LabelManagementDialog(
            labels = allLabels,
            onAddLabel = { viewModel.addLabelToSystem(it) },
            onDeleteLabel = { viewModel.removeLabelFromSystem(it) },
            onUpdateLabel = { id, name -> viewModel.updateLabelInSystem(id, name) },
            onDismiss = { showLabelCreatorDialog = false }
        )
    }

    // Interactive Full-screen Sketch Drawing Canvas
    if (isCanvasOpen) {
        SketchPad(
            initialStrokes = currentCanvasStrokes,
            onSave = { strokes -> viewModel.saveCanvasStrokes(strokes) },
            onCancel = { viewModel.cancelCanvas() }
        )
    }

    // Rich Interactive Note Editor Sheet Dialog
    editingNote?.let { note ->
        NoteEditorDialog(
            note = note,
            availableLabels = allLabels,
            onDismiss = { viewModel.closeNoteEditor(save = true) },
            onDiscard = { viewModel.discardEditingNote() },
            onUpdate = { block -> viewModel.updateEditingNote(block) },
            onTrash = {
                viewModel.moveNoteToTrash(note, true)
                viewModel.closeNoteEditor(save = false)
            },
            onArchive = {
                viewModel.moveNoteToArchive(note, !note.isArchived)
                viewModel.closeNoteEditor(save = false)
            },
            onOpenSketch = { viewModel.openCanvasForEditing() }
        )
    }
}

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onPinClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onTrashClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDeletePermanentClick: () -> Unit,
    isTrashSection: Boolean,
    modifier: Modifier = Modifier
) {
    val (bgColor, contentColor) = getNoteCardColors(note.colorIndex)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor, contentColor = contentColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Pin Button header if not Trashed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (note.title.isNotBlank()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (!isTrashSection) {
                        IconButton(
                            onClick = { onPinClick() },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("pin_button_${note.id}")
                        ) {
                            Icon(
                                imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Note",
                                tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (note.title.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Inline drawing vector brush strokes miniaturization
                if (!note.drawingData.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokes = note.getDrawingStrokes()
                            if (strokes.isNotEmpty()) {
                                var minX = Float.MAX_VALUE
                                var maxX = Float.MIN_VALUE
                                var minY = Float.MAX_VALUE
                                var maxY = Float.MIN_VALUE
                                
                                for (stroke in strokes) {
                                    for (pt in stroke.points) {
                                        if (pt.x < minX) minX = pt.x
                                        if (pt.x > maxX) maxX = pt.x
                                        if (pt.y < minY) minY = pt.y
                                        if (pt.y > maxY) maxY = pt.y
                                    }
                                }
                                val width = maxX - minX
                                val height = maxY - minY
                                if (width > 0 && height > 0) {
                                    val padding = 12f
                                    val scaleX = (size.width - padding * 2) / width
                                    val scaleY = (size.height - padding * 2) / height
                                    val scale = minOf(scaleX, scaleY)

                                    val offsetX = padding + (size.width - padding * 2 - width * scale) / 2 - minX * scale
                                    val offsetY = padding + (size.height - padding * 2 - height * scale) / 2 - minY * scale

                                    for (stroke in strokes) {
                                        if (stroke.points.size > 1) {
                                            val path = Path().apply {
                                                moveTo(noteScale(stroke.points[0].x, scale, offsetX), noteScale(stroke.points[0].y, scale, offsetY))
                                                for (i in 1 until stroke.points.size) {
                                                    lineTo(noteScale(stroke.points[i].x, scale, offsetX), noteScale(stroke.points[i].y, scale, offsetY))
                                                }
                                            }
                                            drawPath(
                                                path = path,
                                                color = Color(AndroidColor.parseColor(stroke.colorHex)),
                                                style = Stroke(
                                                    width = maxOf(1f, stroke.width * scale),
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Render content text or compact checklist format
                if (note.isChecklist) {
                    val items = note.getChecklistItems()
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (item in items.take(4)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (item.isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = item.text,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                                    color = if (item.isChecked) MaterialTheme.colorScheme.outline else contentColor
                                )
                            }
                        }
                        if (items.size > 4) {
                            Text(
                                text = "+ ${items.size - 4} more items",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                            )
                        }
                    }
                } else {
                    if (note.content.isNotBlank()) {
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Labels chip row
                if (note.labels.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (lbl in note.labels.split(",")) {
                            if (lbl.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    modifier = Modifier.height(18.dp)
                                ) {
                                    Text(
                                        text = lbl,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Reminder time tag
                note.reminderTime?.let { rem ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(3.dp))
                            val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            Text(
                                text = formatter.format(Date(rem)),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Quick Action Bar layout on list feed card
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTrashSection) {
                        IconButton(onClick = onRestoreClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore note", modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = onDeletePermanentClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete note permanently", modifier = Modifier.size(14.dp))
                        }
                    } else {
                        IconButton(onClick = onArchiveClick, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (note.isArchived) "Unarchive note" else "Archive note",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onTrashClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Move to Trash", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

fun noteScale(original: Float, scale: Float, offset: Float): Float {
    return original * scale + offset
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorDialog(
    note: Note,
    availableLabels: List<Label>,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    onUpdate: ((Note) -> Note) -> Unit,
    onTrash: () -> Unit,
    onArchive: () -> Unit,
    onOpenSketch: () -> Unit
) {
    var showColorMenu by remember { mutableStateOf(false) }
    var showLabelMenu by remember { mutableStateOf(false) }
    var showReminderMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val (editorBgColor, editorContentColor) = getNoteCardColors(note.colorIndex)

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { onDismiss() }, modifier = Modifier.testTag("editor_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Save and Back")
                        }
                    },
                    title = { },
                    actions = {
                        IconButton(onClick = { onOpenSketch() }, modifier = Modifier.testTag("editor_drawing_button")) {
                            Icon(Icons.Default.Brush, contentDescription = "Add Drawing Sketch")
                        }
                        IconButton(onClick = {
                            val nextChecked = !note.isChecklist
                            onUpdate { current ->
                                if (nextChecked) {
                                    // Plain multiline to JSON Checklist format
                                    val initialItems = current.content.split("\n")
                                        .filter { it.isNotBlank() }
                                        .map { ChecklistItem(text = it, isChecked = false) }
                                    current.copy(
                                        isChecklist = true,
                                        content = ChecklistItem.listToJson(initialItems)
                                    )
                                } else {
                                    // JSON Checklist back to multiline line strings
                                    val plain = current.getChecklistItems().joinToString("\n") { it.text }
                                    current.copy(
                                        isChecklist = false,
                                        content = plain
                                    )
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (note.isChecklist) Icons.Default.Notes else Icons.Default.CheckBox,
                                contentDescription = "Toggle Checklist checkboxes"
                            )
                        }
                        IconButton(onClick = { showColorMenu = true }) {
                            Icon(Icons.Default.Palette, contentDescription = "Change background color")
                        }
                        IconButton(onClick = { showLabelMenu = true }) {
                            Icon(Icons.Default.Label, contentDescription = "Labels selector")
                        }
                        IconButton(onClick = { showReminderMenu = true }) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Reminder timer")
                        }
                        IconButton(onClick = { onArchive() }) {
                            Icon(
                                imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = "Archive note"
                            )
                        }
                        IconButton(onClick = { onTrash() }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete note")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = editorBgColor,
                        navigationIconContentColor = editorContentColor,
                        actionIconContentColor = editorContentColor
                    )
                )
            },
            containerColor = editorBgColor,
            contentColor = editorContentColor
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Large editable note title
                TextField(
                    value = note.title,
                    onValueChange = { newTitle -> onUpdate { it.copy(title = newTitle) } },
                    placeholder = { Text("Title", fontSize = 21.sp, fontWeight = FontWeight.Bold) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = editorContentColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Display drawings inside the editor as big preview if present
                if (!note.drawingData.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            .clickable { onOpenSketch() }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokes = note.getDrawingStrokes()
                            if (strokes.isNotEmpty()) {
                                var minX = Float.MAX_VALUE
                                var maxX = Float.MIN_VALUE
                                var minY = Float.MAX_VALUE
                                var maxY = Float.MIN_VALUE
                                
                                for (stroke in strokes) {
                                    for (pt in stroke.points) {
                                        if (pt.x < minX) minX = pt.x
                                        if (pt.x > maxX) maxX = pt.x
                                        if (pt.y < minY) minY = pt.y
                                        if (pt.y > maxY) maxY = pt.y
                                    }
                                }
                                val width = maxX - minX
                                val height = maxY - minY
                                if (width > 0 && height > 0) {
                                    val padding = 20f
                                    val scaleX = (size.width - padding * 2) / width
                                    val scaleY = (size.height - padding * 2) / height
                                    val scale = minOf(scaleX, scaleY)

                                    val offsetX = padding + (size.width - padding * 2 - width * scale) / 2 - minX * scale
                                    val offsetY = padding + (size.height - padding * 2 - height * scale) / 2 - minY * scale

                                    for (stroke in strokes) {
                                        if (stroke.points.size > 1) {
                                            val path = Path().apply {
                                                moveTo(noteScale(stroke.points[0].x, scale, offsetX), noteScale(stroke.points[0].y, scale, offsetY))
                                                for (i in 1 until stroke.points.size) {
                                                    lineTo(noteScale(stroke.points[i].x, scale, offsetX), noteScale(stroke.points[i].y, scale, offsetY))
                                                }
                                            }
                                            drawPath(
                                                path = path,
                                                color = Color(AndroidColor.parseColor(stroke.colorHex)),
                                                style = Stroke(
                                                    width = maxOf(1.5f, stroke.width * scale),
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        "Tap canvas drawing to edit blueprint sketches",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Plain content vs rich Checklist entries
                if (note.isChecklist) {
                    val allItems = note.getChecklistItems()
                    val (completed, active) = allItems.partition { it.isChecked }
                    
                    var newItemText by remember { mutableStateOf("") }

                    Text(
                        "Checklist",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Active Items
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (item in active) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { chk ->
                                        val modItems = allItems.toMutableList()
                                        val targetIdx = modItems.indexOfFirst { it.id == item.id }
                                        if (targetIdx != -1) {
                                            modItems[targetIdx] = modItems[targetIdx].copy(isChecked = chk)
                                            onUpdate { it.copy(content = ChecklistItem.listToJson(modItems)) }
                                        }
                                    }
                                )
                                TextField(
                                    value = item.text,
                                    onValueChange = { updated ->
                                        val modItems = allItems.toMutableList()
                                        val targetIdx = modItems.indexOfFirst { it.id == item.id }
                                        if (targetIdx != -1) {
                                            modItems[targetIdx] = modItems[targetIdx].copy(text = updated)
                                            onUpdate { it.copy(content = ChecklistItem.listToJson(modItems)) }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = editorContentColor,
                                        unfocusedTextColor = editorContentColor
                                    )
                                )
                                IconButton(onClick = {
                                    val modItems = allItems.filter { it.id != item.id }
                                    onUpdate { it.copy(content = ChecklistItem.listToJson(modItems)) }
                                }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Delete item",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        // Add new entry line
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(14.dp)
                            )
                            TextField(
                                value = newItemText,
                                onValueChange = { newVal ->
                                    newItemText = newVal
                                    if (newVal.contains("\n")) {
                                        val textCleaned = newVal.replace("\n", "")
                                        if (textCleaned.isNotBlank()) {
                                            val modItems = allItems.toMutableList()
                                            modItems.add(ChecklistItem(text = textCleaned, isChecked = false))
                                            onUpdate { it.copy(content = ChecklistItem.listToJson(modItems)) }
                                        }
                                        newItemText = ""
                                    }
                                },
                                placeholder = { Text("List item") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                            if (newItemText.isNotBlank()) {
                                IconButton(onClick = {
                                    val modItems = allItems.toMutableList()
                                    modItems.add(ChecklistItem(text = newItemText.trim(), isChecked = false))
                                    onUpdate { it.copy(content = ChecklistItem.listToJson(modItems)) }
                                    newItemText = ""
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Add Item")
                                }
                            }
                        }
                    }

                    // Completed Items collapsed group
                    if (completed.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        var completedExpanded by remember { mutableStateOf(false) }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { completedExpanded = !completedExpanded }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (completedExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Completed items (${completed.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        if (completedExpanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                for (item in completed) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = item.isChecked,
                                            onCheckedChange = { chk ->
                                                val modItems = allItems.toMutableList()
                                                val targetIdx = modItems.indexOfFirst { it.id == item.id }
                                                if (targetIdx != -1) {
                                                    modItems[targetIdx] = modItems[targetIdx].copy(isChecked = chk)
                                                    onUpdate { it.copy(content = ChecklistItem.listToJson(modItems)) }
                                                }
                                            }
                                        )
                                        Text(
                                            text = item.text,
                                            fontSize = 14.sp,
                                            textDecoration = TextDecoration.LineThrough,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 14.dp)
                                        )
                                        IconButton(onClick = {
                                            val modItems = allItems.filter { it.id != item.id }
                                            onUpdate { it.copy(content = ChecklistItem.listToJson(modItems)) }
                                        }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Delete completed",
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Plain body description notes
                    TextField(
                        value = note.content,
                        onValueChange = { newDesc -> onUpdate { it.copy(content = newDesc) } },
                        placeholder = { Text("Note", fontSize = 16.sp) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            color = editorContentColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_content_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                // Currently applied labels chip display
                if (note.labels.isNotBlank()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Applied Labels", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.outline)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        for (lbl in note.labels.split(",")) {
                            if (lbl.isNotBlank()) {
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        val remain = note.labels.split(",").filter { it != lbl && it.isNotBlank() }.joinToString(",")
                                        onUpdate { it.copy(labels = remain) }
                                    },
                                    label = { Text(lbl, fontSize = 11.sp) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove Tag", modifier = Modifier.size(12.dp)) }
                                )
                            }
                        }
                    }
                }

                // Currently pending reminder display
                note.reminderTime?.let { rem ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Reminders Scheduled", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InputChip(
                            selected = true,
                            onClick = { onUpdate { it.copy(reminderTime = null) } },
                            label = {
                                val formatter = SimpleDateFormat("EEEE, MMM dd, h:mm a", Locale.getDefault())
                                Text(formatter.format(Date(rem)), fontSize = 11.sp)
                            },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear reminder", modifier = Modifier.size(12.dp)) },
                            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )
                    }
                }
            }
        }
    }

    // Color Selector Dropdown/Modal
    if (showColorMenu) {
        AlertDialog(
            onDismissRequest = { showColorMenu = false },
            title = { Text("Note Background Color") },
            text = {
                Column {
                    FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (idx in 0 until 12) {
                            val colors = getFilterColor(idx)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(colors.first)
                                    .border(
                                        width = if (note.colorIndex == idx) 2.dp else 1.dp,
                                        color = if (note.colorIndex == idx) MaterialTheme.colorScheme.primary else colors.second,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onUpdate { it.copy(colorIndex = idx) }
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorMenu = false }) { Text("Ok") }
            }
        )
    }

    // Label multi-selector modal
    if (showLabelMenu) {
        AlertDialog(
            onDismissRequest = { showLabelMenu = false },
            title = { Text("Add labels to note") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (availableLabels.isEmpty()) {
                        item {
                            Text("Create labels in left sidebar drawer.", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                        }
                    }
                    items(availableLabels) { lbl ->
                        val labelsList = note.labels.split(",").filter { it.isNotBlank() }
                        val isApplied = labelsList.contains(lbl.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val nextList = if (isApplied) {
                                        labelsList.filter { it != lbl.name }
                                    } else {
                                        labelsList + lbl.name
                                    }
                                    onUpdate { it.copy(labels = nextList.joinToString(",")) }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isApplied,
                                onCheckedChange = { chk ->
                                    val nextList = if (chk == true) {
                                        labelsList + lbl.name
                                    } else {
                                        labelsList.filter { it != lbl.name }
                                    }
                                    onUpdate { it.copy(labels = nextList.joinToString(",")) }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(lbl.name, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLabelMenu = false }) { Text("Done") }
            }
        )
    }

    // Reminder selector modal
    if (showReminderMenu) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showReminderMenu = false },
            title = { Text("Set College Lecture Reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a quick college alarm trigger to schedule study alarms:", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    Button(
                        onClick = {
                            val time = System.currentTimeMillis() + 10 * 60 * 1000 // 10 minutes
                            onUpdate { it.copy(reminderTime = time) }
                            showReminderMenu = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("In 10 Minutes")
                    }
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 8)
                                set(Calendar.MINUTE, 0)
                            }
                            onUpdate { it.copy(reminderTime = calendar.timeInMillis) }
                            showReminderMenu = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("Tomorrow at 8:00 AM (Lecture)")
                    }
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 7)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                            }
                            onUpdate { it.copy(reminderTime = calendar.timeInMillis) }
                            showReminderMenu = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("Next Week Monday at 9:00 AM")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderMenu = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelManagementDialog(
    labels: List<Label>,
    onAddLabel: (String) -> Unit,
    onDeleteLabel: (Label) -> Unit,
    onUpdateLabel: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var newLabelName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Edit Labels", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // New Label Creator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = newLabelName,
                        onValueChange = { newLabelName = it },
                        placeholder = { Text("Create new label...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (newLabelName.isNotBlank()) {
                            onAddLabel(newLabelName.trim())
                            newLabelName = ""
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Create Label")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Scrollable label list with edit and delete
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    if (labels.isEmpty()) {
                        item {
                            Text("No tags created.", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                        }
                    }
                    items(labels) { lbl ->
                        var editLabelName by remember { mutableStateOf(lbl.name) }
                        var isEditingLabel by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                onDeleteLabel(lbl)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Label", tint = MaterialTheme.colorScheme.error)
                            }

                            if (isEditingLabel) {
                                TextField(
                                    value = editLabelName,
                                    onValueChange = { editLabelName = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    onUpdateLabel(lbl.id, editLabelName)
                                    isEditingLabel = false
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save edit")
                                }
                            } else {
                                Text(
                                    text = lbl.name,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 15.sp
                                )
                                IconButton(onClick = {
                                    isEditingLabel = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit label name")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) { Text("Close") }
        }
    )
}

@Composable
fun SketchPad(
    initialStrokes: List<DrawingStroke>,
    onSave: (List<DrawingStroke>) -> Unit,
    onCancel: () -> Unit
) {
    var strokes by remember { mutableStateOf(initialStrokes) }
    var currentPoints by remember { mutableStateOf(emptyList<DrawingPoint>()) }
    var selectedColor by remember { mutableStateOf("#3F51B5") } // Default indigo
    var brushWidth by remember { mutableStateOf(8f) }
    
    val colors = listOf("#000000", "#3F51B5", "#F44336", "#4CAF50", "#FFEB3B", "#9C27B0", "#FF9800", "#FFF9C4")

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onCancel() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Sketch")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Canvas Sketchpad", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Row {
                    IconButton(onClick = {
                        if (strokes.isNotEmpty()) {
                            strokes = strokes.dropLast(1)
                        }
                    }) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo Drawing")
                    }
                    IconButton(onClick = { strokes = emptyList() }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Canvas")
                    }
                    Button(
                        onClick = { onSave(strokes) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Blueprint")
                    }
                }
            }
        },
        bottomBar = {
            // Colors & Tool selects
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Brush Width Selection slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = brushWidth,
                        onValueChange = { brushWidth = it },
                        valueRange = 2f..24f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Palette brush list circles
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(colors) { col ->
                        val isSelected = selectedColor == col
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(AndroidColor.parseColor(col)))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = col }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .pointerInput(selectedColor, brushWidth) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints = listOf(DrawingPoint(offset.x, offset.y))
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentPoints = currentPoints + DrawingPoint(change.position.x, change.position.y)
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                strokes = strokes + DrawingStroke(selectedColor, brushWidth, currentPoints)
                                currentPoints = emptyList()
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Done strokes drawing
                for (stroke in strokes) {
                    if (stroke.points.size > 1) {
                        val path = Path().apply {
                            moveTo(stroke.points[0].x, stroke.points[0].y)
                            for (i in 1 until stroke.points.size) {
                                lineTo(stroke.points[i].x, stroke.points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = Color(AndroidColor.parseColor(stroke.colorHex)),
                            style = Stroke(
                                width = stroke.width,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    } else if (stroke.points.size == 1) {
                        drawCircle(
                            color = Color(AndroidColor.parseColor(stroke.colorHex)),
                            radius = stroke.width / 2,
                            center = Offset(stroke.points[0].x, stroke.points[0].y)
                        )
                    }
                }

                // Temporary current active stroke
                if (currentPoints.size > 1) {
                    val path = Path().apply {
                        moveTo(currentPoints[0].x, currentPoints[0].y)
                        for (i in 1 until currentPoints.size) {
                            lineTo(currentPoints[i].x, currentPoints[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(AndroidColor.parseColor(selectedColor)),
                        style = Stroke(
                            width = brushWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else if (currentPoints.size == 1) {
                    drawCircle(
                        color = Color(AndroidColor.parseColor(selectedColor)),
                        radius = brushWidth / 2,
                        center = Offset(currentPoints[0].x, currentPoints[0].y)
                    )
                }
            }
        }
    }
}

// Map color presets cleanly for card containers
@Composable
fun getNoteCardColors(colorIndex: Int): Pair<Color, Color> {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val bg = if (isDark) {
        when (colorIndex) {
            1 -> Color(0xFF5C2B29)
            2 -> Color(0xFF5E4917)
            3 -> Color(0xFF615E1C)
            4 -> Color(0xFF2E4D2B)
            5 -> Color(0xFF16504B)
            6 -> Color(0xFF20485F)
            7 -> Color(0xFF1E3A5F)
            8 -> Color(0xFF3F195E)
            9 -> Color(0xFF5B1B47)
            10 -> Color(0xFF423225)
            11 -> Color(0xFF3C4043)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    } else {
        when (colorIndex) {
            1 -> Color(0xFFF28B82)
            2 -> Color(0xFFFBBC04)
            3 -> Color(0xFFFFF475)
            4 -> Color(0xFFCCFF90)
            5 -> Color(0xFFA7FFEB)
            6 -> Color(0xFFCBF0F8)
            7 -> Color(0xFFAECBFA)
            8 -> Color(0xFFD7AEFB)
            9 -> Color(0xFFFDCFE8)
            10 -> Color(0xFFE6C9A8)
            11 -> Color(0xFFE8EAED)
            else -> MaterialTheme.colorScheme.surface
        }
    }

    val contentColor = if (isDark) {
        Color.White
    } else {
        Color(0xFF202124)
    }

    return Pair(bg, contentColor)
}

fun getFilterColor(colorIndex: Int): Pair<Color, Color> {
    val lightColor = when (colorIndex) {
        1 -> Color(0xFFF28B82)
        2 -> Color(0xFFFBBC04)
        3 -> Color(0xFFFFF475)
        4 -> Color(0xFFCCFF90)
        5 -> Color(0xFFA7FFEB)
        6 -> Color(0xFFCBF0F8)
        7 -> Color(0xFFAECBFA)
        8 -> Color(0xFFD7AEFB)
        9 -> Color(0xFFFDCFE8)
        10 -> Color(0xFFE6C9A8)
        11 -> Color(0xFFE8EAED)
        else -> Color.White
    }
    val borderColor = if (colorIndex == 0) Color.LightGray else Color.Transparent
    return Pair(lightColor, borderColor)
}

@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = textStyle,
        modifier = modifier
    )
}

@Composable
fun ToDoListScreen(viewModel: NoteViewModel) {
    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentSelectedSound by viewModel.soundType.collectAsStateWithLifecycle()
    val soundDuration by viewModel.soundDurationSeconds.collectAsStateWithLifecycle()
    val playScope = rememberCoroutineScope()
    
    val taskListNote = remember(activeNotes) {
        activeNotes.find { it.title == "Global Tasks List" && it.isChecklist }
    }
    
    LaunchedEffect(taskListNote) {
        if (taskListNote == null) {
            viewModel.saveNote(
                Note(
                    title = "Global Tasks List",
                    content = "[]",
                    isChecklist = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    val items = remember(taskListNote) {
        taskListNote?.getChecklistItems() ?: emptyList()
    }
    
    // UI state
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var viewMode by remember { mutableStateOf("Timeline") } // "Timeline" or "Category"
    
    var newTaskText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("General") }
    
    var showTimeConfig by remember { mutableStateOf(false) }
    var taskHour by remember { mutableStateOf(8) }
    var taskMinute by remember { mutableStateOf(0) }
    var taskAmPm by remember { mutableStateOf("AM") }
    var reminderOffsetMinutes by remember { mutableStateOf(0) } // 0=None, 3=3m, 5=5m, 10=10m
    var showCustomMinDialog by remember { mutableStateOf(false) }
    var tempCustomMinuteStr by remember { mutableStateOf("") }
    
    var isRoutineEnabled by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")) }
    
    var completedExpanded by remember { mutableStateOf(true) }
    
    var editingItemId by remember { mutableStateOf<String?>(null) }
    var editingItemText by remember { mutableStateOf("") }
    var taskToEdit by remember { mutableStateOf<ChecklistItem?>(null) }
    
    // In-app Alert Notifications
    var activeAlarmTask by remember { mutableStateOf<ChecklistItem?>(null) }
    val triggeredReminderIds = remember { mutableStateMapOf<String, Long>() }
    
    // Current live time for visual display and trigger checks
    var currentLiveTimeStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            currentLiveTimeStr = sdf.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    // Helper functions for time conversions
    fun timeToMinutes(timeStr: String): Int? {
        if (timeStr.isBlank()) return null
        return try {
            val clean = timeStr.trim().uppercase()
            val ampm = if (clean.contains("PM")) "PM" else "AM"
            val parts = clean.replace("AM", "").replace("PM", "").trim().split(":")
            var hr = parts[0].toInt()
            val min = parts[1].toInt()
            if (ampm == "PM" && hr < 12) hr += 12
            if (ampm == "AM" && hr == 12) hr = 0
            hr * 60 + min
        } catch (e: Exception) {
            null
        }
    }

    fun playNotificationSound() {
        AlarmSoundManager.playSound(context, currentSelectedSound, soundDuration, playScope)
    }

    // Checking reminders dynamically every 5 seconds
    LaunchedEffect(items) {
        while (true) {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentMinutes = currentHour * 60 + currentMinute
            val currentHour12 = calendar.get(Calendar.HOUR)
            val currentAmPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            
            // Format to match: "hh:mm AM" (e.g. "06:05 AM")
            val hrFormatted = String.format("%02d", if (currentHour12 == 0) 12 else currentHour12)
            val minFormatted = String.format("%02d", currentMinute)
            
            for (item in items) {
                if (!item.isChecked && item.time.isNotBlank()) {
                    val itemMinutes = timeToMinutes(item.time)
                    if (itemMinutes != null) {
                        val reminderMinutes = itemMinutes - item.reminderOffsetMinutes
                        val key = "${item.id}_${item.time}_${item.reminderOffsetMinutes}"
                        
                        // Fire if within current minute and hasn't triggered today
                        if (currentMinutes == reminderMinutes && !triggeredReminderIds.containsKey(key)) {
                            triggeredReminderIds[key] = System.currentTimeMillis()
                            activeAlarmTask = item
                            playNotificationSound()
                        }
                    }
                }
            }
            kotlinx.coroutines.delay(5000)
        }
    }

    fun updateTaskList(newItems: List<ChecklistItem>) {
        val currentNote = taskListNote ?: Note(
            title = "Global Tasks List",
            isChecklist = true,
            updatedAt = System.currentTimeMillis()
        )
        viewModel.saveNote(
            currentNote.copy(
                content = ChecklistItem.listToJson(newItems),
                updatedAt = System.currentTimeMillis()
            )
        )
        // Refresh the persistent background task reminder service immediately
        com.example.data.TaskReminderService.startService(context)
    }

    fun addTask() {
        if (newTaskText.isBlank()) return
        val updated = items.toMutableList()
        
        val customTimeString = if (showTimeConfig) {
            String.format("%02d:%02d %s", taskHour, taskMinute, taskAmPm)
        } else {
            ""
        }
        
        val isRoutine = isRoutineEnabled
        val routineDaysStr = if (isRoutine) selectedDays.joinToString(",") else ""
        
        updated.add(
            ChecklistItem(
                text = newTaskText.trim(),
                isChecked = false,
                category = selectedCategory,
                time = customTimeString,
                reminderOffsetMinutes = if (showTimeConfig) reminderOffsetMinutes else 0,
                isRoutine = isRoutine,
                routineDays = routineDaysStr
            )
        )
        updateTaskList(updated)
        newTaskText = ""
        showTimeConfig = false
        isRoutineEnabled = false
        selectedDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    fun toggleTask(item: ChecklistItem, isChecked: Boolean) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDateStr = sdfDate.format(Date())
        val updated = items.map {
            if (it.id == item.id) {
                it.copy(
                    isChecked = isChecked,
                    lastCheckedDate = if (isChecked) currentDateStr else ""
                )
            } else {
                it
            }
        }
        updateTaskList(updated)
    }

    fun deleteTask(item: ChecklistItem) {
        val updated = items.filter { it.id != item.id }
        updateTaskList(updated)
    }

    fun updateTaskText(item: ChecklistItem, text: String) {
        val updated = items.map {
            if (it.id == item.id) it.copy(text = text) else it
        }
        updateTaskList(updated)
    }

    fun updateTask(
        item: ChecklistItem,
        text: String,
        category: String,
        time: String,
        reminderOffset: Int,
        isRoutine: Boolean,
        routineDays: String
    ) {
        val updated = items.map {
            if (it.id == item.id) {
                it.copy(
                    text = text,
                    category = category,
                    time = time,
                    reminderOffsetMinutes = reminderOffset,
                    isRoutine = isRoutine,
                    routineDays = routineDays
                )
            } else {
                it
            }
        }
        updateTaskList(updated)
    }

    fun clearAllCompleted() {
        val updated = items.filter { !it.isChecked }
        updateTaskList(updated)
    }

    fun getCategoryIcon(cat: String): ImageVector {
        return when (cat) {
            "Work" -> Icons.Default.BusinessCenter
            "Day to Life" -> Icons.Default.Home
            "General" -> Icons.Default.PushPin
            else -> Icons.Default.Label
        }
    }

    fun getCategoryColor(cat: String): Color {
        return when (cat) {
            "Work" -> Color(0xFF3F51B5) // Custom Deep Indigo
            "Day to Life" -> Color(0xFF10B981) // Emerald Green
            "General" -> Color(0xFFFBBF24) // Gold / Amber
            else -> Color(0xFFEC4899) // Rose
        }
    }

    fun getDayPeriod(timeStr: String): String {
        val minutes = timeToMinutes(timeStr) ?: return "Unscheduled 📅"
        return when (minutes) {
            in 300 until 720 -> "Morning 🌅 (5AM - 12PM)"
            in 720 until 1020 -> "Afternoon ☀️ (12PM - 5PM)"
            in 1020 until 1260 -> "Evening 🌆 (5PM - 9PM)"
            else -> "Night 🌃 (9PM - 5AM)"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Dynamic Active Alarm Banner / Popup
        AnimatedVisibility(
            visible = activeAlarmTask != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            activeAlarmTask?.let { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Alarm Active",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Task Reminder Alert!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = task.text,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(task.category) },
                                    icon = { Icon(getCategoryIcon(task.category), null, modifier = Modifier.size(14.dp)) }
                                )
                                if (task.time.isNotBlank()) {
                                    Text(
                                        text = "@ ${task.time}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            TextButton(
                                onClick = {
                                    toggleTask(task, true)
                                    AlarmSoundManager.stopSound()
                                    activeAlarmTask = null
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFF10B981)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    Text("Mark Done", fontWeight = FontWeight.Bold)
                                }
                            }

                            TextButton(
                                onClick = {
                                    AlarmSoundManager.stopSound()
                                    activeAlarmTask = null
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }

        if (taskToEdit != null) {
            EditTaskDialog(
                item = taskToEdit!!,
                onDismiss = { taskToEdit = null },
                onSave = { txt, cat, tm, offset, isRot, rotDays ->
                    updateTask(taskToEdit!!, txt, cat, tm, offset, isRot, rotDays)
                    taskToEdit = null
                },
                getCategoryIcon = { getCategoryIcon(it) },
                getCategoryColor = { getCategoryColor(it) }
            )
        }

        if (showCustomMinDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showCustomMinDialog = false 
                    tempCustomMinuteStr = ""
                },
                title = { Text("Enter Custom Minutes") },
                text = {
                    Column {
                        Text("Please enter a value between 0 and 59:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempCustomMinuteStr,
                            onValueChange = { newValue ->
                                if (newValue.length <= 2 && newValue.all { it.isDigit() }) {
                                    tempCustomMinuteStr = newValue
                                }
                            },
                            label = { Text("Minutes") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val mins = tempCustomMinuteStr.toIntOrNull()
                            if (mins != null && mins in 0..59) {
                                taskMinute = mins
                            }
                            showCustomMinDialog = false
                            tempCustomMinuteStr = ""
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showCustomMinDialog = false
                        tempCustomMinuteStr = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Live Clock & Stats Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Routine Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Grouping tasks from morning to night",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = currentLiveTimeStr,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Live Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(
                            onClick = {
                                val updated = items.map {
                                    if (it.isRoutine) it.copy(isChecked = false, lastCheckedDate = "") else it
                                }
                                updateTaskList(updated)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Manual Reset Routines",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                val filteredItemsCount = items.filter { selectedCategoryFilter == "All" || it.category == selectedCategoryFilter }.size
                val completedFilteredCount = items.filter { (selectedCategoryFilter == "All" || it.category == selectedCategoryFilter) && it.isChecked }.size
                val progressPercent = if (filteredItemsCount > 0) (completedFilteredCount.toFloat() / filteredItemsCount) else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$completedFilteredCount / $filteredItemsCount Done (${(progressPercent * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }

        // View Mode Toggle Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewMode = "Timeline" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewMode == "Timeline") MaterialTheme.colorScheme.surface else Color.Transparent,
                    contentColor = if (viewMode == "Timeline") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = if (viewMode == "Timeline") ButtonDefaults.buttonElevation(defaultElevation = 1.dp) else null,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                    Text("Daily Timeline 🌅", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { viewMode = "Category" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewMode == "Category") MaterialTheme.colorScheme.surface else Color.Transparent,
                    contentColor = if (viewMode == "Category") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = if (viewMode == "Category") ButtonDefaults.buttonElevation(defaultElevation = 1.dp) else null,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.GridView, null, modifier = Modifier.size(16.dp))
                    Text("Categories 💼", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Horizontal Category Filter Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                val allCount = items.filter { !it.isChecked }.size
                FilterChip(
                    selected = selectedCategoryFilter == "All",
                    onClick = { selectedCategoryFilter = "All" },
                    label = { Text("All ($allCount)") },
                    leadingIcon = { Icon(Icons.Default.FormatListBulleted, null, modifier = Modifier.size(16.dp)) }
                )
            }

            val listCats = listOf("Work", "Day to Life", "General", "Other")
            items(listCats) { cat ->
                val catCount = items.filter { it.category == cat && !it.isChecked }.size
                FilterChip(
                    selected = selectedCategoryFilter == cat,
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text("$cat ($catCount)") },
                    leadingIcon = { Icon(getCategoryIcon(cat), null, modifier = Modifier.size(16.dp), tint = getCategoryColor(cat)) }
                )
            }
        }

        // Expanded/Integrated Task Creator Box Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getCategoryIcon(selectedCategory),
                        contentDescription = null,
                        tint = getCategoryColor(selectedCategory),
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp).size(24.dp)
                    )
                    
                    TextField(
                        value = newTaskText,
                        onValueChange = { newTaskText = it },
                        placeholder = { Text("Add standard task (e.g., gym 10 pushup)", fontSize = 14.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { 
                                addTask() 
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("todo_add_input")
                    )

                    IconButton(
                        onClick = { showTimeConfig = !showTimeConfig },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (showTimeConfig) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Configure Schedule and Reminders"
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { addTask() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("todo_add_submit")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Task",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Expandable Schedule Config Board
                AnimatedVisibility(
                    visible = showTimeConfig,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "1. SELECT CATEGORY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val catOptions = listOf("Work", "Day to Life", "General", "Other")
                            catOptions.forEach { cat ->
                                InputChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    leadingIcon = { Icon(getCategoryIcon(cat), null, modifier = Modifier.size(14.dp), tint = getCategoryColor(cat)) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "2. SET ALARM & SCHEDULE TIME",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Hour selector dialog substitute
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Hour: ", style = MaterialTheme.typography.bodySmall)
                                AssistChip(
                                    onClick = { 
                                        taskHour = if (taskHour >= 12) 1 else taskHour + 1 
                                    },
                                    label = { Text(String.format("%02d", taskHour)) }
                                )
                            }

                            // Minute Selector 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Min: ", style = MaterialTheme.typography.bodySmall)
                                AssistChip(
                                    onClick = { 
                                        taskMinute = if (taskMinute >= 55) 0 else taskMinute + 5 
                                    },
                                    label = { Text(String.format("%02d", taskMinute)) }
                                )
                                IconButton(
                                    onClick = { 
                                        tempCustomMinuteStr = taskMinute.toString()
                                        showCustomMinDialog = true 
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Custom Minutes",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // AM/PM selector
                            AssistChip(
                                onClick = { 
                                    taskAmPm = if (taskAmPm == "AM") "PM" else "AM" 
                                },
                                label = { Text(taskAmPm) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "3. REMINDER NOTIFICATION OFFSET",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val reminderOptions = listOf(
                                0 to "At scheduled time",
                                3 to "3 min before",
                                5 to "5 min before",
                                10 to "10 min before"
                            )
                            reminderOptions.forEach { option ->
                                FilterChip(
                                    selected = reminderOffsetMinutes == option.first,
                                    onClick = { reminderOffsetMinutes = option.first },
                                    label = { Text(option.second, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "4. DAILY ROUTINE / REPEAT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isRoutineEnabled,
                                onCheckedChange = { isRoutineEnabled = it }
                            )
                            Text("Make this a Daily Routine task", style = MaterialTheme.typography.bodyMedium)
                        }

                        if (isRoutineEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select active days:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                weekdays.forEach { day ->
                                    val isSelected = selectedDays.contains(day)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedDays = if (isSelected) {
                                                selectedDays.filter { it != day }
                                            } else {
                                                selectedDays + day
                                            }
                                        },
                                        label = { Text(day, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // List representation
        if (items.isEmpty()) {
            // Empty state placeholder
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckBox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No tasks in $selectedCategoryFilter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add chores, gym entries, and work meetings with custom alarms.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Filter and Sort our active items
            val activeTasksForFilter = items.filter { 
                !it.isChecked && (selectedCategoryFilter == "All" || it.category == selectedCategoryFilter) 
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeTasksForFilter.isNotEmpty()) {
                    if (viewMode == "Timeline") {
                        // Chronological morning-to-night sorting
                        // Group active tasks by period
                        val groupedByPeriod = activeTasksForFilter.groupBy { getDayPeriod(it.time) }
                        
                        // Sort periods chronologically
                        val sortedPeriods = listOf(
                            "Morning 🌅 (5AM - 12PM)",
                            "Afternoon ☀️ (12PM - 5PM)",
                            "Evening 🌆 (5PM - 9PM)",
                            "Night 🌃 (9PM - 5AM)",
                            "Unscheduled 📅"
                        )

                        sortedPeriods.forEach { period ->
                            val tasksInPeriod = groupedByPeriod[period]
                            if (!tasksInPeriod.isNullOrEmpty()) {
                                item {
                                    Text(
                                        text = period.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                                    )
                                }

                                // Sort within period
                                val sortedTasks = tasksInPeriod.sortedWith { a, b ->
                                    val tA = timeToMinutes(a.time) ?: 9999
                                    val tB = timeToMinutes(b.time) ?: 9999
                                    tA.compareTo(tB)
                                }

                                items(sortedTasks, key = { it.id }) { item ->
                                    TaskItemCard(
                                        item = item,
                                        onToggle = { toggleTask(item, it) },
                                        onDelete = { deleteTask(item) },
                                        isEditing = editingItemId == item.id,
                                        editingText = editingItemText,
                                        onEditStart = {
                                            taskToEdit = item
                                        },
                                        onEditChange = { editingItemText = it },
                                        onEditDone = {
                                            if (editingItemText.isNotBlank()) {
                                                updateTaskText(item, editingItemText.trim())
                                            }
                                            editingItemId = null
                                        },
                                        onPlayBeep = {
                                            triggeredReminderIds.remove("${item.id}_${item.time}_${item.reminderOffsetMinutes}")
                                            activeAlarmTask = item
                                            playNotificationSound()
                                        },
                                        getCategoryColor = { getCategoryColor(it) },
                                        getCategoryIcon = { getCategoryIcon(it) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Category Grouped Board view
                        val sortedCategories = listOf("Work", "Day to Life", "General", "Other")
                        sortedCategories.forEach { cat ->
                            val tasksInCat = activeTasksForFilter.filter { it.category == cat }
                            if (tasksInCat.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(getCategoryIcon(cat), null, modifier = Modifier.size(16.dp), tint = getCategoryColor(cat))
                                        Text(
                                            text = cat.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = getCategoryColor(cat)
                                        )
                                    }
                                }

                                items(tasksInCat, key = { it.id }) { item ->
                                    TaskItemCard(
                                        item = item,
                                        onToggle = { toggleTask(item, it) },
                                        onDelete = { deleteTask(item) },
                                        isEditing = editingItemId == item.id,
                                        editingText = editingItemText,
                                        onEditStart = {
                                            taskToEdit = item
                                        },
                                        onEditChange = { editingItemText = it },
                                        onEditDone = {
                                            if (editingItemText.isNotBlank()) {
                                                updateTaskText(item, editingItemText.trim())
                                            }
                                            editingItemId = null
                                        },
                                        onPlayBeep = {
                                            triggeredReminderIds.remove("${item.id}_${item.time}_${item.reminderOffsetMinutes}")
                                            activeAlarmTask = item
                                            playNotificationSound()
                                        },
                                        getCategoryColor = { getCategoryColor(it) },
                                        getCategoryIcon = { getCategoryIcon(it) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Completed tasks (filtered by selected category)
                val completedTasksForCategory = items.filter { 
                    it.isChecked && (selectedCategoryFilter == "All" || it.category == selectedCategoryFilter) 
                }

                if (completedTasksForCategory.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { completedExpanded = !completedExpanded }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (completedExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "COMPLETED (${completedTasksForCategory.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            
                            if (completedExpanded) {
                                TextButton(
                                    onClick = { clearAllCompleted() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Clear All Completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    if (completedExpanded) {
                        items(completedTasksForCategory, key = { item -> item.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { toggleTask(item, it) },
                                        modifier = Modifier.testTag("todo_checkbox_${item.id}")
                                    )
                                    
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 6.dp, horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = item.text,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                            )
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(getCategoryIcon(item.category), null, modifier = Modifier.size(12.dp), tint = getCategoryColor(item.category).copy(alpha = 0.5f))
                                            Text(
                                                text = item.category,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                    
                                    IconButton(
                                        onClick = { deleteTask(item) }
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Task",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItemCard(
    item: ChecklistItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    isEditing: Boolean,
    editingText: String,
    onEditStart: () -> Unit,
    onEditChange: (String) -> Unit,
    onEditDone: () -> Unit,
    onPlayBeep: () -> Unit,
    getCategoryColor: (String) -> Color,
    getCategoryIcon: (String) -> ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onToggle(it) },
                modifier = Modifier.testTag("todo_checkbox_${item.id}")
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                if (isEditing) {
                    TextField(
                        value = editingText,
                        onValueChange = { onEditChange(it) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { onEditDone() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onEditStart() }
                            .padding(vertical = 2.dp)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        // Category tag chip
                        Box(
                            modifier = Modifier
                                .background(
                                    color = getCategoryColor(item.category).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(item.category),
                                    contentDescription = null,
                                    tint = getCategoryColor(item.category),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = item.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = getCategoryColor(item.category)
                                )
                            }
                        }

                        // Routine repeating badge
                        if (item.isRoutine) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = if (item.routineDays.isBlank() || item.routineDays == "Mon,Tue,Wed,Thu,Fri,Sat,Sun") {
                                            "Daily"
                                        } else {
                                            "Routine: ${item.routineDays}"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }

                        // Time schedule marker
                        if (item.time.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = item.time,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (item.reminderOffsetMinutes > 0) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "With reminder offset",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "${item.reminderOffsetMinutes}m before",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isEditing) {
                IconButton(onClick = { onEditDone() }) {
                    Icon(Icons.Default.Check, contentDescription = "Save edit", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                // Instant Alarm Alarm Test button - for users to easily experience the chime sound and alert banner instantly
                IconButton(
                    onClick = { onPlayBeep() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Test Alarm Sound",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onEditStart() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Task",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            IconButton(
                onClick = { onDelete() },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

object AlarmSoundManager {
    private var activeRingtone: android.media.Ringtone? = null
    private var playbackJob: kotlinx.coroutines.Job? = null
    private var toneGenerator: android.media.ToneGenerator? = null

    fun playSound(context: android.content.Context, soundType: String, durationSeconds: Int, scope: kotlinx.coroutines.CoroutineScope) {
        stopSound()
        
        playbackJob = scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                when (soundType) {
                    "System Default" -> {
                        val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                        val r = android.media.RingtoneManager.getRingtone(context, uri)
                        if (r != null) {
                            activeRingtone = r
                            r.play()
                        }
                    }
                    "Sweet Harp Alarm" -> {
                        val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM) ?: 
                                  android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                        val r = android.media.RingtoneManager.getRingtone(context, uri)
                        if (r != null) {
                            activeRingtone = r
                            r.play()
                        }
                    }
                    "Melodic Synth Chime" -> {
                        val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 90)
                        toneGenerator = tg
                        val msEnd = System.currentTimeMillis() + (durationSeconds * 1000)
                        while (System.currentTimeMillis() < msEnd && isActive) {
                            tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 120)
                            kotlinx.coroutines.delay(200)
                            tg.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 100)
                            kotlinx.coroutines.delay(400)
                        }
                    }
                    "Classic Beepy Trio" -> {
                        val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 85)
                        toneGenerator = tg
                        val msEnd = System.currentTimeMillis() + (durationSeconds * 1000)
                        while (System.currentTimeMillis() < msEnd && isActive) {
                            tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 150)
                            kotlinx.coroutines.delay(250)
                            tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 150)
                            kotlinx.coroutines.delay(600)
                        }
                    }
                    "Cosmic Radar Alert" -> {
                        val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 95)
                        toneGenerator = tg
                        val msEnd = System.currentTimeMillis() + (durationSeconds * 1000)
                        while (System.currentTimeMillis() < msEnd && isActive) {
                            tg.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
                            kotlinx.coroutines.delay(1200)
                        }
                    }
                    else -> {
                        val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                        val r = android.media.RingtoneManager.getRingtone(context, uri)
                        if (r != null) {
                            activeRingtone = r
                            r.play()
                        }
                    }
                }

                if (activeRingtone != null) {
                    kotlinx.coroutines.delay(durationSeconds * 1000L)
                    activeRingtone?.stop()
                    activeRingtone = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopSound() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            activeRingtone?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        activeRingtone = null
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        toneGenerator = null
    }
}

@Composable
fun EditTaskDialog(
    item: ChecklistItem,
    onDismiss: () -> Unit,
    onSave: (text: String, category: String, time: String, reminderOffset: Int, isRoutine: Boolean, routineDays: String) -> Unit,
    getCategoryIcon: (String) -> androidx.compose.ui.graphics.vector.ImageVector,
    getCategoryColor: (String) -> androidx.compose.ui.graphics.Color
) {
    var text by remember { mutableStateOf(item.text) }
    var selectedCategory by remember { mutableStateOf(item.category) }
    
    var hasTime by remember { mutableStateOf(item.time.isNotBlank()) }
    var taskHour by remember { mutableStateOf(8) }
    var taskMinute by remember { mutableStateOf(0) }
    var taskAmPm by remember { mutableStateOf("AM") }
    
    var isRoutineEnabled by remember { mutableStateOf(item.isRoutine) }
    var selectedDays by remember {
        mutableStateOf(
            if (item.routineDays.isBlank()) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            } else {
                item.routineDays.split(",")
            }
        )
    }
    
    LaunchedEffect(item.time) {
        if (item.time.isNotBlank()) {
            try {
                val clean = item.time.trim().uppercase()
                val ampm = if (clean.contains("PM")) "PM" else "AM"
                val parts = clean.replace("AM", "").replace("PM", "").trim().split(":")
                taskHour = parts[0].toInt()
                taskMinute = parts[1].toInt()
                taskAmPm = ampm
            } catch (e: Exception) {
                // fallbacks
            }
        }
    }
    
    var reminderOffsetMinutes by remember { mutableStateOf(item.reminderOffsetMinutes) }
    var showCustomMinDialog by remember { mutableStateOf(false) }
    var tempCustomMinuteStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Task Details", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Task Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "SELECT CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    val catOptions = listOf("Work", "Day to Life", "General", "Other")
                    catOptions.forEach { cat ->
                        InputChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            leadingIcon = { 
                                Icon(
                                    imageVector = getCategoryIcon(cat), 
                                    contentDescription = null, 
                                    modifier = Modifier.size(14.dp), 
                                    tint = getCategoryColor(cat)
                                ) 
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SET TIME & ALARM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Switch(
                        checked = hasTime,
                        onCheckedChange = { hasTime = it }
                    )
                }

                if (hasTime) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("Hour: ", style = MaterialTheme.typography.bodySmall)
                            AssistChip(
                                onClick = { 
                                    taskHour = if (taskHour >= 12) 1 else taskHour + 1 
                                },
                                label = { Text(String.format("%02d", taskHour)) }
                            )
                        }

                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("Min: ", style = MaterialTheme.typography.bodySmall)
                            AssistChip(
                                onClick = { 
                                    taskMinute = if (taskMinute >= 55) 0 else taskMinute + 5 
                                },
                                label = { Text(String.format("%02d", taskMinute)) }
                            )
                            IconButton(
                                onClick = { 
                                    tempCustomMinuteStr = taskMinute.toString()
                                    showCustomMinDialog = true 
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Custom Minutes",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        AssistChip(
                            onClick = { 
                                taskAmPm = if (taskAmPm == "AM") "PM" else "AM" 
                            },
                            label = { Text(taskAmPm) }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "REMINDER NOTIFICATION OFFSET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        val reminderOptions = listOf(
                            0 to "At time",
                            3 to "3m early",
                            5 to "5m early",
                            10 to "10m early"
                        )
                        reminderOptions.forEach { option ->
                            FilterChip(
                                selected = reminderOffsetMinutes == option.first,
                                onClick = { reminderOffsetMinutes = option.first },
                                label = { Text(option.second, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DAILY ROUTINE / REPEAT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Switch(
                        checked = isRoutineEnabled,
                        onCheckedChange = { isRoutineEnabled = it }
                    )
                }

                if (isRoutineEnabled) {
                    Text(
                        text = "Select active days:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        weekdays.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedDays = if (isSelected) {
                                        selectedDays.filter { it != day }
                                    } else {
                                        selectedDays + day
                                    }
                                },
                                label = { Text(day, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        val customTimeString = if (hasTime) {
                            String.format("%02d:%02d %s", taskHour, taskMinute, taskAmPm)
                        } else {
                            ""
                        }
                        val routineDaysStr = if (isRoutineEnabled) selectedDays.joinToString(",") else ""
                        onSave(
                            text.trim(),
                            selectedCategory,
                            customTimeString,
                            if (hasTime) reminderOffsetMinutes else 0,
                            isRoutineEnabled,
                            routineDaysStr
                        )
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showCustomMinDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCustomMinDialog = false 
                tempCustomMinuteStr = ""
            },
            title = { Text("Enter Custom Minutes") },
            text = {
                Column {
                    Text("Please enter a value between 0 and 59:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempCustomMinuteStr,
                        onValueChange = { newValue ->
                            if (newValue.length <= 2 && newValue.all { it.isDigit() }) {
                                tempCustomMinuteStr = newValue
                            }
                        },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = tempCustomMinuteStr.toIntOrNull()
                        if (mins != null && mins in 0..59) {
                            taskMinute = mins
                        }
                        showCustomMinDialog = false
                        tempCustomMinuteStr = ""
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCustomMinDialog = false
                    tempCustomMinuteStr = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

