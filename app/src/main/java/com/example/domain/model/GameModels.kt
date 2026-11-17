package com.example.domain.model

enum class Division(
    val label: String,
    val shortLabel: String,
    val code: String,
    val country: String,
    val espnSlug: String,
    val totalRounds: Int,
    val promotedTo: String? = null,
    val relegatedTo: String? = null,
    val promotionSpots: Int = 0,
    val relegationSpots: Int = 0,
    val championsSpots: Int = 4,
    val secondaryContSpots: Int = 0
) {
    SERIE_A(
        label = "Brasileirão Série A",
        shortLabel = "Série A",
        code = "SERIE_A",
        country = "Brasil",
        espnSlug = "bra.1",
        totalRounds = 38,
        relegatedTo = "SERIE_B",
        relegationSpots = 4,
        championsSpots = 6,
        secondaryContSpots = 12
    ),
    SERIE_B(
        label = "Brasileirão Série B",
        shortLabel = "Série B",
        code = "SERIE_B",
        country = "Brasil",
        espnSlug = "bra.2",
        totalRounds = 38,
        promotedTo = "SERIE_A",
        relegatedTo = "SERIE_C",
        promotionSpots = 4,
        relegationSpots = 4,
        championsSpots = 0
    ),
    SERIE_C(
        label = "Brasileirão Série C",
        shortLabel = "Série C",
        code = "SERIE_C",
        country = "Brasil",
        espnSlug = "",
        totalRounds = 38,
        promotedTo = "SERIE_B",
        relegatedTo = "SERIE_D",
        promotionSpots = 4,
        relegationSpots = 4,
        championsSpots = 0
    ),
    SERIE_D(
        label = "Brasileirão Série D",
        shortLabel = "Série D",
        code = "SERIE_D",
        country = "Brasil",
        espnSlug = "",
        totalRounds = 38,
        promotedTo = "SERIE_C",
        promotionSpots = 4,
        championsSpots = 0
    ),
    PREMIER_LEAGUE(
        label = "Premier League",
        shortLabel = "Premier League",
        code = "PREMIER_LEAGUE",
        country = "Inglaterra",
        espnSlug = "eng.1",
        totalRounds = 38,
        relegationSpots = 3,
        championsSpots = 4,
        secondaryContSpots = 6
    ),
    LA_LIGA(
        label = "La Liga",
        shortLabel = "La Liga",
        code = "LA_LIGA",
        country = "Espanha",
        espnSlug = "esp.1",
        totalRounds = 38,
        relegationSpots = 3,
        championsSpots = 4,
        secondaryContSpots = 6
    ),
    SERIE_A_ITA(
        label = "Serie A Itália",
        shortLabel = "Serie A ITA",
        code = "SERIE_A_ITA",
        country = "Itália",
        espnSlug = "ita.1",
        totalRounds = 38,
        relegationSpots = 3,
        championsSpots = 4,
        secondaryContSpots = 6
    ),
    BUNDESLIGA(
        label = "Bundesliga",
        shortLabel = "Bundesliga",
        code = "BUNDESLIGA",
        country = "Alemanha",
        espnSlug = "ger.1",
        totalRounds = 34,
        relegationSpots = 3,
        championsSpots = 4,
        secondaryContSpots = 6
    ),
    LIGUE_1(
        label = "Ligue 1",
        shortLabel = "Ligue 1",
        code = "LIGUE_1",
        country = "França",
        espnSlug = "fra.1",
        totalRounds = 34,
        relegationSpots = 3,
        championsSpots = 4,
        secondaryContSpots = 6
    ),
    LIGA_ARGENTINA(
        label = "Liga Argentina",
        shortLabel = "Liga Argentina",
        code = "LIGA_ARGENTINA",
        country = "Argentina",
        espnSlug = "arg.1",
        totalRounds = 38,
        relegationSpots = 4,
        championsSpots = 4,
        secondaryContSpots = 8
    );

    companion object {
        fun fromCode(code: String): Division =
            entries.firstOrNull { it.code == code } ?: SERIE_A
    }
}

enum class PlayerPosition(val code: String, val displayName: String, val sectorOrder: Int) {
    GOL("GOL", "Goleiro", 0),
    ZAG("ZAG", "Zagueiro", 1),
    LAT("LAT", "Lateral", 2),
    MEI("MEI", "Meia", 3),
    ATA("ATA", "Atacante", 4)
}

