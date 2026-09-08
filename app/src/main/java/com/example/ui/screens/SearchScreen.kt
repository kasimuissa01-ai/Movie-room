package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.SampleMovies
import com.example.model.Movie
import com.example.ui.components.CineSearchBar
import com.example.ui.components.EmptySearchIllustration
import com.example.ui.components.EmptyState
import com.example.ui.components.GenreBadge
import com.example.ui.components.MovieGrid
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import com.example.ui.theme.CineTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedGenre: String?,
    onSelectGenre: (String?) -> Unit,
    searchResults: List<Movie>,
    recentSearches: List<String>,
    onSelectRecentSearch: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    isSearching: Boolean = false,
    searchErrorMessage: String? = null,
    onRetrySearch: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineBlack)
            .statusBarsPadding()
            .testTag("search_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                CineSearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    onClear = {
                        onQueryChange("")
                        onSelectGenre(null)
                    }
                )
            }

            // Genre Chips Filter Bar
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SampleMovies.popularGenres.take(6).forEach { genre ->
                    GenreBadge(
                        genre = genre,
                        isSelected = selectedGenre == genre,
                        onClick = { onSelectGenre(genre) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // State: If query is empty and no genre selected -> show discovery info (trending, recent)
            if (query.isBlank() && selectedGenre == null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    // Recent Searches
                    if (recentSearches.isNotEmpty()) {
                        item(key = "recent_header") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    color = CineTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = onClearRecentSearches) {
                                    Text(
                                        text = "Clear All",
                                        color = CineRedPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        items(recentSearches, key = { it }) { term ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onQueryChange(term)
                                        onSelectRecentSearch(term)
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = CineTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = term,
                                        color = CineTextSecondary,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onRemoveRecentSearch(term) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = CineTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        item(key = "divider_spacing") {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Trending Searches
                    item(key = "trending_header") {
                        Text(
                            text = "Trending Searches",
                            color = CineTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(SampleMovies.trendingSearches, key = { "trend_$it" }) { trend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onQueryChange(trend)
                                    onSelectRecentSearch(trend)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = CineRedPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = trend,
                                color = CineTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }

                    item(key = "bottom_space") {
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            } else if (isSearching) {
                // Loading search results
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
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Searching movies...",
                        color = CineTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else if (searchErrorMessage != null && searchResults.isEmpty()) {
                // Error search results
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(CineRedPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = CineRedPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Search request failed",
                        color = CineTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = searchErrorMessage,
                        color = CineTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onRetrySearch,
                        colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Showing Search Results
                if (searchResults.isEmpty()) {
                    EmptyState(
                        illustration = { EmptySearchIllustration() },
                        title = "No matches found",
                        subtitle = "We couldn't find anything matching '$query'. Try searching for another movie, actor, or genre.",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "${searchResults.size} results found",
                            color = CineTextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )

                        MovieGrid(
                            movies = searchResults,
                            onMovieClick = onMovieClick,
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp)
                        )
                    }
                }
            }
        }
    }
}
