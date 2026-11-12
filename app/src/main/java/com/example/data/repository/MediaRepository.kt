package com.example.data.repository

import com.example.core.security.XtreamSourceProfile
import com.example.data.local.CachedCatalogEntity
import com.example.data.local.NextVuDatabase
import com.example.data.remote.ApiResult
import com.example.data.remote.XtreamApiService
import com.example.data.remote.XtreamCategory
import com.example.data.remote.XtreamEpgItem
import com.example.data.remote.XtreamLiveChannel
import com.example.data.remote.XtreamMovie
import com.example.data.remote.XtreamSeries
import com.example.data.remote.XtreamSeriesDetails
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class LiveCatalogBundle(
    val categories: List<XtreamCategory>,
    val channels: List<XtreamLiveChannel>,
    val fromCache: Boolean = false
)

data class MoviesCatalogBundle(
    val categories: List<XtreamCategory>,
    val movies: List<XtreamMovie>,
    val fromCache: Boolean = false
)

data class SeriesCatalogBundle(
    val categories: List<XtreamCategory>,
    val series: List<XtreamSeries>,
    val fromCache: Boolean = false
)

/**
 * Fetches dynamic Live TV, EPG, Movies, and Series exclusively from the active Xtream source,
 * with per-source Room offline caching when network/API fails.
 */
