package com.example.domain.engine

import com.example.domain.model.Club
import com.example.domain.model.CupCompetitionState
import com.example.domain.model.CupMatchTie
import com.example.domain.model.CupType
import com.example.domain.model.Division
import com.example.domain.model.LiveFixtureState
import com.example.domain.model.LiveMatchState
import com.example.domain.model.ManagerJobOffer
import com.example.domain.model.MatchEvent
import com.example.domain.model.MatchEventType
import com.example.domain.model.PenaltyShootoutResult
import com.example.domain.model.Player
import com.example.domain.model.PlayerPosition
import com.example.domain.model.PlayerTrait
import com.example.domain.model.SeasonAwardSummary
import com.example.domain.model.SponsorDeal
import com.example.domain.model.TicketPriceLevel
import com.example.domain.model.TrainingFocus
import com.example.domain.model.YouthProspect
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.random.Random

object CupAndCareerEngine {

    private val STAGE_TRIGGER_ROUNDS = listOf(6, 13, 20, 27)

    fun buildInitialCups(userClub: Club, allClubs: List<Club>): List<CupCompetitionState> {
        val cups = mutableListOf<CupCompetitionState>()

        if (userClub.division.country == "Brasil") {
            val brPool = allClubs
                .filter { it.division.country == "Brasil" && it.id != userClub.id }
                .sortedByDescending { it.averageOverall }
                .take(15)
                .toMutableList()
            brPool.add(userClub)
            cups.add(createBracket(CupType.COPA_DO_BRASIL, brPool))

            if (userClub.division == Division.SERIE_A || userClub.prestige >= 3) {
                val libPool = allClubs
                    .filter {
                        it.id != userClub.id &&
                            (it.division == Division.SERIE_A || it.division == Division.LIGA_ARGENTINA)
                    }
                    .sortedByDescending { it.averageOverall }
                    .take(15)
                    .toMutableList()
                libPool.add(userClub)
                cups.add(createBracket(CupType.LIBERTADORES, libPool))
            }
        } else if (userClub.division == Division.LIGA_ARGENTINA) {
            val libPool = allClubs
                .filter {
                    it.id != userClub.id &&
                        (it.division == Division.SERIE_A || it.division == Division.LIGA_ARGENTINA)
                }
                .sortedByDescending { it.averageOverall }
                .take(15)
                .toMutableList()
            libPool.add(userClub)
            cups.add(createBracket(CupType.LIBERTADORES, libPool))
        } else {
            val euroDivs = setOf(
                Division.PREMIER_LEAGUE,
                Division.LA_LIGA,
                Division.SERIE_A_ITA,
                Division.BUNDESLIGA,
                Division.LIGUE_1
            )
            val uclPool = allClubs
                .filter { it.id != userClub.id && it.division in euroDivs }
                .sortedByDescending { it.averageOverall }
                .take(15)
                .toMutableList()
            uclPool.add(userClub)
            cups.add(createBracket(CupType.CHAMPIONS, uclPool))
        }

        return cups
    }

    private fun createBracket(cupType: CupType, participants: List<Club>): CupCompetitionState {
        val seeded = participants.distinctBy { it.id }.take(16).shuffled()
        val triggerRound = STAGE_TRIGGER_ROUNDS.first()
        val ties = seeded.chunked(2).mapNotNull { pair ->
            if (pair.size < 2) null
            else CupMatchTie(
                id = "${cupType.name}_s0_${pair[0].id}_${pair[1].id}",
                cupType = cupType,
                stageIndex = 0,
                stageLabel = cupType.stageNames[0],
                homeClubId = pair[0].id,
                awayClubId = pair[1].id,
                triggerAfterLeagueRound = triggerRound
            )
        }
        return CupCompetitionState(
            cupType = cupType,
            currentStageIndex = 0,
            championClubId = null,
            ties = ties
        )
    }

