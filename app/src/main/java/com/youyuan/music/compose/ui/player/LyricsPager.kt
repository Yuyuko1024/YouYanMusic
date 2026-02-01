package com.youyuan.music.compose.ui.player

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R
import com.youyuan.music.compose.constants.PlayerCoverVerticalPadding
import com.youyuan.music.compose.constants.PlayerHorizontalPadding
import com.youyuan.music.compose.ui.utils.LocalPlayerUIColor
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel
import com.youyuan.music.compose.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@SuppressLint("UnusedBoxWithConstraintsScope")
@UnstableSaltUiApi
@UnstableApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun LyricsPager(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel
) {
    val lyrics = playerViewModel.lyrics.collectAsState().value
    val currentPlaying = playerViewModel.currentMediaItem.collectAsState().value
    val currentPositionState = playerViewModel.currentPosition.collectAsState()
    val isPlayingState = playerViewModel.isPlaying.collectAsState()
    val durationState = playerViewModel.duration.collectAsState()

    val uiColor = LocalPlayerUIColor.current

    // 缓存 AutoParser 实例
    val autoParser = remember { AutoParser.Builder().build() }
    // 使用 remember 保存解析后的歌词，避免重复解析
    var parsedLyrics by remember { mutableStateOf<SyncedLyrics?>(null) }
    val listState = rememberLazyListState()

    // 定义动画状态
    val animatedPositionState = remember { mutableLongStateOf(0L) }
    val currentPositionProvider = remember {
        { animatedPositionState.longValue.toInt() }
    }

    var anchorState by remember { mutableStateOf(0L to 0L) }

    val currentPosition = currentPositionState.value
    LaunchedEffect(currentPosition) {
        anchorState = currentPosition to System.currentTimeMillis()

        // 如果误差过大（比如拖动进度条、切歌），直接强制同步，不平滑过渡
        if (abs(animatedPositionState.longValue - currentPosition) > 1000) {
            animatedPositionState.longValue = currentPosition
        }
    }

    // 解析歌词
    LaunchedEffect(lyrics) {
        parsedLyrics = lyrics?.let {
            try {
                withContext(Dispatchers.Default) {
                    autoParser.parse(it)
                }
            } catch (e: Exception) {
                Logger.err("LyricsPager", "解析歌词失败: ${e.message}")
                null
            }
        }
        Logger.debug("LyricsPager", "解析歌词完成: ${parsedLyrics != null}")
    }

    val isPlaying = isPlayingState.value
    val latestDuration by rememberUpdatedState(durationState.value) // 始终获取最新时长

    // 平滑的位置更新动画
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                // 获取当前的锚点信息
                val (anchorPos, anchorTime) = anchorState

                val elapsed = System.currentTimeMillis() - anchorTime

                val newPosition = (anchorPos + elapsed).coerceAtMost(latestDuration)

                val currentAnimPos = animatedPositionState.longValue
                if (currentAnimPos <= newPosition || abs(newPosition - currentAnimPos) >= 100) {
                    animatedPositionState.longValue = newPosition
                }
                // 等待下一帧
                awaitFrame()
            }
        } else {
            // 暂停状态下：直接同步真实进度
            animatedPositionState.longValue = anchorState.first
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = PlayerHorizontalPadding,
                vertical = PlayerCoverVerticalPadding
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            if (currentPlaying != null) {
                LyricsNowPlayingHeader(
                    mediaItem = currentPlaying,
                    color = uiColor,
                    onClick = {
                        // do nothing
                    }
                )
            }

            parsedLyrics?.let { syncedLyrics ->
                if (syncedLyrics.lines.isNotEmpty()) {
                    KaraokeLyricsView(
                        listState = listState,
                        lyrics = syncedLyrics,
                        currentPosition = { currentPositionProvider() },
                        onLineClicked = { line ->
                            playerViewModel.seekTo(line.start.toLong())
                        },
                        onLinePressed = { line ->
                            // 可以在这里添加长按功能，比如分享歌词等
                            Logger.debug("LyricsPager", "长按歌词行: ${line.start}")
                        },
                        normalLineTextStyle = SaltTheme.textStyles.paragraph,
                        accompanimentLineTextStyle = SaltTheme.textStyles.main,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                                blendMode = BlendMode.Plus
                            }
                    )
                } else {
                    // 显示无歌词状态,这里是防止部分歌词只有一行的情况下解析失败的问题
                    NoLyricsPlaceholder(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } ?: run {
                // 显示无歌词状态
                NoLyricsPlaceholder(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun NoLyricsPlaceholder(
    modifier: Modifier = Modifier
) {
    val uiColor = LocalPlayerUIColor.current

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_lyrics),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = uiColor,
        )
        Text(
            text = stringResource(R.string.enjoy_pure_music),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = uiColor,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun LyricsNowPlayingHeader(
    mediaItem: MediaItem?,
    color: Color,
    onClick: () -> Unit = { }
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = {
                onClick()
            })
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(mediaItem?.mediaMetadata?.artworkUri)
                .crossfade(true)
                .placeholder(R.drawable.ic_nav_music)
                .build(),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(60.dp)
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
                text = mediaItem?.mediaMetadata?.title.toString(),
                fontSize = 20.sp,
                style = SaltTheme.textStyles.main,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = color
            )

            Spacer(Modifier.height(4.dp))

            val artist = mediaItem?.mediaMetadata?.artist.toString()

            Text(
                text = artist,
                style = SaltTheme.textStyles.sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = color.copy(alpha = 0.5f)
            )
        }
    }

}