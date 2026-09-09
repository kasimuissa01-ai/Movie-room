package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
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

@Composable
fun AdminEditMovieDialog(
    movie: Movie,
    viewModel: MovieViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Form fields pre-filled from the movie
    var title by remember { mutableStateOf(movie.title) }
    var description by remember { mutableStateOf(movie.description) }
    var category by remember { mutableStateOf(movie.category.ifBlank { "Action" }) }
    var genres by remember { mutableStateOf(if (movie.genres.isNotEmpty()) movie.genres.joinToString(", ") else "Action") }
    var year by remember { mutableStateOf(movie.year.toString()) }
    var durationMinutes by remember { mutableStateOf(movie.durationMinutes.toString()) }
    var rating by remember { mutableStateOf(movie.rating.toString()) }

    // Cover / Poster
    var selectedPosterUri by remember { mutableStateOf<Uri?>(null) }
    var directPosterUrl by remember { mutableStateOf(movie.posterUrl) }

    // Trailer
    var selectedTrailerUri by remember { mutableStateOf<Uri?>(null) }
    var directTrailerUrl by remember { mutableStateOf(movie.trailerUrl) }

    // Video link (if changed)
    var directVideoUrl by remember { mutableStateOf(movie.videoUrl) }

    var isHeroFeatured by remember { mutableStateOf(movie.featured) }

    val isUploading = viewModel.isUploading.value
    val uploadProgress = viewModel.uploadProgress.value
    val uploadStatusText = viewModel.uploadStatusText.value

    // Image media picker for Poster
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPosterUri = uri
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
                // Header Bar
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
                                .background(CineGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Movie",
                                tint = CineGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Edit Movie Details",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineTextPrimary
                            )
                            Text(
                                text = "Modify title, description, cover image, or trailer",
                                fontSize = 12.sp,
                                color = CineTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (!isUploading) onDismiss() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CineSurfaceElevated)
                            .testTag("admin_edit_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CineTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Progress Bar when saving
                AnimatedVisibility(visible = isUploading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = uploadStatusText.ifBlank { "Saving changes..." },
                                fontSize = 12.sp,
                                color = CineGold
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
                                .height(6.dp)
                                .clip(CircleShape),
                            color = CineGold,
                            trackColor = CineSurfaceElevated
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title Field
                    Text("Movie Title *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CineTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Movie Title", color = CineTextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_movie_title_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description / Synopsis Field
                    Text("Synopsis / Description *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CineTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Movie plot summary...", color = CineTextMuted) },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_movie_description_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category & Genres
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

                    // Year, Duration, Rating
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
                                    text = if (selectedPosterUri != null) "New Image Selected" else if (directPosterUrl.isNotBlank()) "Current Cover Image" else "No cover selected",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CineTextPrimary
                                )
                                Text(
                                    text = if (selectedPosterUri != null) "Ready to upload" else "Tap 'Change Image' or edit URL below",
                                    fontSize = 11.sp,
                                    color = if (selectedPosterUri != null) CineGreen else CineTextMuted,
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
                                Text("Change Image", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = directPosterUrl,
                        onValueChange = {
                            directPosterUrl = it
                            selectedPosterUri = null
                        },
                        placeholder = { Text("Or paste image link (https://...)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_movie_poster_url_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: Trailer (Optional / Updatable)
                    Text(
                        text = "Movie Trailer",
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
                                    text = if (selectedTrailerUri != null) "New Trailer Selected" else if (directTrailerUrl.isNotBlank()) "Trailer Configured" else "Choose Trailer Video (Optional)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CineTextPrimary
                                )
                                Text(
                                    text = if (selectedTrailerUri != null) selectedTrailerUri?.lastPathSegment ?: "Ready to upload" else if (directTrailerUrl.isNotBlank()) "Direct URL configured" else "Short preview video that plays automatically",
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
                        onValueChange = {
                            directTrailerUrl = it
                            selectedTrailerUri = null
                        },
                        placeholder = { Text("Or paste trailer link (https://...)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_movie_trailer_url_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hero Carousel Toggle
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CineSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Feature on Home Banner",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CineTextPrimary
                                )
                                Text(
                                    text = "Show prominently in the hero carousel at the top of Home",
                                    fontSize = 11.sp,
                                    color = CineTextMuted
                                )
                            }
                            Switch(
                                checked = isHeroFeatured,
                                onCheckedChange = { isHeroFeatured = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CineRedPrimary,
                                    uncheckedThumbColor = CineTextMuted,
                                    uncheckedTrackColor = CineSurfaceElevated
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Action Buttons: Cancel and Save Changes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (!isUploading) onDismiss() },
                        enabled = !isUploading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextPrimary)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Please provide a movie title", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val parsedYear = year.toIntOrNull() ?: movie.year
                            val parsedDuration = durationMinutes.toIntOrNull() ?: movie.durationMinutes
                            val parsedRating = rating.toFloatOrNull() ?: movie.rating

                            viewModel.updateMovie(
                                context = context,
                                movieId = movie.id,
                                title = title,
                                description = description,
                                category = category,
                                genres = genres,
                                year = parsedYear,
                                durationMinutes = parsedDuration,
                                rating = parsedRating,
                                posterUri = selectedPosterUri,
                                trailerUri = selectedTrailerUri,
                                directPosterUrl = directPosterUrl,
                                directTrailerUrl = directTrailerUrl,
                                directVideoUrl = directVideoUrl,
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
                            .testTag("admin_edit_save_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CineGold)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = CineBlack,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...", color = CineBlack, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = CineBlack,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Changes", color = CineBlack, fontWeight = FontWeight.Bold)
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
    focusedBorderColor = CineGold,
    unfocusedBorderColor = Color(0x33FFFFFF),
    focusedContainerColor = CineSurface,
    unfocusedContainerColor = CineSurface,
    cursorColor = CineGold
)
