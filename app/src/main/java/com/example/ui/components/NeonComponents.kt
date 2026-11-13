package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsInputAntenna
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.core.security.XtreamSourceProfile
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.HdrGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

/**
 * Interactive card container optimized for both Mobile touch (tap + long-press)
 * and Android TV / Fire Stick D-Pad remote focus with subtle neon border glow ONLY when focused or active.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NextVuInteractiveCard(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    shape: Shape = RoundedCornerShape(14.dp),
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable (isFocused: Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.035f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 450f),
        label = "focus_scale"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> ElectricCyan
            isActive -> ElectricCyan.copy(alpha = 0.72f)
            else -> SurfaceBorderSubtle.copy(alpha = 0.7f)
        },
        label = "border_color"
    )
    val borderWidth = if (isFocused || isActive) 1.6.dp else 1.dp

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                if (isFocused || isActive) {
                    val glowAlpha = if (isFocused) 0.18f else 0.08f
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                ElectricCyan.copy(alpha = glowAlpha),
                                DeepViolet.copy(alpha = glowAlpha)
                            )
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
                    )
                }
            }
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = SurfaceCard,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = if (isActive || isFocused) 4.dp else 1.dp
    ) {
        content(isFocused)
    }
}

@Composable
fun NextVuTopBar(
    title: String,
    activeSource: XtreamSourceProfile?,
    isOfflineMode: Boolean,
    onActiveSourceClick: () -> Unit,
    onSearchClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgCharcoal, BgCharcoal.copy(alpha = 0.94f))
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
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
                Image(
                    painter = painterResource(id = R.drawable.nextvu_icon_fg_1794523727740),
                    contentDescription = strings.appName,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(9.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isOfflineMode) {
                    Surface(
                        color = WarningAmber.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
                        modifier = Modifier.testTag("offline_mode_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = strings.offlineMode,
                                tint = WarningAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.offlineMode,
                                style = MaterialTheme.typography.labelSmall,
                                color = WarningAmber,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Tappable Active-Source Quick-Switch Trigger
                Surface(
                    onClick = onActiveSourceClick,
                    shape = RoundedCornerShape(999.dp),
                    color = SurfaceCardVariant,
                    border = BorderStroke(
                        1.dp,
                        if (activeSource != null) ElectricCyan.copy(alpha = 0.45f) else SurfaceBorderSubtle
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("active_source_switch_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (activeSource != null) SuccessGreen else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeSource?.sourceName ?: strings.noActiveSource,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = strings.quickSwitchTitle,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (onSearchClick != null) {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(SurfaceCardVariant)
                            .testTag("top_bar_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = strings.searchHint,
                            tint = ElectricCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickSourceSwitchDialog(
    sources: List<XtreamSourceProfile>,
    activeSource: XtreamSourceProfile?,
    onSelectSource: (XtreamSourceProfile) -> Unit,
    onManageSources: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var pendingConfirmSource by remember { mutableStateOf<XtreamSourceProfile?>(null) }

    if (pendingConfirmSource != null) {
        val target = pendingConfirmSource!!
        AlertDialog(
            onDismissRequest = { pendingConfirmSource = null },
            containerColor = SurfaceCard,
            title = {
                Text(strings.setActiveSourceTitle, color = TextPrimary)
            },
            text = {
                Text(
                    "${target.sourceName} (${target.truncatedUrl()})\n\n${strings.setActiveSourceConfirm}",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSelectSource(target)
                        pendingConfirmSource = null
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = BgCharcoal),
                    modifier = Modifier.testTag("confirm_quick_switch_button")
                ) {
                    Text(strings.switchSource)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmSource = null }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = ElectricCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.quickSwitchTitle, color = TextPrimary)
            }
        },
        text = {
            if (sources.isEmpty()) {
                Text(strings.noSourceSubtitle, color = TextSecondary)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sources, key = { it.id }) { source ->
                        val isCurrent = source.id == activeSource?.id
                        NextVuInteractiveCard(
                            isActive = isCurrent,
                            onClick = {
                                if (isCurrent) onDismiss() else pendingConfirmSource = source
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_switch_item_${source.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Dns,
                                        contentDescription = null,
                                        tint = if (isCurrent) ElectricCyan else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = source.sourceName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = source.truncatedUrl(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = strings.activeBadge,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onManageSources()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepViolet, contentColor = TextPrimary),
                modifier = Modifier.testTag("quick_switch_manage_button")
            ) {
                Text(strings.manageSources)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = TextSecondary)
            }
        }
    )
}

@Composable
fun PosterCard(
    title: String,
    imageUrl: String,
    subtitle: String? = null,
    rating: Double = 0.0,
    progressFraction: Float? = null,
    isWatchlisted: Boolean = false,
    aspectRatio: Float = 0.68f,
    cardWidth: Dp = 136.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NextVuInteractiveCard(
        modifier = modifier.width(cardWidth),
        onClick = onClick
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .background(SurfaceCardVariant)
            ) {
                if (imageUrl.isNotBlank()) {
                    val context = LocalContext.current
                    val posterRequest = remember(imageUrl) {
                        ImageRequest.Builder(context)
                            .data(imageUrl)
                            .size(360)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = posterRequest,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tv,
                            contentDescription = title,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Transparent, BgCharcoal.copy(alpha = 0.85f))
                            )
                        )
                )

                if (rating > 0.0) {
                    Surface(
                        color = BgCharcoal.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = HdrGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", rating),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }

                if (isWatchlisted) {
                    Icon(
                        imageVector = Icons.Rounded.Bookmark,
                        contentDescription = "Watchlisted",
                        tint = ElectricCyan,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }

                if (progressFraction != null && progressFraction > 0f) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp),
                        color = ElectricCyan,
                        trackColor = SurfaceCardVariant
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingStateView(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = ElectricCyan,
                trackColor = SurfaceCardVariant,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector = Icons.Rounded.SettingsInputAntenna,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SurfaceCardVariant)
                    .border(1.dp, DeepViolet.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ElectricCyan,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = BgCharcoal
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ErrorRose.copy(alpha = 0.14f))
                    .border(1.dp, ErrorRose.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = "Error",
                    tint = ErrorRose,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = BgCharcoal),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("error_retry_button")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.retry)
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        border = BorderStroke(1.dp, DeepVioletSoftOrMuted),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(secondaryActionLabel, color = TextPrimary)
                    }
                }
            }
        }
    }
}

private val DeepVioletSoftOrMuted = DeepViolet.copy(alpha = 0.7f)

@Composable
fun LegalDisclaimerBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCard.copy(alpha = 0.75f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SurfaceBorderSubtle.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/**
 * Search box that stays collapsed as a D-pad/hover-safe trigger until explicitly clicked/pressed,
 * preventing Fire Stick / Android TV remotes from popping open the text editor & keyboard on focus.
 */
