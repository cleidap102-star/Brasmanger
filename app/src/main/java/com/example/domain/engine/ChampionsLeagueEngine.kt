package com.example.domain.engine

import com.example.domain.model.ChampionsLeagueFixture
import com.example.domain.model.ChampionsLeaguePhase
import com.example.domain.model.ChampionsLeagueStanding
import com.example.domain.model.ChampionsLeagueState
import com.example.domain.model.Club
import com.example.domain.model.CompetitionType
import com.example.domain.model.CupMatchTie
import com.example.domain.model.CupType
import com.example.domain.model.Division
import com.example.domain.model.LiveFixtureState
import com.example.domain.model.LiveMatchState
import com.example.domain.model.MatchEvent
import com.example.domain.model.MatchEventType
import com.example.domain.model.PenaltyShootoutResult
import com.example.domain.model.TicketPriceLevel
import kotlin.random.Random

object ChampionsLeagueEngine {

    private val EURO_DIVISIONS = setOf(
        Division.PREMIER_LEAGUE,
        Division.LA_LIGA,
        Division.SERIE_A_ITA,
        Division.BUNDESLIGA,
        Division.LIGUE_1
    )

    // Fits comfortably inside both 34-round (Bundesliga/Ligue 1) and 38-round seasons
    private val LEAGUE_PHASE_TRIGGER_ROUNDS = listOf(2, 4, 6, 8, 10, 12, 14, 16)
    private const val PLAYOFF_TRIGGER_ROUND = 19
    private val KNOCKOUT_TRIGGER_ROUNDS = listOf(22, 25, 28, 31) // Oitavas, Quartas, Semifinal, Final

    fun isEuropeanClub(club: Club): Boolean = club.division in EURO_DIVISIONS

    fun buildInitialChampionsLeague(userClub: Club, allClubs: List<Club>): ChampionsLeagueState {
        val euroClubs = allClubs.filter { it.division in EURO_DIVISIONS }
        val pool = if (isEuropeanClub(userClub)) {
            val others = euroClubs.filter { it.id != userClub.id }
                .sortedByDescending { it.prestige * 20 + it.averageOverall }
                .take(35)
            (others + userClub).sortedByDescending { it.prestige * 20 + it.averageOverall }
        } else {
            euroClubs
                .sortedByDescending { it.prestige * 20 + it.averageOverall }
                .take(36)
        }

        val participants = pool.take(36)
        val standings = participants.mapIndexed { idx, club ->
            ChampionsLeagueStanding(
                clubId = club.id,
                pot = (idx / 9) + 1
            )
        }

        // 8 balanced League Phase matchdays via Berger rotation (every club plays 8 distinct clubs, 4H/4A)
        val n = participants.size
        val fixtures = mutableListOf<ChampionsLeagueFixture>()
        if (n >= 2) {
            val rotation = participants.drop(1).toMutableList()
            val fixed = participants.first()
            val half = n / 2

            for (md in 1..8) {
                val roundTeams = listOf(fixed) + rotation
                val trigger = LEAGUE_PHASE_TRIGGER_ROUNDS[md - 1]
                for (i in 0 until half) {
                    val t1 = roundTeams[i]
                    val t2 = roundTeams[n - 1 - i]
                    val (home, away) = if ((md + i) % 2 == 0) t1 to t2 else t2 to t1
                    fixtures.add(
                        ChampionsLeagueFixture(
                            id = "ucl_md${md}_${home.id}_${away.id}",
                            matchday = md,
                            homeClubId = home.id,
                            awayClubId = away.id,
                            triggerAfterLeagueRound = trigger
                        )
                    )
                }
                val last = rotation.removeAt(rotation.lastIndex)
                rotation.add(0, last)
            }
        }

        return ChampionsLeagueState(
            phase = ChampionsLeaguePhase.LEAGUE_PHASE,
            currentMatchday = 1,
            standings = standings,
            leagueFixtures = fixtures,
            knockoutTies = emptyList(),
            currentKnockoutStageIndex = 0,
            championClubId = null
        )
    }

