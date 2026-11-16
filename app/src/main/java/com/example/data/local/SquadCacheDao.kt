package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SquadCacheDao {
    @Query("SELECT * FROM synced_players")
    suspend fun getAllSyncedPlayers(): List<SyncedPlayerEntity>

    @Query("SELECT * FROM synced_players WHERE clubId = :clubId")
    suspend fun getSyncedPlayersForClub(clubId: String): List<SyncedPlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncedPlayers(players: List<SyncedPlayerEntity>)

    @Query("DELETE FROM synced_players WHERE clubId = :clubId")
    suspend fun deleteSyncedPlayersForClub(clubId: String)

    @Query("SELECT * FROM synced_club_meta")
    suspend fun getAllClubMeta(): List<SyncedClubMetaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClubMeta(meta: List<SyncedClubMetaEntity>)

    @Query("SELECT * FROM sync_logs ORDER BY timestampMs DESC LIMIT 20")
    fun observeSyncLogs(): Flow<List<SyncLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLogEntity)
}
