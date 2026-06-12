package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val activeNotes: Flow<List<Note>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotes()
    val trashedNotes: Flow<List<Note>> = noteDao.getTrashedNotes()
    val allLabels: Flow<List<Label>> = noteDao.getAllLabels()

    suspend fun getNoteById(id: Int): Note? {
        return noteDao.getNoteById(id)
    }

    suspend fun insertNote(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    suspend fun insertLabel(label: Label): Long {
        return noteDao.insertLabel(label)
    }

    suspend fun deleteLabel(label: Label) {
        noteDao.deleteLabel(label)
    }

    suspend fun updateLabel(labelId: Int, newName: String) {
        noteDao.updateLabel(labelId, newName)
    }
}
