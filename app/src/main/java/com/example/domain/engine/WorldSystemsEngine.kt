package com.example.domain.engine

import com.example.domain.model.AnnualAwardsRecord
import com.example.domain.model.BallonDorCandidate
import com.example.domain.model.ChampionsLeagueState
import com.example.domain.model.Club
import com.example.domain.model.Division
import com.example.domain.model.InjuryRecord
import com.example.domain.model.InjurySeverity
import com.example.domain.model.MedicalTreatmentType
import com.example.domain.model.NewsCategory
import com.example.domain.model.NewsItem
import com.example.domain.model.Player
import com.example.domain.model.PlayerPosition
import com.example.domain.model.TransferWindowStatus
import java.util.UUID
import kotlin.random.Random

object TransferWindowEngine {

    fun windowStatus(currentRound: Int, totalRounds: Int): TransferWindowStatus {
        val w1End = 6
        val midStart = (totalRounds / 2).coerceAtLeast(15)
        val midEnd = (midStart + 5).coerceAtMost(totalRounds - 4)
        val rangesText = "Rodadas 1–$w1End e $midStart–$midEnd"

        return when {
            currentRound > totalRounds -> TransferWindowStatus(
                isOpen = false,
                windowName = "Entretela / Fim de Época",
                statusLabel = "FECHADA • Inicie a próxima temporada para reabrir",
                roundsRemaining = 0,
                openRangesText = rangesText
            )
            currentRound in 1..w1End -> {
                val rem = (w1End - currentRound + 1).coerceAtLeast(1)
                TransferWindowStatus(
                    isOpen = true,
                    windowName = "Janela de Abertura",
                    statusLabel = "ABERTA • Fecha após a Rodada $w1End ($rem rod. restante${if (rem > 1) "s" else ""})",
                    roundsRemaining = rem,
                    openRangesText = rangesText
                )
            }
            currentRound in midStart..midEnd -> {
                val rem = (midEnd - currentRound + 1).coerceAtLeast(1)
                TransferWindowStatus(
                    isOpen = true,
                    windowName = "Janela de Meio de Temporada",
                    statusLabel = "ABERTA • Fecha após a Rodada $midEnd ($rem rod. restante${if (rem > 1) "s" else ""})",
                    roundsRemaining = rem,
                    openRangesText = rangesText
                )
            }
            currentRound < midStart -> {
                val until = (midStart - currentRound).coerceAtLeast(1)
                TransferWindowStatus(
                    isOpen = false,
                    windowName = "Janela Fechada",
                    statusLabel = "FECHADA • Reabre na Rodada $midStart (faltam $until rod.)",
                    roundsRemaining = until,
                    openRangesText = rangesText
                )
            }
            else -> TransferWindowStatus(
                isOpen = false,
                windowName = "Janela Encerrada",
                statusLabel = "FECHADA • Próxima janela só na temporada seguinte",
                roundsRemaining = (totalRounds - currentRound + 1).coerceAtLeast(1),
                openRangesText = rangesText
            )
        }
    }

    fun transitionNewsForRound(
        newRound: Int,
        totalRounds: Int,
        userClub: Club
    ): NewsItem? {
        val midStart = (totalRounds / 2).coerceAtLeast(15)
        val midEnd = (midStart + 5).coerceAtMost(totalRounds - 4)

        val (headline, body, tag) = when (newRound) {
            7 -> Triple(
                "Janela de Abertura fechada!",
                "O prazo inicial de inscrições se encerrou após a Rodada 6. Novas contratações só serão liberadas na Rodada $midStart.",
                "JANELA FECHADA"
            )
            midStart -> Triple(
                "ABERTA a Janela de Meio de Temporada!",
                "O mercado está oficialmente liberado da Rodada $midStart até a Rodada $midEnd para reforços e vendas.",
                "JANELA ABERTA"
            )
            midEnd + 1 -> Triple(
                "Janela de Transferências encerrada na temporada!",
                "O período de registros foi fechado após a Rodada $midEnd. Os elencos estão fechados até a próxima época.",
                "JANELA FECHADA"
            )
            else -> return null
        }

        return NewsItem(
            id = "news_window_${newRound}_${UUID.randomUUID().toString().take(5)}",
            category = NewsCategory.TRANSFER,
            round = newRound,
            headline = headline,
            body = body,
            clubId = userClub.id,
            clubName = userClub.name,
            clubShortName = userClub.shortName,
            clubColorHex = userClub.primaryColorHex,
            clubSecondaryColorHex = userClub.secondaryColorHex,
            isBreaking = true,
            tagLabel = tag
        )
    }
}

object BallonDorEngine {

    private val TOP_LEAGUES = setOf(
        Division.SERIE_A,
        Division.PREMIER_LEAGUE,
        Division.LA_LIGA,
        Division.SERIE_A_ITA,
        Division.BUNDESLIGA,
        Division.LIGUE_1,
        Division.LIGA_ARGENTINA
    )

