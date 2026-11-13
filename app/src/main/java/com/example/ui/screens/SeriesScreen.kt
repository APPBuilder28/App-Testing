package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.filter.ContentFilters
import com.example.core.i18n.LocalAppStrings
import com.example.core.security.XtreamSourceProfile
import com.example.data.local.MediaContentType
import com.example.data.local.PlaybackProgressEntity
import com.example.data.local.WatchlistEntity
import com.example.data.remote.XtreamEpisode
import com.example.data.remote.XtreamSeries
import com.example.data.remote.XtreamSeriesDetails
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.NextVuInteractiveCard
import com.example.ui.components.NextVuTopBar
import com.example.ui.components.PosterCard
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HdrGold
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CatalogUiState
import com.example.viewmodel.MediaSortOption
import com.example.viewmodel.SeriesCatalogData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesScreen(
    activeSource: XtreamSourceProfile?,
    isOfflineMode: Boolean,
    seriesState: CatalogUiState<SeriesCatalogData>,
    isRefreshing: Boolean,
    watchlist: List<WatchlistEntity>,
    englishOnly: Boolean,
    onRefresh: () -> Unit,
    onOpenQuickSwitch: () -> Unit,
    onNavigateToSources: () -> Unit,
    onOpenSeriesDetail: (XtreamSeries) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedYear by remember { mutableStateOf<String?>(null) }
    var minRating by remember { mutableDoubleStateOf(0.0) }
    var sortOption by remember { mutableStateOf(MediaSortOption.RECENT) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var showYearMenu by remember { mutableStateOf(false) }
    var showRatingMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val favoriteIds = remember(watchlist) {
        watchlist.filter { it.contentType == MediaContentType.SERIES.name }.map { it.contentId }.toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCharcoal)
    ) {
        NextVuTopBar(
            title = strings.navSeries,
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

        when (seriesState) {
            is CatalogUiState.Idle, is CatalogUiState.Loading -> LoadingStateView("Loading Series...")
            is CatalogUiState.Error -> ErrorStateView(
                message = seriesState.error.userMessage,
                onRetry = onRefresh,
                secondaryActionLabel = strings.manageSources,
                onSecondaryAction = onNavigateToSources
            )

            is CatalogUiState.Success -> {
                val effectiveData = remember(seriesState.data, englishOnly) {
                    ContentFilters.filterSeriesCatalog(seriesState.data, englishOnly)
                }
                val categories = effectiveData.categories
                val availableYears = remember(effectiveData.series) {
                    effectiveData.series.mapNotNull { it.year.takeIf { y -> y.length == 4 } }
                        .distinct().sortedDescending()
                }

                val activeCategoryId = selectedCategoryId?.takeIf { sel ->
                    categories.any { it.categoryId == sel }
                }

                val filtered = remember(
                    effectiveData.series,
                    activeCategoryId,
                    selectedYear,
                    minRating,
                    sortOption,
                    favoritesOnly,
                    favoriteIds,
                    searchQuery
                ) {
                    val base = effectiveData.series.filter { s ->
                        val catOk = activeCategoryId == null || s.categoryId == activeCategoryId
                        val yearOk = selectedYear == null || s.year == selectedYear
                        val ratingOk = s.rating >= minRating
                        val favOk = !favoritesOnly || favoriteIds.contains(s.seriesId)
                        val searchOk = searchQuery.isBlank() || s.name.contains(searchQuery, ignoreCase = true)
                        catOk && yearOk && ratingOk && favOk && searchOk
                    }
                    when (sortOption) {
                        MediaSortOption.RECENT -> base.sortedByDescending { it.lastModified }
                        MediaSortOption.NAME_ASC -> base.sortedBy { it.name.lowercase() }
                        MediaSortOption.RATING_DESC -> base.sortedByDescending { it.rating }
                        MediaSortOption.YEAR_DESC -> base.sortedByDescending { it.year }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
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
                            .fillMaxWidth()
                            .testTag("series_search_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = favoritesOnly,
                                onClick = { favoritesOnly = !favoritesOnly },
                                label = { Text(strings.favoritesOnly) },
                                leadingIcon = {
                                    Icon(
                                        if (favoritesOnly) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }

                        item {
                            Box {
                                FilterChip(
                                    selected = selectedYear != null,
                                    onClick = { showYearMenu = true },
                                    label = { Text(selectedYear ?: "Year: All") }
                                )
                                DropdownMenu(
                                    expanded = showYearMenu,
                                    onDismissRequest = { showYearMenu = false },
                                    containerColor = SurfaceCard
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Years", color = TextPrimary) },
                                        onClick = { selectedYear = null; showYearMenu = false }
                                    )
                                    availableYears.take(20).forEach { y ->
                                        DropdownMenuItem(
                                            text = { Text(y, color = TextPrimary) },
                                            onClick = { selectedYear = y; showYearMenu = false }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Box {
                                FilterChip(
                                    selected = minRating > 0.0,
                                    onClick = { showRatingMenu = true },
                                    label = { Text(if (minRating > 0) "Rating ≥ ${minRating.toInt()}★" else "Rating: All") }
                                )
                                DropdownMenu(
                                    expanded = showRatingMenu,
                                    onDismissRequest = { showRatingMenu = false },
                                    containerColor = SurfaceCard
                                ) {
                                    listOf(0.0, 3.0, 3.5, 4.0, 4.5).forEach { r ->
                                        DropdownMenuItem(
                                            text = { Text(if (r == 0.0) "All Ratings" else "$r★ & up", color = TextPrimary) },
                                            onClick = { minRating = r; showRatingMenu = false }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Box {
                                FilterChip(
                                    selected = true,
                                    onClick = { showSortMenu = true },
                                    leadingIcon = { Icon(Icons.Rounded.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text(sortOption.label) }
                                )
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    containerColor = SurfaceCard
                                ) {
                                    MediaSortOption.entries.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt.label, color = TextPrimary) },
                                            onClick = { sortOption = opt; showSortMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = activeCategoryId == null,
                                onClick = { selectedCategoryId = null },
                                label = { Text(strings.allCategories) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                        itemsIndexed(
                            categories,
                            key = { idx, cat -> "scat_${cat.categoryId}_$idx" }
                        ) { _, cat ->
                            FilterChip(
                                selected = activeCategoryId == cat.categoryId,
                                onClick = { selectedCategoryId = cat.categoryId },
                                label = { Text(cat.categoryName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }
                }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.weight(1f)
                ) {
                    if (filtered.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Rounded.Tv,
                            title = strings.emptyResultsTitle,
                            subtitle = strings.emptyResultsSubtitle
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 132.dp),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("series_poster_grid")
                        ) {
                            itemsIndexed(
                                filtered,
                                key = { idx, s -> "sr_${s.seriesId}_${s.categoryId}_$idx" }
                            ) { _, s ->
                                PosterCard(
                                    title = s.name,
                                    imageUrl = s.coverUrl,
                                    subtitle = s.year.ifBlank { null },
                                    rating = s.rating,
                                    isWatchlisted = favoriteIds.contains(s.seriesId),
                                    onClick = { onOpenSeriesDetail(s) },
                                    modifier = Modifier.testTag("series_card_${s.seriesId}")
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
fun SeriesDetailScreen(
    detailsState: CatalogUiState<XtreamSeriesDetails>,
    episodesProgress: List<PlaybackProgressEntity>,
    isWatchlisted: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayEpisode: (XtreamSeries, XtreamEpisode, List<XtreamEpisode>) -> Unit,
    onToggleWatchlist: (XtreamSeries) -> Unit
) {
    val strings = LocalAppStrings.current
    BackHandler(onBack = onBack)

    val progressByEpId = remember(episodesProgress) {
        episodesProgress.associateBy { it.contentId }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCharcoal)
    ) {
        when (detailsState) {
            is CatalogUiState.Idle, is CatalogUiState.Loading -> LoadingStateView("Loading Series & Episodes...")
            is CatalogUiState.Error -> ErrorStateView(message = detailsState.error.userMessage, onRetry = onRetry)
            is CatalogUiState.Success -> {
                val details = detailsState.data
                val series = details.series
                val seasons = details.seasons
                var selectedSeason by remember(seasons) { mutableIntStateOf(seasons.firstOrNull() ?: 1) }
                var seasonDropdownExpanded by remember { mutableStateOf(false) }
                val episodes = details.episodesBySeason[selectedSeason].orEmpty()

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 28.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .background(SurfaceCardVariant)
                        ) {
                            val bannerUrl = series.backdropUrl.ifBlank { series.coverUrl }
                            if (bannerUrl.isNotBlank()) {
                                AsyncImage(
                                    model = bannerUrl,
                                    contentDescription = series.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                BgCharcoal.copy(alpha = 0.35f),
                                                Color.Transparent,
                                                BgCharcoal
                                            )
                                        )
                                    )
                            )
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(12.dp)
                                    .clip(CircleShape)
                                    .background(BgCharcoal.copy(alpha = 0.72f))
                                    .minimumInteractiveComponentSize()
                                    .testTag("series_detail_back")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = series.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (series.year.isNotBlank()) MetaPill(series.year)
                                if (series.genre.isNotBlank()) MetaPill(series.genre)
                                if (series.rating > 0.0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Star, contentDescription = null, tint = HdrGold, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(String.format("%.1f", series.rating), color = TextPrimary)
                                    }
                                }
                            }
                            if (series.plot.isNotBlank()) {
                                Text(
                                    text = series.plot,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Season Dropdown Selector
                                Box {
                                    OutlinedButton(
                                        onClick = { seasonDropdownExpanded = true },
                                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("season_dropdown_button")
                                    ) {
                                        Text("Season $selectedSeason", color = ElectricCyan, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = strings.seasons, tint = ElectricCyan)
                                    }

                                    DropdownMenu(
                                        expanded = seasonDropdownExpanded,
                                        onDismissRequest = { seasonDropdownExpanded = false },
                                        containerColor = SurfaceCard
                                    ) {
                                        seasons.forEach { sNum ->
                                            DropdownMenuItem(
                                                text = { Text("Season $sNum", color = TextPrimary) },
                                                onClick = {
                                                    selectedSeason = sNum
                                                    seasonDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onToggleWatchlist(series) },
                                    border = BorderStroke(1.dp, if (isWatchlisted) ElectricCyan else SurfaceBorderSubtle),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("toggle_series_watchlist_button")
                                ) {
                                    Icon(
                                        imageVector = if (isWatchlisted) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (isWatchlisted) ElectricCyan else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isWatchlisted) strings.inWatchlist else strings.addToWatchlist,
                                        color = if (isWatchlisted) ElectricCyan else TextPrimary
                                    )
                                }
                            }

                            Text(
                                text = "${strings.episodes} (${episodes.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    if (episodes.isEmpty()) {
                        item {
                            Box(modifier = Modifier.padding(20.dp)) {
                                Text("No episodes available for Season $selectedSeason.", color = TextMuted)
                            }
                        }
                    } else {
                        itemsIndexed(
                            episodes,
                            key = { idx, ep -> "ep_${ep.episodeId}_$idx" }
                        ) { _, episode ->
                            val saved = progressByEpId[episode.episodeId]
                            EpisodeListRow(
                                episode = episode,
                                progressFraction = saved?.progressFraction ?: 0f,
                                onClick = { onPlayEpisode(series, episode, episodes) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeListRow(
    episode: XtreamEpisode,
    progressFraction: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NextVuInteractiveCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("episode_row_${episode.episodeId}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = ElectricCyan.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "E${episode.episodeNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = episode.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (episode.duration.isNotBlank()) {
                            Text(
                                text = episode.duration,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play episode",
                    tint = ElectricCyan
                )
            }

            if (progressFraction > 0f) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = ElectricCyan,
                    trackColor = SurfaceCardVariant
                )
            }
        }
    }
}
