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
 * **Mode B – Private Import Storage**
 *
 * New files are dropped into a public inbox at `/storage/emulated/0/SanDen/`.
 * [processInbox] detects new files there and safely moves them into internal
 * app storage at `context.filesDir/vault/`.
 *
 * Move strategy:
 * 1. Try [File.renameTo] (works only on the same volume).
 * 2. If rename fails: streaming copy → CRC32 verification → delete source.
 *    The source is **NEVER** deleted if the copy is incomplete or its checksum
 *    does not match.
 */
class PrivateImportStorage @Inject constructor(
    @ApplicationContext private val context: Context
) : VaultStorage {

    companion object {
        private const val TAG = "PrivateImportStorage"
        private const val ROOT_FOLDER_NAME = "SanDen"
        private const val INTERNAL_VAULT_DIR = "vault"
        private const val BUFFER_SIZE = 8192
    }

    /** Public drop-folder / inbox. */
    private val dropFolder: File
        get() = File(Environment.getExternalStorageDirectory(), ROOT_FOLDER_NAME)

    /** Internal vault directory. */
    private val internalVault: File
        get() = File(context.filesDir, INTERNAL_VAULT_DIR)

    // ── VaultStorage implementation ─────────────────────────────────────

    override fun getRootPath(): String = dropFolder.absolutePath

    override fun getInternalVaultPath(): String = internalVault.absolutePath

    override suspend fun ensureVaultExists() = withContext(Dispatchers.IO) {
        if (!dropFolder.exists()) {
            dropFolder.mkdirs()
            Log.i(TAG, "Drop folder created: ${dropFolder.absolutePath}")
        }
        if (!internalVault.exists()) {
            internalVault.mkdirs()
            Log.i(TAG, "Internal vault created: ${internalVault.absolutePath}")
        }
    }

    override suspend fun ensureNoMedia() = withContext(Dispatchers.IO) {
        // .nomedia in drop folder
        val dropNoMedia = File(dropFolder, ".nomedia")
        if (!dropNoMedia.exists()) {
            dropNoMedia.createNewFile()
            Log.i(TAG, ".nomedia created in drop folder")
        }
        // Internal vault doesn't need .nomedia (already private), but add for safety
        val internalNoMedia = File(internalVault, ".nomedia")
        if (!internalNoMedia.exists()) {
            internalNoMedia.createNewFile()
        }
    }

    override suspend fun importFile(
        sourceUri: Uri,
        destFolder: String,
        progressCallback: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val destDir = resolveDestDir(internalVault, destFolder).also { it.mkdirs() }
            val fileName = resolveFileNameFromUri(sourceUri)
                ?: "imported_${System.currentTimeMillis()}"
            val knownSize = resolveFileSizeFromUri(sourceUri)
            if (knownSize > 0) {
                SafeFileOperations.ensureSufficientSpace(destDir, knownSize)
                    .onFailure { return@withContext Result.failure(it) }
            }

            var destFile = File(destDir, fileName)
            destFile = SafeFileOperations.resolveNameCollision(destFile)

            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(
                    IllegalStateException("Cannot open URI: $sourceUri")
                )

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
                    IllegalArgumentException("Source does not exist: $sourcePath")
                )
            }

            val destDir = File(internalVault, destFolder).also { it.mkdirs() }
            var destFile = File(destDir, sourceFile.name)
            destFile = SafeFileOperations.resolveNameCollision(destFile)

            SafeFileOperations.ensureSufficientSpace(destDir, sourceFile.length())
                .onFailure { return@withContext Result.failure(it) }

            val moveResult = SafeFileOperations.safeMoveFile(sourceFile, destFile, progressCallback)
            moveResult.onFailure { return@withContext Result.failure(it) }

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
                Result.failure(
                    IllegalStateException("Failed to create folder: ${folder.absolutePath}")
                )
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
     * Scans the drop folder for new files and safely moves them into the
     * internal vault. Each file is moved using [SafeFileOperations.safeMoveFile],
     * which guarantees the source is only deleted after CRC32 verification.
     */
    override suspend fun processInbox() = withContext(Dispatchers.IO) {
        val files = dropFolder.listFiles() ?: return@withContext
        var processed = 0

        for (file in files) {
            // Skip .nomedia and directories
            if (file.name == ".nomedia") continue
            if (file.isDirectory) {
                // Recursively process sub-directories
                processInboxDirectory(file, internalVault)
                continue
            }

            try {
                var dest = File(internalVault, file.name)
                dest = SafeFileOperations.resolveNameCollision(dest)

                SafeFileOperations.ensureSufficientSpace(internalVault, file.length())
                    .onFailure {
                        Log.e(TAG, "Insufficient space to import: ${file.name}", it)
                        return@withContext
                    }

                SafeFileOperations.safeMoveFile(file, dest)
                    .onSuccess { processed++ }
                    .onFailure {
                        Log.e(TAG, "Failed to move inbox file: ${file.name}", it)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing inbox file: ${file.name}", e)
            }
        }

        if (processed > 0) {
            Log.i(TAG, "processInbox: moved $processed file(s) to internal vault")
        }
    }

    // ── Private helpers ─────────────────────────────────────────────────

    private fun processInboxDirectory(sourceDir: File, parentDest: File) {
        val destDir = File(parentDest, sourceDir.name).also { it.mkdirs() }
        val children = sourceDir.listFiles() ?: return

        for (child in children) {
            if (child.name == ".nomedia") continue

            if (child.isDirectory) {
                processInboxDirectory(child, destDir)
                continue
            }

            try {
                var dest = File(destDir, child.name)
                dest = SafeFileOperations.resolveNameCollision(dest)

                SafeFileOperations.safeMoveFile(child, dest)
                    .onFailure {
                        Log.e(TAG, "Failed to move: ${child.absolutePath}", it)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error moving: ${child.absolutePath}", e)
            }
        }

        // Remove source directory if empty
        if (sourceDir.listFiles()?.isEmpty() == true) {
            sourceDir.delete()
        }
    }

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
