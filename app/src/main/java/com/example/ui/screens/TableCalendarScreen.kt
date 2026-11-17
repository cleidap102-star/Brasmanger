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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import com.example.domain.engine.ChampionsLeagueEngine
import com.example.domain.engine.ContinentalGroupEngine
import com.example.domain.engine.SeasonEngine
import com.example.domain.model.AnnualAwardsRecord
import com.example.domain.model.ChampionsLeaguePhase
import com.example.domain.model.ChampionsLeagueState
import com.example.domain.model.ContinentalPhase
import com.example.domain.model.ContinentalTournamentState
import com.example.domain.model.CupCompetitionState
import com.example.domain.model.CupMatchTie
import com.example.domain.model.Division
import com.example.ui.components.ClubBadge
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrazilGold500
import com.example.ui.theme.InfoSky
import com.example.ui.theme.PitchEmerald950
import com.example.ui.theme.PitchGreen500
import com.example.ui.theme.ZoneLibertadores
import com.example.ui.theme.ZoneRebaixamento
import com.example.ui.theme.ZoneSulAmericana
import com.example.ui.viewmodel.BrasManagerUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TableCalendarScreen(
    uiState: BrasManagerUiState,
    onVoteBallonDor: (String, String) -> Unit = { _, _ -> }
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

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Tabela") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Destaques") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Rodada $currentRound") })
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        val totalCups = uiState.cups.size + uiState.continentalTournaments.size + if (uiState.championsLeague != null) 1 else 0
                        Text("Copas ($totalCups)")
                    },
                    modifier = Modifier.testTag("tab_cups")
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Bola de Ouro") },
                    modifier = Modifier.testTag("tab_ballon_dor")
                )
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
                3 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        uiState.championsLeague?.let { ucl ->
                            item(key = "ucl_league_phase") {
                                ChampionsLeagueSectionCard(
                                    ucl = ucl,
                                    clubMap = clubMap,
                                    userClubId = userClub.id
                                )
                            }
                        }

                        if (uiState.continentalTournaments.isNotEmpty()) {
                            items(uiState.continentalTournaments, key = { "cont_${it.cupType.name}" }) { cont ->
                                ContinentalTournamentCard(
                                    tournament = cont,
                                    clubMap = clubMap,
                                    userClubId = userClub.id
                                )
                            }
                        }

                        if (uiState.cups.isNotEmpty()) {
                            items(uiState.cups, key = { it.cupType.name }) { cup ->
                                CupBracketCard(
                                    cup = cup,
                                    clubMap = clubMap,
                                    userClubId = userClub.id
                                )
                            }
                        }

                        if (uiState.championsLeague == null && uiState.continentalTournaments.isEmpty() && uiState.cups.isEmpty()) {
                            item {
                                Text(
                                    text = "Nenhuma copa em disputa no momento.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                else -> {
                    val hasVotedThisSeason = uiState.userBallonDorVoteSeason == career.seasonYear && !uiState.userBallonDorPlayerId.isNullOrBlank()
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ballon_dor_election_card")
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = BrazilGold500, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "ELEIÇÃO BOLA DE OURO ${career.seasonYear}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = BrazilGold500,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (hasVotedThisSeason) {
                                            Surface(color = PitchGreen500.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                                Text(
                                                    text = "Voto Computado ✓",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PitchGreen500,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Corrida ao prêmio de Melhor do Mundo. Vote no seu favorito para impulsionar (+22 pts) a eleição oficial do fim da temporada.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (uiState.ballonDorContenders.isEmpty()) {
                            item {
                                Text(
                                    text = "A lista de finalistas será preenchida assim que as primeiras rodadas forem disputadas.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            itemsIndexed(uiState.ballonDorContenders, key = { _, c -> c.playerId }) { index, cand ->
                                val club = clubMap[cand.clubId]
                                val isVotedByUser = hasVotedThisSeason && uiState.userBallonDorPlayerId == cand.playerId
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isVotedByUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}º",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (index < 3) BrazilGold500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(30.dp)
                                        )
                                        if (club != null) {
                                            ClubBadge(club.shortName, club.primaryColorHex, club.secondaryColorHex, club.crestUrl, size = 28.dp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${cand.playerName} (${cand.position.code})",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${cand.clubShortName} (${cand.divisionLabel}) • OVR ${cand.overall} • ${cand.goals}G ${cand.assists}A",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${cand.score} pts na corrida",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = InfoSky
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        if (isVotedByUser) {
                                            Button(
                                                onClick = { onVoteBallonDor(cand.playerId, cand.playerName) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = PitchGreen500,
                                                    contentColor = PitchEmerald950
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("vote_ballon_${cand.playerId}")
                                            ) {
                                                Icon(Icons.Default.HowToVote, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Votado ✓", fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = { onVoteBallonDor(cand.playerId, cand.playerName) },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("vote_ballon_${cand.playerId}")
                                            ) {
                                                Text("Votar")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "HALL DA FAMA • PREMIADOS DE FIM DE ANO",
                                style = MaterialTheme.typography.labelMedium,
                                color = BrazilGold500,
                                modifier = Modifier.testTag("annual_awards_history_section")
                            )
                        }

                        if (uiState.annualAwardsHistory.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Conclua uma temporada para eternizar aqui o vencedor da Bola de Ouro, Artilheiro, Garçom, Luva de Ouro, Revelação Sub-21, Técnico do Ano e a Seleção da Época!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        } else {
                            items(uiState.annualAwardsHistory, key = { "awards_${it.seasonYear}" }) { record ->
                                AnnualAwardsHistoryCard(record = record)
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
private fun ChampionsLeagueSectionCard(
    ucl: ChampionsLeagueState,
    clubMap: Map<String, com.example.domain.model.Club>,
    userClubId: String
) {
    var showMode by remember(ucl.phase) {
        mutableIntStateOf(if (ucl.phase == ChampionsLeaguePhase.LEAGUE_PHASE || ucl.knockoutTies.isEmpty()) 0 else 1)
    }
    val sorted = remember(ucl.standings, clubMap) {
        ChampionsLeagueEngine.sortStandings(ucl.standings, clubMap)
    }
    val champion = ucl.championClubId?.let { clubMap[it] }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ucl_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = InfoSky, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("UEFA Champions League", style = MaterialTheme.typography.titleLarge, color = BrazilGold500)
                }
                Surface(color = InfoSky.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = if (champion != null) "Campeão: ${champion.shortName}" else ucl.phase.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = InfoSky,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = showMode == 0,
                    onClick = { showMode = 0 },
                    label = { Text("Fase de Liga (${sorted.size}) • J${ucl.currentMatchday}/8") },
                    modifier = Modifier.testTag("ucl_league_tab_chip")
                )
                if (ucl.knockoutTies.isNotEmpty()) {
                    FilterChip(
                        selected = showMode == 1,
                        onClick = { showMode = 1 },
                        label = { Text("Mata-Mata") },
                        modifier = Modifier.testTag("ucl_knockout_tab_chip")
                    )
                }
            }

            if (showMode == 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(26.dp))
                    Text("Clube (Top 8 Oitavas • 9-24 Playoffs)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    Text("P", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                    Text("J", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(24.dp))
                    Text("SG", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(30.dp))
                }
                sorted.forEachIndexed { idx, st ->
                    val pos = idx + 1
                    val club = clubMap[st.clubId]
                    val isUser = st.clubId == userClubId
                    val zoneColor = when {
                        pos <= 8 -> PitchGreen500
                        pos <= 24 -> InfoSky
                        else -> AlertRed
                    }
                    Surface(
                        color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(zoneColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$pos", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(22.dp))
                            if (club != null) {
                                ClubBadge(club.shortName, club.primaryColorHex, club.secondaryColorHex, club.crestUrl, size = 20.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = club?.name ?: st.clubId,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isUser) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${st.points}", style = MaterialTheme.typography.labelMedium, color = BrazilGold500, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                            Text("${st.played}", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(24.dp))
                            Text(
                                text = if (st.goalDifference > 0) "+${st.goalDifference}" else "${st.goalDifference}",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(30.dp)
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ZoneLegend("1º–8º Direto", PitchGreen500)
                    ZoneLegend("9º–24º Playoffs", InfoSky)
                    ZoneLegend("25º–36º Eliminado", AlertRed)
                }
            } else {
                val stageTies = if (ucl.phase == ChampionsLeaguePhase.FINISHED) {
                    val maxS = ucl.knockoutTies.maxOfOrNull { it.stageIndex } ?: 0
                    ucl.knockoutTies.filter { it.stageIndex == maxS }
                } else {
                    ucl.knockoutTies.filter { it.stageIndex == ucl.currentKnockoutStageIndex }
                }
                stageTies.forEach { tie ->
                    CupTieRowCard(tie = tie, clubMap = clubMap, userClubId = userClubId)
                }
            }
        }
    }
}

@Composable
private fun ContinentalTournamentCard(
    tournament: ContinentalTournamentState,
    clubMap: Map<String, com.example.domain.model.Club>,
    userClubId: String
) {
    val groupNames = remember(tournament.standings) {
        tournament.standings.map { it.groupName }.distinct().sorted()
    }
    val userGroup = remember(tournament.standings, userClubId) {
        tournament.standings.firstOrNull { it.clubId == userClubId }?.groupName
    }
    var selectedGroup by remember(tournament.cupType, userGroup) {
        mutableStateOf(userGroup ?: groupNames.firstOrNull() ?: "A")
    }
    var showKnockout by remember(tournament.phase) {
        mutableStateOf(tournament.phase != ContinentalPhase.GROUP_STAGE && tournament.knockoutTies.isNotEmpty())
    }
    val champion = tournament.championClubId?.let { clubMap[it] }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("continental_card_${tournament.cupType.name.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tournament.cupType.title, style = MaterialTheme.typography.titleLarge, color = BrazilGold500, modifier = Modifier.weight(1f))
                Surface(color = PitchGreen500.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = if (champion != null) "Campeão: ${champion.shortName}" else tournament.phase.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = PitchGreen500,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (tournament.knockoutTies.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showKnockout,
                        onClick = { showKnockout = false },
                        label = { Text("Grupos A–H • J${tournament.currentMatchday}/6") }
                    )
                    FilterChip(
                        selected = showKnockout,
                        onClick = { showKnockout = true },
                        label = { Text("Mata-Mata") }
                    )
                }
            }

            if (!showKnockout) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(groupNames) { gName ->
                        val isUserGrp = gName == userGroup
                        FilterChip(
                            selected = selectedGroup == gName,
                            onClick = { selectedGroup = gName },
                            label = { Text(if (isUserGrp) "Grupo $gName ★" else "Grupo $gName") }
                        )
                    }
                }
                val groupStandings = remember(tournament.standings, selectedGroup, clubMap) {
                    ContinentalGroupEngine.sortGroupStandings(tournament.standings, selectedGroup, clubMap)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(24.dp))
                    Text("Grupo $selectedGroup (Top 2 Oitavas)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    Text("P", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                    Text("J", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(22.dp))
                    Text("SG", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(28.dp))
                }
                groupStandings.forEachIndexed { idx, st ->
                    val pos = idx + 1
                    val club = clubMap[st.clubId]
                    val isUser = st.clubId == userClubId
                    Surface(
                        color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (pos <= 2) ZoneLibertadores else Color.Transparent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$pos", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(20.dp))
                            if (club != null) {
                                ClubBadge(club.shortName, club.primaryColorHex, club.secondaryColorHex, club.crestUrl, size = 20.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = club?.name ?: st.clubId,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isUser) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${st.points}", style = MaterialTheme.typography.labelMedium, color = BrazilGold500, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                            Text("${st.played}", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(22.dp))
                            Text(
                                text = if (st.goalDifference > 0) "+${st.goalDifference}" else "${st.goalDifference}",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(28.dp)
                            )
                        }
                    }
                }
            } else {
                val displayTies = if (tournament.phase == ContinentalPhase.FINISHED) {
                    val maxS = tournament.knockoutTies.maxOfOrNull { it.stageIndex } ?: 0
                    tournament.knockoutTies.filter { it.stageIndex == maxS }
                } else {
                    tournament.knockoutTies.filter { it.stageIndex == tournament.currentKnockoutStageIndex }
                }
                displayTies.forEach { tie ->
                    CupTieRowCard(tie = tie, clubMap = clubMap, userClubId = userClubId)
                }
            }
        }
    }
}

@Composable
private fun CupTieRowCard(
    tie: CupMatchTie,
    clubMap: Map<String, com.example.domain.model.Club>,
    userClubId: String
) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnnualAwardsHistoryCard(record: AnnualAwardsRecord) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "TEMPORADA ${record.seasonYear} • PREMIADOS",
                style = MaterialTheme.typography.titleMedium,
                color = BrazilGold500,
                fontWeight = FontWeight.Bold
            )
            record.ballonDorTop3.forEachIndexed { idx, cand ->
                val medal = when (idx) {
                    0 -> "🥇 Bola de Ouro"
                    1 -> "🥈 2º Lugar"
                    else -> "🥉 3º Lugar"
                }
                Text(
                    text = "$medal: ${cand.playerName} (${cand.clubShortName}) • ${cand.goals}G ${cand.assists}A • ${cand.score} pts",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (idx == 0) BrazilGold500 else MaterialTheme.colorScheme.onSurface
                )
            }
            record.goldenBoot?.let {
                Text("⚽ Artilheiro: ${it.playerName} (${it.clubShortName}) — ${it.goals} gols", style = MaterialTheme.typography.bodySmall)
            }
            record.topPlaymaker?.let {
                Text("🎯 Rei das Assistências: ${it.playerName} (${it.clubShortName}) — ${it.assists} assist.", style = MaterialTheme.typography.bodySmall)
            }
            record.bestGoalkeeper?.let {
                Text("🧤 Luva de Ouro: ${it.playerName} (${it.clubShortName}) — OVR ${it.overall}", style = MaterialTheme.typography.bodySmall)
            }
            record.bestYoungPlayer?.let {
                Text("🌟 Revelação Sub-21: ${it.playerName} (${it.clubShortName}, ${it.age} anos)", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "👔 Melhor Técnico: ${record.bestManagerName} (${record.bestManagerClubName})",
                style = MaterialTheme.typography.bodySmall
            )
            if (record.teamOfTheYear.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("SELEÇÃO DA TEMPORADA", style = MaterialTheme.typography.labelSmall, color = PitchGreen500)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    record.teamOfTheYear.forEach { star ->
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "${star.position.code} ${star.playerName} (${star.clubShortName})",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
