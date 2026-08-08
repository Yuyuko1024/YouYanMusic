package com.youyuan.music.compose.ui.player

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.youyuan.music.compose.R
import com.youyuan.music.compose.data.model.SongItem as SongItemModel
import com.youyuan.music.compose.ui.uicomponent.flowing.FlowingLightCanvasBackground
import com.youyuan.music.compose.ui.utils.LocalPlayerUIColor
import com.youyuan.music.compose.ui.utils.composable.Capturable
import com.youyuan.music.compose.ui.utils.composable.CapturableController
import com.youyuan.music.compose.ui.utils.composable.rememberCapturableController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.absoluteValue

private enum class ShareStep {
    SELECTING,
    GENERATING,
}

data class ShareLyricLine(
    val content: String,
    val translation: String?,
    val start: Int,
    val end: Int,
)

private fun ISyncedLine.toShareLyricLine(): ShareLyricLine? {
    return when (this) {
        is KaraokeLine -> {
            val text = syllables.joinToString("") { it.content }.trim()
            if (text.isEmpty()) null else ShareLyricLine(text, translation, start, end)
        }

        is SyncedLine -> {
            val text = content.trim()
            if (text.isEmpty()) null else ShareLyricLine(text, translation, start, end)
        }

        else -> null
    }
}

fun buildShareLyricLines(lyrics: SyncedLyrics): List<ShareLyricLine> {
    return lyrics.lines
        .mapNotNull { it.toShareLyricLine() }
        .sortedBy { it.start }
}

