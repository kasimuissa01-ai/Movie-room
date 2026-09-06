package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineSurfaceElevated

/**
 * A minimal, friendly, modern illustration of a cinema chair with a film reel.
 */
@Composable
fun EmptyWatchlistIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Ambient glow circle
        drawCircle(
            color = CineSurfaceElevated.copy(alpha = 0.6f),
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.52f)
        )

        // Cinema Seat Back
        drawRoundRect(
            color = CineRedPrimary,
            topLeft = Offset(w * 0.28f, h * 0.22f),
            size = Size(w * 0.44f, h * 0.42f),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
        )

        // Seat back cushion inner lines
        drawLine(
            color = Color.Black.copy(alpha = 0.25f),
            start = Offset(w * 0.42f, h * 0.25f),
            end = Offset(w * 0.42f, h * 0.58f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.25f),
            start = Offset(w * 0.58f, h * 0.25f),
            end = Offset(w * 0.58f, h * 0.58f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Seat Cushion (horizontal base)
        drawRoundRect(
            color = Color(0xFFB50810),
            topLeft = Offset(w * 0.24f, h * 0.60f),
            size = Size(w * 0.52f, h * 0.16f),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )

        // Armrests
        drawRoundRect(
            color = Color(0xFF2C2C38),
            topLeft = Offset(w * 0.18f, h * 0.48f),
            size = Size(w * 0.08f, h * 0.22f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF2C2C38),
            topLeft = Offset(w * 0.74f, h * 0.48f),
            size = Size(w * 0.08f, h * 0.22f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Chair Pedestal / Legs
        drawLine(
            color = Color(0xFF38384A),
            start = Offset(w * 0.50f, h * 0.76f),
            end = Offset(w * 0.50f, h * 0.90f),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF38384A),
            start = Offset(w * 0.35f, h * 0.90f),
            end = Offset(w * 0.65f, h * 0.90f),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Floating friendly Film Reel beside the seat
        val reelCenter = Offset(w * 0.76f, h * 0.25f)
        val reelRadius = w * 0.14f

        drawCircle(
            color = Color(0xFF2E2E3C),
            radius = reelRadius,
            center = reelCenter
        )
        drawCircle(
            color = Color(0xFF1E1E28),
            radius = reelRadius * 0.72f,
            center = reelCenter
        )
        // 4 Reel holes
        for (i in 0 until 4) {
            val angle = Math.toRadians((i * 90.0) + 45.0)
            val holeX = reelCenter.x + (reelRadius * 0.45f * Math.cos(angle)).toFloat()
            val holeY = reelCenter.y + (reelRadius * 0.45f * Math.sin(angle)).toFloat()
            drawCircle(
                color = CineRedPrimary,
                radius = reelRadius * 0.16f,
                center = Offset(holeX, holeY)
            )
        }
        drawCircle(
            color = Color.White,
            radius = reelRadius * 0.12f,
            center = reelCenter
        )
    }
}

/**
 * A minimal, friendly illustration of a magnifying glass inspecting a film reel for empty search results.
 */
@Composable
fun EmptySearchIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Ambient glow
        drawCircle(
            color = CineSurfaceElevated.copy(alpha = 0.6f),
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Film strip background ribbon
        val ribbonPath = Path().apply {
            moveTo(w * 0.2f, h * 0.65f)
            cubicTo(w * 0.35f, h * 0.45f, w * 0.65f, h * 0.85f, w * 0.85f, h * 0.60f)
        }
        drawPath(
            path = ribbonPath,
            color = Color(0xFF2A2A38),
            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
        )

        // Film Reel base
        val reelCenter = Offset(w * 0.42f, h * 0.44f)
        val reelRadius = w * 0.24f

        drawCircle(
            color = Color(0xFF22222E),
            radius = reelRadius,
            center = reelCenter
        )
        drawCircle(
            color = Color(0xFF161620),
            radius = reelRadius * 0.85f,
            center = reelCenter,
            style = Stroke(width = 3.dp.toPx())
        )

        // Magnifying glass lens
        val lensCenter = Offset(w * 0.55f, h * 0.40f)
        val lensRadius = w * 0.22f

        drawCircle(
            color = Color(0x33E50914),
            radius = lensRadius,
            center = lensCenter
        )
        drawCircle(
            color = CineRedPrimary,
            radius = lensRadius,
            center = lensCenter,
            style = Stroke(width = 5.dp.toPx())
        )

        // Magnifying glass handle
        drawLine(
            color = Color(0xFFEEEEF2),
            start = Offset(lensCenter.x + lensRadius * 0.707f, lensCenter.y + lensRadius * 0.707f),
            end = Offset(lensCenter.x + lensRadius * 1.55f, lensCenter.y + lensRadius * 1.55f),
            strokeWidth = 8.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Sparkle glint on glass
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(lensCenter.x - lensRadius * 0.5f, lensCenter.y - lensRadius * 0.3f),
            end = Offset(lensCenter.x - lensRadius * 0.2f, lensCenter.y - lensRadius * 0.6f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
