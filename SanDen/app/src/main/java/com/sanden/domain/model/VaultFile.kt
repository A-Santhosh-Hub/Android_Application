package com.sanden.domain.model

/**
 * Represents a single file or directory inside the vault.
 */
data class VaultFile(
    val id: Long,
    val path: String,
    val name: String,
    val parentPath: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val category: FileCategory,
    val isDirectory: Boolean
) {
    // ── Helper properties ──────────────────────────────────────────────

    /** File extension without the leading dot, or empty string if none. */
    val extension: String
        get() {
            val dot = name.lastIndexOf('.')
            return if (dot != -1 && !isDirectory) name.substring(dot + 1).lowercase() else ""
        }

    /**
     * Human-readable file size (e.g. "1.24 MB").
     * Returns "—" for directories.
     */
    val formattedSize: String
        get() {
            if (isDirectory) return "—"
            return when {
                size < 1_024L -> "$size B"
                size < 1_024L * 1_024L -> {
                    val kb = size / 1_024.0
                    "%.2f KB".format(kb)
                }
                size < 1_024L * 1_024L * 1_024L -> {
                    val mb = size / (1_024.0 * 1_024.0)
                    "%.2f MB".format(mb)
                }
                else -> {
                    val gb = size / (1_024.0 * 1_024.0 * 1_024.0)
                    "%.2f GB".format(gb)
                }
            }
        }

    /** `true` when the file is an image. */
    val isImage: Boolean get() = category == FileCategory.IMAGE

    /** `true` when the file is an audio track. */
    val isAudio: Boolean get() = category == FileCategory.AUDIO

    /** `true` when the file is a video. */
    val isVideo: Boolean get() = category == FileCategory.VIDEO

    /** `true` when the file is a document. */
    val isDocument: Boolean get() = category == FileCategory.DOCUMENT
}
