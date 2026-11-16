package com.example.domain.engine

import com.example.domain.model.Club
import com.example.domain.model.Fixture
import com.example.domain.model.MonthlyFinancePoint
import com.example.domain.model.MonthlyLedgerRecord
import com.example.domain.model.SponsorDeal
import com.example.domain.model.TicketPriceLevel
import kotlin.math.roundToLong

object FinanceEngine {

    val SEASON_MONTHS = listOf("Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov")

    fun monthIndexForRound(round: Int, totalRounds: Int): Int {
        val safeTotal = totalRounds.coerceAtLeast(1)
        return (((round - 1).coerceAtLeast(0) * SEASON_MONTHS.size) / safeTotal)
            .coerceIn(0, SEASON_MONTHS.lastIndex)
    }

    fun estimateHomeMatchAttendanceAndRevenue(
        club: Club,
        ticketPriceLevel: TicketPriceLevel
    ): Pair<Int, Long> {
        val fillRatio = (0.58 + (club.prestige * 0.06)) * ticketPriceLevel.attendanceFactor
        val attendance = (club.stadiumCapacity * fillRatio.coerceIn(0.30, 0.99)).toInt()
        val revenue = attendance.toLong() * ticketPriceLevel.priceReais
        return attendance to revenue
    }

    fun buildInitialLedgerFromPlayedRounds(
        club: Club,
        fixtures: List<Fixture>,
        currentRound: Int,
        ticketPriceLevel: TicketPriceLevel
    ): Map<Int, MonthlyLedgerRecord> {
        val totalRounds = club.division.totalRounds
        val playedRounds = (currentRound - 1).coerceAtLeast(0)
        if (playedRounds == 0) return emptyMap()

        val (_, estHomeRevenue) = estimateHomeMatchAttendanceAndRevenue(club, ticketPriceLevel)
        val roundSalary = club.weeklyWages / 4L
        val clubFixtures = fixtures.filter { it.homeClubId == club.id || it.awayClubId == club.id }
        val result = mutableMapOf<Int, MonthlyLedgerRecord>()

        for (r in 1..playedRounds) {
            val monthIdx = monthIndexForRound(r, totalRounds)
            val fx = clubFixtures.firstOrNull { it.round == r }
            val isHome = fx?.homeClubId == club.id
            val ticketRev = if (isHome) estHomeRevenue else 0L
            val awayTv = if (!isHome && fx != null) 320_000L else 0L
            val prev = result[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
            result[monthIdx] = prev.copy(
                ticketIncome = prev.ticketIncome + ticketRev,
                salaryExpense = prev.salaryExpense + roundSalary,
                otherDelta = prev.otherDelta + awayTv,
                roundsPlayed = prev.roundsPlayed + 1,
                homeMatchesPlayed = prev.homeMatchesPlayed + if (isHome) 1 else 0
            )
        }
        return result
    }

    fun buildMonthlyTrends(
        club: Club,
        fixtures: List<Fixture>,
        currentRound: Int,
        ticketPriceLevel: TicketPriceLevel,
        realizedLedger: Map<Int, MonthlyLedgerRecord>,
        activeSponsor: SponsorDeal? = null
    ): List<MonthlyFinancePoint> {
        val totalRounds = club.division.totalRounds
        val clubFixturesByRound = fixtures
            .filter { it.homeClubId == club.id || it.awayClubId == club.id }
            .associateBy { it.round }

        val roundsByMonth = (1..totalRounds).groupBy { r -> monthIndexForRound(r, totalRounds) }
        val (_, estHomeRevenue) = estimateHomeMatchAttendanceAndRevenue(club, ticketPriceLevel)
        val roundSalary = club.weeklyWages / 4L
        val currentMonthIdx = monthIndexForRound(currentRound.coerceAtMost(totalRounds), totalRounds)

        val points = mutableListOf<MonthlyFinancePoint>()
        var rollingBudget = club.budget

        // Determine current month's remaining rounds vs future months so rollingBudget flows forward cleanly
        for (monthIdx in SEASON_MONTHS.indices) {
            val monthRounds = roundsByMonth[monthIdx].orEmpty()
            val recorded = realizedLedger[monthIdx]
            val isPastOrCurrentMonth = currentRound > 1 && monthIdx < currentMonthIdx
            val isCurrentPartialMonth = monthIdx == currentMonthIdx && (recorded?.roundsPlayed ?: 0) > 0

            if (isPastOrCurrentMonth && recorded != null) {
                val net = recorded.ticketIncome + recorded.otherDelta - recorded.salaryExpense
                val monthBudget = recorded.endingBudget ?: club.budget
                points.add(
                    MonthlyFinancePoint(
                        monthIndex = monthIdx,
                        monthLabel = SEASON_MONTHS[monthIdx],
                        ticketIncome = recorded.ticketIncome,
                        salaryExpense = recorded.salaryExpense,
                        netCashFlow = net,
                        budgetBalance = monthBudget,
                        homeMatches = recorded.homeMatchesPlayed,
                        isProjected = false
                    )
                )
            } else if (isCurrentPartialMonth && recorded != null) {
                val remainingRounds = monthRounds.filter { it >= currentRound }
                val remHome = remainingRounds.count { clubFixturesByRound[it]?.homeClubId == club.id }
                val remAway = remainingRounds.size - remHome
                val projTicket = remHome * estHomeRevenue
                val projSalary = remainingRounds.size * roundSalary
                val projOther = remAway * 320_000L

                val totalTicket = recorded.ticketIncome + projTicket
                val totalSalary = recorded.salaryExpense + projSalary
                val totalNet = totalTicket + recorded.otherDelta + projOther - totalSalary
                rollingBudget = club.budget + projTicket + projOther - projSalary

                points.add(
                    MonthlyFinancePoint(
                        monthIndex = monthIdx,
                        monthLabel = SEASON_MONTHS[monthIdx],
                        ticketIncome = totalTicket,
                        salaryExpense = totalSalary,
                        netCashFlow = totalNet,
                        budgetBalance = rollingBudget,
                        homeMatches = recorded.homeMatchesPlayed + remHome,
                        isProjected = remainingRounds.isNotEmpty()
                    )
                )
            } else {
                // Fully upcoming / projected month (or month 0 at round 1)
                val futureRounds = monthRounds.filter { it >= currentRound }
                val activeRounds = if (futureRounds.isNotEmpty()) futureRounds else monthRounds
                val homeCount = activeRounds.count { clubFixturesByRound[it]?.homeClubId == club.id }
                    .let { count ->
                        if (clubFixturesByRound.isEmpty()) (activeRounds.size + 1) / 2 else count
                    }
                val awayCount = (activeRounds.size - homeCount).coerceAtLeast(0)
                val ticketIncome = homeCount * estHomeRevenue
                val salaryExpense = activeRounds.size * roundSalary
                val tvIncome = awayCount * 320_000L
                val net = ticketIncome + tvIncome - salaryExpense

                // Add commercial/sponsorship income (active sponsor deal or club prestige baseline)
                val sponsorIncome = activeSponsor?.let { it.perRoundBase * activeRounds.size }
                    ?: (club.prestige * 180_000L)
                val fullNet = net + sponsorIncome
                rollingBudget += fullNet

                points.add(
                    MonthlyFinancePoint(
                        monthIndex = monthIdx,
                        monthLabel = SEASON_MONTHS[monthIdx],
                        ticketIncome = ticketIncome,
                        salaryExpense = salaryExpense,
                        netCashFlow = fullNet,
                        budgetBalance = rollingBudget,
                        homeMatches = homeCount,
                        isProjected = true
                    )
                )
            }
        }

        return points
    }
}
