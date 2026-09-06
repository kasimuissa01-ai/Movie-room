package com.example.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.model.Movie
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineCardBorder
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineGreen
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineSurfaceElevated
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import com.example.ui.theme.CineTextSecondary
import kotlinx.coroutines.delay

@Composable
fun HeroCarousel(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    onWatchClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 490,
    onDownloadClick: (Movie) -> Unit = {},
    isMovieDownloaded: (String) -> Boolean = { false },
    downloadProgress: (String) -> Int? = { null }
) {
    if (movies.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { movies.size })
    val coroutineScope = rememberCoroutineScope()

    // Auto-advance every 6 seconds unless user is interacting
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            while (true) {
                delay(6000)
                val nextPage = (pagerState.currentPage + 1) % movies.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 800)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .testTag("hero_carousel")
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val movie = movies[page]

            Box(modifier = Modifier.fillMaxSize()) {
                // Large Backdrop Artwork
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(movie.backdropUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Gradient Scrim for status bar and header icons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xEE070709),
                                    Color(0x66070709),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Multi-stop Deep Bottom Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x22070709),
                                    Color(0x99070709),
                                    Color(0xEE070709),
                                    CineBlack
                                ),
                                startY = 120f
                            )
                        )
                )

                // Movie Information Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    // Category & Quality Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QualityBadge(quality = movie.quality)
                        Text(
                            text = movie.genres.joinToString(" • "),
                            color = CineRedPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "•",
                            color = CineTextMuted,
                            fontSize = 12.sp
                        )
                        RatingBadge(rating = movie.rating)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Title
                    Text(
                        text = movie.title.uppercase(),
                        color = CineTextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Short description
                    Text(
                        text = movie.description,
                        color = CineTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons: [ Watch Now ] and [ Download ] with status card & details info
                    val isDownloaded = isMovieDownloaded(movie.id)
                    val progress = downloadProgress(movie.id)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. WATCH NOW / PLAY
                        PrimaryButton(
                            text = if (isDownloaded) "Watch Offline" else "Watch Now",
                            icon = Icons.Default.PlayArrow,
                            onClick = { onWatchClick(movie) },
                            modifier = Modifier.weight(1.1f),
                            testTag = "hero_watch_button"
                        )

                        // 2. DOWNLOAD ACTION BUTTON / STATUS CARD (Directly facilitates downloading)
                        if (progress != null && progress in 0..99) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CineGold.copy(alpha = 0.6f)),
                                colors = CardDefaults.cardColors(containerColor = CineSurfaceElevated),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("hero_downloading_${movie.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { progress / 100f },
                                        modifier = Modifier.size(16.dp),
                                        color = CineGold,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$progress%",
                                        color = CineGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else if (isDownloaded) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CineGreen.copy(alpha = 0.6f)),
                                colors = CardDefaults.cardColors(containerColor = CineSurfaceElevated),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable { onWatchClick(movie) }
                                    .testTag("hero_downloaded_${movie.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CineGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Offline",
                                        color = CineGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            SecondaryButton(
                                text = "Download",
                                icon = Icons.Default.Download,
                                onClick = { onDownloadClick(movie) },
                                modifier = Modifier.weight(1f),
                                testTag = "hero_download_button"
                            )
                        }

                        // 3. Compact Info button to view full movie details & synopsis
                        IconButton(
                            onClick = { onMovieClick(movie) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CineSurfaceElevated)
                                .border(BorderStroke(1.dp, CineCardBorder), RoundedCornerShape(12.dp))
                                .testTag("hero_more_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "More Info",
                                tint = CineTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reassurance note: in-app private download, never in phone gallery
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CineGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Private In-App Download • Saved in Downloads tab (never in phone gallery)",
                            color = CineTextMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Small Carousel Indicators (dots)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(movies.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(if (isSelected) 22.dp else 6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isSelected) CineRedPrimary else Color(0x66FFFFFF)
                        )
                )
            }
        }
    }
}
