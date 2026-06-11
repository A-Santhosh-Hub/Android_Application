package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val url: String,
    val localPath: String? = null,
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val durationMs: Long = 0,
    val artworkHex: String = "#FF22E3", // Neon Magenta default
    val genre: String = "Electronic"
) {
    fun getPlayablePath(appCacheDir: String): String {
        return if (isDownloaded && localPath != null) {
            localPath
        } else {
            url
        }
    }
}
