package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // Note Queries
    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY updatedAt DESC")
    fun getTrashedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()

    // Label Queries
    @Query("SELECT * FROM labels ORDER BY name ASC")
    fun getAllLabels(): Flow<List<Label>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLabel(label: Label): Long

    @Delete
    suspend fun deleteLabel(label: Label)

    @Query("UPDATE labels SET name = :newName WHERE id = :labelId")
    suspend fun updateLabel(labelId: Int, newName: String)
}
