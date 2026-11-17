package com.example.domain.engine

import com.example.domain.model.Club
import com.example.domain.model.ClubGovernanceState
import com.example.domain.model.ContinentalCupType
import com.example.domain.model.ContinentalGroupFixture
import com.example.domain.model.ContinentalGroupStanding
import com.example.domain.model.ContinentalPhase
import com.example.domain.model.ContinentalTournamentState
import com.example.domain.model.CupMatchTie
import com.example.domain.model.CupType
import com.example.domain.model.Division
import com.example.domain.model.FairPlayState
import com.example.domain.model.FfpComplianceLevel
import com.example.domain.model.LiveFixtureState
import com.example.domain.model.LiveMatchState
import com.example.domain.model.MatchEvent
import com.example.domain.model.MatchEventType
import com.example.domain.model.MonthlyLedgerRecord
import com.example.domain.model.PenaltyShootoutResult
import com.example.domain.model.PresidentialCandidate
import com.example.domain.model.TicketPriceLevel
import kotlin.random.Random

object ContinentalGroupEngine {

    private val GROUP_NAMES = listOf("A", "B", "C", "D", "E", "F", "G", "H")
    private val GROUP_TRIGGER_ROUNDS = listOf(3, 6, 9, 12, 15, 18)
    private val KNOCKOUT_TRIGGER_ROUNDS = listOf(21, 24, 27, 30)

    fun buildInitialContinentalTournaments(
        userClub: Club,
        allClubs: List<Club>
    ): List<ContinentalTournamentState> {
        val southAmerica = allClubs.filter {
            it.division in setOf(
                Division.SERIE_A,
                Division.LIGA_ARGENTINA,
                Division.SERIE_B,
                Division.SERIE_C,
                Division.SERIE_D
            )
        }.sortedByDescending { it.prestige * 20 + it.averageOverall }

        val userEligibleLib = userClub.division == Division.SERIE_A || userClub.division == Division.LIGA_ARGENTINA
        val userEligibleSula = !userEligibleLib &&
            userClub.division in setOf(Division.SERIE_B, Division.SERIE_C)

        val libClubs = if (userEligibleLib) {
            val others = southAmerica.filter { it.id != userClub.id }.take(31)
            (others + userClub).distinctBy { it.id }.take(32)
        } else {
            southAmerica.take(32)
        }

        val libIds = libClubs.map { it.id }.toSet()
        val sulaPoolBase = southAmerica.filter { it.id !in libIds }
        val sulaClubs = if (userEligibleSula) {
            val others = sulaPoolBase.filter { it.id != userClub.id }.take(31)
            (others + userClub).distinctBy { it.id }.take(32)
        } else {
            sulaPoolBase.take(32)
        }

        val list = mutableListOf<ContinentalTournamentState>()
        if (libClubs.size >= 32) list.add(createGroupTournament(ContinentalCupType.LIBERTADORES, libClubs))
        if (sulaClubs.size >= 32) list.add(createGroupTournament(ContinentalCupType.SUL_AMERICANA, sulaClubs))
        return list
    }

    private fun createGroupTournament(
        cupType: ContinentalCupType,
        participants: List<Club>
    ): ContinentalTournamentState {
        val clubCount = (participants.size / 4) * 4
        val groups = participants.take(clubCount).chunked(4)
        val standings = mutableListOf<ContinentalGroupStanding>()
        val fixtures = mutableListOf<ContinentalGroupFixture>()

        groups.forEachIndexed { gIdx, groupClubs ->
            val gName = GROUP_NAMES.getOrElse(gIdx) { "G${gIdx + 1}" }
            groupClubs.forEach { c ->
                standings.add(ContinentalGroupStanding(clubId = c.id, groupName = gName))
            }
            if (groupClubs.size == 4) {
                val a = groupClubs[0].id
                val b = groupClubs[1].id
                val c = groupClubs[2].id
                val d = groupClubs[3].id
                val schedule = listOf(
                    1 to listOf(a to b, c to d),
                    2 to listOf(b to c, d to a),
                    3 to listOf(a to c, b to d),
                    4 to listOf(b to a, d to c),
                    5 to listOf(c to b, a to d),
                    6 to listOf(c to a, d to b)
                )
                schedule.forEach { (md, pairs) ->
                    val trigger = GROUP_TRIGGER_ROUNDS[md - 1]
                    pairs.forEach { (h, aw) ->
                        fixtures.add(
                            ContinentalGroupFixture(
                                id = "${cupType.name}_g${gName}_m${md}_${h}_${aw}",
                                cupType = cupType,
                                groupName = gName,
                                matchday = md,
                                homeClubId = h,
                                awayClubId = aw,
                                triggerAfterLeagueRound = trigger
                            )
                        )
                    }
                }
            }
        }

        return ContinentalTournamentState(
            cupType = cupType,
            phase = ContinentalPhase.GROUP_STAGE,
            currentMatchday = 1,
            standings = standings,
            groupFixtures = fixtures,
            knockoutTies = emptyList(),
            currentKnockoutStageIndex = 0,
            championClubId = null
        )
    }

