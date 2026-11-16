package com.sanden.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.sanden.data.db.VaultFileDao
import com.sanden.data.db.VaultFileEntity
import com.sanden.data.db.toDomainModel
import com.sanden.data.db.toDomainModels
import com.sanden.data.db.toEntity
import com.sanden.data.scanner.CategoryDetector
import com.sanden.data.scanner.FileObserverManager
import com.sanden.data.scanner.FileScanner
import com.sanden.data.scanner.ScanDiff
import com.sanden.data.storage.StorageModeManager
import com.sanden.domain.model.FileCategory
import com.sanden.domain.model.FileOperation
import com.sanden.domain.model.ScanResult
import com.sanden.domain.model.StorageMode
import com.sanden.domain.model.VaultFile
import com.sanden.domain.repository.VaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full implementation of domain [VaultRepository].
 *
 * Coordinates Room DB, file scanning, storage strategies (Drop Folder and Private Import),
 * and recursive file observers.
 */
@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val vaultFileDao: VaultFileDao,
    private val storageModeManager: StorageModeManager,
    private val fileScanner: FileScanner,
    private val fileObserverManager: FileObserverManager,
    @ApplicationContext private val context: Context
) : VaultRepository {

    companion object {
        private const val TAG = "VaultRepositoryImpl"
    }

    private val storage get() = storageModeManager.getStorage()

    // ── Queries ────────────────────────────────────────────────────────

    override fun getFilesByCategory(category: FileCategory): Flow<List<VaultFile>> {
        return vaultFileDao.getByCategory(category).map { it.toDomainModels() }
    }

    override fun getFilesInFolder(parentPath: String): Flow<List<VaultFile>> {
        return vaultFileDao.getByParentPath(parentPath).map { it.toDomainModels() }
    }

    override fun searchFiles(query: String): Flow<List<VaultFile>> {
        return vaultFileDao.searchByName(query).map { it.toDomainModels() }
    }

    override fun getCategoryCounts(): Flow<Map<FileCategory, Pair<Int, Long>>> {
        return vaultFileDao.getCategoryCounts().map { countsList ->
            val result = FileCategory.entries.associateWith { 0 to 0L }.toMutableMap()
            for (c in countsList) {
                result[c.category] = (c.count to c.totalSize)
            }
            result
        }
    }

    override fun getAllFolders(): Flow<List<VaultFile>> {
        return vaultFileDao.getAllFolders().map { it.toDomainModels() }
    }

    override suspend fun getFileById(id: Long): VaultFile? = withContext(Dispatchers.IO) {
        vaultFileDao.getById(id)?.toDomainModel()
    }

    override suspend fun getFileByPath(path: String): VaultFile? = withContext(Dispatchers.IO) {
        vaultFileDao.getByPath(path)?.toDomainModel()
    }

    // ── Scanning & Sync ────────────────────────────────────────────────

    override suspend fun scanVault(): ScanResult = withContext(Dispatchers.IO) {
        ensureVaultReady()
        processInbox()

        val vaultPath = storage.getInternalVaultPath()
        val currentFiles = fileScanner.scanDirectoryDirect(vaultPath)
        Log.i(TAG, "Scanned $vaultPath, found ${currentFiles.size} file(s) on disk")

        val oldIndex = vaultFileDao.getAll().map { it.toDomainModel() }
        val diff = ScanDiff(oldIndex, currentFiles).compute()

        // 1. Remove deleted files
        if (diff.removed.isNotEmpty()) {
            val removedPaths = diff.removed.map { it.path }
            removedPaths.chunked(500).forEach { chunk ->
                vaultFileDao.deleteByPaths(chunk)
            }
            Log.i(TAG, "Removed ${diff.removed.size} file(s) from DB")
        }

        // 2. Insert new files
        if (diff.added.isNotEmpty()) {
            val addedEntities = diff.added.map { it.toEntity() }
            vaultFileDao.insertAll(addedEntities)
            Log.i(TAG, "Added ${diff.added.size} file(s) to DB")
        }

        // 3. Update modified files
        if (diff.modified.isNotEmpty()) {
            for (mod in diff.modified) {
                val existing = vaultFileDao.getByPath(mod.path)
                if (existing != null) {
                    vaultFileDao.update(
                        existing.copy(
                            size = mod.size,
                            lastModified = mod.lastModified
                        )
                    )
                } else {
                    vaultFileDao.insert(mod.toEntity())
                }
            }
            Log.i(TAG, "Updated ${diff.modified.size} file(s) in DB")
        }

        diff
    }

    // ── Import Operations ──────────────────────────────────────────────

    override fun importFiles(uris: List<Uri>, destFolder: String): Flow<FileOperation> = flow {
        val total = uris.size.toFloat()
        val targetFolder = destFolder.ifEmpty { storage.getInternalVaultPath() }

        for ((index, uri) in uris.withIndex()) {
            emit(FileOperation.Import(uri = uri, destFolder = targetFolder, progress = index / total))
            try {
                val result = importFile(uri, targetFolder)
                if (result.isFailure) {
                    emit(
                        FileOperation.Import(
                            uri = uri,
                            destFolder = targetFolder,
                            progress = (index + 1) / total,
                            error = result.exceptionOrNull()?.message
                        )
                    )
                } else {
                    emit(FileOperation.Import(uri = uri, destFolder = targetFolder, progress = (index + 1) / total))
                }
            } catch (e: Exception) {
                emit(
                    FileOperation.Import(
                        uri = uri,
                        destFolder = targetFolder,
                        progress = (index + 1) / total,
                        error = e.message
                    )
                )
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun importFile(sourceUri: Uri, destFolder: String): Result<VaultFile> = withContext(Dispatchers.IO) {
        val target = destFolder.ifEmpty { storage.getInternalVaultPath() }
        val importResult = storage.importFile(sourceUri, target) { /* progress */ }
        importResult.fold(
            onSuccess = { importedPath ->
                val vaultFile = buildVaultFileFromPath(importedPath)
                val id = vaultFileDao.insert(vaultFile.toEntity())
                Result.success(vaultFile.copy(id = id))
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun importFile(uriString: String): Result<VaultFile> = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        importFile(uri, storage.getInternalVaultPath())
    }

    // ── Export Operations ──────────────────────────────────────────────

    override fun exportFile(vaultFile: VaultFile): Flow<FileOperation> = flow {
        emit(FileOperation.Export(vaultPath = vaultFile.path, destFolder = "Downloads", progress = 0f))
        val result = storage.exportToPublic(vaultFile.path)
        if (result.isSuccess) {
            emit(FileOperation.Export(vaultPath = vaultFile.path, destFolder = result.getOrNull() ?: "Downloads", progress = 1f))
        } else {
            emit(FileOperation.Export(vaultPath = vaultFile.path, destFolder = "Downloads", progress = 1f, error = result.exceptionOrNull()?.message))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun exportFile(fileId: Long, destination: String): Result<String> = withContext(Dispatchers.IO) {
        val file = vaultFileDao.getById(fileId) ?: return@withContext Result.failure(IllegalArgumentException("File not found"))
        storage.exportToPublic(file.path)
    }

    // ── Delete Operations ──────────────────────────────────────────────

    override fun deleteFiles(files: List<VaultFile>): Flow<FileOperation> = flow {
        val total = files.size.toFloat()
        for ((index, file) in files.withIndex()) {
            emit(FileOperation.Delete(path = file.path, progress = index / total))
            val res = deleteFile(file.path)
            if (res.isFailure) {
                emit(FileOperation.Delete(path = file.path, progress = (index + 1) / total, error = res.exceptionOrNull()?.message))
            } else {
                emit(FileOperation.Delete(path = file.path, progress = (index + 1) / total))
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun deleteFile(fileId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val entity = vaultFileDao.getById(fileId) ?: return@withContext Result.failure(IllegalArgumentException("File not found"))
        deleteFile(entity.path)
    }

    override suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        val deleteResult = storage.deleteFile(path)
        deleteResult.onSuccess {
            val entity = vaultFileDao.getByPath(path)
            if (entity != null) {
                if (entity.isDirectory) {
                    val all = vaultFileDao.getAll()
                    val toDelete = all.filter { it.path == path || it.path.startsWith("$path/") }.map { it.path }
                    toDelete.chunked(500).forEach { vaultFileDao.deleteByPaths(it) }
                } else {
                    vaultFileDao.deleteByPaths(listOf(path))
                }
            }
        }
        deleteResult
    }

    // ── Rename Operations ──────────────────────────────────────────────

    override suspend fun renameFile(file: VaultFile, newName: String): Result<VaultFile> = withContext(Dispatchers.IO) {
        val renameResult = storage.renameFile(file.path, newName)
        renameResult.fold(
            onSuccess = { newPath ->
                val oldEntity = vaultFileDao.getByPath(file.path)
                if (oldEntity != null) {
                    if (oldEntity.isDirectory) {
                        updateDirectoryPaths(file.path, newPath)
                    } else {
                        val updatedFile = buildVaultFileFromPath(newPath)
                        vaultFileDao.update(updatedFile.toEntity().copy(id = oldEntity.id))
                    }
                }
                val resultFile = buildVaultFileFromPath(newPath)
                Result.success(resultFile)
            },
            onFailure = { Result.failure(it) }
        )
    }

    // ── Move Operations ────────────────────────────────────────────────

    override fun moveFiles(files: List<VaultFile>, destFolder: String): Flow<FileOperation> = flow {
        val total = files.size.toFloat()
        for ((index, file) in files.withIndex()) {
            val destPath = File(destFolder, file.name).absolutePath
            emit(FileOperation.Move(source = file.path, dest = destPath, progress = index / total))
            val res = moveFile(file.path, destPath)
            if (res.isFailure) {
                emit(FileOperation.Move(source = file.path, dest = destPath, progress = (index + 1) / total, error = res.exceptionOrNull()?.message))
            } else {
                emit(FileOperation.Move(source = file.path, dest = destPath, progress = (index + 1) / total))
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun moveFile(fileId: Long, destination: String): Result<VaultFile> = withContext(Dispatchers.IO) {
        val entity = vaultFileDao.getById(fileId) ?: return@withContext Result.failure(IllegalArgumentException("File not found"))
        val destPath = if (File(destination).isDirectory) File(destination, entity.name).absolutePath else destination
        moveFile(entity.path, destPath)
    }

    override suspend fun moveFile(sourcePath: String, destPath: String): Result<VaultFile> = withContext(Dispatchers.IO) {
        val moveResult = storage.moveFile(sourcePath, destPath)
        moveResult.fold(
            onSuccess = { newPath ->
                val oldEntity = vaultFileDao.getByPath(sourcePath)
                if (oldEntity != null) {
                    if (oldEntity.isDirectory) {
                        updateDirectoryPaths(sourcePath, newPath)
                    } else {
                        val moved = buildVaultFileFromPath(newPath)
                        vaultFileDao.update(moved.toEntity().copy(id = oldEntity.id))
                    }
                }
                Result.success(buildVaultFileFromPath(newPath))
            },
            onFailure = { Result.failure(it) }
        )
    }

    // ── Create Folder ──────────────────────────────────────────────────

    override suspend fun createFolder(parentPath: String, name: String): Result<VaultFile> = withContext(Dispatchers.IO) {
        val targetParent = parentPath.ifEmpty { storage.getInternalVaultPath() }
        val createResult = storage.createFolder(targetParent, name)
        createResult.fold(
            onSuccess = { folderPath ->
                val vaultFile = buildVaultFileFromPath(folderPath, isDirectory = true)
                val id = vaultFileDao.insert(vaultFile.toEntity())
                Result.success(vaultFile.copy(id = id))
            },
            onFailure = { Result.failure(it) }
        )
    }

    // ── Configuration & Sync ───────────────────────────────────────────

    override fun getVaultRootPath(): String = storage.getRootPath()

    override fun getStorageMode(): StorageMode = storageModeManager.getMode()

    override fun setStorageMode(mode: StorageMode) {
        storageModeManager.setMode(mode)
    }

    override fun resolveCollision(path: String): String = storage.resolveCollision(path)

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observerJob: Job? = null

    override fun startWatching() {
        fileObserverManager.startWatching(storage.getInternalVaultPath())
        observerJob?.cancel()
        observerJob = repoScope.launch {
            fileObserverManager.events.collect { event ->
                Log.d(TAG, "FileObserver event: ${event.eventType} on ${event.path}")
                try {
                    scanVault()
                } catch (e: Exception) {
                    Log.e(TAG, "Auto-scan on file event failed", e)
                }
            }
        }
    }

    override fun stopWatching() {
        observerJob?.cancel()
        observerJob = null
        fileObserverManager.stopWatching()
    }

    override suspend fun processInbox() = withContext(Dispatchers.IO) {
        storage.processInbox()
    }

    override suspend fun ensureVaultReady() = withContext(Dispatchers.IO) {
        storage.ensureVaultExists()
        storage.ensureNoMedia()
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun buildVaultFileFromPath(path: String, isDirectory: Boolean = false): VaultFile {
        val file = File(path)
        val actualIsDir = if (file.exists()) file.isDirectory else isDirectory
        val fileName = file.name
        val category = if (actualIsDir) FileCategory.OTHER else CategoryDetector.detectCategory(fileName)
        val mimeType = if (actualIsDir) null else CategoryDetector.detectMimeType(fileName)

        return VaultFile(
            id = 0L,
            path = file.absolutePath,
            name = fileName,
            parentPath = file.parent ?: "",
            size = if (actualIsDir) 0L else file.length(),
            lastModified = file.lastModified(),
            mimeType = mimeType,
            category = category,
            isDirectory = actualIsDir
        )
    }

    private suspend fun updateDirectoryPaths(oldDirPath: String, newDirPath: String) {
        val allEntities = vaultFileDao.getAll()
        val affected = allEntities.filter { it.path == oldDirPath || it.path.startsWith("$oldDirPath/") }
        for (entity in affected) {
            val updatedPath = entity.path.replaceFirst(oldDirPath, newDirPath)
            val updatedParent = entity.parentPath.replaceFirst(oldDirPath, newDirPath)
            val updatedName = if (entity.path == oldDirPath) File(newDirPath).name else entity.name
            vaultFileDao.update(
                entity.copy(
                    path = updatedPath,
                    parentPath = updatedParent,
                    name = updatedName
                )
            )
        }
    }
}
