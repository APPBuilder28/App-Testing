package com.example.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class XtreamCategory(
    val categoryId: String,
    val categoryName: String,
    val parentId: Int = 0
)

@Serializable
data class XtreamLiveChannel(
    val streamId: String,
    val num: Int = 0,
    val name: String,
    val streamIcon: String = "",
    val epgChannelId: String = "",
    val categoryId: String = "",
    val tvArchive: Boolean = false,
    val tvArchiveDurationDays: Int = 0,
    val nowPlayingTitle: String = "",
    val nowPlayingTime: String = ""
)

@Serializable
data class XtreamEpgItem(
    val id: String,
    val title: String,
    val description: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val startTimestamp: Long = 0L,
    val stopTimestamp: Long = 0L,
    val isNowPlaying: Boolean = false,
    val hasArchive: Boolean = false
)

@Serializable
data class XtreamMovie(
    val streamId: String,
    val num: Int = 0,
    val name: String,
    val streamIcon: String = "",
    val rating: Double = 0.0,
    val year: String = "",
    val addedTimestamp: Long = 0L,
    val categoryId: String = "",
    val containerExtension: String = "mp4",
    val genre: String = "",
    val plot: String = "",
    val runtime: String = "",
    val director: String = "",
    val cast: String = "",
    val backdropUrl: String = ""
)

@Serializable
data class XtreamSeries(
    val seriesId: String,
    val num: Int = 0,
    val name: String,
    val coverUrl: String = "",
    val backdropUrl: String = "",
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    val releaseDate: String = "",
    val year: String = "",
    val lastModified: Long = 0L,
    val rating: Double = 0.0,
    val categoryId: String = ""
)

@Serializable
data class XtreamEpisode(
    val episodeId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val containerExtension: String = "mp4",
    val duration: String = "",
    val durationSeconds: Int = 0,
    val plot: String = "",
    val thumbnailUrl: String = ""
)

@Serializable
data class XtreamSeriesDetails(
    val series: XtreamSeries,
    val seasons: List<Int>,
    val episodesBySeason: Map<Int, List<XtreamEpisode>>
)

data class XtreamConnectionResult(
    val isAuthorized: Boolean,
    val status: String,
    val username: String,
    val expDate: String?,
    val activeConnections: String?,
    val maxConnections: String?,
    val serverProtocol: String?,
    val serverPort: String?
)

sealed class XtreamApiError(val userMessage: String) {
    data object InvalidUrl : XtreamApiError("Invalid server URL. Include http:// or https://")
    data object Unauthorized : XtreamApiError("Authentication failed. Check username or password.")
    data object AccountExpiredOrInactive : XtreamApiError("Xtream account is inactive or expired.")
    data object Timeout : XtreamApiError("Connection timed out. Server did not respond in time.")
    data object RateLimited : XtreamApiError("Server rate-limit reached (HTTP 429). Please wait and retry.")
    data object NetworkOffline : XtreamApiError("Network unavailable. Switched to offline indicator.")
    data class ServerError(val code: Int, val detail: String) :
        XtreamApiError("Server error ($code): $detail")
}

sealed class ApiResult<out T> {
    data class Success<T>(val data: T, val fromCache: Boolean = false) : ApiResult<T>()
    data class Failure(val error: XtreamApiError) : ApiResult<Nothing>()
}
