package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NextVuDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF061318),
    primaryContainer = Color(0xFF003642),
    onPrimaryContainer = ElectricCyan,
    secondary = DeepViolet,
    onSecondary = TextPrimary,
    secondaryContainer = Color(0xFF2E1659),
    onSecondaryContainer = DeepVioletSoft,
    tertiary = HdrGold,
    onTertiary = Color(0xFF1E1700),
    background = BgCharcoal,
    onBackground = TextPrimary,
    surface = BgCharcoalElevated,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = SurfaceCard,
    surfaceContainer = SurfaceCardVariant,
    outline = SurfaceBorderSubtle,
    outlineVariant = Color(0xFF28283B),
    error = ErrorRose,
    onError = Color(0xFF260009)
)

val NextVuShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun NextVuTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NextVuDarkColorScheme,
        typography = NextVuTypography,
        shapes = NextVuShapes,
        content = content
    )
}
