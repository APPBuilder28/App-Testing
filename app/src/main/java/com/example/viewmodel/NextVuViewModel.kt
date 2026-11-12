package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.i18n.AppLanguage
import com.example.core.security.SecureSourceStorage
import com.example.core.security.XtreamSourceProfile
import com.example.data.local.MediaContentType
import com.example.data.local.NextVuDatabase
import com.example.data.local.PlaybackProgressEntity
import com.example.data.local.SettingsRepository
import com.example.data.local.SubtitleColorOption
import com.example.data.local.UserSettings
import com.example.data.local.VideoQualityPreset
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchlistEntity
import com.example.data.remote.ApiResult
import com.example.data.remote.XtreamApiError
import com.example.data.remote.XtreamApiService
import com.example.data.remote.XtreamCategory
import com.example.data.remote.XtreamConnectionResult
import com.example.data.remote.XtreamEpgItem
import com.example.data.remote.XtreamEpisode
import com.example.data.remote.XtreamLiveChannel
import com.example.data.remote.XtreamMovie
import com.example.data.remote.XtreamSeries
import com.example.data.remote.XtreamSeriesDetails
import com.example.data.repository.MediaRepository
import com.example.data.repository.PerSourceDataRepository
import com.example.data.repository.SourceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CatalogUiState<out T> {
    data object Idle : CatalogUiState<Nothing>
    data object Loading : CatalogUiState<Nothing>
    data class Success<T>(val data: T, val fromCache: Boolean = false) : CatalogUiState<T>
    data class Error(val error: XtreamApiError) : CatalogUiState<Nothing>
}

sealed interface TestConnectionUiState {
    data object Idle : TestConnectionUiState
    data object Testing : TestConnectionUiState
    data class Success(val result: XtreamConnectionResult) : TestConnectionUiState
    data class Error(val message: String) : TestConnectionUiState
}

enum class MediaSortOption(val label: String) {
    RECENT("Recently Added"),
    NAME_ASC("Name A-Z"),
    RATING_DESC("Top Rated"),
    YEAR_DESC("Newest Year")
}

sealed class PlaybackTarget {
    data class Live(
        val channel: XtreamLiveChannel,
        val streamUrl: String,
        val isCatchUp: Boolean = false,
        val catchUpTitle: String = ""
    ) : PlaybackTarget()

    data class Movie(
        val movie: XtreamMovie,
        val streamUrl: String,
        val resumePositionMs: Long
    ) : PlaybackTarget()

    data class SeriesEpisode(
        val series: XtreamSeries,
        val episode: XtreamEpisode,
        val seasonEpisodes: List<XtreamEpisode>,
        val streamUrl: String,
        val resumePositionMs: Long
    ) : PlaybackTarget()
}

data class LiveCatalogData(
    val categories: List<XtreamCategory>,
    val channels: List<XtreamLiveChannel>
)

data class MoviesCatalogData(
    val categories: List<XtreamCategory>,
    val movies: List<XtreamMovie>
)

data class SeriesCatalogData(
    val categories: List<XtreamCategory>,
    val series: List<XtreamSeries>
)

