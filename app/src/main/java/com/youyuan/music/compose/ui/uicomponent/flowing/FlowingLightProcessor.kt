package com.youyuan.music.compose.ui.uicomponent.flowing

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.youyuan.music.compose.ui.utils.blur
import com.youyuan.music.compose.ui.utils.brightness
import com.youyuan.music.compose.ui.utils.handleImageEffect
import com.youyuan.music.compose.ui.utils.mesh
import com.youyuan.music.compose.ui.utils.zoom
import com.youyuan.music.compose.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FlowingLightProcessor(private val context: Context) {

    // Apple Music中的Mesh参数之一
    private val meshFloats = floatArrayOf(
        -0.2351f, -0.0967f, 0.2135f, -0.1414f, 0.9221f, -0.0908f, 0.9221f, -0.0685f, 1.3027f, 0.0253f, 1.2351f, 0.1786f,
        -0.3768f, 0.1851f, 0.2f, 0.2f, 0.6615f, 0.3146f, 0.9543f, 0.0f, 0.6969f, 0.1911f, 1.0f, 0.2f,
        0.0f, 0.4f, 0.2f, 0.4f, 0.0776f, 0.2318f, 0.6f, 0.4f, 0.6615f, 0.3851f, 1.0f, 0.4f,
        0.0f, 0.6f, 0.1291f, 0.6f, 0.4f, 0.6f, 0.4f, 0.4304f, 0.4264f, 0.5792f, 1.2029f, 0.8188f,
        -0.1192f, 1.0f, 0.6f, 0.8f, 0.4264f, 0.8104f, 0.6f, 0.8f, 0.8f, 0.8f, 1.0f, 0.8f,
        0.0f, 1.0f, 0.0776f, 1.0283f, 0.4f, 1.0f, 0.6f, 1.0f, 0.8f, 1.0f, 1.1868f, 1.0283f
    )

    suspend fun loadAndProcessImage(imageUrl: String?): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // 降低 Coil 加载的原始尺寸，避免加载几千像素的大图，极度节省内存
            val request = ImageRequest.Builder(context)
                .data(imageUrl ?: Uri.EMPTY)
                .allowHardware(false)
                .size(200) // 限制加载尺寸，流光溢彩不需要原图
                .crossfade(true)
                .build()

            Logger.debug("FlowingLightProcessor", "Loading image from http URL: $imageUrl")
            return@withContext (ImageLoader(context).execute(request).image as? BitmapImage)
                ?.bitmap?.let { processBitmap(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun processBitmap(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        // 1. 将图片缩小到合适尺寸，保持比例
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val targetHeight = (60 * aspectRatio).toInt().coerceAtLeast(10)

        // 初始缩放
        val resized = bitmap.zoom(60f, targetHeight.toFloat())

        // 2. 增加饱和度
        val saturated = resized.handleImageEffect(1.8f)

        // 3. 根据亮度调整处理参数，应用Apple Music的mesh效果
        val brightness = saturated.brightness()

        // 中间处理结果
        val meshed = when {
            brightness < 0.3f -> {
                saturated
                    .blur(18f) // 降低模糊半径，因为图片变小了，相对模糊度其实很大
                    .zoom(280f, (targetHeight * 2).toFloat())
            }
            brightness > 0.7f -> {
                saturated
                    .blur(30f)
                    .mesh(meshFloats)
                    .zoom(260f, (targetHeight * 1.9f))
            }
            else -> {
                saturated
                    .blur(9f)
                    .zoom(118f, (targetHeight * 1.95f))
            }
        }

        val finalResult = meshed.blur(20f)
        finalResult
    }
}