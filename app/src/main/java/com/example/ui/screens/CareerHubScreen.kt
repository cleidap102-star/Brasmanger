package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.firebase.CareerSave
import com.example.domain.model.Club
import com.example.domain.model.Division
import com.example.ui.components.ClubBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.BrazilGold500
import com.example.ui.theme.InfoSky
import com.example.ui.theme.PitchEmerald900
import com.example.ui.theme.PitchEmerald950
import com.example.ui.theme.PitchGreen500
import com.example.ui.viewmodel.BrasManagerUiState

private enum class HubRegionFilter(val label: String) {
    BRASIL("Brasil"),
    EUROPA("Europa"),
    AMERICA("Argentina")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CareerHubScreen(
    uiState: BrasManagerUiState,
    onResumeCareer: (CareerSave) -> Unit,
    onStartNewCareer: (String) -> Unit,
    onDeleteCareer: (String) -> Unit,
    onSyncNow: () -> Unit,
    onUpdateManagerName: (String) -> Unit = {}
) {
    val hasSaves = uiState.savedCareers.isNotEmpty()
    // 0 = Meus Saves, 1 = Nova Carreira
    var hubMode by remember(hasSaves) { mutableIntStateOf(if (hasSaves) 0 else 1) }
    var selectedRegion by remember { mutableStateOf(HubRegionFilter.BRASIL) }
    var selectedDivision by remember { mutableStateOf(Division.SERIE_A) }
    var searchQuery by remember { mutableStateOf("") }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember(uiState.profile?.managerName) {
        mutableStateOf(uiState.profile?.managerName ?: "Técnico Brasileiro")
    }

    val regionDivisions = remember(selectedRegion) {
        when (selectedRegion) {
            HubRegionFilter.BRASIL -> listOf(Division.SERIE_A, Division.SERIE_B, Division.SERIE_C, Division.SERIE_D)
            HubRegionFilter.EUROPA -> listOf(
                Division.PREMIER_LEAGUE,
                Division.LA_LIGA,
                Division.SERIE_A_ITA,
                Division.BUNDESLIGA,
                Division.LIGUE_1
            )
            HubRegionFilter.AMERICA -> listOf(Division.LIGA_ARGENTINA)
        }
    }

    val clubCountsByDivision = remember(uiState.clubs) {
        uiState.clubs.groupingBy { it.division }.eachCount()
    }

    val filteredClubs = remember(uiState.clubs, selectedDivision, searchQuery) {
        val base = if (searchQuery.isBlank()) {
            uiState.clubs.filter { it.division == selectedDivision }
        } else {
            uiState.clubs.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.shortName.contains(searchQuery, ignoreCase = true) ||
                    it.division.shortLabel.contains(searchQuery, ignoreCase = true) ||
                    it.division.country.contains(searchQuery, ignoreCase = true)
            }
        }
        base.sortedByDescending { it.averageOverall }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PitchEmerald950, MaterialTheme.colorScheme.background)
                )
            )
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Stadium Hero Entrance Header
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, PitchGreen500.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_stadium_hero),
                            contentDescription = "Estádio BrasManager",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(196.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(196.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            PitchEmerald950.copy(alpha = 0.62f),
                                            PitchEmerald950.copy(alpha = 0.94f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(50),
                                    onClick = {
                                        editedName = uiState.profile?.managerName ?: "Técnico Brasileiro"
                                        showEditNameDialog = true
                                    },
                                    modifier = Modifier.testTag("edit_manager_name_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = "${uiState.profile?.managerName ?: "Técnico"} • ${uiState.profile?.totalTitles ?: 0} 🏆",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar nome do técnico",
                                            tint = BrazilGold500,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Surface(
                                    color = PitchGreen500.copy(alpha = 0.18f),
                                    shape = CircleShape,
                                    onClick = onSyncNow,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("hub_sync_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = "Atualizar Elencos",
                                            tint = PitchGreen500,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = BrazilGold500.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, BrazilGold500.copy(alpha = 0.5f)),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = BrazilGold500,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "BRASMANAGER 26",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = BrazilGold500,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Comando tático, mercado, Champions e Libertadores",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HeroStatChip("${Division.entries.size} Ligas", PitchGreen500)
                                HeroStatChip("Champions 36 Clubes", InfoSky)
                                HeroStatChip("Save Offline Rápido", BrazilGold500)
                            }
                        }
                    }
                }
            }

            // 2. Spotlight 1-Tap Continue Save Card (when a saved career exists)
            if (hasSaves) {
                val latestCareer = uiState.savedCareers.first()
                val latestClub = uiState.clubs.firstOrNull { it.id == latestCareer.clubId }
                val latestDiv = Division.fromCode(latestCareer.division)
                val progress = (latestCareer.currentRound.toFloat() / latestDiv.totalRounds.coerceAtLeast(1)).coerceIn(0f, 1f)

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PitchEmerald900),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, PitchGreen500.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ÚLTIMO SAVE GRAVADO",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PitchGreen500,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatCurrency(latestCareer.budget),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrazilGold500,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ClubBadge(
                                    shortName = latestClub?.shortName ?: latestCareer.clubName.take(3).uppercase(),
                                    primaryColorHex = latestClub?.primaryColorHex ?: 0xFF10B981L,
                                    secondaryColorHex = latestClub?.secondaryColorHex ?: 0xFFFFFFFFL,
                                    crestUrl = latestClub?.crestUrl,
                                    size = 50.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = latestCareer.clubName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${latestDiv.shortLabel} • Temp. ${latestCareer.seasonYear} • ${latestCareer.leaguePosition}º lugar (${latestCareer.points} pts)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                color = PitchGreen500,
                                trackColor = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )

                            Button(
                                onClick = { onResumeCareer(latestCareer) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PitchGreen500,
                                    contentColor = PitchEmerald950
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("continue_latest_save_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CONTINUAR (${latestCareer.clubName.uppercase()} • ROD. ${latestCareer.currentRound})",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // 3. Clean Mode Switcher Pills (Saves vs New Career)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModeSelectorCard(
                            selected = hubMode == 0,
                            title = "Meus Saves (${uiState.savedCareers.size})",
                            icon = Icons.Default.Bookmarks,
                            accent = PitchGreen500,
                            onClick = { hubMode = 0 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hub_mode_saves")
                        )
                        ModeSelectorCard(
                            selected = hubMode == 1,
                            title = "Nova Carreira",
                            icon = Icons.Default.AddCircleOutline,
                            accent = BrazilGold500,
                            onClick = { hubMode = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hub_mode_new")
                        )
                    }
                }
            }

            // 4A. Saved Careers List
            if (hasSaves && hubMode == 0) {
                items(uiState.savedCareers, key = { it.id }) { career ->
                    val club = uiState.clubs.firstOrNull { it.id == career.clubId }
                    val savedDiv = Division.fromCode(career.division)
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
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(club?.primaryColorHex ?: 0xFF10B981L))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            ClubBadge(
                                shortName = club?.shortName ?: career.clubName.take(3).uppercase(),
                                primaryColorHex = club?.primaryColorHex ?: 0xFF10B981L,
                                secondaryColorHex = club?.secondaryColorHex ?: 0xFFFFFFFFL,
                                crestUrl = club?.crestUrl,
                                size = 46.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = career.clubName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${savedDiv.shortLabel} • Temp. ${career.seasonYear} • Rodada ${career.currentRound}/${savedDiv.totalRounds}",
                                    style = MaterialTheme.typography.bodySmall,
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

            // 4B. New Career Quick Club Picker
            if (!hasSaves || hubMode == 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "ESCOLHA SEU CLUBE",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrazilGold500,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar clube, país ou liga...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (searchQuery.isBlank()) {
                            // Region tabs so mobile users aren't flooded by 10 chips at once
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HubRegionFilter.entries.forEach { reg ->
                                    FilterChip(
                                        selected = selectedRegion == reg,
                                        onClick = {
                                            selectedRegion = reg
                                            selectedDivision = when (reg) {
                                                HubRegionFilter.BRASIL -> Division.SERIE_A
                                                HubRegionFilter.EUROPA -> Division.PREMIER_LEAGUE
                                                HubRegionFilter.AMERICA -> Division.LIGA_ARGENTINA
                                            }
                                        },
                                        label = { Text(reg.label) }
                                    )
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(regionDivisions) { div ->
                                    val count = clubCountsByDivision[div] ?: 0
                                    FilterChip(
                                        selected = selectedDivision == div,
                                        onClick = { selectedDivision = div },
                                        label = { Text("${div.shortLabel} ($count)") },
                                        modifier = Modifier.testTag("filter_${div.code.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredClubs.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Nenhum clube encontrado para \"$searchQuery\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(filteredClubs, key = { it.id }) { club ->
                        ClubSelectionRow(
                            club = club,
                            onSelect = { onStartNewCareer(club.id) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Nome do Técnico") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { if (it.length <= 40) editedName = it },
                    label = { Text("Seu nome na beira do campo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manager_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateManagerName(editedName)
                        showEditNameDialog = false
                    },
                    modifier = Modifier.testTag("save_manager_name_button")
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun HeroStatChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ModeSelectorCard(
    selected: Boolean,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) accent else Color.Transparent),
        onClick = onClick,
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
            .clickable(onClick = onSelect)
            .testTag("select_club_${club.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(club.primaryColorHex))
            )
            Spacer(modifier = Modifier.width(10.dp))
            ClubBadge(
                shortName = club.shortName,
                primaryColorHex = club.primaryColorHex,
                secondaryColorHex = club.secondaryColorHex,
                crestUrl = club.crestUrl,
                size = 46.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = club.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
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
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${club.division.shortLabel} • ${formatCurrency(club.budget)} • ${club.stadiumName}",
                    style = MaterialTheme.typography.bodySmall,
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
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${club.players.size} atletas",
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
                modifier = Modifier
                    .height(48.dp)
                    .testTag("start_career_${club.id}")
            ) {
                Text("Assumir", fontWeight = FontWeight.Bold)
            }
        }
    }
}
