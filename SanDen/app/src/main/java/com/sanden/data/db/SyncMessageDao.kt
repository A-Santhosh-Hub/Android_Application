package com.sanden.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMessageDao {

    @Query("SELECT * FROM sync_messages ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<SyncMessageEntity>>

    @Query("SELECT * FROM sync_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<SyncMessageEntity>

    @Query("SELECT * FROM sync_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SyncMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: SyncMessageEntity): Long

    @Query("DELETE FROM sync_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_messages")
    suspend fun clearAll()
}
