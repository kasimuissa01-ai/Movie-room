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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
fun AdminUploadMovieDialog(
    viewModel: MovieViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Action") }
    var genres by remember { mutableStateOf("Action, Sci-Fi, 4K") }
    var year by remember { mutableStateOf("2026") }
    var durationMinutes by remember { mutableStateOf("125") }
    var rating by remember { mutableStateOf("8.8") }

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedTrailerUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPosterUri by remember { mutableStateOf<Uri?>(null) }
    var directVideoUrl by remember { mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4") }
    var directTrailerUrl by remember { mutableStateOf("") }
    var directPosterUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&q=80") }

    val isUploading = viewModel.isUploading.value
    val uploadProgress = viewModel.uploadProgress.value
    val uploadStatusText = viewModel.uploadStatusText.value

    val isR2Configured = viewModel.isR2Configured
    val r2Bucket = viewModel.r2BucketName

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
                                .background(CineRedPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Admin Upload",
                                tint = CineRedPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Upload Movie",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineTextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CineRedPrimary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = "Cloudflare R2 Object Storage",
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

                // Cloudflare R2 Storage Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CineSurfaceElevated)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = if (isR2Configured) CineGreen else CineGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "R2 Bucket: $r2Bucket",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CineTextPrimary
                                )
                                Text(
                                    text = if (isR2Configured) "Active AWS SigV4 R2 Pipeline" else "Demo Mode (Set R2 in Secrets panel)",
                                    fontSize = 11.sp,
                                    color = if (isR2Configured) CineGreen else CineGold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isR2Configured) CineGreen.copy(alpha = 0.2f) else CineGold.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isR2Configured) "R2 ONLINE" else "READY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isR2Configured) CineGreen else CineGold
                            )
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
                        placeholder = { Text("e.g. Cyberpunk: Neon Dawn") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_upload_title_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description / Synopsis
                    Text("Synopsis / Description", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CineTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Enter movie overview...") },
                        maxLines = 3,
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
                            Text("Duration (m)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CineTextPrimary)
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

                    Spacer(modifier = Modifier.height(18.dp))

                    // Section: Video Media
                    Text(
                        text = "Movie Video File (Cloudflare R2)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Video file selector button
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
                            if (selectedVideoUri != null) CineGreen else Color(0x33FFFFFF)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedVideoUri != null) Icons.Default.CheckCircle else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (selectedVideoUri != null) CineGreen else CineRedPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedVideoUri != null) "Video File Selected" else "Select Movie Video File (MP4/MKV)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CineTextPrimary
                                )
                                Text(
                                    text = if (selectedVideoUri != null) selectedVideoUri?.lastPathSegment ?: "Ready to upload" else "Tap to choose from device gallery or storage",
                                    fontSize = 11.sp,
                                    color = CineTextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Or direct video stream URL
                    OutlinedTextField(
                        value = directVideoUrl,
                        onValueChange = { directVideoUrl = it },
                        label = { Text("Or Cloudflare R2 Direct Streaming URL") },
                        placeholder = { Text("https://pub-xxx.r2.dev/movies/movie.mp4") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: Trailer Video (Short preview)
                    Text(
                        text = "Movie Trailer (Short Preview Video)",
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
                            if (selectedTrailerUri != null) CineGreen else Color(0x33FFFFFF)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedTrailerUri != null) Icons.Default.CheckCircle else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (selectedTrailerUri != null) CineGreen else CineGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedTrailerUri != null) "Trailer Video Selected" else "Pick Short Trailer Video (MP4/MKV)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CineTextPrimary
                                )
                                Text(
                                    text = if (selectedTrailerUri != null) selectedTrailerUri?.lastPathSegment ?: "Ready to upload" else "Short teaser / preview that auto-plays on movie page",
                                    fontSize = 11.sp,
                                    color = CineTextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = directTrailerUrl,
                        onValueChange = { directTrailerUrl = it },
                        label = { Text("Or Direct Trailer Streaming URL") },
                        placeholder = { Text("https://pub-xxx.r2.dev/trailers/trailer.mp4") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: Poster Image
                    Text(
                        text = "Movie Poster Artwork",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CineSurface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedPosterUri != null) CineGreen else Color(0x33FFFFFF)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedPosterUri != null) Icons.Default.CheckCircle else Icons.Default.Image,
                                contentDescription = null,
                                tint = if (selectedPosterUri != null) CineGreen else CineGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedPosterUri != null) "Poster Image Selected" else "Pick Poster Artwork Image",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CineTextPrimary
                                )
                                Text(
                                    text = if (selectedPosterUri != null) selectedPosterUri?.lastPathSegment ?: "Ready to upload" else "Tap to choose image file",
                                    fontSize = 11.sp,
                                    color = CineTextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = directPosterUrl,
                        onValueChange = { directPosterUrl = it },
                        label = { Text("Or Poster Image URL") },
                        placeholder = { Text("https://image.tmdb.org/... or https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = outlinedFieldColors()
                    )

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
                                text = uploadStatusText.ifBlank { "Uploading to Cloudflare R2..." },
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
                                Toast.makeText(context, "Please enter movie title", Toast.LENGTH_SHORT).show()
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
                                directTrailerUrl = directTrailerUrl
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
                            Text("Uploading...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload & Publish")
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