    fun sortStandings(
        standings: List<ChampionsLeagueStanding>,
        clubMap: Map<String, Club>
    ): List<ChampionsLeagueStanding> {
        return standings.sortedWith(
            compareByDescending<ChampionsLeagueStanding> { it.points }
                .thenByDescending { it.goalDifference }
                .thenByDescending { it.goalsFor }
                .thenByDescending { it.wins }
                .thenBy { clubMap[it.clubId]?.name ?: it.clubId }
        )
    }

    sealed interface PendingChampionsMatch {
        data class LeaguePhaseMatch(val fixture: ChampionsLeagueFixture) : PendingChampionsMatch
        data class KnockoutMatch(val tie: CupMatchTie) : PendingChampionsMatch
    }

    fun findPendingUserMatch(
        ucl: ChampionsLeagueState?,
        userClubId: String,
        completedLeagueRound: Int
    ): PendingChampionsMatch? {
        if (ucl == null || ucl.championClubId != null || ucl.phase == ChampionsLeaguePhase.FINISHED) return null
        if (ucl.phase == ChampionsLeaguePhase.LEAGUE_PHASE) {
            val f = ucl.leagueFixtures.firstOrNull {
                !it.played &&
                    it.matchday == ucl.currentMatchday &&
                    (it.homeClubId == userClubId || it.awayClubId == userClubId) &&
                    completedLeagueRound >= it.triggerAfterLeagueRound
            }
            if (f != null) return PendingChampionsMatch.LeaguePhaseMatch(f)
        } else {
            val tie = ucl.knockoutTies.firstOrNull {
                it.winnerClubId == null &&
                    it.stageIndex == ucl.currentKnockoutStageIndex &&
                    (it.homeClubId == userClubId || it.awayClubId == userClubId) &&
                    completedLeagueRound >= it.triggerAfterLeagueRound
            }
            if (tie != null) return PendingChampionsMatch.KnockoutMatch(tie)
        }
        return null
    }

    fun prepareLeaguePhaseLiveMatch(
        fixture: ChampionsLeagueFixture,
        userClub: Club,
        allClubs: List<Club>,
        ucl: ChampionsLeagueState,
        ticketPriceLevel: TicketPriceLevel
    ): LiveMatchState {
        val clubMap = allClubs.associateBy { it.id }
        val homeClub = clubMap[fixture.homeClubId] ?: userClub
        val awayClub = clubMap[fixture.awayClubId] ?: userClub
        val userIsHome = homeClub.id == userClub.id

        val fillRatio = (0.78 + (homeClub.prestige * 0.045)) * ticketPriceLevel.attendanceFactor
        val attendance = (homeClub.stadiumCapacity * fillRatio.coerceIn(0.50, 1.0)).toInt()
        val revenue = if (userIsHome) attendance.toLong() * ticketPriceLevel.priceReais else 0L

        val userLive = LiveFixtureState(
            fixtureId = fixture.id,
            homeClubId = homeClub.id,
            homeName = homeClub.name,
            homeShort = homeClub.shortName,
            homeColor = homeClub.primaryColorHex,
            awayClubId = awayClub.id,
            awayName = awayClub.name,
            awayShort = awayClub.shortName,
            awayColor = awayClub.primaryColorHex
        )

        val otherMatches = ucl.leagueFixtures.filter {
            it.matchday == fixture.matchday && it.id != fixture.id && !it.played
        }.mapNotNull { other ->
            val h = clubMap[other.homeClubId] ?: return@mapNotNull null
            val a = clubMap[other.awayClubId] ?: return@mapNotNull null
            LiveFixtureState(
                fixtureId = other.id,
                homeClubId = h.id,
                homeName = h.name,
                homeShort = h.shortName,
                homeColor = h.primaryColorHex,
                awayClubId = a.id,
                awayName = a.name,
                awayShort = a.shortName,
                awayColor = a.primaryColorHex
            )
        }

        val kickoff = MatchEvent(
            minute = 0,
            type = MatchEventType.KICKOFF,
            clubId = homeClub.id,
            playerName = "",
            description = "Champions League • Fase de Liga (Rodada ${fixture.matchday}/8) em ${homeClub.stadiumName}!"
        )

        return LiveMatchState(
            round = fixture.triggerAfterLeagueRound,
            competition = CompetitionType.CHAMPIONS_LEAGUE,
            minute = 0,
            phase = "1º Tempo",
            isPaused = false,
            isFinished = false,
            speed = 2,
            userIsHome = userIsHome,
            userMatch = userLive,
            otherMatches = otherMatches,
            events = listOf(kickoff),
            possessionHome = 50,
            attendance = attendance,
            ticketRevenue = revenue,
            isKnockout = false,
            cupType = CupType.CHAMPIONS,
            cupStageLabel = "Fase de Liga • J${fixture.matchday}/8",
            ballZone = 0.5f,
            championsFixtureId = fixture.id
        )
    }

