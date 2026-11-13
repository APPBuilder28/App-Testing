package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.filter.ContentFilters
import com.example.core.i18n.LocalAppStrings
import com.example.core.security.XtreamSourceProfile
import com.example.data.local.MediaContentType
import com.example.data.local.UserSettings
import com.example.data.local.WatchlistEntity
import com.example.data.remote.XtreamEpgItem
import com.example.data.remote.XtreamLiveChannel
import com.example.player.NextVuPlayerController
import com.example.player.PlayerTelemetryState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LiveEpgGuideChannelRow
import com.example.ui.components.LiveEpgTimelineHeaderRow
import com.example.ui.components.LiveHeroHeaderSection
import com.example.ui.components.LivePlaylistSelection
import com.example.ui.components.LivePlaylistsSidebar
import com.example.ui.components.LiveTvGuideTopBar
import com.example.ui.components.LiveTvScaleControlPanel
import com.example.ui.components.LiveTvUiMetrics
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CatalogUiState
import com.example.viewmodel.LiveCatalogData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTvScreen(
    activeSource: XtreamSourceProfile?,
    isOfflineMode: Boolean,
    liveState: CatalogUiState<LiveCatalogData>,
    isRefreshing: Boolean,
    watchlist: List<WatchlistEntity>,
    channelEpgMap: Map<String, List<XtreamEpgItem>>,
    settings: UserSettings,
    isFullScreenPlayerOpen: Boolean,
    buildStreamUrl: (XtreamLiveChannel, Boolean) -> String?,
    onRefresh: () -> Unit,
    onOpenQuickSwitch: () -> Unit,
    onNavigateToSources: () -> Unit,
    onOpenFullScreenPlayer: (XtreamLiveChannel) -> Unit,
    onPlayCatchUp: (XtreamLiveChannel, XtreamEpgItem) -> Unit,
    onFetchChannelEpg: (XtreamLiveChannel) -> Unit,
    onToggleFavorite: (XtreamLiveChannel) -> Unit,
    onUiScaleChange: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var selectedPlaylist by remember(activeSource?.id) {
        mutableStateOf<LivePlaylistSelection>(LivePlaylistSelection.All)
    }
    var previewChannel by remember(activeSource?.id) { mutableStateOf<XtreamLiveChannel?>(null) }
    var focusedEpgItem by remember(activeSource?.id) { mutableStateOf<XtreamEpgItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var showEpgInfo by remember { mutableStateOf(true) }
    var sidebarExpanded by remember { mutableStateOf(true) }
    var showScalePanel by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    var useTsFallback by remember(previewChannel?.streamId, activeSource?.id) { mutableStateOf(false) }
    var triedTsFallback by remember(previewChannel?.streamId, activeSource?.id) { mutableStateOf(false) }

    var nowEpochSec by remember { mutableLongStateOf(System.currentTimeMillis() / 1000L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            nowEpochSec = System.currentTimeMillis() / 1000L
        }
    }

    // Hold at most ONE ExoPlayer/MediaCodec instance app-wide
    val shouldHoldPreviewPlayer = previewChannel != null && !isFullScreenPlayerOpen
    val previewController = remember(shouldHoldPreviewPlayer) {
        if (shouldHoldPreviewPlayer) NextVuPlayerController(context) else null
    }
    val emptyTelemetryFlow = remember { MutableStateFlow(PlayerTelemetryState()) }
    val previewTelemetry by (previewController?.state ?: emptyTelemetryFlow).collectAsState()

    DisposableEffect(previewController) {
        onDispose {
            previewController?.release()
        }
    }

    // Automatic single fallback from .m3u8 to .ts if server rejects .m3u8
    LaunchedEffect(previewTelemetry.errorMessage, previewChannel?.streamId) {
        if (previewTelemetry.errorMessage != null && previewChannel != null && !triedTsFallback) {
            triedTsFallback = true
            useTsFallback = true
        }
    }

    // Prepare & play selected channel in the upper-right preview box
    LaunchedEffect(previewChannel?.streamId, activeSource?.id, previewController, useTsFallback) {
        val ch = previewChannel
        val ctrl = previewController
        if (ch != null && ctrl != null) {
            val url = buildStreamUrl(ch, useTsFallback)
            if (!url.isNullOrBlank()) {
                ctrl.prepareAndPlay(url, 0L, settings.videoQuality)
                if (isMuted) ctrl.setVolume(0f)
                onFetchChannelEpg(ch)
            }
        }
    }

    BackHandler(
        enabled = showScalePanel || selectedPlaylist !is LivePlaylistSelection.All || searchQuery.isNotBlank()
    ) {
        when {
            showScalePanel -> showScalePanel = false
            searchQuery.isNotBlank() -> searchQuery = ""
            else -> {
                favoritesOnly = false
                selectedPlaylist = LivePlaylistSelection.All
            }
        }
    }

    val favoriteIds = remember(watchlist) {
        watchlist.filter { it.contentType == MediaContentType.LIVE.name }.map { it.contentId }.toSet()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCharcoal)
    ) {
        val isCompact = maxWidth < 600.dp
        val metrics = remember(settings.liveTvUiScalePercent, isCompact) {
            LiveTvUiMetrics.forScale(settings.liveTvUiScalePercent, isCompact)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            LiveTvGuideTopBar(
                title = strings.navLiveTv,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                activeSource = activeSource,
                isOfflineMode = isOfflineMode,
                sidebarExpanded = sidebarExpanded,
                favoritesOnly = favoritesOnly,
                showEpgInfo = showEpgInfo,
                showScalePanel = showScalePanel,
                metrics = metrics,
                isCompact = isCompact,
                onToggleSidebar = { sidebarExpanded = !sidebarExpanded },
                onToggleFavorites = {
                    favoritesOnly = !favoritesOnly
                    if (favoritesOnly) {
                        selectedPlaylist = LivePlaylistSelection.Favorites
                    } else if (selectedPlaylist is LivePlaylistSelection.Favorites) {
                        selectedPlaylist = LivePlaylistSelection.All
                    }
                },
                onToggleEpg = { showEpgInfo = !showEpgInfo },
                onToggleScalePanel = { showScalePanel = !showScalePanel },
                onOpenQuickSwitch = onOpenQuickSwitch
            )

            if (activeSource == null) {
                EmptyStateView(
                    icon = Icons.Rounded.Dns,
                    title = strings.noSourceTitle,
                    subtitle = strings.noSourceSubtitle,
                    actionLabel = strings.manageSources,
                    onAction = onNavigateToSources
                )
                return@Column
            }

            when (liveState) {
                is CatalogUiState.Idle, is CatalogUiState.Loading -> {
                    LoadingStateView("Loading Live TV guide & playlists...")
                }

                is CatalogUiState.Error -> {
                    ErrorStateView(
                        message = liveState.error.userMessage,
                        onRetry = onRefresh,
                        secondaryActionLabel = strings.manageSources,
                        onSecondaryAction = onNavigateToSources
                    )
                }

                is CatalogUiState.Success -> {
                    val effectiveLiveData = remember(liveState.data, settings.ukLivePlaylistsOnly) {
                        ContentFilters.filterLiveCatalog(liveState.data, settings.ukLivePlaylistsOnly)
                    }
                    val categories = effectiveLiveData.categories
                    val allChannels = effectiveLiveData.channels

                    LaunchedEffect(settings.ukLivePlaylistsOnly, categories) {
                        val catSel = selectedPlaylist as? LivePlaylistSelection.Category
                        if (catSel != null && categories.none { it.categoryId == catSel.category.categoryId }) {
                            selectedPlaylist = LivePlaylistSelection.All
                        }
                    }

                    val countsByCategory = remember(allChannels) {
                        allChannels.groupingBy { it.categoryId }.eachCount()
                    }
                    val favoritesCount = remember(allChannels, favoriteIds) {
                        allChannels.count { favoriteIds.contains(it.streamId) }
                    }

                    val channelsInSelectedPlaylist = remember(
                        allChannels,
                        selectedPlaylist,
                        searchQuery,
                        favoritesOnly,
                        favoriteIds
                    ) {
                        val playlistChannels = when (val sel = selectedPlaylist) {
                            is LivePlaylistSelection.All -> allChannels
                            is LivePlaylistSelection.Favorites -> allChannels.filter { favoriteIds.contains(it.streamId) }
                            is LivePlaylistSelection.Category -> allChannels.filter { it.categoryId == sel.category.categoryId }
                        }
                        if (searchQuery.isBlank() && !favoritesOnly) {
                            playlistChannels
                        } else {
                            playlistChannels.filter { ch ->
                                val searchOk = searchQuery.isBlank() || ch.name.contains(searchQuery, ignoreCase = true)
                                val favOk = !favoritesOnly || favoriteIds.contains(ch.streamId)
                                searchOk && favOk
                            }
                        }
                    }

                    val heroChannel = previewChannel ?: channelsInSelectedPlaylist.firstOrNull()
                    val heroEpgList = if (heroChannel != null) channelEpgMap[heroChannel.streamId].orEmpty() else emptyList()
                    val heroFocusedEpg = focusedEpgItem
                        ?: heroEpgList.firstOrNull { it.isNowPlaying }
                        ?: heroEpgList.firstOrNull()

                    // Upper Program Hero + Live Preview Window (scaled 0% - 100%)
                    LiveHeroHeaderSection(
                        focusedChannel = heroChannel,
                        focusedEpg = heroFocusedEpg,
                        nowEpochSec = nowEpochSec,
                        isLivePreviewActive = previewChannel != null && previewController != null && !isFullScreenPlayerOpen,
                        isCompact = isCompact,
                        metrics = metrics,
                        settings = settings,
                        previewController = previewController,
                        previewTelemetry = previewTelemetry,
                        onExpandFullScreen = {
                            heroChannel?.let { onOpenFullScreenPlayer(it) }
                        },
                        onTogglePlayPause = { previewController?.togglePlayPause() },
                        onToggleMute = {
                            isMuted = !isMuted
                            previewController?.setVolume(if (isMuted) 0f else 1f)
                        },
                        onOpenAudioDialog = { showAudioDialog = true },
                        onOpenSubtitleDialog = { showSubtitleDialog = true },
                        onClosePreview = {
                            previewChannel = null
                            focusedEpgItem = null
                        }
                    )

                    HorizontalDivider(color = SurfaceBorderSubtle.copy(alpha = 0.5f))

                    // Lower Split Workspace: Left = Playlists Sidebar, Right = TiviMate-Style EPG TV Guide
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AnimatedVisibility(
                            visible = sidebarExpanded,
                            enter = expandHorizontally(),
                            exit = shrinkHorizontally()
                        ) {
                            LivePlaylistsSidebar(
                                categories = categories,
                                allChannelsCount = allChannels.size,
                                favoritesCount = favoritesCount,
                                countsByCategory = countsByCategory,
                                selectedPlaylist = selectedPlaylist,
                                metrics = metrics,
                                onSelectPlaylist = { sel ->
                                    selectedPlaylist = sel
                                    favoritesOnly = sel is LivePlaylistSelection.Favorites
                                }
                            )
                        }

                        // Right TV Guide Area
                        val guideListState = rememberLazyListState()
                        val timelineScrollState = rememberScrollState()
                        val slotCount = 8
                        val flooredHalfHourSec = (nowEpochSec / 1800L) * 1800L
                        val windowStartSec = flooredHalfHourSec - 1800L
                        val dpPerSecond = metrics.slotWidth.value / 1800f
                        val nowOffsetDp = (((nowEpochSec - windowStartSec).coerceIn(0L, slotCount * 1800L)) * dpPerSecond).dp

                        val visibleIndices by remember {
                            derivedStateOf {
                                guideListState.layoutInfo.visibleItemsInfo.map { it.index }
                            }
                        }
                        LaunchedEffect(showEpgInfo, selectedPlaylist, visibleIndices, channelsInSelectedPlaylist.size) {
                            if (showEpgInfo && channelsInSelectedPlaylist.isNotEmpty()) {
                                delay(250L)
                                visibleIndices
                                    .mapNotNull { channelsInSelectedPlaylist.getOrNull(it) }
                                    .take(14)
                                    .forEach { ch ->
                                        if (!channelEpgMap.containsKey(ch.streamId)) {
                                            onFetchChannelEpg(ch)
                                        }
                                    }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            LiveEpgTimelineHeaderRow(
                                nowEpochSec = nowEpochSec,
                                windowStartSec = windowStartSec,
                                slotCount = slotCount,
                                metrics = metrics,
                                nowOffsetDp = nowOffsetDp,
                                timelineScrollState = timelineScrollState
                            )
                            HorizontalDivider(color = SurfaceBorderSubtle.copy(alpha = 0.55f))

                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = onRefresh,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                if (channelsInSelectedPlaylist.isEmpty()) {
                                    EmptyStateView(
                                        icon = Icons.Rounded.LiveTv,
                                        title = strings.emptyResultsTitle,
                                        subtitle = strings.emptyResultsSubtitle
                                    )
                                } else {
                                    LazyColumn(
                                        state = guideListState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("live_channel_grid")
                                    ) {
                                        itemsIndexed(
                                            channelsInSelectedPlaylist,
                                            key = { idx, ch -> "guide_${ch.streamId}_${ch.categoryId}_$idx" }
                                        ) { _, channel ->
                                            val epgItems = channelEpgMap[channel.streamId].orEmpty()
                                            val isFav = favoriteIds.contains(channel.streamId)
                                            val isPreviewing = previewChannel?.streamId == channel.streamId

                                            LiveEpgGuideChannelRow(
                                                channel = channel,
                                                epgItems = epgItems,
                                                showEpgInfo = showEpgInfo,
                                                isFavorite = isFav,
                                                isPreviewing = isPreviewing,
                                                isSelectedForInfo = heroChannel?.streamId == channel.streamId,
                                                metrics = metrics,
                                                slotCount = slotCount,
                                                windowStartSec = windowStartSec,
                                                nowOffsetDp = nowOffsetDp,
                                                timelineScrollState = timelineScrollState,
                                                onChannelClick = {
                                                    if (isPreviewing) {
                                                        onOpenFullScreenPlayer(channel)
                                                    } else {
                                                        previewChannel = channel
                                                        focusedEpgItem = epgItems.firstOrNull { it.isNowPlaying } ?: epgItems.firstOrNull()
                                                    }
                                                },
                                                onToggleFavorite = { onToggleFavorite(channel) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Real-Time UI Scale Slider Popover (0% - 100%)
        AnimatedVisibility(
            visible = showScalePanel,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = metrics.toolbarControlHeight + metrics.topBarVerticalPad * 2 + 4.dp, end = 12.dp)
        ) {
            LiveTvScaleControlPanel(
                scalePercent = settings.liveTvUiScalePercent,
                onScaleChange = onUiScaleChange,
                onDismiss = { showScalePanel = false }
            )
        }
    }

    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Audio Track", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (previewTelemetry.audioTracks.isEmpty()) {
                        Text("Default stream audio active", color = TextSecondary)
                    } else {
                        previewTelemetry.audioTracks.forEach { track ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        previewController?.selectAudioTrack(track)
                                        showAudioDialog = false
                                    }
                                    .padding(vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = track.isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(track.label, color = TextPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Subtitles", color = TextPrimary) },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                previewController?.selectSubtitleTrack(null)
                                showSubtitleDialog = false
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = previewTelemetry.subtitleTracks.none { it.isSelected },
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Off", color = TextPrimary)
                    }
                    previewTelemetry.subtitleTracks.forEach { track ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    previewController?.selectSubtitleTrack(track)
                                    showSubtitleDialog = false
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = track.isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(track.label, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }
}
