package com.example.domain.engine

import com.example.domain.model.Club
import com.example.domain.model.CompetitionType
import com.example.domain.model.Division
import com.example.domain.model.Fixture
import com.example.domain.model.Formation
import com.example.domain.model.IncomingTransferOffer
import com.example.domain.model.LiveFixtureState
import com.example.domain.model.LiveMatchState
import com.example.domain.model.MarkingStyle
import com.example.domain.model.MatchEvent
import com.example.domain.model.MatchEventType
import com.example.domain.model.Mentality
import com.example.domain.model.Player
import com.example.domain.model.PlayerPosition
import com.example.domain.model.TicketPriceLevel
import com.example.domain.model.TrainingFocus
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.random.Random

object SeasonEngine {

    /**
     * Generates a complete 38-round double round-robin schedule using Berger tables.
     */
    fun generateLeagueFixtures(clubs: List<Club>, division: Division): List<Fixture> {
        val divClubs = clubs.filter { it.division == division }.shuffled()
        if (divClubs.size < 2) return emptyList()
        val n = divClubs.size
        val half = n / 2
        val rotation = divClubs.drop(1).toMutableList()
        val fixed = divClubs.first()
        val firstHalf = mutableListOf<Fixture>()

        for (round in 1 until n) {
            val roundTeams = listOf(fixed) + rotation
            for (i in 0 until half) {
                val t1 = roundTeams[i]
                val t2 = roundTeams[n - 1 - i]
                val (home, away) = if ((round + i) % 2 == 0) t1 to t2 else t2 to t1
                firstHalf.add(
                    Fixture(
                        id = "${division.code}_r${round}_${home.id}_${away.id}",
                        round = round,
                        division = division,
                        homeClubId = home.id,
                        awayClubId = away.id
                    )
                )
            }
            val last = rotation.removeAt(rotation.lastIndex)
            rotation.add(0, last)
        }

        val secondHalf = firstHalf.map { f ->
            f.copy(
                id = "${division.code}_r${f.round + (n - 1)}_${f.awayClubId}_${f.homeClubId}",
                round = f.round + (n - 1),
                homeClubId = f.awayClubId,
                awayClubId = f.homeClubId
            )
        }
        return firstHalf + secondHalf
    }

    /**
     * Auto-picks the best available 11 starters matching the selected formation.
     */
    fun autoSelectBestLineup(players: List<Player>, formation: Formation): List<Player> {
        val available = players.filter { it.isAvailable }.sortedByDescending { it.effectiveOverall }
        val chosenIds = mutableSetOf<String>()

        fun pick(pos: PlayerPosition, count: Int) {
            available.filter { it.position == pos && it.id !in chosenIds }
                .take(count)
                .forEach { chosenIds.add(it.id) }
        }

        pick(PlayerPosition.GOL, 1)
        val lats = if (formation.defenders >= 4) 2 else 0
        val zags = formation.defenders - lats
        pick(PlayerPosition.ZAG, zags)
        pick(PlayerPosition.LAT, lats)
        pick(PlayerPosition.MEI, formation.midfielders)
        pick(PlayerPosition.ATA, formation.forwards)

        // Fill any gaps with highest-rated available outfield players
        if (chosenIds.size < 11) {
            available.filter { it.id !in chosenIds && it.position != PlayerPosition.GOL }
                .take(11 - chosenIds.size)
                .forEach { chosenIds.add(it.id) }
        }
        if (chosenIds.size < 11) {
            available.filter { it.id !in chosenIds }
                .take(11 - chosenIds.size)
                .forEach { chosenIds.add(it.id) }
        }

        return players.map { it.copy(isStarter = it.id in chosenIds) }
    }

