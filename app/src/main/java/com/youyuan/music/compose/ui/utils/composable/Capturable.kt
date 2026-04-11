package com.youyuan.music.compose.ui.utils.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.launch

class CapturableController {
    internal var triggerCapture: ((onCaptured: (ImageBitmap) -> Unit) -> Unit)? = null

    fun capture(onCaptured: (ImageBitmap) -> Unit) {
        triggerCapture?.invoke(onCaptured)
    }
}

@Composable
fun rememberCapturableController(): CapturableController {
    return remember { CapturableController() }
}

@Composable
fun Capturable(
    controller: CapturableController,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    controller.triggerCapture = { onCapturedCallback ->
        coroutineScope.launch {
            val imageBitmap = graphicsLayer.toImageBitmap()
            onCapturedCallback(imageBitmap)
        }
    }

    Box(
        modifier = modifier.drawWithContent {
            graphicsLayer.record {
                this@drawWithContent.drawContent()
            }
            drawLayer(graphicsLayer)
        }
    ) {
        content()
    }
}
