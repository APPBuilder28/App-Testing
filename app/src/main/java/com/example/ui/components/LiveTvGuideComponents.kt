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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
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
import kotlin.math.roundToInt

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

data class LiveTvUiMetrics(
    val scalePercent: Int,
    val topBarVerticalPad: Dp,
    val toolbarControlHeight: Dp,
    val searchTextSp: TextUnit,
    val heroVerticalPad: Dp,
    val heroLogoSize: Dp,
    val heroTitleSp: TextUnit,
    val heroProgramSp: TextUnit,
    val heroDescSp: TextUnit,
    val heroDescLineHeightSp: TextUnit,
    val heroDescMaxLines: Int,
    val heroProgressWidth: Dp,
    val previewWidth: Dp,
    val previewControlSize: Dp,
    val sidebarWidth: Dp,
    val playlistHeaderPad: Dp,
    val playlistRowPadV: Dp,
    val playlistFontSp: TextUnit,
    val playlistCountSp: TextUnit,
    val playlistIconSize: Dp,
    val timelineHeaderHeight: Dp,
    val timelineHeaderSp: TextUnit,
    val channelColWidth: Dp,
    val guideRowHeight: Dp,
    val guideLogoSize: Dp,
    val guideChannelSp: TextUnit,
    val guideProgramSp: TextUnit,
    val slotWidth: Dp
) {
    companion object {
        fun forScale(scalePercent: Int, isCompact: Boolean): LiveTvUiMetrics {
            val clamped = scalePercent.coerceIn(0, 100)
            val t = clamped / 100f
            val descLines = when {
                clamped <= 25 -> 1
                clamped <= 70 -> 2
                else -> 3
            }
            return LiveTvUiMetrics(
                scalePercent = clamped,
                topBarVerticalPad = lerp(2.dp, 8.dp, t),
                toolbarControlHeight = lerp(32.dp, 46.dp, t),
                searchTextSp = lerpSp(11.sp, 14.sp, t),
                heroVerticalPad = lerp(2.dp, 8.dp, t),
                heroLogoSize = lerp(22.dp, 44.dp, t),
                heroTitleSp = lerpSp(13.sp, 21.sp, t),
                heroProgramSp = lerpSp(10.sp, 14.sp, t),
                heroDescSp = lerpSp(9.5.sp, 13.sp, t),
                heroDescLineHeightSp = lerpSp(12.sp, 17.sp, t),
                heroDescMaxLines = descLines,
                heroProgressWidth = lerp(48.dp, 110.dp, t),
                previewWidth = if (isCompact) lerp(94.dp, 160.dp, t) else lerp(116.dp, 260.dp, t),
                previewControlSize = lerp(20.dp, 30.dp, t),
                sidebarWidth = if (isCompact) lerp(136.dp, 196.dp, t) else lerp(176.dp, 256.dp, t),
                playlistHeaderPad = lerp(5.dp, 10.dp, t),
                playlistRowPadV = lerp(3.dp, 9.dp, t),
                playlistFontSp = lerpSp(10.sp, 13.sp, t),
                playlistCountSp = lerpSp(9.sp, 11.sp, t),
                playlistIconSize = lerp(12.dp, 16.dp, t),
                timelineHeaderHeight = lerp(24.dp, 40.dp, t),
                timelineHeaderSp = lerpSp(9.5.sp, 13.sp, t),
                channelColWidth = if (isCompact) lerp(96.dp, 148.dp, t) else lerp(140.dp, 220.dp, t),
                guideRowHeight = lerp(34.dp, 62.dp, t),
                guideLogoSize = lerp(20.dp, 36.dp, t),
                guideChannelSp = lerpSp(10.5.sp, 14.5.sp, t),
                guideProgramSp = lerpSp(10.sp, 14.sp, t),
                slotWidth = lerp(90.dp, 156.dp, t)
            )
        }

        private fun lerpSp(start: TextUnit, stop: TextUnit, fraction: Float): TextUnit {
            return (start.value + (stop.value - start.value) * fraction).sp
        }
    }
}

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
    showScalePanel: Boolean,
    metrics: LiveTvUiMetrics,
    isCompact: Boolean,
    onToggleSidebar: () -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleEpg: () -> Unit,
    onToggleScalePanel: () -> Unit,
    onOpenQuickSwitch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = metrics.topBarVerticalPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = onToggleSidebar,
            shape = RoundedCornerShape(10.dp),
            color = if (sidebarExpanded) ElectricCyan.copy(alpha = 0.16f) else SurfaceCardVariant,
            modifier = Modifier
                .size(metrics.toolbarControlHeight)
                .testTag("toggle_live_sidebar_btn")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (sidebarExpanded) Icons.Rounded.ChevronLeft else Icons.Rounded.MenuOpen,
                    contentDescription = "Playlists",
                    tint = ElectricCyan,
                    modifier = Modifier.size((metrics.toolbarControlHeight * 0.52f).coerceAtLeast(16.dp))
                )
            }
        }

        if (!isCompact) {
            Text(
                text = title,
                fontSize = metrics.heroTitleSp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
        }

        // Custom sleek pill Search field that scales down cleanly without clipping
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, SurfaceBorderSubtle),
            modifier = Modifier
                .weight(1f)
                .height(metrics.toolbarControlHeight)
                .testTag("live_search_input")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size((metrics.toolbarControlHeight * 0.45f).coerceAtLeast(14.dp))
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search channels...",
                            fontSize = metrics.searchTextSp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = metrics.searchTextSp
                        ),
                        cursorBrush = SolidColor(ElectricCyan),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onSearchQueryChange("") }
                    )
                }
            }
        }

        // UI Scale 0-100% Toggle Pill
        Surface(
            onClick = onToggleScalePanel,
            shape = RoundedCornerShape(10.dp),
            color = if (showScalePanel) ElectricCyan.copy(alpha = 0.2f) else SurfaceCard,
            border = BorderStroke(
                1.dp,
                if (showScalePanel) ElectricCyan else SurfaceBorderSubtle
            ),
            modifier = Modifier
                .height(metrics.toolbarControlHeight)
                .testTag("live_ui_scale_btn")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "UI Scale",
                    tint = ElectricCyan,
                    modifier = Modifier.size((metrics.toolbarControlHeight * 0.45f).coerceAtLeast(14.dp))
                )
                Text(
                    text = "${metrics.scalePercent}%",
                    fontSize = metrics.playlistFontSp,
                    fontWeight = FontWeight.SemiBold,
                    color = ElectricCyan
                )
            }
        }

        // Compact Favorites Toggle
        Surface(
            onClick = onToggleFavorites,
            shape = RoundedCornerShape(10.dp),
            color = if (favoritesOnly) ElectricCyan.copy(alpha = 0.18f) else SurfaceCard,
            border = BorderStroke(
                1.dp,
                if (favoritesOnly) ElectricCyan.copy(alpha = 0.7f) else SurfaceBorderSubtle
            ),
            modifier = Modifier
                .height(metrics.toolbarControlHeight)
                .testTag("live_favorites_filter")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (isCompact) 8.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (favoritesOnly) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = "Favorites",
                    tint = if (favoritesOnly) ElectricCyan else TextSecondary,
                    modifier = Modifier.size((metrics.toolbarControlHeight * 0.45f).coerceAtLeast(14.dp))
                )
                if (!isCompact) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Fav",
                        fontSize = metrics.playlistFontSp,
                        color = if (favoritesOnly) ElectricCyan else TextSecondary
                    )
                }
            }
        }

        // EPG Toggle Pill
        Surface(
            onClick = onToggleEpg,
            shape = RoundedCornerShape(10.dp),
            color = if (showEpgInfo) DeepViolet.copy(alpha = 0.30f) else SurfaceCard,
            border = BorderStroke(
                1.dp,
                if (showEpgInfo) ElectricCyan.copy(alpha = 0.55f) else SurfaceBorderSubtle
            ),
            modifier = Modifier
                .height(metrics.toolbarControlHeight)
                .testTag("live_epg_toggle")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (isCompact) 8.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = "EPG",
                    tint = if (showEpgInfo) ElectricCyan else TextSecondary,
                    modifier = Modifier.size((metrics.toolbarControlHeight * 0.45f).coerceAtLeast(14.dp))
                )
                if (!isCompact) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "EPG",
                        fontSize = metrics.playlistFontSp,
                        color = TextPrimary
                    )
                }
            }
        }

        // Active Source Selector Pill
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
                    .height(metrics.toolbarControlHeight)
                    .testTag("live_source_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isOfflineMode) ErrorRose else ElectricCyan)
                    )
                    if (!isCompact) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = activeSource.sourceName,
                            fontSize = metrics.playlistFontSp,
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
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Switch source",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveTvScaleControlPanel(
    scalePercent: Int,
    onScaleChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BgCharcoalElevated.copy(alpha = 0.98f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.65f)),
        shadowElevation = 8.dp,
        modifier = modifier
            .widthIn(min = 260.dp, max = 360.dp)
            .testTag("live_ui_scale_panel")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live TV UI Scale",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "$scalePercent%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )
            }

            Slider(
                value = scalePercent.toFloat(),
                onValueChange = { onScaleChange(it.roundToInt().coerceIn(0, 100)) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = ElectricCyan,
                    activeTrackColor = ElectricCyan,
                    inactiveTrackColor = SurfaceCardVariant
                ),
                modifier = Modifier.testTag("live_ui_scale_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0, 25, 50, 75, 100).forEach { preset ->
                        FilterChip(
                            selected = scalePercent == preset,
                            onClick = { onScaleChange(preset) },
                            label = {
                                Text(
                                    text = "$preset%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                selectedLabelColor = ElectricCyan
                            )
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Done", color = ElectricCyan)
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
    metrics: LiveTvUiMetrics,
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
            "Streaming ${focusedChannel.name}. Tap preview or channel to launch full-screen."
        } else {
            "Select a playlist on the left and tap any channel in the TV guide below to preview."
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = metrics.heroVerticalPad),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Channel Header + Progress + EPG Title & Description
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy((metrics.heroVerticalPad * 0.6f).coerceAtLeast(2.dp))
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
                            .size(metrics.heroLogoSize)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCardVariant)
                            .border(1.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
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
                                    .padding(3.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.LiveTv,
                                contentDescription = channelTitle,
                                tint = ElectricCyan,
                                modifier = Modifier.size((metrics.heroLogoSize * 0.55f).coerceAtLeast(14.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = channelTitle,
                        fontSize = metrics.heroTitleSp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isLivePreviewActive) ElectricCyan.copy(alpha = 0.14f) else SurfaceCard,
                    border = BorderStroke(
                        1.dp,
                        if (isLivePreviewActive) ElectricCyan.copy(alpha = 0.7f) else SurfaceBorderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isLivePreviewActive) ElectricCyan else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isLivePreviewActive) "Live" else "Guide",
                            fontSize = metrics.playlistCountSp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLivePreviewActive) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = ElectricCyan,
                    trackColor = SurfaceCardVariant,
                    modifier = Modifier
                        .width(metrics.heroProgressWidth)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Text(
                    text = programHeader,
                    fontSize = metrics.heroProgramSp,
                    fontWeight = FontWeight.Medium,
                    color = if (focusedEpg != null) ElectricCyan else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = descriptionText,
                fontSize = metrics.heroDescSp,
                lineHeight = metrics.heroDescLineHeightSp,
                color = TextSecondary,
                maxLines = metrics.heroDescMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right: Scaled 16:9 Live Preview Video Window
        Box(
            modifier = Modifier
                .width(metrics.previewWidth)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black)
                .border(
                    1.2.dp,
                    if (isLivePreviewActive) ElectricCyan.copy(alpha = 0.75f) else SurfaceBorderSubtle,
                    RoundedCornerShape(10.dp)
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
                            .size(22.dp),
                        strokeWidth = 2.dp
                    )
                }

                if (previewTelemetry.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BgCharcoal.copy(alpha = 0.85f))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = previewTelemetry.errorMessage ?: "Stream error",
                            fontSize = metrics.playlistCountSp,
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
                            modifier = Modifier.size(metrics.previewControlSize)
                        ) {
                            Icon(
                                imageVector = if (previewTelemetry.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = ElectricCyan,
                                modifier = Modifier.size((metrics.previewControlSize * 0.65f).coerceAtLeast(13.dp))
                            )
                        }
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.size(metrics.previewControlSize)
                        ) {
                            Icon(
                                imageVector = if (previewTelemetry.volume <= 0.01f) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                                contentDescription = "Mute",
                                tint = TextPrimary,
                                modifier = Modifier.size((metrics.previewControlSize * 0.6f).coerceAtLeast(12.dp))
                            )
                        }
                        if (!isCompact && metrics.scalePercent >= 25) {
                            IconButton(
                                onClick = onOpenAudioDialog,
                                modifier = Modifier.size(metrics.previewControlSize)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Audiotrack,
                                    contentDescription = "Audio",
                                    tint = TextSecondary,
                                    modifier = Modifier.size((metrics.previewControlSize * 0.55f).coerceAtLeast(12.dp))
                                )
                            }
                            IconButton(
                                onClick = onOpenSubtitleDialog,
                                modifier = Modifier.size(metrics.previewControlSize)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ClosedCaption,
                                    contentDescription = "Subtitles",
                                    tint = TextSecondary,
                                    modifier = Modifier.size((metrics.previewControlSize * 0.55f).coerceAtLeast(12.dp))
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onExpandFullScreen,
                            modifier = Modifier
                                .size(metrics.previewControlSize)
                                .testTag("embedded_player_fullscreen_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = ElectricCyan,
                                modifier = Modifier.size((metrics.previewControlSize * 0.65f).coerceAtLeast(13.dp))
                            )
                        }
                        IconButton(
                            onClick = onClosePreview,
                            modifier = Modifier
                                .size(metrics.previewControlSize)
                                .testTag("embedded_player_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Preview",
                                tint = TextSecondary,
                                modifier = Modifier.size((metrics.previewControlSize * 0.6f).coerceAtLeast(12.dp))
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
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Preview",
                            tint = ElectricCyan,
                            modifier = Modifier.size(if (isCompact) 20.dp else 26.dp)
                        )
                        Text(
                            text = if (focusedChannel != null) "Tap to Watch" else "Preview",
                            fontSize = metrics.playlistCountSp,
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
    metrics: LiveTvUiMetrics,
    onSelectPlaylist: (LivePlaylistSelection) -> Unit
) {
    Surface(
        color = BgCharcoalElevated,
        border = BorderStroke(1.dp, SurfaceBorderSubtle.copy(alpha = 0.5f)),
        modifier = Modifier
            .width(metrics.sidebarWidth)
            .fillMaxHeight()
            .testTag("live_category_sidebar")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard)
                    .padding(horizontal = 8.dp, vertical = metrics.playlistHeaderPad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Playlists",
                    fontSize = metrics.playlistFontSp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )
                Text(
                    text = "${categories.size}",
                    fontSize = metrics.playlistCountSp,
                    color = TextMuted
                )
            }
            HorizontalDivider(color = SurfaceBorderSubtle.copy(alpha = 0.5f))

            LazyColumn(
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "pl_all_channels") {
                    PlaylistSidebarRow(
                        label = "All Channels",
                        count = allChannelsCount,
                        selected = selectedPlaylist is LivePlaylistSelection.All,
                        isSpecial = true,
                        metrics = metrics,
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
                        metrics = metrics,
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
                        metrics = metrics,
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
    metrics: LiveTvUiMetrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(7.dp),
        color = when {
            isFocused -> ElectricCyan.copy(alpha = 0.24f)
            selected -> ElectricCyan.copy(alpha = 0.16f)
            else -> Color.Transparent
        },
        border = when {
            isFocused -> BorderStroke(2.dp, ElectricCyan)
            selected -> BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.65f))
            else -> null
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = metrics.playlistRowPadV),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isSpecial) Icons.Rounded.PlaylistPlay else Icons.Rounded.Folder,
                contentDescription = null,
                tint = if (selected || isFocused) ElectricCyan else TextMuted,
                modifier = Modifier.size(metrics.playlistIconSize)
            )
            Text(
                text = label,
                fontSize = metrics.playlistFontSp,
                lineHeight = (metrics.playlistFontSp.value * 1.22f).sp,
                fontWeight = if (selected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected || isFocused) ElectricCyan else TextPrimary,
                softWrap = true,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$count",
                fontSize = metrics.playlistCountSp,
                color = if (selected || isFocused) ElectricCyan else TextMuted
            )
        }
    }
}

