package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "synced_players")
data class SyncedPlayerEntity(
    @PrimaryKey val id: String,
    val clubId: String,
    val name: String,
    val positionCode: String,
    val overall: Int,
    val age: Int,
    val nationality: String,
    val shirtNumber: Int,
    val espnAthleteId: String?,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "synced_club_meta")
data class SyncedClubMetaEntity(
    @PrimaryKey val clubId: String,
    val espnTeamId: String,
    val crestUrl: String?,
    val lastSyncedAtMs: Long
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val sourceLabel: String,
    val clubsSynced: Int,
    val playersAdded: Int,
    val playersUpdated: Int,
    val summary: String
)
