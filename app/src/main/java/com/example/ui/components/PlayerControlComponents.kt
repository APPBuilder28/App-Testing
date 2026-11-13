package com.example.ui.components

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PlayerFocusIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    onFocusReceived: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    iconSize: Dp = 26.dp,
    isPrimary: Boolean = false
) {
    val hoverState = rememberHoverFocusState()
    val highlighted = hoverState.isHighlighted
    val scale by animateFloatAsState(
        targetValue = if (highlighted) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f),
        label = "icon_btn_focus_scale"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            isPrimary && highlighted -> Color.White
            isPrimary -> ElectricCyan
            highlighted -> ElectricCyan.copy(alpha = 0.30f)
            else -> SurfaceCard.copy(alpha = 0.8f)
        },
        label = "icon_btn_bg"
    )
    val tintColor = when {
        isPrimary -> BgCharcoal
        highlighted -> ElectricCyan
        else -> TextPrimary
    }
    val borderStroke = when {
        highlighted -> null
        isPrimary -> BorderStroke(1.5.dp, Color.White.copy(alpha = 0.55f))
        else -> BorderStroke(1.dp, SurfaceBorderSubtle.copy(alpha = 0.7f))
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        border = borderStroke,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .trackHoverFocus(hoverState) { focused ->
                if (focused) onFocusReceived()
            }
            .thickBlackHoverOutline(
                highlighted = highlighted,
                shape = CircleShape,
                contrastHaloColor = ElectricCyan.copy(alpha = 0.92f)
            )
            .minimumInteractiveComponentSize()
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tintColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun PlayerToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onFocusReceived: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val hoverState = rememberHoverFocusState()
    val highlighted = hoverState.isHighlighted
    val btnShape = RoundedCornerShape(10.dp)
    val scale by animateFloatAsState(
        targetValue = if (highlighted) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 460f),
        label = "tool_btn_focus_scale"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            highlighted -> ElectricCyan.copy(alpha = 0.28f)
            isActive -> ElectricCyan.copy(alpha = 0.16f)
            else -> SurfaceCard.copy(alpha = 0.82f)
        },
        label = "tool_btn_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isActive -> ElectricCyan.copy(alpha = 0.65f)
            else -> SurfaceBorderSubtle
        },
        label = "tool_btn_border"
    )

    Surface(
        onClick = onClick,
        shape = btnShape,
        color = containerColor,
        border = if (highlighted) null else BorderStroke(1.dp, borderColor),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .trackHoverFocus(hoverState) { focused ->
                if (focused) onFocusReceived()
            }
            .thickBlackHoverOutline(
                highlighted = highlighted,
                shape = btnShape,
                contrastHaloColor = ElectricCyan.copy(alpha = 0.9f)
            )
            .minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (highlighted) Color.White else ElectricCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (highlighted) Color.White else TextPrimary
            )
        }
    }
}

@Composable
fun PlayerNextEpisodeChip(
    checked: Boolean,
    onToggle: () -> Unit,
    onFocusReceived: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hoverState = rememberHoverFocusState()
    val highlighted = hoverState.isHighlighted
    val chipShape = RoundedCornerShape(10.dp)
    val scale by animateFloatAsState(
        targetValue = if (highlighted) 1.07f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 460f),
        label = "next_ep_focus_scale"
    )

    Surface(
        onClick = onToggle,
        shape = chipShape,
        color = if (highlighted) ElectricCyan.copy(alpha = 0.28f) else SurfaceCard.copy(alpha = 0.82f),
        border = if (highlighted) null else BorderStroke(1.dp, SurfaceBorderSubtle),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .trackHoverFocus(hoverState) { focused ->
                if (focused) onFocusReceived()
            }
            .thickBlackHoverOutline(
                highlighted = highlighted,
                shape = chipShape,
                contrastHaloColor = ElectricCyan.copy(alpha = 0.9f)
            )
            .minimumInteractiveComponentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Next Ep",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BgCharcoal,
                    checkedTrackColor = ElectricCyan
                ),
                modifier = Modifier.focusProperties { canFocus = false }
            )
        }
    }
}

