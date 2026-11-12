package com.example.ui.components

import android.graphics.Color as AndroidColor
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.example.data.local.UserSettings
import com.example.data.remote.XtreamEpgItem
import com.example.data.remote.XtreamLiveChannel
import com.example.player.HdrBadgeType
import com.example.player.NextVuPlayerController
import com.example.player.PlayerTelemetryState
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.BgCharcoalElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.HdrGold
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(UnstableApi::class)
@Composable
fun EmbeddedLivePlayerDock(
    channel: XtreamLiveChannel,
    epgItems: List<XtreamEpgItem>,
    showEpgPanel: Boolean,
    isWideLayout: Boolean,
    isLocked: Boolean,
    settings: UserSettings,
    controller: NextVuPlayerController,
    telemetry: PlayerTelemetryState,
    onTogglePlayPause: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPrevChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onToggleEpg: () -> Unit,
    onOpenAudioDialog: () -> Unit,
    onOpenSubtitleDialog: () -> Unit,
    onToggleLock: () -> Unit,
    onExpandFullScreen: () -> Unit,
    onClosePreview: () -> Unit,
    onPlayCatchUp: (XtreamEpgItem) -> Unit
) {
    Surface(
        color = BgCharcoalElevated,
        border = BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("embedded_live_player_dock")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Now Playing EPG",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${channel.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (telemetry.hdrType != HdrBadgeType.NONE) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = HdrGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, HdrGold)
                        ) {
                            Text(
                                text = telemetry.hdrType.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = HdrGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onExpandFullScreen,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("embedded_player_fullscreen_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = ElectricCyan
                        )
                    }
                    IconButton(
                        onClick = onClosePreview,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("embedded_player_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Preview",
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val playerHeight = if (isWideLayout) 190.dp else 168.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(playerHeight),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(if (isWideLayout && showEpgPanel) 1.25f else 1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .clickable { onExpandFullScreen() }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                player = controller.exoPlayer
                                keepScreenOn = true
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
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
                        modifier = Modifier.fillMaxSize()
                    )

                    if (telemetry.isBuffering && telemetry.errorMessage == null) {
                        CircularProgressIndicator(
                            color = ElectricCyan,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(30.dp),
                            strokeWidth = 2.5.dp
                        )
                    }

                    if (telemetry.errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BgCharcoal.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = telemetry.errorMessage ?: "Stream error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorRose,
                                maxLines = 2
                            )
                        }
                    }
                }

                if (isWideLayout && showEpgPanel) {
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SurfaceBorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (epgItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No EPG schedule available for ${channel.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(
                                    epgItems,
                                    key = { idx, epg -> "dock_epg_${epg.id}_$idx" }
                                ) { _, epg ->
                                    val canCatchUp = channel.tvArchive && epg.hasArchive && !epg.isNowPlaying
                                    Surface(
                                        onClick = { if (canCatchUp) onPlayCatchUp(epg) },
                                        enabled = canCatchUp,
                                        color = if (epg.isNowPlaying) ElectricCyan.copy(alpha = 0.14f) else SurfaceCardVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (epg.isNowPlaying) ElectricCyan.copy(alpha = 0.6f) else SurfaceBorderSubtle
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${epg.startTime} - ${epg.endTime}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (epg.isNowPlaying) ElectricCyan else TextMuted
                                                )
                                                Text(
                                                    text = epg.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (canCatchUp) {
                                                Icon(
                                                    imageVector = Icons.Rounded.History,
                                                    contentDescription = "Catch-Up",
                                                    tint = ElectricCyan,
                                                    modifier = Modifier.size(16.dp)
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onTogglePlayPause,
                        enabled = !isLocked,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan)
                            .testTag("dock_play_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (telemetry.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = BgCharcoal
                        )
                    }

                    IconButton(
                        onClick = onPrevChannel,
                        enabled = !isLocked,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous channel", tint = TextPrimary)
                    }

                    IconButton(
                        onClick = onNextChannel,
                        enabled = !isLocked,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Next channel", tint = TextPrimary)
                    }

                    if (isWideLayout) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(120.dp)
                        ) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = "Volume", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Slider(
                                value = telemetry.volume,
                                onValueChange = onVolumeChange,
                                enabled = !isLocked,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricCyan,
                                    activeTrackColor = ElectricCyan,
                                    inactiveTrackColor = SurfaceCardVariant
                                )
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleEpg,
                        enabled = !isLocked,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = "EPG",
                            tint = if (showEpgPanel) ElectricCyan else TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onOpenAudioDialog,
                        enabled = !isLocked,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Audiotrack, contentDescription = "Audio", tint = TextSecondary)
                    }
                    IconButton(
                        onClick = onOpenSubtitleDialog,
                        enabled = !isLocked,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.ClosedCaption, contentDescription = "Subtitles", tint = TextSecondary)
                    }
                    IconButton(
                        onClick = onToggleLock,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = "Lock",
                            tint = if (isLocked) ElectricCyan else TextSecondary
                        )
                    }
                }
            }
        }
    }
}
