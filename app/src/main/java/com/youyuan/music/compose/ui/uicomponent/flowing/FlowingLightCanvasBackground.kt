package com.youyuan.music.compose.ui.uicomponent.flowing

import android.graphics.Bitmap
import android.graphics.Color.BLACK
import android.graphics.Color.HSVToColor
import android.graphics.Color.argb
import android.graphics.Color.colorToHSV
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class FlowLayer(
    val anchorX: Float,
    val anchorY: Float,
    val colorIndex: Int,
    val radiusFactor: Float,
    val alpha: Float,
    val orbitXFactor: Float,
    val orbitYFactor: Float,
    val phase: Float,
    val speed: Float,
)

private data class PaletteState(
    val blobColors: List<Color>,
    val backgroundColor: Color,
)

private object FlowingLightPaletteCache {
    var lastState: PaletteState? = null
    val byUrl = mutableMapOf<String, PaletteState>()
}

@Composable
fun FlowingLightCanvasBackground(
    isPlaying: Boolean,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    onImageLoadResult: ((Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val processor = remember { FlowingLightProcessor(context) }

    val cachedInitial = remember { FlowingLightPaletteCache.lastState }

    var paletteState by remember {
        mutableStateOf(
            cachedInitial ?: PaletteState(
                blobColors = listOf(
                    Color(0xFF2A2238),
                    Color(0xFF23324A),
                    Color(0xFF3A2448),
                    Color(0xFF1E2C38),
                    Color(0xFF2B2636),
                ),
                backgroundColor = Color(0xFF0E0E14),
            )
        )
    }
    var stableImageUrl by remember { mutableStateOf("") }
    var hasValidPalette by remember { mutableStateOf(cachedInitial != null) }
    var fromPaletteState by remember { mutableStateOf(paletteState) }
    var toPaletteState by remember { mutableStateOf(paletteState) }
    val paletteTransition = remember { Animatable(1f) }

    val rotation1 = remember { Animatable(0f) }
    val rotation2 = remember { Animatable(0f) }
    val motion1 = remember { Animatable(0f) }
    val motion2 = remember { Animatable(0f) }

    val layers = remember {
        listOf(
            FlowLayer(0.18f, 0.16f, 0, 0.70f, 0.50f, 0.12f, 0.10f, 0.2f, 0.9f),
            FlowLayer(0.84f, 0.20f, 1, 0.66f, 0.48f, 0.10f, 0.12f, 1.1f, 1.1f),
            FlowLayer(0.20f, 0.84f, 2, 0.72f, 0.46f, 0.11f, 0.09f, 2.3f, 0.8f),
            FlowLayer(0.84f, 0.82f, 3, 0.68f, 0.44f, 0.09f, 0.10f, 3.1f, 1.0f),
            FlowLayer(0.50f, 0.52f, 4, 0.82f, 0.52f, 0.06f, 0.06f, 4.2f, 0.7f),
        )
    }

    val renderedPalette = blendPalette(fromPaletteState, toPaletteState, paletteTransition.value)

    val unitBrushes = remember(renderedPalette.blobColors, layers) {
        layers.map { layer ->
            val baseColor = renderedPalette.blobColors[layer.colorIndex].copy(alpha = layer.alpha)
            Brush.radialGradient(
                colorStops = arrayOf(
                    0f to baseColor,
                    0.62f to baseColor.copy(alpha = baseColor.alpha * 0.42f),
                    1f to Color.Transparent,
                ),
                center = Offset.Zero,
                radius = 1f,
            )
        }
    }

    LaunchedEffect(imageUrl) {
        if (imageUrl != stableImageUrl) {
            delay(200)
            stableImageUrl = imageUrl ?: ""
        }
    }

    LaunchedEffect(stableImageUrl) {
        if (stableImageUrl.isBlank()) {
            hasValidPalette = FlowingLightPaletteCache.lastState != null
            onImageLoadResult?.invoke(false)
            return@LaunchedEffect
        }

        FlowingLightPaletteCache.byUrl[stableImageUrl]?.let { cached ->
            val current = blendPalette(fromPaletteState, toPaletteState, paletteTransition.value)
            fromPaletteState = current
            toPaletteState = cached
            paletteState = cached
            paletteTransition.snapTo(0f)
            paletteTransition.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = LinearEasing),
            )
            hasValidPalette = true
        }

        try {
            val sourceBitmap = processor.loadAndProcessImage(stableImageUrl)
            if (sourceBitmap == null) {
                onImageLoadResult?.invoke(false)
                return@LaunchedEffect
            }

            val extracted = extractPaletteState(sourceBitmap)
            FlowingLightPaletteCache.byUrl[stableImageUrl] = extracted
            FlowingLightPaletteCache.lastState = extracted
            val current = blendPalette(fromPaletteState, toPaletteState, paletteTransition.value)
            fromPaletteState = current
            toPaletteState = extracted
            paletteState = extracted
            paletteTransition.snapTo(0f)
            paletteTransition.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 760, easing = LinearEasing),
            )
            hasValidPalette = true
            onImageLoadResult?.invoke(true)
        } catch (_: Exception) {
            onImageLoadResult?.invoke(false)
        }
    }

    val shouldAnimate = isPlaying && hasValidPalette
    LaunchedEffect(shouldAnimate) {
        if (shouldAnimate) {
            coroutineScope {
                launch {
                    rotation1.animateTo(
                        targetValue = rotation1.value + 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 98000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        ),
                    )
                }
                launch {
                    rotation2.animateTo(
                        targetValue = rotation2.value - 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 116000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        ),
                    )
                }
                launch {
                    motion1.animateTo(
                        targetValue = motion1.value + 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 72000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        ),
                    )
                }
                launch {
                    motion2.animateTo(
                        targetValue = motion2.value + 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 84000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        ),
                    )
                }
            }
        } else {
            rotation1.stop()
            rotation2.stop()
            motion1.stop()
            motion2.stop()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(renderedPalette.backgroundColor)
            .clipToBounds(),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val maxDim = maxOf(widthPx, heightPx)

        val baseCenters = remember(widthPx, heightPx, layers) {
            layers.map { layer ->
                Offset(x = widthPx * layer.anchorX, y = heightPx * layer.anchorY)
            }
        }

        val blobRadii = remember(maxDim, layers) {
            layers.map { layer -> maxDim * layer.radiusFactor }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            val canvas = drawContext.canvas
            val t1 = motion1.value * (2f * PI.toFloat())
            val t2 = motion2.value * (2f * PI.toFloat())

            for (index in layers.indices) {
                val layer = layers[index]
                val rotation = if (index % 2 == 0) rotation1.value else rotation2.value
                val orbitX = size.width * layer.orbitXFactor
                val orbitY = size.height * layer.orbitYFactor
                val centerX = baseCenters[index].x + sin(t1 * layer.speed + layer.phase) * orbitX
                val centerY = baseCenters[index].y + cos(t2 * layer.speed + layer.phase * 1.37f) * orbitY
                val radius = blobRadii[index]

                canvas.save()
                canvas.translate(cx, cy)
                canvas.rotate(rotation)
                canvas.translate(-cx, -cy)
                canvas.translate(centerX, centerY)
                canvas.scale(radius, radius)

                drawCircle(
                    brush = unitBrushes[index],
                    radius = 1f,
                    center = Offset.Zero,
                    blendMode = BlendMode.Screen,
                )

                canvas.restore()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f)),
        )
    }
}

