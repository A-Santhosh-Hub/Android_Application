package com.sanden.data.scanner

import android.webkit.MimeTypeMap
import com.sanden.domain.model.FileCategory

/**
 * Detects file category and MIME type using [MimeTypeMap] with a comprehensive
 * fallback extension map for formats that Android's built-in map doesn't cover.
 */
object CategoryDetector {

    // ── Fallback extension → category map ───────────────────────────────

    private val extensionCategoryMap: Map<String, FileCategory> = buildMap {
        // Image
        listOf(
            "heic", "heif", "webp", "avif", "svg", "raw", "cr2", "nef",
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "ico"
        ).forEach { put(it, FileCategory.IMAGE) }

        // Audio
        listOf(
            "flac", "opus", "ogg", "aac", "wma", "m4a", "wav", "mp3", "aiff", "aif"
        ).forEach { put(it, FileCategory.AUDIO) }

        // Video
        listOf(
            "mkv", "webm", "avi", "mov", "wmv", "flv", "m4v", "3gp", "ts", "mp4",
            "mpeg", "mpg"
        ).forEach { put(it, FileCategory.VIDEO) }

        // Document
        listOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "rtf", "csv", "epub", "mobi",
            "odt", "ods", "odp",
            "html", "htm", "xml", "json", "md", "log"
        ).forEach { put(it, FileCategory.DOCUMENT) }

        // Other (archives & packages)
        listOf(
            "apk", "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "bin", "exe"
        ).forEach { put(it, FileCategory.OTHER) }
    }

    // ── Fallback extension → MIME type map ──────────────────────────────

    private val extensionMimeMap: Map<String, String> = mapOf(
        "heic" to "image/heic",
        "heif" to "image/heif",
        "avif" to "image/avif",
        "svg" to "image/svg+xml",
        "raw" to "image/x-raw",
        "cr2" to "image/x-canon-cr2",
        "nef" to "image/x-nikon-nef",
        "webp" to "image/webp",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "gif" to "image/gif",
        "flac" to "audio/flac",
        "opus" to "audio/opus",
        "ogg" to "audio/ogg",
        "aac" to "audio/aac",
        "wma" to "audio/x-ms-wma",
        "m4a" to "audio/mp4",
        "wav" to "audio/wav",
        "mp3" to "audio/mpeg",
        "aiff" to "audio/aiff",
        "aif" to "audio/aiff",
        "mkv" to "video/x-matroska",
        "webm" to "video/webm",
        "avi" to "video/x-msvideo",
        "mp4" to "video/mp4",
        "mov" to "video/quicktime",
        "flv" to "video/x-flv",
        "m4v" to "video/x-m4v",
        "3gp" to "video/3gpp",
        "ts" to "video/mp2t",
        "pdf" to "application/pdf",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "epub" to "application/epub+zip",
        "mobi" to "application/x-mobipocket-ebook",
        "odt" to "application/vnd.oasis.opendocument.text",
        "ods" to "application/vnd.oasis.opendocument.spreadsheet",
        "odp" to "application/vnd.oasis.opendocument.presentation",
        "txt" to "text/plain",
        "md" to "text/markdown",
        "json" to "application/json",
        "xml" to "application/xml",
        "zip" to "application/zip",
        "7z" to "application/x-7z-compressed",
        "apk" to "application/vnd.android.package-archive",
        "gz" to "application/gzip",
        "bz2" to "application/x-bzip2",
        "xz" to "application/x-xz",
        "iso" to "application/x-iso9660-image"
    )

    /**
     * Detects the [FileCategory] for the given file name.
     * First attempts MIME-based detection, then falls back to the extension map.
     */
    fun detectCategory(fileName: String): FileCategory {
        val extension = getExtension(fileName)

        // 1) Try MimeTypeMap-based category
        val mimeType = try {
            MimeTypeMap.getSingleton()?.getMimeTypeFromExtension(extension)
        } catch (_: Throwable) {
            null
        }

        if (mimeType != null) {
            val mimeCategory = categoryFromMimeType(mimeType)
            if (mimeCategory != null) return mimeCategory
        }

        // 2) Fallback to extension map
        if (extension.isNotEmpty()) {
            extensionCategoryMap[extension]?.let { return it }
        }

        return FileCategory.OTHER
    }

    /**
     * Detects the MIME type for the given file name.
     * Returns null if the type cannot be determined.
     */
    fun detectMimeType(fileName: String): String? {
        val extension = getExtension(fileName)
        if (extension.isEmpty()) return null

        // 1) Try system MimeTypeMap
        val systemMime = try {
            MimeTypeMap.getSingleton()?.getMimeTypeFromExtension(extension)
        } catch (_: Throwable) {
            null
        }
        if (systemMime != null) return systemMime

        // 2) Fallback to our own map
        return extensionMimeMap[extension]
    }

    // ── Private helpers ─────────────────────────────────────────────────

    private fun getExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        if (dotIndex < 0 || dotIndex == fileName.length - 1) return ""
        return fileName.substring(dotIndex + 1).lowercase()
    }

    private fun categoryFromMimeType(mimeType: String): FileCategory? {
        return when {
            mimeType.startsWith("image/") -> FileCategory.IMAGE
            mimeType.startsWith("audio/") -> FileCategory.AUDIO
            mimeType.startsWith("video/") -> FileCategory.VIDEO
            mimeType.startsWith("text/") -> FileCategory.DOCUMENT
            mimeType == "application/pdf" -> FileCategory.DOCUMENT
            mimeType.contains("document") -> FileCategory.DOCUMENT
            mimeType.contains("spreadsheet") -> FileCategory.DOCUMENT
            mimeType.contains("presentation") -> FileCategory.DOCUMENT
            mimeType == "application/json" -> FileCategory.DOCUMENT
            mimeType == "application/xml" -> FileCategory.DOCUMENT
            else -> null
        }
    }
}