    fun prepareKnockoutLiveMatch(
        tie: CupMatchTie,
        userClub: Club,
        allClubs: List<Club>,
        ucl: ChampionsLeagueState,
        ticketPriceLevel: TicketPriceLevel
    ): LiveMatchState {
        val clubMap = allClubs.associateBy { it.id }
        val homeClub = clubMap[tie.homeClubId] ?: userClub
        val awayClub = clubMap[tie.awayClubId] ?: userClub
        val userIsHome = homeClub.id == userClub.id

        val fillRatio = (0.82 + (homeClub.prestige * 0.04)) * ticketPriceLevel.attendanceFactor
        val attendance = (homeClub.stadiumCapacity * fillRatio.coerceIn(0.55, 1.0)).toInt()
        val revenue = if (userIsHome) attendance.toLong() * ticketPriceLevel.priceReais else 0L

        val userLive = LiveFixtureState(
            fixtureId = tie.id,
            homeClubId = homeClub.id,
            homeName = homeClub.name,
            homeShort = homeClub.shortName,
            homeColor = homeClub.primaryColorHex,
            awayClubId = awayClub.id,
            awayName = awayClub.name,
            awayShort = awayClub.shortName,
            awayColor = awayClub.primaryColorHex
        )

        val otherTies = ucl.knockoutTies.filter {
            it.stageIndex == tie.stageIndex && it.id != tie.id && it.winnerClubId == null
        }.mapNotNull { other ->
            val h = clubMap[other.homeClubId] ?: return@mapNotNull null
            val a = clubMap[other.awayClubId] ?: return@mapNotNull null
            LiveFixtureState(
                fixtureId = other.id,
                homeClubId = h.id,
                homeName = h.name,
                homeShort = h.shortName,
                homeColor = h.primaryColorHex,
                awayClubId = a.id,
                awayName = a.name,
                awayShort = a.shortName,
                awayColor = a.primaryColorHex
            )
        }

        val kickoff = MatchEvent(
            minute = 0,
            type = MatchEventType.KICKOFF,
            clubId = homeClub.id,
            playerName = "",
            description = "UEFA Champions League (${tie.stageLabel})! Bola rolando em ${homeClub.stadiumName}!"
        )

        return LiveMatchState(
            round = tie.triggerAfterLeagueRound,
            competition = CompetitionType.CHAMPIONS_LEAGUE,
            minute = 0,
            phase = "1º Tempo",
            isPaused = false,
            isFinished = false,
            speed = 2,
            userIsHome = userIsHome,
            userMatch = userLive,
            otherMatches = otherTies,
            events = listOf(kickoff),
            possessionHome = 50,
            attendance = attendance,
            ticketRevenue = revenue,
            isKnockout = true,
            cupType = CupType.CHAMPIONS,
            cupTieId = tie.id,
            cupStageLabel = tie.stageLabel,
            ballZone = 0.5f
        )
    }

