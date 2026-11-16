package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SyncedPlayerEntity::class,
        SyncedClubMetaEntity::class,
        SyncLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BrasManagerDatabase : RoomDatabase() {
    abstract fun squadCacheDao(): SquadCacheDao

    companion object {
        @Volatile
        private var INSTANCE: BrasManagerDatabase? = null

        fun getInstance(context: Context): BrasManagerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BrasManagerDatabase::class.java,
                    "brasmanager_cache.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
