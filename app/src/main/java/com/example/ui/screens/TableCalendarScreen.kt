package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.engine.SeasonEngine
import com.example.domain.model.Division
import com.example.ui.components.ClubBadge
import com.example.ui.theme.BrazilGold500
import com.example.ui.theme.ZoneLibertadores
import com.example.ui.theme.ZonePreLiberta
import com.example.ui.theme.ZoneRebaixamento
import com.example.ui.theme.ZoneSulAmericana
import com.example.ui.viewmodel.BrasManagerUiState

@Composable
fun TableCalendarScreen(
    uiState: BrasManagerUiState
) {
    val career = uiState.activeCareer ?: return
    val userClub = uiState.userClub ?: return
    var selectedTab by remember { mutableIntStateOf(if (userClub.division == Division.SERIE_A) 0 else 1) }

    val tableA = remember(uiState.clubs) { SeasonEngine.sortStandings(uiState.clubs, Division.SERIE_A) }
    val tableB = remember(uiState.clubs) { SeasonEngine.sortStandings(uiState.clubs, Division.SERIE_B) }

    val topScorers = remember(uiState.clubs) {
        uiState.clubs.flatMap { c -> c.players.map { p -> p to c } }
            .filter { it.first.goalsSeason > 0 }
            .sortedByDescending { it.first.goalsSeason }
            .take(20)
    }

    val currentRound = career.currentRound.toInt().coerceAtMost(38)
    val roundFixtures = remember(uiState.fixtures, currentRound, userClub.division) {
        uiState.fixtures.filter { it.round == currentRound && it.division == userClub.division }
    }
    val clubMap = remember(uiState.clubs) { uiState.clubs.associateBy { it.id } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter)
        ) {
            Text(
                text = "CAMPEONATO BRASILEIRO ${career.seasonYear}",
                style = MaterialTheme.typography.headlineMedium,
                color = BrazilGold500,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Série A") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Série B") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Artilharia") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Rodada $currentRound") })
            }

            when (selectedTab) {
                0, 1 -> {
                    val div = if (selectedTab == 0) Division.SERIE_A else Division.SERIE_B
                    val list = if (selectedTab == 0) tableA else tableB
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(26.dp))
                                Text("CLUBE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                Text("P", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(28.dp))
                                Text("J", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                                Text("V", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                                Text("SG", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(34.dp))
                            }
                        }

                        itemsIndexed(list, key = { _, c -> c.id }) { idx, club ->
                            val pos = idx + 1
                            val zoneColor = when {
                                div == Division.SERIE_A && pos <= 4 -> ZoneLibertadores
                                div == Division.SERIE_A && pos <= 6 -> ZonePreLiberta
                                div == Division.SERIE_A && pos <= 12 -> ZoneSulAmericana
                                div == Division.SERIE_A && pos >= 17 -> ZoneRebaixamento
                                div == Division.SERIE_B && pos <= 4 -> ZoneLibertadores
                                div == Division.SERIE_B && pos >= 17 -> ZoneRebaixamento
                                else -> Color.Transparent
                            }
                            val isUser = club.id == userClub.id
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(22.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(zoneColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$pos",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.width(20.dp)
                                    )
                                    ClubBadge(
                                        shortName = club.shortName,
                                        primaryColorHex = club.primaryColorHex,
                                        secondaryColorHex = club.secondaryColorHex,
                                        crestUrl = club.crestUrl,
                                        size = 24.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = club.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isUser) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(club.points.toString(), style = MaterialTheme.typography.labelMedium, color = BrazilGold500, textAlign = TextAlign.End, modifier = Modifier.width(28.dp))
                                    Text(club.played.toString(), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                                    Text(club.wins.toString(), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                                    Text(
                                        text = if (club.goalDifference > 0) "+${club.goalDifference}" else club.goalDifference.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(34.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(6.dp)) {
                                ZoneLegend(if (div == Division.SERIE_A) "G4 Liberta" else "G4 Acesso", ZoneLibertadores)
                                if (div == Division.SERIE_A) {
                                    ZoneLegend("Pré-Lib", ZonePreLiberta)
                                    ZoneLegend("Sul-Am", ZoneSulAmericana)
                                }
                                ZoneLegend("Z4 Rebaix.", ZoneRebaixamento)
                            }
                        }
                    }
                }
                2 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (topScorers.isEmpty()) {
                            item {
                                Text(
                                    text = "A artilharia será preenchida assim que os gols da temporada começarem!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            itemsIndexed(topScorers) { index, (player, club) ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${index + 1}º", style = MaterialTheme.typography.labelMedium, color = BrazilGold500, modifier = Modifier.width(30.dp))
                                        ClubBadge(club.shortName, club.primaryColorHex, club.secondaryColorHex, club.crestUrl, size = 28.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(player.name, style = MaterialTheme.typography.titleMedium)
                                            Text("${club.name} • ${player.position.code}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Surface(color = BrazilGold500.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                            Text("${player.goalsSeason} gols", style = MaterialTheme.typography.labelMedium, color = BrazilGold500, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(roundFixtures) { _, fixture ->
                            val home = clubMap[fixture.homeClubId]
                            val away = clubMap[fixture.awayClubId]
                            val isUserMatch = home?.id == userClub.id || away?.id == userClub.id
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUserMatch) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        if (home != null) {
                                            ClubBadge(home.shortName, home.primaryColorHex, home.secondaryColorHex, home.crestUrl, 26.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(home.shortName, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                    Text(
                                        text = if (fixture.played) "${fixture.homeScore} x ${fixture.awayScore}" else "vs",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BrazilGold500
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (away != null) {
                                            Text(away.shortName, style = MaterialTheme.typography.titleMedium)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            ClubBadge(away.shortName, away.primaryColorHex, away.secondaryColorHex, away.crestUrl, 26.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