    fun resolveLeagueMatchday(
        ucl: ChampionsLeagueState,
        userFixtureId: String?,
        userHomeScore: Int?,
        userAwayScore: Int?,
        liveOtherMatches: List<LiveFixtureState> = emptyList(),
        clubMap: Map<String, Club>,
        random: Random = Random.Default
    ): ChampionsLeagueState {
        if (ucl.phase != ChampionsLeaguePhase.LEAGUE_PHASE) return ucl
        val md = ucl.currentMatchday
        val liveById = liveOtherMatches.associateBy { it.fixtureId }
        val standingMap = ucl.standings.associateBy { it.clubId }.toMutableMap()

        val updatedFixtures = ucl.leagueFixtures.map { f ->
            if (f.matchday != md || f.played) {
                f
            } else {
                val hScore: Int
                val aScore: Int
                if (f.id == userFixtureId && userHomeScore != null && userAwayScore != null) {
                    hScore = userHomeScore
                    aScore = userAwayScore
                } else {
                    val live = liveById[f.id]
                    if (live != null) {
                        hScore = live.homeScore
                        aScore = live.awayScore
                    } else {
                        val hStr = (clubMap[f.homeClubId]?.averageOverall ?: 76) + 2
                        val aStr = clubMap[f.awayClubId]?.averageOverall ?: 76
                        val diff = (hStr - aStr) / 10.0
                        hScore = random.nextDouble(0.0, 2.7 + diff.coerceAtLeast(0.0)).toInt().coerceIn(0, 5)
                        aScore = random.nextDouble(0.0, 2.4 - diff.coerceAtMost(0.0)).toInt().coerceIn(0, 5)
                    }
                }

                val hPts = when {
                    hScore > aScore -> 3
                    hScore == aScore -> 1
                    else -> 0
                }
                val aPts = when {
                    aScore > hScore -> 3
                    hScore == aScore -> 1
                    else -> 0
                }

                standingMap[f.homeClubId]?.let { hs ->
                    standingMap[f.homeClubId] = hs.copy(
                        played = hs.played + 1,
                        wins = hs.wins + if (hPts == 3) 1 else 0,
                        draws = hs.draws + if (hPts == 1) 1 else 0,
                        losses = hs.losses + if (hPts == 0) 1 else 0,
                        goalsFor = hs.goalsFor + hScore,
                        goalsAgainst = hs.goalsAgainst + aScore,
                        points = hs.points + hPts
                    )
                }
                standingMap[f.awayClubId]?.let { aws ->
                    standingMap[f.awayClubId] = aws.copy(
                        played = aws.played + 1,
                        wins = aws.wins + if (aPts == 3) 1 else 0,
                        draws = aws.draws + if (aPts == 1) 1 else 0,
                        losses = aws.losses + if (aPts == 0) 1 else 0,
                        goalsFor = aws.goalsFor + aScore,
                        goalsAgainst = aws.goalsAgainst + hScore,
                        points = aws.points + aPts
                    )
                }

                f.copy(homeScore = hScore, awayScore = aScore, played = true)
            }
        }

        val sortedStandings = sortStandings(standingMap.values.toList(), clubMap)
        if (md < 8) {
            return ucl.copy(
                currentMatchday = md + 1,
                standings = sortedStandings,
                leagueFixtures = updatedFixtures
            )
        }

        // League phase completed -> Build Playoffs (9th..24th -> 8 ties)
        val playoffTeams = sortedStandings.drop(8).take(16)
        val playoffTies = if (playoffTeams.size >= 16) {
            (0 until 8).map { i ->
                val seededHome = playoffTeams[i]          // 9th..16th
                val unseededAway = playoffTeams[15 - i]   // 24th..17th
                CupMatchTie(
                    id = "ucl_po_${seededHome.clubId}_${unseededAway.clubId}",
                    cupType = CupType.CHAMPIONS,
                    stageIndex = 0,
                    stageLabel = "Playoffs (Repescagem)",
                    homeClubId = seededHome.clubId,
                    awayClubId = unseededAway.clubId,
                    triggerAfterLeagueRound = PLAYOFF_TRIGGER_ROUND
                )
            }
        } else emptyList()

        return ucl.copy(
            phase = ChampionsLeaguePhase.PLAYOFFS,
            currentMatchday = 8,
            standings = sortedStandings,
            leagueFixtures = updatedFixtures,
            knockoutTies = playoffTies,
            currentKnockoutStageIndex = 0
        )
    }

