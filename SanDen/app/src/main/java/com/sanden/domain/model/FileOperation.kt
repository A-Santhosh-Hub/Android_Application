package com.sanden.domain.model

import android.net.Uri

/**
 * Represents a file-system operation that may be in-progress, completed, or failed.
 *
 * Each subclass carries its own contextual data. All share:
 * - [progress] – completion fraction in the range 0 f .. 1 f.
 * - [isCancelled] – `true` when the user cancelled the operation.
 * - [error] – non-null when the operation failed.
 */
sealed class FileOperation {
    abstract val progress: Float
    abstract val isCancelled: Boolean
    abstract val error: String?

    data class Copy(
        val source: String,
        val dest: String,
        override val progress: Float = 0f,
        override val isCancelled: Boolean = false,
        override val error: String? = null
    ) : FileOperation()

    data class Move(
        val source: String,
        val dest: String,
        override val progress: Float = 0f,
        override val isCancelled: Boolean = false,
        override val error: String? = null
    ) : FileOperation()

    data class Delete(
        val path: String,
        override val progress: Float = 0f,
        override val isCancelled: Boolean = false,
        override val error: String? = null
    ) : FileOperation()

    data class Rename(
        val oldPath: String,
        val newName: String,
        override val progress: Float = 0f,
        override val isCancelled: Boolean = false,
        override val error: String? = null
    ) : FileOperation()

    data class Import(
        val uri: Uri,
        val destFolder: String,
        override val progress: Float = 0f,
        override val isCancelled: Boolean = false,
        override val error: String? = null
    ) : FileOperation()

    data class Export(
        val vaultPath: String,
        val destFolder: String,
        override val progress: Float = 0f,
        override val isCancelled: Boolean = false,
        override val error: String? = null
    ) : FileOperation()
}