    fun sortGroupStandings(
        standings: List<ContinentalGroupStanding>,
        groupName: String,
        clubMap: Map<String, Club>
    ): List<ContinentalGroupStanding> {
        return standings.filter { it.groupName == groupName }
            .sortedWith(
                compareByDescending<ContinentalGroupStanding> { it.points }
                    .thenByDescending { it.goalDifference }
                    .thenByDescending { it.goalsFor }
                    .thenByDescending { it.wins }
                    .thenBy { clubMap[it.clubId]?.name ?: it.clubId }
            )
    }

    sealed interface PendingContinentalMatch {
        data class GroupMatch(val tournament: ContinentalTournamentState, val fixture: ContinentalGroupFixture) : PendingContinentalMatch
        data class KnockoutMatch(val tournament: ContinentalTournamentState, val tie: CupMatchTie) : PendingContinentalMatch
    }

    fun findPendingUserMatch(
        tournaments: List<ContinentalTournamentState>,
        userClubId: String,
        completedLeagueRound: Int
    ): PendingContinentalMatch? {
        tournaments.forEach { t ->
            if (t.phase == ContinentalPhase.FINISHED || t.championClubId != null) return@forEach
            if (t.phase == ContinentalPhase.GROUP_STAGE) {
                val f = t.groupFixtures.firstOrNull {
                    !it.played &&
                        it.matchday == t.currentMatchday &&
                        (it.homeClubId == userClubId || it.awayClubId == userClubId) &&
                        completedLeagueRound >= it.triggerAfterLeagueRound
                }
                if (f != null) return PendingContinentalMatch.GroupMatch(t, f)
            } else {
                val tie = t.knockoutTies.firstOrNull {
                    it.winnerClubId == null &&
                        it.stageIndex == t.currentKnockoutStageIndex &&
                        (it.homeClubId == userClubId || it.awayClubId == userClubId) &&
                        completedLeagueRound >= it.triggerAfterLeagueRound
                }
                if (tie != null) return PendingContinentalMatch.KnockoutMatch(t, tie)
            }
        }
        return null
    }

    fun prepareGroupLiveMatch(
        tournament: ContinentalTournamentState,
        fixture: ContinentalGroupFixture,
        userClub: Club,
        allClubs: List<Club>,
        ticketPriceLevel: TicketPriceLevel
    ): LiveMatchState {
        val clubMap = allClubs.associateBy { it.id }
        val homeClub = clubMap[fixture.homeClubId] ?: userClub
        val awayClub = clubMap[fixture.awayClubId] ?: userClub
        val userIsHome = homeClub.id == userClub.id

        val fillRatio = (0.76 + (homeClub.prestige * 0.05)) * ticketPriceLevel.attendanceFactor
        val attendance = (homeClub.stadiumCapacity * fillRatio.coerceIn(0.45, 1.0)).toInt()
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

        val others = tournament.groupFixtures.filter {
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
            description = "${tournament.cupType.title} • Grupo ${fixture.groupName} (Rod. ${fixture.matchday}/6) em ${homeClub.stadiumName}!"
        )

        return LiveMatchState(
            round = fixture.triggerAfterLeagueRound,
            competition = tournament.cupType.competition,
            minute = 0,
            phase = "1º Tempo",
            isPaused = false,
            isFinished = false,
            speed = 2,
            userIsHome = userIsHome,
            userMatch = userLive,
            otherMatches = others,
            events = listOf(kickoff),
            possessionHome = 50,
            attendance = attendance,
            ticketRevenue = revenue,
            isKnockout = false,
            cupStageLabel = "Grupo ${fixture.groupName} • J${fixture.matchday}/6",
            ballZone = 0.5f,
            continentalFixtureId = fixture.id,
            continentalCupType = tournament.cupType
        )
    }