    private fun scorePlayer(
        player: Player,
        club: Club,
        leagueWinnerIds: Set<String>,
        uclWinnerId: String?
    ): Int {
        val leagueWeight = when (club.division) {
            Division.PREMIER_LEAGUE, Division.LA_LIGA, Division.SERIE_A -> 12
            Division.SERIE_A_ITA, Division.BUNDESLIGA, Division.LIGUE_1 -> 10
            Division.LIGA_ARGENTINA -> 7
            else -> 2
        }
        val titleBonus = (if (club.id in leagueWinnerIds) 18 else 0) + (if (club.id == uclWinnerId) 28 else 0)
        val keeperCleanBonus = if (player.position == PlayerPosition.GOL) {
            ((club.wins * 2) - (club.goalsAgainst / 4)).coerceAtLeast(0) + (player.overall - 72).coerceAtLeast(0)
        } else 0

        return (player.goalsSeason * 4) +
            (player.assistsSeason * 3) +
            (player.overall * 2) +
            (club.prestige * 5) +
            leagueWeight +
            titleBonus +
            keeperCleanBonus
    }

    private fun toCandidate(
        player: Player,
        club: Club,
        score: Int
    ): BallonDorCandidate = BallonDorCandidate(
        playerId = player.id,
        playerName = player.name,
        position = player.position,
        clubId = club.id,
        clubName = club.name,
        clubShortName = club.shortName,
        divisionLabel = club.division.shortLabel,
        age = player.age,
        overall = player.overall,
        goals = player.goalsSeason,
        assists = player.assistsSeason,
        score = score
    )

    fun computeLiveCandidates(
        clubs: List<Club>,
        championsLeague: ChampionsLeagueState?,
        limit: Int = 12,
        userVotedPlayerId: String? = null
    ): List<BallonDorCandidate> {
        val leaders = Division.entries.mapNotNull { div ->
            SeasonEngine.sortStandings(clubs, div).firstOrNull()?.id
        }.toSet()
        val uclChamp = championsLeague?.championClubId

        return clubs.asSequence()
            .filter { it.division in TOP_LEAGUES || it.played > 0 }
            .flatMap { club ->
                club.players.asSequence().map { player ->
                    val voteBonus = if (player.id == userVotedPlayerId) 22 else 0
                    val s = scorePlayer(player, club, leaders, uclChamp) + voteBonus
                    toCandidate(player, club, s)
                }
            }
            .sortedWith(
                compareByDescending<BallonDorCandidate> { it.score }
                    .thenByDescending { it.goals }
                    .thenByDescending { it.overall }
            )
            .take(limit)
            .toList()
    }

    fun computeAnnualAwards(
        clubs: List<Club>,
        championsLeague: ChampionsLeagueState?,
        seasonYear: Long,
        userClub: Club,
        managerName: String,
        userVotedPlayerId: String? = null
    ): AnnualAwardsRecord {
        val leaders = Division.entries.mapNotNull { div ->
            SeasonEngine.sortStandings(clubs, div).firstOrNull()?.id
        }.toSet()
        val uclChamp = championsLeague?.championClubId

        val allScored = clubs.flatMap { club ->
            club.players.map { p ->
                val voteBonus = if (p.id == userVotedPlayerId) 22 else 0
                toCandidate(p, club, scorePlayer(p, club, leaders, uclChamp) + voteBonus)
            }
        }

        val ballonDorTop3 = allScored
            .sortedByDescending { it.score }
            .take(3)

        val goldenBoot = allScored.maxWithOrNull(
            compareBy<BallonDorCandidate> { it.goals }.thenBy { it.score }
        )
        val topPlaymaker = allScored.maxWithOrNull(
            compareBy<BallonDorCandidate> { it.assists }.thenBy { it.score }
        )
        val bestGoalkeeper = allScored
            .filter { it.position == PlayerPosition.GOL }
            .maxByOrNull { it.score }
        val bestYoung = allScored
            .filter { it.age <= 21 }
            .maxByOrNull { it.score }

        val teamOfYear = buildList {
            allScored.filter { it.position == PlayerPosition.GOL }.sortedByDescending { it.score }.take(1).forEach(::add)
            allScored.filter { it.position == PlayerPosition.ZAG }.sortedByDescending { it.score }.take(2).forEach(::add)
            allScored.filter { it.position == PlayerPosition.LAT }.sortedByDescending { it.score }.take(2).forEach(::add)
            allScored.filter { it.position == PlayerPosition.MEI }.sortedByDescending { it.score }.take(3).forEach(::add)
            allScored.filter { it.position == PlayerPosition.ATA }.sortedByDescending { it.score }.take(3).forEach(::add)
        }

        val userDivPos = SeasonEngine.sortStandings(clubs, userClub.division).indexOfFirst { it.id == userClub.id } + 1
        val bestCoachClub = uclChamp?.let { cid -> clubs.firstOrNull { it.id == cid } }
            ?: SeasonEngine.sortStandings(clubs, userClub.division).firstOrNull()
            ?: userClub
        val bestCoachName = if (userDivPos == 1 || userClub.id == uclChamp) {
            managerName
        } else {
            "Comandante do ${bestCoachClub.shortName}"
        }

        return AnnualAwardsRecord(
            seasonYear = seasonYear,
            ballonDorTop3 = ballonDorTop3,
            goldenBoot = goldenBoot,
            topPlaymaker = topPlaymaker,
            bestGoalkeeper = bestGoalkeeper,
            bestYoungPlayer = bestYoung,
            bestManagerName = bestCoachName,
            bestManagerClubName = bestCoachClub.name,
            teamOfTheYear = teamOfYear
        )
    }
}

