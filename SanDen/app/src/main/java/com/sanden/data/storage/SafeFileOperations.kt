package com.sanden.data.storage

import android.os.StatFs
import android.util.Log
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32

/**
 * Utility object for safe, streaming file operations that handle multi-GB files
 * without loading them entirely into memory.
 */
object SafeFileOperations {

    private const val TAG = "SafeFileOperations"
    private const val DEFAULT_BUFFER_SIZE = 8192

    /**
     * Copies data from [input] to [output] using a streaming buffer.
     *
     * @param input           source stream
     * @param output          destination stream
     * @param totalSize       total expected bytes (used for progress; pass -1 if unknown)
     * @param bufferSize      copy buffer size in bytes
     * @param onProgress      callback with progress fraction 0f–1f (called only when totalSize > 0)
     * @return total number of bytes actually copied
     */
    fun streamCopy(
        input: InputStream,
        output: OutputStream,
        totalSize: Long,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        onProgress: (Float) -> Unit = {}
    ): Long {
        val buffer = ByteArray(bufferSize)
        var totalCopied = 0L
        var bytesRead: Int

        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
            totalCopied += bytesRead

            if (totalSize > 0) {
                onProgress((totalCopied.toFloat() / totalSize).coerceAtMost(1f))
            }
        }

        output.flush()
        return totalCopied
    }

    /**
     * Safely moves a file from [source] to [dest].
     *
     * Strategy:
     * 1. Try [File.renameTo] (atomic, same-volume only).
     * 2. If rename fails: streaming copy → verify integrity → delete source.
     *    Source is **never** deleted if the copy is incomplete or corrupt.
     *
     * @return [Result.success] or [Result.failure] with a descriptive exception
     */
    fun safeMoveFile(
        source: File,
        dest: File,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> {
        if (!source.exists()) {
            return Result.failure(IllegalArgumentException("Source does not exist: ${source.absolutePath}"))
        }

        // Ensure parent directory exists
        dest.parentFile?.mkdirs()

        // 1) Fast path: atomic rename
        if (source.renameTo(dest)) {
            onProgress(1f)
            return Result.success(Unit)
        }

        // 2) Slow path: copy + verify + delete
        return try {
            source.inputStream().buffered().use { input ->
                dest.outputStream().buffered().use { output ->
                    streamCopy(input, output, source.length(), onProgress = onProgress)
                }
            }

            if (!verifyFileCopy(source, dest)) {
                // Clean up partial/corrupt copy
                dest.delete()
                return Result.failure(
                    IllegalStateException("Copy verification failed for: ${source.absolutePath}")
                )
            }

            // Safe to delete source
            if (!source.delete()) {
                Log.w(TAG, "Could not delete source after verified copy: ${source.absolutePath}")
                // Not a fatal error – the file was successfully copied
            }

            Result.success(Unit)
        } catch (e: Exception) {
            // Clean up partial copy if it exists
            if (dest.exists() && !verifyFileCopy(source, dest)) {
                dest.delete()
            }
            Result.failure(e)
        }
    }

    /**
     * Verifies that [dest] is an identical copy of [source] by checking file size
     * and CRC32 checksum.
     */
    fun verifyFileCopy(source: File, dest: File): Boolean {
        if (!source.exists() || !dest.exists()) return false
        if (source.length() != dest.length()) {
            Log.w(TAG, "Size mismatch: source=${source.length()}, dest=${dest.length()}")
            return false
        }

        val sourceCrc = calculateCRC32(source)
        val destCrc = calculateCRC32(dest)

        if (sourceCrc != destCrc) {
            Log.w(TAG, "CRC32 mismatch: source=$sourceCrc, dest=$destCrc")
            return false
        }

        return true
    }

    /**
     * Calculates the CRC32 checksum of the given file.
     */
    fun calculateCRC32(file: File): Long {
        val crc = CRC32()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        file.inputStream().buffered().use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                crc.update(buffer, 0, bytesRead)
            }
        }
        return crc.value
    }

    /**
     * Generates a unique file name to avoid collisions.
     *
     * Given `/path/to/photo.jpg`, produces `/path/to/photo (1).jpg`,
     * then `/path/to/photo (2).jpg`, etc. until a name is found that
     * does not exist on disk.
     */
    fun resolveNameCollision(file: File): File {
        if (!file.exists()) return file

        val parent = file.parentFile ?: return file
        val nameWithoutExt = file.nameWithoutExtension
        val extension = file.extension
        val dotExt = if (extension.isNotEmpty()) ".$extension" else ""

        var counter = 1
        var candidate: File

        do {
            candidate = File(parent, "$nameWithoutExt ($counter)$dotExt")
            counter++
        } while (candidate.exists())

        return candidate
    }

    /**
     * Returns available free space in bytes at the given [path].
     */
    fun getAvailableSpace(path: File): Long {
        return try {
            val stat = StatFs(path.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query available space for: ${path.absolutePath}", e)
            -1L
        }
    }

    /**
     * Checks whether [path] has at least [requiredBytes] of free space.
     *
     * @return [Result.success] if sufficient space is available;
     *         [Result.failure] with an [InsufficientStorageException] otherwise.
     */
    fun ensureSufficientSpace(path: File, requiredBytes: Long): Result<Unit> {
        val available = getAvailableSpace(path)
        if (available < 0) {
            return Result.failure(
                IllegalStateException("Unable to determine available space at: ${path.absolutePath}")
            )
        }
        if (available < requiredBytes) {
            return Result.failure(
                InsufficientStorageException(
                    required = requiredBytes,
                    available = available,
                    path = path.absolutePath
                )
            )
        }
        return Result.success(Unit)
    }
}

/**
 * Thrown when there is not enough storage space for a requested operation.
 */
class InsufficientStorageException(
    val required: Long,
    val available: Long,
    val path: String
) : Exception(
    "Insufficient storage at $path: required ${required / 1_048_576} MB, " +
        "available ${available / 1_048_576} MB"
)
