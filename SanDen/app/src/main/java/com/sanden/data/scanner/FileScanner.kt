package com.sanden.data.scanner

import android.util.Log
import com.sanden.domain.model.VaultFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Progress update emitted while a scan is in progress.
 */
data class ScanProgress(
    val scannedCount: Int,
    val currentPath: String,
    val filesFound: List<VaultFile>
)

/**
 * Recursively scans a directory and builds [VaultFile] objects for every
 * file and sub-directory found. Emits [ScanProgress] updates as it traverses
 * the tree, and runs entirely on [Dispatchers.IO].
 *
 * Unreadable files and directories are logged and skipped gracefully.
 */
class FileScanner @Inject constructor() {

    companion object {
        private const val TAG = "FileScanner"
        private const val PROGRESS_EMIT_INTERVAL = 50 // emit progress every N files
    }

    /**
     * Recursively scans [rootPath] and returns the complete list of [VaultFile]s found.
     * Guaranteed to return all files without flow abortion or truncation.
     */
    suspend fun scanDirectoryDirect(rootPath: String): List<VaultFile> = withContext(Dispatchers.IO) {
        val rootDir = File(rootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) {
            Log.w(TAG, "Root path does not exist or is not a directory: $rootPath")
            return@withContext emptyList()
        }

        val allFiles = mutableListOf<VaultFile>()
        collectFiles(rootDir, rootPath, allFiles)
        allFiles
    }

    /**
     * Scans [rootPath] recursively and emits a [ScanProgress] for every batch
     * of files discovered. The final emission contains the complete list.
     */
    fun scan(rootPath: String): Flow<ScanProgress> = flow {
        val rootDir = File(rootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) {
            Log.w(TAG, "Root path does not exist or is not a directory: $rootPath")
            emit(ScanProgress(scannedCount = 0, currentPath = rootPath, filesFound = emptyList()))
            return@flow
        }

        val allFiles = mutableListOf<VaultFile>()
        var scannedCount = 0

        scanDirectory(rootDir, rootPath, allFiles) { count, path ->
            scannedCount = count
            // Emit progress at intervals
            if (count % PROGRESS_EMIT_INTERVAL == 0) {
                emit(
                    ScanProgress(
                        scannedCount = count,
                        currentPath = path,
                        filesFound = allFiles.toList()
                    )
                )
            }
        }

        // Final emission with complete results
        emit(
            ScanProgress(
                scannedCount = scannedCount,
                currentPath = rootPath,
                filesFound = allFiles.toList()
            )
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Returns a flat list of [FileSnapshot]s for every file/directory under [rootPath].
     * Useful for building diffs.
     */
    fun snapshotDirectory(rootPath: String): List<FileSnapshot> {
        val rootDir = File(rootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return emptyList()

        val snapshots = mutableListOf<FileSnapshot>()
        collectSnapshots(rootDir, snapshots)
        return snapshots
    }

    // ── Private helpers ─────────────────────────────────────────────────

    private suspend fun scanDirectory(
        dir: File,
        rootPath: String,
        results: MutableList<VaultFile>,
        onProgress: suspend (count: Int, currentPath: String) -> Unit
    ) {
        val children = try {
            dir.listFiles()
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied reading directory: ${dir.absolutePath}", e)
            null
        }

        if (children == null) {
            Log.w(TAG, "Cannot list directory (null): ${dir.absolutePath}")
            return
        }

        for (child in children) {
            try {
                // Skip .nomedia files from the vault file list
                if (child.name == ".nomedia") continue

                val vaultFile = buildVaultFile(child, rootPath)
                results.add(vaultFile)
                onProgress(results.size, child.absolutePath)

                if (child.isDirectory) {
                    scanDirectory(child, rootPath, results, onProgress)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error processing file: ${child.absolutePath}", e)
                // Skip unreadable files and continue
            }
        }
    }

    private fun collectFiles(dir: File, rootPath: String, results: MutableList<VaultFile>) {
        val children = try {
            dir.listFiles()
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied reading directory: ${dir.absolutePath}", e)
            null
        } ?: return

        for (child in children) {
            try {
                if (child.name == ".nomedia") continue

                val vaultFile = buildVaultFile(child, rootPath)
                results.add(vaultFile)

                if (child.isDirectory) {
                    collectFiles(child, rootPath, results)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting file: ${child.absolutePath}", e)
            }
        }
    }

    private fun buildVaultFile(file: File, rootPath: String): VaultFile {
        val absolutePath = file.absolutePath
        val parentPath = file.parent ?: rootPath
        val isDir = file.isDirectory
        val fileName = file.name

        val category = if (isDir) {
            com.sanden.domain.model.FileCategory.OTHER
        } else {
            CategoryDetector.detectCategory(fileName)
        }

        val mimeType = if (isDir) null else CategoryDetector.detectMimeType(fileName)

        return VaultFile(
            id = 0L, // Will be assigned by Room on insert
            path = absolutePath,
            name = fileName,
            parentPath = parentPath,
            size = if (isDir) 0L else file.length(),
            lastModified = file.lastModified(),
            mimeType = mimeType,
            category = category,
            isDirectory = isDir
        )
    }

    private fun collectSnapshots(dir: File, snapshots: MutableList<FileSnapshot>) {
        val children = try {
            dir.listFiles()
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied listing: ${dir.absolutePath}", e)
            null
        } ?: return

        for (child in children) {
            try {
                if (child.name == ".nomedia") continue

                snapshots.add(
                    FileSnapshot(
                        path = child.absolutePath,
                        size = if (child.isDirectory) 0L else child.length(),
                        lastModified = child.lastModified(),
                        isDirectory = child.isDirectory
                    )
                )

                if (child.isDirectory) {
                    collectSnapshots(child, snapshots)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error snapshotting: ${child.absolutePath}", e)
            }
        }
    }
}
