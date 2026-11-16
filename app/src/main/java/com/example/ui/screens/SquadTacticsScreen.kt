package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.engine.CupAndCareerEngine
import com.example.domain.model.Formation
import com.example.domain.model.MarkingStyle
import com.example.domain.model.Mentality
import com.example.domain.model.Player
import com.example.domain.model.YouthProspect
import com.example.ui.components.TacticalPitchView
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrazilGold500
import com.example.ui.theme.InfoSky
import com.example.ui.theme.PitchEmerald950
import com.example.ui.theme.PitchGreen500
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.BrasManagerUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SquadTacticsScreen(
    uiState: BrasManagerUiState,
    onToggleStarter: (String) -> Unit,
    onAutoBestLineup: () -> Unit,
    onUpdateTactics: (Formation, Mentality, MarkingStyle) -> Unit,
    onToggleForSale: (String) -> Unit,
    onPromoteYouth: (String) -> Unit = {},
    onRenewContract: (String) -> Unit = {}
) {
    val club = uiState.userClub ?: return
    var subTab by remember { mutableIntStateOf(0) }

    val starters = remember(club.players) {
        club.players.filter { it.isStarter }.sortedBy { it.position.sectorOrder }
    }
    val reserves = remember(club.players) {
        club.players.filterNot { it.isStarter }.sortedByDescending { it.overall }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ELENCO & TÁTICA",
                        style = MaterialTheme.typography.headlineMedium,
                        color = BrazilGold500
                    )
                    Text(
                        text = "${starters.size}/11 titulares • Força média ${club.averageOverall}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAutoBestLineup,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PitchGreen500,
                        contentColor = PitchEmerald950
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("auto_lineup_button")
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Melhor XI", fontWeight = FontWeight.Bold)
                }
            }

            TabRow(selectedTabIndex = subTab) {
                Tab(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    text = { Text("Campo") }
                )
                Tab(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    text = { Text("Elenco (${club.players.size})") }
                )
                Tab(
                    selected = subTab == 2,
                    onClick = { subTab = 2 },
                    text = { Text("Base (${uiState.youthProspects.size})") }
                )
            }

            if (subTab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        TacticalPitchView(
                            starters = starters,
                            formation = uiState.formation,
                            clubColorHex = club.primaryColorHex,
                            onPlayerClick = { onToggleStarter(it.id) }
                        )
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Formação Tática", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Formation.entries.forEach { f ->
                                        FilterChip(
                                            selected = uiState.formation == f,
                                            onClick = { onUpdateTactics(f, uiState.mentality, uiState.marking) },
                                            label = { Text(f.code) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Postura da Equipe", style = MaterialTheme.typography.titleMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Mentality.entries.forEach { m ->
                                        FilterChip(
                                            selected = uiState.mentality == m,
                                            onClick = { onUpdateTactics(uiState.formation, m, uiState.marking) },
                                            label = { Text(m.label) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Estilo de Marcação", style = MaterialTheme.typography.titleMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MarkingStyle.entries.forEach { mk ->
                                        FilterChip(
                                            selected = uiState.marking == mk,
                                            onClick = { onUpdateTactics(uiState.formation, uiState.mentality, mk) },
                                            label = { Text(mk.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "BANCO DE RESERVAS (Toque para escalar)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(reserves, key = { it.id }) { player ->
                        PlayerRosterCard(
                            player = player,
                            onToggleStarter = { onToggleStarter(player.id) },
                            onToggleForSale = { onToggleForSale(player.id) },
                            onRenewContract = { onRenewContract(player.id) }
                        )
                    }
                }
            } else if (subTab == 1) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "TITULARES (${starters.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = PitchGreen500
                        )
                    }
                    items(starters, key = { it.id }) { player ->
                        PlayerRosterCard(
                            player = player,
                            onToggleStarter = { onToggleStarter(player.id) },
                            onToggleForSale = { onToggleForSale(player.id) },
                            onRenewContract = { onRenewContract(player.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "RESERVAS & NÃO RELACIONADOS (${reserves.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(reserves, key = { it.id }) { player ->
                        PlayerRosterCard(
                            player = player,
                            onToggleStarter = { onToggleStarter(player.id) },
                            onToggleForSale = { onToggleForSale(player.id) },
                            onRenewContract = { onRenewContract(player.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = BrazilGold500, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Categorias de Base Sub-20", style = MaterialTheme.typography.titleMedium, color = BrazilGold500)
                                    Text(
                                        text = "Promova jovens talentos sem custo de transferência! Dica: use o treino 'Base' para revelar joias ainda melhores.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    if (uiState.youthProspects.isEmpty()) {
                        item {
                            Text(
                                text = "Todas as promessas desta safra já foram promovidas! Novos talentos chegam na próxima temporada.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        items(uiState.youthProspects, key = { it.id }) { prospect ->
                            YouthProspectCard(
                                prospect = prospect,
                                onPromote = { onPromoteYouth(prospect.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRosterCard(
    player: Player,
    onToggleStarter: () -> Unit,
    onToggleForSale: () -> Unit,
    onRenewContract: () -> Unit = {}
) {
    val posColor = when (player.position.code) {
        "GOL" -> BrazilGold500
        "ZAG", "LAT" -> InfoSky
        "MEI" -> PitchGreen500
        else -> AlertRed
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (player.isStarter) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleStarter)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = posColor.copy(alpha = 0.20f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(width = 44.dp, height = 32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = player.position.code,
                        style = MaterialTheme.typography.labelMedium,
                        color = posColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${player.shirtNumber} ${player.name}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (player.suspendedRounds > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = AlertRed.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("SUSP", color = AlertRed, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    if (player.forSale) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = WarningAmber.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("À VENDA", color = WarningAmber, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
                Text(
                    text = "${player.age} anos • ${player.goalsSeason}g/${player.assistsSeason}a • Contrato ${player.contractRounds}r",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (player.contractRounds <= 12) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { player.energy / 100f },
                        color = if (player.energy >= 75) PitchGreen500 else if (player.energy >= 55) WarningAmber else AlertRed,
                        trackColor = Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${player.energy}%", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = if (player.overall >= 80) BrazilGold500 else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = player.overall.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (player.overall >= 80) PitchEmerald950 else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            IconButton(
                onClick = onRenewContract,
                modifier = Modifier.testTag("renew_contract_${player.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = "Renovar contrato",
                    tint = if (player.contractRounds <= 16) BrazilGold500 else InfoSky,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onToggleForSale) {
                Icon(
                    imageVector = Icons.Default.Sell,
                    contentDescription = "Colocar à venda",
                    tint = if (player.forSale) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onToggleStarter) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = if (player.isStarter) "Mover para reserva" else "Escalar titular",
                    tint = if (player.isStarter) PitchGreen500 else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun YouthProspectCard(
    prospect: YouthProspect,
    onPromote: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = PitchGreen500.copy(alpha = 0.20f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(width = 46.dp, height = 34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(prospect.position.code, style = MaterialTheme.typography.labelMedium, color = PitchGreen500)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(prospect.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${prospect.age} anos • OVR ${prospect.overall} → POT ${prospect.potential} • Salário ${formatCurrency(prospect.salary)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onPromote,
                colors = ButtonDefaults.buttonColors(containerColor = BrazilGold500, contentColor = PitchEmerald950),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("promote_youth_${prospect.id}")
            ) {
                Text("Promover", fontWeight = FontWeight.Bold)
            }
        }
    }
}