object MedicalEngine {

    private val MILD_INJURIES = listOf(
        "Fadiga Muscular Aguda",
        "Contusão no Tornozelo",
        "Sobrecarga na Panturrilha"
    )
    private val MODERATE_INJURIES = listOf(
        "Estiramento Muscular na Coxa",
        "Entorse no Tornozelo",
        "Edema no Adutor"
    )
    private val SEVERE_INJURIES = listOf(
        "Lesão Ligamentar no Joelho",
        "Ruptura Parcial do Posterior",
        "Fratura por Estresse no Pé"
    )

    fun inflictInjury(
        player: Player,
        seasonYear: Long,
        round: Int,
        severity: InjurySeverity = pickSeverity(player.energy),
        random: Random = Random.Default
    ): Player {
        if (player.injuredRounds > 0) return player
        val duration = random.nextInt(severity.minRounds, severity.maxRounds + 1)
        val injuryName = when (severity) {
            InjurySeverity.LEVE -> MILD_INJURIES.random(random)
            InjurySeverity.MODERADA -> MODERATE_INJURIES.random(random)
            InjurySeverity.GRAVE -> SEVERE_INJURIES.random(random)
        }
        val record = InjuryRecord(
            id = "inj_${UUID.randomUUID().toString().replace("-", "").take(8)}",
            seasonYear = seasonYear,
            roundOccurred = round,
            injuryName = injuryName,
            severity = severity,
            estimatedRounds = duration,
            remainingRounds = duration,
            treatmentApplied = false,
            recoveredRound = null
        )
        return player.copy(
            injuredRounds = duration,
            isStarter = false,
            currentInjuryName = injuryName,
            currentInjurySeverity = severity,
            injuryEstimatedRounds = duration,
            injuryTreatedThisInjury = false,
            injuryHistory = listOf(record) + player.injuryHistory
        )
    }

    private fun pickSeverity(energy: Int, random: Random = Random.Default): InjurySeverity {
        val roll = random.nextDouble()
        return when {
            energy < 55 && roll < 0.25 -> InjurySeverity.GRAVE
            roll < 0.55 -> InjurySeverity.LEVE
            roll < 0.88 -> InjurySeverity.MODERADA
            else -> InjurySeverity.GRAVE
        }
    }

    fun advanceRecoveryOneRound(player: Player, currentRound: Int): Player {
        if (player.injuredRounds <= 0) return player
        val nextRemaining = (player.injuredRounds - 1).coerceAtLeast(0)
        val updatedHistory = player.injuryHistory.mapIndexed { idx, rec ->
            if (idx == 0 && rec.recoveredRound == null) {
                rec.copy(
                    remainingRounds = nextRemaining,
                    recoveredRound = if (nextRemaining == 0) currentRound else null
                )
            } else rec
        }
        return if (nextRemaining == 0) {
            player.copy(
                injuredRounds = 0,
                currentInjuryName = null,
                currentInjurySeverity = null,
                injuryEstimatedRounds = 0,
                injuryTreatedThisInjury = false,
                injuryHistory = updatedHistory
            )
        } else {
            player.copy(
                injuredRounds = nextRemaining,
                injuryHistory = updatedHistory
            )
        }
    }

    fun applyTreatment(
        player: Player,
        treatment: MedicalTreatmentType,
        currentRound: Int
    ): Player {
        if (player.injuredRounds <= 0) return player
        val reduced = treatment.roundsReduced
        val nextRemaining = (player.injuredRounds - reduced).coerceAtLeast(0)
        val energyGain = if (treatment == MedicalTreatmentType.TRATAMENTO_ESPECIALIZADO) 15 else 6
        val updatedHistory = player.injuryHistory.mapIndexed { idx, rec ->
            if (idx == 0 && rec.recoveredRound == null) {
                rec.copy(
                    remainingRounds = nextRemaining,
                    treatmentApplied = true,
                    recoveredRound = if (nextRemaining == 0) currentRound else null
                )
            } else rec
        }
        return if (nextRemaining == 0) {
            player.copy(
                injuredRounds = 0,
                energy = (player.energy + energyGain).coerceAtMost(100),
                currentInjuryName = null,
                currentInjurySeverity = null,
                injuryEstimatedRounds = 0,
                injuryTreatedThisInjury = false,
                injuryHistory = updatedHistory
            )
        } else {
            player.copy(
                injuredRounds = nextRemaining,
                energy = (player.energy + energyGain).coerceAtMost(100),
                injuryTreatedThisInjury = true,
                injuryHistory = updatedHistory
            )
        }
    }
}
