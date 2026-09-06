package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Movie
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineGreen
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ScreenFitMode(val label: String, val description: String) {
    FIT_16_9("16:9 Fit", "Classic letterbox aspect"),
    FILL_CROP("Crop to Fill", "Fills entire screen completely"),
    STRETCH("Stretch", "Stretches to window bounds"),
    WIDE_21_9("21:9 UltraWide", "Anamorphic cinemascope")
}

@Composable
fun VideoPlayerScreen(
    movie: Movie,
    onBackClick: () -> Unit,
    onSaveProgress: (positionSeconds: Int, totalSeconds: Int) -> Unit,
    modifier: Modifier = Modifier,
    isTrailer: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val totalSeconds = remember {
        if (isTrailer) 145 else movie.durationMinutes * 60
    }
    var currentSeconds by remember { mutableIntStateOf(if (isTrailer) 0 else 42) }
    var isPlaying by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }

    // Volume & Brightness Gesture States
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxAudioVolume = remember {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
    }

    var volumePercent by remember {
        val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10
        mutableFloatStateOf((currentVol.toFloat() / maxAudioVolume.toFloat()).coerceIn(0f, 1f))
    }
    var brightnessPercent by remember { mutableFloatStateOf(0.75f) }

    var isVolumeHudVisible by remember { mutableStateOf(false) }
    var isBrightnessHudVisible by remember { mutableStateOf(false) }

    // Double-tap Seek Animation Overlays
    var showRewindIndicator by remember { mutableStateOf(false) }
    var showForwardIndicator by remember { mutableStateOf(false) }

    // Screen Fit & Aspect Ratio
    var screenFitMode by remember { mutableStateOf(if (isLandscape) ScreenFitMode.FILL_CROP else ScreenFitMode.FIT_16_9) }
    var showFitMenu by remember { mutableStateOf(false) }

    // Quality, Speed, Captions
    var playbackSpeed by remember { mutableStateOf("1.0x") }
    var selectedQuality by remember { mutableStateOf(movie.quality.ifBlank { "4K Ultra HD" }) }
    var captionsLanguage by remember { mutableStateOf("English [CC]") }

    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showCaptionsMenu by remember { mutableStateOf(false) }

    // Toggle Screen Fullscreen / Landscape Rotation (like YouTube)
    fun toggleFullscreenOrientation() {
        val activity = context as? Activity ?: return
        if (isLandscape) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    // Apply Immersive Fullscreen Mode on Landscape
    LaunchedEffect(isLandscape) {
        val activity = context as? Activity
        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isLandscape) {
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Apply brightness to system window
    LaunchedEffect(brightnessPercent) {
        val activity = context as? Activity
        activity?.let {
            val lp = it.window.attributes
            lp.screenBrightness = brightnessPercent.coerceIn(0.01f, 1.0f)
            it.window.attributes = lp
        }
    }

    // Cleanup when exiting player (reset orientation & brightness)
    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? Activity
            activity?.let {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val lp = it.window.attributes
                lp.screenBrightness = -1.0f
                it.window.attributes = lp
                it.window?.let { win ->
                    val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    // Playback timer loop
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

    // Auto-hide controls after 4 seconds
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Auto-hide Volume HUD after 1.5 seconds
    LaunchedEffect(isVolumeHudVisible, volumePercent) {
        if (isVolumeHudVisible) {
            delay(1500)
            isVolumeHudVisible = false
        }
    }

    // Auto-hide Brightness HUD after 1.5 seconds
    LaunchedEffect(isBrightnessHudVisible, brightnessPercent) {
        if (isBrightnessHudVisible) {
            delay(1500)
            isBrightnessHudVisible = false
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

    val isOfflinePlayback = remember(movie.videoUrl) {
        movie.videoUrl.startsWith("/") || movie.videoUrl.startsWith("file:")
    }

    // Subtitles text generator
    val currentSubtitleText = remember(currentSeconds, captionsLanguage, isTrailer) {
        if (captionsLanguage == "Off") {
            ""
        } else {
            val cycleTime = currentSeconds % 40
            when (captionsLanguage) {
                "Spanish" -> when (cycleTime) {
                    in 0..7 -> "Todo lo que arriesgamos nos trajo hasta este punto."
                    in 8..15 -> "¡Mantengan la posición de defensa perimetral ahora!"
                    in 16..25 -> "Iniciando la secuencia del núcleo cinemático."
                    in 26..33 -> "Estamos a segundos del punto sin retorno."
                    else -> "¿Estás listo para lo que viene a continuación?"
                }
                "French" -> when (cycleTime) {
                    in 0..7 -> "Chaque décision nous a conduit directement à cet instant."
                    in 8..15 -> "Maintenez le périmètre d'extraction immédiatement !"
                    in 16..25 -> "Activation de la séquence centrale en cours."
                    in 26..33 -> "Nous approchons du point de non-retour."
                    else -> "Êtes-vous prêt pour ce qui nous attend ?"
                }
                "German" -> when (cycleTime) {
                    in 0..7 -> "Jede Entscheidung hat uns genau hierher geführt."
                    in 8..15 -> "Sichert die Extraktionszone sofort!"
                    in 16..25 -> "Kinosequenz wird jetzt initialisiert."
                    in 26..33 -> "Wir haben den Punkt ohne Wiederkehr erreicht."
                    else -> "Bist du bereit für das, was jetzt kommt?"
                }
                else -> when (cycleTime) { // English [CC]
                    in 0..7 -> if (isTrailer) "\"Every choice we made led us directly to this moment.\"" else "\"Stand down and hold the extraction perimeter now!\""
                    in 8..15 -> if (isTrailer) "\"They said the system was impenetrable. They were wrong.\"" else "\"All teams synchronize your timers. We move on my mark.\""
                    in 16..25 -> if (isTrailer) "\"Prepare for the ultimate cinematic showdown.\"" else "\"Energy signature detected in Sector 7. Lock and load!\""
                    in 26..33 -> if (isTrailer) "\"Only in theaters and streaming in Movie Room 4K.\"" else "\"We have visual on the target. Commencing final protocol.\""
                    else -> "\"Watch your six. This isn't over yet.\""
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineBlack)
            .testTag("video_player_screen")
    ) {
        // Video Stage with Adaptive Fit
        val videoModifier = when (screenFitMode) {
            ScreenFitMode.FIT_16_9 -> Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .align(Alignment.Center)
            ScreenFitMode.FILL_CROP -> Modifier.fillMaxSize()
            ScreenFitMode.STRETCH -> Modifier.fillMaxSize()
            ScreenFitMode.WIDE_21_9 -> Modifier
                .fillMaxWidth()
                .aspectRatio(21f / 9f)
                .align(Alignment.Center)
        }

        val videoContentScale = when (screenFitMode) {
            ScreenFitMode.FIT_16_9 -> ContentScale.Fit
            ScreenFitMode.FILL_CROP -> ContentScale.Crop
            ScreenFitMode.STRETCH -> ContentScale.FillBounds
            ScreenFitMode.WIDE_21_9 -> ContentScale.Crop
        }

        Box(
            modifier = videoModifier.background(Color.Black)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(movie.backdropUrl.ifBlank { movie.posterUrl })
                    .crossfade(true)
                    .build(),
                contentDescription = movie.title,
                contentScale = videoContentScale,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic screen brightness dimmer overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = ((1f - brightnessPercent) * 0.75f).coerceIn(0f, 0.85f)))
            )

            // Cinematic Ambience Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color(0x22070709), Color(0x88070709)),
                            radius = 1400f
                        )
                    )
            )
        }

        // Subtitles Overlay (Floating neatly above controls)
        if (currentSubtitleText.isNotBlank() && isPlaying) {
            Surface(
                color = Color(0xDD070709),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, Color(0x44FFFFFF)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (controlsVisible) (if (isLandscape) 92.dp else 120.dp) else (if (isLandscape) 28.dp else 44.dp))
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = currentSubtitleText,
                    color = Color.White,
                    fontSize = if (isLandscape) 16.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // ==========================================
        // GESTURE RECOGNITION: LEFT (BRIGHTNESS/REWIND) & RIGHT (VOLUME/FORWARD)
        // ==========================================
        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT SIDE: Brightness Drag & Double Tap to Rewind (-10s)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { isBrightnessHudVisible = true },
                            onDragEnd = { },
                            onDragCancel = { isBrightnessHudVisible = false },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                isBrightnessHudVisible = true
                                val delta = -dragAmount / 450f
                                brightnessPercent = (brightnessPercent + delta).coerceIn(0.05f, 1.0f)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible },
                            onDoubleTap = {
                                currentSeconds = (currentSeconds - 10).coerceAtLeast(0)
                                showRewindIndicator = true
                                coroutineScope.launch {
                                    delay(700)
                                    showRewindIndicator = false
                                }
                            }
                        )
                    }
            )

            // RIGHT SIDE: Volume Drag & Double Tap to Forward (+10s)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { isVolumeHudVisible = true },
                            onDragEnd = { },
                            onDragCancel = { isVolumeHudVisible = false },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                isVolumeHudVisible = true
                                val delta = -dragAmount / 450f
                                volumePercent = (volumePercent + delta).coerceIn(0f, 1f)
                                audioManager?.let { am ->
                                    val newStreamVol = (volumePercent * maxAudioVolume).toInt()
                                    am.setStreamVolume(AudioManager.STREAM_MUSIC, newStreamVol, 0)
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible },
                            onDoubleTap = {
                                currentSeconds = (currentSeconds + 10).coerceAtMost(totalSeconds)
                                showForwardIndicator = true
                                coroutineScope.launch {
                                    delay(700)
                                    showForwardIndicator = false
                                }
                            }
                        )
                    }
            )
        }

        // ==========================================
        // ANIMATED GESTURE HUDS (HIDDEN UNTIL TOUCHED)
        // ==========================================

        // 1. Left Brightness HUD Indicator
        AnimatedVisibility(
            visible = isBrightnessHudVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = if (isLandscape) 40.dp else 24.dp)
        ) {
            Surface(
                color = Color(0xEE0D0D12),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0x44FFD700)),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val brightnessIcon: ImageVector = when {
                        brightnessPercent < 0.35f -> Icons.Default.BrightnessLow
                        brightnessPercent < 0.70f -> Icons.Default.BrightnessMedium
                        else -> Icons.Default.BrightnessHigh
                    }

                    Icon(
                        imageVector = brightnessIcon,
                        contentDescription = "Brightness",
                        tint = CineGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(100.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x44FFFFFF)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(brightnessPercent)
                                .background(CineGold)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${(brightnessPercent * 100).toInt()}%",
                        color = CineTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Brightness",
                        color = CineTextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }

        // 2. Right Volume HUD Indicator
        AnimatedVisibility(
            visible = isVolumeHudVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = if (isLandscape) 40.dp else 24.dp)
        ) {
            Surface(
                color = Color(0xEE0D0D12),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CineRedPrimary.copy(alpha = 0.6f)),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val volumeIcon: ImageVector = when {
                        volumePercent <= 0.01f -> Icons.AutoMirrored.Filled.VolumeMute
                        volumePercent < 0.50f -> Icons.AutoMirrored.Filled.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    }

                    Icon(
                        imageVector = volumeIcon,
                        contentDescription = "Volume",
                        tint = CineRedPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(100.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x44FFFFFF)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(volumePercent)
                                .background(CineRedPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${(volumePercent * 100).toInt()}%",
                        color = CineTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Volume",
                        color = CineTextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }

        // 3. Double-Tap Rewind (-10s) Overlay
        AnimatedVisibility(
            visible = showRewindIndicator,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = if (isLandscape) 80.dp else 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xDD0D0D12)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = "-10s",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Double-Tap Forward (+10s) Overlay
        AnimatedVisibility(
            visible = showForwardIndicator,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = if (isLandscape) 80.dp else 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xDD0D0D12)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = "+10s",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // MAIN HUD CONTROLS OVERLAY (Toggled via tap)
        // ==========================================
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88070709))
            ) {
                // TOP BAR: Back, Movie Title, Quality/Offline tags
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(
                            horizontal = if (isLandscape) 28.dp else 16.dp,
                            vertical = if (isLandscape) 12.dp else 8.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
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

                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = movie.title,
                                    color = CineTextPrimary,
                                    fontSize = if (isLandscape) 17.sp else 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                if (isOfflinePlayback) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0x3346D369)
                                    ) {
                                        Text(
                                            text = "OFFLINE",
                                            color = CineGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isOfflinePlayback) {
                                    "Saved Offline • ${screenFitMode.label}"
                                } else if (isTrailer) {
                                    "Trailer Teaser • ${screenFitMode.label}"
                                } else {
                                    "Streaming 4K • ${screenFitMode.label}"
                                },
                                color = CineTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Top quick orientation indicator
                    IconButton(onClick = { toggleFullscreenOrientation() }) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Rotate Screen",
                            tint = if (isLandscape) CineRedPrimary else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // CENTER PLAY / PAUSE CONTROLS
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 48.dp else 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            currentSeconds = (currentSeconds - 10).coerceAtLeast(0)
                        },
                        modifier = Modifier
                            .size(if (isLandscape) 56.dp else 48.dp)
                            .clip(CircleShape)
                            .background(Color(0x66070709))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(if (isLandscape) 30.dp else 26.dp)
                        )
                    }

                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(if (isLandscape) 80.dp else 70.dp)
                            .clip(CircleShape)
                            .background(CineRedPrimary)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(if (isLandscape) 44.dp else 38.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            currentSeconds = (currentSeconds + 10).coerceAtMost(totalSeconds)
                        },
                        modifier = Modifier
                            .size(if (isLandscape) 56.dp else 48.dp)
                            .clip(CircleShape)
                            .background(Color(0x66070709))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(if (isLandscape) 30.dp else 26.dp)
                        )
                    }
                }

                // ==========================================
                // BOTTOM BAR: YOUTUBE-STYLE CONTROLS & NAVS
                // (Scrubber + Time + Captions + Fit + Quality + Speed + Fullscreen)
                // ==========================================
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            horizontal = if (isLandscape) 28.dp else 16.dp,
                            vertical = if (isLandscape) 8.dp else 10.dp
                        )
                ) {
                    // 1. Live Time Scrubber Slider
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
                            .height(26.dp)
                            .testTag("player_seek_bar")
                    )

                    // 2. YouTube-style Bottom Navigation Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Current / Total Time Stamp
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${formatTime(currentSeconds)} / ${formatTime(totalSeconds)}",
                                color = CineTextPrimary,
                                fontSize = if (isLandscape) 13.sp else 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (playbackSpeed != "1.0x") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CineRedPrimary.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = playbackSpeed,
                                        color = CineRedPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Right: Full YouTube Bottom Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 4.dp else 0.dp)
                        ) {
                            // 1. CAPTIONS / SUBTITLE BUTTON
                            Box {
                                IconButton(
                                    onClick = { showCaptionsMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ClosedCaption,
                                        contentDescription = "Subtitles",
                                        tint = if (captionsLanguage != "Off") CineRedPrimary else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showCaptionsMenu,
                                    onDismissRequest = { showCaptionsMenu = false }
                                ) {
                                    listOf("English [CC]", "Spanish", "French", "German", "Off").forEach { lang ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(lang, fontWeight = if (captionsLanguage == lang) FontWeight.Bold else FontWeight.Normal)
                                                    if (captionsLanguage == lang) {
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = CineRedPrimary, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            },
                                            onClick = {
                                                captionsLanguage = lang
                                                showCaptionsMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 2. SCREEN FIT / ASPECT RATIO BUTTON
                            Box {
                                IconButton(
                                    onClick = { showFitMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AspectRatio,
                                        contentDescription = "Screen Fit Mode",
                                        tint = if (screenFitMode != ScreenFitMode.FIT_16_9) CineRedPrimary else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showFitMenu,
                                    onDismissRequest = { showFitMenu = false }
                                ) {
                                    ScreenFitMode.values().forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(mode.label, fontWeight = if (mode == screenFitMode) FontWeight.Bold else FontWeight.Normal)
                                                        Text(mode.description, fontSize = 10.sp, color = CineTextMuted)
                                                    }
                                                    if (mode == screenFitMode) {
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = CineRedPrimary, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            },
                                            onClick = {
                                                screenFitMode = mode
                                                showFitMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 3. VIDEO QUALITY BUTTON
                            Box {
                                IconButton(
                                    onClick = { showQualityMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = "Video Quality",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showQualityMenu,
                                    onDismissRequest = { showQualityMenu = false }
                                ) {
                                    listOf("Auto (1080p)", "4K Ultra HD", "1080p Full HD", "720p HD").forEach { quality ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(quality, fontWeight = if (selectedQuality == quality) FontWeight.Bold else FontWeight.Normal)
                                                    if (selectedQuality == quality) {
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = CineRedPrimary, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedQuality = quality
                                                showQualityMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 4. SPEED BUTTON
                            Box {
                                IconButton(
                                    onClick = { showSpeedMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = "Playback Speed",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false }
                                ) {
                                    listOf("0.75x", "1.0x", "1.25x", "1.5x", "2.0x").forEach { speed ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(speed, fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal)
                                                    if (playbackSpeed == speed) {
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = CineRedPrimary, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            },
                                            onClick = {
                                                playbackSpeed = speed
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 5. MUTE / UNMUTE QUICK TOGGLE
                            IconButton(
                                onClick = {
                                    if (volumePercent > 0f) {
                                        volumePercent = 0f
                                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                                    } else {
                                        volumePercent = 0.8f
                                        audioManager?.setStreamVolume(
                                            AudioManager.STREAM_MUSIC,
                                            (0.8f * maxAudioVolume).toInt(),
                                            0
                                        )
                                    }
                                    isVolumeHudVisible = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (volumePercent <= 0.01f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Mute / Unmute",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 6. YOUTUBE FULLSCREEN / ROTATION TOGGLE BUTTON (Corner right)
                            IconButton(
                                onClick = { toggleFullscreenOrientation() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("player_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = if (isLandscape) "Exit Fullscreen" else "Enter Fullscreen",
                                    tint = CineRedPrimary,
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