enum class PlayerTrait(val label: String) {
    VELOCIDADE("Velocidade"),
    FINALIZACAO("Finalização"),
    PASSE("Passe"),
    CABECEIO("Cabeceio"),
    DESARME("Desarme"),
    REFLEXO("Reflexo"),
    LIDERANCA("Liderança")
}

enum class Formation(
    val code: String,
    val defenders: Int,
    val midfielders: Int,
    val forwards: Int
) {
    F_4_4_2("4-4-2", 4, 4, 2),
    F_4_3_3("4-3-3", 4, 3, 3),
    F_4_2_3_1("4-2-3-1", 4, 5, 1),
    F_3_5_2("3-5-2", 3, 5, 2),
    F_5_3_2("5-3-2", 5, 3, 2),
    F_4_1_4_1("4-1-4-1", 4, 5, 1);

    companion object {
        fun fromCode(code: String): Formation =
            entries.firstOrNull { it.code == code } ?: F_4_3_3
    }
}

enum class Mentality(val code: String, val label: String, val attackMod: Double, val defenseMod: Double) {
    DEFENSIVO("DEFENSIVO", "Retranca / Defensivo", 0.86, 1.15),
    EQUILIBRADO("EQUILIBRADO", "Equilibrado", 1.0, 1.0),
    OFENSIVO("OFENSIVO", "Ofensivo", 1.16, 0.87),
    CONTRA_ATAQUE("CONTRA_ATAQUE", "Contra-Ataque", 1.06, 1.05);

    companion object {
        fun fromCode(code: String): Mentality =
            entries.firstOrNull { it.code == code } ?: EQUILIBRADO
    }
}

enum class MarkingStyle(val code: String, val label: String, val energyDrain: Double, val foulRisk: Double) {
    ZONAL("ZONAL", "Marcação Zonal", 1.0, 1.0),
    PRESSAO("PRESSAO", "Pressão Alta", 1.25, 1.2),
    INDIVIDUAL("INDIVIDUAL", "Homem a Homem", 1.12, 1.35);

    companion object {
        fun fromCode(code: String): MarkingStyle =
            entries.firstOrNull { it.code == code } ?: ZONAL
    }
}

enum class TicketPriceLevel(val code: String, val label: String, val priceReais: Int, val attendanceFactor: Double) {
    POPULAR("POPULAR", "Popular (R$ 40)", 40, 1.18),
    NORMAL("NORMAL", "Normal (R$ 75)", 75, 0.92),
    CARO("CARO", "Caro (R$ 130)", 130, 0.68);

    companion object {
        fun fromCode(code: String): TicketPriceLevel =
            entries.firstOrNull { it.code == code } ?: NORMAL
    }
}

enum class TrainingFocus(val code: String, val label: String, val description: String) {
    EQUILIBRADO("EQUILIBRADO", "Equilibrado", "Mantém forma física e evolução geral"),
    ATAQUE("ATAQUE", "Finalização & Ataque", "+Moral/evolução para meias e atacantes"),
    DEFESA("DEFESA", "Sistema Defensivo", "+Moral/evolução para goleiros e defensores"),
    FISICO("FISICO", "Recuperação Física", "Recupera +8% energia extra entre rodadas"),
    BASE("BASE", "Categorias de Base", "Acelera evolução de atletas Sub-21");

    companion object {
        fun fromCode(code: String): TrainingFocus =
            entries.firstOrNull { it.code == code } ?: EQUILIBRADO
    }
}

enum class InjurySeverity(val label: String, val minRounds: Int, val maxRounds: Int) {
    LEVE("Leve", 1, 2),
    MODERADA("Moderada", 2, 4),
    GRAVE("Grave", 4, 7)
}

enum class MedicalTreatmentType(val label: String, val description: String, val costReais: Long, val roundsReduced: Int) {
    CONSERVADOR("Repouso Padrão", "Recuperação natural no DM sem custo extra", 0L, 0),
    FISIOTERAPIA_INTENSIVA("Fisioterapia Intensiva", "Reduz 1 rodada do tempo estimado (-1 rod.)", 450_000L, 1),
    TRATAMENTO_ESPECIALIZADO("Centro Médico Avançado", "Reduz 2 rodadas + recupera +15% energia", 950_000L, 2)
}

data class InjuryRecord(
    val id: String,
    val seasonYear: Long,
    val roundOccurred: Int,
    val injuryName: String,
    val severity: InjurySeverity,
    val estimatedRounds: Int,
    val remainingRounds: Int,
    val treatmentApplied: Boolean = false,
    val recoveredRound: Int? = null
)

