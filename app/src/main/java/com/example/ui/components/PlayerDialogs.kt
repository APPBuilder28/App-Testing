package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.VideoQualityPreset
import com.example.data.remote.XtreamEpgItem
import com.example.data.remote.XtreamLiveChannel
import com.example.player.MediaTrackInfo
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.BgCharcoalElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HdrGold
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PlayerEpgSlidePanel(
    visible: Boolean,
    channel: XtreamLiveChannel?,
    epgList: List<XtreamEpgItem>,
    onPlayCatchUp: (XtreamLiveChannel, XtreamEpgItem) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val closeFocusRequester = remember { FocusRequester() }

    AnimatedVisibility(
        visible = visible && channel != null,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier
    ) {
        val liveChannel = channel ?: return@AnimatedVisibility
        LaunchedEffect(Unit) {
            withFrameNanos { }
            try {
                closeFocusRequester.requestFocus()
            } catch (_: Throwable) {
            }
        }

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
                    val closeHover = rememberHoverFocusState()
                    val closeShape = RoundedCornerShape(8.dp)
                    Surface(
                        onClick = onClose,
                        shape = closeShape,
                        color = if (closeHover.isHighlighted) ElectricCyan.copy(alpha = 0.25f) else Color.Transparent,
                        border = if (closeHover.isHighlighted) null else BorderStroke(1.dp, SurfaceBorderSubtle),
                        modifier = Modifier
                            .focusRequester(closeFocusRequester)
                            .trackHoverFocus(closeHover)
                            .thickBlackHoverOutline(
                                highlighted = closeHover.isHighlighted,
                                shape = closeShape,
                                contrastHaloColor = ElectricCyan.copy(alpha = 0.9f)
                            )
                            .minimumInteractiveComponentSize()
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (closeHover.isHighlighted) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (epgList.isEmpty()) {
                    Text(
                        text = "No EPG entries provided by this Xtream server for ${liveChannel.name}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(
                            epgList,
                            key = { idx, epg -> "epg_${epg.id}_$idx" }
                        ) { _, epg ->
                            val canReplay = liveChannel.tvArchive && epg.hasArchive && !epg.isNowPlaying
                            NextVuInteractiveCard(
                                isActive = epg.isNowPlaying,
                                onClick = {
                                    if (canReplay) {
                                        onClose()
                                        onPlayCatchUp(liveChannel, epg)
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
                                                Icon(
                                                    Icons.Rounded.History,
                                                    contentDescription = null,
                                                    tint = ElectricCyan,
                                                    modifier = Modifier.size(13.dp)
                                                )
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
}

@Composable
fun Player4kWarningDialog(
    title: String,
    message: String,
    proceedLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onUse1080p: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = HdrGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = TextPrimary)
            }
        },
        text = { Text(message, color = TextSecondary) },
        confirmButton = {
            NextVuButton(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = BgCharcoal),
                modifier = Modifier.testTag("confirm_4k_warning_button")
            ) {
                Text(proceedLabel)
            }
        },
        dismissButton = {
            NextVuTextButton(onClick = onUse1080p) {
                Text("Use 1080p", color = TextSecondary)
            }
        }
    )
}

@Composable
fun PlayerResolutionDialog(
    selectedQuality: VideoQualityPreset,
    cancelLabel: String,
    onSelect: (VideoQualityPreset) -> Unit,
    onDismiss: () -> Unit
) {
    val firstFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        try {
            firstFocusRequester.requestFocus()
        } catch (_: Throwable) {
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Select Video Resolution", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                VideoQualityPreset.entries.forEach { preset ->
                    val isSelected = selectedQuality == preset
                    FocusAwareRadioOptionRow(
                        label = preset.label,
                        selected = isSelected,
                        enabled = true,
                        onClick = { onSelect(preset) },
                        modifier = if (isSelected) Modifier.focusRequester(firstFocusRequester) else Modifier
                    )
                }
            }
        },
        confirmButton = {
            NextVuTextButton(onClick = onDismiss) {
                Text(cancelLabel, color = TextSecondary)
            }
        }
    )
}

@Composable
fun PlayerAudioTrackDialog(
    isAc3PassthroughSupported: Boolean,
    audioTracks: List<MediaTrackInfo>,
    cancelLabel: String,
    onSelectTrack: (MediaTrackInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedFocusRequester = remember { FocusRequester() }
    val hasSelectedOrSupported = audioTracks.any { it.isSelected } || audioTracks.any { it.isSupported }
    LaunchedEffect(Unit) {
        if (hasSelectedOrSupported) {
            withFrameNanos { }
            try {
                selectedFocusRequester.requestFocus()
            } catch (_: Throwable) {
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Audio Track", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isAc3PassthroughSupported) {
                        "AC3 / E-AC3 Hardware Passthrough: Supported"
                    } else {
                        "AC3 / E-AC3 Passthrough unavailable — PCM software audio fallback active"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
                if (audioTracks.isEmpty()) {
                    Text("Default stream audio active", color = TextSecondary)
                } else {
                    val focusTargetId = audioTracks.firstOrNull { it.isSelected }?.id
                        ?: audioTracks.firstOrNull { it.isSupported }?.id
                    audioTracks.forEach { track ->
                        FocusAwareRadioOptionRow(
                            label = track.label,
                            selected = track.isSelected,
                            enabled = track.isSupported,
                            onClick = { onSelectTrack(track) },
                            modifier = if (track.id == focusTargetId) Modifier.focusRequester(selectedFocusRequester) else Modifier
                        )
                    }
                }
            }
        },
        confirmButton = {
            NextVuTextButton(onClick = onDismiss) {
                Text(cancelLabel, color = TextSecondary)
            }
        }
    )
}

@Composable
fun PlayerSubtitleTrackDialog(
    subtitleTracks: List<MediaTrackInfo>,
    cancelLabel: String,
    onSelectTrack: (MediaTrackInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedFocusRequester = remember { FocusRequester() }
    val noneSelected = subtitleTracks.none { it.isSelected }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        try {
            selectedFocusRequester.requestFocus()
        } catch (_: Throwable) {
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Subtitles", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FocusAwareRadioOptionRow(
                    label = "Off",
                    selected = noneSelected,
                    enabled = true,
                    onClick = { onSelectTrack(null) },
                    modifier = if (noneSelected) Modifier.focusRequester(selectedFocusRequester) else Modifier
                )
                subtitleTracks.forEach { track ->
                    FocusAwareRadioOptionRow(
                        label = track.label,
                        selected = track.isSelected,
                        enabled = true,
                        onClick = { onSelectTrack(track) },
                        modifier = if (track.isSelected) Modifier.focusRequester(selectedFocusRequester) else Modifier
                    )
                }
            }
        },
        confirmButton = {
            NextVuTextButton(onClick = onDismiss) {
                Text(cancelLabel, color = TextSecondary)
            }
        }
    )
}

@Composable
private fun FocusAwareRadioOptionRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = when {
            isFocused -> ElectricCyan.copy(alpha = 0.22f)
            selected -> SurfaceCardVariant
            else -> Color.Transparent
        },
        border = when {
            isFocused -> BorderStroke(2.dp, ElectricCyan)
            selected -> BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
            else -> BorderStroke(1.dp, SurfaceBorderSubtle.copy(alpha = 0.35f))
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .minimumInteractiveComponentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = if (isFocused || selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (enabled) TextPrimary else TextMuted
            )
        }
    }
}
