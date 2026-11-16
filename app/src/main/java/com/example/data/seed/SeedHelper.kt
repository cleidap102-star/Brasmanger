package com.example.data.seed

import com.example.domain.model.Club
import com.example.domain.model.Division
import com.example.domain.model.Player
import com.example.domain.model.PlayerPosition
import com.example.domain.model.PlayerTrait

internal fun makePlayer(
    clubId: String,
    index: Int,
    name: String,
    pos: PlayerPosition,
    ovr: Int,
    age: Int,
    shirt: Int,
    nationality: String = "Brasil",
    traits: List<PlayerTrait> = emptyList(),
    isStarter: Boolean = false
): Player {
    val marketBase = when {
        ovr >= 83 -> 45_000_000L
        ovr >= 79 -> 24_000_000L
        ovr >= 75 -> 12_000_000L
        ovr >= 71 -> 6_000_000L
        ovr >= 67 -> 2_800_000L
        else -> 1_100_000L
    }
    val ageFactor = when {
        age <= 21 -> 1.30
        age <= 25 -> 1.15
        age <= 29 -> 1.0
        age <= 32 -> 0.72
        else -> 0.45
    }
    val value = ((marketBase * ageFactor) / 100_000L).toLong() * 100_000L
    val salary = ((value * 0.011).toLong().coerceAtLeast(25_000L) / 5_000L) * 5_000L

    return Player(
        id = "${clubId}_p${index}",
        name = name,
        position = pos,
        overall = ovr,
        age = age,
        nationality = nationality,
        shirtNumber = shirt,
        salary = salary,
        marketValue = value,
        traits = traits.ifEmpty {
            when (pos) {
                PlayerPosition.GOL -> listOf(PlayerTrait.REFLEXO)
                PlayerPosition.ZAG -> listOf(PlayerTrait.DESARME, PlayerTrait.CABECEIO)
                PlayerPosition.LAT -> listOf(PlayerTrait.VELOCIDADE, PlayerTrait.PASSE)
                PlayerPosition.MEI -> listOf(PlayerTrait.PASSE)
                PlayerPosition.ATA -> listOf(PlayerTrait.FINALIZACAO, PlayerTrait.VELOCIDADE)
            }
        },
        energy = 100,
        morale = 80,
        isStarter = isStarter
    )
}

internal fun buildClub(
    id: String,
    name: String,
    shortName: String,
    division: Division,
    primaryHex: Long,
    secondaryHex: Long,
    stadium: String,
    capacity: Int,
    prestige: Int,
    budget: Long,
    espnId: String,
    specs: List<PlayerSpec>
): Club {
    val players = specs.mapIndexed { idx, s ->
        makePlayer(
            clubId = id,
            index = idx + 1,
            name = s.name,
            pos = s.pos,
            ovr = s.ovr,
            age = s.age,
            shirt = s.shirt,
            nationality = s.nat,
            isStarter = idx < 11
        )
    }
    return Club(
        id = id,
        name = name,
        shortName = shortName,
        division = division,
        primaryColorHex = primaryHex,
        secondaryColorHex = secondaryHex,
        stadiumName = stadium,
        stadiumCapacity = capacity,
        prestige = prestige,
        budget = budget,
        espnTeamId = espnId,
        players = players
    )
}

internal data class PlayerSpec(
    val name: String,
    val pos: PlayerPosition,
    val ovr: Int,
    val age: Int,
    val shirt: Int,
    val nat: String = "Brasil"
)
