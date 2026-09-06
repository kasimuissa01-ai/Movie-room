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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleMovies
import com.example.model.Movie
import com.example.ui.components.CategorySection
import com.example.ui.components.CineHeader
import com.example.ui.components.HeroCarousel
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineSurfaceElevated
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary

@Composable
fun HomeScreen(
    featuredMovies: List<Movie>,
    categories: List<Pair<String, String>>,
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineBlack)
            .testTag("home_screen")
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Carousel at top with integrated Watch Now and Download action cards
            item(key = "hero_carousel") {
                HeroCarousel(
                    movies = (uploadedMovies.take(1) + featuredMovies).distinctBy { it.id },
                    onMovieClick = onMovieClick,
                    onWatchClick = onWatchClick,
                    onDownloadClick = onDownloadClick,
                    isMovieDownloaded = { movieId -> downloadedMovieIds.contains(movieId) },
                    downloadProgress = { movieId -> downloadProgressMap[movieId] }
                )
            }

            // Spacing
            item(key = "hero_spacing") {
                Spacer(modifier = Modifier.height(16.dp))
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
                                    text = "Tap here to upload movies directly to Cloudflare R2 storage.",
                                    fontSize = 12.sp,
                                    color = CineTextMuted
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Section for Admin Uploaded Movies from Cloudflare R2
            if (uploadedMovies.isNotEmpty()) {
                item(key = "admin_r2_movies_section") {
                    CategorySection(
                        title = "Cloudflare R2 Cinema (Uploaded)",
                        movies = uploadedMovies,
                        onSeeAllClick = { onSeeAllClick("uploaded", "Cloudflare R2 Cinema") },
                        onMovieClick = onMovieClick
                    )
                }
            }

            // Horizontally scrollable movie sections for each category
            items(categories, key = { it.first }) { (categoryKey, categoryTitle) ->
                val movies = SampleMovies.getMoviesForCategory(categoryKey)
                CategorySection(
                    title = categoryTitle,
                    movies = movies,
                    onSeeAllClick = { onSeeAllClick(categoryKey, categoryTitle) },
                    onMovieClick = onMovieClick
                )
            }

            // Bottom space for bottom navigation bar and FAB
            item(key = "bottom_spacing") {
                Spacer(modifier = Modifier.height(96.dp))
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
