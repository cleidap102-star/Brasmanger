package com.example.domain.model

enum class Division(val label: String, val code: String) {
    SERIE_A("Brasileirão Série A", "SERIE_A"),
    SERIE_B("Brasileirão Série B", "SERIE_B")
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
    val espnAthleteId: String? = null
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
    COPA_DO_BRASIL("Copa do Brasil")
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
    val competition: CompetitionType = if (division == Division.SERIE_A) CompetitionType.BRASILEIRAO_A else CompetitionType.BRASILEIRAO_B,
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
    val manOfTheMatch: String? = null
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
