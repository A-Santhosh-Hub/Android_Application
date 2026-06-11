package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.data.repository.LyricsProvider
import com.example.player.RepeatMode
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PlayerScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    val track = currentTrack ?: return

    Box(modifier = modifier) {
        if (!isExpanded) {
            // Persistent Bottom Mini Player View
            MiniPlayerView(
                track = track,
                viewModel = viewModel,
                onExpandClick = { isExpanded = true }
            )
        } else {
            // Full Deck Overlay expanded view
            FullDeckPlayerView(
                track = track,
                viewModel = viewModel,
                onCollapseClick = { isExpanded = false }
            )
        }
    }
}

@Composable
fun MiniPlayerView(
    track: Track,
    viewModel: MusicViewModel,
    onExpandClick: () -> Unit
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progressMs by viewModel.playbackProgress.collectAsState()
    val durationMs by viewModel.trackDuration.collectAsState()

    val cardAccentColor = remember(track.artworkHex) {
        try {
            Color(android.graphics.Color.parseColor(track.artworkHex))
        } catch (e: Exception) {
            Color(0xFF00E5FF)
        }
    }

    val progressFraction = remember(progressMs, durationMs) {
        if (durationMs > 0) progressMs.toFloat() / durationMs.toFloat() else 0f
    }

    Card(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onExpandClick)
            .testTag("mini_player_container")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Line mini progress bar across the top of the card
            LinearProgressIndicator(
                progress = { progressFraction },
                color = cardAccentColor,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniature Art circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(cardAccentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = cardAccentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track and Artist metadata
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Playback Controllers
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.playerManager.prevTrack() },
                        modifier = Modifier.testTag("mini_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .shadow(2.dp, CircleShape)
                            .background(cardAccentColor, CircleShape)
                            .size(38.dp)
                            .clickable { viewModel.playerManager.togglePlayPause() }
                            .testTag("mini_play_pause_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause Toggle",
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.playerManager.nextTrack() },
                        modifier = Modifier.testTag("mini_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next Track",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullDeckPlayerView(
    track: Track,
    viewModel: MusicViewModel,
    onCollapseClick: () -> Unit
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progressMs by viewModel.playbackProgress.collectAsState()
    val durationMs by viewModel.trackDuration.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val visualizerBars by viewModel.visualizerFlow.collectAsState()
    val lyrics by viewModel.currentTrackLyrics.collectAsState()
    val isLyricsLoading by viewModel.isLyricsLoading.collectAsState()

    var deckActiveTab by remember { mutableIntStateOf(0) } // 0 = Player, 1 = Lyrics

    val cardAccentColor = remember(track.artworkHex) {
        try {
            Color(android.graphics.Color.parseColor(track.artworkHex))
        } catch (e: Exception) {
            Color(0xFF00E5FF)
        }
    }

    // Dynamic rotation angle for spinning Vinyl/Disc based on playing state
    var rotationValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotationValue = (rotationValue + 1.5f) % 360f
                delay(16) // Smooth 60FPS continuous rotation
            }
        }
    }

    // Slide-up draggable layout boundaries
    var offsetY by remember { mutableFloatStateOf(0f) }
    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragEnd = {
                if (offsetY > 300) {
                    onCollapseClick()
                }
                offsetY = 0f
            },
            onDrag = { change, dragAmount ->
                change.consume()
                if (offsetY + dragAmount.y >= 0) {
                    offsetY += dragAmount.y
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .then(dragModifier)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        cardAccentColor.copy(alpha = 0.15f)
                    )
                )
            )
            .testTag("full_player_deck_view")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle and close top-deck
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapseClick,
                    modifier = Modifier.testTag("collapse_deck_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse slide-up player",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp, 5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
                )

                IconButton(
                    onClick = { viewModel.toggleFavorite(track) },
                    modifier = Modifier.testTag("deck_fav_btn")
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite track status",
                        tint = if (track.isFavorite) cardAccentColor else MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Premium Switcher Row
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (deckActiveTab == 0) cardAccentColor else Color.Transparent)
                        .clickable { deckActiveTab = 0 }
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (deckActiveTab == 0) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Player",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (deckActiveTab == 0) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (deckActiveTab == 1) cardAccentColor else Color.Transparent)
                        .clickable { deckActiveTab = 1 }
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = null,
                            tint = if (deckActiveTab == 1) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Lyrics",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (deckActiveTab == 1) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = deckActiveTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "deck_view_switcher"
            ) { activeModeTab ->
                if (activeModeTab == 0) {
                    // Standard Vinyl + Title + Visualizer mode
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Beautiful Spinning Vinyl Record Disk
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .aspectRatio(1f)
                                .shadow(16.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(0xFF0F1116))
                                .rotate(rotationValue),
                            contentAlignment = Alignment.Center
                        ) {
                            // Vinyl grooves lines drawings
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val radius = this.size.minDimension / 2f
                                drawCircle(color = Color(0xFF1E222D), radius = radius * 0.9f)
                                drawCircle(color = Color(0xFF141720), radius = radius * 0.72f)
                                drawCircle(color = Color(0xFF0B0D11), radius = radius * 0.5f)
                            }

                            // Decorative Center Album sticker
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                cardAccentColor, 
                                                MaterialTheme.colorScheme.secondary, 
                                                cardAccentColor
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Metadata text blocks
                        Text(
                            text = track.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = track.artist,
                            fontSize = 16.sp,
                            color = cardAccentColor,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Integrated bouncy Visualizer bars
                        SpectrumVisualizer(
                            bars = visualizerBars,
                            isPlaying = isPlaying,
                            accentColor = cardAccentColor,
                            secondaryColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Transparent)
                                .padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    // Smart automatic synced lyrics mode using live Gemini fetches
                    if (isLyricsLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = cardAccentColor,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Searching Real Lyrics...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Querying Gemini API for \"${track.title}\" by \"${track.artist}\"...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Synced Scrolling Lyrics
                        val activeLineIndex = remember(progressMs, lyrics) {
                            val idx = lyrics.indexOfLast { progressMs >= it.timeMs }
                            if (idx == -1) 0 else idx
                        }

                        val lazyListState = rememberLazyListState()
                        
                        LaunchedEffect(activeLineIndex) {
                            if (lyrics.isNotEmpty() && activeLineIndex >= 0) {
                                lazyListState.animateScrollToItem(
                                    index = activeLineIndex,
                                    scrollOffset = -100
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Lyrics synced instantly",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00E676),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(vertical = 90.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(lyrics) { index, line ->
                                    val isActive = index == activeLineIndex
                                    
                                    val lyricColor = if (isActive) cardAccentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                    val lyricFontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium
                                    val lyricScale = if (isActive) 1.15f else 0.90f
                                    
                                    val animatedColor by animateColorAsState(targetValue = lyricColor, label = "lyric_color")
                                    val animatedScale by animateFloatAsState(targetValue = lyricScale, label = "lyric_scale")

                                    Text(
                                        text = line.text,
                                        color = animatedColor,
                                        fontWeight = lyricFontWeight,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .graphicsLayer {
                                                scaleX = animatedScale
                                                scaleY = animatedScale
                                            }
                                            .clickable {
                                                // Instant Touch Seek to sync location
                                                viewModel.playerManager.seekTo(line.timeMs)
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Scrubbing Seek Bar & Durations text
            val currentProgressStr = formatTime(progressMs)
            val durationMsActual = if (durationMs > 0) durationMs else track.durationMs
            val totalDurationStr = formatTime(durationMsActual)

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progressMs.toFloat(),
                    onValueChange = { viewModel.playerManager.seekTo(it.toLong()) },
                    valueRange = 0f..(if (durationMsActual > 0) durationMsActual.toFloat() else 100f),
                    colors = SliderDefaults.colors(
                        activeTrackColor = cardAccentColor,
                        thumbColor = cardAccentColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scrub_bar_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentProgressStr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = totalDurationStr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary control buttons deck
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle mode action
                IconButton(
                    onClick = { viewModel.playerManager.toggleShuffle() },
                    modifier = Modifier.testTag("deck_shuffle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle Playlist Toggle",
                        tint = if (shuffleEnabled) cardAccentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }

                // Skip Back action
                IconButton(
                    onClick = { viewModel.playerManager.prevTrack() },
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("deck_prev_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous song",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Major Play-Pause toggle FAB
                Box(
                    modifier = Modifier
                        .shadow(12.dp, CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(cardAccentColor, MaterialTheme.colorScheme.secondary)
                            ), 
                            CircleShape
                        )
                        .size(72.dp)
                        .clickable { viewModel.playerManager.togglePlayPause() }
                        .testTag("deck_play_pause_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Major Play Trigger",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Skip Forward action
                IconButton(
                    onClick = { viewModel.playerManager.nextTrack() },
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("deck_next_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next song",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Repeat mode state action
                IconButton(
                    onClick = { viewModel.playerManager.toggleRepeatMode() },
                    modifier = Modifier.testTag("deck_repeat_btn")
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            RepeatMode.ALL -> Icons.Default.Repeat
                            RepeatMode.OFF -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat Track modes toggle",
                        tint = if (repeatMode == RepeatMode.OFF) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f) else cardAccentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Advanced: Playback Tempo/Speed slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = cardAccentColor,
                    modifier = Modifier.size(18.dp)
                )
                
                Spacer(modifier = Modifier.width(10.dp))
                
                Text(
                    text = "Speed:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Slider(
                    value = speed,
                    onValueChange = { viewModel.playerManager.setPlaybackSpeed(it) },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = cardAccentColor,
                        thumbColor = cardAccentColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .testTag("playback_speed_slider")
                )

                Text(
                    text = String.format(Locale.getDefault(), "%.1fx", speed),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = cardAccentColor
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Composable
fun SpectrumVisualizer(
    bars: List<Float>,
    isPlaying: Boolean,
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barCount = 15
        val displayBars = remember(bars) {
            if (bars.isEmpty()) List(barCount) { 0.2f } else bars.take(barCount)
        }
        
        displayBars.forEachIndexed { index, rawAmplitude ->
            // Simulating bounce animation when playing
            val simulatedAmplitude = if (isPlaying) {
                // Combine raw amplitude with dynamic index-based offsets
                val bounceState = rememberInfiniteTransition(label = "bar_$index")
                val offsetAnim by bounceState.animateFloat(
                    initialValue = 0.15f,
                    targetValue = 0.85f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 300 + (index * 30)),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "bar_anim"
                )
                (rawAmplitude * 0.4f + offsetAnim * 0.6f).coerceIn(0.1f, 1.0f)
            } else {
                0.15f
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(simulatedAmplitude)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor, secondaryColor)
                        )
                    )
            )
        }
    }
}
