package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.filter.ContentFilters
import com.example.core.i18n.LocalAppStrings
import com.example.core.security.XtreamSourceProfile
import com.example.data.local.MediaContentType
import com.example.data.local.PlaybackProgressEntity
import com.example.data.local.WatchlistEntity
import com.example.data.remote.XtreamLiveChannel
import com.example.data.remote.XtreamMovie
import com.example.data.remote.XtreamSeries
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.NextVuInteractiveCard
import com.example.ui.components.NextVuTopBar
import com.example.ui.components.PosterCard
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.BgCharcoalElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CatalogUiState
import com.example.viewmodel.LiveCatalogData
import com.example.viewmodel.MoviesCatalogData
import com.example.viewmodel.SeriesCatalogData

@Composable
fun HomeDashboardScreen(
    activeSource: XtreamSourceProfile?,
    isOfflineMode: Boolean,
    continueWatching: List<PlaybackProgressEntity>,
    watchlist: List<WatchlistEntity>,
    liveState: CatalogUiState<LiveCatalogData>,
    moviesState: CatalogUiState<MoviesCatalogData>,
    seriesState: CatalogUiState<SeriesCatalogData>,
    ukLivePlaylistsOnly: Boolean,
    englishVodOnly: Boolean,
    onOpenQuickSwitch: () -> Unit,
    onNavigateToSources: () -> Unit,
    onRetryLoad: () -> Unit,
    onResumeContinueWatching: (PlaybackProgressEntity) -> Unit,
    onOpenLiveChannel: (XtreamLiveChannel) -> Unit,
    onOpenMovieDetail: (XtreamMovie) -> Unit,
    onOpenSeriesDetail: (XtreamSeries) -> Unit,
    onOpenWatchlistItem: (WatchlistEntity) -> Unit
) {
    val strings = LocalAppStrings.current
    var showSearchOverlay by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCharcoal)
    ) {
        NextVuTopBar(
            title = strings.appName,
            activeSource = activeSource,
            isOfflineMode = isOfflineMode,
            onActiveSourceClick = onOpenQuickSwitch,
            onSearchClick = if (activeSource != null) {
                { showSearchOverlay = true }
            } else null
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

        val allLoading = moviesState is CatalogUiState.Loading &&
            seriesState is CatalogUiState.Loading &&
            liveState is CatalogUiState.Loading

        val allError = (moviesState as? CatalogUiState.Error)
            ?.takeIf { seriesState is CatalogUiState.Error && liveState is CatalogUiState.Error }

        when {
            allLoading && continueWatching.isEmpty() && watchlist.isEmpty() -> {
                LoadingStateView(message = strings.loadingSources)
            }

            allError != null && continueWatching.isEmpty() && watchlist.isEmpty() -> {
                ErrorStateView(
                    message = allError.error.userMessage,
                    onRetry = onRetryLoad,
                    secondaryActionLabel = strings.manageSources,
                    onSecondaryAction = onNavigateToSources
                )
            }

            else -> {
                val filteredLive = remember(liveState, ukLivePlaylistsOnly) {
                    (liveState as? CatalogUiState.Success)?.data?.let {
                        ContentFilters.filterLiveCatalog(it, ukLivePlaylistsOnly)
                    }
                }
                val filteredMovies = remember(moviesState, englishVodOnly) {
                    (moviesState as? CatalogUiState.Success)?.data?.let {
                        ContentFilters.filterMoviesCatalog(it, englishVodOnly)
                    }
                }
                val filteredSeries = remember(seriesState, englishVodOnly) {
                    (seriesState as? CatalogUiState.Success)?.data?.let {
                        ContentFilters.filterSeriesCatalog(it, englishVodOnly)
                    }
                }

                val recentMovies = filteredMovies?.movies
                    ?.sortedByDescending { it.addedTimestamp }
                    ?.take(16)
                    .orEmpty()

                val recentSeries = filteredSeries?.series
                    ?.sortedByDescending { it.lastModified }
                    ?.take(16)
                    .orEmpty()

                val isEverythingEmpty = continueWatching.isEmpty() &&
                    watchlist.isEmpty() &&
                    recentMovies.isEmpty() &&
                    recentSeries.isEmpty()

                if (isEverythingEmpty) {
                    EmptyStateView(
                        icon = Icons.Rounded.Tv,
                        title = strings.emptyResultsTitle,
                        subtitle = strings.emptyResultsSubtitle,
                        actionLabel = strings.retry,
                        onAction = onRetryLoad
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("home_dashboard_list")
                    ) {
                        if (continueWatching.isNotEmpty()) {
                            item(key = "row_continue") {
                                HomeSectionHeader(
                                    icon = Icons.Rounded.History,
                                    title = strings.continueWatching
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(continueWatching, key = { "${it.contentType}_${it.contentId}" }) { item ->
                                        PosterCard(
                                            title = item.title,
                                            imageUrl = item.posterUrl,
                                            subtitle = item.subtitle.ifBlank { formatRemainingTime(item.durationMs - item.positionMs) },
                                            progressFraction = item.progressFraction,
                                            aspectRatio = 1.45f,
                                            cardWidth = 200.dp,
                                            onClick = { onResumeContinueWatching(item) },
                                            modifier = Modifier.testTag("continue_card_${item.contentId}")
                                        )
                                    }
                                }
                            }
                        }

                        if (watchlist.isNotEmpty()) {
                            item(key = "row_watchlist") {
                                HomeSectionHeader(
                                    icon = Icons.Rounded.Bookmark,
                                    title = strings.favoritesWatchlist
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(watchlist, key = { "${it.contentType}_${it.contentId}" }) { fav ->
                                        PosterCard(
                                            title = fav.title,
                                            imageUrl = fav.posterUrl,
                                            subtitle = when (fav.contentType) {
                                                MediaContentType.LIVE.name -> strings.navLiveTv
                                                MediaContentType.MOVIE.name -> strings.navMovies
                                                else -> strings.navSeries
                                            },
                                            rating = fav.rating.toDoubleOrNull() ?: 0.0,
                                            isWatchlisted = true,
                                            onClick = { onOpenWatchlistItem(fav) },
                                            modifier = Modifier.testTag("watchlist_card_${fav.contentId}")
                                        )
                                    }
                                }
                            }
                        }

                        if (recentMovies.isNotEmpty()) {
                            item(key = "row_recent_movies") {
                                HomeSectionHeader(
                                    icon = Icons.Rounded.NewReleases,
                                    title = "${strings.recentlyAdded} • ${strings.navMovies}"
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    itemsIndexed(
                                        recentMovies,
                                        key = { idx, m -> "rmv_${m.streamId}_$idx" }
                                    ) { _, movie ->
                                        PosterCard(
                                            title = movie.name,
                                            imageUrl = movie.streamIcon,
                                            subtitle = movie.year.ifBlank { null },
                                            rating = movie.rating,
                                            onClick = { onOpenMovieDetail(movie) },
                                            modifier = Modifier.testTag("recent_movie_${movie.streamId}")
                                        )
                                    }
                                }
                            }
                        }

                        if (recentSeries.isNotEmpty()) {
                            item(key = "row_recent_series") {
                                HomeSectionHeader(
                                    icon = Icons.Rounded.Tv,
                                    title = "${strings.recentlyAdded} • ${strings.navSeries}"
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    itemsIndexed(
                                        recentSeries,
                                        key = { idx, s -> "rsr_${s.seriesId}_$idx" }
                                    ) { _, series ->
                                        PosterCard(
                                            title = series.name,
                                            imageUrl = series.coverUrl,
                                            subtitle = series.year.ifBlank { null },
                                            rating = series.rating,
                                            onClick = { onOpenSeriesDetail(series) },
                                            modifier = Modifier.testTag("recent_series_${series.seriesId}")
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

    if (showSearchOverlay) {
        val searchChannels = remember(liveState, ukLivePlaylistsOnly) {
            (liveState as? CatalogUiState.Success)?.data?.let {
                ContentFilters.filterLiveCatalog(it, ukLivePlaylistsOnly).channels
            }.orEmpty()
        }
        val searchMovies = remember(moviesState, englishVodOnly) {
            (moviesState as? CatalogUiState.Success)?.data?.let {
                ContentFilters.filterMoviesCatalog(it, englishVodOnly).movies
            }.orEmpty()
        }
        val searchSeries = remember(seriesState, englishVodOnly) {
            (seriesState as? CatalogUiState.Success)?.data?.let {
                ContentFilters.filterSeriesCatalog(it, englishVodOnly).series
            }.orEmpty()
        }
        GlobalSearchDialog(
            liveChannels = searchChannels,
            movies = searchMovies,
            series = searchSeries,
            onSelectChannel = {
                showSearchOverlay = false
                onOpenLiveChannel(it)
            },
            onSelectMovie = {
                showSearchOverlay = false
                onOpenMovieDetail(it)
            },
            onSelectSeries = {
                showSearchOverlay = false
                onOpenSeriesDetail(it)
            },
            onDismiss = { showSearchOverlay = false }
        )
    }
}

@Composable
private fun HomeSectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ElectricCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GlobalSearchDialog(
    liveChannels: List<XtreamLiveChannel>,
    movies: List<XtreamMovie>,
    series: List<XtreamSeries>,
    onSelectChannel: (XtreamLiveChannel) -> Unit,
    onSelectMovie: (XtreamMovie) -> Unit,
    onSelectSeries: (XtreamSeries) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var query by remember { mutableStateOf("") }

    val matchingChannels = remember(query, liveChannels) {
        if (query.isBlank()) emptyList()
        else liveChannels.filter { it.name.contains(query, ignoreCase = true) }.take(15)
    }
    val matchingMovies = remember(query, movies) {
        if (query.isBlank()) emptyList()
        else movies.filter { it.name.contains(query, ignoreCase = true) }.take(15)
    }
    val matchingSeries = remember(query, series) {
        if (query.isBlank()) emptyList()
        else series.filter { it.name.contains(query, ignoreCase = true) }.take(15)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = BgCharcoalElevated,
            border = BorderStroke(1.dp, SurfaceBorderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(strings.searchHint) },
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
                            .testTag("global_search_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.cancel, tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (query.isBlank()) {
                    EmptyStateView(
                        icon = Icons.Rounded.Search,
                        title = strings.searchHint,
                        subtitle = "Results are searched dynamically inside your active Xtream source."
                    )
                } else if (matchingChannels.isEmpty() && matchingMovies.isEmpty() && matchingSeries.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Rounded.Search,
                        title = strings.emptyResultsTitle,
                        subtitle = strings.emptyResultsSubtitle
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (matchingChannels.isNotEmpty()) {
                            item {
                                Text(strings.navLiveTv, style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                            }
                            itemsIndexed(
                                matchingChannels,
                                key = { idx, ch -> "ch_${ch.streamId}_$idx" }
                            ) { _, ch ->
                                SearchResultRow(
                                    icon = Icons.Rounded.LiveTv,
                                    title = ch.name,
                                    subtitle = strings.navLiveTv,
                                    onClick = { onSelectChannel(ch) }
                                )
                            }
                        }
                        if (matchingMovies.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(strings.navMovies, style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                            }
                            itemsIndexed(
                                matchingMovies,
                                key = { idx, mv -> "mv_${mv.streamId}_$idx" }
                            ) { _, mv ->
                                SearchResultRow(
                                    icon = Icons.Rounded.Movie,
                                    title = mv.name,
                                    subtitle = mv.year.ifBlank { strings.navMovies },
                                    onClick = { onSelectMovie(mv) }
                                )
                            }
                        }
                        if (matchingSeries.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(strings.navSeries, style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                            }
                            itemsIndexed(
                                matchingSeries,
                                key = { idx, sr -> "sr_${sr.seriesId}_$idx" }
                            ) { _, sr ->
                                SearchResultRow(
                                    icon = Icons.Rounded.Tv,
                                    title = sr.name,
                                    subtitle = sr.year.ifBlank { strings.navSeries },
                                    onClick = { onSelectSeries(sr) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    NextVuInteractiveCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}

private fun formatRemainingTime(remainingMs: Long): String {
    if (remainingMs <= 0L) return "Watched"
    val mins = (remainingMs / 60_000L).coerceAtLeast(1L)
    return "${mins}m left"
}