    fun calculateClubStrength(
        club: Club,
        formation: Formation,
        mentality: Mentality,
        isHome: Boolean
    ): Double {
        val starters = club.players.filter { it.isStarter }.ifEmpty {
            club.players.filter { it.isAvailable }.sortedByDescending { it.effectiveOverall }.take(11)
        }
        val base = if (starters.isEmpty()) 65.0 else starters.map { it.effectiveOverall }.average()
        val homeBonus = if (isHome) 3.2 else 0.0
        val tacticalFit = when (mentality) {
            Mentality.OFENSIVO -> if (formation.forwards >= 3) 1.2 else 0.4
            Mentality.DEFENSIVO -> if (formation.defenders >= 4) 1.0 else 0.2
            Mentality.CONTRA_ATAQUE -> 0.8
            Mentality.EQUILIBRADO -> 0.7
        }
        val redPenalty = if (starters.size < 11) (11 - starters.size) * 4.5 else 0.0
        return (base + homeBonus + tacticalFit - redPenalty).coerceIn(45.0, 96.0)
    }

    fun prepareLiveMatch(
        round: Int,
        userClub: Club,
        allClubs: List<Club>,
        roundFixtures: List<Fixture>,
        ticketPriceLevel: TicketPriceLevel
    ): LiveMatchState {
        val clubMap = allClubs.associateBy { it.id }
        val userFixture = roundFixtures.firstOrNull {
            it.homeClubId == userClub.id || it.awayClubId == userClub.id
        } ?: roundFixtures.first()

        val userIsHome = userFixture.homeClubId == userClub.id
        val homeClub = clubMap[userFixture.homeClubId] ?: userClub
        val awayClub = clubMap[userFixture.awayClubId] ?: userClub

        val fillRatio = (0.58 + (homeClub.prestige * 0.06)) * ticketPriceLevel.attendanceFactor
        val attendance = (homeClub.stadiumCapacity * fillRatio.coerceIn(0.30, 0.99)).toInt()
        val revenue = if (userIsHome) attendance.toLong() * ticketPriceLevel.priceReais else 0L

        val userLive = LiveFixtureState(
            fixtureId = userFixture.id,
            homeClubId = homeClub.id,
            homeName = homeClub.name,
            homeShort = homeClub.shortName,
            homeColor = homeClub.primaryColorHex,
            awayClubId = awayClub.id,
            awayName = awayClub.name,
            awayShort = awayClub.shortName,
            awayColor = awayClub.primaryColorHex
        )

        val others = roundFixtures.filter { it.id != userFixture.id }.mapNotNull { f ->
            val h = clubMap[f.homeClubId] ?: return@mapNotNull null
            val a = clubMap[f.awayClubId] ?: return@mapNotNull null
            LiveFixtureState(
                fixtureId = f.id,
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

        val kickoffEvent = MatchEvent(
            minute = 0,
            type = MatchEventType.KICKOFF,
            clubId = homeClub.id,
            playerName = "",
            description = "Bola rolando em ${homeClub.stadiumName}! Público: $attendance torcedores."
        )

        return LiveMatchState(
            round = round,
            competition = userFixture.competition,
            minute = 0,
            phase = "1º Tempo",
            isPaused = false,
            isFinished = false,
            speed = 2,
            userIsHome = userIsHome,
            userMatch = userLive,
            otherMatches = others,
            events = listOf(kickoffEvent),
            possessionHome = 50,
            attendance = attendance,
            ticketRevenue = revenue
        )
    }

    /**
     * Simulates a 3-minute tick of the live matchday (both user's match and all 9 other simultaneous fixtures).
     */
    fun simulateTick(
        state: LiveMatchState,
        userClub: Club,
        allClubs: List<Club>,
        formation: Formation,
        mentality: Mentality,
        marking: MarkingStyle
    ): LiveMatchState {
        if (state.isFinished || state.isPaused) return state

        val nextMinute = (state.minute + 3).coerceAtMost(90)
        val clubMap = allClubs.associateBy { it.id }
        val newEvents = state.events.toMutableList()
        var userMatch = state.userMatch
        var shotsH = state.shotsHome
        var shotsA = state.shotsAway
        var cornersH = state.cornersHome
        var cornersA = state.cornersAway

        val homeClub = clubMap[userMatch.homeClubId] ?: userClub
        val awayClub = clubMap[userMatch.awayClubId] ?: userClub

        val homeStr = if (homeClub.id == userClub.id) {
            calculateClubStrength(homeClub, formation, mentality, true)
        } else {
            calculateClubStrength(homeClub, Formation.F_4_3_3, Mentality.EQUILIBRADO, true)
        }
        val awayStr = if (awayClub.id == userClub.id) {
            calculateClubStrength(awayClub, formation, mentality, false)
        } else {
            calculateClubStrength(awayClub, Formation.F_4_3_3, Mentality.EQUILIBRADO, false)
        }

        val userMentalityFactor = when {
            mentality == Mentality.OFENSIVO -> 1.18
            mentality == Mentality.DEFENSIVO -> 0.84
            else -> 1.0
        }

        // Simulate user match events in this 3-min window
        val homeChanceProb = ((homeStr / (homeStr + awayStr)) * 0.24 *
            (if (state.userIsHome) userMentalityFactor else 1.0)).coerceIn(0.06, 0.26)
        val awayChanceProb = ((awayStr / (homeStr + awayStr)) * 0.24 *
            (if (!state.userIsHome) userMentalityFactor else 1.0)).coerceIn(0.06, 0.26)

        val roll = Random.nextDouble()
        if (roll < homeChanceProb) {
            shotsH++
            if (Random.nextDouble() < 0.35) cornersH++
            val scorer = pickAttackerOrMid(homeClub)
            val assist = pickAssistPlayer(homeClub, scorer)
            val goalProb = (0.28 + (homeStr - awayStr) * 0.008).coerceIn(0.16, 0.48)
            if (Random.nextDouble() < goalProb) {
                userMatch = userMatch.copy(
                    homeScore = userMatch.homeScore + 1,
                    lastGoalText = "${nextMinute}' ${scorer.name}"
                )
                newEvents.add(
                    0,
                    MatchEvent(
                        minute = nextMinute,
                        type = MatchEventType.GOAL,
                        clubId = homeClub.id,
                        playerName = scorer.name,
                        assistName = assist?.name,
                        description = "GOOOOL DO ${homeClub.shortName}! ${scorer.name} balança as redes" +
                            (assist?.let { " após passe de ${it.name}!" } ?: "!")
                    )
                )
            } else {
                val gk = awayClub.players.firstOrNull { it.position == PlayerPosition.GOL && it.isStarter }
                    ?: awayClub.players.firstOrNull { it.position == PlayerPosition.GOL }
                newEvents.add(
                    0,
                    MatchEvent(
                        minute = nextMinute,
                        type = MatchEventType.SAVE,
                        clubId = awayClub.id,
                        playerName = gk?.name ?: "Goleiro",
                        description = "Quase! ${scorer.name} finalizou forte, mas ${gk?.name ?: "o goleiro"} salvou!"
                    )
                )
            }
        } else if (roll < homeChanceProb + awayChanceProb) {
            shotsA++
            if (Random.nextDouble() < 0.35) cornersA++
            val scorer = pickAttackerOrMid(awayClub)
            val assist = pickAssistPlayer(awayClub, scorer)
            val goalProb = (0.28 + (awayStr - homeStr) * 0.008).coerceIn(0.16, 0.48)
            if (Random.nextDouble() < goalProb) {
                userMatch = userMatch.copy(
                    awayScore = userMatch.awayScore + 1,
                    lastGoalText = "${nextMinute}' ${scorer.name}"
                )
                newEvents.add(
                    0,
                    MatchEvent(
                        minute = nextMinute,
                        type = MatchEventType.GOAL,
                        clubId = awayClub.id,
                        playerName = scorer.name,
                        assistName = assist?.name,
                        description = "GOOOOL DO ${awayClub.shortName}! ${scorer.name} marca para os visitantes!"
                    )
                )
            } else {
                val gk = homeClub.players.firstOrNull { it.position == PlayerPosition.GOL && it.isStarter }
                    ?: homeClub.players.firstOrNull { it.position == PlayerPosition.GOL }
                newEvents.add(
                    0,
                    MatchEvent(
                        minute = nextMinute,
                        type = MatchEventType.SAVE,
                        clubId = homeClub.id,
                        playerName = gk?.name ?: "Goleiro",
                        description = "Defesaça de ${gk?.name ?: "goleiro"} no chute de ${scorer.name}!"
                    )
                )
            }
        } else if (Random.nextDouble() < 0.09 * marking.foulRisk) {
            val isHomeCard = Random.nextBoolean()
            val targetClub = if (isHomeCard) homeClub else awayClub
            val offender = targetClub.players.filter { it.isStarter }.randomOrNull() ?: targetClub.players.firstOrNull()
            if (offender != null) {
                if (Random.nextDouble() < 0.08) {
                    userMatch = if (isHomeCard) {
                        userMatch.copy(homeRedCards = userMatch.homeRedCards + 1)
                    } else {
                        userMatch.copy(awayRedCards = userMatch.awayRedCards + 1)
                    }
                    newEvents.add(
                        0,
                        MatchEvent(
                            minute = nextMinute,
                            type = MatchEventType.RED_CARD,
                            clubId = targetClub.id,
                            playerName = offender.name,
                            description = "EXPULSO! Cartão vermelho direto para ${offender.name} (${targetClub.shortName})!"
                        )
                    )
                } else {
                    newEvents.add(
                        0,
                        MatchEvent(
                            minute = nextMinute,
                            type = MatchEventType.YELLOW_CARD,
                            clubId = targetClub.id,
                            playerName = offender.name,
                            description = "Cartão amarelo para ${offender.name} (${targetClub.shortName})."
                        )
                    )
                }
            }
        }

        // Simulate simultaneous other matches (Brasfoot iconic scoreboard!)
        val updatedOthers = state.otherMatches.map { other ->
            val h = clubMap[other.homeClubId]
            val a = clubMap[other.awayClubId]
            val hStr = h?.averageOverall?.toDouble()?.plus(2.5) ?: 72.0
            val aStr = a?.averageOverall?.toDouble() ?: 72.0
            val pH = ((hStr / (hStr + aStr)) * 0.085).coerceIn(0.02, 0.075)
            val pA = ((aStr / (hStr + aStr)) * 0.078).coerceIn(0.02, 0.07)
            val r = Random.nextDouble()
            when {
                r < pH -> {
                    val sc = h?.let { pickAttackerOrMid(it).name } ?: "Atacante"
                    other.copy(homeScore = other.homeScore + 1, lastGoalText = "${nextMinute}' $sc")
                }
                r < pH + pA -> {
                    val sc = a?.let { pickAttackerOrMid(it).name } ?: "Atacante"
                    other.copy(awayScore = other.awayScore + 1, lastGoalText = "${nextMinute}' $sc")
                }
                else -> other
            }
        }

        // Halftime pause at 45'
        val hitHalftime = state.minute < 45 && nextMinute >= 45
        val hitFulltime = nextMinute >= 90

        if (hitHalftime) {
            newEvents.add(
                0,
                MatchEvent(
                    minute = 45,
                    type = MatchEventType.HALFTIME,
                    clubId = homeClub.id,
                    playerName = "",
                    description = "Fim do 1º tempo! ${userMatch.homeShort} ${userMatch.homeScore} x ${userMatch.awayScore} ${userMatch.awayShort}"
                )
            )
        }
        if (hitFulltime) {
            newEvents.add(
                0,
                MatchEvent(
                    minute = 90,
                    type = MatchEventType.FULLTIME,
                    clubId = homeClub.id,
                    playerName = "",
                    description = "Apita o árbitro! Fim de jogo: ${userMatch.homeName} ${userMatch.homeScore} x ${userMatch.awayScore} ${userMatch.awayName}"
                )
            )
        }

        val possession = ((homeStr / (homeStr + awayStr)) * 100).roundToInt().coerceIn(32, 68)
        val motm = if (hitFulltime) {
            val winnerClub = when {
                userMatch.homeScore > userMatch.awayScore -> homeClub
                userMatch.awayScore > userMatch.homeScore -> awayClub
                else -> userClub
            }
            newEvents.firstOrNull { it.type == MatchEventType.GOAL && it.clubId == winnerClub.id }?.playerName
                ?: pickAttackerOrMid(winnerClub).name
        } else null

        return state.copy(
            minute = nextMinute,
            phase = when {
                hitFulltime -> "Fim de Jogo"
                hitHalftime -> "Intervalo"
                nextMinute >= 45 -> "2º Tempo"
                else -> "1º Tempo"
            },
            isPaused = hitHalftime || hitFulltime,
            isFinished = hitFulltime,
            userMatch = userMatch,
            otherMatches = updatedOthers,
            events = newEvents.take(60),
            possessionHome = possession,
            shotsHome = shotsH,
            shotsAway = shotsA,
            cornersHome = cornersH,
            cornersAway = cornersA,
            manOfTheMatch = motm
        )
    }

    /**
     * Generates AI incoming transfer offers for user's players listed for sale or top performers.
     */
    fun generateIncomingOffers(userClub: Club, allClubs: List<Club>): List<IncomingTransferOffer> {
        val candidates = userClub.players.filter { it.forSale || (it.overall >= 76 && Random.nextDouble() < 0.25) }
            .take(2)
        if (candidates.isEmpty()) return emptyList()
        val rivals = allClubs.filter { it.id != userClub.id && it.budget > 4_000_000L }
        if (rivals.isEmpty()) return emptyList()

        return candidates.mapNotNull { player ->
            val buyer = rivals.randomOrNull() ?: return@mapNotNull null
            val mult = if (player.forSale) Random.nextDouble(0.92, 1.15) else Random.nextDouble(1.08, 1.32)
            val amount = ((player.marketValue * mult) / 100_000L).toLong() * 100_000L
            IncomingTransferOffer(
                id = "off_${UUID.randomUUID().toString().take(8)}",
                player = player,
                offeringClubId = buyer.id,
                offeringClubName = buyer.name,
                offerAmount = amount.coerceAtLeast(500_000L)
            )
        }
    }

    /**
     * Applies training, stamina recovery, wages, and matchday table standings updates after round finishes.
     */
    fun applyRoundResults(
        clubs: List<Club>,
        fixtures: List<Fixture>,
        liveState: LiveMatchState,
        userClubId: String,
        trainingFocus: TrainingFocus,
        marking: MarkingStyle
    ): Pair<List<Club>, List<Fixture>> {
        val allLiveMatches = listOf(liveState.userMatch) + liveState.otherMatches
        val liveById = allLiveMatches.associateBy { it.fixtureId }
        val otherDivisionFixtures = fixtures.filter { f ->
            f.round == liveState.round && !f.played && liveById[f.id] == null
        }

        val clubMap = clubs.associateBy { it.id }.toMutableMap()

        // Update played fixtures for user's division + auto-simulate the other division's round
        val updatedFixtures = fixtures.map { fixture ->
            val liveMatch = liveById[fixture.id]
            if (liveMatch != null) {
                val events = if (fixture.id == liveState.userMatch.fixtureId) liveState.events else emptyList()
                fixture.copy(
                    homeScore = liveMatch.homeScore,
                    awayScore = liveMatch.awayScore,
                    played = true,
                    events = events,
                    attendance = if (fixture.id == liveState.userMatch.fixtureId) liveState.attendance else 18_000,
                    revenue = if (fixture.id == liveState.userMatch.fixtureId) liveState.ticketRevenue else 0L
                )
            } else if (otherDivisionFixtures.any { it.id == fixture.id }) {
                val h = clubMap[fixture.homeClubId]
                val a = clubMap[fixture.awayClubId]
                val hStr = (h?.averageOverall ?: 70) + 2
                val aStr = (a?.averageOverall ?: 70)
                val diff = (hStr - aStr) / 10.0
                val hGoals = (Random.nextDouble(0.0, 2.5 + diff.coerceAtLeast(0.0))).toInt().coerceIn(0, 5)
                val aGoals = (Random.nextDouble(0.0, 2.2 - diff.coerceAtMost(0.0))).toInt().coerceIn(0, 4)
                fixture.copy(homeScore = hGoals, awayScore = aGoals, played = true)
            } else {
                fixture
            }
        }

        val roundPlayedFixtures = updatedFixtures.filter { it.round == liveState.round && it.played }
        roundPlayedFixtures.forEach { f ->
            val home = clubMap[f.homeClubId] ?: return@forEach
            val away = clubMap[f.awayClubId] ?: return@forEach

            val homePts = when {
                f.homeScore > f.awayScore -> 3
                f.homeScore == f.awayScore -> 1
                else -> 0
            }
            val awayPts = when {
                f.awayScore > f.homeScore -> 3
                f.homeScore == f.awayScore -> 1
                else -> 0
            }

            val homeForm = (home.form + when (homePts) { 3 -> "V"; 1 -> "E"; else -> "D" }).takeLast(5)
            val awayForm = (away.form + when (awayPts) { 3 -> "V"; 1 -> "E"; else -> "D" }).takeLast(5)

            val homeRevenue = if (home.id == userClubId) liveState.ticketRevenue else (home.stadiumCapacity * 30L)
            val awayTvBonus = 320_000L

            clubMap[home.id] = home.copy(
                points = home.points + homePts,
                played = home.played + 1,
                wins = home.wins + if (homePts == 3) 1 else 0,
                draws = home.draws + if (homePts == 1) 1 else 0,
                losses = home.losses + if (homePts == 0) 1 else 0,
                goalsFor = home.goalsFor + f.homeScore,
                goalsAgainst = home.goalsAgainst + f.awayScore,
                form = homeForm,
                budget = home.budget + homeRevenue - (home.weeklyWages / 4L)
            )
            clubMap[away.id] = away.copy(
                points = away.points + awayPts,
                played = away.played + 1,
                wins = away.wins + if (awayPts == 3) 1 else 0,
                draws = away.draws + if (awayPts == 1) 1 else 0,
                losses = away.losses + if (awayPts == 0) 1 else 0,
                goalsFor = away.goalsFor + f.awayScore,
                goalsAgainst = away.goalsAgainst + f.homeScore,
                form = awayForm,
                budget = away.budget + awayTvBonus - (away.weeklyWages / 4L)
            )
        }

        // Apply stamina drain/recovery, goal stats, cards, and training development
        val goalScorers = liveState.events.filter { it.type == MatchEventType.GOAL }
        val yellowCards = liveState.events.filter { it.type == MatchEventType.YELLOW_CARD }
        val redCards = liveState.events.filter { it.type == MatchEventType.RED_CARD }

        val finalClubs = clubMap.values.map { club ->
            val isUser = club.id == userClubId
            val drain = if (isUser) (11 * marking.energyDrain).toInt() else 10
            val recovery = if (isUser && trainingFocus == TrainingFocus.FISICO) 15 else 9

            val updatedPlayers = club.players.map { p ->
                val goalsScored = goalScorers.count { it.clubId == club.id && it.playerName == p.name }
                val assistsMade = goalScorers.count { it.clubId == club.id && it.assistName == p.name }
                val yellows = yellowCards.count { it.clubId == club.id && it.playerName == p.name }
                val reds = redCards.count { it.clubId == club.id && it.playerName == p.name }

                val totalYellows = p.yellowCards + yellows
                val newSusp = when {
                    reds > 0 -> 1
                    p.suspendedRounds > 0 -> (p.suspendedRounds - 1).coerceAtLeast(0)
                    totalYellows >= 3 -> 1
                    else -> 0
                }
                val resetYellows = if (totalYellows >= 3) 0 else totalYellows

                val newEnergy = if (p.isStarter) {
                    (p.energy - drain + recovery).coerceIn(45, 100)
                } else {
                    (p.energy + recovery + 12).coerceIn(60, 100)
                }

                // Training development chance
                val growthChance = when {
                    isUser && trainingFocus == TrainingFocus.BASE && p.age <= 21 -> 0.18
                    isUser && trainingFocus == TrainingFocus.ATAQUE && (p.position == PlayerPosition.ATA || p.position == PlayerPosition.MEI) -> 0.12
                    isUser && trainingFocus == TrainingFocus.DEFESA && (p.position == PlayerPosition.GOL || p.position == PlayerPosition.ZAG || p.position == PlayerPosition.LAT) -> 0.12
                    p.age <= 22 -> 0.06
                    else -> 0.02
                }
                val newOvr = if (Random.nextDouble() < growthChance && p.overall < 90) p.overall + 1 else p.overall

                p.copy(
                    overall = newOvr,
                    energy = newEnergy,
                    goalsSeason = p.goalsSeason + goalsScored,
                    assistsSeason = p.assistsSeason + assistsMade,
                    yellowCards = resetYellows,
                    suspendedRounds = newSusp,
                    injuredRounds = (p.injuredRounds - 1).coerceAtLeast(0)
                )
            }
            club.copy(players = updatedPlayers)
        }

        return finalClubs to updatedFixtures
    }

    fun sortStandings(clubs: List<Club>, division: Division): List<Club> {
        return clubs.filter { it.division == division }
            .sortedWith(
                compareByDescending<Club> { it.points }
                    .thenByDescending { it.wins }
                    .thenByDescending { it.goalDifference }
                    .thenByDescending { it.goalsFor }
                    .thenBy { it.name }
            )
    }

    /**
     * Handles end-of-season promotion (G4 Serie B -> Serie A) & relegation (Z4 Serie A -> Serie B),
     * resets seasonal stats, ages players by 1 year, and keeps squads/finances intact.
     */
    fun advanceToNextSeason(clubs: List<Club>): List<Club> {
        val tableA = sortStandings(clubs, Division.SERIE_A)
        val tableB = sortStandings(clubs, Division.SERIE_B)
        val relegatedIds = tableA.takeLast(4).map { it.id }.toSet()
        val promotedIds = tableB.take(4).map { it.id }.toSet()

        return clubs.map { club ->
            val newDiv = when {
                club.id in relegatedIds -> Division.SERIE_B
                club.id in promotedIds -> Division.SERIE_A
                else -> club.division
            }
            val prize = when (club.id) {
                tableA.firstOrNull()?.id -> 45_000_000L
                tableB.firstOrNull()?.id -> 12_000_000L
                in promotedIds -> 8_000_000L
                else -> 3_000_000L
            }
            club.copy(
                division = newDiv,
                budget = club.budget + prize,
                points = 0,
                played = 0,
                wins = 0,
                draws = 0,
                losses = 0,
                goalsFor = 0,
                goalsAgainst = 0,
                form = emptyList(),
                copaEliminated = false,
                players = club.players.map { p ->
                    p.copy(
                        age = p.age + 1,
                        energy = 100,
                        goalsSeason = 0,
                        assistsSeason = 0,
                        yellowCards = 0,
                        suspendedRounds = 0,
                        injuredRounds = 0
                    )
                }
            )
        }
    }

    private fun pickAttackerOrMid(club: Club): Player {
        val starters = club.players.filter { it.isStarter }.ifEmpty { club.players.take(11) }
        val attackers = starters.filter { it.position == PlayerPosition.ATA }
        val mids = starters.filter { it.position == PlayerPosition.MEI }
        val pool = when {
            attackers.isNotEmpty() && Random.nextDouble() < 0.65 -> attackers
            mids.isNotEmpty() && Random.nextDouble() < 0.85 -> mids
            else -> starters.filter { it.position != PlayerPosition.GOL }.ifEmpty { starters }
        }
        return pool.randomOrNull() ?: club.players.first()
    }

    private fun pickAssistPlayer(club: Club, scorer: Player): Player? {
        val starters = club.players.filter { it.isStarter && it.id != scorer.id && it.position != PlayerPosition.GOL }
        val mids = starters.filter { it.position == PlayerPosition.MEI || it.position == PlayerPosition.LAT }
        return (if (mids.isNotEmpty()) mids else starters).randomOrNull()
    }
}
