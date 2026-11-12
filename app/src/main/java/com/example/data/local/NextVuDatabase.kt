package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaybackProgressEntity::class,
        WatchHistoryEntity::class,
        WatchlistEntity::class,
        CachedCatalogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NextVuDatabase : RoomDatabase() {
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun cachedCatalogDao(): CachedCatalogDao

    companion object {
        @Volatile
        private var INSTANCE: NextVuDatabase? = null

        fun getInstance(context: Context): NextVuDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NextVuDatabase::class.java,
                    "nextvu_local_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