@Composable
fun LyricsShareDialog(
    lyrics: SyncedLyrics,
    initialLineStart: Int,
    isPlaying: Boolean,
    artworkUrl: String?,
    currentSong: SongItemModel?,
    dragModifier: Modifier,
    onDismissRequest: () -> Unit,
) {

    val localContext = androidx.compose.ui.platform.LocalContext.current
    val allLines = remember(lyrics) { buildShareLyricLines(lyrics) }
    if (allLines.isEmpty()) return

    val initialSelection = remember(allLines, initialLineStart) {
        val initialLine = allLines.minByOrNull { kotlin.math.abs(it.start - initialLineStart) }
            ?: allLines.first()
        listOf(initialLine)
    }

    var step by remember { mutableStateOf(ShareStep.SELECTING) }
    var selectedLines by remember { mutableStateOf(initialSelection) }
    var showTranslation by remember { mutableStateOf(true) }
    var showSongDetail by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(vertical = 12.dp)
    ) {
        Box(
            modifier = dragModifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(SaltTheme.colors.subText.copy(alpha = 0.35f))
                    .width(36.dp)
                    .height(4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("歌词分享", style = SaltTheme.textStyles.main)
            Text(
                text = "关闭",
                style = SaltTheme.textStyles.sub,
                modifier = Modifier.clickable { onDismissRequest() }
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(0.5.dp)
                .background(SaltTheme.colors.subText.copy(alpha = 0.25f))
        )

        when (step) {
            ShareStep.SELECTING -> {
                ShareSelectionStep(
                    lines = allLines,
                    selectedLines = selectedLines,
                    onLineToggled = { line ->
                        selectedLines = toggleSelectedLine(selectedLines, allLines, line)
                    },
                    onGenerate = { step = ShareStep.GENERATING }
                )
            }

            ShareStep.GENERATING -> {
                ShareGenerateStep(
                    context = localContext,
                    selectedLines = selectedLines,
                    showTranslation = showTranslation,
                    showSongDetail = showSongDetail,
                    isPlaying = isPlaying,
                    currentSong = currentSong,
                    artworkUrl = artworkUrl,
                    onToggleTranslation = { showTranslation = it },
                    onToggleSongDetail = { showSongDetail = it },
                    onBack = { step = ShareStep.SELECTING }
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ShareSelectionStep(
    lines: List<ShareLyricLine>,
    selectedLines: List<ShareLyricLine>,
    onLineToggled: (ShareLyricLine) -> Unit,
    onGenerate: () -> Unit,
) {
    val shareLazyState = rememberLazyListState()

    LaunchedEffect(selectedLines.firstOrNull()) {
        val line = selectedLines.firstOrNull() ?: return@LaunchedEffect
        val index = lines.indexOf(line)
        if (index >= 0) {
            shareLazyState.scrollToItem(index)
        }
    }

    LazyColumn(
        state = shareLazyState,
        modifier = Modifier
            .weight(1f)
            .background(if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(lines, key = { it.start }) { line ->
            val isSelected = selectedLines.contains(line)
            val maxedOut = selectedLines.size >= 5
            val background by animateColorAsState(targetValue = when {
                isSelected -> Color(0xFF3482FF).copy(alpha = 0.2f)
                maxedOut -> Color.Gray.copy(alpha = 0.16f)
                else -> SaltTheme.colors.popup
            }, label = "share_line_bg")

            val shape = if (selectedLines.size > 1) {
                when (line) {
                    selectedLines.first() -> RoundedCornerShape(16.dp, 16.dp, 8.dp, 8.dp)
                    selectedLines.last() -> RoundedCornerShape(8.dp, 8.dp, 16.dp, 16.dp)
                    else -> RoundedCornerShape(8.dp)
                }
            } else {
                RoundedCornerShape(16.dp)
            }

            Column(
                modifier = Modifier
                    .clip(shape)
                    .fillMaxWidth()
                    .background(background)
                    .clickable { onLineToggled(line) }
                    .padding(14.dp)
            ) {
                Text(
                    text = line.content,
                    style = SaltTheme.textStyles.main.copy(fontWeight = FontWeight.Bold)
                )
                line.translation?.let {
                    Text(
                        text = it,
                        style = SaltTheme.textStyles.sub,
                        modifier = Modifier.alpha(0.7f)
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("已选择 ${selectedLines.size}/5 行", style = SaltTheme.textStyles.sub)
        Spacer(Modifier.weight(1f))
        PillActionButton(
            text = "生成卡片",
            enabled = selectedLines.isNotEmpty(),
            onClick = onGenerate
        )
    }
}

@Composable
private fun ColumnScope.ShareGenerateStep(
    context: Context,
    selectedLines: List<ShareLyricLine>,
    showTranslation: Boolean,
    showSongDetail: Boolean,
    onToggleTranslation: (Boolean) -> Unit,
    onToggleSongDetail: (Boolean) -> Unit,
    isPlaying: Boolean,
    artworkUrl: String?,
    currentSong: SongItemModel?,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val appleController = rememberCapturableController()
    val spotifyController = rememberCapturableController()
    val capturableControllers = listOf(appleController, spotifyController)

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)),
        beyondViewportPageCount = 2,
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pagerCubeInDepthTransition(page, pagerState)
        ) {
            when (page) {
                0 -> LyricsShareCardApple(
                    capturableController = capturableControllers[page],
                    selectedLines = selectedLines,
                    showTranslation = showTranslation,
                    showSongDetail = showSongDetail,
                    isPlaying = isPlaying,
                    currentSong = currentSong,
                    artworkUrl = artworkUrl,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .sizeIn(maxWidth = 320.dp)
                )

                1 -> LyricsShareCardSpotify(
                    capturableController = capturableControllers[page],
                    selectedLines = selectedLines,
                    showTranslation = showTranslation,
                    currentSong = currentSong,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .sizeIn(maxWidth = 320.dp)
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pagerState.pageCount) { index ->
            val active = pagerState.currentPage == index
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (active) SaltTheme.colors.highlight else SaltTheme.colors.subText.copy(alpha = 0.35f))
                    .width(if (active) 18.dp else 8.dp)
                    .height(8.dp)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (selectedLines.any { it.translation != null }) {
            ItemSwitcher(
                state = showTranslation,
                onChange = onToggleTranslation,
                text = "显示翻译"
            )
        }
        // Apple样式才可以用
        AnimatedVisibility(pagerState.currentPage == 0) {
            ItemSwitcher(
                state = showSongDetail,
                onChange = onToggleSongDetail,
                text = "显示歌曲信息"
            )
        }
    }


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PillActionButton(text = "返回", enabled = true, onClick = onBack)
        PillActionButton(
            text = "保存",
            enabled = selectedLines.isNotEmpty(),
            modifier = Modifier.weight(1f),
            onClick = {
                capturableControllers[pagerState.currentPage].capture { bitmap ->
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            saveBitmapToGallery(context, bitmap.asAndroidBitmap())
                        }
                        Toast.makeText(
                            context,
                            if (ok) "已保存到相册" else "保存失败",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
        PillActionButton(
            text = "分享",
            enabled = selectedLines.isNotEmpty(),
            modifier = Modifier.weight(1f),
            onClick = {
                capturableControllers[pagerState.currentPage].capture { bitmap ->
                    scope.launch {
                        val uri = withContext(Dispatchers.IO) {
                            writeBitmapToShareUri(context, bitmap.asAndroidBitmap())
                        }
                        if (uri == null) {
                            Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        shareImageUri(context, uri)
                    }
                }
            }
        )
    }
}

@Composable
private fun LyricsShareCardApple(
    capturableController: CapturableController,
    selectedLines: List<ShareLyricLine>,
    showTranslation: Boolean,
    showSongDetail: Boolean,
    isPlaying: Boolean,
    artworkUrl: String?,
    currentSong: SongItemModel?,
    modifier: Modifier = Modifier,
) {
    val uiColor = LocalPlayerUIColor.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.15f))
    ) {
        Capturable(controller = capturableController) {
            Box {
                FlowingLightCanvasBackground(
                    modifier = Modifier.matchParentSize(),
                    isPlaying = isPlaying,
                    imageUrl = artworkUrl
                )
                LazyColumn(modifier = Modifier.padding(vertical = 16.dp)) {
                    item {
                        AnimatedVisibility(visible = showSongDetail) {
                            NowPlayingHeader(
                                song = currentSong,
                                albumArtUrl = artworkUrl,
                                color = uiColor
                            )
                        }
                    }

                    items(selectedLines, key = { it.start }) { line ->
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                text = line.content,
                                style = SaltTheme.textStyles.main.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            AnimatedVisibility(visible = showTranslation) {
                                line.translation?.let {
                                    Text(
                                        text = it,
                                        style = SaltTheme.textStyles.sub,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "by YouYanMusic",
                                style = SaltTheme.textStyles.sub,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsShareCardSpotify(
    capturableController: CapturableController,
    selectedLines: List<ShareLyricLine>,
    currentSong: SongItemModel?,
    showTranslation: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1F1F1F))
    ) {
        Capturable(controller = capturableController) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A2A2A))
                    .padding(vertical = 16.dp)
            ) {
                items(selectedLines, key = { it.start }) { line ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = line.content,
                            style = SaltTheme.textStyles.main.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        AnimatedVisibility(visible = showTranslation) {
                            line.translation?.let {
                                Text(
                                    text = it,
                                    style = SaltTheme.textStyles.sub,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.10f))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        val artist = currentSong?.artists?.joinToString(", ") { it.name } ?: ""

                        Text(
                            text = "${currentSong?.name}",
                            style = SaltTheme.textStyles.main,
                            color = Color.White
                        )
                        Text(
                            text = artist,
                            style = SaltTheme.textStyles.sub,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "by YouYanMusic",
                            style = SaltTheme.textStyles.sub,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.pagerCubeInDepthTransition(page: Int, pagerState: PagerState) = graphicsLayer {
    cameraDistance = 32f
    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
    transformOrigin = TransformOrigin(0.5f, 0.5f)
    rotationY = -90f * pageOffset

    val scale = 1f - 0.2f * pageOffset.absoluteValue
    scaleX = scale
    scaleY = scale
}

@Composable
private fun PillActionButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = if (enabled) SaltTheme.colors.highlight else SaltTheme.colors.highlight.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .width(88.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, style = SaltTheme.textStyles.sub)
    }
}

private fun toggleSelectedLine(
    current: List<ShareLyricLine>,
    allLines: List<ShareLyricLine>,
    line: ShareLyricLine,
): List<ShareLyricLine> {
    return if (current.contains(line)) {
        if (current.size > 1) current - line else current
    } else {
        if (current.size >= 5) current
        else (current + line).sortedBy { allLines.indexOf(it) }
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
    return runCatching {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "YouYanMusic_Share_${System.currentTimeMillis()}.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/YouYanMusic")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return false
        resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } ?: return false
        true
    }.getOrDefault(false)
}

private fun writeBitmapToShareUri(context: Context, bitmap: Bitmap): Uri? {
    return runCatching {
        val cachePath = File(context.cacheDir, "shared_images")
        cachePath.mkdirs()
        val imageFile = File(cachePath, "lyrics_share_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }.getOrNull()
}

private fun shareImageUri(context: Context, uri: Uri) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_STREAM, uri)
        type = "image/png"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooserIntent = Intent.createChooser(sendIntent, null).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooserIntent)
}

@Composable
private fun NowPlayingHeader(
    song: SongItemModel?,
    albumArtUrl: String?,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(albumArtUrl)
                .crossfade(true)
                .placeholder(R.drawable.ic_nav_music)
                .build(),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp))
                .align(Alignment.CenterVertically),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = song?.name ?: "",
                style = SaltTheme.textStyles.main,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = color
            )

            val artist = song?.artists?.joinToString(", ") { it.name } ?: ""
            val album = song?.album?.name ?: ""

            val subTitle = "$artist - $album"
            Text(
                text = subTitle,
                style = SaltTheme.textStyles.sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = color.copy(alpha = 0.5f)
            )
        }
    }

}