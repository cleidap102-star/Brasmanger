package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.theme.BarlowCondensedFamily

@Composable
fun ClubBadge(
    shortName: String,
    primaryColorHex: Long,
    secondaryColorHex: Long,
    crestUrl: String? = null,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    if (!crestUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = crestUrl,
            contentDescription = "Escudo $shortName",
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
            loading = {
                VectorClubShield(shortName, primaryColorHex, secondaryColorHex, size)
            },
            error = {
                VectorClubShield(shortName, primaryColorHex, secondaryColorHex, size)
            }
        )
    } else {
        VectorClubShield(shortName, primaryColorHex, secondaryColorHex, size, modifier)
    }
}

@Composable
fun VectorClubShield(
    shortName: String,
    primaryColorHex: Long,
    secondaryColorHex: Long,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val primary = Color(primaryColorHex)
    val secondary = Color(secondaryColorHex)
    val fontSize = (size.value * 0.28f).sp

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val shieldPath = Path().apply {
                moveTo(w * 0.12f, h * 0.10f)
                lineTo(w * 0.88f, h * 0.10f)
                lineTo(w * 0.88f, h * 0.54f)
                cubicTo(w * 0.88f, h * 0.78f, w * 0.50f, h * 0.94f, w * 0.50f, h * 0.94f)
                cubicTo(w * 0.50f, h * 0.94f, w * 0.12f, h * 0.78f, w * 0.12f, h * 0.54f)
                close()
            }
            drawPath(path = shieldPath, color = primary)
            drawPath(
                path = shieldPath,
                color = secondary.copy(alpha = 0.85f),
                style = Stroke(width = w * 0.06f)
            )
            // Subtle diagonal sash accent
            drawLine(
                color = secondary.copy(alpha = 0.22f),
                start = Offset(w * 0.20f, h * 0.14f),
                end = Offset(w * 0.80f, h * 0.70f),
                strokeWidth = w * 0.12f
            )
        }
        val textColor = if (primary.luminanceApprox() > 0.6f) Color(0xFF111111) else Color.White
        Text(
            text = shortName.take(3),
            color = textColor,
            fontFamily = BarlowCondensedFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private fun Color.luminanceApprox(): Float =
    (0.299f * red + 0.587f * green + 0.114f * blue)
