package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Formation
import com.example.domain.model.Player
import com.example.domain.model.PlayerPosition
import com.example.ui.theme.BrazilGold500
import com.example.ui.theme.PitchEmerald800
import com.example.ui.theme.PitchEmerald900
import com.example.ui.theme.PitchGreen500

@Composable
fun TacticalPitchView(
    starters: List<Player>,
    formation: Formation,
    clubColorHex: Long,
    onPlayerClick: (Player) -> Unit,
    modifier: Modifier = Modifier
) {
    val gks = starters.filter { it.position == PlayerPosition.GOL }.take(1)
    val defs = starters.filter { it.position == PlayerPosition.ZAG || it.position == PlayerPosition.LAT }
    val mids = starters.filter { it.position == PlayerPosition.MEI }
    val atts = starters.filter { it.position == PlayerPosition.ATA }

    val remaining = starters.filter { p ->
        p !in gks && p !in defs && p !in mids && p !in atts
    }
    val allMids = mids + remaining

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(PitchEmerald800, PitchEmerald900, PitchEmerald800)
                )
            )
    ) {
        // Draw crisp tactical field markings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val lineColor = Color.White.copy(alpha = 0.24f)
            val stroke = Stroke(width = 2.dp.toPx())

            // Mowed grass stripes
            val stripeCount = 8
            val stripeH = h / stripeCount
            for (i in 0 until stripeCount step 2) {
                drawRect(
                    color = Color.White.copy(alpha = 0.03f),
                    topLeft = Offset(0f, i * stripeH),
                    size = Size(w, stripeH)
                )
            }

            // Outer pitch boundary
            val pad = 14.dp.toPx()
            drawRect(
                color = lineColor,
                topLeft = Offset(pad, pad),
                size = Size(w - pad * 2, h - pad * 2),
                style = stroke
            )

            // Halfway line & center circle
            drawLine(
                color = lineColor,
                start = Offset(pad, h / 2f),
                end = Offset(w - pad, h / 2f),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(
                color = lineColor,
                radius = w * 0.14f,
                center = Offset(w / 2f, h / 2f),
                style = stroke
            )

            // Top & Bottom penalty areas
            val boxW = w * 0.48f
            val boxH = h * 0.14f
            drawRect(
                color = lineColor,
                topLeft = Offset((w - boxW) / 2f, pad),
                size = Size(boxW, boxH),
                style = stroke
            )
            drawRect(
                color = lineColor,
                topLeft = Offset((w - boxW) / 2f, h - pad - boxH),
                size = Size(boxW, boxH),
                style = stroke
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 18.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Attackers (Top)
            PitchLineRow(players = atts, clubColorHex = clubColorHex, onPlayerClick = onPlayerClick)
            // Midfielders
            PitchLineRow(players = allMids, clubColorHex = clubColorHex, onPlayerClick = onPlayerClick)
            // Defenders
            PitchLineRow(players = defs, clubColorHex = clubColorHex, onPlayerClick = onPlayerClick)
            // Goalkeeper (Bottom)
            PitchLineRow(players = gks, clubColorHex = 0xFFF5B800L, onPlayerClick = onPlayerClick)
        }

        Surface(
            color = Color.Black.copy(alpha = 0.45f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Text(
                text = formation.code,
                style = MaterialTheme.typography.labelMedium,
                color = BrazilGold500,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PitchLineRow(
    players: List<Player>,
    clubColorHex: Long,
    onPlayerClick: (Player) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        players.forEach { player ->
            PitchPlayerToken(
                player = player,
                clubColor = Color(clubColorHex),
                onClick = { onPlayerClick(player) }
            )
        }
    }
}

@Composable
private fun PitchPlayerToken(
    player: Player,
    clubColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = clubColor,
                shadowElevation = 4.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = player.overall.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (clubColor == BrazilGold500) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Energy ring dot
            val energyColor = when {
                player.energy >= 75 -> PitchGreen500
                player.energy >= 55 -> BrazilGold500
                else -> Color(0xFFEF4444)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(energyColor)
            )
        }
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = player.name.split(" ").lastOrNull()?.take(10) ?: player.name.take(10),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}
