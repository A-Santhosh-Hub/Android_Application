package com.sanden.data.storage

import android.net.Uri

/**
 * Abstraction over the vault's file-system operations.
 *
 * Two implementations exist:
 * - [DropFolderStorage] – files remain in the public SanDen folder (Mode A).
 * - [PrivateImportStorage] – files are moved from the public inbox to internal
 *   app storage (Mode B).
 */
interface VaultStorage {

    /** Returns the path to the public root folder (e.g. /storage/emulated/0/SanDen/). */
    fun getRootPath(): String

    /** Returns the path where vault files are actually stored (may differ per mode). */
    fun getInternalVaultPath(): String

    /** Ensures the vault directory structure exists on disk. */
    suspend fun ensureVaultExists()

    /** Creates or verifies the .nomedia file in the appropriate directories. */
    suspend fun ensureNoMedia()

    /**
     * Imports a file from a content [Uri] into the vault.
     *
     * @param sourceUri     the content:// or file:// URI to import from
     * @param destFolder    the target sub-folder inside the vault
     * @param progressCallback reports copy progress as a fraction 0f–1f
     * @return [Result] containing the absolute path of the imported file
     */
    suspend fun importFile(
        sourceUri: Uri,
        destFolder: String,
        progressCallback: (Float) -> Unit
    ): Result<String>

    /**
     * Imports a file from a file-system path into the vault.
     *
     * @param sourcePath    absolute path of the source file
     * @param destFolder    the target sub-folder inside the vault
     * @param progressCallback reports copy progress as a fraction 0f–1f
     * @return [Result] containing the absolute path of the imported file
     */
    suspend fun importFile(
        sourcePath: String,
        destFolder: String,
        progressCallback: (Float) -> Unit
    ): Result<String>

    /**
     * Moves a file within the vault.
     *
     * @return [Result] containing the new absolute path
     */
    suspend fun moveFile(sourcePath: String, destPath: String): Result<String>

    /**
     * Copies a file within the vault.
     *
     * @return [Result] containing the absolute path of the copy
     */
    suspend fun copyFile(
        sourcePath: String,
        destPath: String,
        progressCallback: (Float) -> Unit
    ): Result<String>

    /** Deletes a file or directory at [path]. */
    suspend fun deleteFile(path: String): Result<Unit>

    /**
     * Renames a file or directory.
     *
     * @return [Result] containing the new absolute path
     */
    suspend fun renameFile(path: String, newName: String): Result<String>

    /**
     * Creates a new folder.
     *
     * @return [Result] containing the absolute path of the new folder
     */
    suspend fun createFolder(parentPath: String, name: String): Result<String>

    /**
     * Exports a vault file to a publicly accessible location (e.g. Downloads).
     *
     * @return [Result] containing the public path of the exported file
     */
    suspend fun exportToPublic(vaultPath: String): Result<String>

    /**
     * Resolves filename collisions by generating a unique name
     * (e.g. "photo (1).jpg", "photo (2).jpg").
     */
    fun resolveCollision(path: String): String

    /**
     * For Private Import mode: processes the inbox / drop-folder by moving
     * new files into internal storage. No-op for Drop Folder mode.
     */
    suspend fun processInbox()
}