    fun prepareKnockoutLiveMatch(
        tournament: ContinentalTournamentState,
        tie: CupMatchTie,
        userClub: Club,
        allClubs: List<Club>,
        ticketPriceLevel: TicketPriceLevel
    ): LiveMatchState {
        val clubMap = allClubs.associateBy { it.id }
        val homeClub = clubMap[tie.homeClubId] ?: userClub
        val awayClub = clubMap[tie.awayClubId] ?: userClub
        val userIsHome = homeClub.id == userClub.id

        val fillRatio = (0.80 + (homeClub.prestige * 0.04)) * ticketPriceLevel.attendanceFactor
        val attendance = (homeClub.stadiumCapacity * fillRatio.coerceIn(0.50, 1.0)).toInt()
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

        val otherTies = tournament.knockoutTies.filter {
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
            description = "${tournament.cupType.title} (${tie.stageLabel}) em ${homeClub.stadiumName}!"
        )

        return LiveMatchState(
            round = tie.triggerAfterLeagueRound,
            competition = tournament.cupType.competition,
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
            cupType = if (tournament.cupType == ContinentalCupType.LIBERTADORES) CupType.LIBERTADORES else null,
            cupTieId = tie.id,
            cupStageLabel = tie.stageLabel,
            ballZone = 0.5f,
            continentalCupType = tournament.cupType
        )
    }

