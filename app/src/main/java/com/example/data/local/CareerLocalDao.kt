package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CareerLocalDao {
    @Query("SELECT * FROM local_manager_profile WHERE userId = :userId LIMIT 1")
    fun observeManagerProfile(userId: String): Flow<LocalManagerProfileEntity?>

    @Query("SELECT * FROM local_manager_profile WHERE userId = :userId LIMIT 1")
    suspend fun getManagerProfile(userId: String): LocalManagerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManagerProfile(profile: LocalManagerProfileEntity)

    @Query("SELECT * FROM local_careers WHERE userId = :userId ORDER BY updatedAtMs DESC")
    fun observeCareers(userId: String): Flow<List<LocalCareerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCareer(career: LocalCareerEntity)

    @Update
    suspend fun updateCareer(career: LocalCareerEntity)

    @Query("DELETE FROM local_careers WHERE id = :careerId")
    suspend fun deleteCareer(careerId: String)

    @Query("SELECT * FROM local_transfers WHERE careerId = :careerId ORDER BY createdAtMs DESC")
    fun observeTransfers(careerId: String): Flow<List<LocalTransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: LocalTransferEntity)

    @Query("DELETE FROM local_transfers WHERE careerId = :careerId")
    suspend fun deleteTransfersForCareer(careerId: String)

    @Query("SELECT * FROM local_trophies WHERE careerId = :careerId ORDER BY seasonYear DESC")
    fun observeTrophies(careerId: String): Flow<List<LocalTrophyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrophy(trophy: LocalTrophyEntity)

    @Query("DELETE FROM local_trophies WHERE careerId = :careerId")
    suspend fun deleteTrophiesForCareer(careerId: String)

    @Query("SELECT * FROM local_season_snapshots WHERE careerId = :careerId LIMIT 1")
    suspend fun getSeasonSnapshot(careerId: String): LocalSeasonSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeasonSnapshot(snapshot: LocalSeasonSnapshotEntity)

    @Query("DELETE FROM local_season_snapshots WHERE careerId = :careerId")
    suspend fun deleteSeasonSnapshot(careerId: String)
}

