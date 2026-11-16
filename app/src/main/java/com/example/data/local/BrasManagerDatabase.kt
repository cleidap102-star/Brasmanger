package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SyncedPlayerEntity::class,
        SyncedClubMetaEntity::class,
        SyncLogEntity::class,
        LocalManagerProfileEntity::class,
        LocalCareerEntity::class,
        LocalTransferEntity::class,
        LocalTrophyEntity::class,
        LocalSeasonSnapshotEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class BrasManagerDatabase : RoomDatabase() {
    abstract fun squadCacheDao(): SquadCacheDao
    abstract fun careerLocalDao(): CareerLocalDao

    companion object {
        @Volatile
        private var INSTANCE: BrasManagerDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_manager_profile` (
                        `userId` TEXT NOT NULL,
                        `managerName` TEXT NOT NULL,
                        `autoSyncEnabled` INTEGER NOT NULL,
                        `preferredSimSpeed` INTEGER NOT NULL,
                        `totalTitles` INTEGER NOT NULL,
                        `totalWins` INTEGER NOT NULL,
                        `lastSquadSyncAt` INTEGER,
                        `createdAtMs` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_careers` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `clubId` TEXT NOT NULL,
                        `clubName` TEXT NOT NULL,
                        `division` TEXT NOT NULL,
                        `seasonYear` INTEGER NOT NULL,
                        `currentRound` INTEGER NOT NULL,
                        `budget` INTEGER NOT NULL,
                        `stadiumCapacity` INTEGER NOT NULL,
                        `ticketPriceLevel` TEXT NOT NULL,
                        `trainingFocus` TEXT NOT NULL,
                        `formation` TEXT NOT NULL,
                        `mentality` TEXT NOT NULL,
                        `marking` TEXT NOT NULL,
                        `boardConfidence` INTEGER NOT NULL,
                        `points` INTEGER NOT NULL,
                        `leaguePosition` INTEGER NOT NULL,
                        `wins` INTEGER NOT NULL,
                        `draws` INTEGER NOT NULL,
                        `losses` INTEGER NOT NULL,
                        `goalsFor` INTEGER NOT NULL,
                        `goalsAgainst` INTEGER NOT NULL,
                        `copaStage` TEXT NOT NULL,
                        `activeStarterIdsCsv` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_transfers` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `careerId` TEXT NOT NULL,
                        `playerName` TEXT NOT NULL,
                        `position` TEXT NOT NULL,
                        `fromClubName` TEXT NOT NULL,
                        `toClubName` TEXT NOT NULL,
                        `fee` INTEGER NOT NULL,
                        `transferType` TEXT NOT NULL,
                        `seasonYear` INTEGER NOT NULL,
                        `roundNumber` INTEGER NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_trophies` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `careerId` TEXT NOT NULL,
                        `competitionName` TEXT NOT NULL,
                        `clubName` TEXT NOT NULL,
                        `seasonYear` INTEGER NOT NULL,
                        `finalPosition` INTEGER NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_season_snapshots` (
                        `careerId` TEXT NOT NULL,
                        `snapshotJson` TEXT NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`careerId`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): BrasManagerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BrasManagerDatabase::class.java,
                    "brasmanager_cache.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
