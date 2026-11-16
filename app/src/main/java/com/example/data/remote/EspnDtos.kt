package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EspnTeamsResponse(
    val sports: List<EspnSportDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnSportDto(
    val leagues: List<EspnLeagueDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnLeagueDto(
    val teams: List<EspnTeamWrapperDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnTeamWrapperDto(
    val team: EspnTeamDto? = null
)

@JsonClass(generateAdapter = true)
data class EspnTeamDto(
    val id: String? = null,
    val displayName: String? = null,
    val shortDisplayName: String? = null,
    val abbreviation: String? = null,
    val logos: List<EspnLogoDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnLogoDto(
    val href: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnRosterResponse(
    val athletes: List<EspnAthleteDto>? = null
)

@JsonClass(generateAdapter = true)
data class EspnAthleteDto(
    val id: String? = null,
    val fullName: String? = null,
    val displayName: String? = null,
    val shortName: String? = null,
    val age: Int? = null,
    val jersey: String? = null,
    val citizenship: String? = null,
    val position: EspnPositionDto? = null
)

@JsonClass(generateAdapter = true)
data class EspnPositionDto(
    val abbreviation: String? = null,
    val displayName: String? = null
)
