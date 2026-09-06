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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.ui.theme.CineRedPrimary
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
    height: Int = 480
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

                    // Buttons: [ Watch Now ] [ More Info ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PrimaryButton(
                            text = "Watch Now",
                            icon = Icons.Default.PlayArrow,
                            onClick = { onWatchClick(movie) },
                            modifier = Modifier.weight(1f),
                            testTag = "hero_watch_button"
                        )
                        SecondaryButton(
                            text = "More Info",
                            icon = Icons.Default.Info,
                            onClick = { onMovieClick(movie) },
                            modifier = Modifier.weight(1f),
                            testTag = "hero_more_info_button"
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
