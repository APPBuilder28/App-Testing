package com.example.ui.components

import android.graphics.Color as AndroidColor
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.MenuOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.security.XtreamSourceProfile
import com.example.data.local.UserSettings
import com.example.data.remote.XtreamCategory
import com.example.data.remote.XtreamEpgItem
import com.example.data.remote.XtreamLiveChannel
import com.example.player.NextVuPlayerController
import com.example.player.PlayerTelemetryState
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.BgCharcoalElevated
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class LivePlaylistSelection {
    data object All : LivePlaylistSelection()
    data object Favorites : LivePlaylistSelection()
    data class Category(val category: XtreamCategory) : LivePlaylistSelection()
}

data class TimelineSegment(
    val startSec: Long,
    val endSec: Long,
    val epgItem: XtreamEpgItem?
)

@Composable
fun LiveTvGuideTopBar(
    title: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    activeSource: XtreamSourceProfile?,
    isOfflineMode: Boolean,
    sidebarExpanded: Boolean,
    favoritesOnly: Boolean,
    showEpgInfo: Boolean,
    isCompact: Boolean,
    onToggleSidebar: () -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleEpg: () -> Unit,
    onOpenQuickSwitch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onToggleSidebar,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .background(if (sidebarExpanded) ElectricCyan.copy(alpha = 0.16f) else SurfaceCardVariant)
                .testTag("toggle_live_sidebar_btn")
        ) {
            Icon(
                imageVector = if (sidebarExpanded) Icons.Rounded.ChevronLeft else Icons.Rounded.MenuOpen,
                contentDescription = "Playlists",
                tint = ElectricCyan
            )
        }

        if (!isCompact) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search channels...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                focusedBorderColor = ElectricCyan.copy(alpha = 0.7f),
                unfocusedBorderColor = SurfaceBorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("live_search_input")
        )

        // Compact Favorites Toggle
        Surface(
            onClick = onToggleFavorites,
            shape = RoundedCornerShape(12.dp),
            color = if (favoritesOnly) ElectricCyan.copy(alpha = 0.18f) else SurfaceCard,
            border = BorderStroke(
                1.dp,
                if (favoritesOnly) ElectricCyan.copy(alpha = 0.7f) else SurfaceBorderSubtle
            ),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("live_favorites_filter")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (isCompact) 10.dp else 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (favoritesOnly) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = "Favorites",
                    tint = if (favoritesOnly) ElectricCyan else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                if (!isCompact) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Fav",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (favoritesOnly) ElectricCyan else TextSecondary
                    )
                }
            }
        }

        // EPG Toggle Pill
        Surface(
            onClick = onToggleEpg,
            shape = RoundedCornerShape(12.dp),
            color = if (showEpgInfo) DeepViolet.copy(alpha = 0.30f) else SurfaceCard,
            border = BorderStroke(
                1.dp,
                if (showEpgInfo) ElectricCyan.copy(alpha = 0.55f) else SurfaceBorderSubtle
            ),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("live_epg_toggle")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (isCompact) 10.dp else 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = "EPG",
                    tint = if (showEpgInfo) ElectricCyan else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                if (!isCompact) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "EPG",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }
        }

        // Active Source Selector Pill (matches reference top-right pill)
        if (activeSource != null) {
            Surface(
                onClick = onOpenQuickSwitch,
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                border = BorderStroke(
                    1.dp,
                    if (isOfflineMode) ErrorRose.copy(alpha = 0.7f) else SurfaceBorderSubtle
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("live_source_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOfflineMode) ErrorRose else ElectricCyan)
                    )
                    if (!isCompact) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeSource.sourceName,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Dns,
                            contentDescription = activeSource.sourceName,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Switch source",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun LiveHeroHeaderSection(
    focusedChannel: XtreamLiveChannel?,
    focusedEpg: XtreamEpgItem?,
    nowEpochSec: Long,
    isLivePreviewActive: Boolean,
    isCompact: Boolean,
    settings: UserSettings,
    previewController: NextVuPlayerController?,
    previewTelemetry: PlayerTelemetryState,
    onExpandFullScreen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenAudioDialog: () -> Unit,
    onOpenSubtitleDialog: () -> Unit,
    onClosePreview: () -> Unit
) {
    val progress = remember(focusedEpg, nowEpochSec) {
        if (focusedEpg != null && focusedEpg.stopTimestamp > focusedEpg.startTimestamp && nowEpochSec >= focusedEpg.startTimestamp) {
            val total = (focusedEpg.stopTimestamp - focusedEpg.startTimestamp).toFloat()
            ((nowEpochSec - focusedEpg.startTimestamp).toFloat() / total).coerceIn(0.03f, 1f)
        } else {
            0.22f
        }
    }

    val channelTitle = focusedChannel?.name ?: "Live TV"
    val programHeader = if (focusedEpg != null) {
        "${focusedEpg.startTime} - ${focusedEpg.endTime}  •  ${focusedEpg.title}"
    } else {
        "NO EPG Available"
    }
    val descriptionText = focusedEpg?.description?.takeIf { it.isNotBlank() }
        ?: if (focusedChannel != null) {
            "Streaming ${focusedChannel.name}. Tap the preview window or tap the channel again to launch full-screen playback."
        } else {
            "Select a playlist on the left and tap any channel row in the TV guide below to preview its stream and program schedule."
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Channel Header + Progress + EPG Title & Description
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 36.dp else 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCardVariant)
                            .border(1.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconUrl = focusedChannel?.streamIcon.orEmpty()
                        if (iconUrl.isNotBlank()) {
                            val context = LocalContext.current
                            val request = remember(iconUrl) {
                                ImageRequest.Builder(context)
                                    .data(iconUrl)
                                    .size(128)
                                    .crossfade(false)
                                    .build()
                            }
                            AsyncImage(
                                model = request,
                                contentDescription = channelTitle,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.LiveTv,
                                contentDescription = channelTitle,
                                tint = ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = channelTitle,
                        style = if (isCompact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isLivePreviewActive) ElectricCyan.copy(alpha = 0.14f) else SurfaceCard,
                    border = BorderStroke(
                        1.dp,
                        if (isLivePreviewActive) ElectricCyan.copy(alpha = 0.7f) else SurfaceBorderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isLivePreviewActive) ElectricCyan else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLivePreviewActive) "Live" else "Guide",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLivePreviewActive) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = ElectricCyan,
                    trackColor = SurfaceCardVariant,
                    modifier = Modifier
                        .width(if (isCompact) 76.dp else 110.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Text(
                    text = programHeader,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (focusedEpg != null) ElectricCyan else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = if (isCompact) 2 else 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right: Live Preview Video Window (16:9)
        val previewWidth = if (isCompact) 146.dp else 252.dp
        Box(
            modifier = Modifier
                .width(previewWidth)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .border(
                    1.2.dp,
                    if (isLivePreviewActive) ElectricCyan.copy(alpha = 0.75f) else SurfaceBorderSubtle,
                    RoundedCornerShape(12.dp)
                )
                .clickable(enabled = focusedChannel != null) { onExpandFullScreen() }
                .testTag("live_hero_preview_box")
        ) {
            if (isLivePreviewActive && previewController != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            player = previewController.exoPlayer
                            keepScreenOn = true
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        if (view.player !== previewController.exoPlayer) {
                            view.player = previewController.exoPlayer
                        }
                        view.subtitleView?.let { subView ->
                            subView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, settings.subtitleFontSizeSp)
                            val bgAlpha = (settings.subtitleBgOpacity * 255).toInt().coerceIn(0, 255)
                            subView.setStyle(
                                CaptionStyleCompat(
                                    settings.subtitleColor.argb,
                                    AndroidColor.argb(bgAlpha, 0, 0, 0),
                                    AndroidColor.TRANSPARENT,
                                    CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                                    AndroidColor.BLACK,
                                    null
                                )
                            )
                        }
                    },
                    onRelease = { view ->
                        view.player = null
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (previewTelemetry.isBuffering && previewTelemetry.errorMessage == null) {
                    CircularProgressIndicator(
                        color = ElectricCyan,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(26.dp),
                        strokeWidth = 2.5.dp
                    )
                }

                if (previewTelemetry.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BgCharcoal.copy(alpha = 0.85f))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = previewTelemetry.errorMessage ?: "Stream error",
                            style = MaterialTheme.typography.labelSmall,
                            color = ErrorRose,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Compact Preview Overlay Controls
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, BgCharcoal.copy(alpha = 0.86f))
                            )
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (previewTelemetry.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (previewTelemetry.volume <= 0.01f) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                                contentDescription = "Mute",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (!isCompact) {
                            IconButton(
                                onClick = onOpenAudioDialog,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Audiotrack,
                                    contentDescription = "Audio",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            IconButton(
                                onClick = onOpenSubtitleDialog,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ClosedCaption,
                                    contentDescription = "Subtitles",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onExpandFullScreen,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("embedded_player_fullscreen_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onClosePreview,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("embedded_player_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Preview",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BgCharcoalElevated, SurfaceCard)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Preview",
                            tint = ElectricCyan,
                            modifier = Modifier.size(if (isCompact) 24.dp else 32.dp)
                        )
                        Text(
                            text = if (focusedChannel != null) "Tap to Watch" else "Channel Preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LivePlaylistsSidebar(
    categories: List<XtreamCategory>,
    allChannelsCount: Int,
    favoritesCount: Int,
    countsByCategory: Map<String, Int>,
    selectedPlaylist: LivePlaylistSelection,
    sidebarWidth: Dp,
    onSelectPlaylist: (LivePlaylistSelection) -> Unit
) {
    Surface(
        color = BgCharcoalElevated,
        border = BorderStroke(1.dp, SurfaceBorderSubtle.copy(alpha = 0.5f)),
        modifier = Modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .testTag("live_category_sidebar")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )
                Text(
                    text = "${categories.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            HorizontalDivider(color = SurfaceBorderSubtle.copy(alpha = 0.5f))

            LazyColumn(
                contentPadding = PaddingValues(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "pl_all_channels") {
                    PlaylistSidebarRow(
                        label = "All Channels",
                        count = allChannelsCount,
                        selected = selectedPlaylist is LivePlaylistSelection.All,
                        isSpecial = true,
                        onClick = { onSelectPlaylist(LivePlaylistSelection.All) },
                        modifier = Modifier.testTag("playlist_card_all")
                    )
                }
                item(key = "pl_favorites") {
                    PlaylistSidebarRow(
                        label = "Favorites",
                        count = favoritesCount,
                        selected = selectedPlaylist is LivePlaylistSelection.Favorites,
                        isSpecial = true,
                        onClick = { onSelectPlaylist(LivePlaylistSelection.Favorites) },
                        modifier = Modifier.testTag("playlist_card_favorites")
                    )
                }
                itemsIndexed(
                    categories,
                    key = { idx, cat -> "pl_side_${cat.categoryId}_$idx" }
                ) { _, cat ->
                    val isSelected = (selectedPlaylist as? LivePlaylistSelection.Category)
                        ?.category?.categoryId == cat.categoryId
                    PlaylistSidebarRow(
                        label = cat.categoryName,
                        count = countsByCategory[cat.categoryId] ?: 0,
                        selected = isSelected,
                        isSpecial = false,
                        onClick = { onSelectPlaylist(LivePlaylistSelection.Category(cat)) },
                        modifier = Modifier.testTag("playlist_card_${cat.categoryId}")
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistSidebarRow(
    label: String,
    count: Int,
    selected: Boolean,
    isSpecial: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) ElectricCyan.copy(alpha = 0.16f) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.65f)) else null,
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isSpecial) Icons.Rounded.PlaylistPlay else Icons.Rounded.Folder,
                contentDescription = null,
                tint = if (selected) ElectricCyan else TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) ElectricCyan else TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) ElectricCyan else TextMuted
            )
        }
    }
}

@Composable
fun LiveEpgTimelineHeaderRow(
    nowEpochSec: Long,
    windowStartSec: Long,
    slotCount: Int,
    slotWidth: Dp,
    channelColWidth: Dp,
    nowOffsetDp: Dp,
    timelineScrollState: ScrollState
) {
    val headerDateText = remember(nowEpochSec / 60L) {
        SimpleDateFormat("EEE, dd MMM, HH:mm", Locale.getDefault()).format(Date(nowEpochSec * 1000L))
    }
    val timeLabels = remember(windowStartSec, slotCount) {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        List(slotCount) { idx ->
            fmt.format(Date((windowStartSec + idx * 1800L) * 1000L))
        }
    }
    val totalTimelineWidth = slotWidth * slotCount

    Surface(
        color = SurfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left header cell (current date & time)
            Box(
                modifier = Modifier
                    .width(channelColWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = headerDateText,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right horizontally-scrollable time slots header + cyan NOW dot
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(timelineScrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(totalTimelineWidth)
                        .fillMaxHeight()
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        timeLabels.forEach { label ->
                            Box(
                                modifier = Modifier
                                    .width(slotWidth)
                                    .fillMaxHeight()
                                    .padding(start = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Cyan vertical NOW line + glowing top circle node
                    Box(
                        modifier = Modifier
                            .offset(x = nowOffsetDp - 1.dp)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(ElectricCyan)
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = nowOffsetDp - 5.dp)
                            .align(Alignment.CenterStart)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveEpgGuideChannelRow(
    channel: XtreamLiveChannel,
    epgItems: List<XtreamEpgItem>,
    showEpgInfo: Boolean,
    isFavorite: Boolean,
    isPreviewing: Boolean,
    isSelectedForInfo: Boolean,
    channelColWidth: Dp,
    slotWidth: Dp,
    slotCount: Int,
    windowStartSec: Long,
    nowOffsetDp: Dp,
    timelineScrollState: ScrollState,
    onChannelClick: () -> Unit,
    onProgramClick: (XtreamEpgItem) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val windowEndSec = windowStartSec + slotCount * 1800L
    val dpPerSecond = slotWidth.value / 1800f
    val totalTimelineWidth = slotWidth * slotCount

    val timelineSegments = remember(epgItems, windowStartSec, showEpgInfo) {
        if (!showEpgInfo || epgItems.isEmpty()) {
            emptyList()
        } else {
            buildTimelineSegments(epgItems, windowStartSec, windowEndSec)
        }
    }

    val rowBg = when {
        isPreviewing -> ElectricCyan.copy(alpha = 0.11f)
        isSelectedForInfo -> SurfaceCardVariant.copy(alpha = 0.72f)
        else -> BgCharcoal
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .testTag("live_channel_card_${channel.streamId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Pinned Channel Info Column
            Row(
                modifier = Modifier
                    .width(channelColWidth)
                    .fillMaxHeight()
                    .clickable { onChannelClick() }
                    .padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(if (isPreviewing) ElectricCyan else Color.Transparent)
                )
                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCardVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (channel.streamIcon.isNotBlank()) {
                        val context = LocalContext.current
                        val iconRequest = remember(channel.streamIcon) {
                            ImageRequest.Builder(context)
                                .data(channel.streamIcon)
                                .size(96)
                                .crossfade(false)
                                .build()
                        }
                        AsyncImage(
                            model = iconRequest,
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.LiveTv,
                            contentDescription = channel.name,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isPreviewing) FontWeight.Bold else FontWeight.Medium,
                    color = if (isPreviewing) ElectricCyan else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) ElectricCyan else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Right Synchronized Horizontally-Scrollable EPG Timeline Cells
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(timelineScrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(totalTimelineWidth)
                        .fillMaxHeight()
                ) {
                    if (timelineSegments.isEmpty()) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            repeat(slotCount) { idx ->
                                Box(
                                    modifier = Modifier
                                        .width(slotWidth)
                                        .fillMaxHeight()
                                        .clickable { onChannelClick() }
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = if (showEpgInfo) "NO EPG Available" else "Live Broadcast",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isPreviewing) TextPrimary else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (idx < slotCount - 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .fillMaxHeight()
                                            .background(SurfaceBorderSubtle.copy(alpha = 0.25f))
                                    )
                                }
                            }
                        }
                    } else {
                        timelineSegments.forEach { seg ->
                            val offsetDp = ((seg.startSec - windowStartSec).coerceAtLeast(0L) * dpPerSecond).dp
                            val widthDp = ((seg.endSec - seg.startSec).coerceAtLeast(60L) * dpPerSecond).dp
                            val epg = seg.epgItem
                            val isNow = epg?.isNowPlaying == true
                            val canCatchUp = epg != null && channel.tvArchive && epg.hasArchive && !isNow

                            Box(
                                modifier = Modifier
                                    .offset(x = offsetDp)
                                    .width(widthDp)
                                    .fillMaxHeight()
                                    .background(
                                        if (isNow) ElectricCyan.copy(alpha = 0.10f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = SurfaceBorderSubtle.copy(alpha = 0.35f)
                                    )
                                    .clickable {
                                        if (epg != null) onProgramClick(epg) else onChannelClick()
                                    }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = epg?.title ?: "NO EPG Available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isNow) FontWeight.SemiBold else FontWeight.Normal,
                                        color = when {
                                            isNow -> TextPrimary
                                            epg != null -> TextSecondary
                                            else -> TextMuted
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (canCatchUp) {
                                        Icon(
                                            imageVector = Icons.Rounded.History,
                                            contentDescription = "Catch-Up",
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Cyan vertical NOW timeline beam & row node (matches reference guide!)
                    Box(
                        modifier = Modifier
                            .offset(x = nowOffsetDp - 1.dp)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(ElectricCyan.copy(alpha = 0.9f))
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = nowOffsetDp - 4.dp)
                            .align(Alignment.CenterStart)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BgCharcoal)
                            .border(2.dp, ElectricCyan, CircleShape)
                    )
                }
            }
        }
        HorizontalDivider(color = SurfaceBorderSubtle.copy(alpha = 0.35f), thickness = 0.7.dp)
    }
}

private fun buildTimelineSegments(
    epgItems: List<XtreamEpgItem>,
    windowStartSec: Long,
    windowEndSec: Long
): List<TimelineSegment> {
    val overlapping = epgItems
        .filter { it.stopTimestamp > windowStartSec && it.startTimestamp < windowEndSec && it.stopTimestamp > it.startTimestamp }
        .sortedBy { it.startTimestamp }

    if (overlapping.isEmpty()) return emptyList()

    val segments = mutableListOf<TimelineSegment>()
    var cursor = windowStartSec
    for (item in overlapping) {
        val s = item.startTimestamp.coerceIn(windowStartSec, windowEndSec)
        val e = item.stopTimestamp.coerceIn(windowStartSec, windowEndSec)
        if (e <= cursor) continue
        if (s > cursor) {
            segments.add(TimelineSegment(cursor, s, null))
        }
        segments.add(TimelineSegment(maxOf(cursor, s), e, item))
        cursor = e
    }
    if (cursor < windowEndSec) {
        segments.add(TimelineSegment(cursor, windowEndSec, null))
    }
    return segments
}
