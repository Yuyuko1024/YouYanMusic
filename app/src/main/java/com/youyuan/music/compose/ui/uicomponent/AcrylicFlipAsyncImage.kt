package com.youyuan.music.compose.ui.uicomponent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun AcrylicFlipAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(12.dp),
    cameraDistance: Dp = 28.dp,
    flipDurationMillis: Int = 500,
) {
    val density = LocalDensity.current
    val rotationYAnim = remember { Animatable(0f) }

    var initialized by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(imageUrl) }
    var incomingUrl by remember { mutableStateOf<String?>(null) }
    var showingIncoming by remember { mutableStateOf(false) }

    val displayUrl by remember {
        derivedStateOf {
            if (showingIncoming) incomingUrl ?: currentUrl else currentUrl
        }
    }

    val flipProgress by remember {
        derivedStateOf {
            (kotlin.math.abs(rotationYAnim.value) / 90f).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(imageUrl) {
        if (!initialized) {
            currentUrl = imageUrl
            initialized = true
            return@LaunchedEffect
        }

        if (imageUrl == currentUrl || imageUrl == incomingUrl) return@LaunchedEffect

        incomingUrl = imageUrl
        showingIncoming = false
        rotationYAnim.snapTo(0f)

        val preFlipDuration = (flipDurationMillis * 0.35f).toInt().coerceAtLeast(90)

        rotationYAnim.animateTo(
            targetValue = -90f,
            animationSpec = tween(durationMillis = preFlipDuration, easing = LinearEasing)
        )

        showingIncoming = true
        currentUrl = incomingUrl
        rotationYAnim.snapTo(90f)

        rotationYAnim.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            )
        )

        incomingUrl = null
        showingIncoming = false
    }

    Box(
        modifier = modifier
            .clip(shape)
            .graphicsLayer {
                rotationY = rotationYAnim.value
                transformOrigin = TransformOrigin.Center
                this.cameraDistance = with(density) { cameraDistance.toPx() }
            }
    ) {
        AsyncImage(
            model = displayUrl,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val sweepX = size.width * (0.15f + 0.7f * flipProgress)
                    val highlightHalfWidth = size.width * 0.22f
                    val highlightBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.28f),
                            Color.Transparent,
                        ),
                        start = Offset(sweepX - highlightHalfWidth, 0f),
                        end = Offset(sweepX + highlightHalfWidth, size.height),
                    )
                    onDrawBehind {
                        drawRect(
                            brush = highlightBrush,
                            alpha = (0.16f + 0.5f * flipProgress).coerceAtMost(0.52f)
                        )
                    }
                }
                .background(
                    Color.White.copy(alpha = 0.04f + 0.06f * flipProgress)
                )
        )
    }
}
