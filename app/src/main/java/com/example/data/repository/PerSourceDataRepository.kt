package com.example.data.repository

import com.example.data.local.MediaContentType
import com.example.data.local.NextVuDatabase
import com.example.data.local.PlaybackProgressEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchlistEntity
import kotlinx.coroutines.flow.Flow

/**
 * Manages Watch History, Continue Watching progress, and Unified Watchlist/Favorites
 * strictly isolated per individual Xtream source profile.
 */
class PerSourceDataRepository(database: NextVuDatabase) {
    private val progressDao = database.playbackProgressDao()
    private val historyDao = database.watchHistoryDao()
    private val watchlistDao = database.watchlistDao()

    fun observeContinueWatching(sourceId: String): Flow<List<PlaybackProgressEntity>> =
        progressDao.observeProgressForSource(sourceId)

    fun observeSeriesEpisodesProgress(sourceId: String, seriesId: String): Flow<List<PlaybackProgressEntity>> =
        progressDao.observeSeriesEpisodesProgress(sourceId, seriesId)

    fun observeWatchHistory(sourceId: String): Flow<List<WatchHistoryEntity>> =
        historyDao.observeHistoryForSource(sourceId)

    fun observeWatchlist(sourceId: String): Flow<List<WatchlistEntity>> =
        watchlistDao.observeWatchlistForSource(sourceId)

    fun observeIsInWatchlist(sourceId: String, contentType: MediaContentType, contentId: String): Flow<Boolean> =
        watchlistDao.observeIsInWatchlist(sourceId, contentType.name, contentId)

    suspend fun getSavedProgress(
        sourceId: String,
        contentType: MediaContentType,
        contentId: String
    ): PlaybackProgressEntity? = progressDao.getProgress(sourceId, contentType.name, contentId)

    suspend fun savePlaybackProgress(
        sourceId: String,
        contentType: MediaContentType,
        contentId: String,
        title: String,
        subtitle: String = "",
        posterUrl: String = "",
        streamExtension: String = "mp4",
        positionMs: Long,
        durationMs: Long,
        seriesId: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ) {
        if (positionMs < 0L) return
        progressDao.upsertProgress(
            PlaybackProgressEntity(
                sourceId = sourceId,
                contentType = contentType.name,
                contentId = contentId,
                seriesId = seriesId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                title = title,
                subtitle = subtitle,
                posterUrl = posterUrl,
                streamExtension = streamExtension,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis()
            )
        )
        historyDao.upsertHistory(
            WatchHistoryEntity(
                sourceId = sourceId,
                contentType = contentType.name,
                contentId = contentId,
                title = title,
                subtitle = subtitle,
                posterUrl = posterUrl,
                watchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordLiveWatch(
        sourceId: String,
        streamId: String,
        channelName: String,
        logoUrl: String
    ) {
        historyDao.upsertHistory(
            WatchHistoryEntity(
                sourceId = sourceId,
                contentType = MediaContentType.LIVE.name,
                contentId = streamId,
                title = channelName,
                subtitle = "Live TV",
                posterUrl = logoUrl,
                watchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleWatchlist(
        sourceId: String,
        contentType: MediaContentType,
        contentId: String,
        title: String,
        posterUrl: String,
        categoryName: String = "",
        rating: String = "",
        year: String = "",
        streamExtension: String = "mp4"
    ): Boolean {
        val exists = watchlistDao.isInWatchlist(sourceId, contentType.name, contentId)
        return if (exists) {
            watchlistDao.removeFromWatchlist(sourceId, contentType.name, contentId)
            false
        } else {
            watchlistDao.addToWatchlist(
                WatchlistEntity(
                    sourceId = sourceId,
                    contentType = contentType.name,
                    contentId = contentId,
                    title = title,
                    posterUrl = posterUrl,
                    categoryName = categoryName,
                    rating = rating,
                    year = year,
                    streamExtension = streamExtension
                )
            )
            true
        }
    }

    suspend fun clearHistoryAndProgressForSource(sourceId: String) {
        progressDao.clearProgressForSource(sourceId)
        historyDao.clearHistoryForSource(sourceId)
    }

    suspend fun clearAllSourcesHistoryAndProgress() {
        progressDao.clearAllProgress()
        historyDao.clearAllHistory()
    }
}
