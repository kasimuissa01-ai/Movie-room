package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.SampleMovies
import com.example.model.Movie
import com.example.ui.components.GenreBadge
import com.example.ui.components.MovieCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.QualityBadge
import com.example.ui.components.RatingBadge
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineCardBorder
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineGreen
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineSurfaceElevated
import com.example.ui.theme.CineSurfaceVariant
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import com.example.ui.theme.CineTextSecondary

@Composable
fun MovieDetailsScreen(
    movie: Movie?,
    isInWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
    onWatchClick: (Movie) -> Unit,
    onTrailerClick: (Movie) -> Unit,
    onRelatedMovieClick: (Movie) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isError: Boolean = false,
    errorMessage: String = "",
    onRetry: () -> Unit = {},
    recommendations: List<Movie> = emptyList(),
    isLoadingRecommendations: Boolean = false,
    isDownloaded: Boolean = false,
    downloadProgress: Int? = null,
    onDownloadClick: (Movie) -> Unit = {},
    onDeleteDownloadClick: (String) -> Unit = {},
    isOnline: Boolean = true,
    isRoomCached: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineBlack)
            .testTag("movie_details_screen")
    ) {
        if (isLoading && movie == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = CineRedPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Loading Movie Details...",
                    color = CineTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (isError && movie == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CineRedPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Error",
                        tint = CineRedPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Failed to load movie",
                    color = CineTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage.ifEmpty { "Unable to connect to the movie API server." },
                    color = CineTextSecondary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(
                    text = "Retry",
                    onClick = onRetry
                )
            }
        } else if (movie != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
            // Large Backdrop Header with 16:9 Cinematic Framing and floating Poster
            item(key = "header_backdrop") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(movie.backdropUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Scrims
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xCC070709),
                                        Color(0x33070709),
                                        Color(0xEE070709),
                                        CineBlack
                                    )
                                )
                            )
                    )

                    // Movie Poster & Title card overlapping bottom
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Poster Thumbnail
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CineCardBorder),
                            colors = CardDefaults.cardColors(containerColor = CineSurfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .width(96.dp)
                                .aspectRatio(0.68f)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(movie.posterUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = movie.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Title, Year, Rating, Quality
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QualityBadge(quality = movie.quality)
                                RatingBadge(rating = movie.rating)
                                if (isDownloaded) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = CineGreen.copy(alpha = 0.3f),
                                        border = BorderStroke(0.5.dp, CineGreen)
                                    ) {
                                        Text(
                                            text = "OFFLINE",
                                            color = CineGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                } else if (isInWatchlist || isRoomCached) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = CineGold.copy(alpha = 0.25f),
                                        border = BorderStroke(0.5.dp, CineGold.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = "ROOM CACHE",
                                            color = CineGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0x33FFFFFF)
                                    ) {
                                        Text(
                                            text = movie.contentRating,
                                            color = CineTextPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = movie.title,
                                color = CineTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 26.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${movie.year} • ${movie.durationFormatted} • ${movie.director}",
                                color = CineTextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Genre Badges Row
            item(key = "genres_row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(movie.genres) { genre ->
                        GenreBadge(genre = genre)
                    }
                }
            }

            // Action Buttons: [ Watch Movie ] [ Watch Trailer ] [ + My List ] [ Download for Offline ]
            item(key = "action_buttons") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryButton(
                        text = if (isDownloaded) "Watch Offline" else "Watch Movie",
                        icon = Icons.Default.PlayArrow,
                        onClick = { onWatchClick(movie) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "detail_watch_button"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryButton(
                            text = "Trailer",
                            icon = Icons.Default.Videocam,
                            onClick = { onTrailerClick(movie) },
                            modifier = Modifier.weight(1f),
                            testTag = "detail_trailer_button"
                        )

                        SecondaryButton(
                            text = if (isInWatchlist) "In My List" else "Add to List",
                            icon = if (isInWatchlist) Icons.Default.Check else Icons.Default.BookmarkBorder,
                            onClick = onToggleWatchlist,
                            modifier = Modifier.weight(1f),
                            testTag = "detail_watchlist_button"
                        )
                    }

                    // Dedicated Offline Download Button
                    if (downloadProgress != null && downloadProgress in 0..99) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CineGold.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = CineSurfaceElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { downloadProgress / 100f },
                                    modifier = Modifier.size(20.dp),
                                    color = CineGold,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Downloading for Offline Watching ($downloadProgress%)",
                                    color = CineGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (isDownloaded) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CineGreen.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = CineSurfaceElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CineGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Downloaded • Ready Offline",
                                        color = CineGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                TextButton(
                                    onClick = { onDeleteDownloadClick(movie.id) }
                                ) {
                                    Text(
                                        text = "Delete",
                                        color = CineTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        SecondaryButton(
                            text = "Download for Offline Watching",
                            icon = Icons.Default.Download,
                            onClick = { onDownloadClick(movie) },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "detail_download_button"
                        )
                    }

                    // Private storage reassurance note
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CineGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Stored privately in Downloads tab • Never exported to phone gallery",
                            color = CineTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Synopsis / Description
            item(key = "synopsis") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (!isOnline) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CineGold.copy(alpha = 0.12f))
                                .border(BorderStroke(0.8.dp, CineGold.copy(alpha = 0.45f)), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = CineGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Offline Mode: Movie information, storyline, and metadata loaded from local Room database cache.",
                                color = CineTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Text(
                        text = "Storyline",
                        color = CineTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = movie.description,
                        color = CineTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            // Official Trailer Auto-Preview Card
            if (movie.trailerUrl.isNotBlank()) {
                item(key = "trailer_preview_card") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Official Trailer",
                                color = CineTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CineRedPrimary.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, CineRedPrimary.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "PREVIEW READY",
                                    color = CineRedPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clickable { onTrailerClick(movie) }
                                .testTag("detail_trailer_preview_card"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            colors = CardDefaults.cardColors(containerColor = CineSurfaceElevated)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(movie.backdropUrl.ifBlank { movie.posterUrl })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Trailer Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xBB070709)
                                                )
                                            )
                                        )
                                )

                                // Play trailer pulsing action button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(CineRedPrimary.copy(alpha = 0.9f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Watch Trailer",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                // Bottom text label
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${movie.title} - Official Teaser",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Tap to watch HD trailer preview automatically",
                                            color = CineTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xCC000000)
                                    ) {
                                        Text(
                                            text = "2:25",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Cast Section
            item(key = "cast_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Cast & Crew",
                        color = CineTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(movie.cast) { member ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(84.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(member.avatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = member.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, CineCardBorder, CircleShape)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = member.name,
                                    color = CineTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = member.character,
                                    color = CineTextMuted,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Studio / Metadata Info
            item(key = "meta_info") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Director", color = CineTextMuted, fontSize = 11.sp)
                        Text(text = movie.director, color = CineTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Column {
                        Text(text = "Studio", color = CineTextMuted, fontSize = 11.sp)
                        Text(text = movie.studio, color = CineTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Column {
                        Text(text = "Release", color = CineTextMuted, fontSize = 11.sp)
                        Text(text = movie.releaseDate, color = CineTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // "More Like This" Recommendations from /api/movies/movie/:id/recommendations
            if (isLoadingRecommendations) {
                item(key = "rec_loading") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = CineRedPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Finding recommendations...",
                            color = CineTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (recommendations.isNotEmpty()) {
                item(key = "recommended_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "More Like This",
                            color = CineTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item(key = "recommended_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recommendations, key = { it.id }) { recMovie ->
                            MovieCard(
                                movie = recMovie,
                                onClick = { onRelatedMovieClick(recMovie) }
                            )
                        }
                    }
                }
            }

            // Bottom Spacer
            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
        }

        // Floating Back Button with statusBarsPadding
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x88070709))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .testTag("details_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
