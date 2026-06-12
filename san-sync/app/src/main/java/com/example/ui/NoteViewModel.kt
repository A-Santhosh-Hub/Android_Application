package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class NoteSection {
    object Notes : NoteSection()
    object ToDoList : NoteSection()
    object Archive : NoteSection()
    object Trash : NoteSection()
    data class LabelFilter(val labelName: String) : NoteSection()
}

enum class SearchFilterType {
    CHECKLIST, DRAWING, REMINDER
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class NoteViewModel(private val application: android.app.Application, private val repository: NoteRepository) : ViewModel() {

    private val prefs = application.getSharedPreferences("SanSyncPrefs", android.content.Context.MODE_PRIVATE)

    // Theme state
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Main Flows from DB
    val activeNotes: StateFlow<List<Note>> = repository.activeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<Note>> = repository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<Note>> = repository.trashedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLabels: StateFlow<List<Label>> = repository.allLabels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Configuration States
    private val _currentSection = MutableStateFlow<NoteSection>(NoteSection.Notes)
    val currentSection: StateFlow<NoteSection> = _currentSection.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Search and Filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilterType = MutableStateFlow<SearchFilterType?>(null)
    val selectedFilterType: StateFlow<SearchFilterType?> = _selectedFilterType.asStateFlow()

    private val _selectedFilterColor = MutableStateFlow<Int?>(null)
    val selectedFilterColor: StateFlow<Int?> = _selectedFilterColor.asStateFlow()

    // Note composition / editing
    private val _editingNote = MutableStateFlow<Note?>(null)
    val editingNote: StateFlow<Note?> = _editingNote.asStateFlow()

    // Sketch Canvas state
    private val _isCanvasOpen = MutableStateFlow(false)
    val isCanvasOpen: StateFlow<Boolean> = _isCanvasOpen.asStateFlow()

    private val _currentCanvasStrokes = MutableStateFlow<List<DrawingStroke>>(emptyList())
    val currentCanvasStrokes: StateFlow<List<DrawingStroke>> = _currentCanvasStrokes.asStateFlow()

    // Initialize default labels if any on first boot
    init {
        viewModelScope.launch {
            repository.allLabels.first().let { labels ->
                if (labels.isEmpty()) {
                    repository.insertLabel(Label(name = "Personal"))
                    repository.insertLabel(Label(name = "Work"))
                    repository.insertLabel(Label(name = "College"))
                }
            }
        }
    }

    // Actions
    fun setSection(section: NoteSection) {
        _currentSection.value = section
        // Clear search parameters when switching sections
        clearFilters()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    // Sound alert preferences
    private val _soundType = MutableStateFlow(prefs.getString("soundType", "System Default") ?: "System Default")
    val soundType: StateFlow<String> = _soundType.asStateFlow()

    private val _soundDurationSeconds = MutableStateFlow(prefs.getInt("soundDurationSeconds", 5))
    val soundDurationSeconds: StateFlow<Int> = _soundDurationSeconds.asStateFlow()

    fun setSoundType(type: String) {
        _soundType.value = type
        prefs.edit().putString("soundType", type).apply()
    }

    fun setSoundDurationSeconds(seconds: Int) {
        _soundDurationSeconds.value = seconds
        prefs.edit().putInt("soundDurationSeconds", seconds).apply()
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFilterType(type: SearchFilterType?) {
        _selectedFilterType.value = if (_selectedFilterType.value == type) null else type
    }

    fun selectFilterColor(colorIndex: Int?) {
        _selectedFilterColor.value = if (_selectedFilterColor.value == colorIndex) null else colorIndex
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedFilterType.value = null
        _selectedFilterColor.value = null
    }

    // Compose workflows
    fun startNewNote(isChecklist: Boolean = false, hasDrawing: Boolean = false) {
        val blankNote = Note(
            id = 0,
            isChecklist = isChecklist,
            colorIndex = 0,
            updatedAt = System.currentTimeMillis()
        )
        if (hasDrawing) {
            _currentCanvasStrokes.value = emptyList()
            _isCanvasOpen.value = true
        } else {
            _editingNote.value = blankNote
        }
    }

    fun editNote(note: Note) {
        _editingNote.value = note
        if (note.drawingData != null) {
            _currentCanvasStrokes.value = note.getDrawingStrokes()
        } else {
            _currentCanvasStrokes.value = emptyList()
        }
    }

    fun closeNoteEditor(save: Boolean = true) {
        val note = _editingNote.value
        if (note != null && save) {
            saveNote(note)
        }
        _editingNote.value = null
        _currentCanvasStrokes.value = emptyList()
    }

    fun discardEditingNote() {
        _editingNote.value = null
        _currentCanvasStrokes.value = emptyList()
    }

    fun saveNote(note: Note) {
        viewModelScope.launch {
            if (note.title.isNotBlank() || note.content.isNotBlank() || !note.drawingData.isNullOrBlank()) {
                val finalNote = note.copy(updatedAt = System.currentTimeMillis())
                if (finalNote.id == 0) {
                    repository.insertNote(finalNote)
                } else {
                    repository.updateNote(finalNote)
                }
            } else if (note.id != 0) {
                // Keep-style: Empty existing notes are removed to prevent blank entries
                repository.deleteNote(note)
            }
        }
    }

    fun updateEditingNote(updateBlock: (Note) -> Note) {
        _editingNote.value?.let { current ->
            _editingNote.value = updateBlock(current)
        }
    }

    // Drawing actions
    fun openCanvasForEditing() {
        _editingNote.value?.let { note ->
            _currentCanvasStrokes.value = note.getDrawingStrokes()
        } ?: run {
            _currentCanvasStrokes.value = emptyList()
        }
        _isCanvasOpen.value = true
    }

    fun saveCanvasStrokes(strokes: List<DrawingStroke>) {
        _currentCanvasStrokes.value = strokes
        val jsonStrokes = if (strokes.isEmpty()) null else DrawingStroke.listToJson(strokes)
        
        _editingNote.value?.let { note ->
            // Update existing active editing note
            updateEditingNote { it.copy(drawingData = jsonStrokes) }
        } ?: run {
            // No active editing note; create one with this drawing
            _editingNote.value = Note(
                id = 0,
                colorIndex = 0,
                drawingData = jsonStrokes,
                updatedAt = System.currentTimeMillis()
            )
        }
        _isCanvasOpen.value = false
    }

    fun cancelCanvas() {
        _isCanvasOpen.value = false
        // If we opened canvas without note context and cancel, just restore state
    }

    // Direct Quick Actions from Note cards
    fun togglePinNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun moveNoteToArchive(note: Note, archive: Boolean = true) {
        viewModelScope.launch {
            repository.updateNote(
                note.copy(
                    isArchived = archive,
                    isPinned = if (archive) false else note.isPinned,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun moveNoteToTrash(note: Note, trash: Boolean = true) {
        viewModelScope.launch {
            repository.updateNote(
                note.copy(
                    isTrashed = trash,
                    isPinned = if (trash) false else note.isPinned,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun restoreNoteFromTrash(note: Note) {
        moveNoteToTrash(note, false)
    }

    fun deleteNotePermanently(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    // Label workflows
    fun addLabelToSystem(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertLabel(Label(name = name.trim()))
        }
    }

    fun removeLabelFromSystem(label: Label) {
        viewModelScope.launch {
            repository.deleteLabel(label)
            // also remove this label association from all notes
            _editingNote.value?.let { editing ->
                val remaining = editing.labels.split(",")
                    .filter { it != label.name && it.isNotBlank() }
                    .joinToString(",")
                updateEditingNote { it.copy(labels = remaining) }
            }
        }
    }

    fun updateLabelInSystem(labelId: Int, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateLabel(labelId, newName.trim())
        }
    }

    class Factory(private val application: android.app.Application, private val repository: NoteRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NoteViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
