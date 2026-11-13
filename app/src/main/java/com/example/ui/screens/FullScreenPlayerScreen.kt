package com.example.ui.screens

import android.graphics.Color as AndroidColor
import android.util.TypedValue
import android.view.KeyEvent as AndroidKeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.components.Player4kWarningDialog
import com.example.ui.components.PlayerAudioTrackDialog
import com.example.ui.components.PlayerEpgSlidePanel
import com.example.ui.components.PlayerFocusIconButton
import com.example.ui.components.PlayerNextEpisodeChip
import com.example.ui.components.PlayerResolutionDialog
import com.example.ui.components.PlayerSeekBar
import com.example.ui.components.PlayerStreamErrorOverlay
import com.example.ui.components.PlayerSubtitleTrackDialog
import com.example.ui.components.PlayerToolButton
import com.example.ui.components.PlayerVolumeBar
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HdrGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.PlaybackTarget
import kotlinx.coroutines.delay

private const val PLAYER_AUTO_HIDE_DELAY_MS = 5_000L

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
    var interactionTick by remember { mutableIntStateOf(0) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var showEpgPanel by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var pending4kAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var hasConfirmed4kInitial by remember(target) { mutableStateOf(false) }

    val idleFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val unlockFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }

    val hasOpenOverlay = showEpgPanel ||
        showAudioDialog ||
        showSubtitleDialog ||
        showResolutionDialog ||
        pending4kAction != null ||
        telemetry.errorMessage != null

    fun registerInteraction(showIfHidden: Boolean = true) {
        interactionTick++
        if (showIfHidden && !controlsVisible) {
            controlsVisible = true
        }
    }

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

    // Auto-hide player controls after 5 seconds of inactivity (paused while dialogs/EPG are open)
    LaunchedEffect(controlsVisible, interactionTick, hasOpenOverlay) {
        if (controlsVisible && !hasOpenOverlay) {
            delay(PLAYER_AUTO_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }

    // Keep D-pad key receiver active when controls are hidden, and restore focus when overlays close
    var wasOverlayOpen by remember { mutableStateOf(false) }
    LaunchedEffect(controlsVisible, hasOpenOverlay, isScreenLocked) {
        if (!controlsVisible && !hasOpenOverlay) {
            withFrameNanos { }
            try {
                idleFocusRequester.requestFocus()
            } catch (_: Throwable) {
            }
        } else if (wasOverlayOpen && !hasOpenOverlay && controlsVisible) {
            withFrameNanos { }
            try {
                if (isScreenLocked) {
                    unlockFocusRequester.requestFocus()
                } else {
                    playPauseFocusRequester.requestFocus()
                }
            } catch (_: Throwable) {
            }
        }
        wasOverlayOpen = hasOpenOverlay
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
            val pos = controller.state.value.positionMs
            val dur = controller.state.value.durationMs
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
            .onPreviewKeyEvent { keyEvent ->
                if (hasOpenOverlay) return@onPreviewKeyEvent false
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                when (keyEvent.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    AndroidKeyEvent.KEYCODE_SPACE -> {
                        registerInteraction(showIfHidden = true)
                        if (!isScreenLocked) controller.togglePlayPause()
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> {
                        registerInteraction(showIfHidden = true)
                        if (!isScreenLocked && !telemetry.isPlaying) controller.togglePlayPause()
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        registerInteraction(showIfHidden = true)
                        if (!isScreenLocked && telemetry.isPlaying) controller.togglePlayPause()
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        registerInteraction(showIfHidden = true)
                        if (!isScreenLocked && (target !is PlaybackTarget.Live || target.isCatchUp)) {
                            controller.seekBy(10_000L)
                        }
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> {
                        registerInteraction(showIfHidden = true)
                        if (!isScreenLocked && (target !is PlaybackTarget.Live || target.isCatchUp)) {
                            controller.seekBy(-10_000L)
                        }
                        true
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_UP,
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN,
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
                    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                    AndroidKeyEvent.KEYCODE_ENTER,
                    AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
                    AndroidKeyEvent.KEYCODE_MENU -> {
                        if (!controlsVisible) {
                            registerInteraction(showIfHidden = true)
                            true
                        } else {
                            registerInteraction(showIfHidden = false)
                            false
                        }
                    }
                    else -> false
                }
            }
            .testTag("fullscreen_player_screen")
    ) {
        // Media3 PlayerView Surface + Customized Subtitles
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    player = controller.exoPlayer
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                if (view.player !== controller.exoPlayer) {
                    view.player = controller.exoPlayer
                }
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
            onRelease = { view ->
                view.player = null
            },
            modifier = Modifier.fillMaxSize()
        )

        // Touch tap toggle & idle Firestick D-pad focus catcher (never steals focus when controls are visible)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(idleFocusRequester)
                .focusProperties { canFocus = !controlsVisible && !hasOpenOverlay }
                .focusable()
                .pointerInput(hasOpenOverlay) {
                    detectTapGestures {
                        if (!hasOpenOverlay) {
                            if (controlsVisible) {
                                controlsVisible = false
                            } else {
                                registerInteraction(showIfHidden = true)
                            }
                        }
                    }
                }
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
                            } else {
                                "Buffering..."
                            },
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

        // Screen Lock Overlay (auto-focuses Unlock button for Firestick remote)
        AnimatedVisibility(
            visible = isScreenLocked && controlsVisible && telemetry.errorMessage == null,
            enter = fadeIn(animationSpec = tween(160)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            LaunchedEffect(Unit) {
                withFrameNanos { }
                try {
                    unlockFocusRequester.requestFocus()
                } catch (_: Throwable) {
                }
            }
            PlayerFocusIconButton(
                icon = Icons.Rounded.Lock,
                contentDescription = "Unlock screen",
                onClick = {
                    isScreenLocked = false
                    registerInteraction()
                },
                onFocusReceived = { registerInteraction(showIfHidden = false) },
                size = 56.dp,
                iconSize = 26.dp,
                modifier = Modifier
                    .padding(20.dp)
                    .focusRequester(unlockFocusRequester)
                    .testTag("player_unlock_button")
            )
        }

        // Player Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible && !isScreenLocked && telemetry.errorMessage == null,
            enter = fadeIn(animationSpec = tween(160)),
            exit = fadeOut(animationSpec = tween(220))
        ) {
            LaunchedEffect(Unit) {
                if (!hasOpenOverlay) {
                    withFrameNanos { }
                    try {
                        playPauseFocusRequester.requestFocus()
                    } catch (_: Throwable) {
                    }
                }
            }

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
                    PlayerFocusIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = {
                            registerInteraction()
                            onBack()
                        },
                        onFocusReceived = { registerInteraction(showIfHidden = false) },
                        size = 48.dp,
                        iconSize = 24.dp,
                        modifier = Modifier
                            .focusRequester(backFocusRequester)
                            .focusProperties { down = playPauseFocusRequester }
                            .testTag("player_back_button")
                    )
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
                        PlayerFocusIconButton(
                            icon = Icons.Rounded.Replay10,
                            contentDescription = "Rewind 10s",
                            onClick = {
                                registerInteraction()
                                controller.seekBy(-10_000L)
                            },
                            onFocusReceived = { registerInteraction(showIfHidden = false) },
                            size = 56.dp,
                            iconSize = 30.dp,
                            modifier = Modifier.focusProperties { up = backFocusRequester }
                        )
                    }

                    PlayerFocusIconButton(
                        icon = if (telemetry.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (telemetry.isPlaying) "Pause" else "Play",
                        onClick = {
                            registerInteraction()
                            controller.togglePlayPause()
                        },
                        onFocusReceived = { registerInteraction(showIfHidden = false) },
                        size = 72.dp,
                        iconSize = 38.dp,
                        isPrimary = true,
                        modifier = Modifier
                            .focusRequester(playPauseFocusRequester)
                            .focusProperties { up = backFocusRequester }
                            .testTag("player_play_pause_button")
                    )

                    if (target !is PlaybackTarget.Live || target.isCatchUp) {
                        PlayerFocusIconButton(
                            icon = Icons.Rounded.Forward10,
                            contentDescription = "Forward 10s",
                            onClick = {
                                registerInteraction()
                                controller.seekBy(10_000L)
                            },
                            onFocusReceived = { registerInteraction(showIfHidden = false) },
                            size = 56.dp,
                            iconSize = 30.dp,
                            modifier = Modifier.focusProperties { up = backFocusRequester }
                        )
                    }
                }

                // Bottom Control Dock
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (target !is PlaybackTarget.Live || target.isCatchUp) {
                        PlayerSeekBar(
                            positionMs = telemetry.positionMs,
                            durationMs = telemetry.durationMs,
                            onSeekTo = { controller.seekTo(it) },
                            onSeekBy = { controller.seekBy(it) },
                            onInteraction = { registerInteraction(showIfHidden = false) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PlayerVolumeBar(
                            volume = telemetry.volume,
                            onVolumeChange = { controller.setVolume(it) },
                            onInteraction = { registerInteraction(showIfHidden = false) }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            if (target is PlaybackTarget.SeriesEpisode) {
                                PlayerNextEpisodeChip(
                                    checked = settings.autoNextEpisode,
                                    onToggle = {
                                        registerInteraction()
                                        onToggleAutoNextEpisode(!settings.autoNextEpisode)
                                    },
                                    onFocusReceived = { registerInteraction(showIfHidden = false) },
                                    modifier = Modifier.testTag("player_auto_next_toggle")
                                )
                            }

                            if (target is PlaybackTarget.Live) {
                                PlayerToolButton(
                                    icon = Icons.Rounded.Schedule,
                                    label = "EPG",
                                    isActive = showEpgPanel,
                                    onClick = {
                                        registerInteraction()
                                        showEpgPanel = !showEpgPanel
                                    },
                                    onFocusReceived = { registerInteraction(showIfHidden = false) },
                                    modifier = Modifier.testTag("player_epg_button")
                                )
                            }

                            PlayerToolButton(
                                icon = Icons.Rounded.HighQuality,
                                label = telemetry.selectedQuality.label,
                                onClick = {
                                    registerInteraction()
                                    showResolutionDialog = true
                                },
                                onFocusReceived = { registerInteraction(showIfHidden = false) },
                                modifier = Modifier.testTag("player_resolution_button")
                            )

                            PlayerToolButton(
                                icon = Icons.Rounded.Audiotrack,
                                label = "Audio",
                                onClick = {
                                    registerInteraction()
                                    showAudioDialog = true
                                },
                                onFocusReceived = { registerInteraction(showIfHidden = false) },
                                modifier = Modifier.testTag("player_audio_button")
                            )

                            PlayerToolButton(
                                icon = Icons.Rounded.ClosedCaption,
                                label = "Sub",
                                onClick = {
                                    registerInteraction()
                                    showSubtitleDialog = true
                                },
                                onFocusReceived = { registerInteraction(showIfHidden = false) },
                                modifier = Modifier.testTag("player_subtitle_button")
                            )

                            PlayerToolButton(
                                icon = Icons.Rounded.Speed,
                                label = "HUD",
                                isActive = settings.showBitrateOverlay,
                                onClick = {
                                    registerInteraction()
                                    onToggleBitrateHud(!settings.showBitrateOverlay)
                                },
                                onFocusReceived = { registerInteraction(showIfHidden = false) },
                                modifier = Modifier.testTag("player_bitrate_toggle")
                            )

                            PlayerToolButton(
                                icon = Icons.Rounded.LockOpen,
                                label = "Lock",
                                onClick = {
                                    isScreenLocked = true
                                    registerInteraction()
                                },
                                onFocusReceived = { registerInteraction(showIfHidden = false) },
                                modifier = Modifier.testTag("player_lock_button")
                            )
                        }
                    }
                }
            }
        }

        // Slide-In EPG Timeline & Catch-Up Panel (Live TV)
        PlayerEpgSlidePanel(
            visible = showEpgPanel && target is PlaybackTarget.Live,
            channel = (target as? PlaybackTarget.Live)?.channel,
            epgList = epgList,
            onPlayCatchUp = onPlayCatchUp,
            onClose = {
                showEpgPanel = false
                registerInteraction()
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // Stream Failed Error-State Screen
        if (telemetry.errorMessage != null) {
            PlayerStreamErrorOverlay(
                title = strings.streamFailedTitle,
                errorMessage = telemetry.errorMessage ?: "Unable to decode or connect to stream.",
                retryLabel = strings.retry,
                showTsFallback = target is PlaybackTarget.Live && !target.isCatchUp,
                onRetry = {
                    controller.prepareAndPlay(streamUrl, startPositionMs, settings.videoQuality)
                },
                onRetryTs = {
                    if (target is PlaybackTarget.Live) {
                        onRetryLiveWithTs(target.channel)
                    }
                },
                onBack = onBack
            )
        }
    }

    // 4K High-Speed Internet Warning Popup
    pending4kAction?.let { proceedAction ->
        Player4kWarningDialog(
            title = strings.fourKWarningTitle,
            message = strings.fourKWarningMessage,
            proceedLabel = strings.proceed,
            onDismiss = {
                pending4kAction = null
                if (!hasConfirmed4kInitial && settings.videoQuality == VideoQualityPreset.UHD_4K) {
                    onBack()
                }
            },
            onConfirm = {
                pending4kAction = null
                registerInteraction()
                proceedAction()
            },
            onUse1080p = {
                pending4kAction = null
                registerInteraction()
                if (!hasConfirmed4kInitial && settings.videoQuality == VideoQualityPreset.UHD_4K) {
                    onSelectQuality(VideoQualityPreset.FHD_1080)
                    hasConfirmed4kInitial = true
                    controller.prepareAndPlay(streamUrl, startPositionMs, VideoQualityPreset.FHD_1080)
                }
            }
        )
    }

    // Resolution Selector Dialog
    if (showResolutionDialog) {
        PlayerResolutionDialog(
            selectedQuality = telemetry.selectedQuality,
            cancelLabel = strings.cancel,
            onSelect = { preset ->
                showResolutionDialog = false
                registerInteraction()
                if (preset == VideoQualityPreset.UHD_4K) {
                    pending4kAction = {
                        onSelectQuality(preset)
                        controller.applyQualityPreset(preset)
                    }
                } else {
                    onSelectQuality(preset)
                    controller.applyQualityPreset(preset)
                }
            },
            onDismiss = {
                showResolutionDialog = false
                registerInteraction()
            }
        )
    }

    // Audio Track Selector Dialog
    if (showAudioDialog) {
        PlayerAudioTrackDialog(
            isAc3PassthroughSupported = telemetry.isAc3PassthroughSupported,
            audioTracks = telemetry.audioTracks,
            cancelLabel = strings.cancel,
            onSelectTrack = { track ->
                controller.selectAudioTrack(track)
                showAudioDialog = false
                registerInteraction()
            },
            onDismiss = {
                showAudioDialog = false
                registerInteraction()
            }
        )
    }

    // Subtitle Track Selector Dialog
    if (showSubtitleDialog) {
        PlayerSubtitleTrackDialog(
            subtitleTracks = telemetry.subtitleTracks,
            cancelLabel = strings.cancel,
            onSelectTrack = { track ->
                controller.selectSubtitleTrack(track)
                showSubtitleDialog = false
                registerInteraction()
            },
            onDismiss = {
                showSubtitleDialog = false
                registerInteraction()
            }
        )
    }
}
