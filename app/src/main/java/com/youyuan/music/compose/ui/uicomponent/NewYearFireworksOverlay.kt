package com.youyuan.music.compose.ui.uicomponent

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.youyuan.music.compose.ui.theme.NewYearAccentGold
import com.youyuan.music.compose.ui.theme.NewYearPrimaryRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class FireworkSeed(
    val xFraction: Float,
    val yFraction: Float,
    val sizeFraction: Float,
    val cycleOffset: Float,
    val color: Color
)

@Composable
fun NewYearFireworksOverlay(modifier: Modifier = Modifier) {
    val seeds = remember {
        val random = Random(20260216)
        List(10) { index ->
            FireworkSeed(
                xFraction = 0.12f + random.nextFloat() * 0.76f,
                yFraction = 0.08f + random.nextFloat() * 0.45f,
                sizeFraction = 0.06f + random.nextFloat() * 0.06f,
                cycleOffset = random.nextFloat(),
                color = if (index % 2 == 0) NewYearPrimaryRed else NewYearAccentGold
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "new-year-fireworks")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    ).value

    Canvas(modifier = modifier) {
        val baseStroke = size.minDimension * 0.0038f
        val rays = 18

        seeds.forEachIndexed { index, seed ->
            val localPhase = (phase + seed.cycleOffset) % 1f
            val alphaIn = (localPhase / 0.18f).coerceIn(0f, 1f)
            val alphaOut = ((1f - localPhase) / 0.65f).coerceIn(0f, 1f)
            val alpha = (alphaIn * alphaOut * 0.7f).coerceIn(0f, 0.7f)
            if (alpha <= 0.01f) return@forEachIndexed

            val center = Offset(
                x = size.width * seed.xFraction,
                y = size.height * seed.yFraction
            )
            val radius = size.minDimension * seed.sizeFraction * (0.35f + localPhase)

            repeat(rays) { rayIndex ->
                val angle = (2 * PI / rays * rayIndex) + index * 0.14
                val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                val start = center + direction * (radius * 0.45f)
                val end = center + direction * radius
                drawLine(
                    color = seed.color.copy(alpha = alpha * (1f - rayIndex / (rays * 2f))),
                    start = start,
                    end = end,
                    strokeWidth = baseStroke * (1f + (rayIndex % 3) * 0.25f)
                )
            }

            drawCircle(
                color = seed.color.copy(alpha = alpha * 0.75f),
                radius = radius * 0.09f + baseStroke,
                center = center
            )
        }
    }
}
