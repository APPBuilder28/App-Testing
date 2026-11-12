package com.example.ui.screens

import android.graphics.Color as AndroidColor
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.MenuOpen
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.i18n.LocalAppStrings
import com.example.core.security.XtreamSourceProfile
import com.example.data.local.MediaContentType
import com.example.data.local.UserSettings
import com.example.data.local.WatchlistEntity
import com.example.data.remote.XtreamCategory
import com.example.data.remote.XtreamEpgItem
import com.example.data.remote.XtreamLiveChannel
import com.example.player.NextVuPlayerController
import com.example.ui.components.EmbeddedLivePlayerDock
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.NextVuInteractiveCard
import com.example.ui.components.NextVuTopBar
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.BgCharcoalElevated
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CatalogUiState
import com.example.viewmodel.LiveCatalogData

private sealed class LivePlaylistSelection {
    data object All : LivePlaylistSelection()
    data object Favorites : LivePlaylistSelection()
    data class Category(val category: XtreamCategory) : LivePlaylistSelection()
}

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
    buildStreamUrl: (XtreamLiveChannel) -> String?,
    onRefresh: () -> Unit,
    onOpenQuickSwitch: () -> Unit,
    onNavigateToSources: () -> Unit,
    onOpenFullScreenPlayer: (XtreamLiveChannel) -> Unit,
    onPlayCatchUp: (XtreamLiveChannel, XtreamEpgItem) -> Unit,
    onFetchChannelEpg: (XtreamLiveChannel) -> Unit,
    onToggleFavorite: (XtreamLiveChannel) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var selectedPlaylist by remember(activeSource?.id) { mutableStateOf<LivePlaylistSelection?>(null) }
    var previewChannel by remember(activeSource?.id) { mutableStateOf<XtreamLiveChannel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var showEpgInfo by remember { mutableStateOf(true) }
    var sidebarExpanded by remember { mutableStateOf(true) }
    var isPreviewLocked by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    // Inline ExoPlayer controller for the embedded Live TV preview dock
    val previewController = remember { NextVuPlayerController(context) }
    val previewTelemetry by previewController.state.collectAsState()

    // Pause inline player when Full-Screen Player opens so two ExoPlayers never overlap
    LaunchedEffect(isFullScreenPlayerOpen) {
        if (isFullScreenPlayerOpen && previewController.exoPlayer.isPlaying) {
            previewController.exoPlayer.pause()
        }
    }

    // Play or switch channel in the inline preview player
    LaunchedEffect(previewChannel?.streamId, activeSource?.id) {
        val ch = previewChannel
        if (ch != null) {
            val url = buildStreamUrl(ch)
            if (!url.isNullOrBlank()) {
                previewController.prepareAndPlay(url, 0L, settings.videoQuality)
                onFetchChannelEpg(ch)
            }
        } else {
            previewController.exoPlayer.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            previewController.release()
        }
    }

    // System Back returns from Channels view -> Playlists list
    BackHandler(enabled = selectedPlaylist != null) {
        if (selectedPlaylist is LivePlaylistSelection.Favorites) {
            favoritesOnly = false
        }
        selectedPlaylist = null
        searchQuery = ""
    }

    val favoriteIds = remember(watchlist) {
        watchlist.filter { it.contentType == MediaContentType.LIVE.name }.map { it.contentId }.toSet()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCharcoal)
    ) {
        val isWideLayout = maxWidth >= 680.dp

        Column(modifier = Modifier.fillMaxSize()) {
            NextVuTopBar(
                title = strings.navLiveTv,
                activeSource = activeSource,
                isOfflineMode = isOfflineMode,
                onActiveSourceClick = onOpenQuickSwitch
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

            // Filter & Navigation Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedPlaylist != null) {
                    Surface(
                        onClick = {
                            if (selectedPlaylist is LivePlaylistSelection.Favorites) {
                                favoritesOnly = false
                            }
                            selectedPlaylist = null
                            searchQuery = ""
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricCyan.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("live_back_to_playlists_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back to Playlists",
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Playlists",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                        }
                    }
                }

                if (isWideLayout) {
                    IconButton(
                        onClick = { sidebarExpanded = !sidebarExpanded },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCardVariant)
                            .testTag("toggle_live_sidebar_btn")
                    ) {
                        Icon(
                            imageVector = if (sidebarExpanded) Icons.Rounded.ChevronLeft else Icons.Rounded.MenuOpen,
                            contentDescription = strings.categories,
                            tint = ElectricCyan
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (selectedPlaylist == null) "Search playlists..." else strings.searchHint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ElectricCyan) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = SurfaceBorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("live_search_input")
                )

                FilterChip(
                    selected = favoritesOnly,
                    onClick = {
                        favoritesOnly = !favoritesOnly
                        if (favoritesOnly && selectedPlaylist == null) {
                            selectedPlaylist = LivePlaylistSelection.Favorites
                        }
                    },
                    label = { Text(strings.favoritesOnly, maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (favoritesOnly) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                        selectedLabelColor = ElectricCyan,
                        selectedLeadingIconColor = ElectricCyan
                    ),
                    modifier = Modifier.testTag("live_favorites_filter")
                )

                FilterChip(
                    selected = showEpgInfo,
                    onClick = { showEpgInfo = !showEpgInfo },
                    label = { Text(strings.epgToggle, maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepViolet.copy(alpha = 0.28f),
                        selectedLabelColor = TextPrimary,
                        selectedLeadingIconColor = ElectricCyan
                    ),
                    modifier = Modifier.testTag("live_epg_toggle")
                )
            }

            when (liveState) {
                is CatalogUiState.Idle, is CatalogUiState.Loading -> {
                    LoadingStateView("Loading Live TV playlists...")
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
                    val categories = liveState.data.categories
                    val allChannels = liveState.data.channels

                    val countsByCategory = remember(allChannels) {
                        allChannels.groupingBy { it.categoryId }.eachCount()
                    }

                    val filteredPlaylists = remember(categories, searchQuery, selectedPlaylist) {
                        if (selectedPlaylist != null || searchQuery.isBlank()) categories
                        else categories.filter { it.categoryName.contains(searchQuery, ignoreCase = true) }
                    }

                    val channelsInSelectedPlaylist = remember(
                        allChannels,
                        selectedPlaylist,
                        searchQuery,
                        favoritesOnly,
                        favoriteIds
                    ) {
                        val playlistChannels = when (val sel = selectedPlaylist) {
                            null -> emptyList()
                            is LivePlaylistSelection.All -> allChannels
                            is LivePlaylistSelection.Favorites -> allChannels.filter { favoriteIds.contains(it.streamId) }
                            is LivePlaylistSelection.Category -> allChannels.filter { it.categoryId == sel.category.categoryId }
                        }
                        playlistChannels.filter { ch ->
                            val searchOk = searchQuery.isBlank() || ch.name.contains(searchQuery, ignoreCase = true)
                            val favOk = !favoritesOnly || favoriteIds.contains(ch.streamId)
                            searchOk && favOk
                        }
                    }

                    // Lazily fetch EPG for first visible channels in selected playlist
                    LaunchedEffect(showEpgInfo, selectedPlaylist, channelsInSelectedPlaylist.take(8).map { it.streamId }) {
                        if (showEpgInfo && selectedPlaylist != null) {
                            channelsInSelectedPlaylist.take(8).forEach { ch ->
                                if (!channelEpgMap.containsKey(ch.streamId)) {
                                    onFetchChannelEpg(ch)
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // Left-side Category Sidebar on wide/TV screens
                        AnimatedVisibility(
                            visible = isWideLayout && sidebarExpanded,
                            enter = expandHorizontally(),
                            exit = shrinkHorizontally()
                        ) {
                            Surface(
                                color = BgCharcoalElevated,
                                border = BorderStroke(1.dp, SurfaceBorderSubtle.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .width(184.dp)
                                    .fillMaxHeight()
                                    .testTag("live_category_sidebar")
                            ) {
                                LazyColumn(
                                    contentPadding = PaddingValues(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    item(key = "all_playlists_nav") {
                                        CategorySidebarItem(
                                            label = "All Playlists",
                                            selected = selectedPlaylist == null,
                                            onClick = {
                                                selectedPlaylist = null
                                                searchQuery = ""
                                            }
                                        )
                                    }
                                    item(key = "all_channels_nav") {
                                        CategorySidebarItem(
                                            label = "All Channels (${allChannels.size})",
                                            selected = selectedPlaylist is LivePlaylistSelection.All,
                                            onClick = {
                                                selectedPlaylist = LivePlaylistSelection.All
                                                searchQuery = ""
                                            }
                                        )
                                    }
                                    itemsIndexed(
                                        categories,
                                        key = { idx, cat -> "cat_${cat.categoryId}_$idx" }
                                    ) { _, cat ->
                                        val isCatSelected = (selectedPlaylist as? LivePlaylistSelection.Category)
                                            ?.category?.categoryId == cat.categoryId
                                        CategorySidebarItem(
                                            label = cat.categoryName,
                                            selected = isCatSelected,
                                            onClick = {
                                                selectedPlaylist = LivePlaylistSelection.Category(cat)
                                                searchQuery = ""
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Main Right Pane: Top = Playlists OR Channels inside Playlist; Bottom = Embedded Player + EPG
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            // Top Content Grid (Playlists list OR Channels within selected playlist)
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = onRefresh,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                if (selectedPlaylist == null) {
                                    // PLAYLISTS LIST VIEW
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            text = "Live TV Playlists (${categories.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ElectricCyan,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )

                                        LazyVerticalGrid(
                                            columns = GridCells.Adaptive(minSize = 185.dp),
                                            contentPadding = PaddingValues(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .testTag("live_playlists_grid")
                                        ) {
                                            item(key = "pl_all_channels") {
                                                PlaylistCard(
                                                    title = "All Channels",
                                                    channelCount = allChannels.size,
                                                    isHighlighted = true,
                                                    onClick = {
                                                        selectedPlaylist = LivePlaylistSelection.All
                                                        searchQuery = ""
                                                    },
                                                    modifier = Modifier.testTag("playlist_card_all")
                                                )
                                            }

                                            item(key = "pl_favorites") {
                                                PlaylistCard(
                                                    title = strings.favoritesOnly,
                                                    channelCount = allChannels.count { favoriteIds.contains(it.streamId) },
                                                    isHighlighted = false,
                                                    onClick = {
                                                        selectedPlaylist = LivePlaylistSelection.Favorites
                                                        searchQuery = ""
                                                    },
                                                    modifier = Modifier.testTag("playlist_card_favorites")
                                                )
                                            }

                                            itemsIndexed(
                                                filteredPlaylists,
                                                key = { idx, cat -> "pl_${cat.categoryId}_$idx" }
                                            ) { _, cat ->
                                                PlaylistCard(
                                                    title = cat.categoryName,
                                                    channelCount = countsByCategory[cat.categoryId] ?: 0,
                                                    isHighlighted = false,
                                                    onClick = {
                                                        selectedPlaylist = LivePlaylistSelection.Category(cat)
                                                        searchQuery = ""
                                                    },
                                                    modifier = Modifier.testTag("playlist_card_${cat.categoryId}")
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // CHANNELS INSIDE SELECTED PLAYLIST VIEW
                                    val playlistTitle = when (val sel = selectedPlaylist) {
                                        is LivePlaylistSelection.All -> "All Channels"
                                        is LivePlaylistSelection.Favorites -> strings.favoritesOnly
                                        is LivePlaylistSelection.Category -> sel.category.categoryName
                                        null -> ""
                                    }

                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "$playlistTitle • ${channelsInSelectedPlaylist.size} channels",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = ElectricCyan,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (channelsInSelectedPlaylist.isEmpty()) {
                                            EmptyStateView(
                                                icon = Icons.Rounded.LiveTv,
                                                title = strings.emptyResultsTitle,
                                                subtitle = strings.emptyResultsSubtitle
                                            )
                                        } else {
                                            LazyVerticalGrid(
                                                columns = GridCells.Adaptive(minSize = 175.dp),
                                                contentPadding = PaddingValues(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .testTag("live_channel_grid")
                                            ) {
                                                itemsIndexed(
                                                    channelsInSelectedPlaylist,
                                                    key = { idx, ch -> "ch_${ch.streamId}_${ch.categoryId}_$idx" }
                                                ) { _, channel ->
                                                    val nowProgram = if (showEpgInfo) {
                                                        channelEpgMap[channel.streamId]?.firstOrNull { it.isNowPlaying }
                                                            ?: channelEpgMap[channel.streamId]?.firstOrNull()
                                                    } else null
                                                    val isFav = favoriteIds.contains(channel.streamId)
                                                    val isPlayingThis = previewChannel?.streamId == channel.streamId

                                                    LiveChannelCard(
                                                        channel = channel,
                                                        nowPlayingText = nowProgram?.let { "${it.startTime} ${it.title}" },
                                                        showEpg = showEpgInfo,
                                                        isFavorite = isFav,
                                                        isCurrentlyPlaying = isPlayingThis,
                                                        onClick = {
                                                            if (isPlayingThis) {
                                                                onOpenFullScreenPlayer(channel)
                                                            } else {
                                                                previewChannel = channel
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

                            // EMBEDDED LIVE PLAYER + NOW PLAYING EPG DOCK (matches mockup!)
                            val activePreview = previewChannel
                            if (activePreview != null) {
                                val activeChannelList = channelsInSelectedPlaylist.ifEmpty { allChannels }
                                val currentIdx = activeChannelList.indexOfFirst { it.streamId == activePreview.streamId }

                                EmbeddedLivePlayerDock(
                                    channel = activePreview,
                                    epgItems = channelEpgMap[activePreview.streamId].orEmpty(),
                                    showEpgPanel = showEpgInfo,
                                    isWideLayout = isWideLayout,
                                    isLocked = isPreviewLocked,
                                    settings = settings,
                                    controller = previewController,
                                    telemetry = previewTelemetry,
                                    onTogglePlayPause = { previewController.togglePlayPause() },
                                    onVolumeChange = { previewController.setVolume(it) },
                                    onPrevChannel = {
                                        if (activeChannelList.isNotEmpty() && currentIdx > 0) {
                                            previewChannel = activeChannelList[currentIdx - 1]
                                        }
                                    },
                                    onNextChannel = {
                                        if (activeChannelList.isNotEmpty() && currentIdx in 0 until activeChannelList.lastIndex) {
                                            previewChannel = activeChannelList[currentIdx + 1]
                                        }
                                    },
                                    onToggleEpg = { showEpgInfo = !showEpgInfo },
                                    onOpenAudioDialog = { showAudioDialog = true },
                                    onOpenSubtitleDialog = { showSubtitleDialog = true },
                                    onToggleLock = { isPreviewLocked = !isPreviewLocked },
                                    onExpandFullScreen = { onOpenFullScreenPlayer(activePreview) },
                                    onClosePreview = { previewChannel = null },
                                    onPlayCatchUp = { epg -> onPlayCatchUp(activePreview, epg) }
                                )
                            }
                        }
                    }
                }
            }
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
                                        previewController.selectAudioTrack(track)
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
                                previewController.selectSubtitleTrack(null)
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
                                    previewController.selectSubtitleTrack(track)
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

@Composable
private fun PlaylistCard(
    title: String,
    channelCount: Int,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NextVuInteractiveCard(
        isActive = isHighlighted,
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElectricCyan.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isHighlighted) Icons.Rounded.PlaylistPlay else Icons.Rounded.Folder,
                    contentDescription = title,
                    tint = ElectricCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$channelCount channels",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CategorySidebarItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NextVuInteractiveCard(
        isActive = selected,
        shape = RoundedCornerShape(10.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) ElectricCyan.copy(alpha = 0.14f) else SurfaceCard)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) ElectricCyan else TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LiveChannelCard(
    channel: XtreamLiveChannel,
    nowPlayingText: String?,
    showEpg: Boolean,
    isFavorite: Boolean,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val strings = LocalAppStrings.current
    NextVuInteractiveCard(
        isActive = isCurrentlyPlaying,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_channel_card_${channel.streamId}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCardVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (channel.streamIcon.isNotBlank()) {
                        val context = LocalContext.current
                        val iconRequest = remember(channel.streamIcon) {
                            ImageRequest.Builder(context)
                                .data(channel.streamIcon)
                                .size(144)
                                .crossfade(false)
                                .build()
                        }
                        AsyncImage(
                            model = iconRequest,
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.LiveTv,
                            contentDescription = channel.name,
                            tint = ElectricCyan
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (channel.tvArchive) {
                        Surface(
                            color = DeepViolet.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = strings.catchUpAvailable,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = strings.catchUpAvailable,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) ElectricCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrentlyPlaying) ElectricCyan else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (showEpg) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nowPlayingText ?: "Live Broadcast",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (nowPlayingText != null) ElectricCyan.copy(alpha = 0.9f) else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