    fun resolveKnockoutStage(
        ucl: ChampionsLeagueState,
        userTieId: String?,
        userHomeScore: Int?,
        userAwayScore: Int?,
        userShootout: PenaltyShootoutResult?,
        liveOtherMatches: List<LiveFixtureState> = emptyList(),
        clubMap: Map<String, Club>,
        random: Random = Random.Default
    ): ChampionsLeagueState {
        if (ucl.phase == ChampionsLeaguePhase.LEAGUE_PHASE || ucl.phase == ChampionsLeaguePhase.FINISHED) {
            return ucl
        }
        val stageIdx = ucl.currentKnockoutStageIndex
        val liveById = liveOtherMatches.associateBy { it.fixtureId }

        val updatedTies = ucl.knockoutTies.map { tie ->
            if (tie.stageIndex != stageIdx || tie.winnerClubId != null) {
                tie
            } else if (tie.id == userTieId && userHomeScore != null && userAwayScore != null) {
                val winnerId = when {
                    userHomeScore > userAwayScore -> tie.homeClubId
                    userAwayScore > userHomeScore -> tie.awayClubId
                    else -> userShootout?.winnerClubId ?: tie.homeClubId
                }
                tie.copy(
                    homeScore = userHomeScore,
                    awayScore = userAwayScore,
                    winnerClubId = winnerId,
                    shootout = userShootout
                )
            } else {
                val live = liveById[tie.id]
                val h = clubMap[tie.homeClubId]
                val a = clubMap[tie.awayClubId]
                val hStr = (h?.averageOverall ?: 78).toDouble() + 2.0
                val aStr = (a?.averageOverall ?: 78).toDouble()
                val hScore = live?.homeScore ?: random.nextInt(0, 4)
                val aScore = live?.awayScore ?: random.nextInt(0, 4)
                val shootout = if (hScore == aScore) {
                    CupAndCareerEngine.simulatePenaltyShootout(
                        homeClubId = tie.homeClubId,
                        awayClubId = tie.awayClubId,
                        homeShort = h?.shortName ?: "CAS",
                        awayShort = a?.shortName ?: "VIS",
                        homeStrength = hStr,
                        awayStrength = aStr,
                        random = random
                    )
                } else null
                val winnerId = when {
                    hScore > aScore -> tie.homeClubId
                    aScore > hScore -> tie.awayClubId
                    else -> shootout?.winnerClubId ?: tie.homeClubId
                }
                tie.copy(
                    homeScore = hScore,
                    awayScore = aScore,
                    winnerClubId = winnerId,
                    shootout = shootout
                )
            }
        }

        val currentStageTies = updatedTies.filter { it.stageIndex == stageIdx }
        if (currentStageTies.isEmpty() || currentStageTies.any { it.winnerClubId == null }) {
            return ucl.copy(knockoutTies = updatedTies)
        }

        val winners = currentStageTies.mapNotNull { it.winnerClubId }

        // If Final just concluded
        if (ucl.phase == ChampionsLeaguePhase.FINAL || (stageIdx >= 4 && winners.size == 1)) {
            return ucl.copy(
                phase = ChampionsLeaguePhase.FINISHED,
                knockoutTies = updatedTies,
                championClubId = winners.firstOrNull()
            )
        }

        // Transition from Playoffs (stageIdx 0) -> Round of 16 (stageIdx 1) by pairing Top 8 with 8 Playoff winners
        if (ucl.phase == ChampionsLeaguePhase.PLAYOFFS) {
            val top8 = ucl.standings.take(8).map { it.clubId }
            val r16Ties = (0 until minOf(top8.size, winners.size)).map { i ->
                val seeded = top8[i]
                val unseededWinner = winners[winners.lastIndex - i]
                CupMatchTie(
                    id = "ucl_s1_${seeded}_${unseededWinner}",
                    cupType = CupType.CHAMPIONS,
                    stageIndex = 1,
                    stageLabel = "Oitavas de Final",
                    homeClubId = seeded,
                    awayClubId = unseededWinner,
                    triggerAfterLeagueRound = KNOCKOUT_TRIGGER_ROUNDS[0]
                )
            }
            return ucl.copy(
                phase = ChampionsLeaguePhase.ROUND_OF_16,
                knockoutTies = updatedTies + r16Ties,
                currentKnockoutStageIndex = 1
            )
        }

        // Standard knockout progression: R16 (1) -> QF (2) -> SF (3) -> Final (4)
        val nextStageIdx = stageIdx + 1
        val nextPhase = when (nextStageIdx) {
            2 -> ChampionsLeaguePhase.QUARTER_FINALS
            3 -> ChampionsLeaguePhase.SEMI_FINALS
            4 -> ChampionsLeaguePhase.FINAL
            else -> ChampionsLeaguePhase.FINISHED
        }
        val nextStageLabel = when (nextPhase) {
            ChampionsLeaguePhase.QUARTER_FINALS -> "Quartas de Final"
            ChampionsLeaguePhase.SEMI_FINALS -> "Semifinal"
            ChampionsLeaguePhase.FINAL -> "Grande Final"
            else -> "Final"
        }
        val nextTrigger = KNOCKOUT_TRIGGER_ROUNDS.getOrElse(nextStageIdx - 1) { 31 }
        val nextTies = winners.chunked(2).mapNotNull { pair ->
            if (pair.size < 2) null
            else CupMatchTie(
                id = "ucl_s${nextStageIdx}_${pair[0]}_${pair[1]}",
                cupType = CupType.CHAMPIONS,
                stageIndex = nextStageIdx,
                stageLabel = nextStageLabel,
                homeClubId = pair[0],
                awayClubId = pair[1],
                triggerAfterLeagueRound = nextTrigger
            )
        }

        return ucl.copy(
            phase = nextPhase,
            knockoutTies = updatedTies + nextTies,
            currentKnockoutStageIndex = nextStageIdx
        )
    }