    /**
     * Finds the next unplayed knockout cup match for the user that is unlocked at or before [completedLeagueRound].
     */
    fun findPendingUserCupTie(
        cups: List<CupCompetitionState>,
        userClubId: String,
        completedLeagueRound: Int
    ): CupMatchTie? {
        return cups.asSequence()
            .filter { it.championClubId == null }
            .flatMap { it.ties.asSequence() }
            .firstOrNull { tie ->
                tie.winnerClubId == null &&
                    (tie.homeClubId == userClubId || tie.awayClubId == userClubId) &&
                    completedLeagueRound >= tie.triggerAfterLeagueRound
            }
    }

    /**
     * Auto-simulates any unplayed AI-only cup ties whose trigger round passed when the user was already eliminated.
     */
    fun autoAdvanceIdleCups(
        cups: List<CupCompetitionState>,
        clubs: List<Club>,
        userClubId: String,
        completedLeagueRound: Int
    ): List<CupCompetitionState> {
        val clubMap = clubs.associateBy { it.id }
        return cups.map { cup ->
            if (cup.championClubId != null) return@map cup
            val stageTies = cup.ties.filter { it.stageIndex == cup.currentStageIndex }
            val userInStage = stageTies.any { it.homeClubId == userClubId || it.awayClubId == userClubId }
            val trigger = stageTies.firstOrNull()?.triggerAfterLeagueRound ?: 99
            if (!userInStage && completedLeagueRound >= trigger && stageTies.any { it.winnerClubId == null }) {
                resolveCupStageWithResults(
                    cup = cup,
                    userTieId = null,
                    userHomeScore = null,
                    userAwayScore = null,
                    userShootout = null,
                    clubMap = clubMap
                )
            } else {
                cup
            }
        }
    }

    fun prepareCupLiveMatch(
        tie: CupMatchTie,
        userClub: Club,
        allClubs: List<Club>,
        cup: CupCompetitionState,
        ticketPriceLevel: TicketPriceLevel
    ): LiveMatchState {
        val clubMap = allClubs.associateBy { it.id }
        val homeClub = clubMap[tie.homeClubId] ?: userClub
        val awayClub = clubMap[tie.awayClubId] ?: userClub
        val userIsHome = homeClub.id == userClub.id

        val fillRatio = (0.72 + (homeClub.prestige * 0.05)) * ticketPriceLevel.attendanceFactor
        val attendance = (homeClub.stadiumCapacity * fillRatio.coerceIn(0.45, 1.0)).toInt()
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

        val otherTies = cup.ties.filter {
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
            description = "${tie.cupType.title} (${tie.stageLabel})! Bola rolando em ${homeClub.stadiumName}!"
        )

        return LiveMatchState(
            round = tie.triggerAfterLeagueRound,
            competition = tie.cupType.competition,
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
            cupType = tie.cupType,
            cupTieId = tie.id,
            cupStageLabel = tie.stageLabel,
            ballZone = 0.5f
        )
    }

    fun simulatePenaltyShootout(
        homeClubId: String,
        awayClubId: String,
        homeShort: String,
        awayShort: String,
        homeStrength: Double,
        awayStrength: Double,
        random: Random = Random.Default
    ): PenaltyShootoutResult {
        var hPens = 0
        var aPens = 0
        val hAcc = (0.72 + (homeStrength - 70.0) * 0.004).coerceIn(0.60, 0.88)
        val aAcc = (0.72 + (awayStrength - 70.0) * 0.004).coerceIn(0.60, 0.88)

        for (kick in 1..5) {
            if (random.nextDouble() < hAcc) hPens++
            val remainingHome = 5 - kick
            val remainingAway = 5 - kick + 1
            if (hPens > aPens + remainingAway || aPens > hPens + remainingHome) break

            if (random.nextDouble() < aAcc) aPens++
            val remAfterA = 5 - kick
            if (hPens > aPens + remAfterA || aPens > hPens + remAfterA) break
        }

        // Sudden death if still tied
        var suddenRounds = 0
        while (hPens == aPens && suddenRounds < 10) {
            suddenRounds++
            val hScored = random.nextDouble() < hAcc
            val aScored = random.nextDouble() < aAcc
            if (hScored) hPens++
            if (aScored) aPens++
        }
        if (hPens == aPens) {
            if (random.nextBoolean()) hPens++ else aPens++
        }

        val winnerId = if (hPens > aPens) homeClubId else awayClubId
        val winnerShort = if (hPens > aPens) homeShort else awayShort
        return PenaltyShootoutResult(
            homePenalties = hPens,
            awayPenalties = aPens,
            winnerClubId = winnerId,
            summary = "Pênaltis: $homeShort $hPens x $aPens $awayShort ($winnerShort avançou!)"
        )
    }

