package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.Club
import com.example.domain.model.Player
import com.example.domain.model.PlayerPosition
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatTimestampPtBr
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrazilGold500
import com.example.ui.theme.InfoSky
import com.example.ui.theme.PitchEmerald950
import com.example.ui.theme.PitchGreen500
import com.example.ui.viewmodel.BrasManagerUiState
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarketSyncScreen(
    uiState: BrasManagerUiState,
    onBuyPlayer: (Player, Club, Long) -> Unit,
    onSignFreeAgent: (Player) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onSyncNow: () -> Unit
) {
    val userClub = uiState.userClub ?: return
    var subTab by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var posFilter by remember { mutableStateOf<PlayerPosition?>(null) }
    var freeAgentsOnly by remember { mutableStateOf(false) }
    var negotiatingTarget by remember { mutableStateOf<Pair<Player, Club>?>(null) }

    val marketPlayers = remember(uiState.clubs, userClub.id, search, posFilter) {
        uiState.clubs
            .filter { it.id != userClub.id }
            .flatMap { c -> c.players.map { p -> p to c } }
            .filter { (p, _) ->
                (posFilter == null || p.position == posFilter) &&
                    (search.isBlank() || p.name.contains(search, ignoreCase = true))
            }
            .sortedByDescending { it.first.overall }
            .take(50)
    }

    val filteredFreeAgents = remember(uiState.freeAgents, search, posFilter) {
        uiState.freeAgents.filter { p ->
            (posFilter == null || p.position == posFilter) &&
                (search.isBlank() || p.name.contains(search, ignoreCase = true))
        }
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
                        text = "MERCADO & SYNC AO VIVO",
                        style = MaterialTheme.typography.headlineMedium,
                        color = BrazilGold500
                    )
                    Text(
                        text = "Caixa disponível: ${formatCurrency(userClub.budget)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PitchGreen500
                    )
                }
            }

            TabRow(selectedTabIndex = subTab) {
                Tab(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    text = { Text("Transferências") },
                    modifier = Modifier.testTag("tab_transfers")
                )
                Tab(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    text = { Text("Sync Elencos") },
                    modifier = Modifier.testTag("tab_sync")
                )
            }

            if (subTab == 0) {
                val window = uiState.transferWindowStatus
                val banRounds = uiState.fairPlay.transferBanRounds
                val marketUnlocked = window.isOpen && banRounds <= 0

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (window.isOpen) PitchGreen500.copy(alpha = 0.14f) else AlertRed.copy(alpha = 0.14f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transfer_window_banner")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (window.isOpen) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (window.isOpen) PitchGreen500 else AlertRed,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = window.windowName.uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (window.isOpen) PitchGreen500 else AlertRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = window.statusLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Calendário oficial de registros: ${window.openRangesText}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (banRounds > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.18f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fifa_transfer_ban_banner")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Gavel, contentDescription = null, tint = AlertRed, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "TRANSFER BAN FIFA • $banRounds RODADA(S)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = AlertRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = uiState.fairPlay.activePunishmentNote
                                                ?: "Contratações suspensas até o cumprimento da punição de Fair Play Financeiro.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = { Text("Buscar jogador...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !freeAgentsOnly,
                                onClick = { freeAgentsOnly = false },
                                label = { Text("Clubes") }
                            )
                            FilterChip(
                                selected = freeAgentsOnly,
                                onClick = { freeAgentsOnly = true },
                                label = { Text("Sem Clube (${uiState.freeAgents.size})") }
                            )
                            PlayerPosition.entries.forEach { pos ->
                                FilterChip(
                                    selected = posFilter == pos,
                                    onClick = { posFilter = if (posFilter == pos) null else pos },
                                    label = { Text(pos.code) }
                                )
                            }
                        }
                    }

                    if (freeAgentsOnly) {
                        items(filteredFreeAgents, key = { it.id }) { player ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${player.name} (${player.position.code})", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "Sem Clube • ${player.age} anos • OVR ${player.overall} • Luvas: ${formatCurrency((player.marketValue * 0.20).toLong().coerceAtLeast(250_000L))}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = { onSignFreeAgent(player) },
                                        enabled = marketUnlocked,
                                        colors = ButtonDefaults.buttonColors(containerColor = PitchGreen500, contentColor = PitchEmerald950),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                banRounds > 0 -> "Banido"
                                                !window.isOpen -> "Fechada"
                                                else -> "Contratar"
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(marketPlayers, key = { it.first.id }) { (player, club) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${player.name} (${player.position.code}) • OVR ${player.overall}",
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${club.name} (${club.division.shortLabel}) • ${player.age} anos • ${formatCurrency(player.marketValue)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Button(
                                        onClick = { negotiatingTarget = player to club },
                                        enabled = marketUnlocked,
                                        colors = ButtonDefaults.buttonColors(containerColor = BrazilGold500, contentColor = PitchEmerald950),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                banRounds > 0 -> "Banido"
                                                !window.isOpen -> "Fechada"
                                                else -> "Negociar"
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Live ESPN Squad Sync Tab
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Atualização Automática na Temporada", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "Sincroniza contratações reais e elencos do Brasileirão e das ligas internacionais via API da ESPN ao abrir a carreira e nas janelas.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = uiState.profile?.autoSyncEnabled != false,
                                        onCheckedChange = onToggleAutoSync,
                                        modifier = Modifier.testTag("auto_sync_switch")
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = onSyncNow,
                                    enabled = !uiState.isSyncing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PitchGreen500,
                                        contentColor = PitchEmerald950
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("sync_now_button")
                                ) {
                                    if (uiState.isSyncing) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PitchEmerald950, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Consultando elencos ao vivo...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.CloudSync, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sincronizar Elencos Agora", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    uiState.lastSyncReport?.let { report ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = PitchGreen500)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(report.sourceLabel, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Text(
                                        text = "${formatTimestampPtBr(report.timestamp)} • ${report.clubsSynced} clubes verificados • ${report.playersAdded} reforços • ${report.playersUpdated} atualizações",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InfoSky,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Text(
                                        text = report.summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }

                        items(report.entries) { entry ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (entry.action == "NOVO") PitchGreen500.copy(alpha = 0.2f) else BrazilGold500.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = entry.action,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (entry.action == "NOVO") PitchGreen500 else BrazilGold500,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${entry.playerName} (${entry.position}) • ${entry.clubName}", style = MaterialTheme.typography.titleMedium)
                                        Text(entry.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.syncLogs.isNotEmpty()) {
                        item {
                            Text(
                                text = "HISTÓRICO DE SINCRONIZAÇÕES",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(uiState.syncLogs, key = { it.id }) { log ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(formatTimestampPtBr(log.timestampMs), style = MaterialTheme.typography.labelMedium, color = BrazilGold500)
                                    Text(log.summary, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    negotiatingTarget?.let { (player, sellerClub) ->
        var offerRatio by remember(player.id) { mutableFloatStateOf(1.1f) }
        val offerAmount = ((player.marketValue * offerRatio).roundToLong() / 100_000L) * 100_000L
        AlertDialog(
            onDismissRequest = { negotiatingTarget = null },
            title = { Text("Negociar com ${sellerClub.name}") },
            text = {
                Column {
                    Text("${player.name} (${player.position.code}) • OVR ${player.overall}")
                    Text("Valor de mercado: ${formatCurrency(player.marketValue)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Sua proposta: ${formatCurrency(offerAmount)}", style = MaterialTheme.typography.titleMedium, color = BrazilGold500)
                    Slider(
                        value = offerRatio,
                        onValueChange = { offerRatio = it },
                        valueRange = 0.85f..1.55f
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBuyPlayer(player, sellerClub, offerAmount)
                        negotiatingTarget = null
                    }
                ) {
                    Text("Enviar Proposta")
                }
            },
            dismissButton = {
                TextButton(onClick = { negotiatingTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
