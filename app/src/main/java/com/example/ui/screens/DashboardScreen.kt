package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stadium
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkOutline
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CupMatchTie
import com.example.domain.model.IncomingTransferOffer
import com.example.domain.model.ManagerJobOffer
import com.example.domain.model.TicketPriceLevel
import com.example.domain.model.TrainingFocus
import com.example.ui.components.ClubBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrazilGold500
import com.example.ui.theme.PitchEmerald950
import com.example.ui.theme.PitchGreen500
import com.example.ui.viewmodel.BrasManagerUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: BrasManagerUiState,
    onPlayRound: () -> Unit,
    onPlayCupMatch: (CupMatchTie) -> Unit = {},
    onStartNextSeason: () -> Unit,
    onTicketPriceChange: (TicketPriceLevel) -> Unit,
    onTrainingChange: (TrainingFocus) -> Unit,
    onExpandStadium: () -> Unit,
    onTakeLoan: () -> Unit,
    onAcceptOffer: (IncomingTransferOffer) -> Unit,
    onRejectOffer: (IncomingTransferOffer) -> Unit,
    onAcceptJobOffer: (ManagerJobOffer) -> Unit = {},
    onTakeEmergencyRehire: () -> Unit = {},
    onOpenFinances: () -> Unit,
    onExitCareer: () -> Unit
) {
    val career = uiState.activeCareer ?: return
    val club = uiState.userClub ?: return
    val totalRounds = club.division.totalRounds
    val nextFixture = uiState.nextUserFixture
    val opponent = nextFixture?.let { f ->
        val oppId = if (f.homeClubId == club.id) f.awayClubId else f.homeClubId
        uiState.clubs.firstOrNull { it.id == oppId }
    }
    val isHome = nextFixture?.homeClubId == club.id

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Stadium Club Header
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_stadium_hero),
                            contentDescription = "Estádio do clube",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            PitchEmerald950.copy(alpha = 0.55f),
                                            PitchEmerald950.copy(alpha = 0.94f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onExitCareer,
                                    modifier = Modifier.testTag("exit_career_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Voltar às carreiras",
                                        tint = Color.White
                                    )
                                }
                                Surface(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        if (uiState.isSyncing) {
                                            Icon(
                                                imageVector = Icons.Default.CloudSync,
                                                contentDescription = null,
                                                tint = PitchGreen500,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = "${club.division.label} • Temp. ${career.seasonYear}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrazilGold500
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ClubBadge(
                                    shortName = club.shortName,
                                    primaryColorHex = club.primaryColorHex,
                                    secondaryColorHex = club.secondaryColorHex,
                                    crestUrl = club.crestUrl,
                                    size = 56.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = club.name.uppercase(),
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${career.leaguePosition}º lugar • ${club.points} pts • Copa: ${career.copaStage}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricPill("CAIXA", formatCurrency(club.budget), BrazilGold500)
                                MetricPill("FORÇA", "${club.averageOverall} OVR", PitchGreen500)
                                MetricPill("RODADA", "${career.currentRound.coerceAtMost(totalRounds.toLong())}/$totalRounds", Color.White)
                            }
                        }
                    }
                }
            }

            // Pending Knockout Cup Match Card
            val pendingCup = uiState.pendingCupTie
            if (pendingCup != null) {
                item {
                    val cupHome = uiState.clubs.firstOrNull { it.id == pendingCup.homeClubId }
                    val cupAway = uiState.clubs.firstOrNull { it.id == pendingCup.awayClubId }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pending_cup_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = BrazilGold500, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${pendingCup.cupType.title.uppercase()} • ${pendingCup.stageLabel.uppercase()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = BrazilGold500,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (cupHome != null && cupAway != null) {
                                Text(
                                    text = "${cupHome.name} x ${cupAway.name} (Jogo único — pênaltis em caso de empate)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onPlayCupMatch(pendingCup) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PitchGreen500,
                                    contentColor = PitchEmerald950
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("play_cup_match_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("JOGAR MATA-MATA DA COPA", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Season Finished Banner OR Next Match CTA
            item {
                if (career.currentRound > totalRounds.toLong()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "FIM DA TEMPORADA ${career.seasonYear}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = BrazilGold500
                            )
                            Text(
                                text = uiState.seasonFinishedMessage ?: "Campeonato encerrado! Inicie a próxima temporada com acesso e rebaixamento aplicados.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            uiState.seasonAwards?.let { awards ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .testTag("season_awards_card")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("PREMIAÇÕES DA TEMPORADA", style = MaterialTheme.typography.labelMedium, color = BrazilGold500)
                                        Text("🏆 Campeão: ${awards.championClubName}", style = MaterialTheme.typography.bodyMedium)
                                        Text("⚽ Artilheiro: ${awards.goldenBootName} (${awards.goldenBootClub}) • ${awards.goldenBootGoals} gols", style = MaterialTheme.typography.bodySmall)
                                        Text("🎯 Rei das Assistências: ${awards.playmakerName} (${awards.playmakerClub}) • ${awards.playmakerAssists} assist.", style = MaterialTheme.typography.bodySmall)
                                        Text("⭐ Craque da Liga: ${awards.bestPlayerName} (${awards.bestPlayerClub})", style = MaterialTheme.typography.bodySmall)
                                        Text("👔 Melhor Técnico: ${awards.managerOfTheYear}", style = MaterialTheme.typography.bodySmall)
                                        Text("💰 Premiação final: +${formatCurrency(awards.prizeMoney)}", style = MaterialTheme.typography.labelMedium, color = PitchGreen500)
                                    }
                                }
                            }
                            Button(
                                onClick = onStartNextSeason,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrazilGold500,
                                    contentColor = PitchEmerald950
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("start_next_season_button")
                            ) {
                                Text("Iniciar Temporada ${career.seasonYear + 1}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "PRÓXIMO CONFRONTO • RODADA ${career.currentRound}",
                                style = MaterialTheme.typography.labelMedium,
                                color = PitchGreen500
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            if (opponent != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        ClubBadge(
                                            shortName = if (isHome) club.shortName else opponent.shortName,
                                            primaryColorHex = if (isHome) club.primaryColorHex else opponent.primaryColorHex,
                                            secondaryColorHex = if (isHome) club.secondaryColorHex else opponent.secondaryColorHex,
                                            crestUrl = if (isHome) club.crestUrl else opponent.crestUrl,
                                            size = 42.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isHome) club.name else opponent.name,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text("Casa", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text(
                                        text = "VS",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = BrazilGold500,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (isHome) opponent.name else club.name,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text("Visitante", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        ClubBadge(
                                            shortName = if (isHome) opponent.shortName else club.shortName,
                                            primaryColorHex = if (isHome) opponent.primaryColorHex else club.primaryColorHex,
                                            secondaryColorHex = if (isHome) opponent.secondaryColorHex else club.secondaryColorHex,
                                            crestUrl = if (isHome) opponent.crestUrl else club.crestUrl,
                                            size = 42.dp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = onPlayRound,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrazilGold500,
                                    contentColor = PitchEmerald950
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("play_round_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("JOGAR PARTIDA AO VIVO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Board Confidence
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Confiança da Diretoria", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${career.boardConfidence}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (career.boardConfidence >= 50) PitchGreen500 else AlertRed
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { career.boardConfidence / 100f },
                            color = if (career.boardConfidence >= 50) PitchGreen500 else AlertRed,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            // Incoming AI Transfer Offers
            if (uiState.incomingOffers.isNotEmpty()) {
                item {
                    Text(
                        text = "PROPOSTAS POR SEUS JOGADORES",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrazilGold500
                    )
                }
                items(uiState.incomingOffers.size) { index ->
                    val offer = uiState.incomingOffers[index]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${offer.offeringClubName} quer ${offer.player.name}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Oferta: ${formatCurrency(offer.offerAmount)} • OVR ${offer.player.overall}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BrazilGold500
                                )
                            }
                            IconButton(onClick = { onRejectOffer(offer) }) {
                                Icon(Icons.Default.Close, contentDescription = "Recusar", tint = AlertRed)
                            }
                            IconButton(onClick = { onAcceptOffer(offer) }) {
                                Icon(Icons.Default.Check, contentDescription = "Aceitar", tint = PitchGreen500)
                            }
                        }
                    }
                }
            }

            // Managerial Job Offers
            if (uiState.jobOffers.isNotEmpty()) {
                item {
                    Text(
                        text = "CONVITES DE OUTROS CLUBES",
                        style = MaterialTheme.typography.labelMedium,
                        color = PitchGreen500
                    )
                }
                items(uiState.jobOffers.size) { index ->
                    val job = uiState.jobOffers[index]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("job_offer_${job.clubId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WorkOutline, contentDescription = null, tint = BrazilGold500, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${job.clubName} (${job.division.shortLabel})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${job.reason} • Caixa ${formatCurrency(job.budget)} • OVR ${job.avgOverall}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { onAcceptJobOffer(job) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrazilGold500, contentColor = PitchEmerald950),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Assumir", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Training Focus
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = PitchGreen500, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Treinamento Semanal", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = uiState.trainingFocus.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TrainingFocus.entries.forEach { focus ->
                                FilterChip(
                                    selected = uiState.trainingFocus == focus,
                                    onClick = { onTrainingChange(focus) },
                                    label = { Text(focus.label) }
                                )
                            }
                        }
                    }
                }
            }

            // Finances & Stadium Management (Brasfoot Classic!)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Stadium, contentDescription = null, tint = BrazilGold500, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Estádio & Finanças", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${club.stadiumName} • Capacidade: ${club.stadiumCapacity} • Folha/rodada: ${formatCurrency(club.weeklyWages / 4)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        uiState.activeSponsor?.let { sponsor ->
                            Text(
                                text = "Patrocínio Máster: ${sponsor.name} (+${formatCurrency(sponsor.perRoundBase)}/rod)",
                                style = MaterialTheme.typography.labelMedium,
                                color = PitchGreen500,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Preço do Ingresso", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TicketPriceLevel.entries.forEach { level ->
                                FilterChip(
                                    selected = uiState.ticketPriceLevel == level,
                                    onClick = { onTicketPriceChange(level) },
                                    label = { Text(level.label) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onExpandStadium,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("expand_stadium_button")
                            ) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+5 mil lugares")
                            }
                            OutlinedButton(
                                onClick = onTakeLoan,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("bank_loan_button")
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Empréstimo 10M")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onOpenFinances,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PitchGreen500,
                                contentColor = PitchEmerald950
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_finances_button")
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver Finanças & Tendência Mensal", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Trophy Room
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.img_trophy_room),
                                contentDescription = "Sala de Troféus",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = BrazilGold500)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sala de Troféus (${uiState.trophies.size})",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (uiState.trophies.isEmpty()) {
                                Text(
                                    text = "Conquiste qualquer campeonato para eternizar sua primeira taça na sala de troféus!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                uiState.trophies.forEach { trophy ->
                                    Text(
                                        text = "🏆 ${trophy.seasonYear} • ${trophy.competitionName} (${trophy.clubName})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BrazilGold500,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (uiState.isSacked) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed) },
            title = { Text("Demissão Confirmada!") },
            text = {
                Text("A confiança da diretoria do ${club.name} chegou ao limite após a sequência recente de resultados. Você pode assumir outro clube para continuar sua trajetória!")
            },
            confirmButton = {
                Button(
                    onClick = onTakeEmergencyRehire,
                    colors = ButtonDefaults.buttonColors(containerColor = BrazilGold500, contentColor = PitchEmerald950),
                    modifier = Modifier.testTag("rehire_other_club_button")
                ) {
                    Text("Assumir Novo Clube", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onExitCareer) {
                    Text("Voltar às Carreiras")
                }
            }
        )
    }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.65f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