@OptIn(ExperimentalCoroutinesApi::class)
class NextVuViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NextVuDatabase.getInstance(application)
    private val secureStorage = SecureSourceStorage(application)
    private val apiService = XtreamApiService()
    private val sourceRepository = SourceRepository(secureStorage, apiService, database)
    private val mediaRepository = MediaRepository(apiService, database)
    private val perSourceRepo = PerSourceDataRepository(database)
    private val settingsRepo = SettingsRepository(application)

    val sources: StateFlow<List<XtreamSourceProfile>> = sourceRepository.sources
    val activeSource: StateFlow<XtreamSourceProfile?> = sourceRepository.activeSource

    val settings: StateFlow<UserSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    // Per-source reactive flows (automatically switch when activeSource changes!)
    val continueWatching: StateFlow<List<PlaybackProgressEntity>> = activeSource
        .flatMapLatest { source ->
            if (source == null) flowOf(emptyList())
            else perSourceRepo.observeContinueWatching(source.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = activeSource
        .flatMapLatest { source ->
            if (source == null) flowOf(emptyList())
            else perSourceRepo.observeWatchHistory(source.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlist: StateFlow<List<WatchlistEntity>> = activeSource
        .flatMapLatest { source ->
            if (source == null) flowOf(emptyList())
            else perSourceRepo.observeWatchlist(source.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _liveState = MutableStateFlow<CatalogUiState<LiveCatalogData>>(CatalogUiState.Idle)
    val liveState: StateFlow<CatalogUiState<LiveCatalogData>> = _liveState.asStateFlow()

    private val _moviesState = MutableStateFlow<CatalogUiState<MoviesCatalogData>>(CatalogUiState.Idle)
    val moviesState: StateFlow<CatalogUiState<MoviesCatalogData>> = _moviesState.asStateFlow()

    private val _seriesState = MutableStateFlow<CatalogUiState<SeriesCatalogData>>(CatalogUiState.Idle)
    val seriesState: StateFlow<CatalogUiState<SeriesCatalogData>> = _seriesState.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _offlineMessage = MutableStateFlow<String?>(null)
    val offlineMessage: StateFlow<String?> = _offlineMessage.asStateFlow()

    private val _isRefreshingLive = MutableStateFlow(false)
    val isRefreshingLive: StateFlow<Boolean> = _isRefreshingLive.asStateFlow()

    private val _isRefreshingMovies = MutableStateFlow(false)
    val isRefreshingMovies: StateFlow<Boolean> = _isRefreshingMovies.asStateFlow()

    private val _isRefreshingSeries = MutableStateFlow(false)
    val isRefreshingSeries: StateFlow<Boolean> = _isRefreshingSeries.asStateFlow()

    private val _testConnectionState = MutableStateFlow<TestConnectionUiState>(TestConnectionUiState.Idle)
    val testConnectionState: StateFlow<TestConnectionUiState> = _testConnectionState.asStateFlow()

    // Selected detail items
    private val _selectedMovieDetail = MutableStateFlow<CatalogUiState<XtreamMovie>>(CatalogUiState.Idle)
    val selectedMovieDetail: StateFlow<CatalogUiState<XtreamMovie>> = _selectedMovieDetail.asStateFlow()

    private val _selectedSeriesDetail = MutableStateFlow<CatalogUiState<XtreamSeriesDetails>>(CatalogUiState.Idle)
    val selectedSeriesDetail: StateFlow<CatalogUiState<XtreamSeriesDetails>> = _selectedSeriesDetail.asStateFlow()

    // Live channel EPG
    private val _channelEpgMap = MutableStateFlow<Map<String, List<XtreamEpgItem>>>(emptyMap())
    val channelEpgMap: StateFlow<Map<String, List<XtreamEpgItem>>> = _channelEpgMap.asStateFlow()

    private val _activePlaybackTarget = MutableStateFlow<PlaybackTarget?>(null)
    val activePlaybackTarget: StateFlow<PlaybackTarget?> = _activePlaybackTarget.asStateFlow()

    init {
        viewModelScope.launch {
            activeSource.collect { source ->
                if (source != null) {
                    loadAllCatalogsForSource(source, forceRefresh = false)
                } else {
                    _liveState.value = CatalogUiState.Idle
                    _moviesState.value = CatalogUiState.Idle
                    _seriesState.value = CatalogUiState.Idle
                    _isOfflineMode.value = false
                }
            }
        }
    }

    fun loadAllCatalogsForSource(source: XtreamSourceProfile, forceRefresh: Boolean) {
        loadLiveCatalog(source, forceRefresh)
        loadMoviesCatalog(source, forceRefresh)
        loadSeriesCatalog(source, forceRefresh)
    }

    fun refreshLive() {
        val src = activeSource.value ?: return
        viewModelScope.launch {
            _isRefreshingLive.value = true
            loadLiveCatalog(src, forceRefresh = true)
            _isRefreshingLive.value = false
        }
    }

    fun refreshMovies() {
        val src = activeSource.value ?: return
        viewModelScope.launch {
            _isRefreshingMovies.value = true
            loadMoviesCatalog(src, forceRefresh = true)
            _isRefreshingMovies.value = false
        }
    }

    fun refreshSeries() {
        val src = activeSource.value ?: return
        viewModelScope.launch {
            _isRefreshingSeries.value = true
            loadSeriesCatalog(src, forceRefresh = true)
            _isRefreshingSeries.value = false
        }
    }

    private fun loadLiveCatalog(source: XtreamSourceProfile, forceRefresh: Boolean) {
        viewModelScope.launch {
            if (!forceRefresh) _liveState.value = CatalogUiState.Loading
            when (val res = mediaRepository.fetchLiveCatalog(source, forceRefresh)) {
                is ApiResult.Success -> {
                    _liveState.value = CatalogUiState.Success(
                        LiveCatalogData(res.data.categories, res.data.channels),
                        fromCache = res.fromCache
                    )
                    if (res.fromCache) {
                        _isOfflineMode.value = true
                        _offlineMessage.value = "Showing cached Live TV catalog (Offline)"
                    } else {
                        _isOfflineMode.value = false
                        _offlineMessage.value = null
                    }
                }
                is ApiResult.Failure -> {
                    _liveState.value = CatalogUiState.Error(res.error)
                    _isOfflineMode.value = true
                    _offlineMessage.value = res.error.userMessage
                }
            }
        }
    }

    private fun loadMoviesCatalog(source: XtreamSourceProfile, forceRefresh: Boolean) {
        viewModelScope.launch {
            if (!forceRefresh) _moviesState.value = CatalogUiState.Loading
            when (val res = mediaRepository.fetchMoviesCatalog(source, forceRefresh)) {
                is ApiResult.Success -> {
                    _moviesState.value = CatalogUiState.Success(
                        MoviesCatalogData(res.data.categories, res.data.movies),
                        fromCache = res.fromCache
                    )
                    if (!res.fromCache) _isOfflineMode.value = false
                }
                is ApiResult.Failure -> {
                    _moviesState.value = CatalogUiState.Error(res.error)
                    _isOfflineMode.value = true
                    _offlineMessage.value = res.error.userMessage
                }
            }
        }
    }

    private fun loadSeriesCatalog(source: XtreamSourceProfile, forceRefresh: Boolean) {
        viewModelScope.launch {
            if (!forceRefresh) _seriesState.value = CatalogUiState.Loading
            when (val res = mediaRepository.fetchSeriesCatalog(source, forceRefresh)) {
                is ApiResult.Success -> {
                    _seriesState.value = CatalogUiState.Success(
                        SeriesCatalogData(res.data.categories, res.data.series),
                        fromCache = res.fromCache
                    )
                    if (!res.fromCache) _isOfflineMode.value = false
                }
                is ApiResult.Failure -> {
                    _seriesState.value = CatalogUiState.Error(res.error)
                    _isOfflineMode.value = true
                    _offlineMessage.value = res.error.userMessage
                }
            }
        }
    }

    private val inFlightEpgIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun fetchChannelEpg(channel: XtreamLiveChannel) {
        val src = activeSource.value ?: return
        if (!inFlightEpgIds.add(channel.streamId)) return
        viewModelScope.launch {
            try {
                when (val res = mediaRepository.fetchShortEpg(src, channel.streamId)) {
                    is ApiResult.Success -> {
                        _channelEpgMap.update { it + (channel.streamId to res.data) }
                    }
                    is ApiResult.Failure -> Unit
                }
            } finally {
                inFlightEpgIds.remove(channel.streamId)
            }
        }
    }

    fun loadMovieDetail(movie: XtreamMovie) {
        val src = activeSource.value ?: return
        _selectedMovieDetail.value = CatalogUiState.Success(movie)
        viewModelScope.launch {
            when (val res = mediaRepository.fetchMovieDetail(src, movie.streamId, movie)) {
                is ApiResult.Success -> _selectedMovieDetail.value = CatalogUiState.Success(res.data)
                is ApiResult.Failure -> Unit // Keep basic movie metadata if extended info fails
            }
        }
    }

    fun loadSeriesDetail(series: XtreamSeries) {
        val src = activeSource.value ?: return
        _selectedSeriesDetail.value = CatalogUiState.Loading
        viewModelScope.launch {
            when (val res = mediaRepository.fetchSeriesDetail(src, series.seriesId, series)) {
                is ApiResult.Success -> _selectedSeriesDetail.value = CatalogUiState.Success(res.data)
                is ApiResult.Failure -> _selectedSeriesDetail.value = CatalogUiState.Error(res.error)
            }
        }
    }

    fun observeSeriesEpisodesProgress(seriesId: String) =
        activeSource.flatMapLatest { src ->
            if (src == null) flowOf(emptyList())
            else perSourceRepo.observeSeriesEpisodesProgress(src.id, seriesId)
        }

    fun observeIsWatchlisted(type: MediaContentType, contentId: String) =
        activeSource.flatMapLatest { src ->
            if (src == null) flowOf(false)
            else perSourceRepo.observeIsInWatchlist(src.id, type, contentId)
        }

    fun toggleWatchlist(
        type: MediaContentType,
        contentId: String,
        title: String,
        posterUrl: String,
        categoryName: String = "",
        rating: String = "",
        year: String = "",
        streamExtension: String = "mp4"
    ) {
        val src = activeSource.value ?: return
        viewModelScope.launch {
            perSourceRepo.toggleWatchlist(
                sourceId = src.id,
                contentType = type,
                contentId = contentId,
                title = title,
                posterUrl = posterUrl,
                categoryName = categoryName,
                rating = rating,
                year = year,
                streamExtension = streamExtension
            )
        }
    }

    // Playback Launchers
    fun playLiveChannel(channel: XtreamLiveChannel, useTsFallback: Boolean = false) {
        val src = activeSource.value ?: return
        val url = mediaRepository.buildLiveUrl(src, channel.streamId, useTsFallback)
        _activePlaybackTarget.value = PlaybackTarget.Live(channel = channel, streamUrl = url)
        fetchChannelEpg(channel)
        viewModelScope.launch {
            perSourceRepo.recordLiveWatch(src.id, channel.streamId, channel.name, channel.streamIcon)
        }
    }

    fun playCatchUpProgram(channel: XtreamLiveChannel, epgItem: XtreamEpgItem) {
        val src = activeSource.value ?: return
        val durationMin = (((epgItem.stopTimestamp - epgItem.startTimestamp) / 60L).toInt()).coerceAtLeast(30)
        val url = mediaRepository.buildCatchUpUrl(src, channel.streamId, epgItem.startTimestamp, durationMin)
        _activePlaybackTarget.value = PlaybackTarget.Live(
            channel = channel,
            streamUrl = url,
            isCatchUp = true,
            catchUpTitle = epgItem.title
        )
    }

    fun playMovie(movie: XtreamMovie) {
        val src = activeSource.value ?: return
        viewModelScope.launch {
            val saved = perSourceRepo.getSavedProgress(src.id, MediaContentType.MOVIE, movie.streamId)
            val url = mediaRepository.buildMovieUrl(src, movie.streamId, movie.containerExtension)
            _activePlaybackTarget.value = PlaybackTarget.Movie(
                movie = movie,
                streamUrl = url,
                resumePositionMs = saved?.positionMs ?: 0L
            )
        }
    }

    fun playEpisode(series: XtreamSeries, episode: XtreamEpisode, seasonEpisodes: List<XtreamEpisode>) {
        val src = activeSource.value ?: return
        viewModelScope.launch {
            val saved = perSourceRepo.getSavedProgress(src.id, MediaContentType.SERIES_EPISODE, episode.episodeId)
            val url = mediaRepository.buildEpisodeUrl(src, episode.episodeId, episode.containerExtension)
            _activePlaybackTarget.value = PlaybackTarget.SeriesEpisode(
                series = series,
                episode = episode,
                seasonEpisodes = seasonEpisodes,
                streamUrl = url,
                resumePositionMs = saved?.positionMs ?: 0L
            )
        }
    }

    fun resumeFromContinueWatching(progress: PlaybackProgressEntity) {
        val src = activeSource.value ?: return
        when (progress.contentType) {
            MediaContentType.MOVIE.name -> {
                val movie = XtreamMovie(
                    streamId = progress.contentId,
                    name = progress.title,
                    streamIcon = progress.posterUrl,
                    containerExtension = progress.streamExtension
                )
                val url = mediaRepository.buildMovieUrl(src, movie.streamId, movie.containerExtension)
                _activePlaybackTarget.value = PlaybackTarget.Movie(
                    movie = movie,
                    streamUrl = url,
                    resumePositionMs = progress.positionMs
                )
            }
            MediaContentType.SERIES_EPISODE.name -> {
                val series = XtreamSeries(
                    seriesId = progress.seriesId.orEmpty(),
                    name = progress.subtitle.ifBlank { progress.title },
                    coverUrl = progress.posterUrl
                )
                val ep = XtreamEpisode(
                    episodeId = progress.contentId,
                    seasonNumber = progress.seasonNumber ?: 1,
                    episodeNumber = progress.episodeNumber ?: 1,
                    title = progress.title,
                    containerExtension = progress.streamExtension,
                    thumbnailUrl = progress.posterUrl
                )
                val url = mediaRepository.buildEpisodeUrl(src, ep.episodeId, ep.containerExtension)
                _activePlaybackTarget.value = PlaybackTarget.SeriesEpisode(
                    series = series,
                    episode = ep,
                    seasonEpisodes = listOf(ep),
                    streamUrl = url,
                    resumePositionMs = progress.positionMs
                )
            }
        }
    }

    fun saveCurrentProgress(target: PlaybackTarget, positionMs: Long, durationMs: Long) {
        val src = activeSource.value ?: return
        viewModelScope.launch {
            when (target) {
                is PlaybackTarget.Movie -> perSourceRepo.savePlaybackProgress(
                    sourceId = src.id,
                    contentType = MediaContentType.MOVIE,
                    contentId = target.movie.streamId,
                    title = target.movie.name,
                    subtitle = target.movie.year,
                    posterUrl = target.movie.streamIcon,
                    streamExtension = target.movie.containerExtension,
                    positionMs = positionMs,
                    durationMs = durationMs
                )
                is PlaybackTarget.SeriesEpisode -> perSourceRepo.savePlaybackProgress(
                    sourceId = src.id,
                    contentType = MediaContentType.SERIES_EPISODE,
                    contentId = target.episode.episodeId,
                    title = "S${target.episode.seasonNumber}E${target.episode.episodeNumber} • ${target.episode.title}",
                    subtitle = target.series.name,
                    posterUrl = target.episode.thumbnailUrl.ifBlank { target.series.coverUrl },
                    streamExtension = target.episode.containerExtension,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    seriesId = target.series.seriesId,
                    seasonNumber = target.episode.seasonNumber,
                    episodeNumber = target.episode.episodeNumber
                )
                is PlaybackTarget.Live -> Unit
            }
        }
    }

    fun clearPlaybackTarget() {
        _activePlaybackTarget.value = null
    }

    // Multi-Xtream Source Manager actions
    fun resetTestConnectionState() {
        _testConnectionState.value = TestConnectionUiState.Idle
    }

    fun testSourceConnection(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _testConnectionState.value = TestConnectionUiState.Testing
            when (val res = sourceRepository.testConnection(serverUrl, username, password)) {
                is ApiResult.Success -> {
                    _testConnectionState.value = TestConnectionUiState.Success(res.data)
                }
                is ApiResult.Failure -> {
                    _testConnectionState.value = TestConnectionUiState.Error(res.error.userMessage)
                }
            }
        }
    }

    fun validateAndSaveSource(
        existingId: String?,
        sourceName: String,
        serverUrl: String,
        username: String,
        password: String,
        makeActive: Boolean,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            _testConnectionState.value = TestConnectionUiState.Testing
            when (val res = sourceRepository.testConnection(serverUrl, username, password)) {
                is ApiResult.Success -> {
                    _testConnectionState.value = TestConnectionUiState.Success(res.data)
                    sourceRepository.addOrUpdateSource(
                        existingId = existingId,
                        sourceName = sourceName,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        connectionResult = res.data,
                        makeActive = makeActive
                    )
                    onSaved()
                }
                is ApiResult.Failure -> {
                    _testConnectionState.value = TestConnectionUiState.Error(res.error.userMessage)
                }
            }
        }
    }

    fun switchActiveSource(sourceId: String) {
        _channelEpgMap.value = emptyMap()
        sourceRepository.setActiveSource(sourceId)
    }

    fun renameSource(sourceId: String, newName: String) {
        sourceRepository.renameSource(sourceId, newName)
    }

    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            sourceRepository.deleteSource(sourceId)
        }
    }

    fun resetAllSources() {
        viewModelScope.launch {
            sourceRepository.resetAllSources()
        }
    }

    // Settings & Parental PIN actions
    fun setVideoQuality(preset: VideoQualityPreset) = viewModelScope.launch {
        settingsRepo.setVideoQuality(preset)
    }

    fun setSubtitleFontSize(sizeSp: Float) = viewModelScope.launch {
        settingsRepo.setSubtitleFontSize(sizeSp)
    }

    fun setSubtitleColor(color: SubtitleColorOption) = viewModelScope.launch {
        settingsRepo.setSubtitleColor(color)
    }

    fun setSubtitleBgOpacity(opacity: Float) = viewModelScope.launch {
        settingsRepo.setSubtitleBgOpacity(opacity)
    }

    fun setShowBitrateOverlay(show: Boolean) = viewModelScope.launch {
        settingsRepo.setShowBitrateOverlay(show)
    }

    fun setAutoNextEpisode(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.setAutoNextEpisode(enabled)
    }

    fun setScreenshotBlocking(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.setScreenshotBlocking(enabled)
    }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        settingsRepo.setLanguage(language)
    }

    fun hasParentalPin(): Boolean = !secureStorage.getParentalPin().isNullOrBlank()

    fun verifyParentalPin(enteredPin: String): Boolean {
        val saved = secureStorage.getParentalPin()
        return saved != null && saved == enteredPin
    }

    fun enableParentalLock(pin: String) {
        secureStorage.saveParentalPin(pin)
        viewModelScope.launch { settingsRepo.setParentalLockEnabled(true) }
    }

    fun disableParentalLock() {
        secureStorage.saveParentalPin(null)
        viewModelScope.launch { settingsRepo.setParentalLockEnabled(false) }
    }

    fun clearHistoryForActiveSource() {
        val src = activeSource.value ?: return
        viewModelScope.launch {
            perSourceRepo.clearHistoryAndProgressForSource(src.id)
        }
    }

    fun clearHistoryForAllSources() {
        viewModelScope.launch {
            perSourceRepo.clearAllSourcesHistoryAndProgress()
        }
    }
}
