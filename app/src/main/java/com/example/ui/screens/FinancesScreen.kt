package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.engine.FinanceEngine
import com.example.domain.model.FfpComplianceLevel
import com.example.domain.model.MonthlyFinancePoint
import com.example.domain.model.SponsorDeal
import com.example.domain.model.TicketPriceLevel
import com.example.ui.components.ClubBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrazilGold500
import com.example.ui.theme.InfoSky
import com.example.ui.theme.PitchEmerald800
import com.example.ui.theme.PitchEmerald950
import com.example.ui.theme.PitchGreen500
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.BrasManagerUiState
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private enum class FinanceChartMode(val label: String) {
    CASH_FLOW("Bilheteria vs. Salários"),
    BUDGET("Evolução do Caixa")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinancesScreen(
    uiState: BrasManagerUiState,
    onTicketPriceChange: (TicketPriceLevel) -> Unit,
    onExpandStadium: () -> Unit,
    onTakeLoan: () -> Unit,
    onSelectSponsor: (SponsorDeal) -> Unit = {},
    onBack: () -> Unit
) {
    val career = uiState.activeCareer ?: return
    val club = uiState.userClub ?: return
    val monthlyTrends = uiState.monthlyFinances

    val (estAttendance, estHomeRevenue) = remember(club.stadiumCapacity, club.prestige, uiState.ticketPriceLevel) {
        FinanceEngine.estimateHomeMatchAttendanceAndRevenue(club, uiState.ticketPriceLevel)
    }

    val monthlySalary = club.weeklyWages
    val perRoundSalary = monthlySalary / 4L
    val topEarners = remember(club.players) {
        club.players.sortedByDescending { it.salary }.take(5)
    }
    val maxSalary = topEarners.firstOrNull()?.salary?.coerceAtLeast(1L) ?: 1L

    val totalProjectedTickets = remember(monthlyTrends) {
        monthlyTrends.sumOf { it.ticketIncome }
    }
    val totalProjectedSalaries = remember(monthlyTrends) {
        monthlyTrends.sumOf { it.salaryExpense }
    }

    var chartMode by remember { mutableStateOf(FinanceChartMode.CASH_FLOW) }
    val currentMonthIdx = remember(career.currentRound, club.division.totalRounds) {
        FinanceEngine.monthIndexForRound(career.currentRound.toInt(), club.division.totalRounds)
    }
    var selectedMonthIndex by remember(monthlyTrends.size, currentMonthIdx) {
        mutableIntStateOf(currentMonthIdx.coerceIn(0, (monthlyTrends.size - 1).coerceAtLeast(0)))
    }
    val selectedPoint = monthlyTrends.getOrNull(selectedMonthIndex)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 700.dp)
                .align(Alignment.TopCenter)
                .testTag("finances_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top App Bar Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("finances_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar ao Clube"
                        )
                    }
                    ClubBadge(
                        shortName = club.shortName,
                        primaryColorHex = club.primaryColorHex,
                        secondaryColorHex = club.secondaryColorHex,
                        crestUrl = club.crestUrl,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FINANÇAS DO CLUBE",
                            style = MaterialTheme.typography.headlineSmall,
                            color = BrazilGold500,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${club.name} • Temp. ${career.seasonYear} • Rodada ${career.currentRound}/${club.division.totalRounds}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Hero Budget Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PitchEmerald800),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("finance_budget_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        PitchEmerald950,
                                        PitchEmerald800
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Caixa do clube",
                                    tint = BrazilGold500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ORÇAMENTO / CAIXA ATUAL",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            Surface(
                                color = if (club.budget >= 0) PitchGreen500.copy(alpha = 0.2f) else AlertRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (club.budget >= 0) "SAUDÁVEL" else "NO VERMELHO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (club.budget >= 0) PitchGreen500 else AlertRed,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatCurrency(club.budget),
                            style = MaterialTheme.typography.displaySmall,
                            color = if (club.budget >= 0) BrazilGold500 else AlertRed,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val netSeasonEst = totalProjectedTickets - totalProjectedSalaries
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (netSeasonEst >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (netSeasonEst >= 0) PitchGreen500 else AlertRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Balanço bilheteria vs. salários na temp.: ${if (netSeasonEst >= 0) "+" else ""}${formatCurrency(netSeasonEst)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Salary Expenses & Ticket Income Summary Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Player Salary Expenses
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finance_salary_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = AlertRed.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = "Despesas salariais",
                                            tint = AlertRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SALÁRIOS / MÊS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = formatCurrency(monthlySalary),
                                style = MaterialTheme.typography.titleLarge,
                                color = AlertRed,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${formatCurrency(perRoundSalary)} / rodada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${club.players.size} atletas no elenco",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Income from Ticket Sales
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finance_ticket_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = PitchGreen500.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ConfirmationNumber,
                                            contentDescription = "Receita de bilheteria",
                                            tint = PitchGreen500,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "BILHETERIA ACUM.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = formatCurrency(uiState.seasonTicketRevenue),
                                style = MaterialTheme.typography.titleLarge,
                                color = PitchGreen500,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "~${formatCurrency(estHomeRevenue)} / jogo em casa",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Público est.: $estAttendance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Financial Fair Play (FIFA FFP) Card
            item {
                val ffp = uiState.fairPlay
                val ffpColor = when (ffp.compliance) {
                    FfpComplianceLevel.COMPLIANT -> PitchGreen500
                    FfpComplianceLevel.WARNING -> WarningAmber
                    FfpComplianceLevel.BREACH -> AlertRed
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("finance_ffp_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Gavel, contentDescription = null, tint = ffpColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fair Play Financeiro (FIFA)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Surface(color = ffpColor.copy(alpha = 0.18f), shape = RoundedCornerShape(50)) {
                                Text(
                                    text = ffp.compliance.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ffpColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TooltipMetric(
                                label = "Resultado Líq.",
                                value = "${if (ffp.seasonNetResult >= 0) "+" else ""}${formatCurrency(ffp.seasonNetResult)}",
                                color = if (ffp.seasonNetResult >= 0) PitchGreen500 else AlertRed
                            )
                            TooltipMetric(
                                label = "Limite Déficit",
                                value = "-${formatCurrency(ffp.allowedDeficitLimit)}",
                                color = InfoSky
                            )
                            TooltipMetric(
                                label = "Folha/Receita",
                                value = "${ffp.wageToIncomeRatioPct}%",
                                color = if (ffp.wageToIncomeRatioPct <= 85) PitchGreen500 else WarningAmber
                            )
                        }
                        if (ffp.transferBanRounds > 0 || ffp.pointsDeducted > 0 || ffp.totalFinesPaid > 0L) {
                            HorizontalDivider()
                            Text(
                                text = "Sanções FIFA: ${ffp.transferBanRounds} rod. Transfer Ban • -${ffp.pointsDeducted} pts • Multas ${formatCurrency(ffp.totalFinesPaid)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AlertRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        ffp.activePunishmentNote?.let { note ->
                            Surface(color = AlertRed.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AlertRed,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Monthly Trend Chart (Recharts-style interactive multi-series area/line chart)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("finance_trend_chart_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tendência Mensal (${career.seasonYear})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Toque em um mês para inspecionar os valores",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FinanceChartMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = chartMode == mode,
                                    onClick = { chartMode = mode },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.testTag("chart_mode_${mode.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Legend
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (chartMode == FinanceChartMode.CASH_FLOW) {
                                ChartLegendItem(color = PitchGreen500, label = "Bilheteria")
                                ChartLegendItem(color = AlertRed, label = "Salários")
                            } else {
                                ChartLegendItem(color = BrazilGold500, label = "Caixa Projetado / Real")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (monthlyTrends.isNotEmpty()) {
                            MonthlyTrendCanvasChart(
                                points = monthlyTrends,
                                mode = chartMode,
                                selectedIndex = selectedMonthIndex,
                                onSelectMonth = { selectedMonthIndex = it }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            selectedPoint?.let { pt ->
                                SelectedMonthTooltipCard(point = pt)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sem dados mensais disponíveis ainda.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Ticket Pricing & Capital Controls
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Gestão de Bilheteria & Infraestrutura",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${club.stadiumName} • Capacidade: ${club.stadiumCapacity} lugares • Preço ajusta público e receita projetada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TicketPriceLevel.entries.forEach { level ->
                                FilterChip(
                                    selected = uiState.ticketPriceLevel == level,
                                    onClick = { onTicketPriceChange(level) },
                                    label = { Text(level.label) },
                                    modifier = Modifier.testTag("finance_ticket_${level.code.lowercase()}")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onExpandStadium,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("finance_expand_stadium")
                            ) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+5 mil (12M)")
                            }
                            OutlinedButton(
                                onClick = onTakeLoan,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("finance_take_loan")
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Empréstimo 10M")
                            }
                        }
                    }
                }
            }

            // Master Sponsorship Deals
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("finance_sponsor_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = BrazilGold500, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Patrocínio Máster", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Atual: ${uiState.activeSponsor?.name ?: "Nenhum"} • Escolha um contrato comercial:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        uiState.availableSponsors.forEach { deal ->
                            val isActive = uiState.activeSponsor?.id == deal.id
                            val eligible = club.prestige >= deal.minPrestige
                            Surface(
                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${deal.name} (${deal.minPrestige}★+)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "+${formatCurrency(deal.perRoundBase)}/rod • Vitória +${formatCurrency(deal.winBonus)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PitchGreen500
                                        )
                                    }
                                    FilterChip(
                                        selected = isActive,
                                        enabled = eligible && !isActive,
                                        onClick = { onSelectSponsor(deal) },
                                        label = { Text(if (isActive) "Ativo" else if (eligible) "Assinar" else "${deal.minPrestige}★") },
                                        modifier = Modifier.testTag("select_sponsor_${deal.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Player Salary Breakdown (Top 5 Earners)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = InfoSky,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Maiores Salários do Elenco",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Total: ${formatCurrency(monthlySalary)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AlertRed
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        topEarners.forEachIndexed { index, player ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}. ${player.name} (${player.position.code} • OVR ${player.overall})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${formatCurrency(player.salary)}/mês",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BrazilGold500,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (player.salary.toFloat() / maxSalary.toFloat()).coerceIn(0.05f, 1f) },
                                    color = BrazilGold500,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }

            // Recent Transfer Cash Movements
            if (uiState.transfers.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Movimentações no Mercado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.transfers.take(6).forEachIndexed { idx, tr ->
                                if (idx > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                val isIncome = tr.transferType == "SELL"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${tr.playerName} (${tr.position})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${tr.fromClubName} → ${tr.toClubName} • Rodada ${tr.roundNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${if (isIncome) "+" else "-"}${formatCurrency(tr.fee)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isIncome) PitchGreen500 else AlertRed,
                                        fontWeight = FontWeight.Bold
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
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthlyTrendCanvasChart(
    points: List<MonthlyFinancePoint>,
    mode: FinanceChartMode,
    selectedIndex: Int,
    onSelectMonth: (Int) -> Unit
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryValues = remember(points, mode) {
        if (mode == FinanceChartMode.CASH_FLOW) {
            points.map { it.ticketIncome.toDouble() }
        } else {
            points.map { it.budgetBalance.toDouble() }
        }
    }
    val secondaryValues = remember(points, mode) {
        if (mode == FinanceChartMode.CASH_FLOW) {
            points.map { it.salaryExpense.toDouble() }
        } else {
            emptyList()
        }
    }

    val allVals = primaryValues + secondaryValues
    val rawMax = (allVals.maxOrNull() ?: 1_000_000.0).coerceAtLeast(1_000_000.0)
    val rawMin = if (mode == FinanceChartMode.BUDGET) {
        (allVals.minOrNull() ?: 0.0).coerceAtMost(0.0)
    } else {
        0.0
    }
    val span = (rawMax - rawMin).coerceAtLeast(1_000_000.0)
    val maxY = rawMax + span * 0.12
    val minY = if (rawMin < 0.0) rawMin - span * 0.08 else 0.0
    val rangeY = (maxY - minY).coerceAtLeast(1.0)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Y-Axis Ticks
            Column(
                modifier = Modifier
                    .width(62.dp)
                    .height(176.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                val ticks = 4
                for (i in ticks downTo 0) {
                    val v = (minY + (rangeY * i / ticks)).roundToLong()
                    Text(
                        text = formatCurrency(v).replace("R$ ", ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Chart Plot Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(176.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points.size) {
                            detectTapGestures { offset ->
                                if (points.isNotEmpty() && size.width > 0) {
                                    val stepX = if (points.size > 1) size.width.toFloat() / (points.size - 1) else size.width.toFloat()
                                    val idx = (offset.x / stepX).roundToInt().coerceIn(0, points.lastIndex)
                                    onSelectMonth(idx)
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val padV = 10.dp.toPx()
                    val plotH = (h - padV * 2).coerceAtLeast(1f)

                    // Horizontal dashed gridlines
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val y = padV + plotH * (1f - i.toFloat() / gridSteps)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    fun xFor(index: Int): Float =
                        if (points.size <= 1) w / 2f else (index.toFloat() / (points.size - 1)) * w

                    fun yFor(value: Double): Float {
                        val ratio = ((value - minY) / rangeY).toFloat().coerceIn(0f, 1f)
                        return padV + plotH * (1f - ratio)
                    }

                    fun drawSmoothAreaSeries(values: List<Double>, strokeColor: Color) {
                        if (values.isEmpty()) return
                        val pts = values.mapIndexed { idx, v -> Offset(xFor(idx), yFor(v)) }
                        val linePath = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            for (i in 1 until pts.size) {
                                val prev = pts[i - 1]
                                val curr = pts[i]
                                val ctrlX = (prev.x + curr.x) / 2f
                                cubicTo(ctrlX, prev.y, ctrlX, curr.y, curr.x, curr.y)
                            }
                        }
                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(pts.last().x, padV + plotH)
                            lineTo(pts.first().x, padV + plotH)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    strokeColor.copy(alpha = 0.28f),
                                    strokeColor.copy(alpha = 0.02f)
                                ),
                                startY = padV,
                                endY = padV + plotH
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = strokeColor,
                            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        pts.forEachIndexed { idx, offset ->
                            val isSelected = idx == selectedIndex
                            drawCircle(
                                color = strokeColor,
                                radius = if (isSelected) 5.5.dp.toPx() else 3.dp.toPx(),
                                center = offset
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.2.dp.toPx(),
                                    center = offset
                                )
                            }
                        }
                    }

                    // Active crosshair for selected month
                    if (selectedIndex in points.indices) {
                        val cx = xFor(selectedIndex)
                        drawLine(
                            color = BrazilGold500.copy(alpha = 0.55f),
                            start = Offset(cx, padV),
                            end = Offset(cx, padV + plotH),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    if (mode == FinanceChartMode.CASH_FLOW) {
                        drawSmoothAreaSeries(secondaryValues, AlertRed)
                        drawSmoothAreaSeries(primaryValues, PitchGreen500)
                    } else {
                        drawSmoothAreaSeries(primaryValues, BrazilGold500)
                    }
                }
            }
        }

        // X-Axis Month Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 70.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { idx, pt ->
                Text(
                    text = pt.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (idx == selectedIndex) BrazilGold500 else labelColor
                )
            }
        }
    }
}

@Composable
private fun SelectedMonthTooltipCard(point: MonthlyFinancePoint) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, BrazilGold500.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("finance_month_tooltip")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detalhamento: ${point.monthLabel} (${point.homeMatches} jogos em casa)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = if (point.isProjected) InfoSky.copy(alpha = 0.16f) else PitchGreen500.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (point.isProjected) "PROJEÇÃO" else "REALIZADO",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (point.isProjected) InfoSky else PitchGreen500,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TooltipMetric(label = "Bilheteria", value = formatCurrency(point.ticketIncome), color = PitchGreen500)
                TooltipMetric(label = "Salários", value = formatCurrency(point.salaryExpense), color = AlertRed)
                TooltipMetric(
                    label = "Saldo Líq.",
                    value = "${if (point.netCashFlow >= 0) "+" else ""}${formatCurrency(point.netCashFlow)}",
                    color = if (point.netCashFlow >= 0) PitchGreen500 else AlertRed
                )
                TooltipMetric(label = "Caixa Fim Mês", value = formatCurrency(point.budgetBalance), color = BrazilGold500)
            }
        }
    }
}

@Composable
private fun TooltipMetric(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
