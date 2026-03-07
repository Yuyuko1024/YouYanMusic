package com.youyuan.music.compose.ui.player

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R
import com.youyuan.music.compose.constants.MiniPlayerHeight
import com.youyuan.music.compose.constants.PlayerHorizontalPadding
import com.youyuan.music.compose.ui.uicomponent.AcrylicFlipAsyncImage
import com.youyuan.music.compose.ui.theme.UiDimens
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.PlayerPause
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.Playlist
import kotlin.math.abs
import kotlin.math.roundToInt

@UnstableSaltUiApi
@ExperimentalMaterial3Api
@UnstableApi
@ExperimentalFoundationApi
@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    context: Context,
    playerViewModel: PlayerViewModel,
    onPlaylistClick: () -> Unit = {},
) {
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val maxSwipeOffsetPx = with(density) { UiDimens.miniPlayerSwipeMaxOffset.toPx() }
    val triggerOffsetPx = maxSwipeOffsetPx * UiDimens.miniPlayerSwipeTriggerRatio
    val animatedOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = if (isDragging) {
            tween(durationMillis = 0)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "mini_player_swipe_offset"
    )


    // 当前Song对象
    val currentSong = playerViewModel.currentSong.collectAsState().value
    // 封面
    val currentArtworkUrl = playerViewModel.currentAlbumArtUrl.collectAsState().value
    // 标题
    fun List<String?>?.toDisplayText(): String? =
        this
            .orEmpty()
            .asSequence()
            .mapNotNull { it?.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(" / ")
            .takeIf { it.isNotBlank() }

    val baseTitle = currentSong?.name ?: stringResource(R.string.unknown_song)
    val aliasText = currentSong?.alia.toDisplayText()
        ?: currentSong?.tns.toDisplayText()
    val title = if (aliasText != null) "$baseTitle（$aliasText）" else baseTitle
    // 艺术家
    val artistName = playerViewModel.currentArtistNames.collectAsState().value
    val playlist = playerViewModel.playlist.collectAsState().value
    val player = playerViewModel.getPlayer()

    val isInTriggerZone = isDragging && abs(dragOffsetX) >= triggerOffsetPx
    val targetSongName = remember(playlist, dragOffsetX, player?.currentMediaItemIndex) {
        if (playlist.isEmpty()) return@remember null

        val targetIndex = if (dragOffsetX < 0f) {
            player?.nextMediaItemIndex
        } else if (dragOffsetX > 0f) {
            player?.previousMediaItemIndex
        } else {
            null
        }

        targetIndex
            ?.takeIf { it in playlist.indices }
            ?.let { playlist[it].song.name }
            ?.takeIf { it.isNotBlank() }
    }

    Row(
        modifier = modifier
            .height(MiniPlayerHeight)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {

        // 封面图片容器和背景
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 2.dp)
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(maxSwipeOffsetPx, triggerOffsetPx) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX = (dragOffsetX + dragAmount)
                                .coerceIn(-maxSwipeOffsetPx, maxSwipeOffsetPx)
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffsetX = 0f
                        },
                        onDragEnd = {
                            val shouldTrigger = abs(dragOffsetX) >= triggerOffsetPx
                            val endOffset = dragOffsetX
                            isDragging = false
                            dragOffsetX = 0f
                            if (shouldTrigger) {
                                if (endOffset < 0f) {
                                    playerViewModel.skipToNext()
                                } else {
                                    playerViewModel.skipToPrevious()
                                }
                            }
                        }
                    )
                }
                .align(Alignment.CenterVertically)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .align(Alignment.CenterVertically),
            ) {
                AcrylicFlipAsyncImage(
                    imageUrl = currentArtworkUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = "Cover art",
                    contentScale = ContentScale.Crop,
                    shape = RoundedCornerShape(8.dp),
                )
            }
            Spacer(
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .align(Alignment.CenterVertically)
            )
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = title,
                    style = SaltTheme.textStyles.main,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    maxLines = 1,
                )
                Text(
                    text = if (isInTriggerZone) {
                        targetSongName?.let { "松手播放：$it" } ?: "松手播放"
                    } else {
                        artistName
                    },
                    style = SaltTheme.textStyles.sub,
                    maxLines = 1,
                    color = if (isInTriggerZone) SaltTheme.colors.highlight else SaltTheme.colors.subText
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        // 保持原布局间距，控制区不参与拖拽
        Spacer(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .align(Alignment.CenterVertically)
            )

        // 将播放控制相关组件封装起来
        PlayerControls(
            modifier = Modifier.align(Alignment.CenterVertically),
            playerViewModel = playerViewModel,
            onPlaylistClick = onPlaylistClick
        )
    }
}

/**
 * 播放控制组件，将频繁更新的状态读取限制在此范围内
 */
@UnstableSaltUiApi
@ExperimentalMaterial3Api
@UnstableApi
@ExperimentalFoundationApi
@Composable
private fun PlayerControls(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel,
    onPlaylistClick: () -> Unit = {}
) {
    // 进度
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()

    // 播放状态
    val isPlaying by playerViewModel.isPlaying.collectAsState()

    Row(modifier = modifier) {
        // 播放按钮
        Box(
            modifier = Modifier
                .size(48.dp, 48.dp)
        ) {
            PlayPauseButton(
                modifier = Modifier.align(Alignment.Center),
                isPlaying = isPlaying,
                onClick = {
                    playerViewModel.togglePlayPause()
                }
            )
            CircularProgressIndicator(
                progress = {
                    if (duration > 0) {
                        currentPosition.toFloat() / duration.toFloat()
                    } else {
                        0f
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center),
                color = SaltTheme.colors.highlight,
                strokeWidth = 3.dp,
                trackColor = SaltTheme.colors.subBackground,
            )
        }
        // 播放列表按钮
        Box(
            modifier = Modifier
                .size(48.dp, 48.dp)
        ) {
            IconButton(
                modifier = Modifier.align(Alignment.Center),
                onClick = onPlaylistClick
            ) {
                Icon(
                    painter = rememberVectorPainter(TablerIcons.Playlist),
                    contentDescription = "播放列表",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }

    // 点击时的缩放动画
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(100),
        label = "press_scale"
    )

    // 状态变化时的缩放动画
    val stateScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.1f else 1f,
        animationSpec = tween(300),
        label = "state_scale"
    )

    IconButton(
        modifier = modifier
            .size(48.dp)
            .padding(8.dp),
        onClick = onClick,
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> isPressed = true
                            is PressInteraction.Release -> isPressed = false
                            is PressInteraction.Cancel -> isPressed = false
                        }
                    }
                }
            }
    ) {
        Icon(
            painter = if (isPlaying) {
                rememberVectorPainter(TablerIcons.PlayerPause)
            } else {
                rememberVectorPainter(TablerIcons.PlayerPlay)
            },
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .scale(pressScale * stateScale), // 组合两个缩放效果
        )
    }
}
