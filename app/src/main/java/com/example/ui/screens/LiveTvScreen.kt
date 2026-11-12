package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.MenuOpen
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.local.WatchlistEntity
import com.example.data.remote.XtreamEpgItem
import com.example.data.remote.XtreamLiveChannel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTvScreen(
    activeSource: XtreamSourceProfile?,
    isOfflineMode: Boolean,
    liveState: CatalogUiState<LiveCatalogData>,
    isRefreshing: Boolean,
    watchlist: List<WatchlistEntity>,
    channelEpgMap: Map<String, List<XtreamEpgItem>>,
    onRefresh: () -> Unit,
    onOpenQuickSwitch: () -> Unit,
    onNavigateToSources: () -> Unit,
    onSelectChannel: (XtreamLiveChannel) -> Unit,
    onFetchChannelEpg: (XtreamLiveChannel) -> Unit,
    onToggleFavorite: (XtreamLiveChannel) -> Unit
) {
    val strings = LocalAppStrings.current
    var sidebarExpanded by remember { mutableStateOf(true) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var showEpgInfo by remember { mutableStateOf(true) }

    val favoriteIds = remember(watchlist) {
        watchlist.filter { it.contentType == MediaContentType.LIVE.name }.map { it.contentId }.toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCharcoal)
    ) {
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

        // Filter toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                onClick = { favoritesOnly = !favoritesOnly },
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
                LoadingStateView("Loading Live TV channels...")
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
                val filteredChannels = remember(
                    liveState.data.channels,
                    selectedCategoryId,
                    searchQuery,
                    favoritesOnly,
                    favoriteIds
                ) {
                    liveState.data.channels.filter { ch ->
                        val catOk = selectedCategoryId == null || ch.categoryId == selectedCategoryId
                        val searchOk = searchQuery.isBlank() || ch.name.contains(searchQuery, ignoreCase = true)
                        val favOk = !favoritesOnly || favoriteIds.contains(ch.streamId)
                        catOk && searchOk && favOk
                    }
                }

                // Fetch EPG lazily for first visible channels when EPG toggle is ON
                LaunchedEffect(showEpgInfo, filteredChannels.take(8).map { it.streamId }) {
                    if (showEpgInfo) {
                        filteredChannels.take(8).forEach { ch ->
                            if (!channelEpgMap.containsKey(ch.streamId)) {
                                onFetchChannelEpg(ch)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Collapsible Left-Side Category Sidebar
                    AnimatedVisibility(
                        visible = sidebarExpanded,
                        enter = expandHorizontally(),
                        exit = shrinkHorizontally()
                    ) {
                        Surface(
                            color = BgCharcoalElevated,
                            border = BorderStroke(1.dp, SurfaceBorderSubtle.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .width(176.dp)
                                .fillMaxHeight()
                                .testTag("live_category_sidebar")
                        ) {
                            LazyColumn(
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item(key = "all_cats") {
                                    CategorySidebarItem(
                                        label = strings.allCategories,
                                        selected = selectedCategoryId == null,
                                        onClick = { selectedCategoryId = null }
                                    )
                                }
                                itemsIndexed(
                                    categories,
                                    key = { idx, cat -> "cat_${cat.categoryId}_$idx" }
                                ) { _, cat ->
                                    CategorySidebarItem(
                                        label = cat.categoryName,
                                        selected = selectedCategoryId == cat.categoryId,
                                        onClick = { selectedCategoryId = cat.categoryId }
                                    )
                                }
                            }
                        }
                    }

                    // Channel Grid with Pull-to-Refresh
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (filteredChannels.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Rounded.LiveTv,
                                title = strings.emptyResultsTitle,
                                subtitle = strings.emptyResultsSubtitle
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 185.dp),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("live_channel_grid")
                            ) {
                                itemsIndexed(
                                    filteredChannels,
                                    key = { idx, ch -> "ch_${ch.streamId}_${ch.categoryId}_$idx" }
                                ) { _, channel ->
                                    val nowProgram = if (showEpgInfo) {
                                        channelEpgMap[channel.streamId]?.firstOrNull { it.isNowPlaying }
                                            ?: channelEpgMap[channel.streamId]?.firstOrNull()
                                    } else null
                                    val isFav = favoriteIds.contains(channel.streamId)

                                    LiveChannelCard(
                                        channel = channel,
                                        nowPlayingText = nowProgram?.let { "${it.startTime} ${it.title}" },
                                        showEpg = showEpgInfo,
                                        isFavorite = isFav,
                                        onClick = { onSelectChannel(channel) },
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
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val strings = LocalAppStrings.current
    NextVuInteractiveCard(
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
                color = TextPrimary,
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
