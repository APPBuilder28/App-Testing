package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SurfaceCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    hasSavedSources: Boolean,
    onNavigateToHome: () -> Unit,
    onNavigateToAddSource: () -> Unit
) {
    val strings = LocalAppStrings.current
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    LaunchedEffect(hasSavedSources) {
        delay(2000L)
        if (hasSavedSources) onNavigateToHome() else onNavigateToAddSource()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        DeepViolet.copy(alpha = 0.16f),
                        BgCharcoal,
                        BgCharcoal
                    )
                )
            )
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("splash_screen")
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ElectricCyan.copy(alpha = glowAlpha),
                                    DeepViolet.copy(alpha = glowAlpha * 0.6f),
                                    Color.Transparent
                                )
                            ),
                            radius = size.maxDimension * 0.72f
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nextvu_icon_fg_1794523727740),
                    contentDescription = strings.appName,
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(listOf(ElectricCyan, DeepViolet)),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = strings.appName,
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.loadingSources,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = ElectricCyan,
                trackColor = SurfaceCardVariant
            )
        }

        Text(
            text = strings.legalDisclaimerShort,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .alpha(0.9f)
                .testTag("splash_legal_disclaimer")
        )
    }
}