    fun resolveCupStageWithResults(
        cup: CupCompetitionState,
        userTieId: String?,
        userHomeScore: Int?,
        userAwayScore: Int?,
        userShootout: PenaltyShootoutResult?,
        liveOtherMatches: List<LiveFixtureState> = emptyList(),
        clubMap: Map<String, Club>,
        random: Random = Random.Default
    ): CupCompetitionState {
        val liveById = liveOtherMatches.associateBy { it.fixtureId }
        val updatedTies = cup.ties.map { tie ->
            if (tie.stageIndex != cup.currentStageIndex || tie.winnerClubId != null) {
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
                val hStr = (h?.averageOverall ?: 72).toDouble() + 2.0
                val aStr = (a?.averageOverall ?: 72).toDouble()
                val hScore = live?.homeScore ?: random.nextInt(0, 4)
                val aScore = live?.awayScore ?: random.nextInt(0, 4)
                val shootout = if (hScore == aScore) {
                    simulatePenaltyShootout(
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

        val currentStageTies = updatedTies.filter { it.stageIndex == cup.currentStageIndex }
        val stageDone = currentStageTies.isNotEmpty() && currentStageTies.all { it.winnerClubId != null }
        if (!stageDone) {
            return cup.copy(ties = updatedTies)
        }

        val winners = currentStageTies.mapNotNull { it.winnerClubId }
        val isFinalStage = cup.currentStageIndex >= cup.cupType.stageNames.lastIndex || winners.size <= 1
        if (isFinalStage) {
            return cup.copy(
                championClubId = winners.firstOrNull(),
                ties = updatedTies
            )
        }

        val nextStageIdx = cup.currentStageIndex + 1
        val nextTrigger = STAGE_TRIGGER_ROUNDS.getOrElse(nextStageIdx) { 27 }
        val nextTies = winners.chunked(2).mapNotNull { pair ->
            if (pair.size < 2) null
            else CupMatchTie(
                id = "${cup.cupType.name}_s${nextStageIdx}_${pair[0]}_${pair[1]}",
                cupType = cup.cupType,
                stageIndex = nextStageIdx,
                stageLabel = cup.cupType.stageNames[nextStageIdx],
                homeClubId = pair[0],
                awayClubId = pair[1],
                triggerAfterLeagueRound = nextTrigger
            )
        }

        return cup.copy(
            currentStageIndex = nextStageIdx,
            ties = updatedTies + nextTies
        )
    }

    fun cupAdvancePrize(cupType: CupType, stageIndex: Int, isChampion: Boolean): Long {
        val base = when (cupType) {
            CupType.COPA_DO_BRASIL -> 2_200_000L
            CupType.LIBERTADORES -> 3_500_000L
            CupType.CHAMPIONS -> 5_500_000L
        }
        return if (isChampion) base * 4L else base * (stageIndex + 1)
    }

    // --- Youth Academy ---
    private val YOUTH_FIRST_NAMES = listOf(
        "Kauã", "Endrick", "Pedro", "Matheus", "Gabriel", "Ryan", "Bernardo",
        "Vitor", "Thales", "Lucas", "Davi", "Enzo", "João", "Yuri", "Caio", "Rafael"
    )
    private val YOUTH_LAST_NAMES = listOf(
        "Silva", "Rocha", "Alves", "Mendes", "Costa", "Nunes", "Cardoso",
        "Ferreira", "Barros", "Gomes", "Monteiro", "Tavares", "Ribeiro", "Santana"
    )

    fun generateYouthIntake(
        club: Club,
        trainingFocus: TrainingFocus,
        count: Int = 4,
        random: Random = Random.Default
    ): List<YouthProspect> {
        val positions = PlayerPosition.entries
        val baseOvr = (club.averageOverall - 15).coerceIn(56, 71)
        val focusBonus = if (trainingFocus == TrainingFocus.BASE) 3 else 0
        return (1..count).map { idx ->
            val pos = positions[idx % positions.size]
            val ovr = (baseOvr + focusBonus + random.nextInt(-2, 4)).coerceIn(55, 74)
            val pot = (ovr + random.nextInt(10, 20) + focusBonus).coerceIn(75, 92)
            val age = random.nextInt(16, 20)
            val mv = ((pot - 50) * 180_000L).coerceAtLeast(450_000L)
            YouthProspect(
                id = "yth_${UUID.randomUUID().toString().replace("-", "").take(8)}",
                name = "${YOUTH_FIRST_NAMES.random(random)} ${YOUTH_LAST_NAMES.random(random)}",
                position = pos,
                overall = ovr,
                potential = pot,
                age = age,
                salary = (18_000L + ovr * 650L),
                marketValue = mv,
                traits = listOf(
                    when (pos) {
                        PlayerPosition.GOL -> PlayerTrait.REFLEXO
                        PlayerPosition.ZAG -> PlayerTrait.DESARME
                        PlayerPosition.LAT -> PlayerTrait.VELOCIDADE
                        PlayerPosition.MEI -> PlayerTrait.PASSE
                        PlayerPosition.ATA -> PlayerTrait.FINALIZACAO
                    }
                )
            )
        }
    }

    fun promoteProspectToPlayer(prospect: YouthProspect, existingNumbers: Set<Int>): Player {
        val number = (1..99).firstOrNull { it !in existingNumbers } ?: 44
        return Player(
            id = "pl_${prospect.id}",
            name = prospect.name,
            position = prospect.position,
            overall = prospect.overall,
            age = prospect.age,
            nationality = "Brasil",
            shirtNumber = number,
            salary = prospect.salary,
            marketValue = prospect.marketValue,
            traits = prospect.traits,
            energy = 100,
            morale = 88,
            isStarter = false,
            contractRounds = 76,
            potential = prospect.potential
        )
    }

    // --- Contracts ---
    fun calculateRenewalCostAndSalary(player: Player): Pair<Long, Long> {
        val bonus = (player.marketValue * 0.04).toLong().coerceIn(80_000L, 1_500_000L)
        val newSalary = ((player.salary * 1.12).roundToInt().toLong()).coerceAtLeast(player.salary + 5_000L)
        return bonus to newSalary
    }

    fun renewPlayerContract(player: Player): Player {
        val (_, newSalary) = calculateRenewalCostAndSalary(player)
        return player.copy(
            contractRounds = player.contractRounds + 38,
            salary = newSalary,
            morale = (player.morale + 10).coerceAtMost(100)
        )
    }

    // --- Sponsorships ---
    fun availableSponsorsForClub(club: Club): List<SponsorDeal> {
        val prestigeMult = club.prestige.coerceIn(1, 5)
        return listOf(
            SponsorDeal(
                id = "sp_seguro",
                name = "Banco Nacional",
                tagline = "Receita fixa garantida toda rodada",
                perRoundBase = 240_000L * prestigeMult,
                winBonus = 100_000L * prestigeMult,
                titleBonus = 2_500_000L * prestigeMult,
                minPrestige = 1
            ),
            SponsorDeal(
                id = "sp_vitoria",
                name = "BetGol Esportes",
                tagline = "Bônus pesado por cada vitória em campo",
                perRoundBase = 140_000L * prestigeMult,
                winBonus = 360_000L * prestigeMult,
                titleBonus = 3_500_000L * prestigeMult,
                minPrestige = 1
            ),
            SponsorDeal(
                id = "sp_aero",
                name = "AeroBrasil Linhas",
                tagline = "Equilíbrio comercial + prêmio por taças",
                perRoundBase = 210_000L * prestigeMult,
                winBonus = 220_000L * prestigeMult,
                titleBonus = 5_000_000L * prestigeMult,
                minPrestige = 2
            ),
            SponsorDeal(
                id = "sp_global",
                name = "TechGlobal Master",
                tagline = "Patrocínio de elite para clubes vencedores",
                perRoundBase = 340_000L * prestigeMult,
                winBonus = 400_000L * prestigeMult,
                titleBonus = 8_000_000L * prestigeMult,
                minPrestige = 3
            )
        )
    }

    // --- Job Offers & Sacking ---
    fun evaluateJobOffers(
        userClub: Club,
        userPosition: Int,
        boardConfidence: Long,
        allClubs: List<Club>,
        currentRound: Int
    ): List<ManagerJobOffer> {
        if (currentRound < 8 || boardConfidence < 65L || userPosition > 6) return emptyList()
        val candidates = allClubs
            .filter {
                it.id != userClub.id &&
                    it.averageOverall >= userClub.averageOverall &&
                    (it.division == Division.SERIE_A || it.prestige >= 4)
            }
            .sortedByDescending { it.averageOverall }
            .take(8)
            .shuffled()
            .take(2)

        return candidates.map { target ->
            ManagerJobOffer(
                id = "job_${target.id}_r$currentRound",
                clubId = target.id,
                clubName = target.name,
                division = target.division,
                budget = target.budget,
                avgOverall = target.averageOverall,
                prestige = target.prestige,
                reason = "Impressionado com o ${userPosition}º lugar no ${userClub.shortName}"
            )
        }
    }

    // --- End-of-Season Awards ---
    fun computeSeasonAwards(
        clubs: List<Club>,
        userClub: Club,
        seasonYear: Long,
        managerName: String
    ): SeasonAwardSummary {
        val divTable = SeasonEngine.sortStandings(clubs, userClub.division)
        val champion = divTable.firstOrNull() ?: userClub
        val userPos = (divTable.indexOfFirst { it.id == userClub.id } + 1).coerceIn(1, divTable.size.coerceAtLeast(1))

        val divPlayers = clubs.filter { it.division == userClub.division }
            .flatMap { club -> club.players.map { it to club } }

        val topScorer = divPlayers.maxByOrNull { it.first.goalsSeason }
        val topAssist = divPlayers.maxByOrNull { it.first.assistsSeason }
        val mvp = divPlayers.maxByOrNull { it.first.goalsSeason * 2 + it.first.assistsSeason + it.first.effectiveOverall / 10 }

        val prizeMoney = when {
            userPos == 1 -> when (userClub.division) {
                Division.SERIE_A, Division.PREMIER_LEAGUE, Division.LA_LIGA,
                Division.SERIE_A_ITA, Division.BUNDESLIGA, Division.LIGUE_1 -> 45_000_000L
                Division.LIGA_ARGENTINA -> 22_000_000L
                Division.SERIE_B -> 12_000_000L
                Division.SERIE_C -> 5_000_000L
                Division.SERIE_D -> 2_500_000L
            }
            userPos <= 4 -> 8_000_000L
            else -> 3_000_000L
        }

        val moty = if (userPos <= 3) "$managerName (${userClub.shortName})" else "Treinador do ${champion.shortName}"

        return SeasonAwardSummary(
            seasonYear = seasonYear,
            divisionLabel = userClub.division.label,
            championClubName = champion.name,
            userFinishPos = userPos,
            prizeMoney = prizeMoney,
            goldenBootName = topScorer?.first?.name ?: "-",
            goldenBootClub = topScorer?.second?.shortName ?: "-",
            goldenBootGoals = topScorer?.first?.goalsSeason ?: 0,
            playmakerName = topAssist?.first?.name ?: "-",
            playmakerClub = topAssist?.second?.shortName ?: "-",
            playmakerAssists = topAssist?.first?.assistsSeason ?: 0,
            bestPlayerName = mvp?.first?.name ?: "-",
            bestPlayerClub = mvp?.second?.shortName ?: "-",
            managerOfTheYear = moty
        )
    }
}
