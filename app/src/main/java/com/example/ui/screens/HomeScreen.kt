package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Movie
import com.example.ui.HomeFeedState
import com.example.ui.components.CategorySection
import com.example.ui.components.CineHeader
import com.example.ui.components.HeroCarousel
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineSurfaceElevated
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import com.example.ui.theme.CineTextSecondary

@Composable
fun HomeScreen(
    homeFeedState: HomeFeedState,
    onRefresh: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    onWatchClick: (Movie) -> Unit,
    onSeeAllClick: (categoryKey: String, categoryTitle: String) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAdminLoggedIn: Boolean = false,
    uploadedMovies: List<Movie> = emptyList(),
    onUploadClick: () -> Unit = {},
    onDownloadClick: (Movie) -> Unit = {},
    downloadedMovieIds: Set<String> = emptySet(),
    downloadProgressMap: Map<String, Int> = emptyMap()
) {
    val scrollState = rememberLazyListState()

    val isFullEmpty = homeFeedState.trending.isEmpty() &&
            homeFeedState.popular.isEmpty() &&
            homeFeedState.action.isEmpty() &&
            homeFeedState.nowPlaying.isEmpty() &&
            homeFeedState.upcoming.isEmpty() &&
            uploadedMovies.isEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineBlack)
            .testTag("home_screen")
    ) {
        if (homeFeedState.isLoading && isFullEmpty) {
            // Full Screen Loading State
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
                    text = "Loading Movies...",
                    color = CineTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Connecting securely to Cloudflare Worker API",
                    color = CineTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else if (homeFeedState.isError && isFullEmpty) {
            // Full Screen Error State with Retry
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
                        contentDescription = "Connection Error",
                        tint = CineRedPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Unable to load movies",
                    color = CineTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = homeFeedState.errorMessage.ifEmpty { "Please check your network connection and try again." },
                    color = CineTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Retry",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Main Content Feed
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Hero / Featured Carousel -> /api/movies/trending
                val heroMovies = (uploadedMovies.take(1) + homeFeedState.trending.ifEmpty { homeFeedState.popular }).distinctBy { it.id }
                if (heroMovies.isNotEmpty()) {
                    item(key = "hero_carousel") {
                        HeroCarousel(
                            movies = heroMovies,
                            onMovieClick = onMovieClick,
                            onWatchClick = onWatchClick,
                            onDownloadClick = onDownloadClick,
                            isMovieDownloaded = { movieId -> downloadedMovieIds.contains(movieId) },
                            downloadProgress = { movieId -> downloadProgressMap[movieId] }
                        )
                    }

                    item(key = "hero_spacing") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // If Admin is logged in, show an Admin banner if no movies uploaded yet
                if (isAdminLoggedIn && uploadedMovies.isEmpty()) {
                    item(key = "admin_empty_banner") {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CineSurfaceElevated),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .clickable { onUploadClick() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(CineRedPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = CineRedPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Admin Mode Active",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = CineTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(CineGold)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "R2 READY",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tap here to add and publish new movies to the catalog.",
                                        fontSize = 12.sp,
                                        color = CineTextMuted
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Section for Admin Uploaded Movies
                if (uploadedMovies.isNotEmpty()) {
                    item(key = "admin_r2_movies_section") {
                        CategorySection(
                            title = "Featured Uploads",
                            movies = uploadedMovies,
                            onSeeAllClick = { onSeeAllClick("uploaded", "Featured Uploads") },
                            onMovieClick = onMovieClick
                        )
                    }
                }

                // 2. Popular Movies -> /api/movies/popular
                if (homeFeedState.popular.isNotEmpty()) {
                    item(key = "popular_movies_section") {
                        CategorySection(
                            title = "Popular Movies",
                            movies = homeFeedState.popular,
                            onSeeAllClick = { onSeeAllClick("popular", "Popular Movies") },
                            onMovieClick = onMovieClick
                        )
                    }
                }

                // 3. Action Movies -> /api/movies/action
                if (homeFeedState.action.isNotEmpty()) {
                    item(key = "action_movies_section") {
                        CategorySection(
                            title = "Action & Adventure",
                            movies = homeFeedState.action,
                            onSeeAllClick = { onSeeAllClick("action", "Action & Adventure") },
                            onMovieClick = onMovieClick
                        )
                    }
                }

                // 4. Now Playing -> /api/movies/now-playing
                if (homeFeedState.nowPlaying.isNotEmpty()) {
                    item(key = "now_playing_section") {
                        CategorySection(
                            title = "Now Playing in Theatres",
                            movies = homeFeedState.nowPlaying,
                            onSeeAllClick = { onSeeAllClick("now_playing", "Now Playing") },
                            onMovieClick = onMovieClick
                        )
                    }
                }

                // 5. Upcoming -> /api/movies/upcoming
                if (homeFeedState.upcoming.isNotEmpty()) {
                    item(key = "upcoming_section") {
                        CategorySection(
                            title = "Upcoming Releases",
                            movies = homeFeedState.upcoming,
                            onSeeAllClick = { onSeeAllClick("upcoming", "Upcoming Releases") },
                            onMovieClick = onMovieClick
                        )
                    }
                }

                // Bottom space for bottom navigation bar and FAB
                item(key = "bottom_spacing") {
                    Spacer(modifier = Modifier.height(96.dp))
                }
            }
        }

        // Overlay Header with Gradient Scrim
        CineHeader(
            onSearchClick = onSearchClick,
            onProfileClick = onProfileClick,
            isAdminLoggedIn = isAdminLoggedIn,
            onUploadClick = onUploadClick,
            modifier = Modifier.fillMaxWidth()
        )

        // Floating Action Button ONLY visible to Admin
        if (isAdminLoggedIn) {
            ExtendedFloatingActionButton(
                onClick = onUploadClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload Movie",
                        tint = Color.White
                    )
                },
                text = {
                    Text(
                        text = "Upload Movie",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                containerColor = CineRedPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
                    .testTag("admin_upload_fab")
            )
        }
    }
}