    /**
     * Automatically advances any UCL matchdays or knockout rounds whose trigger league round has passed
     * whenever the user's club is not actively playing in that matchday/tie.
     */
    fun autoAdvanceIdleChampionsLeague(
        ucl: ChampionsLeagueState?,
        clubs: List<Club>,
        userClubId: String,
        completedLeagueRound: Int,
        random: Random = Random.Default
    ): ChampionsLeagueState? {
        if (ucl == null || ucl.phase == ChampionsLeaguePhase.FINISHED) return ucl
        val clubMap = clubs.associateBy { it.id }
        var state: ChampionsLeagueState = ucl

        repeat(12) {
            if (state.phase == ChampionsLeaguePhase.FINISHED) return state
            if (state.phase == ChampionsLeaguePhase.LEAGUE_PHASE) {
                val md = state.currentMatchday
                val mdFixtures = state.leagueFixtures.filter { it.matchday == md && !it.played }
                val trigger = mdFixtures.firstOrNull()?.triggerAfterLeagueRound ?: 99
                val userInMd = mdFixtures.any { it.homeClubId == userClubId || it.awayClubId == userClubId }
                if (!userInMd && mdFixtures.isNotEmpty() && completedLeagueRound >= trigger) {
                    state = resolveLeagueMatchday(
                        ucl = state,
                        userFixtureId = null,
                        userHomeScore = null,
                        userAwayScore = null,
                        clubMap = clubMap,
                        random = random
                    )
                } else {
                    return state
                }
            } else {
                val stageTies = state.knockoutTies.filter {
                    it.stageIndex == state.currentKnockoutStageIndex && it.winnerClubId == null
                }
                val trigger = stageTies.firstOrNull()?.triggerAfterLeagueRound ?: 99
                val userInStage = stageTies.any { it.homeClubId == userClubId || it.awayClubId == userClubId }
                if (!userInStage && stageTies.isNotEmpty() && completedLeagueRound >= trigger) {
                    state = resolveKnockoutStage(
                        ucl = state,
                        userTieId = null,
                        userHomeScore = null,
                        userAwayScore = null,
                        userShootout = null,
                        clubMap = clubMap,
                        random = random
                    )
                } else {
                    return state
                }
            }
        }
        return state
    }

    fun prizeForChampionsStage(phase: ChampionsLeaguePhase, isWin: Boolean, isChampion: Boolean): Long {
        return when {
            isChampion -> 28_000_000L
            phase == ChampionsLeaguePhase.LEAGUE_PHASE -> if (isWin) 2_100_000L else 700_000L
            phase == ChampionsLeaguePhase.PLAYOFFS -> 3_500_000L
            phase == ChampionsLeaguePhase.ROUND_OF_16 -> 6_500_000L
            phase == ChampionsLeaguePhase.QUARTER_FINALS -> 9_000_000L
            phase == ChampionsLeaguePhase.SEMI_FINALS -> 12_500_000L
            else -> 6_000_000L
        }
    }
}
