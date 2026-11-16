package com.sanden.domain.repository

import android.net.Uri
import com.sanden.domain.model.FileCategory
import com.sanden.domain.model.FileOperation
import com.sanden.domain.model.ScanResult
import com.sanden.domain.model.StorageMode
import com.sanden.domain.model.VaultFile
import kotlinx.coroutines.flow.Flow

/**
 * Core repository contract for all vault file operations.
 *
 * Implementations handle the actual file-system I/O, database persistence
 * and progress reporting. All long-running operations expose a [Flow] of
 * [FileOperation] so callers can observe progress and cancellation.
 */
interface VaultRepository {

    // ── Queries ────────────────────────────────────────────────────────

    /** Observe all files that belong to the given [category]. */
    fun getFilesByCategory(category: FileCategory): Flow<List<VaultFile>>

    /** Observe the immediate children of [parentPath]. */
    fun getFilesInFolder(parentPath: String): Flow<List<VaultFile>>

    /** Search file names (and optionally content metadata) for [query]. */
    fun searchFiles(query: String): Flow<List<VaultFile>>

    /**
     * Observe per-category statistics.
     * The map value is a [Pair] of **(fileCount, totalSizeBytes)**.
     */
    fun getCategoryCounts(): Flow<Map<FileCategory, Pair<Int, Long>>>

    /** Observe all directories in the vault. */
    fun getAllFolders(): Flow<List<VaultFile>>

    /** Return a single [VaultFile] by its database [id], or `null`. */
    suspend fun getFileById(id: Long): VaultFile?

    /** Return a single [VaultFile] by its [path], or `null`. */
    suspend fun getFileByPath(path: String): VaultFile?

    // ── Mutations ──────────────────────────────────────────────────────

    /** Full vault scan – reconciles the database with the file system. */
    suspend fun scanVault(): ScanResult

    /**
     * Import one or more content-URIs into [destFolder] inside the vault.
     * Emits progress updates as each file is copied.
     */
    fun importFiles(uris: List<Uri>, destFolder: String): Flow<FileOperation>

    /** Single file URI import convenience method. */
    suspend fun importFile(sourceUri: Uri, destFolder: String = ""): Result<VaultFile>

    /** Single file URI string import convenience method. */
    suspend fun importFile(uriString: String): Result<VaultFile>

    /** Export a [vaultFile] to the device's public/shared storage. */
    fun exportFile(vaultFile: VaultFile): Flow<FileOperation>

    /** Export a file by its database ID to [destination]. */
    suspend fun exportFile(fileId: Long, destination: String): Result<String>

    /** Delete the given [files] from the vault. */
    fun deleteFiles(files: List<VaultFile>): Flow<FileOperation>

    /** Delete a file by database ID. */
    suspend fun deleteFile(fileId: Long): Result<Unit>

    /** Delete a file by path. */
    suspend fun deleteFile(path: String): Result<Unit>

    /** Rename [file] to [newName]. Returns the updated [VaultFile]. */
    suspend fun renameFile(file: VaultFile, newName: String): Result<VaultFile>

    /** Move [files] into [destFolder]. Emits progress per file. */
    fun moveFiles(files: List<VaultFile>, destFolder: String): Flow<FileOperation>

    /** Move a single file by ID into [destination]. */
    suspend fun moveFile(fileId: Long, destination: String): Result<VaultFile>

    /** Move a file by source path into [destPath]. */
    suspend fun moveFile(sourcePath: String, destPath: String): Result<VaultFile>

    /** Create a new sub-folder under [parentPath] with the given [name]. */
    suspend fun createFolder(parentPath: String, name: String): Result<VaultFile>

    // ── Configuration & Sync ───────────────────────────────────────────

    /** Absolute path of the vault root directory. */
    fun getVaultRootPath(): String

    /** Current storage mode. */
    fun getStorageMode(): StorageMode

    /** Switch the storage mode. */
    fun setStorageMode(mode: StorageMode)

    /**
     * Given a [path] that may already exist, return a collision-free variant
     * (e.g. appending " (1)", " (2)" etc.).
     */
    fun resolveCollision(path: String): String

    /** Start watching for file changes. */
    fun startWatching()

    /** Stop watching for file changes. */
    fun stopWatching()

    /** Process drop folder inbox (Private Import mode). */
    suspend fun processInbox()

    /** Ensure vault directories and .nomedia exist. */
    suspend fun ensureVaultReady()
}
