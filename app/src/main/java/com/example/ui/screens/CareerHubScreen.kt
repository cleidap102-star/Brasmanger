package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.firebase.CareerSave
import com.example.domain.model.Club
import com.example.domain.model.Division
import com.example.ui.components.ClubBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.BrazilGold500
import com.example.ui.theme.PitchEmerald950
import com.example.ui.theme.PitchGreen500
import com.example.ui.viewmodel.BrasManagerUiState

@Composable
fun CareerHubScreen(
    uiState: BrasManagerUiState,
    onResumeCareer: (CareerSave) -> Unit,
    onStartNewCareer: (String) -> Unit,
    onDeleteCareer: (String) -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit
) {
    var selectedDivision by remember { mutableStateOf(Division.SERIE_A) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredClubs = remember(uiState.clubs, selectedDivision, searchQuery) {
        uiState.clubs
            .filter { it.division == selectedDivision }
            .filter {
                searchQuery.isBlank() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.shortName.contains(searchQuery, ignoreCase = true)
            }
            .sortedByDescending { it.averageOverall }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BRASMANAGER 26",
                            style = MaterialTheme.typography.headlineLarge,
                            color = BrazilGold500
                        )
                        Text(
                            text = "Olá, ${uiState.profile?.managerName ?: "Técnico"} • ${uiState.profile?.totalTitles ?: 0} títulos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onSyncNow,
                        modifier = Modifier.testTag("hub_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Atualizar Elencos",
                            tint = PitchGreen500
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sair da conta",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.savedCareers.isNotEmpty()) {
                item {
                    Text(
                        text = "CARREIRAS SALVAS NA NUVEM",
                        style = MaterialTheme.typography.labelMedium,
                        color = PitchGreen500,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(uiState.savedCareers, key = { it.id }) { career ->
                    val club = uiState.clubs.firstOrNull { it.id == career.clubId }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResumeCareer(career) }
                            .testTag("saved_career_${career.clubId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ClubBadge(
                                shortName = club?.shortName ?: career.clubName.take(3).uppercase(),
                                primaryColorHex = club?.primaryColorHex ?: 0xFF10B981L,
                                secondaryColorHex = club?.secondaryColorHex ?: 0xFFFFFFFFL,
                                crestUrl = club?.crestUrl,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = career.clubName,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${if (career.division == "SERIE_A") "Série A" else "Série B"} • Temp. ${career.seasonYear} • Rodada ${career.currentRound}/38",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${career.leaguePosition}º lugar • ${career.points} pts • ${formatCurrency(career.budget)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrazilGold500
                                )
                            }
                            IconButton(onClick = { onDeleteCareer(career.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Excluir carreira",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onResumeCareer(career) }) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Continuar carreira",
                                    tint = PitchGreen500
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "INICIAR NOVA CARREIRA",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrazilGold500
                )
                Text(
                    text = "Escolha qualquer clube das Séries A ou B do Campeonato Brasileiro 2026:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedDivision == Division.SERIE_A,
                        onClick = { selectedDivision = Division.SERIE_A },
                        label = { Text("Série A (20)") },
                        modifier = Modifier.testTag("filter_serie_a")
                    )
                    FilterChip(
                        selected = selectedDivision == Division.SERIE_B,
                        onClick = { selectedDivision = Division.SERIE_B },
                        label = { Text("Série B (20)") },
                        modifier = Modifier.testTag("filter_serie_b")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar clube...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(filteredClubs, key = { it.id }) { club ->
                ClubSelectionRow(
                    club = club,
                    onSelect = { onStartNewCareer(club.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ClubSelectionRow(
    club: Club,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("select_club_${club.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClubBadge(
                shortName = club.shortName,
                primaryColorHex = club.primaryColorHex,
                secondaryColorHex = club.secondaryColorHex,
                crestUrl = club.crestUrl,
                size = 48.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = club.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = PitchGreen500.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "OVR ${club.averageOverall}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PitchGreen500,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${club.stadiumName} • Caixa ${formatCurrency(club.budget)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    repeat(club.prestige) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = BrazilGold500,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${club.players.size} jogadores",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSelect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrazilGold500,
                    contentColor = PitchEmerald950
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier.testTag("start_career_${club.id}")
            ) {
                Text("Assumir", fontWeight = FontWeight.Bold)
            }
        }
    }
}
