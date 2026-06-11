package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Track
import com.example.data.model.EqualizerPreset
import com.example.data.repository.MusicRepository
import com.example.player.AudioPlayerManager
import com.example.player.RepeatMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

class MusicViewModel(
    private val context: Context,
    private val repository: MusicRepository,
    val playerManager: AudioPlayerManager
) : ViewModel() {

    // Themes
    private val _themeMode = MutableStateFlow(AppThemeMode.DARK) // Premium dark default
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // Offline Simulation Toggle
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    // Active Library Search Queries
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    // Database Flows
    val allTracks: StateFlow<List<Track>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val downloadedTracks: StateFlow<List<Track>> = repository.downloadedTracks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteTracks: StateFlow<List<Track>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val equalizerPresets: StateFlow<List<EqualizerPreset>> = repository.customPresets
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Currently selected preset name
    private val _selectedPresetName = MutableStateFlow("Flat")
    val selectedPresetName: StateFlow<String> = _selectedPresetName.asStateFlow()

    // Active Equalizer custom levels
    private val _band60Hz = MutableStateFlow(0f)
    val band60Hz: StateFlow<Float> = _band60Hz.asStateFlow()

    private val _band230Hz = MutableStateFlow(0f)
    val band230Hz: StateFlow<Float> = _band230Hz.asStateFlow()

    private val _band910Hz = MutableStateFlow(0f)
    val band910Hz: StateFlow<Float> = _band910Hz.asStateFlow()

    private val _band4kHz = MutableStateFlow(0f)
    val band4kHz: StateFlow<Float> = _band4kHz.asStateFlow()

    private val _band14kHz = MutableStateFlow(0f)
    val band14kHz: StateFlow<Float> = _band14kHz.asStateFlow()

    // Download state maps (trackId -> progress percentage 0.0 to 1.0)
    private val _downloadProgressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Float>> = _downloadProgressMap.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Set<String>>(emptySet())
    val activeDownloads: StateFlow<Set<String>> = _activeDownloads.asStateFlow()

    // UI feedback toast/error events
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Sync-tracked Live lyrics flows for the active track
    private val _currentTrackLyrics = MutableStateFlow<List<com.example.data.model.LyricLine>>(emptyList())
    val currentTrackLyrics: StateFlow<List<com.example.data.model.LyricLine>> = _currentTrackLyrics.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    // Player manager exposed items
    val currentTrack = playerManager.currentTrack
    val isPlaying = playerManager.isPlaying
    val playbackProgress = playerManager.progressMs
    val trackDuration = playerManager.durationMs
    val repeatMode = playerManager.repeatMode
    val shuffleEnabled = playerManager.shuffleEnabled
    val playbackSpeed = playerManager.playbackSpeed
    val visualizerFlow = playerManager.visualizerFlow

    init {
        viewModelScope.launch {
            // Seed database
            repository.seedDatabaseIfEmpty()
            
            // Gather initial custom/preloaded presets
            val defaultPreset = equalizerPresets.value.find { it.name == "Flat" }
            defaultPreset?.let { applyPreset(it) }
        }

        // Automatically fetch real lyrics when the current playing track changes
        viewModelScope.launch {
            currentTrack.collect { track ->
                _currentTrackLyrics.value = emptyList() // clear list on track change
                if (track != null) {
                    loadLyricsForCurrentTrack()
                }
            }
        }
    }

    fun loadLyricsForCurrentTrack() {
        val track = currentTrack.value ?: return
        viewModelScope.launch {
            _isLyricsLoading.value = true
            try {
                val result = com.example.data.repository.LyricsProvider.fetchLiveLyrics(track.title, track.artist)
                _currentTrackLyrics.value = result
            } catch (e: Exception) {
                _currentTrackLyrics.value = com.example.data.repository.LyricsProvider.getLyricsForTrack(track.title, track.artist)
            } finally {
                _isLyricsLoading.value = false
            }
        }
    }

    // Set search and filters
    fun scanDeviceStorage() {
        viewModelScope.launch {
            try {
                val scannedCount = repository.scanLocalAudioTracks(context)
                if (scannedCount > 0) {
                    _toastEvent.emit("Successfully loaded $scannedCount local tracks!")
                } else {
                    _toastEvent.emit("No audio tracks found. Showing default library tracks.")
                }
            } catch (e: Exception) {
                _toastEvent.emit("Scan failed: ${e.message}")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedGenre(genre: String?) {
        _selectedGenre.value = genre
    }

    fun toggleTheme() {
        _themeMode.value = when (_themeMode.value) {
            AppThemeMode.DARK -> AppThemeMode.LIGHT
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.SYSTEM -> AppThemeMode.DARK
        }
    }

    fun toggleOfflineSimulation() {
        _isOfflineMode.value = !_isOfflineMode.value
        viewModelScope.launch {
            if (_isOfflineMode.value) {
                _toastEvent.emit("Offline Streaming Mode Enabled")
                // If current track is NOT downloaded, stop playing it
                val track = currentTrack.value
                if (track != null && !track.isDownloaded) {
                    playerManager.stop()
                    _toastEvent.emit("Stopped playback: Track not available offline")
                }
            } else {
                _toastEvent.emit("Online Mode Restored")
            }
        }
    }

    fun playTrackFromQueue(track: Track, sourceList: List<Track>) {
        if (_isOfflineMode.value && !track.isDownloaded) {
            viewModelScope.launch {
                _toastEvent.emit("Cannot stream in offline mode. Please download first!")
            }
            return
        }
        playerManager.setPlaylist(sourceList, track.id)
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val newFav = !track.isFavorite
            repository.toggleFavorite(track.id, newFav)
            // Update current track state if it is the one being favorited
            if (currentTrack.value?.id == track.id) {
                playerManager.currentTrack.value?.let {
                    // Update state flow manually in manager if needed
                }
            }
        }
    }

    fun startDownload(track: Track) {
        if (_activeDownloads.value.contains(track.id)) return

        viewModelScope.launch {
            _activeDownloads.value = _activeDownloads.value + track.id
            _downloadProgressMap.value = _downloadProgressMap.value + (track.id to 0.05f)
            _toastEvent.emit("Downloading '${track.title}' for offline play...")

            val success = repository.downloadTrack(context, track) { progress ->
                _downloadProgressMap.value = _downloadProgressMap.value + (track.id to progress.coerceIn(0.05f, 0.99f))
            }

            _activeDownloads.value = _activeDownloads.value - track.id
            _downloadProgressMap.value = _downloadProgressMap.value - track.id

            if (success) {
                _toastEvent.emit("'${track.title}' available offline!")
            } else {
                _toastEvent.emit("Failed to download '${track.title}'")
            }
        }
    }

    fun removeDownload(track: Track) {
        viewModelScope.launch {
            val deleted = repository.deleteDownloadedTrack(context, track)
            if (deleted) {
                _toastEvent.emit("Deleted offline files for '${track.title}'")
                // If deleted while offline and playing, stop playback
                if (_isOfflineMode.value && currentTrack.value?.id == track.id) {
                    playerManager.stop()
                }
            }
        }
    }

    // Equalizer sliders updates
    fun updateBand(bandIndex: Int, value: Float) {
        val bounded = value.coerceIn(-15f, 15f)
        when (bandIndex) {
            0 -> _band60Hz.value = bounded
            1 -> _band230Hz.value = bounded
            2 -> _band910Hz.value = bounded
            3 -> _band4kHz.value = bounded
            4 -> _band14kHz.value = bounded
        }
        _selectedPresetName.value = "Custom"
        pushEqualizerToPlayer()
    }

    private fun pushEqualizerToPlayer() {
        val bands = listOf(
            _band60Hz.value,
            _band230Hz.value,
            _band910Hz.value,
            _band4kHz.value,
            _band14kHz.value
        )
        playerManager.updateEqualizerBands(bands)
    }

    fun applyPreset(preset: EqualizerPreset) {
        _selectedPresetName.value = preset.name
        _band60Hz.value = preset.band60Hz
        _band230Hz.value = preset.band230Hz
        _band910Hz.value = preset.band910Hz
        _band4kHz.value = preset.band4kHz
        _band14kHz.value = preset.band14kHz
        pushEqualizerToPlayer()
    }

    fun saveCustomPreset(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val custom = EqualizerPreset(
                name = name,
                band60Hz = _band60Hz.value,
                band230Hz = _band230Hz.value,
                band910Hz = _band910Hz.value,
                band4kHz = _band4kHz.value,
                band14kHz = _band14kHz.value,
                isCustom = true
            )
            repository.savePreset(custom)
            _selectedPresetName.value = name
            _toastEvent.emit("Preset '$name' Saved")
        }
    }

    fun deletePreset(preset: EqualizerPreset) {
        viewModelScope.launch {
            repository.deletePreset(preset.name)
            if (_selectedPresetName.value == preset.name) {
                applyPreset(EqualizerPreset("Flat", 0f, 0f, 0f, 0f, 0f, false))
            }
            _toastEvent.emit("Preset '${preset.name}' Deleted")
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}

// ViewModel Factory representation
class MusicViewModelFactory(
    private val context: Context,
    private val repository: MusicRepository,
    private val playerManager: AudioPlayerManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MusicViewModel(context, repository, playerManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
