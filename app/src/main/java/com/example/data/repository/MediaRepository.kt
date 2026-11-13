package com.example.data.repository

import android.content.Context
import com.example.core.security.XtreamSourceProfile
import com.example.data.remote.ApiResult
import com.example.data.remote.XtreamApiService
import com.example.data.remote.XtreamCategory
import com.example.data.remote.XtreamEpgItem
import com.example.data.remote.XtreamLiveChannel
import com.example.data.remote.XtreamMovie
import com.example.data.remote.XtreamSeries
import com.example.data.remote.XtreamSeriesDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

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
 * with streaming GZIP file offline caching per source (avoids SQLite CursorWindow limits and heap spikes).
 */
class MediaRepository(
    context: Context,
    private val apiService: XtreamApiService
) {
    private val cacheDir = File(context.applicationContext.filesDir, "catalog_cache").apply {
        mkdirs()
    }
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

        (streamRes as? ApiResult.Failure) ?: (catRes as ApiResult.Failure)!!
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

    private fun cacheFileFor(sourceId: String, key: String): File {
        val safeSource = sourceId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val safeKey = key.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(cacheDir, "${safeSource}__${safeKey}.json.gz")
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified T> saveCache(sourceId: String, key: String, value: T) {
        withContext(Dispatchers.IO) {
            val targetFile = cacheFileFor(sourceId, key)
            val tempFile = File(cacheDir, "${targetFile.name}.tmp")
            try {
                GZIPOutputStream(BufferedOutputStream(FileOutputStream(tempFile), 16 * 1024)).use { gzOut ->
                    json.encodeToStream(value, gzOut)
                }
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
            } catch (_: Throwable) {
                runCatching { tempFile.delete() }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified T> loadCache(sourceId: String, key: String): T? {
        return withContext(Dispatchers.IO) {
            val targetFile = cacheFileFor(sourceId, key)
            if (!targetFile.exists()) return@withContext null
            try {
                GZIPInputStream(BufferedInputStream(FileInputStream(targetFile), 16 * 1024)).use { gzIn ->
                    json.decodeFromStream<T>(gzIn)
                }
            } catch (_: Throwable) {
                null
            }
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