data class Player(
    val id: String,
    val name: String,
    val position: PlayerPosition,
    val overall: Int,
    val age: Int,
    val nationality: String = "Brasil",
    val shirtNumber: Int = 10,
    val salary: Long,
    val marketValue: Long,
    val traits: List<PlayerTrait> = emptyList(),
    val energy: Int = 100,
    val morale: Int = 80,
    val yellowCards: Int = 0,
    val suspendedRounds: Int = 0,
    val injuredRounds: Int = 0,
    val goalsSeason: Int = 0,
    val assistsSeason: Int = 0,
    val isStarter: Boolean = false,
    val forSale: Boolean = false,
    val espnAthleteId: String? = null,
    val contractRounds: Int = 38,
    val potential: Int = overall,
    val currentInjuryName: String? = null,
    val currentInjurySeverity: InjurySeverity? = null,
    val injuryEstimatedRounds: Int = 0,
    val injuryTreatedThisInjury: Boolean = false,
    val injuryHistory: List<InjuryRecord> = emptyList()
) {
    val isAvailable: Boolean get() = suspendedRounds == 0 && injuredRounds == 0
    val effectiveOverall: Int
        get() {
            val energyFactor = 0.72 + (energy.coerceIn(20, 100) / 100.0) * 0.28
            val moraleBonus = if (morale >= 85) 2 else if (morale <= 45) -2 else 0
            return (overall * energyFactor + moraleBonus).toInt().coerceIn(35, 99)
        }
}

data class Club(
    val id: String,
    val name: String,
    val shortName: String,
    val division: Division,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val stadiumName: String,
    val stadiumCapacity: Int,
    val prestige: Int, // 1..5 stars
    val budget: Long,
    val espnTeamId: String = "",
    val crestUrl: String? = null,
    val players: List<Player> = emptyList(),
    val points: Int = 0,
    val played: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val form: List<String> = emptyList(),
    val copaEliminated: Boolean = false
) {
    val goalDifference: Int get() = goalsFor - goalsAgainst

    val averageOverall: Int
        get() {
            val starters = players.filter { it.isStarter }.ifEmpty { players.sortedByDescending { it.overall }.take(11) }
            return if (starters.isEmpty()) 65 else starters.map { it.overall }.average().toInt()
        }

    val weeklyWages: Long
        get() = players.sumOf { it.salary }
}

enum class CompetitionType(val label: String) {
    BRASILEIRAO_A("Brasileirão Série A"),
    BRASILEIRAO_B("Brasileirão Série B"),
    BRASILEIRAO_C("Brasileirão Série C"),
    BRASILEIRAO_D("Brasileirão Série D"),
    PREMIER_LEAGUE("Premier League"),
    LA_LIGA("La Liga"),
    SERIE_A_ITA("Serie A Itália"),
    BUNDESLIGA("Bundesliga"),
    LIGUE_1("Ligue 1"),
    LIGA_ARGENTINA("Liga Argentina"),
    COPA_DO_BRASIL("Copa do Brasil"),
    LIBERTADORES("Copa Libertadores"),
    COPA_SUL_AMERICANA("Copa Sul-Americana"),
    CHAMPIONS_LEAGUE("UEFA Champions League");

    companion object {
        fun fromDivision(division: Division): CompetitionType = when (division) {
            Division.SERIE_A -> BRASILEIRAO_A
            Division.SERIE_B -> BRASILEIRAO_B
            Division.SERIE_C -> BRASILEIRAO_C
            Division.SERIE_D -> BRASILEIRAO_D
            Division.PREMIER_LEAGUE -> PREMIER_LEAGUE
            Division.LA_LIGA -> LA_LIGA
            Division.SERIE_A_ITA -> SERIE_A_ITA
            Division.BUNDESLIGA -> BUNDESLIGA
            Division.LIGUE_1 -> LIGUE_1
            Division.LIGA_ARGENTINA -> LIGA_ARGENTINA
        }
    }
}

enum class MatchEventType {
    GOAL, YELLOW_CARD, RED_CARD, SUBSTITUTION, CHANCE, SAVE, INJURY, KICKOFF, HALFTIME, FULLTIME
}

data class MatchEvent(
    val minute: Int,
    val type: MatchEventType,
    val clubId: String,
    val playerName: String,
    val assistName: String? = null,
    val description: String
)