@Composable
fun ClickToEditSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    fontSize: TextUnit = 14.sp,
    iconSize: Dp = 18.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    testTag: String = "search_input"
) {
    var isEditing by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var editorHadFocus by remember { mutableStateOf(false) }
    val editorFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    if (isEditing) {
        BackHandler {
            isEditing = false
            keyboardController?.hide()
        }

        LaunchedEffect(Unit) {
            editorHadFocus = false
            withFrameNanos { }
            try {
                editorFocusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Throwable) {
            }
        }

        Surface(
            shape = shape,
            color = SurfaceCard,
            border = BorderStroke(2.dp, ElectricCyan),
            modifier = modifier
                .height(height)
                .testTag(testTag)
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
                    modifier = Modifier.size(iconSize)
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = fontSize,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = fontSize
                        ),
                        cursorBrush = SolidColor(ElectricCyan),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                isEditing = false
                            },
                            onDone = {
                                keyboardController?.hide()
                                isEditing = false
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(editorFocusRequester)
                            .onFocusChanged { state ->
                                if (state.hasFocus) {
                                    editorHadFocus = true
                                } else if (editorHadFocus) {
                                    isEditing = false
                                    keyboardController?.hide()
                                }
                            }
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(iconSize)
                            .clickable { onQueryChange("") }
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Done searching",
                    tint = ElectricCyan,
                    modifier = Modifier
                        .size(iconSize)
                        .clickable {
                            keyboardController?.hide()
                            isEditing = false
                        }
                )
            }
        }
    } else {
        Surface(
            onClick = { isEditing = true },
            shape = shape,
            color = if (isFocused) SurfaceCardVariant else SurfaceCard,
            border = BorderStroke(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) ElectricCyan else SurfaceBorderSubtle
            ),
            modifier = modifier
                .height(height)
                .onFocusChanged { isFocused = it.isFocused }
                .testTag(testTag)
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
                    contentDescription = placeholder,
                    tint = ElectricCyan,
                    modifier = Modifier.size(iconSize)
                )
                Text(
                    text = query.ifEmpty { placeholder },
                    fontSize = fontSize,
                    color = if (query.isEmpty()) TextMuted else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(iconSize)
                            .clickable { onQueryChange("") }
                    )
                }
            }
        }
    }
}
