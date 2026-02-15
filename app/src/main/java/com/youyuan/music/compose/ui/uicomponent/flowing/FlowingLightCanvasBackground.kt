package com.youyuan.music.compose.ui.uicomponent.flowing

import android.graphics.Bitmap
import android.graphics.Color.BLACK
import android.graphics.Color.HSVToColor
import android.graphics.Color.argb
import android.graphics.Color.colorToHSV
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
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

private fun buildFallbackPalette(isDarkTheme: Boolean): PaletteState {
    return if (isDarkTheme) {
        PaletteState(
            blobColors = listOf(
                Color(0xFF2A2238),
                Color(0xFF23324A),
                Color(0xFF3A2448),
                Color(0xFF1E2C38),
                Color(0xFF2B2636),
            ),
            backgroundColor = Color(0xFF0E0E14),
        )
    } else {
        PaletteState(
            blobColors = listOf(
                Color(0xFFC9D3F6),
                Color(0xFFD5CFF2),
                Color(0xFFC6D9EE),
                Color(0xFFDCCDEA),
                Color(0xFFCBD7F4),
            ),
            backgroundColor = Color(0xFFF2F4FA),
        )
    }
}

@Composable
fun FlowingLightCanvasBackground(
    isPlaying: Boolean,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    onImageLoadResult: ((Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val isSystemDarkTheme = isSystemInDarkTheme()
    val density = LocalDensity.current
    val processor = remember { FlowingLightProcessor(context) }
    val fallbackPalette = remember(isSystemDarkTheme) { buildFallbackPalette(isSystemDarkTheme) }

    val cachedInitial = remember { FlowingLightPaletteCache.lastState }
    val initialPalette = remember(imageUrl, cachedInitial, fallbackPalette) {
        if (imageUrl.isNullOrBlank()) fallbackPalette else (cachedInitial ?: fallbackPalette)
    }

    var paletteState by remember {
        mutableStateOf(
            initialPalette
        )
    }
    var stableImageUrl by remember { mutableStateOf(imageUrl ?: "") }
    var hasValidPalette by remember { mutableStateOf(cachedInitial != null) }
    var fromPaletteState by remember { mutableStateOf(paletteState) }
    var toPaletteState by remember { mutableStateOf(paletteState) }
    val paletteTransition = remember { Animatable(1f) }

    var animationClockSeconds by remember { mutableFloatStateOf(0f) }

    val layers = remember {
        listOf(
            FlowLayer(0.16f, 0.14f, 0, 0.74f, 0.52f, 0.16f, 0.14f, 0.2f, 1.28f),
            FlowLayer(0.86f, 0.18f, 1, 0.70f, 0.50f, 0.14f, 0.16f, 1.1f, 1.42f),
            FlowLayer(0.18f, 0.86f, 2, 0.76f, 0.48f, 0.15f, 0.13f, 2.3f, 1.16f),
            FlowLayer(0.86f, 0.84f, 3, 0.72f, 0.46f, 0.13f, 0.15f, 3.1f, 1.34f),
            FlowLayer(0.50f, 0.52f, 4, 0.86f, 0.54f, 0.09f, 0.09f, 4.2f, 1.04f),
        )
    }

    val renderedPalette = remember(fromPaletteState, toPaletteState, paletteTransition.value) {
        blendPalette(fromPaletteState, toPaletteState, paletteTransition.value)
    }

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
            if (stableImageUrl.isBlank() && !imageUrl.isNullOrBlank()) {
                stableImageUrl = imageUrl
            } else {
                delay(200)
                stableImageUrl = imageUrl ?: ""
            }
        }
    }

    LaunchedEffect(stableImageUrl) {
        if (stableImageUrl.isBlank()) {
            if (!imageUrl.isNullOrBlank()) {
                return@LaunchedEffect
            }
            val current = blendPalette(fromPaletteState, toPaletteState, paletteTransition.value)
            fromPaletteState = current
            toPaletteState = fallbackPalette
            paletteState = fallbackPalette
            paletteTransition.snapTo(0f)
            paletteTransition.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = LinearEasing),
            )
            hasValidPalette = false
            onImageLoadResult?.invoke(false)
            return@LaunchedEffect
        }

        if (FlowingLightPaletteCache.lastState != null) {
            onImageLoadResult?.invoke(true)
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
            onImageLoadResult?.invoke(true)
        }

        try {
            val sourceBitmap = processor.loadPaletteBitmap(stableImageUrl)
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
            var lastFrameNanos = 0L
            while (isActive) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameNanos != 0L) {
                        val deltaSeconds = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                        animationClockSeconds += deltaSeconds
                    }
                    lastFrameNanos = frameTimeNanos
                }
            }
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
            val rotation1Value = (animationClockSeconds * (360f / 74f)) % 360f
            val rotation2Value = (-animationClockSeconds * (360f / 92f)) % 360f
            val t1 = (animationClockSeconds / 42f) * (2f * PI.toFloat())
            val t2 = (animationClockSeconds / 54f) * (2f * PI.toFloat())

            for (index in layers.indices) {
                val layer = layers[index]
                val rotation = if (index % 2 == 0) rotation1Value else rotation2Value
                val orbitX = size.width * layer.orbitXFactor
                val orbitY = size.height * layer.orbitYFactor
                val primaryX = sin(t1 * layer.speed + layer.phase) * orbitX
                val primaryY = cos(t2 * layer.speed + layer.phase * 1.37f) * orbitY
                val secondaryX = sin(t2 * layer.speed * 1.7f + layer.phase * 2.1f) * orbitX * 0.34f
                val secondaryY = cos(t1 * layer.speed * 1.5f + layer.phase * 1.9f) * orbitY * 0.34f
                val centerX = baseCenters[index].x + primaryX + secondaryX
                val centerY = baseCenters[index].y + primaryY + secondaryY
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
    val blobColors = buildHarmonicBlobColors(seedHsv, isLowSaturation)

    val darkBase = palette.darkMutedSwatch?.rgb?.let(::Color)
        ?: palette.dominantSwatch?.rgb?.let(::Color)
        ?: Color.Black

    val backgroundColor = if (isLowSaturation) Color(0xFF121212) else applyDarkBackgroundFilter(darkBase)

    PaletteState(
        blobColors = blobColors,
        backgroundColor = backgroundColor,
    )
}

private fun buildHarmonicBlobColors(seedHsv: FloatArray, isLowSaturation: Boolean): List<Color> {
    if (isLowSaturation) {
        val values = floatArrayOf(0.22f, 0.32f, 0.42f, 0.52f, 0.62f)
        return values.map { value ->
            Color(HSVToColor(floatArrayOf(seedHsv[0], 0f, value)))
        }
    }

    val baseHue = seedHsv[0]
    val baseSaturation = seedHsv[1].coerceIn(0.24f, 0.58f)
    val baseValue = seedHsv[2].coerceIn(0.38f, 0.68f)
    val hueOffsets = floatArrayOf(-16f, -7f, 0f, 8f, 17f)
    val satFactors = floatArrayOf(0.92f, 0.98f, 1.0f, 0.95f, 0.9f)
    val valFactors = floatArrayOf(0.84f, 0.92f, 1.0f, 0.9f, 0.82f)

    return List(5) { index ->
        val hsv = floatArrayOf(
            (baseHue + hueOffsets[index] + 360f) % 360f,
            (baseSaturation * satFactors[index]).coerceIn(0.22f, 0.62f),
            (baseValue * valFactors[index]).coerceIn(0.34f, 0.74f)
        )
        Color(HSVToColor(hsv))
    }
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