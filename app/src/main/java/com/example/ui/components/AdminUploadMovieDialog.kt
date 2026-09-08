package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.tmdb.TmdbClient
import com.example.model.Movie
import com.example.ui.MovieViewModel
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineGreen
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineSurface
import com.example.ui.theme.CineSurfaceElevated
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdminUploadMovieDialog(
    viewModel: MovieViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Autofill Search Query State
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var autofillSuccessBanner by remember { mutableStateOf<String?>(null) }

    // Form fields (Auto-populated by TMDB or editable manually)
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Action") }
    var genres by remember { mutableStateOf("Action, Drama") }
    var year by remember { mutableStateOf("2026") }
    var durationMinutes by remember { mutableStateOf("120") }
    var rating by remember { mutableStateOf("8.5") }

    // Media fields
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedTrailerUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPosterUri by remember { mutableStateOf<Uri?>(null) }
    var directVideoUrl by remember { mutableStateOf("") }
    var directTrailerUrl by remember { mutableStateOf("") }
    var directPosterUrl by remember { mutableStateOf("") }
    var isHeroFeatured by remember { mutableStateOf(true) }

    val isUploading = viewModel.isUploading.value
    val uploadProgress = viewModel.uploadProgress.value
    val uploadStatusText = viewModel.uploadStatusText.value

    // Auto-search TMDB as user types in the title / search bar
    fun triggerTmdbSearch(query: String) {
        searchJob?.cancel()
        if (query.trim().length < 2) {
            searchResults = emptyList()
            isSearching = false
            return
        }
        searchJob = coroutineScope.launch {
            delay(350)
            isSearching = true
            try {
                val results = TmdbClient.searchMovies(query.trim())
                searchResults = results
            } catch (e: Exception) {
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    // Function to populate all fields from a selected TMDB movie
    fun applyTmdbMovie(movie: Movie) {
        title = movie.title
        description = movie.description
        category = movie.category.ifBlank { "Action" }
        genres = if (movie.genres.isNotEmpty()) movie.genres.joinToString(", ") else "Movie"
        year = movie.year.toString()
        durationMinutes = movie.durationMinutes.toString()
        rating = movie.rating.toString()
        directPosterUrl = movie.posterUrl
        directTrailerUrl = movie.trailerUrl
        autofillSuccessBanner = "Filled details for \"${movie.title}\""
        searchResults = emptyList()
        searchQuery = movie.title
        focusManager.clearFocus()
    }

    // Video media picker using zero-permission Photo Picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
        }
    }

    // Short trailer media picker
    val trailerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedTrailerUri = uri
        }
    }

    // Image media picker for Poster
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPosterUri = uri
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isUploading) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp),
            color = CineBlack,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header Bar (Clean, simple, non-technical)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CineRedPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Upload Movie",
                                tint = CineRedPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Add New Movie",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineTextPrimary
                            )
                            Text(
                                text = "Search to auto-fill details, or fill manually",
                                fontSize = 12.sp,
                                color = CineTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (!isUploading) onDismiss() },
                        enabled = !isUploading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CineTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TMDB Auto-Fill Smart Search Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CineSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CineGold.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CineGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Auto-Fill with TMDB Movie Search",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineGold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                triggerTmdbSearch(it)
                            },
                            placeholder = { Text("Search movie title metadata...", fontSize = 13.sp) },
                            singleLine = true,
                            leadingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = CineGold,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = CineGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        searchResults = emptyList()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = CineTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                triggerTmdbSearch(searchQuery)
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_tmdb_search_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CineTextPrimary,
                                unfocusedTextColor = CineTextPrimary,
                                focusedBorderColor = CineGold,
                                unfocusedBorderColor = CineGold.copy(alpha = 0.3f),
                                focusedContainerColor = CineSurface,
                                unfocusedContainerColor = CineSurface,
                                cursorColor = CineGold
                            )
                        )

                        // TMDB Live Results Horizontal Selector
                        if (searchResults.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Tap a movie to auto-fill title, cover, and synopsis:",
                                fontSize = 11.sp,
                                color = CineTextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(searchResults) { tmdbMovie ->
                                    Card(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clickable { applyTmdbMovie(tmdbMovie) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = CineSurface),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CineRedPrimary.copy(alpha = 0.6f))
                                    ) {
                                        Column {
                                            AsyncImage(
                                                model = tmdbMovie.posterUrl,
                                                contentDescription = tmdbMovie.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(80.dp)
                                                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                                    .background(CineSurface)
                                            )
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text(
                                                    text = tmdbMovie.title,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CineTextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = CineGold,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "${tmdbMovie.rating} • ${tmdbMovie.year}",
                                                        fontSize = 10.sp,
                                                        color = CineTextMuted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Success feedback toast inside card
                        if (autofillSuccessBanner != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CineGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CineGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = autofillSuccessBanner ?: "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CineGreen
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Movie Title
                    Text("Movie Title *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CineTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. Movie Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_upload_title_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description / Synopsis
                    Text("Story / Description", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CineTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Movie story summary...") },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_upload_description_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row: Category & Genres
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CineTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = outlinedFieldColors()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Genres", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CineTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = genres,
                                onValueChange = { genres = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = outlinedFieldColors()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row: Year, Duration, Rating
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Year", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CineTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = year,
                                onValueChange = { year = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = outlinedFieldColors()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Duration (mins)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CineTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = durationMinutes,
                                onValueChange = { durationMinutes = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = outlinedFieldColors()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rating", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CineTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = rating,
                                onValueChange = { rating = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = outlinedFieldColors()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: Cover / Poster Image
                    Text(
                        text = "Cover Image",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Poster Preview & Selection Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CineSurface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (directPosterUrl.isNotBlank() || selectedPosterUri != null) CineGreen else Color(0x33FFFFFF)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (directPosterUrl.isNotBlank() || selectedPosterUri != null) {
                                AsyncImage(
                                    model = selectedPosterUri ?: directPosterUrl,
                                    contentDescription = "Cover Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp, 75.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CineSurfaceElevated)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = CineGold,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (directPosterUrl.isNotBlank() || selectedPosterUri != null) "Cover Image Ready" else "No cover selected",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CineTextPrimary
                                )
                                Text(
                                    text = if (directPosterUrl.isNotBlank()) "Auto-filled from TMDB" else if (selectedPosterUri != null) "Selected from device" else "Auto-fills when you search, or pick a file",
                                    fontSize = 11.sp,
                                    color = if (directPosterUrl.isNotBlank()) CineGreen else CineTextMuted,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CineGold)
                            ) {
                                Text("Choose File", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = directPosterUrl,
                        onValueChange = { directPosterUrl = it },
                        placeholder = { Text("Or paste image link (https://...)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: Full Video File
                    Text(
                        text = "Movie Video",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CineSurface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedVideoUri != null || directVideoUrl.isNotBlank()) CineGreen else Color(0x33FFFFFF)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedVideoUri != null || directVideoUrl.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (selectedVideoUri != null || directVideoUrl.isNotBlank()) CineGreen else CineRedPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedVideoUri != null) "Video File Selected" else if (directVideoUrl.isNotBlank()) "Video Link Configured" else "Select Video File (MP4/MKV)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CineTextPrimary
                                )
                                Text(
                                    text = if (selectedVideoUri != null) selectedVideoUri?.lastPathSegment ?: "Ready to play" else if (directVideoUrl.isNotBlank()) "Direct link provided below" else "Tap to choose video from your phone",
                                    fontSize = 11.sp,
                                    color = CineTextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = directVideoUrl,
                        onValueChange = { directVideoUrl = it },
                        placeholder = { Text("Or paste video link (https://...)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: Trailer (Optional)
                    Text(
                        text = "Movie Trailer (Optional)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                trailerPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CineSurface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedTrailerUri != null || directTrailerUrl.isNotBlank()) CineGreen else Color(0x33FFFFFF)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedTrailerUri != null || directTrailerUrl.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (selectedTrailerUri != null || directTrailerUrl.isNotBlank()) CineGreen else CineGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedTrailerUri != null) "Trailer Video Selected" else if (directTrailerUrl.isNotBlank()) "Trailer Link Auto-Filled" else "Choose Trailer Video (Optional)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CineTextPrimary
                                )
                                Text(
                                    text = if (selectedTrailerUri != null) selectedTrailerUri?.lastPathSegment ?: "Ready" else if (directTrailerUrl.isNotBlank()) "Auto-filled or custom trailer link" else "Short preview video that plays automatically",
                                    fontSize = 11.sp,
                                    color = CineTextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = directTrailerUrl,
                        onValueChange = { directTrailerUrl = it },
                        placeholder = { Text("Or paste trailer link (https://...)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Marketing & Hero Carousel Placement Toggle Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isHeroFeatured = !isHeroFeatured },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CineSurface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isHeroFeatured) CineGold.copy(alpha = 0.8f) else Color(0x33FFFFFF)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isHeroFeatured) CineGold.copy(alpha = 0.2f) else Color(0x22FFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isHeroFeatured) CineGold else CineTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Feature in Hero Carousel",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHeroFeatured) CineGold else CineTextPrimary
                                    )
                                    Text(
                                        text = "Showcase this movie on top marketing carousel",
                                        fontSize = 11.sp,
                                        color = CineTextMuted
                                    )
                                }
                            }
                            androidx.compose.material3.Switch(
                                checked = isHeroFeatured,
                                onCheckedChange = { isHeroFeatured = it },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = CineGold,
                                    uncheckedThumbColor = CineTextMuted,
                                    uncheckedTrackColor = CineSurfaceElevated
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Upload Progress State Box
                AnimatedVisibility(visible = isUploading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = uploadStatusText.ifBlank { "Saving movie..." },
                                fontSize = 12.sp,
                                color = CineGold,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(uploadProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = CineGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CineRedPrimary,
                            trackColor = CineSurface
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (!isUploading) onDismiss() },
                        enabled = !isUploading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = CineTextMuted)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Please enter or select a movie title", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val parsedYear = year.toIntOrNull() ?: 2026
                            val parsedDuration = durationMinutes.toIntOrNull() ?: 120
                            val parsedRating = rating.toFloatOrNull() ?: 8.5f

                            viewModel.uploadMovie(
                                context = context,
                                title = title,
                                description = description,
                                category = category,
                                genres = genres,
                                year = parsedYear,
                                durationMinutes = parsedDuration,
                                rating = parsedRating,
                                videoUri = selectedVideoUri,
                                posterUri = selectedPosterUri,
                                trailerUri = selectedTrailerUri,
                                directVideoUrl = directVideoUrl,
                                directPosterUrl = directPosterUrl,
                                directTrailerUrl = directTrailerUrl,
                                isHeroFeatured = isHeroFeatured
                            ) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                if (success) {
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !isUploading,
                        modifier = Modifier
                            .weight(2f)
                            .testTag("admin_upload_submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save & Publish Movie")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = CineTextPrimary,
    unfocusedTextColor = CineTextPrimary,
    focusedBorderColor = CineRedPrimary,
    unfocusedBorderColor = Color(0x33FFFFFF),
    focusedContainerColor = CineSurface,
    unfocusedContainerColor = CineSurface,
    cursorColor = CineRedPrimary
)