data class Fixture(
    val id: String,
    val round: Int,
    val division: Division,
    val homeClubId: String,
    val awayClubId: String,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val played: Boolean = false,
    val competition: CompetitionType = CompetitionType.fromDivision(division),
    val events: List<MatchEvent> = emptyList(),
    val attendance: Int = 0,
    val revenue: Long = 0L
)

data class LiveFixtureState(
    val fixtureId: String,
    val homeClubId: String,
    val homeName: String,
    val homeShort: String,
    val homeColor: Long,
    val awayClubId: String,
    val awayName: String,
    val awayShort: String,
    val awayColor: Long,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val lastGoalText: String? = null,
    val homeRedCards: Int = 0,
    val awayRedCards: Int = 0
)

data class LiveMatchState(
    val round: Int,
    val competition: CompetitionType,
    val minute: Int = 0,
    val phase: String = "Pré-Jogo",
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val speed: Int = 2,
    val userIsHome: Boolean = true,
    val userMatch: LiveFixtureState,
    val otherMatches: List<LiveFixtureState> = emptyList(),
    val events: List<MatchEvent> = emptyList(),
    val possessionHome: Int = 50,
    val shotsHome: Int = 0,
    val shotsAway: Int = 0,
    val cornersHome: Int = 0,
    val cornersAway: Int = 0,
    val attendance: Int = 0,
    val ticketRevenue: Long = 0L,
    val substitutionsUsed: Int = 0,
    val maxSubstitutions: Int = 5,
    val manOfTheMatch: String? = null,
    val isKnockout: Boolean = false,
    val cupType: CupType? = null,
    val cupTieId: String? = null,
    val cupStageLabel: String? = null,
    val penaltyShootout: PenaltyShootoutResult? = null,
    val ballZone: Float = 0.5f,
    val championsFixtureId: String? = null,
    val continentalFixtureId: String? = null,
    val continentalCupType: ContinentalCupType? = null
)

data class IncomingTransferOffer(
    val id: String,
    val player: Player,
    val offeringClubId: String,
    val offeringClubName: String,
    val offerAmount: Long
)

data class SquadSyncEntry(
    val clubName: String,
    val action: String, // "NOVO", "ATUALIZADO", "SAÍDA"
    val playerName: String,
    val position: String,
    val detail: String
)

data class SquadSyncReport(
    val timestamp: Long,
    val sourceLabel: String,
    val isLiveNetwork: Boolean,
    val clubsSynced: Int,
    val playersAdded: Int,
    val playersUpdated: Int,
    val playersRemoved: Int,
    val entries: List<SquadSyncEntry>,
    val summary: String
)

enum class NewsCategory(val label: String) {
    TRANSFER("Mercado"),
    MATCH_RESULT("Resultados"),
    CLUB_BUZZ("Bastidores"),
    SYNC_UPDATE("Elencos Reais")
}

data class NewsItem(
    val id: String,
    val category: NewsCategory,
    val round: Int,
    val headline: String,
    val body: String,
    val clubId: String? = null,
    val clubName: String? = null,
    val clubShortName: String? = null,
    val clubColorHex: Long? = null,
    val clubSecondaryColorHex: Long? = null,
    val isBreaking: Boolean = false,
    val tagLabel: String = ""
)

data class MonthlyLedgerRecord(
    val monthIndex: Int,
    val ticketIncome: Long = 0L,
    val salaryExpense: Long = 0L,
    val otherDelta: Long = 0L,
    val roundsPlayed: Int = 0,
    val homeMatchesPlayed: Int = 0,
    val endingBudget: Long? = null
)

data class MonthlyFinancePoint(
    val monthIndex: Int,
    val monthLabel: String,
    val ticketIncome: Long,
    val salaryExpense: Long,
    val netCashFlow: Long,
    val budgetBalance: Long,
    val homeMatches: Int,
    val isProjected: Boolean
)

data class PenaltyShootoutResult(
    val homePenalties: Int,
    val awayPenalties: Int,
    val winnerClubId: String,
    val summary: String
)

enum class CupType(
    val competition: CompetitionType,
    val title: String,
    val stageNames: List<String>
) {
    COPA_DO_BRASIL(
        competition = CompetitionType.COPA_DO_BRASIL,
        title = "Copa do Brasil",
        stageNames = listOf("Oitavas de Final", "Quartas de Final", "Semifinal", "Final")
    ),
    LIBERTADORES(
        competition = CompetitionType.LIBERTADORES,
        title = "Copa Libertadores",
        stageNames = listOf("Oitavas de Final", "Quartas de Final", "Semifinal", "Final")
    ),
    CHAMPIONS(
        competition = CompetitionType.CHAMPIONS_LEAGUE,
        title = "UEFA Champions League",
        stageNames = listOf("Oitavas de Final", "Quartas de Final", "Semifinal", "Final")
    )
}

