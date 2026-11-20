package com.sanden.data.storage

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * **Mode A – Drop Folder Storage**
 *
 * All files remain in the publicly accessible folder `/storage/emulated/0/SanDen/`.
 * A `.nomedia` file is created so that the system media scanner ignores the folder.
 *
 * [processInbox] is a no-op because there is no separate inbox – the root folder
 * IS the vault.
 */
class DropFolderStorage @Inject constructor(
    @ApplicationContext private val context: Context
) : VaultStorage {

    companion object {
        private const val TAG = "DropFolderStorage"
        private const val ROOT_FOLDER_NAME = "SanDen"
        private const val BUFFER_SIZE = 8192
    }

    private val rootDir: File
        get() = File(
            Environment.getExternalStorageDirectory(),
            ROOT_FOLDER_NAME
        )

    // ── VaultStorage implementation ─────────────────────────────────────

    override fun getRootPath(): String = rootDir.absolutePath

    override fun getInternalVaultPath(): String = rootDir.absolutePath

    override suspend fun ensureVaultExists() = withContext(Dispatchers.IO) {
        if (!rootDir.exists()) {
            val created = rootDir.mkdirs()
            Log.i(TAG, "Vault directory created ($created): ${rootDir.absolutePath}")
        }
    }

    override suspend fun ensureNoMedia() = withContext(Dispatchers.IO) {
        val noMedia = File(rootDir, ".nomedia")
        if (!noMedia.exists()) {
            noMedia.createNewFile()
            Log.i(TAG, ".nomedia created at: ${noMedia.absolutePath}")
        }
    }

    override suspend fun importFile(
        sourceUri: Uri,
        destFolder: String,
        progressCallback: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val destDir = resolveDestDir(rootDir, destFolder).also { it.mkdirs() }

            // Resolve file name and size from URI
            val fileName = resolveFileNameFromUri(sourceUri) ?: "imported_${System.currentTimeMillis()}"
            val knownSize = resolveFileSizeFromUri(sourceUri)
            if (knownSize > 0) {
                SafeFileOperations.ensureSufficientSpace(destDir, knownSize)
                    .onFailure { return@withContext Result.failure(it) }
            }

            var destFile = File(destDir, fileName)
            destFile = SafeFileOperations.resolveNameCollision(destFile)

            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open URI: $sourceUri"))

            inputStream.use { input ->
                destFile.outputStream().buffered().use { output ->
                    SafeFileOperations.streamCopy(input, output, knownSize, BUFFER_SIZE, progressCallback)
                }
            }

            progressCallback(1f)
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "importFile(Uri) failed", e)
            Result.failure(e)
        }
    }

    override suspend fun importFile(
        sourcePath: String,
        destFolder: String,
        progressCallback: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Source file does not exist: $sourcePath")
                )
            }

            val destDir = File(rootDir, destFolder).also { it.mkdirs() }
            var destFile = File(destDir, sourceFile.name)
            destFile = SafeFileOperations.resolveNameCollision(destFile)

            SafeFileOperations.ensureSufficientSpace(destDir, sourceFile.length())
                .onFailure { return@withContext Result.failure(it) }

            sourceFile.inputStream().buffered().use { input ->
                destFile.outputStream().buffered().use { output ->
                    SafeFileOperations.streamCopy(
                        input, output, sourceFile.length(), BUFFER_SIZE, progressCallback
                    )
                }
            }

            progressCallback(1f)
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "importFile(path) failed", e)
            Result.failure(e)
        }
    }

    override suspend fun moveFile(
        sourcePath: String,
        destPath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            var dest = File(destPath)
            dest.parentFile?.mkdirs()
            dest = SafeFileOperations.resolveNameCollision(dest)

            SafeFileOperations.safeMoveFile(source, dest)
                .onFailure { return@withContext Result.failure(it) }

            Result.success(dest.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "moveFile failed", e)
            Result.failure(e)
        }
    }

    override suspend fun copyFile(
        sourcePath: String,
        destPath: String,
        progressCallback: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            if (!source.exists()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Source does not exist: $sourcePath")
                )
            }

            var dest = File(destPath)
            dest.parentFile?.mkdirs()
            dest = SafeFileOperations.resolveNameCollision(dest)

            SafeFileOperations.ensureSufficientSpace(
                dest.parentFile ?: dest, source.length()
            ).onFailure { return@withContext Result.failure(it) }

            source.inputStream().buffered().use { input ->
                dest.outputStream().buffered().use { output ->
                    SafeFileOperations.streamCopy(
                        input, output, source.length(), BUFFER_SIZE, progressCallback
                    )
                }
            }

            progressCallback(1f)
            Result.success(dest.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "copyFile failed", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext Result.success(Unit)

            val deleted = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }

            if (deleted) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Failed to delete: $path"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteFile failed", e)
            Result.failure(e)
        }
    }

    override suspend fun renameFile(
        path: String,
        newName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val source = File(path)
            if (!source.exists()) {
                return@withContext Result.failure(
                    IllegalArgumentException("File does not exist: $path")
                )
            }

            var dest = File(source.parentFile, newName)
            dest = SafeFileOperations.resolveNameCollision(dest)

            if (source.renameTo(dest)) {
                Result.success(dest.absolutePath)
            } else {
                Result.failure(IllegalStateException("Rename failed for: $path"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "renameFile failed", e)
            Result.failure(e)
        }
    }

    override suspend fun createFolder(
        parentPath: String,
        name: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val folder = File(parentPath, name)
            if (folder.exists()) {
                return@withContext Result.success(folder.absolutePath)
            }

            if (folder.mkdirs()) {
                Result.success(folder.absolutePath)
            } else {
                Result.failure(IllegalStateException("Failed to create folder: ${folder.absolutePath}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createFolder failed", e)
            Result.failure(e)
        }
    }

    override suspend fun exportToPublic(vaultPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val source = File(vaultPath)
            if (!source.exists()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Vault file does not exist: $vaultPath")
                )
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            downloadsDir.mkdirs()

            var destFile = File(downloadsDir, source.name)
            destFile = SafeFileOperations.resolveNameCollision(destFile)

            SafeFileOperations.ensureSufficientSpace(downloadsDir, source.length())
                .onFailure { return@withContext Result.failure(it) }

            source.inputStream().buffered().use { input ->
                destFile.outputStream().buffered().use { output ->
                    SafeFileOperations.streamCopy(input, output, source.length(), BUFFER_SIZE)
                }
            }

            Log.i(TAG, "Exported to: ${destFile.absolutePath}")
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "exportToPublic failed", e)
            Result.failure(e)
        }
    }

    override fun resolveCollision(path: String): String {
        return SafeFileOperations.resolveNameCollision(File(path)).absolutePath
    }

    /**
     * No-op for Drop Folder mode – the root folder IS the vault.
     */
    override suspend fun processInbox() {
        // No-op: files stay in place
    }

    // ── Private helpers ─────────────────────────────────────────────────

    private fun resolveDestDir(baseDir: File, destFolder: String): File {
        if (destFolder.isBlank()) return baseDir
        val candidate = File(destFolder)
        return if (candidate.isAbsolute) candidate else File(baseDir, destFolder)
    }

    private fun resolveFileSizeFromUri(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                    cursor.getLong(sizeIndex)
                } else {
                    -1L
                }
            } ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }

    private fun resolveFileNameFromUri(uri: Uri): String? {
        // Try to get display name from content resolver
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve file name from URI", e)
            uri.lastPathSegment
        }
    }
}