class MediaRepository(
    private val apiService: XtreamApiService,
    private val database: NextVuDatabase
) {
    private val cacheDao = database.cachedCatalogDao()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun fetchLiveCatalog(
        source: XtreamSourceProfile,
        forceRefresh: Boolean = false
    ): ApiResult<LiveCatalogBundle> = coroutineScope {
        val catDeferred = async { apiService.getLiveCategories(source) }
        val streamDeferred = async { apiService.getLiveStreams(source) }

        val catRes = catDeferred.await()
        val streamRes = streamDeferred.await()

        if (catRes is ApiResult.Success && streamRes is ApiResult.Success) {
            val bundle = LiveCatalogBundle(catRes.data, streamRes.data, fromCache = false)
            saveCache(source.id, KEY_LIVE_CATS, catRes.data)
            saveCache(source.id, KEY_LIVE_STREAMS, streamRes.data)
            return@coroutineScope ApiResult.Success(bundle, fromCache = false)
        }

        // Try offline cache fallback if not force-failing without cache
        val cachedCats = loadCache<List<XtreamCategory>>(source.id, KEY_LIVE_CATS)
        val cachedStreams = loadCache<List<XtreamLiveChannel>>(source.id, KEY_LIVE_STREAMS)
        if (!forceRefresh && cachedCats != null && cachedStreams != null) {
            return@coroutineScope ApiResult.Success(
                LiveCatalogBundle(cachedCats, cachedStreams, fromCache = true),
                fromCache = true
            )
        }

        val err = (streamRes as? ApiResult.Failure) ?: (catRes as ApiResult.Failure)!!
        if (cachedCats != null && cachedStreams != null) {
            // Even on force refresh, return error or cache? We expose the failure so offline mode shows!
            return@coroutineScope err
        }
        err
    }

    suspend fun fetchMoviesCatalog(
        source: XtreamSourceProfile,
        forceRefresh: Boolean = false
    ): ApiResult<MoviesCatalogBundle> = coroutineScope {
        val catDeferred = async { apiService.getVodCategories(source) }
        val moviesDeferred = async { apiService.getVodStreams(source) }

        val catRes = catDeferred.await()
        val moviesRes = moviesDeferred.await()

        if (catRes is ApiResult.Success && moviesRes is ApiResult.Success) {
            saveCache(source.id, KEY_VOD_CATS, catRes.data)
            saveCache(source.id, KEY_VOD_STREAMS, moviesRes.data)
            return@coroutineScope ApiResult.Success(
                MoviesCatalogBundle(catRes.data, moviesRes.data, fromCache = false),
                fromCache = false
            )
        }

        val cachedCats = loadCache<List<XtreamCategory>>(source.id, KEY_VOD_CATS)
        val cachedMovies = loadCache<List<XtreamMovie>>(source.id, KEY_VOD_STREAMS)
        if (!forceRefresh && cachedCats != null && cachedMovies != null) {
            return@coroutineScope ApiResult.Success(
                MoviesCatalogBundle(cachedCats, cachedMovies, fromCache = true),
                fromCache = true
            )
        }

        (moviesRes as? ApiResult.Failure) ?: (catRes as ApiResult.Failure)!!
    }

    suspend fun fetchSeriesCatalog(
        source: XtreamSourceProfile,
        forceRefresh: Boolean = false
    ): ApiResult<SeriesCatalogBundle> = coroutineScope {
        val catDeferred = async { apiService.getSeriesCategories(source) }
        val seriesDeferred = async { apiService.getSeriesList(source) }

        val catRes = catDeferred.await()
        val seriesRes = seriesDeferred.await()

        if (catRes is ApiResult.Success && seriesRes is ApiResult.Success) {
            saveCache(source.id, KEY_SERIES_CATS, catRes.data)
            saveCache(source.id, KEY_SERIES_LIST, seriesRes.data)
            return@coroutineScope ApiResult.Success(
                SeriesCatalogBundle(catRes.data, seriesRes.data, fromCache = false),
                fromCache = false
            )
        }

        val cachedCats = loadCache<List<XtreamCategory>>(source.id, KEY_SERIES_CATS)
        val cachedSeries = loadCache<List<XtreamSeries>>(source.id, KEY_SERIES_LIST)
        if (!forceRefresh && cachedCats != null && cachedSeries != null) {
            return@coroutineScope ApiResult.Success(
                SeriesCatalogBundle(cachedCats, cachedSeries, fromCache = true),
                fromCache = true
            )
        }

        (seriesRes as? ApiResult.Failure) ?: (catRes as ApiResult.Failure)!!
    }

    suspend fun fetchShortEpg(source: XtreamSourceProfile, streamId: String): ApiResult<List<XtreamEpgItem>> {
        return apiService.getShortEpg(source, streamId, limit = 15)
    }

    suspend fun fetchMovieDetail(
        source: XtreamSourceProfile,
        vodId: String,
        fallbackMovie: XtreamMovie?
    ): ApiResult<XtreamMovie> {
        return apiService.getVodInfo(source, vodId, fallbackMovie)
    }

    suspend fun fetchSeriesDetail(
        source: XtreamSourceProfile,
        seriesId: String,
        fallbackSeries: XtreamSeries?
    ): ApiResult<XtreamSeriesDetails> {
        return apiService.getSeriesInfo(source, seriesId, fallbackSeries)
    }

    fun buildLiveUrl(source: XtreamSourceProfile, streamId: String, useTsFallback: Boolean = false): String =
        apiService.buildLiveStreamUrl(source, streamId, useTsFallback)

    fun buildCatchUpUrl(
        source: XtreamSourceProfile,
        streamId: String,
        startTimestampSec: Long,
        durationMinutes: Int
    ): String = apiService.buildCatchUpStreamUrl(source, streamId, startTimestampSec, durationMinutes)

    fun buildMovieUrl(source: XtreamSourceProfile, streamId: String, ext: String): String =
        apiService.buildMovieStreamUrl(source, streamId, ext)

    fun buildEpisodeUrl(source: XtreamSourceProfile, episodeId: String, ext: String): String =
        apiService.buildSeriesEpisodeUrl(source, episodeId, ext)

    private suspend inline fun <reified T> saveCache(sourceId: String, key: String, value: T) {
        try {
            val payload = json.encodeToString(value)
            cacheDao.putCache(
                CachedCatalogEntity(
                    sourceId = sourceId,
                    cacheKey = key,
                    payloadJson = payload
                )
            )
        } catch (_: Exception) {
            // Best-effort offline cache
        }
    }

    private suspend inline fun <reified T> loadCache(sourceId: String, key: String): T? {
        return try {
            val entry = cacheDao.getCache(sourceId, key) ?: return null
            json.decodeFromString<T>(entry.payloadJson)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val KEY_LIVE_CATS = "live_categories"
        private const val KEY_LIVE_STREAMS = "live_streams"
        private const val KEY_VOD_CATS = "vod_categories"
        private const val KEY_VOD_STREAMS = "vod_streams"
        private const val KEY_SERIES_CATS = "series_categories"
        private const val KEY_SERIES_LIST = "series_list"
    }
}