private suspend fun extractPaletteState(bitmap: Bitmap): PaletteState = withContext(Dispatchers.Default) {
    val palette = Palette.from(bitmap)
        .resizeBitmapArea(200 * 200)
        .clearFilters()
        .generate()

    val seedColorInt = palette.getVibrantColor(
        palette.getDominantColor(BLACK)
    )
    val seedColor = Color(seedColorInt)

    val seedHsv = FloatArray(3)
    colorToHSV(seedColorInt, seedHsv)
    val isLowSaturation = seedHsv[1] < 0.2f

    val rawColors = mutableListOf<Color>()

    val vibrant = palette.vibrantSwatch?.rgb?.let(::Color)
    val lightVibrant = palette.lightVibrantSwatch?.rgb?.let(::Color)
    val darkVibrant = palette.darkVibrantSwatch?.rgb?.let(::Color)
    val muted = palette.mutedSwatch?.rgb?.let(::Color)
    val darkMuted = palette.darkMutedSwatch?.rgb?.let(::Color)

    if (vibrant != null) rawColors.add(vibrant)
    if (lightVibrant != null) rawColors.add(lightVibrant)
    if (darkVibrant != null) rawColors.add(darkVibrant)
    if (muted != null) rawColors.add(muted)
    if (darkMuted != null && isLowSaturation) rawColors.add(darkMuted)

    while (rawColors.size < 5) {
        if (isLowSaturation) {
            val randomVal = 0.28f + (rawColors.size * 0.12f)
            rawColors.add(shiftValue(seedColor, randomVal))
        } else {
            val shiftAmount = (rawColors.size + 1) * 24f
            rawColors.add(shiftHue(seedColor, shiftAmount))
        }
    }

    val blobColors = rawColors.take(5).map { color ->
        applyVibeBoost(color, isLowSaturation)
    }

    val darkBase = palette.darkMutedSwatch?.rgb?.let(::Color)
        ?: palette.dominantSwatch?.rgb?.let(::Color)
        ?: Color.Black

    val backgroundColor = if (isLowSaturation) {
        Color(0xFF121212)
    } else {
        applyDarkBackgroundFilter(darkBase)
    }

    PaletteState(
        blobColors = blobColors,
        backgroundColor = backgroundColor,
    )
}

