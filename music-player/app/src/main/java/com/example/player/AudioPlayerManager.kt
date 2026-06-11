package com.example.player

import android.content.Context
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import com.example.data.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class RepeatMode {
    OFF, ONE, ALL
}

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progressMs = MutableStateFlow(0L)
    val progressMs: StateFlow<Long> = _progressMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // 0 = Low, 1 = Low-Mid, 2 = Mid, 3 = Mid-High, 4 = High
    private var currentBandLevels = mutableListOf(0f, 0f, 0f, 0f, 0f)

    // Flow for real-time visualizer floating spectrum values
    private val _visualizerFlow = MutableStateFlow(List(16) { 0.1f })
    val visualizerFlow: StateFlow<List<Float>> = _visualizerFlow.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var visualizerJob: Job? = null

    private var playlist: List<Track> = emptyList()
    private var originalPlaylist: List<Track> = emptyList()

    init {
        startProgressUpdates()
        startVisualizerAnimation()
    }

    fun setPlaylist(tracks: List<Track>, playTrackId: String? = null) {
        originalPlaylist = tracks
        playlist = if (_shuffleEnabled.value) {
            tracks.shuffled()
        } else {
            tracks
        }
        
        playTrackId?.let { trackId ->
            val trackToPlay = originalPlaylist.find { it.id == trackId }
            trackToPlay?.let { playTrack(it) }
        }
    }

    fun playTrack(track: Track) {
        coroutineScope.launch {
            try {
                releaseMediaPlayer()
                
                _currentTrack.value = track
                val playablePath = track.getPlayablePath(context.filesDir.absolutePath)
                
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(playablePath)
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        setPlaybackSpeedInternal(_playbackSpeed.value)
                        mp.start()
                        _isPlaying.value = true
                        _durationMs.value = mp.duration.toLong()
                        initializeEqualizer(mp.audioSessionId)
                    }
                    setOnCompletionListener {
                        handleTrackCompletion()
                    }
                    setOnErrorListener { _, what, extra ->
                        _isPlaying.value = false
                        false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isPlaying.value = false
            }
        }
    }

    private fun releaseMediaPlayer() {
        try {
            equalizer?.release()
            equalizer = null
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.start()
            _isPlaying.value = true
        }
    }

    fun stop() {
        mediaPlayer?.pause()
        _isPlaying.value = false
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _progressMs.value = positionMs
    }

    fun nextTrack() {
        val current = _currentTrack.value ?: return
        if (playlist.isEmpty()) return

        val currentIndex = playlist.indexOfFirst { it.id == current.id }
        if (currentIndex != -1) {
            val nextIndex = (currentIndex + 1) % playlist.size
            playTrack(playlist[nextIndex])
        } else {
            playTrack(playlist[0])
        }
    }

    fun prevTrack() {
        val current = _currentTrack.value ?: return
        if (playlist.isEmpty()) return

        val currentIndex = playlist.indexOfFirst { it.id == current.id }
        if (currentIndex != -1) {
            val prevIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
            playTrack(playlist[prevIndex])
        } else {
            playTrack(playlist[0])
        }
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _currentTrack.value?.let { playTrack(it) }
            }
            RepeatMode.ALL -> {
                nextTrack()
            }
            RepeatMode.OFF -> {
                val current = _currentTrack.value ?: return
                val currentIndex = playlist.indexOfFirst { it.id == current.id }
                if (currentIndex != -1 && currentIndex < playlist.size - 1) {
                    nextTrack()
                } else {
                    releaseMediaPlayer()
                    _isPlaying.value = false
                    _progressMs.value = 0L
                }
            }
        }
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        val oldShuffle = _shuffleEnabled.value
        _shuffleEnabled.value = !oldShuffle
        
        val current = _currentTrack.value
        if (_shuffleEnabled.value) {
            playlist = originalPlaylist.shuffled()
        } else {
            playlist = originalPlaylist
        }
        
        // Ensure current track is matched in current queue
        if (current != null && !playlist.contains(current)) {
            playlist = listOf(current) + playlist.filter { it.id != current.id }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val speedVal = speed.coerceIn(0.5f, 2.0f)
        _playbackSpeed.value = speedVal
        setPlaybackSpeedInternal(speedVal)
    }

    private fun setPlaybackSpeedInternal(speed: Float) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.playbackParams = player.playbackParams.setSpeed(speed)
                    } else {
                        // Change is playing params without activating track play
                        val wasPlaying = player.isPlaying
                        player.playbackParams = player.playbackParams.setSpeed(speed)
                        if (!wasPlaying) {
                            player.pause()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Set interactive equalizer frequencies
    private fun initializeEqualizer(audioSessionId: Int) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            applyEqualizerFrequencies()
        } catch (e: Exception) {
            e.printStackTrace()
            equalizer = null
        }
    }

    fun updateEqualizerBands(bandLevels: List<Float>) {
        if (bandLevels.size >= 5) {
            currentBandLevels = bandLevels.toMutableList()
            applyEqualizerFrequencies()
        }
    }

    private fun applyEqualizerFrequencies() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until numBands.coerceAtMost(currentBandLevels.size)) {
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]
                val customValueDb = currentBandLevels[i] // -15 to +15 dB
                val mB = (customValueDb * 100f).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt())
                eq.setBandLevel(i.toShort(), mB.toShort())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startProgressUpdates() {
        progressJob = coroutineScope.launch {
            while (isActive) {
                if (_isPlaying.value) {
                    try {
                        mediaPlayer?.let { player ->
                            _progressMs.value = player.currentPosition.toLong()
                        }
                    } catch (e: Exception) {
                        // safe capture
                    }
                }
                delay(200)
            }
        }
    }

    // High performance procedural waveform generator modulating heights relative to sound levels
    private fun startVisualizerAnimation() {
        visualizerJob = coroutineScope.launch {
            val random = Random(currentTimeMillis())
            while (isActive) {
                if (_isPlaying.value) {
                    // Generate fluid wave
                    val bandsList = List(16) { index ->
                        // Modulate wave height using equalizer bands
                        // index 0..2 (bass), 3..6 (low-mid), 7..10 (mid), 11..13 (mid-high), 14..15 (high)
                        val eqFactor = when (index) {
                            in 0..2 -> currentBandLevels[0]
                            in 3..6 -> currentBandLevels[1]
                            in 7..10 -> currentBandLevels[2]
                            in 11..13 -> currentBandLevels[3]
                            else -> currentBandLevels[4]
                        }
                        
                        // Converts dB (-15 to 15) to positive scalar (0.2 to 2.0)
                        val gain = ((eqFactor + 15f) / 30f) * 1.5f + 0.5f
                        
                        // Procedural sine waves with dynamic random tremors
                        val baseWave = Math.sin((currentTimeMillis() * 0.008) + (index * 0.5)).toFloat()
                        val noise = random.nextFloat() * 0.25f - 0.125f
                        ((baseWave + 1.2f) / 2.2f) * gain * 0.8f + noise + 0.12f
                    }.map { it.coerceIn(0.04f, 1.0f) }
                    
                    _visualizerFlow.value = bandsList
                } else {
                    // Return slow resting ambient floaters
                    val bandsList = List(16) { index ->
                        val baseWave = Math.sin((currentTimeMillis() * 0.002) + (index * 0.4)).toFloat()
                        ((baseWave + 1.2f) / 2.2f) * 0.15f + 0.03f
                    }
                    _visualizerFlow.value = bandsList
                }
                delay(60) // High frame update speed (15 FPS fluid)
            }
        }
    }

    private fun currentTimeMillis(): Long = System.currentTimeMillis()

    fun release() {
        progressJob?.cancel()
        visualizerJob?.cancel()
        releaseMediaPlayer()
        coroutineScope.cancel()
    }
}
