package com.example.data.local

import androidx.room.Entity

enum class MediaContentType {
    LIVE,
    MOVIE,
    SERIES,
    SERIES_EPISODE
}

/**
 * Per-source playback position & resume progress.
 * Keyed by (sourceId, contentType, contentId) so every Xtream source maintains
 * completely isolated progress data.
 */
@Entity(
    tableName = "playback_progress",
    primaryKeys = ["sourceId", "contentType", "contentId"]
)
data class PlaybackProgressEntity(
    val sourceId: String,
    val contentType: String,
    val contentId: String,
    val seriesId: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val title: String,
    val subtitle: String = "",
    val posterUrl: String = "",
    val streamExtension: String = "mp4",
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

/**
 * Per-source watch history entry.
 */
@Entity(
    tableName = "watch_history",
    primaryKeys = ["sourceId", "contentType", "contentId"]
)
data class WatchHistoryEntity(
    val sourceId: String,
    val contentType: String,
    val contentId: String,
    val title: String,
    val subtitle: String = "",
    val posterUrl: String = "",
    val watchedAt: Long = System.currentTimeMillis()
)

/**
 * Unified Watchlist / Favorites combining Live Channels, Movies, and Series,
 * strictly isolated per sourceId.
 */
@Entity(
    tableName = "watchlist_items",
    primaryKeys = ["sourceId", "contentType", "contentId"]
)
data class WatchlistEntity(
    val sourceId: String,
    val contentType: String,
    val contentId: String,
    val title: String,
    val posterUrl: String = "",
    val categoryName: String = "",
    val rating: String = "",
    val year: String = "",
    val streamExtension: String = "mp4",
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Lightweight per-source JSON cache for offline fallback when API connection fails.
 */
@Entity(
    tableName = "cached_catalog",
    primaryKeys = ["sourceId", "cacheKey"]
)
data class CachedCatalogEntity(
    val sourceId: String,
    val cacheKey: String,
    val payloadJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)
