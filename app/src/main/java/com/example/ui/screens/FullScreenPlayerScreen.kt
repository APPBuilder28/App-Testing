package com.example.ui.screens

import android.graphics.Color as AndroidColor
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.example.core.i18n.LocalAppStrings
import com.example.data.local.UserSettings
import com.example.data.local.VideoQualityPreset
import com.example.data.remote.XtreamEpgItem
import com.example.data.remote.XtreamEpisode
import com.example.data.remote.XtreamLiveChannel
import com.example.data.remote.XtreamSeries
import com.example.player.HdrBadgeType
import com.example.player.NextVuPlayerController
import com.example.ui.components.NextVuInteractiveCard
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.BgCharcoalElevated
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.HdrGold
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.PlaybackTarget
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun FullScreenPlayerScreen(
    target: PlaybackTarget,
    settings: UserSettings,
    epgList: List<XtreamEpgItem>,
    onBack: () -> Unit,
    onSaveProgress: (PlaybackTarget, Long, Long) -> Unit,
    onToggleBitrateHud: (Boolean) -> Unit,
    onSelectQuality: (VideoQualityPreset) -> Unit,
    onToggleAutoNextEpisode: (Boolean) -> Unit,
    onPlayCatchUp: (XtreamLiveChannel, XtreamEpgItem) -> Unit,
    onRetryLiveWithTs: (XtreamLiveChannel) -> Unit,
    onPlayNextEpisode: (XtreamSeries, XtreamEpisode, List<XtreamEpisode>) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val controller = remember { NextVuPlayerController(context) }
    val telemetry by controller.state.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var showEpgPanel by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var pending4kAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var hasConfirmed4kInitial by remember(target) { mutableStateOf(false) }

    val streamUrl = when (target) {
        is PlaybackTarget.Live -> target.streamUrl
        is PlaybackTarget.Movie -> target.streamUrl
        is PlaybackTarget.SeriesEpisode -> target.streamUrl
    }
    val startPositionMs = when (target) {
        is PlaybackTarget.Live -> 0L
        is PlaybackTarget.Movie -> target.resumePositionMs
        is PlaybackTarget.SeriesEpisode -> target.resumePositionMs
    }
    val titleText = when (target) {
        is PlaybackTarget.Live -> if (target.isCatchUp) "${target.channel.name} • ${target.catchUpTitle}" else target.channel.name
        is PlaybackTarget.Movie -> target.movie.name
        is PlaybackTarget.SeriesEpisode -> "${target.series.name} • S${target.episode.seasonNumber}E${target.episode.episodeNumber} ${target.episode.title}"
    }

    // Check 4K warning popup before loading 4K preset streams
    LaunchedEffect(target) {
        if (settings.videoQuality == VideoQualityPreset.UHD_4K && !hasConfirmed4kInitial) {
            pending4kAction = {
                hasConfirmed4kInitial = true
                controller.prepareAndPlay(streamUrl, startPositionMs, settings.videoQuality)
            }
        } else {
            controller.prepareAndPlay(streamUrl, startPositionMs, settings.videoQuality)
        }
    }

    // Periodic position sync & progress persistence
    LaunchedEffect(target) {
        while (true) {
            controller.syncPosition()
            if (target !is PlaybackTarget.Live && telemetry.durationMs > 0L) {
                onSaveProgress(target, telemetry.positionMs, telemetry.durationMs)
            }
            delay(2000L)
        }
    }

    // Auto-next episode when episode completes
    LaunchedEffect(telemetry.playbackEnded, target, settings.autoNextEpisode) {
        if (telemetry.playbackEnded && target is PlaybackTarget.SeriesEpisode && settings.autoNextEpisode) {
            val idx = target.seasonEpisodes.indexOfFirst { it.episodeId == target.episode.episodeId }
            val next = if (idx >= 0) target.seasonEpisodes.getOrNull(idx + 1) else null
            if (next != null) {
                onPlayNextEpisode(target.series, next, target.seasonEpisodes)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.syncPosition()
            val pos = controller.exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = controller.exoPlayer.duration.coerceAtLeast(0L)
            if (target !is PlaybackTarget.Live && pos > 0L) {
                onSaveProgress(target, pos, dur)
            }
            controller.release()
        }
    }

    BackHandler {
        if (showEpgPanel) {
            showEpgPanel = false
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
            .testTag("fullscreen_player_screen")
    ) {
        // Media3 PlayerView Surface + Customized Subtitles
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
                    val bgAlphaInt = (settings.subtitleBgOpacity * 255).toInt().coerceIn(0, 255)
                    val bgColor = AndroidColor.argb(bgAlphaInt, 0, 0, 0)
                    subView.setStyle(
                        CaptionStyleCompat(
                            settings.subtitleColor.argb,
                            bgColor,
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

        // Buffering Indicator Overlay (for high-bitrate 4K/HEVC streams)
        if (telemetry.isBuffering && telemetry.errorMessage == null) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = BgCharcoal.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        CircularProgressIndicator(color = ElectricCyan)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (telemetry.is4kStream || telemetry.isHevcStream) {
                                "Buffering High-Bitrate Stream..."
                            } else "Buffering...",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Top Telemetry Badges (HDR + Bitrate HUD + HEVC Fallback Warning)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (telemetry.hdrType != HdrBadgeType.NONE) {
                    Surface(
                        color = HdrGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, HdrGold),
                        modifier = Modifier.testTag("player_hdr_badge")
                    ) {
                        Text(
                            text = telemetry.hdrType.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = HdrGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (telemetry.videoHeight > 0) {
                    Surface(
                        color = BgCharcoal.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = if (telemetry.is4kStream) "4K ${telemetry.videoHeight}p" else "${telemetry.videoHeight}p",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (settings.showBitrateOverlay) {
                    val mbps = telemetry.bitrateKbps / 1000.0
                    val bitrateLabel = if (telemetry.bitrateKbps >= 1000L) {
                        String.format("%.1f Mbps", mbps)
                    } else {
                        "${telemetry.bitrateKbps} kbps"
                    }
                    Surface(
                        color = BgCharcoal.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DeepViolet.copy(alpha = 0.6f)),
                        modifier = Modifier.testTag("player_bitrate_overlay")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Speed, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(bitrateLabel, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        }
                    }
                }
            }

            if (telemetry.showHevcSoftwareFallbackWarning) {
                Surface(
                    color = WarningAmber.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.7f)),
                    modifier = Modifier.testTag("hevc_software_warning")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.hevcFallbackWarning,
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningAmber
                        )
                    }
                }
            }
        }

        // Screen Lock Overlay
        if (isScreenLocked && controlsVisible) {
            IconButton(
                onClick = { isScreenLocked = false },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(20.dp)
                    .clip(CircleShape)
                    .background(BgCharcoal.copy(alpha = 0.8f))
                    .minimumInteractiveComponentSize()
                    .testTag("player_unlock_button")
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = "Unlock screen", tint = ElectricCyan)
            }
        }

        // Player Controls Overlay
        AnimatedVisibility(visible = controlsVisible && !isScreenLocked && telemetry.errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                BgCharcoal.copy(alpha = 0.78f),
                                Color.Transparent,
                                BgCharcoal.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(0.7f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceCard.copy(alpha = 0.75f))
                            .minimumInteractiveComponentSize()
                            .testTag("player_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Center Play/Pause + Seek +/- 10s
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (target !is PlaybackTarget.Live || target.isCatchUp) {
                        IconButton(
                            onClick = { controller.seekBy(-10_000L) },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SurfaceCard.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10s", tint = TextPrimary, modifier = Modifier.size(30.dp))
                        }
                    }

                    IconButton(
                        onClick = { controller.togglePlayPause() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (telemetry.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (telemetry.isPlaying) "Pause" else "Play",
                            tint = BgCharcoal,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    if (target !is PlaybackTarget.Live || target.isCatchUp) {
                        IconButton(
                            onClick = { controller.seekBy(10_000L) },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SurfaceCard.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10s", tint = TextPrimary, modifier = Modifier.size(30.dp))
                        }
                    }
                }

                // Bottom Control Dock
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    if (target !is PlaybackTarget.Live || target.isCatchUp) {
                        val dur = telemetry.durationMs.coerceAtLeast(1L)
                        val pos = telemetry.positionMs.coerceIn(0L, dur)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(formatClockMs(pos), style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                            Slider(
                                value = pos.toFloat(),
                                onValueChange = { controller.seekTo(it.toLong()) },
                                valueRange = 0f..dur.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricCyan,
                                    activeTrackColor = ElectricCyan,
                                    inactiveTrackColor = SurfaceCardVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                                    .testTag("player_seek_bar")
                            )
                            Text(formatClockMs(dur), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Volume Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(160.dp)
                        ) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = "Volume", tint = ElectricCyan, modifier = Modifier.size(20.dp))
                            Slider(
                                value = telemetry.volume,
                                onValueChange = { controller.setVolume(it) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricCyan,
                                    activeTrackColor = ElectricCyan,
                                    inactiveTrackColor = SurfaceCardVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("player_volume_slider")
                            )
                        }

                        // Action Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (target is PlaybackTarget.SeriesEpisode) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Next Ep", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Switch(
                                        checked = settings.autoNextEpisode,
                                        onCheckedChange = onToggleAutoNextEpisode,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = BgCharcoal,
                                            checkedTrackColor = ElectricCyan
                                        ),
                                        modifier = Modifier.testTag("player_auto_next_toggle")
                                    )
                                }
                            }

                            if (target is PlaybackTarget.Live) {
                                PlayerToolButton(
                                    icon = Icons.Rounded.Schedule,
                                    label = "EPG",
                                    onClick = { showEpgPanel = !showEpgPanel },
                                    modifier = Modifier.testTag("player_epg_button")
                                )
                            }

                            PlayerToolButton(
                                icon = Icons.Rounded.HighQuality,
                                label = telemetry.selectedQuality.label,
                                onClick = { showResolutionDialog = true },
                                modifier = Modifier.testTag("player_resolution_button")
                            )

                            PlayerToolButton(
                                icon = Icons.Rounded.Audiotrack,
                                label = "Audio",
                                onClick = { showAudioDialog = true },
                                modifier = Modifier.testTag("player_audio_button")
                            )

                            PlayerToolButton(
                                icon = Icons.Rounded.ClosedCaption,
                                label = "Sub",
                                onClick = { showSubtitleDialog = true },
                                modifier = Modifier.testTag("player_subtitle_button")
                            )

                            PlayerToolButton(
                                icon = Icons.Rounded.Speed,
                                label = "HUD",
                                onClick = { onToggleBitrateHud(!settings.showBitrateOverlay) },
                                modifier = Modifier.testTag("player_bitrate_toggle")
                            )

                            PlayerToolButton(
                                icon = Icons.Rounded.LockOpen,
                                label = "Lock",
                                onClick = { isScreenLocked = true },
                                modifier = Modifier.testTag("player_lock_button")
                            )
                        }
                    }
                }
            }
        }

        // Slide-In EPG Timeline & Catch-Up Panel (Live TV)
        AnimatedVisibility(
            visible = showEpgPanel && target is PlaybackTarget.Live,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            val liveTarget = target as PlaybackTarget.Live
            Surface(
                color = BgCharcoalElevated.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight()
                    .testTag("player_epg_slide_panel")
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EPG Timeline & Catch-Up",
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricCyan
                        )
                        TextButton(onClick = { showEpgPanel = false }) {
                            Text("Close", color = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (epgList.isEmpty()) {
                        Text(
                            text = "No EPG entries provided by this Xtream server for ${liveTarget.channel.name}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(epgList, key = { it.id }) { epg ->
                                val canReplay = liveTarget.channel.tvArchive && epg.hasArchive && !epg.isNowPlaying
                                NextVuInteractiveCard(
                                    isActive = epg.isNowPlaying,
                                    onClick = {
                                        if (canReplay) {
                                            showEpgPanel = false
                                            onPlayCatchUp(liveTarget.channel, epg)
                                        }
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${epg.startTime} - ${epg.endTime}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (epg.isNowPlaying) ElectricCyan else TextMuted
                                            )
                                            if (canReplay) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.History, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("Catch-Up", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(epg.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        if (epg.description.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = epg.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
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

        // Stream Failed Error-State Screen
        if (telemetry.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgCharcoal.copy(alpha = 0.94f))
                    .padding(24.dp)
                    .testTag("player_stream_failed_screen"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = ErrorRose, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(strings.streamFailedTitle, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = telemetry.errorMessage ?: "Unable to decode or connect to stream.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { controller.prepareAndPlay(streamUrl, startPositionMs, settings.videoQuality) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = BgCharcoal)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.retry)
                        }
                        if (target is PlaybackTarget.Live && !target.isCatchUp) {
                            OutlinedButton(onClick = { onRetryLiveWithTs(target.channel) }) {
                                Text("Try .TS Fallback", color = TextPrimary)
                            }
                        }
                        OutlinedButton(onClick = onBack) {
                            Text("Back", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }

    // 4K High-Speed Internet Warning Popup
    pending4kAction?.let { proceedAction ->
        AlertDialog(
            onDismissRequest = {
                pending4kAction = null
                if (!hasConfirmed4kInitial && settings.videoQuality == VideoQualityPreset.UHD_4K) {
                    onBack()
                }
            },
            containerColor = SurfaceCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = HdrGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.fourKWarningTitle, color = TextPrimary)
                }
            },
            text = {
                Text(strings.fourKWarningMessage, color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        pending4kAction = null
                        proceedAction()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = BgCharcoal),
                    modifier = Modifier.testTag("confirm_4k_warning_button")
                ) {
                    Text(strings.proceed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pending4kAction = null
                        if (!hasConfirmed4kInitial && settings.videoQuality == VideoQualityPreset.UHD_4K) {
                            onSelectQuality(VideoQualityPreset.FHD_1080)
                            hasConfirmed4kInitial = true
                            controller.prepareAndPlay(streamUrl, startPositionMs, VideoQualityPreset.FHD_1080)
                        }
                    }
                ) {
                    Text("Use 1080p", color = TextSecondary)
                }
            }
        )
    }

    // Resolution Selector Dialog
    if (showResolutionDialog) {
        AlertDialog(
            onDismissRequest = { showResolutionDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Select Video Resolution", color = TextPrimary) },
            text = {
                Column {
                    VideoQualityPreset.entries.forEach { preset ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showResolutionDialog = false
                                    if (preset == VideoQualityPreset.UHD_4K) {
                                        pending4kAction = {
                                            onSelectQuality(preset)
                                            controller.applyQualityPreset(preset)
                                        }
                                    } else {
                                        onSelectQuality(preset)
                                        controller.applyQualityPreset(preset)
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = telemetry.selectedQuality == preset,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(preset.label, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResolutionDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    // Audio Track Selector Dialog (shows AC3 / E-AC3 passthrough status)
    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Audio Track", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (telemetry.isAc3PassthroughSupported) {
                            "AC3 / E-AC3 Hardware Passthrough: Supported"
                        } else {
                            "AC3 / E-AC3 Passthrough unavailable — PCM software audio fallback active"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                    if (telemetry.audioTracks.isEmpty()) {
                        Text("Default stream audio active", color = TextSecondary)
                    } else {
                        telemetry.audioTracks.forEach { track ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        controller.selectAudioTrack(track)
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

    // Subtitle Track Selector Dialog
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
                                controller.selectSubtitleTrack(null)
                                showSubtitleDialog = false
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = telemetry.subtitleTracks.none { it.isSelected },
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Off", color = TextPrimary)
                    }
                    telemetry.subtitleTracks.forEach { track ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    controller.selectSubtitleTrack(track)
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
private fun PlayerToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCard.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, SurfaceBorderSubtle),
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = ElectricCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
        }
    }
}

private fun formatClockMs(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSeconds / 3600L
    val m = (totalSeconds % 3600L) / 60L
    val s = totalSeconds % 60L
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
