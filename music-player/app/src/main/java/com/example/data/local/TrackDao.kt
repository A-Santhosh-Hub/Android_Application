package com.example.data.local

import androidx.room.*
import com.example.data.model.Track
import com.example.data.model.EqualizerPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    // Track queries
    @Query("SELECT * FROM tracks")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): Track?

    @Query("SELECT * FROM tracks WHERE isDownloaded = 1")
    fun getDownloadedTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1")
    fun getFavoriteTracks(): Flow<List<Track>>

    @Query("DELETE FROM tracks")
    suspend fun clearAllTracks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<Track>)

    @Update
    suspend fun updateTrack(track: Track)

    @Query("UPDATE tracks SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFav: Boolean)

    @Query("UPDATE tracks SET isDownloaded = :isDown, localPath = :path WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, isDown: Boolean, path: String?)

    // Equalizer Preset queries
    @Query("SELECT * FROM equalizer_presets")
    fun getAllPresets(): Flow<List<EqualizerPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqualizerPreset)

    @Query("DELETE FROM equalizer_presets WHERE name = :name")
    suspend fun deletePresetByName(name: String)
}