    fun resolveGroupMatchday(
        tournament: ContinentalTournamentState,
        userFixtureId: String?,
        userHomeScore: Int?,
        userAwayScore: Int?,
        liveOtherMatches: List<LiveFixtureState> = emptyList(),
        clubMap: Map<String, Club>,
        random: Random = Random.Default
    ): ContinentalTournamentState {
        if (tournament.phase != ContinentalPhase.GROUP_STAGE) return tournament
        val md = tournament.currentMatchday
        val liveById = liveOtherMatches.associateBy { it.fixtureId }
        val standingMap = tournament.standings.associateBy { it.clubId }.toMutableMap()

        val updatedFixtures = tournament.groupFixtures.map { f ->
            if (f.matchday != md || f.played) f
            else {
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
                        val hStr = (clubMap[f.homeClubId]?.averageOverall ?: 72) + 2
                        val aStr = clubMap[f.awayClubId]?.averageOverall ?: 72
                        val diff = (hStr - aStr) / 10.0
                        hScore = random.nextDouble(0.0, 2.6 + diff.coerceAtLeast(0.0)).toInt().coerceIn(0, 5)
                        aScore = random.nextDouble(0.0, 2.2 - diff.coerceAtMost(0.0)).toInt().coerceIn(0, 4)
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

        val updatedStandings = standingMap.values.toList()
        if (md < 6) {
            return tournament.copy(
                currentMatchday = md + 1,
                standings = updatedStandings,
                groupFixtures = updatedFixtures
            )
        }

        // Group stage finished -> Seed Top 2 of each group into Round of 16
        val groupNames = updatedStandings.map { it.groupName }.distinct().sorted()
        val winners = mutableListOf<String>()
        val runnersUp = mutableListOf<String>()
        groupNames.forEach { g ->
            val sorted = sortGroupStandings(updatedStandings, g, clubMap)
            if (sorted.size >= 2) {
                winners.add(sorted[0].clubId)
                runnersUp.add(sorted[1].clubId)
            }
        }
        val r16Ties = winners.indices.map { i ->
            val h = winners[i]
            val a = runnersUp[(i + 1) % runnersUp.size]
            CupMatchTie(
                id = "${tournament.cupType.name}_s0_${h}_${a}",
                cupType = if (tournament.cupType == ContinentalCupType.LIBERTADORES) CupType.LIBERTADORES else CupType.COPA_DO_BRASIL,
                stageIndex = 0,
                stageLabel = "Oitavas de Final",
                homeClubId = h,
                awayClubId = a,
                triggerAfterLeagueRound = KNOCKOUT_TRIGGER_ROUNDS[0]
            )
        }

        return tournament.copy(
            phase = ContinentalPhase.ROUND_OF_16,
            currentMatchday = 6,
            standings = updatedStandings,
            groupFixtures = updatedFixtures,
            knockoutTies = r16Ties,
            currentKnockoutStageIndex = 0
        )
    }

    fun resolveKnockoutStage(
        tournament: ContinentalTournamentState,
        userTieId: String?,
        userHomeScore: Int?,
        userAwayScore: Int?,
        userShootout: PenaltyShootoutResult?,
        liveOtherMatches: List<LiveFixtureState> = emptyList(),
        clubMap: Map<String, Club>,
        random: Random = Random.Default
    ): ContinentalTournamentState {
        if (tournament.phase == ContinentalPhase.GROUP_STAGE || tournament.phase == ContinentalPhase.FINISHED) return tournament
        val stageIdx = tournament.currentKnockoutStageIndex
        val liveById = liveOtherMatches.associateBy { it.fixtureId }

        val updatedTies = tournament.knockoutTies.map { tie ->
            if (tie.stageIndex != stageIdx || tie.winnerClubId != null) tie
            else if (tie.id == userTieId && userHomeScore != null && userAwayScore != null) {
                val winnerId = when {
                    userHomeScore > userAwayScore -> tie.homeClubId
                    userAwayScore > userHomeScore -> tie.awayClubId
                    else -> userShootout?.winnerClubId ?: tie.homeClubId
                }
                tie.copy(homeScore = userHomeScore, awayScore = userAwayScore, winnerClubId = winnerId, shootout = userShootout)
            } else {
                val live = liveById[tie.id]
                val h = clubMap[tie.homeClubId]
                val a = clubMap[tie.awayClubId]
                val hScore = live?.homeScore ?: random.nextInt(0, 4)
                val aScore = live?.awayScore ?: random.nextInt(0, 4)
                val shootout = if (hScore == aScore) {
                    CupAndCareerEngine.simulatePenaltyShootout(
                        homeClubId = tie.homeClubId,
                        awayClubId = tie.awayClubId,
                        homeShort = h?.shortName ?: "CAS",
                        awayShort = a?.shortName ?: "VIS",
                        homeStrength = (h?.averageOverall ?: 72).toDouble() + 2.0,
                        awayStrength = (a?.averageOverall ?: 72).toDouble(),
                        random = random
                    )
                } else null
                val winnerId = when {
                    hScore > aScore -> tie.homeClubId
                    aScore > hScore -> tie.awayClubId
                    else -> shootout?.winnerClubId ?: tie.homeClubId
                }
                tie.copy(homeScore = hScore, awayScore = aScore, winnerClubId = winnerId, shootout = shootout)
            }
        }

        val currentStageTies = updatedTies.filter { it.stageIndex == stageIdx }
        if (currentStageTies.isEmpty() || currentStageTies.any { it.winnerClubId == null }) {
            return tournament.copy(knockoutTies = updatedTies)
        }
        val winners = currentStageTies.mapNotNull { it.winnerClubId }
        if (tournament.phase == ContinentalPhase.FINAL || winners.size <= 1) {
            return tournament.copy(
                phase = ContinentalPhase.FINISHED,
                knockoutTies = updatedTies,
                championClubId = winners.firstOrNull()
            )
        }
        val nextStageIdx = stageIdx + 1
        val nextPhase = when (nextStageIdx) {
            1 -> ContinentalPhase.QUARTER_FINALS
            2 -> ContinentalPhase.SEMI_FINALS
            3 -> ContinentalPhase.FINAL
            else -> ContinentalPhase.FINISHED
        }
        val nextStageLabel = when (nextPhase) {
            ContinentalPhase.QUARTER_FINALS -> "Quartas de Final"
            ContinentalPhase.SEMI_FINALS -> "Semifinal"
            ContinentalPhase.FINAL -> "Grande Final"
            else -> "Final"
        }
        val nextTrigger = KNOCKOUT_TRIGGER_ROUNDS.getOrElse(nextStageIdx) { 30 }
        val nextTies = winners.chunked(2).mapNotNull { pair ->
            if (pair.size < 2) null
            else CupMatchTie(
                id = "${tournament.cupType.name}_s${nextStageIdx}_${pair[0]}_${pair[1]}",
                cupType = if (tournament.cupType == ContinentalCupType.LIBERTADORES) CupType.LIBERTADORES else CupType.COPA_DO_BRASIL,
                stageIndex = nextStageIdx,
                stageLabel = nextStageLabel,
                homeClubId = pair[0],
                awayClubId = pair[1],
                triggerAfterLeagueRound = nextTrigger
            )
        }
        return tournament.copy(
            phase = nextPhase,
            knockoutTies = updatedTies + nextTies,
            currentKnockoutStageIndex = nextStageIdx
        )
    }

    fun autoAdvanceIdleTournaments(
        tournaments: List<ContinentalTournamentState>,
        clubs: List<Club>,
        userClubId: String,
        completedLeagueRound: Int,
        random: Random = Random.Default
    ): List<ContinentalTournamentState> {
        val clubMap = clubs.associateBy { it.id }
        return tournaments.map { t ->
            var cur = t
            repeat(10) {
                if (cur.phase == ContinentalPhase.FINISHED) return@map cur
                if (cur.phase == ContinentalPhase.GROUP_STAGE) {
                    val mdFixtures = cur.groupFixtures.filter { it.matchday == cur.currentMatchday && !it.played }
                    val trigger = mdFixtures.firstOrNull()?.triggerAfterLeagueRound ?: 99
                    val userInMd = mdFixtures.any { it.homeClubId == userClubId || it.awayClubId == userClubId }
                    if (!userInMd && mdFixtures.isNotEmpty() && completedLeagueRound >= trigger) {
                        cur = resolveGroupMatchday(cur, null, null, null, emptyList(), clubMap, random)
                    } else {
                        return@map cur
                    }
                } else {
                    val stageTies = cur.knockoutTies.filter {
                        it.stageIndex == cur.currentKnockoutStageIndex && it.winnerClubId == null
                    }
                    val trigger = stageTies.firstOrNull()?.triggerAfterLeagueRound ?: 99
                    val userInStage = stageTies.any { it.homeClubId == userClubId || it.awayClubId == userClubId }
                    if (!userInStage && stageTies.isNotEmpty() && completedLeagueRound >= trigger) {
                        cur = resolveKnockoutStage(cur, null, null, null, null, emptyList(), clubMap, random)
                    } else {
                        return@map cur
                    }
                }
            }
            cur
        }
    }
}

object GovernanceAndFfpEngine {

    private val PRESIDENTS = listOf(
        "Roberto Andrade" to "Equilibrado",
        "Marcelo Vasconcelos" to "Investidor Arrojado",
        "Henrique Bittencourt" to "Austero / FFP"
    )

    const val ELECTION_CYCLE_YEARS = 4L

    fun buildInitialGovernance(club: Club, seasonYear: Long = 2026L): ClubGovernanceState {
        val objMaxPos = when {
            club.division == Division.SERIE_D || club.division == Division.SERIE_C -> if (club.prestige >= 3) 4 else 8
            club.prestige >= 5 -> 4
            club.prestige == 4 -> 6
            club.prestige == 3 -> 10
            else -> 14
        }
        val objLabel = when {
            club.division.promotionSpots > 0 && objMaxPos <= club.division.promotionSpots ->
                "Conquistar acesso (Terminar no G${objMaxPos})"
            objMaxPos <= 4 -> "Classificação direta / G4 na ${club.division.shortLabel}"
            else -> "Terminar até a ${objMaxPos}ª posição na ${club.division.shortLabel}"
        }
        val picked = PRESIDENTS[(club.id.hashCode().let { if (it < 0) -it else it }) % PRESIDENTS.size]
        return ClubGovernanceState(
            presidentName = picked.first,
            presidentStyle = picked.second,
            councilRating = 74,
            councilVerdict = "Conselho otimista com o projeto esportivo e orçamento do clube.",
            seasonObjectiveLabel = objLabel,
            seasonObjectiveMaxPos = objMaxPos,
            objectiveFailedPunished = false,
            electionPending = false,
            nextElectionYear = seasonYear + ELECTION_CYCLE_YEARS,
            lastElectionYear = null,
            electionCandidates = generateCandidates(club)
        )
    }

    fun generateCandidates(club: Club): List<PresidentialCandidate> {
        val baseGrant = when {
            club.prestige >= 4 -> 8_000_000L
            club.prestige == 3 -> 4_500_000L
            else -> 2_200_000L
        }
        return listOf(
            PresidentialCandidate(
                id = "cand_situacao",
                name = "Eduardo Monteiro (Situação)",
                platform = "Continuidade da comissão técnica + bônus equilibrado de caixa.",
                styleLabel = "Estabilidade",
                budgetGrant = baseGrant,
                boardStrictness = 2,
                projectedVoteShare = 38
            ),
            PresidentialCandidate(
                id = "cand_invest",
                name = "Ricardo Magalhães (Chapa Forte)",
                platform = "Injeção agressiva para reforços e cobrança rígida por G4.",
                styleLabel = "Investidor",
                budgetGrant = baseGrant * 2L,
                boardStrictness = 3,
                projectedVoteShare = 34
            ),
            PresidentialCandidate(
                id = "cand_base",
                name = "Dra. Helena Salles (Renovação)",
                platform = "Prioridade total ao Fair Play Financeiro, base Sub-20 e metas realistas.",
                styleLabel = "Fair Play & Base",
                budgetGrant = (baseGrant * 0.8).toLong(),
                boardStrictness = 1,
                projectedVoteShare = 28
            )
        )
    }

    fun resolveElection(
        governance: ClubGovernanceState,
        supportedCandidateId: String,
        club: Club,
        currentSeasonYear: Long = 2026L
    ): Pair<ClubGovernanceState, Long> {
        if (!governance.electionPending && currentSeasonYear < governance.nextElectionYear) {
            return governance to 0L
        }
        val candidates = governance.electionCandidates.ifEmpty { generateCandidates(club) }
        val chosen = candidates.firstOrNull { it.id == supportedCandidateId } ?: candidates.first()
        val strictObj = when (chosen.boardStrictness) {
            3 -> (governance.seasonObjectiveMaxPos - 2).coerceAtLeast(2)
            1 -> (governance.seasonObjectiveMaxPos + 2).coerceAtMost(14)
            else -> governance.seasonObjectiveMaxPos
        }
        val nextCycleYear = currentSeasonYear + ELECTION_CYCLE_YEARS
        val newGov = governance.copy(
            presidentName = chosen.name.substringBefore(" ("),
            presidentStyle = chosen.styleLabel,
            councilRating = (governance.councilRating + 8).coerceIn(40, 96),
            councilVerdict = "Novo presidente empossado (mandato até $nextCycleYear)! Meta ajustada para Top $strictObj.",
            seasonObjectiveMaxPos = strictObj,
            seasonObjectiveLabel = "Terminar até a ${strictObj}ª posição na ${club.division.shortLabel}",
            electionPending = false,
            lastElectionYear = currentSeasonYear,
            nextElectionYear = nextCycleYear,
            lastElectionSummary = "${chosen.name} venceu a eleição de $currentSeasonYear (mandato 4 anos) e injetou ${com.example.ui.components.formatCurrency(chosen.budgetGrant)}."
        )
        return newGov to chosen.budgetGrant
    }

    data class FfpEvaluationOutcome(
        val fairPlay: FairPlayState,
        val governance: ClubGovernanceState,
        val updatedBudget: Long,
        val pointsPenaltyApplied: Int,
        val confidenceDelta: Long,
        val punishmentHeadline: String?
    )

    fun evaluateRoundCompliance(
        currentFfp: FairPlayState,
        currentGov: ClubGovernanceState,
        club: Club,
        userPosition: Int,
        monthlyLedger: Map<Int, MonthlyLedgerRecord>,
        completedRound: Int,
        isSeasonEnd: Boolean,
        seasonYear: Long = 2026L
    ): FfpEvaluationOutcome {
        val totalTicketAndOther = monthlyLedger.values.sumOf { it.ticketIncome + it.otherDelta.coerceAtLeast(0L) } + 1_000_000L
        val totalSalaries = monthlyLedger.values.sumOf { it.salaryExpense } + (club.weeklyWages / 4L)
        val totalOut = monthlyLedger.values.sumOf { it.salaryExpense + (-it.otherDelta).coerceAtLeast(0L) }
        val netResult = monthlyLedger.values.sumOf { it.ticketIncome + it.otherDelta - it.salaryExpense }
        val wagePct = if (totalTicketAndOther > 0L) ((totalSalaries * 100L) / totalTicketAndOther).toInt().coerceIn(10, 180) else 65
        val limit = (club.prestige * 5_000_000L).coerceAtLeast(8_000_000L)

        val decrementedBan = (currentFfp.transferBanRounds - 1).coerceAtLeast(0)
        var newBan = decrementedBan
        var pointsPenalty = 0
        var fine = 0L
        var note: String? = if (decrementedBan > 0) "Bloqueio FIFA de compras ativo por $decrementedBan rod." else null
        var confDelta = 0L

        val isPeriodicAudit = completedRound > 0 && completedRound % 6 == 0
        val severeBreach = club.budget < -4_000_000L || netResult < -limit
        val warnBreach = !severeBreach && (club.budget < 500_000L || wagePct > 90 || netResult < -(limit / 2))

        if (isPeriodicAudit && severeBreach) {
            newBan = maxOf(newBan, 4)
            fine = 1_500_000L
            pointsPenalty = if (club.budget < -10_000_000L) 3 else 0
            confDelta -= 8L
            note = "Punição FIFA Fair Play: bloqueio de contratações (4 rod.)" +
                if (pointsPenalty > 0) " e -$pointsPenalty pts por déficit crítico!" else "!"
        }

        // End-of-season sporting objective check & FIFA/Council punishment
        var objFailed = currentGov.objectiveFailedPunished
        if (isSeasonEnd && userPosition > currentGov.seasonObjectiveMaxPos + 4 && !objFailed) {
            objFailed = true
            newBan = maxOf(newBan, 3)
            pointsPenalty += 0
            confDelta -= 18L
            val finePct = (club.budget * 0.08).toLong().coerceIn(500_000L, 4_000_000L)
            fine += finePct
            note = "Meta não atingida (${userPosition}º vs meta G${currentGov.seasonObjectiveMaxPos}): multa contratual e bloqueio de compras (3 rod.)!"
        }

        val compliance = when {
            newBan > 0 || severeBreach -> FfpComplianceLevel.BREACH
            warnBreach -> FfpComplianceLevel.WARNING
            else -> FfpComplianceLevel.COMPLIANT
        }

        val posScore = (55 - (userPosition - currentGov.seasonObjectiveMaxPos) * 4).coerceIn(15, 55)
        val finScore = when (compliance) {
            FfpComplianceLevel.COMPLIANT -> 40
            FfpComplianceLevel.WARNING -> 25
            FfpComplianceLevel.BREACH -> 10
        }
        val newCouncilRating = (posScore + finScore).coerceIn(12, 99)
        val verdict = when {
            newCouncilRating >= 80 -> "Conselho satisfeito: meta esportiva e Fair Play em dia."
            newCouncilRating >= 55 -> "Conselho cobra atenção ao desempenho e controle da folha."
            else -> "Conselho insatisfeito! Risco de sanções e troca de comando."
        }

        // Presidential elections occur only every 4 years
        val openElection = currentGov.electionPending ||
            (seasonYear >= currentGov.nextElectionYear && currentGov.lastElectionYear != seasonYear)

        val updatedFfp = currentFfp.copy(
            compliance = compliance,
            seasonNetResult = netResult,
            wageToIncomeRatioPct = wagePct,
            allowedDeficitLimit = limit,
            transferBanRounds = newBan,
            pointsDeducted = currentFfp.pointsDeducted + pointsPenalty,
            totalFinesPaid = currentFfp.totalFinesPaid + fine,
            activePunishmentNote = note
        )
        val updatedGov = currentGov.copy(
            councilRating = newCouncilRating,
            councilVerdict = verdict,
            objectiveFailedPunished = objFailed,
            electionPending = openElection,
            electionCandidates = if (currentGov.electionCandidates.isEmpty()) generateCandidates(club) else currentGov.electionCandidates
        )
        return FfpEvaluationOutcome(
            fairPlay = updatedFfp,
            governance = updatedGov,
            updatedBudget = club.budget - fine,
            pointsPenaltyApplied = pointsPenalty,
            confidenceDelta = confDelta,
            punishmentHeadline = if (fine > 0L || pointsPenalty > 0) note else null
        )
    }
}
