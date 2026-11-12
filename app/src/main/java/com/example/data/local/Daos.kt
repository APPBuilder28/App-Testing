package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE sourceId = :sourceId ORDER BY updatedAt DESC")
    fun observeProgressForSource(sourceId: String): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE sourceId = :sourceId AND contentType = :contentType AND contentId = :contentId LIMIT 1")
    suspend fun getProgress(sourceId: String, contentType: String, contentId: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress WHERE sourceId = :sourceId AND seriesId = :seriesId")
    fun observeSeriesEpisodesProgress(sourceId: String, seriesId: String): Flow<List<PlaybackProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(entity: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress WHERE sourceId = :sourceId")
    suspend fun clearProgressForSource(sourceId: String)

    @Query("DELETE FROM playback_progress")
    suspend fun clearAllProgress()
}

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history WHERE sourceId = :sourceId ORDER BY watchedAt DESC LIMIT 50")
    fun observeHistoryForSource(sourceId: String): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entity: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE sourceId = :sourceId")
    suspend fun clearHistoryForSource(sourceId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAllHistory()
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist_items WHERE sourceId = :sourceId ORDER BY addedAt DESC")
    fun observeWatchlistForSource(sourceId: String): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_items WHERE sourceId = :sourceId AND contentType = :contentType AND contentId = :contentId)")
    fun observeIsInWatchlist(sourceId: String, contentType: String, contentId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_items WHERE sourceId = :sourceId AND contentType = :contentType AND contentId = :contentId)")
    suspend fun isInWatchlist(sourceId: String, contentType: String, contentId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist_items WHERE sourceId = :sourceId AND contentType = :contentType AND contentId = :contentId")
    suspend fun removeFromWatchlist(sourceId: String, contentType: String, contentId: String)

    @Query("DELETE FROM watchlist_items WHERE sourceId = :sourceId")
    suspend fun clearWatchlistForSource(sourceId: String)

    @Query("DELETE FROM watchlist_items")
    suspend fun clearAllWatchlist()
}

@Dao
interface CachedCatalogDao {
    @Query("SELECT * FROM cached_catalog WHERE sourceId = :sourceId AND cacheKey = :cacheKey LIMIT 1")
    suspend fun getCache(sourceId: String, cacheKey: String): CachedCatalogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCache(entity: CachedCatalogEntity)

    @Query("DELETE FROM cached_catalog WHERE sourceId = :sourceId")
    suspend fun clearCacheForSource(sourceId: String)

    @Query("DELETE FROM cached_catalog")
    suspend fun clearAllCache()
}
