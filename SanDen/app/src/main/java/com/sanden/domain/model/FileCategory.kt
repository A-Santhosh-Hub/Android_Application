package com.sanden.domain.model

/**
 * Categorizes vault files by their media/content type.
 */
enum class FileCategory {
    IMAGE,
    AUDIO,
    VIDEO,
    DOCUMENT,
    OTHER;

    companion object {
        /**
         * Resolves a [FileCategory] from a MIME type string.
         */
        fun fromMimeType(mimeType: String?): FileCategory {
            if (mimeType == null) return OTHER
            return when {
                mimeType.startsWith("image/") -> IMAGE
                mimeType.startsWith("audio/") -> AUDIO
                mimeType.startsWith("video/") -> VIDEO
                mimeType.startsWith("text/") ||
                mimeType.startsWith("application/pdf") ||
                mimeType.startsWith("application/msword") ||
                mimeType.startsWith("application/vnd.openxmlformats") ||
                mimeType.startsWith("application/vnd.ms-excel") ||
                mimeType.startsWith("application/vnd.ms-powerpoint") -> DOCUMENT
                else -> OTHER
            }
        }

        /**
         * Resolves a [FileCategory] from a file extension.
         */
        fun fromExtension(extension: String): FileCategory {
            return when (extension.lowercase()) {
                "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic", "heif" -> IMAGE
                "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus" -> AUDIO
                "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp" -> VIDEO
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt",
                "csv", "rtf", "odt", "ods", "odp", "html", "htm", "md" -> DOCUMENT
                else -> OTHER
            }
        }
    }
}
