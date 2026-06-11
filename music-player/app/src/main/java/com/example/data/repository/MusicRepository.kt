package com.example.data.repository

import android.content.Context
import com.example.data.local.TrackDao
import com.example.data.model.Track
import com.example.data.model.EqualizerPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MusicRepository(private val trackDao: TrackDao) {

    val allTracks: Flow<List<Track>> = trackDao.getAllTracks()
    val downloadedTracks: Flow<List<Track>> = trackDao.getDownloadedTracks()
    val favoriteTracks: Flow<List<Track>> = trackDao.getFavoriteTracks()
    val customPresets: Flow<List<EqualizerPreset>> = trackDao.getAllPresets()

    suspend fun seedDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            // Check if seeded tracks exist
            val initialTracks = listOf(
                Track(
                    id = "synth_sunset",
                    title = "Synthwave Sunset",
                    artist = "Arcade Pulse",
                    url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    localPath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    isDownloaded = true,
                    durationMs = 372000,
                    artworkHex = "#FF007F",
                    genre = "Synthwave"
                ),
                Track(
                    id = "cyber_alley",
                    title = "Cyberpunk Alley",
                    artist = "Helix Project",
                    url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    localPath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    isDownloaded = true,
                    durationMs = 423000,
                    artworkHex = "#00F0FF",
                    genre = "Cyberpunk"
                ),
                Track(
                    id = "deep_chill",
                    title = "Deep Space Chill",
                    artist = "Lofi Nebula",
                    url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    localPath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    isDownloaded = true,
                    durationMs = 324000,
                    artworkHex = "#7000FF",
                    genre = "Lofi Ambient"
                ),
                Track(
                    id = "velocity_surge",
                    title = "Velocity Surge",
                    artist = "Neon Highway",
                    url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    localPath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    isDownloaded = true,
                    durationMs = 302000,
                    artworkHex = "#FFEE00",
                    genre = "Tech House"
                ),
                Track(
                    id = "tropical_solitude",
                    title = "Tropical Solitude",
                    artist = "Breeze Walker",
                    url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                    localPath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                    isDownloaded = true,
                    durationMs = 350200,
                    artworkHex = "#FF5E00",
                    genre = "Acoustic Chill"
                ),
                Track(
                    id = "metropolis_dawn",
                    title = "Metropolis Dawn",
                    artist = "Retro Future",
                    url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                    localPath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                    isDownloaded = true,
                    durationMs = 334000,
                    artworkHex = "#00FF66",
                    genre = "Futuristic Synth"
                ),
                Track(
                    id = "pixel_highway",
                    title = "Pixel Highway",
                    artist = "8-Bit Pilot",
                    url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                    localPath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                    isDownloaded = true,
                    durationMs = 391000,
                    artworkHex = "#FF003C",
                    genre = "Chiptune Electro"
                ),
                Track(
                    id = "aura_lounge",
                    title = "Aura Lounge",
                    artist = "Groove Grid",
                    url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                    localPath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                    isDownloaded = true,
                    durationMs = 318000,
                    artworkHex = "#A000FF",
                    genre = "Ambient Groove"
                )
            )

            // Seed default tracks
            trackDao.insertTracks(initialTracks)

            // Seed default presets
            val defaultPresets = listOf(
                EqualizerPreset("Flat", 0f, 0f, 0f, 0f, 0f, false),
                EqualizerPreset("Bass Booster", 10f, 6f, 0f, -2f, -4f, false),
                EqualizerPreset("Vocal Booster", -4f, 1f, 8f, 6f, 2f, false),
                EqualizerPreset("Electronic", 7f, 4f, -1f, 3f, 8f, false),
                EqualizerPreset("Acoustic", 3f, 4f, 2f, 5f, 3f, false),
                EqualizerPreset("Pop Wave", 5f, 2f, -1f, 4f, 6f, false)
            )
            for (p in defaultPresets) {
                trackDao.insertPreset(p)
            }
        }
    }

    suspend fun scanLocalAudioTracks(context: Context): Int {
        return withContext(Dispatchers.IO) {
            val audioList = mutableListOf<Track>()
            val contentResolver = context.contentResolver
            val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            
            val projection = arrayOf(
                android.provider.MediaStore.Audio.Media._ID,
                android.provider.MediaStore.Audio.Media.TITLE,
                android.provider.MediaStore.Audio.Media.ARTIST,
                android.provider.MediaStore.Audio.Media.DATA,
                android.provider.MediaStore.Audio.Media.DURATION
            )
            
            val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${android.provider.MediaStore.Audio.Media.TITLE} ASC"
            
            try {
                contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                    val dataColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                    val durationColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                    
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn).toString()
                        val title = cursor.getString(titleColumn) ?: "Unknown Track"
                        val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                        val path = cursor.getString(dataColumn)
                        val duration = cursor.getLong(durationColumn)
                        
                        val artColors = listOf("#00E5FF", "#FF22E3", "#FFD600", "#00FF66", "#7000FF", "#FF5E00", "#FF003C", "#A000FF")
                        val colorIdx = Math.abs(title.hashCode() % artColors.size)
                        val artworkHex = artColors[colorIdx]
                        
                        if (path != null && File(path).exists()) {
                            audioList.add(
                                Track(
                                    id = id,
                                    title = title,
                                    artist = artist,
                                    url = path,
                                    localPath = path,
                                    isDownloaded = true,
                                    durationMs = duration,
                                    artworkHex = artworkHex,
                                    genre = "Local Audio"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            if (audioList.isNotEmpty()) {
                trackDao.clearAllTracks()
                trackDao.insertTracks(audioList)
            }
            audioList.size
        }
    }

    suspend fun toggleFavorite(trackId: String, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            trackDao.updateFavoriteStatus(trackId, isFavorite)
        }
    }

    suspend fun downloadTrack(context: Context, track: Track, onProgress: (Float) -> Unit = {}): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(track.url)
                val connection = url.openConnection()
                connection.connect()

                val fileLength = connection.contentLength
                val input = BufferedInputStream(url.openStream(), 8192)
                
                val downloadsDir = File(context.filesDir, "downloads")
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                val outputFile = File(downloadsDir, "${track.id}.mp3")
                val output = FileOutputStream(outputFile)

                val data = ByteArray(1024)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        onProgress(total.toFloat() / fileLength.toFloat())
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                // Check file size, if successful update DB
                if (outputFile.exists() && outputFile.length() > 1024) {
                    trackDao.updateDownloadStatus(track.id, true, outputFile.absolutePath)
                    true
                } else {
                    outputFile.delete()
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun deleteDownloadedTrack(context: Context, track: Track): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (track.localPath != null) {
                    val file = File(track.localPath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                trackDao.updateDownloadStatus(track.id, false, null)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun savePreset(preset: EqualizerPreset) {
        withContext(Dispatchers.IO) {
            trackDao.insertPreset(preset)
        }
    }

    suspend fun deletePreset(name: String) {
        withContext(Dispatchers.IO) {
            trackDao.deletePresetByName(name)
        }
    }
}
