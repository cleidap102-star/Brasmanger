package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.LiveMatchState
import com.example.domain.model.MatchEventType
import com.example.domain.model.Mentality
import com.example.ui.components.ClubBadge
import com.example.ui.components.MatchSoundEffects
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrazilGold500
import com.example.ui.theme.PitchEmerald800
import com.example.ui.theme.PitchEmerald950
import com.example.ui.theme.PitchGreen500
import com.example.ui.viewmodel.BrasManagerUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveMatchScreen(
    liveMatch: LiveMatchState,
    uiState: BrasManagerUiState,
    onTogglePause: () -> Unit,
    onSetSpeed: (Int) -> Unit,
    onSetMentality: (Mentality) -> Unit,
    onMakeSub: (String, String) -> Unit,
    onSkipToFullTime: () -> Unit,
    onFinishRound: () -> Unit,
    onToggleSound: () -> Unit = {}
) {
    BackHandler(enabled = !liveMatch.isFinished) {
        onTogglePause()
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSubDialog by remember { mutableStateOf(false) }
    val userClub = uiState.userClub

    val latestEvent = liveMatch.events.firstOrNull()
    LaunchedEffect(latestEvent?.minute, latestEvent?.type, latestEvent?.description, uiState.soundEnabled) {
        if (!uiState.soundEnabled || latestEvent == null) return@LaunchedEffect
        when (latestEvent.type) {
            MatchEventType.GOAL -> MatchSoundEffects.playGoal()
            MatchEventType.KICKOFF, MatchEventType.HALFTIME, MatchEventType.FULLTIME -> MatchSoundEffects.playWhistle()
            MatchEventType.YELLOW_CARD, MatchEventType.RED_CARD -> MatchSoundEffects.playCard()
            else -> Unit
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            MatchSoundEffects.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchEmerald950)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 700.dp)
                .align(Alignment.TopCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Broadcast Scoreboard Header
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PitchEmerald800)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(PitchEmerald800, PitchEmerald950)
                            )
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val compTitle = if (liveMatch.isKnockout && liveMatch.cupStageLabel != null) {
                            "${liveMatch.competition.label} • ${liveMatch.cupStageLabel}"
                        } else {
                            "${liveMatch.competition.label} • Rodada ${liveMatch.round}"
                        }
                        Text(
                            text = compTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = BrazilGold500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleSound,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("toggle_match_sound_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = if (uiState.soundEnabled) "Silenciar efeitos" else "Ativar efeitos sonoros",
                                    tint = if (uiState.soundEnabled) BrazilGold500 else Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (liveMatch.isFinished) BrazilGold500 else PitchGreen500.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${liveMatch.minute}' • ${liveMatch.phase}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (liveMatch.isFinished) PitchEmerald950 else PitchGreen500,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            ClubBadge(
                                shortName = liveMatch.userMatch.homeShort,
                                primaryColorHex = liveMatch.userMatch.homeColor,
                                secondaryColorHex = 0xFFFFFFFFL,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = liveMatch.userMatch.homeName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "${liveMatch.userMatch.homeScore}  :  ${liveMatch.userMatch.awayScore}",
                            style = MaterialTheme.typography.displayMedium,
                            color = BrazilGold500,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            ClubBadge(
                                shortName = liveMatch.userMatch.awayShort,
                                primaryColorHex = liveMatch.userMatch.awayColor,
                                secondaryColorHex = 0xFFFFFFFFL,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = liveMatch.userMatch.awayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { liveMatch.minute / 90f },
                        color = BrazilGold500,
                        trackColor = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LivePitchMomentumRadar(
                        ballZone = liveMatch.ballZone,
                        homeColor = Color(liveMatch.userMatch.homeColor),
                        awayColor = Color(liveMatch.userMatch.awayColor),
                        homeShort = liveMatch.userMatch.homeShort,
                        awayShort = liveMatch.userMatch.awayShort
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Posse ${liveMatch.possessionHome}%-${100 - liveMatch.possessionHome}% • Chutes ${liveMatch.shotsHome}-${liveMatch.shotsAway} • Público ${liveMatch.attendance} • Renda ${formatCurrency(liveMatch.ticketRevenue)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }

            // Live Match Controls
            if (!liveMatch.isFinished) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onTogglePause,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (liveMatch.isPaused) PitchGreen500 else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (liveMatch.isPaused) PitchEmerald950 else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("match_pause_button")
                    ) {
                        Icon(if (liveMatch.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (liveMatch.isPaused) "Continuar" else "Pausar")
                    }

                    listOf(1, 2, 4).forEach { spd ->
                        FilterChip(
                            selected = liveMatch.speed == spd,
                            onClick = { onSetSpeed(spd) },
                            label = { Text("${spd}x") }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    OutlinedButton(
                        onClick = { showSubDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("match_sub_button")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${liveMatch.substitutionsUsed}/5")
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Mentality.entries.forEach { m ->
                        FilterChip(
                            selected = uiState.mentality == m,
                            onClick = { onSetMentality(m) },
                            label = { Text(m.label) }
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = onSkipToFullTime,
                        leadingIcon = { Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Fim") },
                        modifier = Modifier.testTag("skip_fulltime_button")
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        liveMatch.penaltyShootout?.let { shootout ->
                            Surface(
                                color = BrazilGold500.copy(alpha = 0.20f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .testTag("penalty_shootout_banner")
                            ) {
                                Text(
                                    text = "⚽ ${shootout.summary}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BrazilGold500,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        Text(
                            text = "Craque do Jogo: ${liveMatch.manOfTheMatch ?: "Equipe"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrazilGold500
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onFinishRound,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrazilGold500,
                                contentColor = PitchEmerald950
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("finish_round_button")
                        ) {
                            Text(
                                text = if (liveMatch.isKnockout) "Confirmar Classificação & Prêmio" else "Avançar Rodada & Atualizar Tabela",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Tabs: Play-by-Play vs Brasfoot Simultaneous Other Matches!
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Lance a Lance") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Outros Jogos (${liveMatch.otherMatches.size})") })
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(liveMatch.events) { event ->
                        val bg = when (event.type) {
                            MatchEventType.GOAL -> PitchGreen500.copy(alpha = 0.16f)
                            MatchEventType.RED_CARD -> AlertRed.copy(alpha = 0.18f)
                            MatchEventType.YELLOW_CARD -> BrazilGold500.copy(alpha = 0.14f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Surface(color = bg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${event.minute}'",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrazilGold500,
                                    modifier = Modifier.width(34.dp)
                                )
                                if (event.type == MatchEventType.GOAL) {
                                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = PitchGreen500, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(liveMatch.otherMatches, key = { it.fixtureId }) { other ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(other.homeName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = "${other.homeScore} x ${other.awayScore}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BrazilGold500,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Text(other.awayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                other.lastGoalText?.let { goal ->
                                    Text("⚽ $goal", style = MaterialTheme.typography.labelSmall, color = PitchGreen500, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubDialog && userClub != null) {
        val starters = userClub.players.filter { it.isStarter }
        val bench = userClub.players.filter { !it.isStarter && it.isAvailable }
        var outId by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSubDialog = false },
            title = { Text(if (outId == null) "Quem vai SAIR?" else "Quem vai ENTRAR?") },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (outId == null) {
                        items(starters, key = { it.id }) { p ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().clickable { outId = p.id }
                            ) {
                                Text(
                                    text = "${p.position.code} • ${p.name} (${p.energy}%)",
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    } else {
                        items(bench, key = { it.id }) { p ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onMakeSub(outId!!, p.id)
                                    showSubDialog = false
                                }
                            ) {
                                Text(
                                    text = "${p.position.code} • ${p.name} (OVR ${p.overall})",
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubDialog = false }) { Text("Fechar") }
            }
        )
    }
}

@Composable
private fun LivePitchMomentumRadar(
    ballZone: Float,
    homeColor: Color,
    awayColor: Color,
    homeShort: String,
    awayShort: String
) {
    val animatedBall by animateFloatAsState(
        targetValue = ballZone.coerceIn(0.05f, 0.95f),
        animationSpec = tween(durationMillis = 280),
        label = "ballZoneRadar"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0C3B24))
                .testTag("live_pitch_radar")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val lineColor = Color.White.copy(alpha = 0.28f)
                val stroke = 1.4.dp.toPx()

                // Pitch border & halfway line
                drawRect(color = lineColor, style = Stroke(width = stroke))
                drawLine(color = lineColor, start = Offset(w / 2f, 0f), end = Offset(w / 2f, h), strokeWidth = stroke)
                drawCircle(color = lineColor, radius = h * 0.34f, center = Offset(w / 2f, h / 2f), style = Stroke(width = stroke))

                // Penalty boxes
                val boxW = w * 0.13f
                val boxH = h * 0.64f
                val boxTop = (h - boxH) / 2f
                drawRect(color = lineColor, topLeft = Offset(0f, boxTop), size = Size(boxW, boxH), style = Stroke(width = stroke))
                drawRect(color = lineColor, topLeft = Offset(w - boxW, boxTop), size = Size(boxW, boxH), style = Stroke(width = stroke))

                // Ball position glow + sphere
                val bx = w * animatedBall
                val by = h * 0.5f
                val zoneColor = if (animatedBall >= 0.5f) homeColor else awayColor
                drawCircle(color = zoneColor.copy(alpha = 0.42f), radius = 14.dp.toPx(), center = Offset(bx, by))
                drawCircle(color = BrazilGold500, radius = 6.dp.toPx(), center = Offset(bx, by))
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(bx, by))
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(homeShort, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
                Text(awayShort, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
            }
        }
    }
}

