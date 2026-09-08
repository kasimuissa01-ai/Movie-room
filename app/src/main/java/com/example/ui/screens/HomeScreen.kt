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

    val isFullEmpty = uploadedMovies.isEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineBlack)
            .testTag("home_screen")
    ) {
        if (isFullEmpty) {
            // Clean Empty State when storage database has no uploaded movies yet
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(CineRedPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "No Movies in Storage",
                        tint = CineRedPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Storage Database Empty",
                    color = CineTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isAdminLoggedIn) {
                        "Your database is ready. Tap 'Upload Movie' below to add movies to your storage."
                    } else {
                        "No movies found in your storage database. Log in as admin to upload and stream movies."
                    },
                    color = CineTextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onUploadClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isAdminLoggedIn) Icons.Default.CloudUpload else Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAdminLoggedIn) "Upload Movie" else "Admin Access / Refresh",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Main Content Feed from Storage Database
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Hero / Featured Carousel from storage database
                val featuredUploads = uploadedMovies.filter { it.featured }
                val heroMovies = featuredUploads.ifEmpty { uploadedMovies }
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

                // Section 1: All Storage Movies / Featured
                item(key = "storage_movies_section") {
                    CategorySection(
                        title = if (featuredUploads.isNotEmpty()) "Featured Movies" else "All Movies in Storage",
                        movies = uploadedMovies,
                        onSeeAllClick = { onSeeAllClick("storage", "Storage Movies") },
                        onMovieClick = onMovieClick
                    )
                }

                // Dynamic genre sections if movies in storage contain specific genres
                val genresInStorage = uploadedMovies
                    .flatMap { it.genres }
                    .filter { it.isNotBlank() }
                    .distinct()

                genresInStorage.forEach { genreName ->
                    val matching = uploadedMovies.filter { movie ->
                        movie.genres.any { it.equals(genreName, ignoreCase = true) } ||
                                movie.category.equals(genreName, ignoreCase = true)
                    }
                    if (matching.isNotEmpty()) {
                        item(key = "genre_sec_$genreName") {
                            CategorySection(
                                title = "$genreName Movies",
                                movies = matching,
                                onSeeAllClick = { onSeeAllClick(genreName, "$genreName Movies") },
                                onMovieClick = onMovieClick
                            )
                        }
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

