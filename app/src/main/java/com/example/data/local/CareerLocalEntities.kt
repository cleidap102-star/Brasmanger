package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_manager_profile")
data class LocalManagerProfileEntity(
    @PrimaryKey val userId: String,
    val managerName: String,
    val autoSyncEnabled: Boolean,
    val preferredSimSpeed: Long,
    val totalTitles: Long,
    val totalWins: Long,
    val lastSquadSyncAt: Long?,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

@Entity(tableName = "local_careers")
data class LocalCareerEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val clubId: String,
    val clubName: String,
    val division: String,
    val seasonYear: Long,
    val currentRound: Long,
    val budget: Long,
    val stadiumCapacity: Long,
    val ticketPriceLevel: String,
    val trainingFocus: String,
    val formation: String,
    val mentality: String,
    val marking: String,
    val boardConfidence: Long,
    val points: Long,
    val leaguePosition: Long,
    val wins: Long,
    val draws: Long,
    val losses: Long,
    val goalsFor: Long,
    val goalsAgainst: Long,
    val copaStage: String,
    val activeStarterIdsCsv: String,
    val status: String,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

@Entity(tableName = "local_transfers")
data class LocalTransferEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val careerId: String,
    val playerName: String,
    val position: String,
    val fromClubName: String,
    val toClubName: String,
    val fee: Long,
    val transferType: String,
    val seasonYear: Long,
    val roundNumber: Long,
    val createdAtMs: Long
)

@Entity(tableName = "local_trophies")
data class LocalTrophyEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val careerId: String,
    val competitionName: String,
    val clubName: String,
    val seasonYear: Long,
    val finalPosition: Long,
    val createdAtMs: Long
)

@Entity(tableName = "local_season_snapshots")
data class LocalSeasonSnapshotEntity(
    @PrimaryKey val careerId: String,
    val snapshotJson: String,
    val updatedAtMs: Long
)