data class CupMatchTie(
    val id: String,
    val cupType: CupType,
    val stageIndex: Int,
    val stageLabel: String,
    val homeClubId: String,
    val awayClubId: String,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val winnerClubId: String? = null,
    val shootout: PenaltyShootoutResult? = null,
    val triggerAfterLeagueRound: Int = 6
)

data class CupCompetitionState(
    val cupType: CupType,
    val currentStageIndex: Int = 0,
    val championClubId: String? = null,
    val ties: List<CupMatchTie> = emptyList()
)

data class SponsorDeal(
    val id: String,
    val name: String,
    val tagline: String,
    val perRoundBase: Long,
    val winBonus: Long,
    val titleBonus: Long,
    val minPrestige: Int = 1
)

data class YouthProspect(
    val id: String,
    val name: String,
    val position: PlayerPosition,
    val overall: Int,
    val potential: Int,
    val age: Int,
    val salary: Long,
    val marketValue: Long,
    val traits: List<PlayerTrait> = emptyList()
)

data class ManagerJobOffer(
    val id: String,
    val clubId: String,
    val clubName: String,
    val division: Division,
    val budget: Long,
    val avgOverall: Int,
    val prestige: Int,
    val reason: String
)

data class SeasonAwardSummary(
    val seasonYear: Long,
    val divisionLabel: String,
    val championClubName: String,
    val userFinishPos: Int,
    val prizeMoney: Long,
    val goldenBootName: String,
    val goldenBootClub: String,
    val goldenBootGoals: Int,
    val playmakerName: String,
    val playmakerClub: String,
    val playmakerAssists: Int,
    val bestPlayerName: String,
    val bestPlayerClub: String,
    val managerOfTheYear: String
)

data class TransferWindowStatus(
    val isOpen: Boolean,
    val windowName: String,
    val statusLabel: String,
    val roundsRemaining: Int,
    val openRangesText: String
)

enum class ChampionsLeaguePhase(val label: String) {
    LEAGUE_PHASE("Fase de Liga (36 Clubes)"),
    PLAYOFFS("Playoffs (9º–24º)"),
    ROUND_OF_16("Oitavas de Final"),
    QUARTER_FINALS("Quartas de Final"),
    SEMI_FINALS("Semifinal"),
    FINAL("Grande Final"),
    FINISHED("Encerrada")
}

data class ChampionsLeagueStanding(
    val clubId: String,
    val played: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val points: Int = 0,
    val pot: Int = 1
) {
    val goalDifference: Int get() = goalsFor - goalsAgainst
}

data class ChampionsLeagueFixture(
    val id: String,
    val matchday: Int,
    val homeClubId: String,
    val awayClubId: String,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val played: Boolean = false,
    val triggerAfterLeagueRound: Int
)

data class ChampionsLeagueState(
    val phase: ChampionsLeaguePhase = ChampionsLeaguePhase.LEAGUE_PHASE,
    val currentMatchday: Int = 1,
    val standings: List<ChampionsLeagueStanding> = emptyList(),
    val leagueFixtures: List<ChampionsLeagueFixture> = emptyList(),
    val knockoutTies: List<CupMatchTie> = emptyList(),
    val currentKnockoutStageIndex: Int = 0,
    val championClubId: String? = null
)

data class BallonDorCandidate(
    val playerId: String,
    val playerName: String,
    val position: PlayerPosition,
    val clubId: String,
    val clubName: String,
    val clubShortName: String,
    val divisionLabel: String,
    val age: Int,
    val overall: Int,
    val goals: Int,
    val assists: Int,
    val score: Int
)

data class AnnualAwardsRecord(
    val seasonYear: Long,
    val ballonDorTop3: List<BallonDorCandidate>,
    val goldenBoot: BallonDorCandidate?,
    val topPlaymaker: BallonDorCandidate?,
    val bestGoalkeeper: BallonDorCandidate?,
    val bestYoungPlayer: BallonDorCandidate?,
    val bestManagerName: String,
    val bestManagerClubName: String,
    val teamOfTheYear: List<BallonDorCandidate> = emptyList()
)

