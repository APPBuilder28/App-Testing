package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.stringsFor
import com.example.data.local.MediaContentType
import com.example.data.local.PlaylistSection
import com.example.data.remote.XtreamLiveChannel
import com.example.data.remote.XtreamMovie
import com.example.data.remote.XtreamSeries
import com.example.ui.components.QuickSourceSwitchDialog
import com.example.ui.screens.FullScreenPlayerScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LiveTvScreen
import com.example.ui.screens.MovieDetailScreen
import com.example.ui.screens.MoviesScreen
import com.example.ui.screens.SeriesDetailScreen
import com.example.ui.screens.SeriesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SourceManagerScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.BgCharcoalElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NextVuTheme
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.CatalogUiState
import com.example.viewmodel.NextVuViewModel
import com.example.viewmodel.PlaybackTarget
import kotlinx.serialization.Serializable

@Serializable data object RouteSplash
@Serializable data object RouteHome
@Serializable data object RouteLiveTv
@Serializable data object RouteMovies
@Serializable data class RouteMovieDetail(val movieId: String)
@Serializable data object RouteSeries
@Serializable data class RouteSeriesDetail(val seriesId: String)
@Serializable data class RouteSources(val openAddInitially: Boolean = false)
@Serializable data object RouteSettings

private data class TopNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Any,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: NextVuViewModel = viewModel()
            val settings by viewModel.settings.collectAsState()

            // Dynamic Screenshot Blocking (FLAG_SECURE) for Android TV / Mobile
            LaunchedEffect(settings.screenshotBlockingEnabled) {
                if (settings.screenshotBlockingEnabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            NextVuTheme {
                CompositionLocalProvider(LocalAppStrings provides stringsFor(settings.language)) {
                    NextVuAppRoot(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun NextVuAppRoot(viewModel: NextVuViewModel) {
    val strings = LocalAppStrings.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val sources by viewModel.sources.collectAsState()
    val activeSource by viewModel.activeSource.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val liveState by viewModel.liveState.collectAsState()
    val moviesState by viewModel.moviesState.collectAsState()
    val seriesState by viewModel.seriesState.collectAsState()
    val isRefreshingLive by viewModel.isRefreshingLive.collectAsState()
    val isRefreshingMovies by viewModel.isRefreshingMovies.collectAsState()
    val isRefreshingSeries by viewModel.isRefreshingSeries.collectAsState()
    val testConnectionState by viewModel.testConnectionState.collectAsState()
    val selectedMovieDetail by viewModel.selectedMovieDetail.collectAsState()
    val selectedSeriesDetail by viewModel.selectedSeriesDetail.collectAsState()
    val channelEpgMap by viewModel.channelEpgMap.collectAsState()
    val activePlaybackTarget by viewModel.activePlaybackTarget.collectAsState()

    var showQuickSwitchDialog by remember { mutableStateOf(false) }

    val navItems = remember(strings) {
        listOf(
            TopNavItem(strings.navHome, Icons.Rounded.Home, Icons.Outlined.Home, RouteHome, "nav_home"),
            TopNavItem(strings.navLiveTv, Icons.Rounded.LiveTv, Icons.Outlined.LiveTv, RouteLiveTv, "nav_live"),
            TopNavItem(strings.navMovies, Icons.Rounded.Movie, Icons.Outlined.Movie, RouteMovies, "nav_movies"),
            TopNavItem(strings.navSeries, Icons.Rounded.Tv, Icons.Outlined.Tv, RouteSeries, "nav_series"),
            TopNavItem(strings.navSources, Icons.Rounded.Dns, Icons.Outlined.Dns, RouteSources(false), "nav_sources"),
            TopNavItem(strings.navSettings, Icons.Rounded.Settings, Icons.Outlined.Settings, RouteSettings, "nav_settings")
        )
    }

    val showChrome = currentDest != null &&
        !currentDest.hasRoute<RouteSplash>() &&
        !currentDest.hasRoute<RouteMovieDetail>() &&
        !currentDest.hasRoute<RouteSeriesDetail>() &&
        activePlaybackTarget == null

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(RouteHome) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCharcoal)
    ) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            containerColor = BgCharcoal,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showChrome && !useRail) {
                    NavigationBar(
                        containerColor = BgCharcoalElevated,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        navItems.forEach { item ->
                            val selected = isTopRouteSelected(currentDest, item.route)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = SurfaceCardVariant,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && useRail) {
                    NavigationRail(
                        containerColor = BgCharcoalElevated,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        navItems.forEach { item ->
                            val selected = isTopRouteSelected(currentDest, item.route)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = SurfaceCardVariant,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = RouteSplash
                    ) {
                        composable<RouteSplash> {
                            SplashScreen(
                                hasSavedSources = sources.isNotEmpty(),
                                onNavigateToHome = {
                                    navController.navigate(RouteHome) {
                                        popUpTo<RouteSplash> { inclusive = true }
                                    }
                                },
                                onNavigateToAddSource = {
                                    navController.navigate(RouteSources(openAddInitially = true)) {
                                        popUpTo<RouteSplash> { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable<RouteHome> {
                            HomeDashboardScreen(
                                activeSource = activeSource,
                                isOfflineMode = isOfflineMode,
                                continueWatching = continueWatching,
                                watchlist = watchlist,
                                liveState = liveState,
                                moviesState = moviesState,
                                seriesState = seriesState,
                                ukLivePlaylistsOnly = settings.ukLivePlaylistsOnly,
                                englishVodOnly = settings.englishVodOnly,
                                visibleLiveCategoryIds = settings.visibleCategoryIdsFor(activeSource?.id, PlaylistSection.LIVE),
                                visibleMovieCategoryIds = settings.visibleCategoryIdsFor(activeSource?.id, PlaylistSection.MOVIES),
                                visibleSeriesCategoryIds = settings.visibleCategoryIdsFor(activeSource?.id, PlaylistSection.SERIES),
                                onOpenQuickSwitch = { showQuickSwitchDialog = true },
                                onNavigateToSources = { navigateTopLevel(RouteSources(false)) },
                                onRetryLoad = {
                                    activeSource?.let { viewModel.loadAllCatalogsForSource(it, forceRefresh = true) }
                                },
                                onResumeContinueWatching = { viewModel.resumeFromContinueWatching(it) },
                                onOpenLiveChannel = { viewModel.playLiveChannel(it) },
                                onOpenMovieDetail = { movie ->
                                    viewModel.loadMovieDetail(movie)
                                    navController.navigate(RouteMovieDetail(movie.streamId))
                                },
                                onOpenSeriesDetail = { series ->
                                    viewModel.loadSeriesDetail(series)
                                    navController.navigate(RouteSeriesDetail(series.seriesId))
                                },
                                onOpenWatchlistItem = { fav ->
                                    when (fav.contentType) {
                                        MediaContentType.LIVE.name -> {
                                            viewModel.playLiveChannel(
                                                XtreamLiveChannel(
                                                    streamId = fav.contentId,
                                                    name = fav.title,
                                                    streamIcon = fav.posterUrl
                                                )
                                            )
                                        }
                                        MediaContentType.MOVIE.name -> {
                                            val mv = XtreamMovie(
                                                streamId = fav.contentId,
                                                name = fav.title,
                                                streamIcon = fav.posterUrl,
                                                year = fav.year,
                                                rating = fav.rating.toDoubleOrNull() ?: 0.0,
                                                containerExtension = fav.streamExtension
                                            )
                                            viewModel.loadMovieDetail(mv)
                                            navController.navigate(RouteMovieDetail(mv.streamId))
                                        }
                                        else -> {
                                            val sr = XtreamSeries(
                                                seriesId = fav.contentId,
                                                name = fav.title,
                                                coverUrl = fav.posterUrl,
                                                year = fav.year,
                                                rating = fav.rating.toDoubleOrNull() ?: 0.0
                                            )
                                            viewModel.loadSeriesDetail(sr)
                                            navController.navigate(RouteSeriesDetail(sr.seriesId))
                                        }
                                    }
                                }
                            )
                        }

                        composable<RouteLiveTv> {
                            LiveTvScreen(
                                activeSource = activeSource,
                                isOfflineMode = isOfflineMode,
                                liveState = liveState,
                                isRefreshing = isRefreshingLive,
                                watchlist = watchlist,
                                channelEpgMap = channelEpgMap,
                                settings = settings,
                                isFullScreenPlayerOpen = activePlaybackTarget != null,
                                buildStreamUrl = { ch, useTsFallback -> viewModel.buildLivePreviewUrl(ch, useTsFallback) },
                                onRefresh = { viewModel.refreshLive() },
                                onOpenQuickSwitch = { showQuickSwitchDialog = true },
                                onNavigateToSources = { navigateTopLevel(RouteSources(false)) },
                                onOpenFullScreenPlayer = { viewModel.playLiveChannel(it) },
                                onPlayCatchUp = { ch, epg -> viewModel.playCatchUpProgram(ch, epg) },
                                onFetchChannelEpg = { viewModel.fetchChannelEpg(it) },
                                onToggleFavorite = { ch ->
                                    viewModel.toggleWatchlist(
                                        type = MediaContentType.LIVE,
                                        contentId = ch.streamId,
                                        title = ch.name,
                                        posterUrl = ch.streamIcon
                                    )
                                },
                                onUiScaleChange = { viewModel.setLiveTvUiScalePercent(it) },
                                onSavePlaylistSelection = { allIds, selectedIds ->
                                    viewModel.setPlaylistSelection(PlaylistSection.LIVE, allIds, selectedIds)
                                }
                            )
                        }

                        composable<RouteMovies> {
                            MoviesScreen(
                                activeSource = activeSource,
                                isOfflineMode = isOfflineMode,
                                moviesState = moviesState,
                                isRefreshing = isRefreshingMovies,
                                watchlist = watchlist,
                                progressList = continueWatching,
                                englishOnly = settings.englishVodOnly,
                                visibleCategoryIds = settings.visibleCategoryIdsFor(activeSource?.id, PlaylistSection.MOVIES),
                                onRefresh = { viewModel.refreshMovies() },
                                onOpenQuickSwitch = { showQuickSwitchDialog = true },
                                onNavigateToSources = { navigateTopLevel(RouteSources(false)) },
                                onOpenMovieDetail = { movie ->
                                    viewModel.loadMovieDetail(movie)
                                    navController.navigate(RouteMovieDetail(movie.streamId))
                                },
                                onSavePlaylistSelection = { allIds, selectedIds ->
                                    viewModel.setPlaylistSelection(PlaylistSection.MOVIES, allIds, selectedIds)
                                }
                            )
                        }

                        composable<RouteMovieDetail> { entry ->
                            val movieId = entry.toRoute<RouteMovieDetail>().movieId
                            val isWatchlisted by viewModel
                                .observeIsWatchlisted(MediaContentType.MOVIE, movieId)
                                .collectAsState(initial = false)
                            val savedPos = continueWatching
                                .firstOrNull { it.contentType == MediaContentType.MOVIE.name && it.contentId == movieId }
                                ?.positionMs ?: 0L

                            MovieDetailScreen(
                                movieState = selectedMovieDetail,
                                savedProgressMs = savedPos,
                                isWatchlisted = isWatchlisted,
                                onBack = { navController.popBackStack() },
                                onWatchMovie = { viewModel.playMovie(it) },
                                onToggleWatchlist = { movie ->
                                    viewModel.toggleWatchlist(
                                        type = MediaContentType.MOVIE,
                                        contentId = movie.streamId,
                                        title = movie.name,
                                        posterUrl = movie.streamIcon,
                                        categoryName = movie.genre,
                                        rating = movie.rating.toString(),
                                        year = movie.year,
                                        streamExtension = movie.containerExtension
                                    )
                                }
                            )
                        }

                        composable<RouteSeries> {
                            SeriesScreen(
                                activeSource = activeSource,
                                isOfflineMode = isOfflineMode,
                                seriesState = seriesState,
                                isRefreshing = isRefreshingSeries,
                                watchlist = watchlist,
                                englishOnly = settings.englishVodOnly,
                                visibleCategoryIds = settings.visibleCategoryIdsFor(activeSource?.id, PlaylistSection.SERIES),
                                onRefresh = { viewModel.refreshSeries() },
                                onOpenQuickSwitch = { showQuickSwitchDialog = true },
                                onNavigateToSources = { navigateTopLevel(RouteSources(false)) },
                                onOpenSeriesDetail = { series ->
                                    viewModel.loadSeriesDetail(series)
                                    navController.navigate(RouteSeriesDetail(series.seriesId))
                                },
                                onSavePlaylistSelection = { allIds, selectedIds ->
                                    viewModel.setPlaylistSelection(PlaylistSection.SERIES, allIds, selectedIds)
                                }
                            )
                        }

                        composable<RouteSeriesDetail> { entry ->
                            val seriesId = entry.toRoute<RouteSeriesDetail>().seriesId
                            val isWatchlisted by viewModel
                                .observeIsWatchlisted(MediaContentType.SERIES, seriesId)
                                .collectAsState(initial = false)
                            val epProgress by viewModel
                                .observeSeriesEpisodesProgress(seriesId)
                                .collectAsState(initial = emptyList())

                            SeriesDetailScreen(
                                detailsState = selectedSeriesDetail,
                                episodesProgress = epProgress,
                                isWatchlisted = isWatchlisted,
                                onBack = { navController.popBackStack() },
                                onRetry = {
                                    (selectedSeriesDetail as? CatalogUiState.Success)?.data?.series?.let {
                                        viewModel.loadSeriesDetail(it)
                                    }
                                },
                                onPlayEpisode = { series, ep, seasonEps ->
                                    viewModel.playEpisode(series, ep, seasonEps)
                                },
                                onToggleWatchlist = { series ->
                                    viewModel.toggleWatchlist(
                                        type = MediaContentType.SERIES,
                                        contentId = series.seriesId,
                                        title = series.name,
                                        posterUrl = series.coverUrl,
                                        categoryName = series.genre,
                                        rating = series.rating.toString(),
                                        year = series.year
                                    )
                                }
                            )
                        }

                        composable<RouteSources> { entry ->
                            val openAdd = entry.toRoute<RouteSources>().openAddInitially
                            SourceManagerScreen(
                                sources = sources,
                                activeSource = activeSource,
                                isOfflineMode = isOfflineMode,
                                testConnectionState = testConnectionState,
                                openAddDialogInitially = openAdd,
                                onOpenQuickSwitch = { showQuickSwitchDialog = true },
                                onTestConnection = { url, user, pass ->
                                    viewModel.testSourceConnection(url, user, pass)
                                },
                                onResetTestState = { viewModel.resetTestConnectionState() },
                                onSaveSource = { existingId, name, url, user, pass, makeActive, onSaved ->
                                    viewModel.validateAndSaveSource(
                                        existingId = existingId,
                                        sourceName = name,
                                        serverUrl = url,
                                        username = user,
                                        password = pass,
                                        makeActive = makeActive,
                                        onSaved = onSaved
                                    )
                                },
                                onSwitchSource = { viewModel.switchActiveSource(it) },
                                onRenameSource = { id, newName -> viewModel.renameSource(id, newName) },
                                onDeleteSource = { viewModel.deleteSource(it) },
                                onSourceReadyNavigateHome = { navigateTopLevel(RouteHome) }
                            )
                        }

                        composable<RouteSettings> {
                            SettingsScreen(
                                activeSource = activeSource,
                                isOfflineMode = isOfflineMode,
                                settings = settings,
                                hasParentalPin = viewModel.hasParentalPin(),
                                onOpenQuickSwitch = { showQuickSwitchDialog = true },
                                onSelectVideoQuality = { viewModel.setVideoQuality(it) },
                                onSetSubtitleSize = { viewModel.setSubtitleFontSize(it) },
                                onSetSubtitleColor = { viewModel.setSubtitleColor(it) },
                                onSetSubtitleBgOpacity = { viewModel.setSubtitleBgOpacity(it) },
                                onSetShowBitrate = { viewModel.setShowBitrateOverlay(it) },
                                onSetAutoNextEpisode = { viewModel.setAutoNextEpisode(it) },
                                onSetScreenshotBlocking = { viewModel.setScreenshotBlocking(it) },
                                onSetLanguage = { viewModel.setLanguage(it) },
                                onSetUkLivePlaylistsOnly = { viewModel.setUkLivePlaylistsOnly(it) },
                                onSetEnglishVodOnly = { viewModel.setEnglishVodOnly(it) },
                                onSetLiveTvUiScale = { viewModel.setLiveTvUiScalePercent(it) },
                                onEnableParentalPin = { viewModel.enableParentalLock(it) },
                                onVerifyAndDisableParentalPin = { pin ->
                                    val ok = viewModel.verifyParentalPin(pin)
                                    if (ok) viewModel.disableParentalLock()
                                    ok
                                },
                                onClearActiveSourceHistory = { viewModel.clearHistoryForActiveSource() },
                                onClearAllSourcesHistory = { viewModel.clearHistoryForAllSources() },
                                onResetAllSources = { viewModel.resetAllSources() },
                                onCheckDuplicateSource = { draft ->
                                    viewModel.isDuplicateSource(draft.serverUrl, draft.username)
                                },
                                onImportSourcesBatch = { selected, onProgress ->
                                    viewModel.importValidatedSources(selected, onProgress)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Global Full-Screen ExoPlayer Overlay
        activePlaybackTarget?.let { target ->
            val epgList = if (target is PlaybackTarget.Live) {
                channelEpgMap[target.channel.streamId].orEmpty()
            } else emptyList()

            FullScreenPlayerScreen(
                target = target,
                settings = settings,
                epgList = epgList,
                onBack = { viewModel.clearPlaybackTarget() },
                onSaveProgress = { t, pos, dur -> viewModel.saveCurrentProgress(t, pos, dur) },
                onToggleBitrateHud = { viewModel.setShowBitrateOverlay(it) },
                onSelectQuality = { viewModel.setVideoQuality(it) },
                onToggleAutoNextEpisode = { viewModel.setAutoNextEpisode(it) },
                onPlayCatchUp = { ch, epg -> viewModel.playCatchUpProgram(ch, epg) },
                onRetryLiveWithTs = { ch -> viewModel.playLiveChannel(ch, useTsFallback = true) },
                onPlayNextEpisode = { series, ep, eps -> viewModel.playEpisode(series, ep, eps) }
            )
        }
    }

    if (showQuickSwitchDialog) {
        QuickSourceSwitchDialog(
            sources = sources,
            activeSource = activeSource,
            onSelectSource = { viewModel.switchActiveSource(it.id) },
            onManageSources = { navigateTopLevel(RouteSources(false)) },
            onDismiss = { showQuickSwitchDialog = false }
        )
    }
}

private fun isTopRouteSelected(
    dest: androidx.navigation.NavDestination?,
    route: Any
): Boolean {
    if (dest == null) return false
    return when (route) {
        is RouteHome -> dest.hasRoute<RouteHome>()
        is RouteLiveTv -> dest.hasRoute<RouteLiveTv>()
        is RouteMovies -> dest.hasRoute<RouteMovies>() || dest.hasRoute<RouteMovieDetail>()
        is RouteSeries -> dest.hasRoute<RouteSeries>() || dest.hasRoute<RouteSeriesDetail>()
        is RouteSources -> dest.hasRoute<RouteSources>()
        is RouteSettings -> dest.hasRoute<RouteSettings>()
        else -> false
    }
}
