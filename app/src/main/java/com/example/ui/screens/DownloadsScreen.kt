package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.MovieDownloadManager
import com.example.data.entity.DownloadedMovieEntity
import com.example.model.Movie
import com.example.ui.components.EmptyState
import com.example.ui.components.PrimaryButton
import com.example.ui.components.QualityBadge
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineCardBorder
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineGreen
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineSurface
import com.example.ui.theme.CineSurfaceElevated
import com.example.ui.theme.CineSurfaceVariant
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import com.example.ui.theme.CineTextSecondary

@Composable
fun DownloadsScreen(
    downloadedMovies: List<DownloadedMovieEntity>,
    downloadProgressMap: Map<String, Int>,
    onWatchMovie: (Movie) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onExploreMoviesClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var movieToDelete by remember { mutableStateOf<DownloadedMovieEntity?>(null) }

    // Calculate Storage metrics
    val totalDownloadBytes = remember(downloadedMovies) {
        downloadedMovies.sumOf { it.fileSizeBytes }
    }
    val formattedTotalDownloads = remember(totalDownloadBytes) {
        MovieDownloadManager.formatFileSize(totalDownloadBytes)
    }

    val storageInfo = remember(downloadedMovies) {
        MovieDownloadManager.getDeviceStorageInfo(context)
    }
    val freeStorageFormatted = remember(storageInfo) {
        MovieDownloadManager.formatFileSize(storageInfo.first)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineBlack)
            .testTag("downloads_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Downloads",
                        color = CineTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (downloadedMovies.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = CineRedPrimary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, CineRedPrimary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${downloadedMovies.size}",
                                color = CineRedPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Quick Search Action button
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CineSurfaceElevated)
                        .testTag("downloads_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CineTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (downloadedMovies.isEmpty() && downloadProgressMap.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        illustration = {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(CineSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                    tint = CineRedPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        },
                        title = "No Offline Downloads Yet",
                        subtitle = "Download movies and shows to watch anywhere without Wi-Fi or mobile cellular data. Your downloaded media is stored securely on your device.",
                        actionButton = {
                            PrimaryButton(
                                text = "Explore Movies to Download",
                                onClick = onExploreMoviesClick
                            )
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Offline Readiness Banner & Storage Card
                    item(key = "storage_info_card") {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CineCardBorder),
                            colors = CardDefaults.cardColors(containerColor = CineSurfaceElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(CineGreen.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OfflinePin,
                                                contentDescription = null,
                                                tint = CineGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Offline Mode Ready",
                                                color = CineTextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "No internet required to play",
                                                color = CineTextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CineCardBorder
                                    ) {
                                        Text(
                                            text = formattedTotalDownloads,
                                            color = CineGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Device storage status bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Free Device Space: $freeStorageFormatted",
                                        color = CineTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${downloadedMovies.size} Videos Downloaded",
                                        color = CineTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Private In-App Vault Reassurance Card
                    item(key = "private_storage_vault_card") {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CineCardBorder),
                            colors = CardDefaults.cardColors(containerColor = CineSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(CineRedPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = CineRedPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "App-Private Offline Storage",
                                            color = CineTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = CineGreen.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "NO GALLERY ACCESS",
                                                color = CineGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Downloaded media stays strictly inside Movie Room and will never show up in your phone's photo/video gallery. You can watch anytime right here.",
                                        color = CineTextMuted,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Active Ongoing Downloads Section
                    if (downloadProgressMap.isNotEmpty()) {
                        item(key = "active_downloads_header") {
                            Text(
                                text = "DOWNLOADING NOW",
                                color = CineGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        items(downloadProgressMap.entries.toList(), key = { it.key }) { entry ->
                            val movieId = entry.key
                            val progress = entry.value
                            val movieEntity = downloadedMovies.firstOrNull { it.movieId == movieId }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, CineGold.copy(alpha = 0.5f)),
                                colors = CardDefaults.cardColors(containerColor = CineSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CineSurfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { progress / 100f },
                                            modifier = Modifier.size(34.dp),
                                            color = CineGold,
                                            strokeWidth = 3.dp
                                        )
                                        Text(
                                            text = "$progress%",
                                            color = CineGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = movieEntity?.title ?: "Downloading Movie...",
                                            color = CineTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Downloading stream to internal storage...",
                                            color = CineTextMuted,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { progress / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(CircleShape),
                                            color = CineGold,
                                            trackColor = CineCardBorder
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = { onCancelDownload(movieId) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancel Download",
                                            tint = CineTextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Completed Downloaded Movies List
                    item(key = "downloaded_list_header") {
                        Text(
                            text = "AVAILABLE OFFLINE",
                            color = CineTextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(downloadedMovies.filter { it.downloadStatus == "COMPLETED" }, key = { it.movieId }) { downloaded ->
                        DownloadedMovieCard(
                            movie = downloaded,
                            onPlayClick = {
                                onWatchMovie(downloaded.toMovie())
                            },
                            onDeleteClick = {
                                movieToDelete = downloaded
                            }
                        )
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (movieToDelete != null) {
            AlertDialog(
                onDismissRequest = { movieToDelete = null },
                containerColor = CineSurfaceElevated,
                titleContentColor = CineTextPrimary,
                textContentColor = CineTextSecondary,
                title = {
                    Text(
                        text = "Delete Download?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to remove \"${movieToDelete?.title}\" from your offline storage? You will need an internet connection to stream or download it again."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            movieToDelete?.let { onDeleteDownload(it.movieId) }
                            movieToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = CineRedPrimary)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { movieToDelete = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = CineTextMuted)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun DownloadedMovieCard(
    movie: DownloadedMovieEntity,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CineCardBorder),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .testTag("downloaded_card_${movie.movieId}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 16:9 Cinematic Video Backdrop Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(CineBlack)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(movie.backdropUrl.ifBlank { movie.posterUrl })
                        .crossfade(true)
                        .build(),
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Cinematic Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x33000000),
                                    Color.Transparent,
                                    Color(0xDD070709)
                                )
                            )
                        )
                )

                // Offline Checkmark Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CineGreen.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "OFFLINE READY",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quality Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    QualityBadge(quality = movie.quality)
                }

                // Center Play Button Ripple Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CineRedPrimary.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Downloaded Movie",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Duration Pill at Bottom Right
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xCC000000),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = movie.durationFormatted,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Movie Details & Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = movie.title,
                        color = CineTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "${movie.year} • ${movie.fileSizeFormatted} • App-Private Storage",
                        color = CineTextMuted,
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Play Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CineRedPrimary,
                        modifier = Modifier.clickable { onPlayClick() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Watch",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Delete button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Download",
                            tint = CineTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
