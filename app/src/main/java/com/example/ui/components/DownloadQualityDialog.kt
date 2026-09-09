package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.Movie
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineCardBorder
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineGreen
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineSurface
import com.example.ui.theme.CineSurfaceElevated
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import com.example.ui.theme.CineTextSecondary

data class DownloadQualityOption(
    val label: String,
    val resolution: String,
    val estimatedSizeMb: Int,
    val description: String,
    val isRecommended: Boolean = false,
    val badgeText: String? = null
)

val defaultQualityOptions = listOf(
    DownloadQualityOption(
        label = "1080p Full HD",
        resolution = "1920x1080",
        estimatedSizeMb = 380,
        description = "High definition • Crisp detail on large displays",
        badgeText = "BEST QUALITY"
    ),
    DownloadQualityOption(
        label = "720p HD",
        resolution = "1280x720",
        estimatedSizeMb = 180,
        description = "Optimized HD • Perfect balance of quality & storage",
        isRecommended = true,
        badgeText = "RECOMMENDED"
    ),
    DownloadQualityOption(
        label = "480p SD",
        resolution = "854x480",
        estimatedSizeMb = 90,
        description = "Standard • Fast download on cellular data",
        badgeText = "DATA SAVER"
    ),
    DownloadQualityOption(
        label = "360p Data Saver",
        resolution = "640x360",
        estimatedSizeMb = 45,
        description = "Compact size • Lowest mobile data consumption",
        badgeText = "ULTRA SAVER"
    )
)

@Composable
fun DownloadQualityDialog(
    movie: Movie,
    onStartDownload: (DownloadQualityOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember {
        mutableStateOf(defaultQualityOptions.first { it.isRecommended })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CineSurface),
            border = BorderStroke(1.dp, CineCardBorder),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .testTag("download_quality_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CineRedPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = CineRedPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Select Download Quality",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineTextPrimary
                            )
                            Text(
                                text = "Choose resolution to optimize data usage",
                                fontSize = 11.sp,
                                color = CineTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_quality_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CineTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Movie Info Snippet
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CineSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = movie.posterUrl,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 40.dp, height = 56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CineCardBorder)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = movie.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CineTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${movie.year} • ${movie.durationFormatted} • ${movie.genres.take(2).joinToString(", ")}",
                                fontSize = 11.sp,
                                color = CineTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Resolution Options List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    defaultQualityOptions.forEach { option ->
                        val isSelected = selectedOption == option

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) CineRedPrimary.copy(alpha = 0.12f) else CineSurfaceElevated,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CineRedPrimary else CineCardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = option }
                                .testTag("quality_option_${option.label.replace(" ", "_").lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedOption = option },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = CineRedPrimary,
                                            unselectedColor = CineTextMuted
                                        )
                                    )

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = option.label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isSelected) CineTextPrimary else CineTextSecondary
                                            )

                                            option.badgeText?.let { badge ->
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = when (badge) {
                                                        "RECOMMENDED" -> CineGreen.copy(alpha = 0.2f)
                                                        "BEST QUALITY" -> CineGold.copy(alpha = 0.2f)
                                                        else -> CineRedPrimary.copy(alpha = 0.2f)
                                                    }
                                                ) {
                                                    Text(
                                                        text = badge,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when (badge) {
                                                            "RECOMMENDED" -> CineGreen
                                                            "BEST QUALITY" -> CineGold
                                                            else -> CineRedPrimary
                                                        },
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = option.description,
                                            fontSize = 11.sp,
                                            color = CineTextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Size Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) CineRedPrimary else CineBlack,
                                    border = BorderStroke(1.dp, if (isSelected) CineRedPrimary else CineCardBorder)
                                ) {
                                    Text(
                                        text = "~${option.estimatedSizeMb} MB",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else CineGold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = { onStartDownload(selectedOption) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_download_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download (${selectedOption.label} • ~${selectedOption.estimatedSizeMb} MB)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