private fun applyVibeBoost(color: Color, isLowSaturation: Boolean): Color {
    val hsv = FloatArray(3)
    colorToHSV(color.toArgb(), hsv)

    if (isLowSaturation) {
        hsv[1] = 0.0f

        if (hsv[2] > 0.4f) {
            hsv[2] = 0.62f + (Math.random().toFloat() * 0.14f)
        } else {
            hsv[2] = 0.18f + (Math.random().toFloat() * 0.08f)
        }
    } else {
        hsv[1] = (hsv[1].coerceAtLeast(0.25f) * 1.12f).coerceIn(0.28f, 0.72f)
        hsv[2] = (hsv[2].coerceAtLeast(0.36f) * 1.08f).coerceIn(0.48f, 0.84f)
    }

    return Color(HSVToColor(hsv))
}

/**
 * 辅助函数：改变亮度 (Value)
 */
private fun shiftValue(color: Color, targetValue: Float): Color {
    val hsv = FloatArray(3)
    colorToHSV(color.toArgb(), hsv)
    hsv[2] = targetValue.coerceIn(0.1f, 1.0f)
    return Color(HSVToColor(hsv))
}

/**
 * 旋转色相：基于基准色，偏移 degrees 度
 */
private fun shiftHue(color: Color, degrees: Float): Color {
    val hsv = FloatArray(3)
    colorToHSV(color.toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees) % 360f
    return Color(HSVToColor(hsv))
}

/**
 * 背景色处理：极暗，但保留一丝色调
 */
private fun applyDarkBackgroundFilter(color: Color): Color {
    val hsv = FloatArray(3)
    colorToHSV(color.toArgb(), hsv)

    hsv[1] = hsv[1] * 0.62f
    hsv[2] = 0.10f

    return Color(HSVToColor(hsv))
}

private fun blendPalette(from: PaletteState, to: PaletteState, t: Float): PaletteState {
    val clamped = t.coerceIn(0f, 1f)
    val size = minOf(from.blobColors.size, to.blobColors.size)
    val blobColors = (0 until size).map { index ->
        blendColor(from.blobColors[index], to.blobColors[index], clamped)
    }
    return PaletteState(
        blobColors = blobColors,
        backgroundColor = blendColor(from.backgroundColor, to.backgroundColor, clamped),
    )
}

private fun blendColor(from: Color, to: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * clamped,
        green = from.green + (to.green - from.green) * clamped,
        blue = from.blue + (to.blue - from.blue) * clamped,
        alpha = from.alpha + (to.alpha - from.alpha) * clamped,
    )
}

private fun Color.toArgb(): Int {
    return argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}