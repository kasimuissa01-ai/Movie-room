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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Movie
import com.example.ui.components.EmptyState
import com.example.ui.components.EmptyWatchlistIllustration
import com.example.ui.components.MovieGrid
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineCardBorder
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineGreen
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineSurfaceElevated
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import com.example.ui.theme.CineTextSecondary

enum class WatchlistSort(val label: String) {
    RECENTLY_ADDED("Recently Added"),
    RATING("Highest Rating"),
    TITLE("Title (A-Z)")
}

@Composable
fun MyListScreen(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    onDiscoverClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
    cachedMoviesCount: Int = 0
) {
    var currentSort by remember { mutableStateOf(WatchlistSort.RECENTLY_ADDED) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedMovies = remember(movies, currentSort) {
        when (currentSort) {
            WatchlistSort.RECENTLY_ADDED -> movies
            WatchlistSort.RATING -> movies.sortedByDescending { it.rating }
            WatchlistSort.TITLE -> movies.sortedBy { it.title }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineBlack)
            .statusBarsPadding()
            .testTag("my_list_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row: "My List (N)" + Sort Dropdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "My List",
                        color = CineTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (movies.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CineRedPrimary
                        ) {
                            Text(
                                text = "${movies.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Sort selector
                if (movies.isNotEmpty()) {
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CineSurfaceElevated)
                                .clickable { showSortMenu = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = CineTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = currentSort.label,
                                color = CineTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = CineTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            WatchlistSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.label) },
                                    onClick = {
                                        currentSort = sort
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Room Database Offline Cache Status Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (!isOnline) CineGold.copy(alpha = 0.12f)
                        else CineSurfaceElevated.copy(alpha = 0.6f)
                    )
                    .border(
                        width = 0.8.dp,
                        color = if (!isOnline) CineGold.copy(alpha = 0.45f) else CineCardBorder.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (!isOnline) Icons.Default.CloudOff else Icons.Default.Storage,
                    contentDescription = null,
                    tint = if (!isOnline) CineGold else CineGreen,
                    modifier = Modifier.size(18.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!isOnline) "Offline Mode Active" else "Room Local Cache Protected",
                        color = if (!isOnline) CineGold else CineTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (!isOnline)
                            "Your saved Watchlist movies and basic info are stored locally and accessible without internet."
                        else
                            "Watchlist titles and basic movie info are persisted in Room database for offline viewing.",
                        color = CineTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (!isOnline) CineGold.copy(alpha = 0.2f) else CineGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (!isOnline) "OFFLINE" else "ROOM DB",
                        color = if (!isOnline) CineGold else CineGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body
            if (sortedMovies.isEmpty()) {
                EmptyState(
                    illustration = { EmptyWatchlistIllustration() },
                    title = "Your watchlist is waiting.",
                    subtitle = "Save movies here to cache their details in Room and browse them even while offline.",
                    actionButton = {
                        PrimaryButton(
                            text = "Discover Movies",
                            icon = Icons.Default.Explore,
                            onClick = onDiscoverClick,
                            testTag = "watchlist_discover_button"
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                MovieGrid(
                    movies = sortedMovies,
                    onMovieClick = onMovieClick,
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp)
                )
            }
        }
    }
}