@Composable
fun LiveEpgTimelineHeaderRow(
    nowEpochSec: Long,
    windowStartSec: Long,
    slotCount: Int,
    metrics: LiveTvUiMetrics,
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
    val totalTimelineWidth = metrics.slotWidth * slotCount

    Surface(
        color = SurfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.timelineHeaderHeight)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(metrics.channelColWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = headerDateText,
                    fontSize = metrics.timelineHeaderSp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

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
                                    .width(metrics.slotWidth)
                                    .fillMaxHeight()
                                    .padding(start = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = label,
                                    fontSize = metrics.timelineHeaderSp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = nowOffsetDp - 1.dp)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(ElectricCyan)
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = nowOffsetDp - 4.dp)
                            .align(Alignment.CenterStart)
                            .size(8.dp)
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
    metrics: LiveTvUiMetrics,
    slotCount: Int,
    windowStartSec: Long,
    nowOffsetDp: Dp,
    timelineScrollState: ScrollState,
    onChannelClick: () -> Unit,
    onProgramClick: (XtreamEpgItem) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val windowEndSec = windowStartSec + slotCount * 1800L
    val dpPerSecond = metrics.slotWidth.value / 1800f
    val totalTimelineWidth = metrics.slotWidth * slotCount

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
                .height(metrics.guideRowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Pinned Channel Info Column
            Row(
                modifier = Modifier
                    .width(metrics.channelColWidth)
                    .fillMaxHeight()
                    .clickable { onChannelClick() }
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(if (isPreviewing) ElectricCyan else Color.Transparent)
                )
                Spacer(modifier = Modifier.width(5.dp))

                Box(
                    modifier = Modifier
                        .size(metrics.guideLogoSize)
                        .clip(RoundedCornerShape(6.dp))
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
                                .padding(2.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.LiveTv,
                            contentDescription = channel.name,
                            tint = ElectricCyan,
                            modifier = Modifier.size((metrics.guideLogoSize * 0.58f).coerceAtLeast(12.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = channel.name,
                    fontSize = metrics.guideChannelSp,
                    fontWeight = if (isPreviewing) FontWeight.Bold else FontWeight.Medium,
                    color = if (isPreviewing) ElectricCyan else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size((metrics.guideRowHeight * 0.62f).coerceAtLeast(24.dp))
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) ElectricCyan else TextMuted,
                        modifier = Modifier.size((metrics.guideLogoSize * 0.55f).coerceAtLeast(13.dp))
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
                                        .width(metrics.slotWidth)
                                        .fillMaxHeight()
                                        .clickable { onChannelClick() }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = if (showEpgInfo) "NO EPG Available" else "Live Broadcast",
                                        fontSize = metrics.guideProgramSp,
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
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = epg?.title ?: "NO EPG Available",
                                        fontSize = metrics.guideProgramSp,
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
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Cyan vertical NOW timeline beam & row node
                    Box(
                        modifier = Modifier
                            .offset(x = nowOffsetDp - 1.dp)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(ElectricCyan.copy(alpha = 0.9f))
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = nowOffsetDp - 3.5.dp)
                            .align(Alignment.CenterStart)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(BgCharcoal)
                            .border(1.5.dp, ElectricCyan, CircleShape)
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
