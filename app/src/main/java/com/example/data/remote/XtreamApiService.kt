package com.example.data.remote

import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import com.example.core.security.XtreamSourceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Dynamic Xtream Codes HTTP API client using OkHttp + resilient JSON parsing.
 * Supports arbitrary user-provided Xtream server URLs with 15s timeouts and HTTP 429 handling.
 * Contains zero hardcoded stream URLs.
 */
class XtreamApiService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun normalizeServerUrl(raw: String): String? {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isBlank()) return null
        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) trimmed else "http://$trimmed"
        return withScheme.toHttpUrlOrNull()?.toString()?.trimEnd('/')
    }

    suspend fun testAndAuthenticate(
        rawServerUrl: String,
        username: String,
        password: String
    ): ApiResult<XtreamConnectionResult> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeServerUrl(rawServerUrl)
            ?: return@withContext ApiResult.Failure(XtreamApiError.InvalidUrl)

        val url = baseUrl.toHttpUrlOrNull()?.newBuilder()
            ?.addPathSegment("player_api.php")
            ?.addQueryParameter("username", username.trim())
            ?.addQueryParameter("password", password)
            ?.build()
            ?: return@withContext ApiResult.Failure(XtreamApiError.InvalidUrl)

        when (val rawResult = executeJsonRequest(url.toString())) {
            is ApiResult.Failure -> rawResult
            is ApiResult.Success -> {
                val root = rawResult.data as? JsonObject
                    ?: return@withContext ApiResult.Failure(XtreamApiError.ServerError(200, "Malformed auth response"))
                val userInfo = root["user_info"] as? JsonObject
                    ?: return@withContext ApiResult.Failure(XtreamApiError.Unauthorized)
                val serverInfo = root["server_info"] as? JsonObject

                val authFlag = userInfo.str("auth")
                val status = userInfo.str("status").ifBlank { "Unknown" }
                val isAuthorized = authFlag == "1" || authFlag.equals("true", ignoreCase = true)
                if (!isAuthorized) {
                    return@withContext ApiResult.Failure(XtreamApiError.Unauthorized)
                }
                if (!status.equals("Active", ignoreCase = true)) {
                    return@withContext ApiResult.Failure(XtreamApiError.AccountExpiredOrInactive)
                }

                val expRaw = userInfo.str("exp_date")
                val formattedExp = expRaw.toLongOrNull()?.takeIf { it > 0L }?.let { epochSec ->
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochSec * 1000L))
                }

                ApiResult.Success(
                    XtreamConnectionResult(
                        isAuthorized = true,
                        status = status,
                        username = userInfo.str("username").ifBlank { username },
                        expDate = formattedExp,
                        activeConnections = userInfo.str("active_cons").ifBlank { null },
                        maxConnections = userInfo.str("max_connections").ifBlank { null },
                        serverProtocol = serverInfo?.str("server_protocol")?.ifBlank { null },
                        serverPort = serverInfo?.str("port")?.ifBlank { null }
                    )
                )
            }
        }
    }

    suspend fun getLiveCategories(source: XtreamSourceProfile): ApiResult<List<XtreamCategory>> =
        fetchCategoryList(source, "get_live_categories")

    suspend fun getVodCategories(source: XtreamSourceProfile): ApiResult<List<XtreamCategory>> =
        fetchCategoryList(source, "get_vod_categories")

    suspend fun getSeriesCategories(source: XtreamSourceProfile): ApiResult<List<XtreamCategory>> =
        fetchCategoryList(source, "get_series_categories")

    private suspend fun fetchCategoryList(
        source: XtreamSourceProfile,
        action: String
    ): ApiResult<List<XtreamCategory>> = withContext(Dispatchers.IO) {
        val url = buildActionUrl(source, action)
            ?: return@withContext ApiResult.Failure(XtreamApiError.InvalidUrl)
        executeStreamingJsonArray(url) { reader ->
            var id = ""
            var name = ""
            var parentId = 0
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "category_id" -> id = reader.nextScalarAsString()
                    "category_name" -> name = reader.nextScalarAsString()
                    "parent_id" -> parentId = reader.nextScalarAsString().toIntOrNull() ?: 0
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            if (id.isBlank() && name.isBlank()) null
            else XtreamCategory(
                categoryId = id,
                categoryName = name.ifBlank { "Uncategorized" },
                parentId = parentId
            )
        }
    }

    suspend fun getLiveStreams(source: XtreamSourceProfile): ApiResult<List<XtreamLiveChannel>> =
        withContext(Dispatchers.IO) {
            val url = buildActionUrl(source, "get_live_streams")
                ?: return@withContext ApiResult.Failure(XtreamApiError.InvalidUrl)
            executeStreamingJsonArray(url) { reader ->
                var streamId = ""
                var num = 0
                var name = ""
                var streamIcon = ""
                var epgChannelId = ""
                var categoryId = ""
                var tvArchive = false
                var tvArchiveDuration = 0

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "stream_id" -> streamId = reader.nextScalarAsString()
                        "num" -> num = reader.nextScalarAsString().toIntOrNull() ?: 0
                        "name" -> name = reader.nextScalarAsString()
                        "stream_icon" -> streamIcon = reader.nextUrlAsString()
                        "epg_channel_id" -> epgChannelId = reader.nextScalarAsString()
                        "category_id" -> categoryId = reader.nextScalarAsString()
                        "tv_archive" -> {
                            val v = reader.nextScalarAsString()
                            tvArchive = v == "1" || v.equals("true", ignoreCase = true)
                        }
                        "tv_archive_duration" -> tvArchiveDuration = reader.nextScalarAsString().toIntOrNull() ?: 0
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                if (streamId.isBlank()) null
                else XtreamLiveChannel(
                    streamId = streamId,
                    num = num,
                    name = name.ifBlank { "Channel $streamId" },
                    streamIcon = streamIcon,
                    epgChannelId = epgChannelId,
                    categoryId = categoryId,
                    tvArchive = tvArchive,
                    tvArchiveDurationDays = tvArchiveDuration
                )
            }
        }

    suspend fun getShortEpg(
        source: XtreamSourceProfile,
        streamId: String,
        limit: Int = 12
    ): ApiResult<List<XtreamEpgItem>> = withContext(Dispatchers.IO) {
        val base = normalizeServerUrl(source.serverUrl)?.toHttpUrlOrNull()
            ?: return@withContext ApiResult.Failure(XtreamApiError.InvalidUrl)
        val url = base.newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", source.username)
            .addQueryParameter("password", source.password)
            .addQueryParameter("action", "get_short_epg")
            .addQueryParameter("stream_id", streamId)
            .addQueryParameter("limit", limit.toString())
            .build()
            .toString()

        when (val res = executeJsonRequest(url)) {
            is ApiResult.Failure -> res
            is ApiResult.Success -> {
                val obj = res.data as? JsonObject ?: return@withContext ApiResult.Success(emptyList())
                val listings = obj["epg_listings"] as? JsonArray ?: JsonArray(emptyList())
                val nowSec = System.currentTimeMillis() / 1000L
                val parsed = listings.mapNotNull { elem ->
                    val item = elem as? JsonObject ?: return@mapNotNull null
                    val rawTitle = item.str("title")
                    val decodedTitle = decodePossibleBase64(rawTitle)
                    val decodedDesc = decodePossibleBase64(item.str("description"))
                    val startTs = item.long("start_timestamp")
                    val stopTs = item.long("stop_timestamp")
                    val isNow = item.int("now_playing") == 1 ||
                        (startTs > 0 && stopTs > 0 && nowSec in startTs..stopTs)
                    XtreamEpgItem(
                        id = item.str("id").ifBlank { "${streamId}_${startTs}" },
                        title = decodedTitle.ifBlank { "Program" },
                        description = decodedDesc,
                        startTime = formatEpgClock(item.str("start"), startTs),
                        endTime = formatEpgClock(item.str("end"), stopTs),
                        startTimestamp = startTs,
                        stopTimestamp = stopTs,
                        isNowPlaying = isNow,
                        hasArchive = item.int("has_archive") == 1
                    )
                }
                ApiResult.Success(parsed)
            }
        }
    }

    suspend fun getVodStreams(source: XtreamSourceProfile): ApiResult<List<XtreamMovie>> =
        withContext(Dispatchers.IO) {
            val url = buildActionUrl(source, "get_vod_streams")
                ?: return@withContext ApiResult.Failure(XtreamApiError.InvalidUrl)
            executeStreamingJsonArray(url) { reader ->
                var streamId = ""
                var num = 0
                var rawName = ""
                var streamIcon = ""
                var fiveBased = ""
                var rawRating = ""
                var year = ""
                var added = 0L
                var categoryId = ""
                var containerExt = ""

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "stream_id" -> streamId = reader.nextScalarAsString()
                        "num" -> num = reader.nextScalarAsString().toIntOrNull() ?: 0
                        "name" -> rawName = reader.nextScalarAsString()
                        "stream_icon" -> streamIcon = reader.nextUrlAsString()
                        "rating_5based" -> fiveBased = reader.nextScalarAsString()
                        "rating" -> rawRating = reader.nextScalarAsString()
                        "year" -> year = reader.nextScalarAsString()
                        "added" -> added = reader.nextScalarAsString().toLongOrNull() ?: 0L
                        "category_id" -> categoryId = reader.nextScalarAsString()
                        "container_extension" -> containerExt = reader.nextScalarAsString()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                if (streamId.isBlank()) null
                else {
                    val safeName = rawName.ifBlank { "Untitled Movie" }
                    XtreamMovie(
                        streamId = streamId,
                        num = num,
                        name = safeName,
                        streamIcon = streamIcon,
                        rating = parseRating(fiveBased, rawRating),
                        year = year.ifBlank { extractYearFromText(safeName) },
                        addedTimestamp = added,
                        categoryId = categoryId,
                        containerExtension = containerExt.ifBlank { "mp4" }
                    )
                }
            }
        }

    suspend fun getVodInfo(
        source: XtreamSourceProfile,
        vodId: String,
        fallbackMovie: XtreamMovie? = null
    ): ApiResult<XtreamMovie> = withContext(Dispatchers.IO) {
        val base = normalizeServerUrl(source.serverUrl)?.toHttpUrlOrNull()
            ?: return@withContext ApiResult.Failure(XtreamApiError.InvalidUrl)
        val url = base.newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", source.username)
            .addQueryParameter("password", source.password)
            .addQueryParameter("action", "get_vod_info")
            .addQueryParameter("vod_id", vodId)
            .build()
            .toString()

        when (val res = executeJsonRequest(url)) {
            is ApiResult.Failure -> res
            is ApiResult.Success -> {
                val root = res.data as? JsonObject
                    ?: return@withContext fallbackMovie?.let { ApiResult.Success(it) }
                        ?: ApiResult.Failure(XtreamApiError.ServerError(200, "Empty VOD info"))
                val info = root["info"] as? JsonObject
                val movieData = root["movie_data"] as? JsonObject
                val backdrop = extractBackdrop(info?.get("backdrop_path"))
                    .ifBlank { info?.str("movie_image").orEmpty() }
                    .ifBlank { fallbackMovie?.streamIcon.orEmpty() }

                val movie = XtreamMovie(
                    streamId = movieData?.str("stream_id")?.ifBlank { vodId } ?: vodId,
                    num = fallbackMovie?.num ?: 0,
                    name = info?.str("name")?.ifBlank { null }
                        ?: movieData?.str("name")?.ifBlank { null }
                        ?: fallbackMovie?.name ?: "Movie",
                    streamIcon = info?.str("movie_image")?.ifBlank { null }
                        ?: fallbackMovie?.streamIcon.orEmpty(),
                    rating = parseRating(
                        info?.str("rating_5based").orEmpty(),
                        info?.str("rating").orEmpty()
                    ).takeIf { it > 0.0 } ?: (fallbackMovie?.rating ?: 0.0),
                    year = info?.str("releasedate")?.take(4)?.ifBlank { null }
                        ?: fallbackMovie?.year.orEmpty(),
                    addedTimestamp = movieData?.long("added") ?: (fallbackMovie?.addedTimestamp ?: 0L),
                    categoryId = movieData?.str("category_id")?.ifBlank { null }
                        ?: fallbackMovie?.categoryId.orEmpty(),
                    containerExtension = movieData?.str("container_extension")?.ifBlank { null }
                        ?: fallbackMovie?.containerExtension ?: "mp4",
                    genre = info?.str("genre").orEmpty(),
                    plot = info?.str("plot")?.ifBlank { null }
                        ?: info?.str("description").orEmpty(),
                    runtime = info?.str("duration")?.ifBlank { null }
                        ?: formatSecondsDuration(info?.int("duration_secs") ?: 0),
                    director = info?.str("director").orEmpty(),
                    cast = info?.str("cast").orEmpty(),
                    backdropUrl = backdrop
                )
                ApiResult.Success(movie)
            }
        }
    }

    suspend fun getSeriesList(source: XtreamSourceProfile): ApiResult<List<XtreamSeries>> =
        withContext(Dispatchers.IO) {
            val url = buildActionUrl(source, "get_series")
                ?: return@withContext ApiResult.Failure(XtreamApiError.InvalidUrl)
            executeStreamingJsonArray(url) { reader ->
                var seriesId = ""
                var num = 0
                var rawName = ""
                var cover = ""
                var backdrop = ""
                var plot = ""
                var cast = ""
                var director = ""
                var genre = ""
                var release = ""
                var lastModified = 0L
                var fiveBased = ""
                var rawRating = ""
                var categoryId = ""

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "series_id" -> seriesId = reader.nextScalarAsString()
                        "num" -> num = reader.nextScalarAsString().toIntOrNull() ?: 0
                        "name" -> rawName = reader.nextScalarAsString()
                        "cover" -> cover = reader.nextUrlAsString()
                        "backdrop_path" -> backdrop = reader.nextFirstBackdropUrl()
                        "plot" -> plot = reader.nextScalarAsString()
                        "cast" -> cast = reader.nextScalarAsString()
                        "director" -> director = reader.nextScalarAsString()
                        "genre" -> genre = reader.nextScalarAsString()
                        "releaseDate" -> release = reader.nextScalarAsString()
                        "last_modified" -> lastModified = reader.nextScalarAsString().toLongOrNull() ?: 0L
                        "rating_5based" -> fiveBased = reader.nextScalarAsString()
                        "rating" -> rawRating = reader.nextScalarAsString()
                        "category_id" -> categoryId = reader.nextScalarAsString()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                if (seriesId.isBlank()) null
                else {
                    val safeName = rawName.ifBlank { "Untitled Series" }
                    XtreamSeries(
                        seriesId = seriesId,
                        num = num,
                        name = safeName,
                        coverUrl = cover,
                        backdropUrl = backdrop.ifBlank { cover },
                        plot = plot,
                        cast = cast,
                        director = director,
                        genre = genre,
                        releaseDate = release,
                        year = release.take(4).ifBlank { extractYearFromText(safeName) },
                        lastModified = lastModified,
                        rating = parseRating(fiveBased, rawRating),
                        categoryId = categoryId
                    )
                }
            }
        }

    suspend fun getSeriesInfo(
        source: XtreamSourceProfile,
        seriesId: String,
        fallbackSeries: XtreamSeries? = null
    ): ApiResult<XtreamSeriesDetails> = withContext(Dispatchers.IO) {
        val base = normalizeServerUrl(source.serverUrl)?.toHttpUrlOrNull()
            ?: return@withContext ApiResult.Failure(XtreamApiError.InvalidUrl)
        val url = base.newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", source.username)
            .addQueryParameter("password", source.password)
            .addQueryParameter("action", "get_series_info")
            .addQueryParameter("series_id", seriesId)
            .build()
            .toString()

        when (val res = executeJsonRequest(url)) {
            is ApiResult.Failure -> res
            is ApiResult.Success -> {
                val root = res.data as? JsonObject
                    ?: return@withContext ApiResult.Failure(XtreamApiError.ServerError(200, "Empty series info"))
                val info = root["info"] as? JsonObject
                val updatedSeries = XtreamSeries(
                    seriesId = seriesId,
                    num = fallbackSeries?.num ?: 0,
                    name = info?.str("name")?.ifBlank { null } ?: fallbackSeries?.name ?: "Series",
                    coverUrl = info?.str("cover")?.ifBlank { null } ?: fallbackSeries?.coverUrl.orEmpty(),
                    backdropUrl = extractBackdrop(info?.get("backdrop_path"))
                        .ifBlank { fallbackSeries?.backdropUrl.orEmpty() }
                        .ifBlank { info?.str("cover").orEmpty() },
                    plot = info?.str("plot")?.ifBlank { null } ?: fallbackSeries?.plot.orEmpty(),
                    cast = info?.str("cast")?.ifBlank { null } ?: fallbackSeries?.cast.orEmpty(),
                    director = info?.str("director")?.ifBlank { null } ?: fallbackSeries?.director.orEmpty(),
                    genre = info?.str("genre")?.ifBlank { null } ?: fallbackSeries?.genre.orEmpty(),
                    releaseDate = info?.str("releaseDate")?.ifBlank { null } ?: fallbackSeries?.releaseDate.orEmpty(),
                    year = info?.str("releaseDate")?.take(4)?.ifBlank { null } ?: fallbackSeries?.year.orEmpty(),
                    lastModified = info?.long("last_modified") ?: (fallbackSeries?.lastModified ?: 0L),
                    rating = parseRating(info?.str("rating_5based").orEmpty(), info?.str("rating").orEmpty())
                        .takeIf { it > 0.0 } ?: (fallbackSeries?.rating ?: 0.0),
                    categoryId = info?.str("category_id")?.ifBlank { null } ?: fallbackSeries?.categoryId.orEmpty()
                )

                val episodesMap = mutableMapOf<Int, List<XtreamEpisode>>()
                val rawEpisodes = root["episodes"]
                if (rawEpisodes is JsonObject) {
                    rawEpisodes.entries.forEach { (seasonKey, epArrayElem) ->
                        val seasonNum = seasonKey.toIntOrNull() ?: 1
                        val arr = epArrayElem as? JsonArray ?: return@forEach
                        val episodes = arr.mapIndexedNotNull { idx, elem ->
                            val epObj = elem as? JsonObject ?: return@mapIndexedNotNull null
                            val epId = epObj.str("id")
                            if (epId.isBlank()) return@mapIndexedNotNull null
                            val epInfo = epObj["info"] as? JsonObject
                            val epNum = epObj.int("episode_num").takeIf { it > 0 } ?: (idx + 1)
                            val durSecs = epInfo?.int("duration_secs") ?: 0
                            XtreamEpisode(
                                episodeId = epId,
                                seasonNumber = epObj.int("season").takeIf { it > 0 } ?: seasonNum,
                                episodeNumber = epNum,
                                title = epObj.str("title").ifBlank { "Episode $epNum" },
                                containerExtension = epObj.str("container_extension").ifBlank { "mp4" },
                                duration = epInfo?.str("duration")?.ifBlank { null }
                                    ?: formatSecondsDuration(durSecs),
                                durationSeconds = durSecs,
                                plot = epInfo?.str("plot").orEmpty(),
                                thumbnailUrl = epInfo?.str("movie_image")?.ifBlank { null }
                                    ?: updatedSeries.backdropUrl
                            )
                        }.sortedBy { it.episodeNumber }
                        if (episodes.isNotEmpty()) {
                            episodesMap[seasonNum] = episodes
                        }
                    }
                }

                val sortedSeasons = episodesMap.keys.sorted()
                ApiResult.Success(
                    XtreamSeriesDetails(
                        series = updatedSeries,
                        seasons = sortedSeasons,
                        episodesBySeason = episodesMap
                    )
                )
            }
        }
    }

    // Dynamic URL builders (NO hardcoded stream URLs)
    fun buildLiveStreamUrl(source: XtreamSourceProfile, streamId: String, useTsFallback: Boolean = false): String {
        val base = normalizeServerUrl(source.serverUrl) ?: source.serverUrl.trimEnd('/')
        val ext = if (useTsFallback) "ts" else "m3u8"
        return "$base/live/${source.username}/${source.password}/$streamId.$ext"
    }

    fun buildCatchUpStreamUrl(
        source: XtreamSourceProfile,
        streamId: String,
        startTimestampSec: Long,
        durationMinutes: Int
    ): String {
        val base = normalizeServerUrl(source.serverUrl) ?: source.serverUrl.trimEnd('/')
        val startFormatted = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US)
            .format(Date(startTimestampSec * 1000L))
        return "$base/timeshift/${source.username}/${source.password}/$durationMinutes/$startFormatted/$streamId.ts"
    }

    fun buildMovieStreamUrl(source: XtreamSourceProfile, streamId: String, ext: String): String {
        val base = normalizeServerUrl(source.serverUrl) ?: source.serverUrl.trimEnd('/')
        val safeExt = ext.ifBlank { "mp4" }.trimStart('.')
        return "$base/movie/${source.username}/${source.password}/$streamId.$safeExt"
    }

    fun buildSeriesEpisodeUrl(source: XtreamSourceProfile, episodeId: String, ext: String): String {
        val base = normalizeServerUrl(source.serverUrl) ?: source.serverUrl.trimEnd('/')
        val safeExt = ext.ifBlank { "mp4" }.trimStart('.')
        return "$base/series/${source.username}/${source.password}/$episodeId.$safeExt"
    }

    private fun buildActionUrl(source: XtreamSourceProfile, action: String): String? {
        val base = normalizeServerUrl(source.serverUrl)?.toHttpUrlOrNull() ?: return null
        return base.newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", source.username)
            .addQueryParameter("password", source.password)
            .addQueryParameter("action", action)
            .build()
            .toString()
    }

    private inline fun <T> executeStreamingJsonArray(
        url: String,
        crossinline readItem: (JsonReader) -> T?
    ): ApiResult<List<T>> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "NextVuPlayer/1.1 (Android)")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    429 -> ApiResult.Failure(XtreamApiError.RateLimited)
                    401, 403 -> ApiResult.Failure(XtreamApiError.Unauthorized)
                    in 200..299 -> {
                        val bodyStream = response.body?.byteStream()
                            ?: return ApiResult.Failure(XtreamApiError.ServerError(response.code, "Empty response"))
                        val reader = JsonReader(BufferedReader(InputStreamReader(bodyStream, Charsets.UTF_8), 32 * 1024))
                        reader.isLenient = true
                        reader.use {
                            when (it.peek()) {
                                JsonToken.BEGIN_ARRAY -> {
                                    val result = ArrayList<T>()
                                    it.beginArray()
                                    while (it.hasNext()) {
                                        if (it.peek() == JsonToken.BEGIN_OBJECT) {
                                            readItem(it)?.let { item -> result.add(item) }
                                        } else {
                                            it.skipValue()
                                        }
                                    }
                                    it.endArray()
                                    ApiResult.Success(result)
                                }
                                else -> ApiResult.Success(emptyList())
                            }
                        }
                    }
                    else -> ApiResult.Failure(
                        XtreamApiError.ServerError(response.code, response.message.ifBlank { "HTTP Error" })
                    )
                }
            }
        } catch (_: SocketTimeoutException) {
            ApiResult.Failure(XtreamApiError.Timeout)
        } catch (_: UnknownHostException) {
            ApiResult.Failure(XtreamApiError.NetworkOffline)
        } catch (_: IOException) {
            ApiResult.Failure(XtreamApiError.NetworkOffline)
        } catch (e: Exception) {
            ApiResult.Failure(XtreamApiError.ServerError(-1, e.localizedMessage ?: "Unexpected error"))
        }
    }

    private fun JsonReader.nextScalarAsString(): String {
        return when (peek()) {
            JsonToken.STRING, JsonToken.NUMBER -> nextString().trim()
            JsonToken.BOOLEAN -> if (nextBoolean()) "true" else "false"
            JsonToken.NULL -> {
                nextNull()
                ""
            }
            else -> {
                skipValue()
                ""
            }
        }
    }

    private fun JsonReader.nextUrlAsString(): String {
        val raw = nextScalarAsString()
        if (raw.isBlank() || raw.equals("null", ignoreCase = true)) return ""
        return if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
            raw
        } else ""
    }

    private fun JsonReader.nextFirstBackdropUrl(): String {
        return when (peek()) {
            JsonToken.STRING -> nextUrlAsString()
            JsonToken.BEGIN_ARRAY -> {
                var first = ""
                beginArray()
                while (hasNext()) {
                    if (first.isBlank()) {
                        first = nextUrlAsString()
                    } else {
                        skipValue()
                    }
                }
                endArray()
                first
            }
            else -> {
                skipValue()
                ""
            }
        }
    }

    private fun executeJsonRequest(url: String): ApiResult<JsonElement> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "NextVuPlayer/1.1 (Android)")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    429 -> ApiResult.Failure(XtreamApiError.RateLimited)
                    401, 403 -> ApiResult.Failure(XtreamApiError.Unauthorized)
                    in 200..299 -> {
                        val body = response.body?.string().orEmpty()
                        if (body.isBlank()) {
                            ApiResult.Failure(XtreamApiError.ServerError(response.code, "Empty response"))
                        } else {
                            ApiResult.Success(json.parseToJsonElement(body))
                        }
                    }
                    else -> ApiResult.Failure(
                        XtreamApiError.ServerError(response.code, response.message.ifBlank { "HTTP Error" })
                    )
                }
            }
        } catch (_: SocketTimeoutException) {
            ApiResult.Failure(XtreamApiError.Timeout)
        } catch (_: UnknownHostException) {
            ApiResult.Failure(XtreamApiError.NetworkOffline)
        } catch (_: IOException) {
            ApiResult.Failure(XtreamApiError.NetworkOffline)
        } catch (e: Exception) {
            ApiResult.Failure(XtreamApiError.ServerError(-1, e.localizedMessage ?: "Unexpected error"))
        }
    }

    private fun JsonObject.str(key: String): String {
        val elem = this[key] ?: return ""
        return (elem as? JsonPrimitive)?.contentOrNull?.trim() ?: ""
    }

    private fun JsonObject.int(key: String): Int {
        return str(key).toIntOrNull() ?: 0
    }

    private fun JsonObject.long(key: String): Long {
        return str(key).toLongOrNull() ?: 0L
    }

    private fun extractBackdrop(elem: JsonElement?): String {
        return when (elem) {
            is JsonPrimitive -> elem.contentOrNull.orEmpty()
            is JsonArray -> (elem.firstOrNull() as? JsonPrimitive)?.contentOrNull.orEmpty()
            else -> ""
        }
    }

    private fun parseRating(fiveBased: String, rawRating: String): Double {
        val five = fiveBased.toDoubleOrNull()
        if (five != null && five > 0.0) return five.coerceIn(0.0, 5.0)
        val raw = rawRating.toDoubleOrNull() ?: return 0.0
        return if (raw > 5.0) (raw / 2.0).coerceIn(0.0, 5.0) else raw.coerceIn(0.0, 5.0)
    }

    private fun extractYearFromText(text: String): String {
        val match = Regex("(19\\d{2}|20\\d{2})").find(text)
        return match?.value.orEmpty()
    }

    private fun formatSecondsDuration(seconds: Int): String {
        if (seconds <= 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    private fun formatEpgClock(raw: String, timestampSec: Long): String {
        if (timestampSec > 0L) {
            return SimpleDateFormat("HH:mm", Locale.US).format(Date(timestampSec * 1000L))
        }
        return if (raw.length >= 16) raw.substring(11, 16) else raw
    }

    private fun decodePossibleBase64(input: String): String {
        if (input.isBlank()) return ""
        val looksBase64 = input.length >= 8 &&
            input.length % 4 == 0 &&
            input.matches(Regex("^[A-Za-z0-9+/=\\r\\n]+$"))
        if (!looksBase64) return input
        return try {
            val decoded = String(Base64.decode(input, Base64.DEFAULT), Charsets.UTF_8).trim()
            if (decoded.any { it.isLetterOrDigit() }) decoded else input
        } catch (_: Exception) {
            input
        }
    }
}
