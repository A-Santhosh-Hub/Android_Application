package com.sanden.data.scanner

import com.sanden.domain.model.ScanResult
import com.sanden.domain.model.VaultFile

/**
 * Snapshot of a file on disk, captured during a scan.
 */
data class FileSnapshot(
    val path: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean
)

/**
 * Computes the diff between the previously-indexed vault files and the files
 * currently present on disk, identifying added, removed, and modified entries.
 *
 * Uses [path] as the unique key. A file is considered modified if its [size]
 * or [lastModified] has changed. Unchanged files are NOT re-indexed.
 */
class ScanDiff(
    private val oldIndex: List<VaultFile>,
    private val currentFiles: List<VaultFile>
) {

    /**
     * Computes and returns a [ScanResult] describing what changed.
     */
    fun compute(): ScanResult {
        val oldMap: Map<String, VaultFile> = oldIndex.associateBy { it.path }
        val currentMap: Map<String, VaultFile> = currentFiles.associateBy { it.path }

        val oldPaths = oldMap.keys
        val currentPaths = currentMap.keys

        // Paths that exist now but were not in the old index
        val addedPaths: Set<String> = currentPaths - oldPaths

        // Paths that were in the old index but no longer exist on disk
        val removedPaths: Set<String> = oldPaths - currentPaths

        // Paths that exist in both – check for modifications
        val commonPaths: Set<String> = oldPaths.intersect(currentPaths)
        val modifiedList = mutableListOf<VaultFile>()

        for (path in commonPaths) {
            val old = oldMap[path]!!
            val current = currentMap[path]!!
            if (old.size != current.size || old.lastModified != current.lastModified) {
                modifiedList.add(current)
            }
        }

        val addedList: List<VaultFile> = addedPaths.mapNotNull { currentMap[it] }
        val removedList: List<VaultFile> = removedPaths.mapNotNull { oldMap[it] }

        // Totals include everything currently on disk
        val totalFiles = currentFiles.count { !it.isDirectory }
        val totalSize = currentFiles.filter { !it.isDirectory }.sumOf { it.size }

        return ScanResult(
            added = addedList,
            removed = removedList,
            modified = modifiedList,
            totalFiles = totalFiles,
            totalSize = totalSize
        )
    }
}
