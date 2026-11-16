package com.sanden.domain.model

/**
 * Result of a vault scan operation, summarising what changed since the last scan.
 */
data class ScanResult(
    /** Files discovered for the first time. */
    val added: List<VaultFile>,

    /** Files that were previously indexed but are no longer on disk. */
    val removed: List<VaultFile>,

    /** Files whose size or last-modified timestamp changed. */
    val modified: List<VaultFile>,

    /** Total number of files currently in the vault after the scan. */
    val totalFiles: Int,

    /** Total size in bytes of all files currently in the vault. */
    val totalSize: Long
)
