package com.example.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ManagerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val managerName: String = "Técnico",
    val autoSyncEnabled: Boolean = true,
    val preferredSimSpeed: Long = 2L,
    val totalTitles: Long = 0L,
    val totalWins: Long = 0L,
    val lastSquadSyncAt: Long? = null,
    @ServerTimestampCompat val createdAt: Timestamp? = null,
    @ServerTimestampCompat val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("managerName", managerName.trim().ifBlank { "Técnico" }.take(60))
        put("autoSyncEnabled", autoSyncEnabled)
        put("preferredSimSpeed", preferredSimSpeed.let { if (it in listOf(1L, 2L, 4L)) it else 2L })
        put("totalTitles", totalTitles.coerceAtLeast(0L))
        put("totalWins", totalWins.coerceAtLeast(0L))
        lastSquadSyncAt?.let { put("lastSquadSyncAt", it.coerceAtLeast(0L)) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

typealias ServerTimestampCompat = com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CareerSave(
    @DocumentId val id: String = "",
    val userId: String = "",
    val clubId: String = "",
    val clubName: String = "",
    val division: String = "SERIE_A",
    val seasonYear: Long = 2026L,
    val currentRound: Long = 1L,
    val budget: Long = 25_000_000L,
    val stadiumCapacity: Long = 40_000L,
    val ticketPriceLevel: String = "NORMAL",
    val trainingFocus: String = "EQUILIBRADO",
    val formation: String = "4-3-3",
    val mentality: String = "EQUILIBRADO",
    val marking: String = "ZONAL",
    val boardConfidence: Long = 75L,
    val points: Long = 0L,
    val leaguePosition: Long = 1L,
    val wins: Long = 0L,
    val draws: Long = 0L,
    val losses: Long = 0L,
    val goalsFor: Long = 0L,
    val goalsAgainst: Long = 0L,
    val copaStage: String = "3ª Fase",
    val activeStarterIds: List<String> = emptyList(),
    val status: String = "active",
    @ServerTimestampCompat val createdAt: Timestamp? = null,
    @ServerTimestampCompat val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "clubId" to clubId,
        "clubName" to clubName.take(64),
        "division" to division,
        "seasonYear" to seasonYear,
        "currentRound" to currentRound,
        "budget" to budget,
        "stadiumCapacity" to stadiumCapacity,
        "ticketPriceLevel" to ticketPriceLevel,
        "trainingFocus" to trainingFocus,
        "formation" to formation,
        "mentality" to mentality,
        "marking" to marking,
        "boardConfidence" to boardConfidence.coerceIn(0L, 100L),
        "points" to points.coerceAtLeast(0L),
        "leaguePosition" to leaguePosition.coerceIn(1L, 20L),
        "wins" to wins.coerceAtLeast(0L),
        "draws" to draws.coerceAtLeast(0L),
        "losses" to losses.coerceAtLeast(0L),
        "goalsFor" to goalsFor.coerceAtLeast(0L),
        "goalsAgainst" to goalsAgainst.coerceAtLeast(0L),
        "copaStage" to copaStage.take(48),
        "activeStarterIds" to activeStarterIds.take(11),
        "status" to "active",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "clubId" to clubId,
        "clubName" to clubName.take(64),
        "division" to division,
        "seasonYear" to seasonYear,
        "currentRound" to currentRound,
        "budget" to budget,
        "stadiumCapacity" to stadiumCapacity,
        "ticketPriceLevel" to ticketPriceLevel,
        "trainingFocus" to trainingFocus,
        "formation" to formation,
        "mentality" to mentality,
        "marking" to marking,
        "boardConfidence" to boardConfidence.coerceIn(0L, 100L),
        "points" to points.coerceAtLeast(0L),
        "leaguePosition" to leaguePosition.coerceIn(1L, 20L),
        "wins" to wins.coerceAtLeast(0L),
        "draws" to draws.coerceAtLeast(0L),
        "losses" to losses.coerceAtLeast(0L),
        "goalsFor" to goalsFor.coerceAtLeast(0L),
        "goalsAgainst" to goalsAgainst.coerceAtLeast(0L),
        "copaStage" to copaStage.take(48),
        "activeStarterIds" to activeStarterIds.take(11),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class TransferRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val careerId: String = "",
    val playerName: String = "",
    val position: String = "MEI",
    val fromClubName: String = "",
    val toClubName: String = "",
    val fee: Long = 0L,
    val transferType: String = "BUY",
    val seasonYear: Long = 2026L,
    val roundNumber: Long = 1L,
    @ServerTimestampCompat val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "careerId" to careerId,
        "playerName" to playerName.take(80),
        "position" to position,
        "fromClubName" to fromClubName.take(64),
        "toClubName" to toClubName.take(64),
        "fee" to fee.coerceAtLeast(0L),
        "transferType" to transferType,
        "seasonYear" to seasonYear,
        "roundNumber" to roundNumber,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class TrophyRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val careerId: String = "",
    val competitionName: String = "",
    val clubName: String = "",
    val seasonYear: Long = 2026L,
    val finalPosition: Long = 1L,
    @ServerTimestampCompat val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "careerId" to careerId,
        "competitionName" to competitionName.take(80),
        "clubName" to clubName.take(64),
        "seasonYear" to seasonYear,
        "finalPosition" to finalPosition.coerceIn(1L, 20L),
        "createdAt" to FieldValue.serverTimestamp()
    )
}
