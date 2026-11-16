package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.engine.SeasonEngine
import com.example.domain.model.CupCompetitionState
import com.example.domain.model.Division
import com.example.ui.components.ClubBadge
import com.example.ui.theme.BrazilGold500
import com.example.ui.theme.InfoSky
import com.example.ui.theme.PitchGreen500
import com.example.ui.theme.ZoneLibertadores
import com.example.ui.theme.ZoneRebaixamento
import com.example.ui.theme.ZoneSulAmericana
import com.example.ui.viewmodel.BrasManagerUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TableCalendarScreen(
    uiState: BrasManagerUiState
) {
    val career = uiState.activeCareer ?: return
    val userClub = uiState.userClub ?: return
    var selectedTab by remember { mutableIntStateOf(0) }
    var leaderboardMode by remember { mutableIntStateOf(0) }
    var selectedDivision by remember(userClub.division) { mutableStateOf(userClub.division) }

    val standings = remember(uiState.clubs, selectedDivision) {
        SeasonEngine.sortStandings(uiState.clubs, selectedDivision)
    }

    val topScorers = remember(uiState.clubs) {
        uiState.clubs.flatMap { c -> c.players.map { p -> p to c } }
            .filter { it.first.goalsSeason > 0 }
            .sortedByDescending { it.first.goalsSeason }
            .take(25)
    }
    val topPlaymakers = remember(uiState.clubs) {
        uiState.clubs.flatMap { c -> c.players.map { p -> p to c } }
            .filter { it.first.assistsSeason > 0 }
            .sortedByDescending { it.first.assistsSeason }
            .take(25)
    }

    val currentRound = career.currentRound.toInt().coerceAtMost(userClub.division.totalRounds)
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
                text = "LIGAS & CLASSIFICAÇÃO ${career.seasonYear}",
                style = MaterialTheme.typography.headlineMedium,
                color = BrazilGold500,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Tabela") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Destaques") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Rodada $currentRound") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Copas (${uiState.cups.size})") })
            }

            when (selectedTab) {
                0 -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(Division.entries) { div ->
                            FilterChip(
                                selected = selectedDivision == div,
                                onClick = { selectedDivision = div },
                                label = { Text(div.shortLabel) }
                            )
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
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
                                Text("${selectedDivision.label} (${selectedDivision.country})", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                Text("P", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(28.dp))
                                Text("J", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                                Text("V", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                                Text("SG", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(34.dp))
                            }
                        }

                        itemsIndexed(standings, key = { _, c -> c.id }) { idx, club ->
                            val pos = idx + 1
                            val relCutoff = standings.size - selectedDivision.relegationSpots + 1
                            val zoneColor = when {
                                selectedDivision.promotionSpots > 0 && pos <= selectedDivision.promotionSpots -> ZoneLibertadores
                                selectedDivision.championsSpots > 0 && pos <= selectedDivision.championsSpots -> ZoneLibertadores
                                selectedDivision.secondaryContSpots > 0 && pos <= selectedDivision.secondaryContSpots -> ZoneSulAmericana
                                selectedDivision.relegationSpots > 0 && pos >= relCutoff -> ZoneRebaixamento
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
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(6.dp)
                            ) {
                                val isAmericas = selectedDivision.country in listOf("Brasil", "Argentina")
                                if (selectedDivision.promotionSpots > 0) {
                                    ZoneLegend("G${selectedDivision.promotionSpots} Acesso", ZoneLibertadores)
                                } else if (selectedDivision.championsSpots > 0) {
                                    ZoneLegend(
                                        if (isAmericas) "G${selectedDivision.championsSpots} Liberta" else "G${selectedDivision.championsSpots} Champions",
                                        ZoneLibertadores
                                    )
                                }
                                if (selectedDivision.secondaryContSpots > 0) {
                                    ZoneLegend(if (isAmericas) "Sul-Americana" else "Liga Europa", ZoneSulAmericana)
                                }
                                if (selectedDivision.relegationSpots > 0) {
                                    ZoneLegend("Z${selectedDivision.relegationSpots} Rebaix.", ZoneRebaixamento)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                1 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = leaderboardMode == 0,
                                onClick = { leaderboardMode = 0 },
                                label = { Text("Artilheiros (Gols)") },
                                modifier = Modifier.testTag("filter_goals_leaders")
                            )
                            FilterChip(
                                selected = leaderboardMode == 1,
                                onClick = { leaderboardMode = 1 },
                                label = { Text("Garçons (Assist.)") },
                                modifier = Modifier.testTag("filter_assist_leaders")
                            )
                        }
                        val activeList = if (leaderboardMode == 0) topScorers else topPlaymakers
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeList.isEmpty()) {
                                item {
                                    Text(
                                        text = if (leaderboardMode == 0) {
                                            "A artilharia será preenchida assim que os gols da temporada começarem!"
                                        } else {
                                            "Os líderes de assistências aparecerão assim que as rodadas avançarem!"
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                itemsIndexed(activeList) { index, (player, club) ->
                                    val statValue = if (leaderboardMode == 0) "${player.goalsSeason} gols" else "${player.assistsSeason} assist."
                                    val badgeColor = if (leaderboardMode == 0) BrazilGold500 else InfoSky
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${index + 1}º", style = MaterialTheme.typography.labelMedium, color = badgeColor, modifier = Modifier.width(30.dp))
                                            ClubBadge(club.shortName, club.primaryColorHex, club.secondaryColorHex, club.crestUrl, size = 28.dp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(player.name, style = MaterialTheme.typography.titleMedium)
                                                Text("${club.name} (${club.division.shortLabel}) • ${player.position.code}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Surface(color = badgeColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                                Text(statValue, style = MaterialTheme.typography.labelMedium, color = badgeColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
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
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.cups.isEmpty()) {
                            item {
                                Text(
                                    text = "Nenhuma copa em disputa no momento.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(uiState.cups, key = { it.cupType.name }) { cup ->
                                CupBracketCard(
                                    cup = cup,
                                    clubMap = clubMap,
                                    userClubId = userClub.id
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CupBracketCard(
    cup: CupCompetitionState,
    clubMap: Map<String, com.example.domain.model.Club>,
    userClubId: String
) {
    val currentStageTies = cup.ties.filter { it.stageIndex == cup.currentStageIndex }
    val champion = cup.championClubId?.let { clubMap[it] }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cup_bracket_${cup.cupType.name.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(cup.cupType.title, style = MaterialTheme.typography.titleLarge, color = BrazilGold500)
                Surface(color = PitchGreen500.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = if (champion != null) "Campeão: ${champion.shortName}" else cup.cupType.stageNames.getOrElse(cup.currentStageIndex) { "Mata-Mata" },
                        style = MaterialTheme.typography.labelMedium,
                        color = PitchGreen500,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            val displayTies = if (champion != null) {
                cup.ties.filter { it.stageIndex == cup.ties.maxOf { t -> t.stageIndex } }
            } else {
                currentStageTies
            }
            displayTies.forEach { tie ->
                val h = clubMap[tie.homeClubId]
                val a = clubMap[tie.awayClubId]
                val isUser = tie.homeClubId == userClubId || tie.awayClubId == userClubId
                Surface(
                    color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(h?.shortName ?: tie.homeClubId, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(
                                text = if (tie.homeScore != null && tie.awayScore != null) "${tie.homeScore} x ${tie.awayScore}" else "Rod. ${tie.triggerAfterLeagueRound}+",
                                style = MaterialTheme.typography.labelLarge,
                                color = BrazilGold500
                            )
                            Text(
                                text = a?.shortName ?: tie.awayClubId,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        tie.shootout?.let { pen ->
                            Text(
                                text = pen.summary,
                                style = MaterialTheme.typography.labelSmall,
                                color = PitchGreen500,
                                modifier = Modifier.padding(top = 2.dp)
                            )
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
