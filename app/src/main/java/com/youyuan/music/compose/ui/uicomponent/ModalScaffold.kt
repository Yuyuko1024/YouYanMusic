package com.youyuan.music.compose.ui.uicomponent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ModalScaffold(
    isModalOpen: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmDismiss: () -> Boolean = { true },
    targetRadius: Dp = 16.dp,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = 380),
    dismissThresholdFraction: Float = 0.5f,
    modalContent: @Composable (dragHandleModifier: Modifier) -> Unit,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isPhoneLike = maxWidth < 600.dp
        if (isPhoneLike) {
            MobileModalScaffold(
                isModalOpen = isModalOpen,
                onDismissRequest = onDismissRequest,
                confirmDismiss = confirmDismiss,
                targetRadius = targetRadius,
                animationSpec = animationSpec,
                dismissThresholdFraction = dismissThresholdFraction,
                modalContent = modalContent,
                content = content
            )
        } else {
            PadModalScaffold(
                isModalOpen = isModalOpen,
                onDismissRequest = onDismissRequest,
                confirmDismiss = confirmDismiss,
                targetRadius = targetRadius,
                animationSpec = animationSpec,
                modalContent = modalContent,
                content = content
            )
        }
    }
}

@Composable
private fun MobileModalScaffold(
    isModalOpen: Boolean,
    onDismissRequest: () -> Unit,
    confirmDismiss: () -> Boolean,
    targetRadius: Dp,
    animationSpec: AnimationSpec<Float>,
    dismissThresholdFraction: Float,
    modalContent: @Composable (dragHandleModifier: Modifier) -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current

    val screenHeight = with(density) { windowInfo.containerSize.height.toDp() }
    val backgroundScale = 0.95f

    val offsetY = remember { Animatable(0f) }
    var modalHeight by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isModalOpen, modalHeight) {
        if (modalHeight == 0f) return@LaunchedEffect
        val target = if (isModalOpen) 0f else modalHeight
        if (offsetY.value != target) {
            offsetY.animateTo(target, animationSpec)
        }
    }

    BackHandler(enabled = isModalOpen) {
        if (confirmDismiss()) onDismissRequest()
    }

    val progress = if (modalHeight > 0f) {
        (offsetY.value / modalHeight).coerceIn(0f, 1f)
    } else {
        if (isModalOpen) 0f else 1f
    }

    val scale = lerp(backgroundScale, 1f, progress)
    val dimAlpha = lerp(0.4f, 0f, progress)
    val topPadding = (screenHeight * (1 - backgroundScale) / 2f + 16.dp).coerceAtLeast(0.dp)

    val screenShape = if (progress != 1f) RoundedCornerShape(targetRadius) else RectangleShape
    val modalShape = RoundedCornerShape(topStart = targetRadius, topEnd = targetRadius)

    val dragHandleModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                scope.launch {
                    val newOffset = (offsetY.value + dragAmount).coerceAtLeast(0f)
                    offsetY.snapTo(newOffset)
                }
            },
            onDragEnd = {
                scope.launch {
                    if (offsetY.value > modalHeight * dismissThresholdFraction) {
                        if (confirmDismiss()) {
                            offsetY.animateTo(modalHeight, animationSpec)
                            onDismissRequest()
                        } else {
                            offsetY.animateTo(0f, animationSpec)
                        }
                    } else {
                        offsetY.animateTo(0f, animationSpec)
                    }
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(screenShape)
        ) {
            content()
        }

        if (progress != 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
                    .clickable {
                        if (confirmDismiss()) onDismissRequest()
                    }
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .alpha(if (progress != 1f) 1f else 0f)
                .padding(top = topPadding)
                .fillMaxSize()
                .clip(modalShape)
                .background(if (isSystemInDarkTheme()) Color(0xFF171717) else Color.White)
                .onSizeChanged { size ->
                    if (modalHeight == 0f && !isModalOpen) {
                        scope.launch { offsetY.snapTo(size.height.toFloat()) }
                    }
                    modalHeight = size.height.toFloat()
                }
        ) {
            modalContent(dragHandleModifier)
        }
    }
}

@Composable
private fun PadModalScaffold(
    isModalOpen: Boolean,
    onDismissRequest: () -> Unit,
    confirmDismiss: () -> Boolean,
    targetRadius: Dp,
    animationSpec: AnimationSpec<Float>,
    modalContent: @Composable (dragHandleModifier: Modifier) -> Unit,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(if (isModalOpen) 0f else 1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isModalOpen) {
        progress.animateTo(if (isModalOpen) 0f else 1f, animationSpec)
    }

    BackHandler(enabled = isModalOpen) {
        if (confirmDismiss()) onDismissRequest()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (progress.value != 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = lerp(0.4f, 0f, progress.value)))
                    .clickable {
                        if (confirmDismiss()) {
                            scope.launch { progress.animateTo(1f, animationSpec) }
                            onDismissRequest()
                        }
                    }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = 1f - progress.value
                    scaleX = (1f - progress.value) * 0.05f + 1f
                    scaleY = (1f - progress.value) * 0.05f + 1f
                }
                .systemBarsPadding()
                .padding(vertical = 20.dp)
                .clip(RoundedCornerShape(targetRadius))
                .background(if (isSystemInDarkTheme()) Color(0xFF171717) else Color.White)
                .sizeIn(maxWidth = 420.dp)
        ) {
            modalContent(Modifier)
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}