enum class ContinentalCupType(
    val competition: CompetitionType,
    val title: String,
    val basePrizePerWin: Long,
    val titlePrize: Long
) {
    LIBERTADORES(
        competition = CompetitionType.LIBERTADORES,
        title = "Copa Libertadores",
        basePrizePerWin = 1_650_000L,
        titlePrize = 22_000_000L
    ),
    SUL_AMERICANA(
        competition = CompetitionType.COPA_SUL_AMERICANA,
        title = "Copa Sul-Americana",
        basePrizePerWin = 900_000L,
        titlePrize = 11_000_000L
    )
}

enum class ContinentalPhase(val label: String) {
    GROUP_STAGE("Fase de Grupos (8 Grupos)"),
    ROUND_OF_16("Oitavas de Final"),
    QUARTER_FINALS("Quartas de Final"),
    SEMI_FINALS("Semifinal"),
    FINAL("Grande Final"),
    FINISHED("Encerrada")
}

data class ContinentalGroupStanding(
    val clubId: String,
    val groupName: String, // "A".."H"
    val played: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val points: Int = 0
) {
    val goalDifference: Int get() = goalsFor - goalsAgainst
}

data class ContinentalGroupFixture(
    val id: String,
    val cupType: ContinentalCupType,
    val groupName: String,
    val matchday: Int, // 1..6
    val homeClubId: String,
    val awayClubId: String,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val played: Boolean = false,
    val triggerAfterLeagueRound: Int
)

data class ContinentalTournamentState(
    val cupType: ContinentalCupType,
    val phase: ContinentalPhase = ContinentalPhase.GROUP_STAGE,
    val currentMatchday: Int = 1,
    val standings: List<ContinentalGroupStanding> = emptyList(),
    val groupFixtures: List<ContinentalGroupFixture> = emptyList(),
    val knockoutTies: List<CupMatchTie> = emptyList(),
    val currentKnockoutStageIndex: Int = 0,
    val championClubId: String? = null
)

enum class FfpComplianceLevel(val label: String) {
    COMPLIANT("Regular (Aprovado)"),
    WARNING("Alerta de Fair Play"),
    BREACH("Violação FFP / FIFA")
}

data class FairPlayState(
    val compliance: FfpComplianceLevel = FfpComplianceLevel.COMPLIANT,
    val seasonNetResult: Long = 0L,
    val wageToIncomeRatioPct: Int = 50,
    val allowedDeficitLimit: Long = 15_000_000L,
    val transferBanRounds: Int = 0,
    val pointsDeducted: Int = 0,
    val totalFinesPaid: Long = 0L,
    val activePunishmentNote: String? = null
)

data class PresidentialCandidate(
    val id: String,
    val name: String,
    val platform: String,
    val styleLabel: String,
    val budgetGrant: Long,
    val boardStrictness: Int = 2,
    val projectedVoteShare: Int = 34
)

data class ClubGovernanceState(
    val presidentName: String = "Roberto Andrade",
    val presidentStyle: String = "Equilibrado",
    val councilRating: Int = 74,
    val councilVerdict: String = "Gestão estável e alinhada às metas do estatuto",
    val seasonObjectiveLabel: String = "Brigar pela parte de cima da tabela",
    val seasonObjectiveMaxPos: Int = 8,
    val objectiveFailedPunished: Boolean = false,
    val electionPending: Boolean = false,
    val nextElectionYear: Long = 2030L,
    val lastElectionYear: Long? = null,
    val electionCandidates: List<PresidentialCandidate> = emptyList(),
    val lastElectionSummary: String? = null
)

data class SeasonSnapshot(
    val clubs: List<Club>,
    val fixtures: List<Fixture>,
    val monthlyLedger: Map<Int, MonthlyLedgerRecord>,
    val seasonTicketRevenue: Long,
    val cups: List<CupCompetitionState> = emptyList(),
    val activeSponsor: SponsorDeal? = null,
    val youthProspects: List<YouthProspect> = emptyList(),
    val pendingJobOffers: List<ManagerJobOffer> = emptyList(),
    val isSacked: Boolean = false,
    val championsLeague: ChampionsLeagueState? = null,
    val annualAwardsHistory: List<AnnualAwardsRecord> = emptyList(),
    val continentalTournaments: List<ContinentalTournamentState> = emptyList(),
    val fairPlay: FairPlayState = FairPlayState(),
    val governance: ClubGovernanceState = ClubGovernanceState(),
    val userBallonDorPlayerId: String? = null,
    val userBallonDorVoteSeason: Long = 0L
)