@Composable
fun PlayerSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dur = durationMs.coerceAtLeast(1L)
    val pos = positionMs.coerceIn(0L, dur)
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) SurfaceCard.copy(alpha = 0.65f) else Color.Transparent,
        border = if (isFocused) BorderStroke(2.dp, ElectricCyan) else null,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onInteraction()
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                            onInteraction()
                            onSeekBy(-10_000L)
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                            onInteraction()
                            onSeekBy(10_000L)
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .clickable(onClick = onInteraction)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = formatPlayerClockMs(pos),
                style = MaterialTheme.typography.labelMedium,
                color = if (isFocused) ElectricCyan else TextPrimary
            )
            Slider(
                value = pos.toFloat(),
                onValueChange = {
                    onInteraction()
                    onSeekTo(it.toLong())
                },
                valueRange = 0f..dur.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = ElectricCyan,
                    activeTrackColor = ElectricCyan,
                    inactiveTrackColor = SurfaceCardVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .focusProperties { canFocus = false }
                    .testTag("player_seek_bar")
            )
            Text(
                text = formatPlayerClockMs(dur),
                style = MaterialTheme.typography.labelMedium,
                color = if (isFocused) ElectricCyan else TextSecondary
            )
        }
    }
}

@Composable
fun PlayerVolumeBar(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var isAdjusting by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) {
        if (!isFocused) {
            isAdjusting = false
        }
    }

    Surface(
        onClick = {
            onInteraction()
            isAdjusting = !isAdjusting
        },
        shape = RoundedCornerShape(10.dp),
        color = when {
            isAdjusting -> ElectricCyan.copy(alpha = 0.24f)
            isFocused -> SurfaceCard.copy(alpha = 0.88f)
            else -> Color.Transparent
        },
        border = when {
            isAdjusting -> BorderStroke(2.dp, Color.White)
            isFocused -> BorderStroke(2.dp, ElectricCyan)
            else -> null
        },
        modifier = modifier
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onInteraction()
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && isAdjusting) {
                    when (event.nativeKeyEvent.keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                            onInteraction()
                            onVolumeChange((volume - 0.1f).coerceIn(0f, 1f))
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                            onInteraction()
                            onVolumeChange((volume + 0.1f).coerceIn(0f, 1f))
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_UP,
                        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                            onInteraction()
                            isAdjusting = false
                            false
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .minimumInteractiveComponentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .width(if (isAdjusting) 185.dp else 156.dp)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = if (volume <= 0.01f) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                contentDescription = "Volume",
                tint = if (isFocused || isAdjusting) Color.White else ElectricCyan,
                modifier = Modifier.size(20.dp)
            )
            Slider(
                value = volume,
                onValueChange = {
                    onInteraction()
                    onVolumeChange(it)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = ElectricCyan,
                    activeTrackColor = ElectricCyan,
                    inactiveTrackColor = SurfaceCardVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusProperties { canFocus = false }
                    .testTag("player_volume_slider")
            )
            if (isAdjusting) {
                Text(
                    text = "${(volume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )
            }
        }
    }
}

@Composable
fun PlayerStreamErrorOverlay(
    title: String,
    errorMessage: String,
    retryLabel: String,
    showTsFallback: Boolean,
    onRetry: () -> Unit,
    onRetryTs: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val retryFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        try {
            retryFocusRequester.requestFocus()
        } catch (_: Throwable) {
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgCharcoal.copy(alpha = 0.94f))
            .padding(24.dp)
            .testTag("player_stream_failed_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = ErrorRose,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NextVuButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = BgCharcoal
                    ),
                    modifier = Modifier
                        .focusRequester(retryFocusRequester)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(retryLabel, fontWeight = FontWeight.SemiBold)
                }

                if (showTsFallback) {
                    NextVuOutlinedButton(
                        onClick = onRetryTs,
                        border = BorderStroke(1.dp, SurfaceBorderSubtle),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("Try .TS Fallback", color = TextPrimary)
                    }
                }

                NextVuOutlinedButton(
                    onClick = onBack,
                    border = BorderStroke(1.dp, SurfaceBorderSubtle),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Back", color = TextSecondary)
                }
            }
        }
    }
}

fun formatPlayerClockMs(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSeconds / 3600L
    val m = (totalSeconds % 3600L) / 60L
    val s = totalSeconds % 60L
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
