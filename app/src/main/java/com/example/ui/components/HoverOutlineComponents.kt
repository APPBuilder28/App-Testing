package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCardVariant

@Stable
class HoverFocusState(
    val hoverInteractionSource: MutableInteractionSource,
    val isHovered: Boolean,
    val isFocused: Boolean,
    val onFocusChanged: (Boolean) -> Unit
) {
    val isHighlighted: Boolean
        get() = isHovered || isFocused
}

@Composable
fun rememberHoverFocusState(): HoverFocusState {
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    var isFocused by remember { mutableStateOf(false) }
    return remember(hoverSource, isHovered, isFocused) {
        HoverFocusState(
            hoverInteractionSource = hoverSource,
            isHovered = isHovered,
            isFocused = isFocused,
            onFocusChanged = { isFocused = it }
        )
    }
}

fun Modifier.trackHoverFocus(
    state: HoverFocusState,
    onFocusCallback: ((Boolean) -> Unit)? = null
): Modifier = this
    .hoverable(state.hoverInteractionSource)
    .onFocusChanged { focusState ->
        val focused = focusState.isFocused
        state.onFocusChanged(focused)
        onFocusCallback?.invoke(focused)
    }

/**
 * Draws a thick black outline (with a thin bright contrast halo on dark surfaces so the black
 * stroke stays unmistakably sharp against both the dark screen background and dark button fills).
 */
fun Modifier.thickBlackHoverOutline(
    highlighted: Boolean,
    shape: Shape = RoundedCornerShape(12.dp),
    blackWidth: Dp = 3.5.dp,
    contrastHaloColor: Color = Color.White.copy(alpha = 0.88f),
    haloPadding: Dp = 1.5.dp
): Modifier = if (!highlighted) {
    this
} else {
    this.drawWithContent {
        drawContent()
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(
            outline = outline,
            color = contrastHaloColor,
            style = Stroke(width = (blackWidth + haloPadding * 2).toPx())
        )
        drawOutline(
            outline = outline,
            color = Color.Black,
            style = Stroke(width = blackWidth.toPx())
        )
    }
}

@Composable
fun NextVuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = ElectricCyan,
        contentColor = BgCharcoal
    ),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val hoverState = rememberHoverFocusState()
    val highlighted = enabled && hoverState.isHighlighted

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        modifier = modifier
            .trackHoverFocus(hoverState)
            .thickBlackHoverOutline(
                highlighted = highlighted,
                shape = shape,
                contrastHaloColor = ElectricCyan.copy(alpha = 0.9f)
            ),
        content = content
    )
}

@Composable
fun NextVuOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    hoverContainerColor: Color = SurfaceCardVariant,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val hoverState = rememberHoverFocusState()
    val highlighted = enabled && hoverState.isHighlighted
    val resolvedColors = if (highlighted) {
        ButtonDefaults.outlinedButtonColors(
            containerColor = hoverContainerColor,
            contentColor = ElectricCyan
        )
    } else {
        colors
    }

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = resolvedColors,
        border = border,
        contentPadding = contentPadding,
        modifier = modifier
            .trackHoverFocus(hoverState)
            .thickBlackHoverOutline(
                highlighted = highlighted,
                shape = shape,
                contrastHaloColor = ElectricCyan.copy(alpha = 0.9f)
            ),
        content = content
    )
}

@Composable
fun NextVuTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val hoverState = rememberHoverFocusState()
    val highlighted = enabled && hoverState.isHighlighted
    val resolvedColors = if (highlighted) {
        ButtonDefaults.textButtonColors(
            containerColor = SurfaceCardVariant,
            contentColor = ElectricCyan
        )
    } else {
        colors
    }

    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = resolvedColors,
        border = border,
        contentPadding = contentPadding,
        modifier = modifier
            .trackHoverFocus(hoverState)
            .thickBlackHoverOutline(
                highlighted = highlighted,
                shape = shape,
                contrastHaloColor = ElectricCyan.copy(alpha = 0.9f)
            ),
        content = content
    )
}

@Composable
fun NextVuIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    hoverContainerColor: Color = SurfaceCardVariant,
    contentColor: Color = LocalContentColor.current,
    visualSize: Dp = 40.dp,
    enforceMinTouchTarget: Boolean = true,
    content: @Composable () -> Unit
) {
    val hoverState = rememberHoverFocusState()
    val highlighted = enabled && hoverState.isHighlighted

    val touchModifier = if (enforceMinTouchTarget) {
        Modifier.minimumInteractiveComponentSize()
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(touchModifier)
            .trackHoverFocus(hoverState)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(visualSize)
                .thickBlackHoverOutline(
                    highlighted = highlighted,
                    shape = CircleShape,
                    blackWidth = 3.dp,
                    contrastHaloColor = ElectricCyan.copy(alpha = 0.92f),
                    haloPadding = 1.2.dp
                )
                .clip(CircleShape)
                .background(if (highlighted) hoverContainerColor else containerColor),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }
}

@Composable
fun NextVuFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
        selectedLabelColor = ElectricCyan
    )
) {
    val hoverState = rememberHoverFocusState()
    val highlighted = enabled && hoverState.isHighlighted

    val effectiveColors = if (highlighted) {
        FilterChipDefaults.filterChipColors(
            containerColor = ElectricCyan.copy(alpha = 0.92f),
            labelColor = BgCharcoal,
            iconColor = BgCharcoal,
            selectedContainerColor = Color.White,
            selectedLabelColor = BgCharcoal,
            selectedLeadingIconColor = BgCharcoal,
            selectedTrailingIconColor = BgCharcoal
        )
    } else {
        colors
    }

    val effectiveBorder = if (highlighted) {
        BorderStroke(3.dp, Color.Black)
    } else {
        FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = SurfaceBorderSubtle,
            selectedBorderColor = ElectricCyan.copy(alpha = 0.65f)
        )
    }

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = effectiveColors,
        border = effectiveBorder,
        modifier = modifier.trackHoverFocus(hoverState)
    )
}

@Composable
fun NextVuFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = ElectricCyan,
    contentColor: Color = BgCharcoal,
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    content: @Composable () -> Unit
) {
    val hoverState = rememberHoverFocusState()
    FloatingActionButton(
        onClick = onClick,
        shape = shape,
        containerColor = if (hoverState.isHighlighted) Color.White else containerColor,
        contentColor = contentColor,
        elevation = elevation,
        modifier = modifier
            .trackHoverFocus(hoverState)
            .thickBlackHoverOutline(
                highlighted = hoverState.isHighlighted,
                shape = shape,
                blackWidth = 3.5.dp,
                contrastHaloColor = ElectricCyan.copy(alpha = 0.95f)
            ),
        content = content
    )
}
