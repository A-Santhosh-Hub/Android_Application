package com.sanden.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sanden.domain.model.FileCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultFileDao {

    // ── Flow-based queries (reactive) ───────────────────────────────────

    @Query("SELECT * FROM vault_files WHERE category = :category ORDER BY name ASC")
    fun getByCategory(category: FileCategory): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE parentPath = :parentPath ORDER BY isDirectory DESC, name ASC")
    fun getByParentPath(parentPath: String): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByName(query: String): Flow<List<VaultFileEntity>>

    @Query(
        """
        SELECT category, COUNT(*) AS count, COALESCE(SUM(size), 0) AS totalSize 
        FROM vault_files 
        WHERE isDirectory = 0 
        GROUP BY category
        """
    )
    fun getCategoryCounts(): Flow<List<CategoryCount>>

    @Query("SELECT * FROM vault_files WHERE isDirectory = 1 ORDER BY path ASC")
    fun getAllFolders(): Flow<List<VaultFileEntity>>

    // ── Suspend / blocking queries ──────────────────────────────────────

    @Query("SELECT * FROM vault_files WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VaultFileEntity?

    @Query("SELECT * FROM vault_files WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): VaultFileEntity?

    @Query("SELECT * FROM vault_files")
    suspend fun getAll(): List<VaultFileEntity>

    // ── Inserts ─────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<VaultFileEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VaultFileEntity): Long

    // ── Update ──────────────────────────────────────────────────────────

    @Update
    suspend fun update(entity: VaultFileEntity)

    // ── Deletes ─────────────────────────────────────────────────────────

    @Query("DELETE FROM vault_files WHERE path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    @Query("DELETE FROM vault_files")
    suspend fun deleteAll()
}
