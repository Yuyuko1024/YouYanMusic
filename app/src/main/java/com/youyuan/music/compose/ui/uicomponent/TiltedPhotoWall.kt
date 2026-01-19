package com.youyuan.music.compose.ui.uicomponent

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Size
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlin.math.ceil
import kotlin.math.sqrt

@Composable
fun TiltedPhotoWall(
    modifier: Modifier = Modifier,
    imageUrls: List<String?>,
    columnCount: Int = 4,
    animationDuration: Int = 30000,
    tiltAngle: Float = -30f
) {
    if (imageUrls.isEmpty()) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val uriCount = imageUrls.size

    // 1. 获取父容器尺寸
    var parentSize by remember { mutableStateOf(Size.Zero) }

    // 2. 计算单个图片尺寸
    val itemSizePx = remember(parentSize.width, columnCount) {
        if (parentSize.width > 0) parentSize.width / columnCount else 0f
    }

    // 3. 计算循环周期大小（一个完整的网格周期）
    val cycleSizePx = remember(itemSizePx, columnCount) {
        itemSizePx * columnCount
    }

    // 4. 无限循环动画（对角线移动）
    val infiniteTransition = rememberInfiniteTransition(label = "TiltedWallAnimation")

    // 使用简单的循环动画（从 0 到一个周期的距离）
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = cycleSizePx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AnimatedOffset"
    )

    // 5. UI 渲染
    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { size ->
                parentSize = Size(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        if (itemSizePx > 0 && parentSize.width > 0 && parentSize.height > 0 && cycleSizePx > 0) {
            val itemSizeDp = with(density) { itemSizePx.toDp() }

            // 计算旋转后需要覆盖的区域
            val diagonal = sqrt(parentSize.width * parentSize.width + parentSize.height * parentSize.height)
            val expandedSize = diagonal * 1.5f

            // 计算当前视口需要显示的网格范围
            val visibleColumns = ceil(expandedSize / itemSizePx).toInt() + 3
            val visibleRows = ceil(expandedSize / itemSizePx).toInt() + 3

            // 计算当前偏移量对应的网格起始位置（使用模运算实现无限循环）
            val offsetCells = (animatedOffset / itemSizePx).toInt()
            val startCol = offsetCells % columnCount
            val startRow = offsetCells % columnCount

            // 整体旋转容器
            Box(modifier = Modifier.rotate(tiltAngle)) {
                Layout(
                    content = {
                        // 只生成可见区域 + 缓冲区的图片
                        for (row in 0 until visibleRows) {
                            for (col in 0 until visibleColumns) {
                                // 计算在原始图片列表中的索引（使用模运算循环）
                                val globalCol = (startCol + col) % columnCount
                                val globalRow = (startRow + row) % columnCount
                                val imageIndex = (globalRow * columnCount + globalCol) % uriCount

                                val currentUri = imageUrls[imageIndex]
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(currentUri)
                                        .size(itemSizePx.toInt())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(itemSizeDp),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                ) { measurables, constraints ->
                    val placeables = measurables.map { measurable ->
                        measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    }

                    val layoutWidth = parentSize.width.toInt().coerceIn(1, 16777215)
                    val layoutHeight = parentSize.height.toInt().coerceIn(1, 16777215)

                    layout(layoutWidth, layoutHeight) {
                        // 计算偏移量（模运算确保无缝循环）
                        val offsetX = -(animatedOffset % itemSizePx)
                        val offsetY = -(animatedOffset % itemSizePx)

                        placeables.forEachIndexed { index, placeable ->
                            val row = index / visibleColumns
                            val col = index % visibleColumns

                            // 放置图片（加上偏移量）
                            placeable.place(
                                x = (offsetX + col * itemSizePx - itemSizePx * 2).toInt(),
                                y = (offsetY + row * itemSizePx - itemSizePx * 2).toInt()
                            )
                        }
                    }
                }
            }
        }
    }
}