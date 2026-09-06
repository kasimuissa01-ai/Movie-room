package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Movie
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import com.example.ui.theme.CineTextSecondary
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    movie: Movie,
    onBackClick: () -> Unit,
    onSaveProgress: (positionSeconds: Int, totalSeconds: Int) -> Unit,
    modifier: Modifier = Modifier,
    isTrailer: Boolean = false
) {
    val totalSeconds = remember {
        if (isTrailer) 145 else movie.durationMinutes * 60
    }
    var currentSeconds by remember { mutableIntStateOf(if (isTrailer) 0 else 42) }
    var isPlaying by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    var volume by remember { mutableFloatStateOf(0.8f) }
    var isMuted by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf("1.0x") }
    var selectedQuality by remember { mutableStateOf("4K Ultra HD") }
    var captionsLanguage by remember { mutableStateOf("English [CC]") }

    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showCaptionsMenu by remember { mutableStateOf(false) }

    // Playback progression timer
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000)
            if (currentSeconds < totalSeconds) {
                currentSeconds += 1
            } else {
                isPlaying = false
            }
        }
    }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    fun formatTime(seconds: Int): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { controlsVisible = !controlsVisible }
            )
            .testTag("video_player_screen")
    ) {
        // Video Stage (Cinematic Backdrop Placeholder with Animated Grain / Overlay)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(movie.backdropUrl)
                .crossfade(true)
                .build(),
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark Vignette & Theater Ambience
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x66070709), Color(0xCC070709)),
                        radius = 1200f
                    )
                )
        )

        // Subtitles Overlay (if active)
        if (captionsLanguage != "Off" && isPlaying) {
            Surface(
                color = Color(0xCC070709),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (controlsVisible) 110.dp else 40.dp)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = if (isTrailer) {
                        "\"Every choice we made led us directly to this moment.\""
                    } else {
                        "\"Stand down and hold the extraction perimeter now!\""
                    },
                    color = CineTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // HUD Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x77070709))
            ) {
                // Top Bar: Back, Title, Mode, Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                onSaveProgress(currentSeconds, totalSeconds)
                                onBackClick()
                            },
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = movie.title,
                                color = CineTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isTrailer) "Official Trailer" else "Now Streaming • ${movie.quality}",
                                color = CineTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Quality & Captions Menus
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Playback Speed button
                        Box {
                            IconButton(onClick = { showSpeedMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Playback Speed",
                                    tint = CineTextPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf("0.75x", "1.0x", "1.25x", "1.5x", "2.0x").forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text(speed) },
                                        onClick = {
                                            playbackSpeed = speed
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Quality selector button
                        Box {
                            IconButton(onClick = { showQualityMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.HighQuality,
                                    contentDescription = "Video Quality",
                                    tint = CineTextPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = showQualityMenu,
                                onDismissRequest = { showQualityMenu = false }
                            ) {
                                listOf("Auto (1080p)", "4K Ultra HD", "1080p Full HD", "720p HD").forEach { quality ->
                                    DropdownMenuItem(
                                        text = { Text(quality) },
                                        onClick = {
                                            selectedQuality = quality
                                            showQualityMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Captions button
                        Box {
                            IconButton(onClick = { showCaptionsMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.ClosedCaption,
                                    contentDescription = "Subtitles",
                                    tint = if (captionsLanguage != "Off") CineRedPrimary else CineTextPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = showCaptionsMenu,
                                onDismissRequest = { showCaptionsMenu = false }
                            ) {
                                listOf("English [CC]", "Spanish", "French", "Off").forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang) },
                                        onClick = {
                                            captionsLanguage = lang
                                            showCaptionsMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Center Controls: Rewind 10s, Play/Pause, Forward 10s
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = {
                            currentSeconds = (currentSeconds - 10).coerceAtLeast(0)
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0x55070709))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Main Play/Pause Button
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(CineRedPrimary)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = {
                            currentSeconds = (currentSeconds + 10).coerceAtMost(totalSeconds)
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0x55070709))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Controls: Scrubber, Time, Volume, Fullscreen
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Scrubber Slider
                    Slider(
                        value = currentSeconds.toFloat(),
                        onValueChange = { currentSeconds = it.toInt() },
                        valueRange = 0f..totalSeconds.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = CineRedPrimary,
                            activeTrackColor = CineRedPrimary,
                            inactiveTrackColor = Color(0x55FFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_seek_bar")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current time / Total time
                        Text(
                            text = "${formatTime(currentSeconds)} / ${formatTime(totalSeconds)}",
                            color = CineTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Right Action Controls: Volume & Fullscreen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Volume / Mute
                            IconButton(onClick = { isMuted = !isMuted }) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Volume",
                                    tint = CineTextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Fullscreen toggle
                            IconButton(onClick = { isFullscreen = !isFullscreen }) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Toggle Fullscreen",
                                    tint = CineTextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
