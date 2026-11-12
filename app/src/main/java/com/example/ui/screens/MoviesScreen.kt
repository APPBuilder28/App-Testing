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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.core.i18n.LocalAppStrings
import com.example.core.security.XtreamSourceProfile
import com.example.data.local.MediaContentType
import com.example.data.local.PlaybackProgressEntity
import com.example.data.local.WatchlistEntity
import com.example.data.remote.XtreamMovie
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
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
import com.example.viewmodel.MoviesCatalogData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    activeSource: XtreamSourceProfile?,
    isOfflineMode: Boolean,
    moviesState: CatalogUiState<MoviesCatalogData>,
    isRefreshing: Boolean,
    watchlist: List<WatchlistEntity>,
    progressList: List<PlaybackProgressEntity>,
    onRefresh: () -> Unit,
    onOpenQuickSwitch: () -> Unit,
    onNavigateToSources: () -> Unit,
    onOpenMovieDetail: (XtreamMovie) -> Unit
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
        watchlist.filter { it.contentType == MediaContentType.MOVIE.name }.map { it.contentId }.toSet()
    }
    val progressByMovieId = remember(progressList) {
        progressList.filter { it.contentType == MediaContentType.MOVIE.name }
            .associateBy { it.contentId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCharcoal)
    ) {
        NextVuTopBar(
            title = strings.navMovies,
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

        when (moviesState) {
            is CatalogUiState.Idle, is CatalogUiState.Loading -> LoadingStateView("Loading Movies...")
            is CatalogUiState.Error -> ErrorStateView(
                message = moviesState.error.userMessage,
                onRetry = onRefresh,
                secondaryActionLabel = strings.manageSources,
                onSecondaryAction = onNavigateToSources
            )

            is CatalogUiState.Success -> {
                val categories = moviesState.data.categories
                val availableYears = remember(moviesState.data.movies) {
                    moviesState.data.movies.mapNotNull { it.year.takeIf { y -> y.length == 4 } }
                        .distinct().sortedDescending()
                }

                val filtered = remember(
                    moviesState.data.movies,
                    selectedCategoryId,
                    selectedYear,
                    minRating,
                    sortOption,
                    favoritesOnly,
                    favoriteIds,
                    searchQuery
                ) {
                    val base = moviesState.data.movies.filter { movie ->
                        val catOk = selectedCategoryId == null || movie.categoryId == selectedCategoryId
                        val yearOk = selectedYear == null || movie.year == selectedYear
                        val ratingOk = movie.rating >= minRating
                        val favOk = !favoritesOnly || favoriteIds.contains(movie.streamId)
                        val searchOk = searchQuery.isBlank() || movie.name.contains(searchQuery, ignoreCase = true)
                        catOk && yearOk && ratingOk && favOk && searchOk
                    }
                    when (sortOption) {
                        MediaSortOption.RECENT -> base.sortedByDescending { it.addedTimestamp }
                        MediaSortOption.NAME_ASC -> base.sortedBy { it.name.lowercase() }
                        MediaSortOption.RATING_DESC -> base.sortedByDescending { it.rating }
                        MediaSortOption.YEAR_DESC -> base.sortedByDescending { it.year }
                    }
                }

                // Search & Filters
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
                            .testTag("movies_search_input")
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

                    // Genre / Category Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null },
                                label = { Text(strings.allCategories) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                        items(categories, key = { it.categoryId }) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.categoryId,
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
                            icon = Icons.Rounded.Movie,
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
                                .testTag("movies_poster_grid")
                        ) {
                            items(filtered, key = { it.streamId }) { movie ->
                                val progress = progressByMovieId[movie.streamId]?.progressFraction
                                PosterCard(
                                    title = movie.name,
                                    imageUrl = movie.streamIcon,
                                    subtitle = movie.year.ifBlank { null },
                                    rating = movie.rating,
                                    progressFraction = progress,
                                    isWatchlisted = favoriteIds.contains(movie.streamId),
                                    onClick = { onOpenMovieDetail(movie) },
                                    modifier = Modifier.testTag("movie_card_${movie.streamId}")
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
fun MovieDetailScreen(
    movieState: CatalogUiState<XtreamMovie>,
    savedProgressMs: Long,
    isWatchlisted: Boolean,
    onBack: () -> Unit,
    onWatchMovie: (XtreamMovie) -> Unit,
    onToggleWatchlist: (XtreamMovie) -> Unit
) {
    val strings = LocalAppStrings.current
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCharcoal)
    ) {
        when (movieState) {
            is CatalogUiState.Idle, is CatalogUiState.Loading -> LoadingStateView("Loading movie details...")
            is CatalogUiState.Error -> ErrorStateView(message = movieState.error.userMessage, onRetry = onBack)
            is CatalogUiState.Success -> {
                val movie = movieState.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .background(SurfaceCardVariant)
                    ) {
                        val heroImg = movie.backdropUrl.ifBlank { movie.streamIcon }
                        if (heroImg.isNotBlank()) {
                            AsyncImage(
                                model = heroImg,
                                contentDescription = movie.name,
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
                                .testTag("movie_detail_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            if (movie.streamIcon.isNotBlank()) {
                                AsyncImage(
                                    model = movie.streamIcon,
                                    contentDescription = movie.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(104.dp)
                                        .aspectRatio(0.68f)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = movie.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (movie.year.isNotBlank()) {
                                        MetaPill(movie.year)
                                    }
                                    if (movie.runtime.isNotBlank()) {
                                        MetaPill(movie.runtime)
                                    }
                                    if (movie.rating > 0.0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Star, contentDescription = null, tint = HdrGold, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(String.format("%.1f", movie.rating), color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                                if (movie.genre.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(movie.genre, style = MaterialTheme.typography.bodyMedium, color = ElectricCyan)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val resumeLabel = if (savedProgressMs > 5_000L) {
                                val mins = savedProgressMs / 60_000L
                                val secs = (savedProgressMs / 1000L) % 60L
                                "${strings.resumePlayback} (${String.format("%02d:%02d", mins, secs)})"
                            } else strings.watchNow

                            Button(
                                onClick = { onWatchMovie(movie) },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = BgCharcoal),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("watch_movie_button")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(resumeLabel, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onToggleWatchlist(movie) },
                                border = BorderStroke(1.dp, if (isWatchlisted) ElectricCyan else SurfaceBorderSubtle),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("toggle_movie_watchlist_button")
                            ) {
                                Icon(
                                    imageVector = if (isWatchlisted) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                    contentDescription = if (isWatchlisted) strings.inWatchlist else strings.addToWatchlist,
                                    tint = if (isWatchlisted) ElectricCyan else TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isWatchlisted) strings.inWatchlist else strings.addToWatchlist,
                                    color = if (isWatchlisted) ElectricCyan else TextPrimary
                                )
                            }
                        }

                        if (movie.plot.isNotBlank()) {
                            Text(
                                text = movie.plot,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }

                        if (movie.director.isNotBlank()) {
                            Text("Director: ${movie.director}", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        }
                        if (movie.cast.isNotBlank()) {
                            Text("Cast: ${movie.cast}", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun MetaPill(text: String) {
    Surface(
        color = SurfaceCardVariant,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, SurfaceBorderSubtle)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
