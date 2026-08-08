package com.youyuan.music.compose.ui.player

import android.annotation.SuppressLint
import android.app.Activity
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Surface
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.ext.safeMainPadding
import com.moriafly.salt.ui.pager.HorizontalPager
import com.moriafly.salt.ui.pager.PagerState
import com.moriafly.salt.ui.pager.VerticalPager
import com.moriafly.salt.ui.pager.rememberPagerState
import com.youyuan.music.compose.R
import com.youyuan.music.compose.constants.PlayerHorizontalPadding
import com.youyuan.music.compose.pref.AudioQualityLevel
import com.youyuan.music.compose.pref.PlayerCoverType
import com.youyuan.music.compose.pref.SettingsDataStore
import com.youyuan.music.compose.ui.screens.ScreenRoute
import com.youyuan.music.compose.ui.uicomponent.ResizableIconButton
import com.youyuan.music.compose.ui.uicomponent.sheet.AudioQualitySheetDialog
import com.youyuan.music.compose.ui.uicomponent.sheet.MusicFXSheetDialog
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionArtist
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionInfo
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionSheetDialog
import com.youyuan.music.compose.ui.utils.AdaptiveLayoutMode
import com.youyuan.music.compose.ui.utils.LocalPlayerUIColor
import com.youyuan.music.compose.ui.utils.getPlayerUIColor
import com.youyuan.music.compose.ui.utils.rememberAdaptiveLayoutMode
import com.youyuan.music.compose.ui.utils.rememberPlayerUIColor
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel
import com.youyuan.music.compose.utils.Logger
import com.youyuan.music.compose.utils.SystemMediaDialogUtils
import com.youyuan.music.compose.utils.formatTimeString
import compose.icons.TablerIcons
import compose.icons.tablericons.Cast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@UnstableSaltUiApi
@UnstableApi
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    context: Activity,
    modifier: Modifier = Modifier
) {

    // 协程作用域
    val coroutineScope = rememberCoroutineScope()

    // 当前Song对象
    val currentSong = playerViewModel.currentSongItem.collectAsState().value
    val currentSongId = currentSong?.id
    val isFavorite by playerViewModel.isCurrentSongLiked.collectAsState(initial = false)
    // 封面
    val currentArtworkUrl = playerViewModel.currentAlbumArtUrl.collectAsState().value

    // 标题
    fun List<String>?.toDisplayText(): String? =
        this
            .orEmpty()
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(" / ")
            .takeIf { it.isNotBlank() }

    val baseTitle = currentSong?.name ?: stringResource(R.string.unknown_song)
    val aliasText = currentSong?.alias.toDisplayText()
    val title = if (aliasText != null) "$baseTitle（$aliasText）" else baseTitle
    // 艺术家
    val artistName = playerViewModel.currentArtistNames.collectAsState().value.orEmpty()

    // 进度
    val currentPosition = playerViewModel.currentPosition.collectAsStateWithLifecycle().value
    val duration = playerViewModel.duration.collectAsStateWithLifecycle().value

    // 设置数据存储
    val settingsDataStore = remember { SettingsDataStore(context) }
    val playerCoverType by settingsDataStore.playerCoverType.collectAsState(initial = PlayerCoverType.DEFAULT.ordinal)

    // 播放状态
    val isPlaying = playerViewModel.isPlaying.collectAsState().value
    // 缓冲状态
    val isBuffering = playerViewModel.isBuffering.collectAsState().value

    // 循环/随机模式
    val repeatMode = playerViewModel.repeatMode.collectAsState().value
    val shuffleModeEnabled = playerViewModel.shuffleModeEnabled.collectAsState().value

    // 系统主题
    val isSystemInDarkTheme = isSystemInDarkTheme()

    // Pager状态
    val horizontalPagerState = rememberPagerState(
        pageCount = { 3 },
        initialPage = 1
    )
    val verticalPagerState = rememberPagerState(
        pageCount = { 2 },
        initialPage = 0
    )

    // 均衡器对话框显示控制
    var showEqualizerDialog by remember { mutableStateOf(false) }

    // 音质对话框显示控制
    var showAudioQualityDialog by remember { mutableStateOf(false) }

    // 歌曲操作对话框显示控制
    var showSongActionDialog by remember { mutableStateOf(false) }

    // 显示均衡器对话框
    if (showEqualizerDialog) {
        MusicFXSheetDialog(
            playerViewModel = playerViewModel,
            onDismissRequest = {
                showEqualizerDialog = false
            }
        )
    }

    // 显示音质对话框
    if (showAudioQualityDialog) {
        AudioQualitySheetDialog(
            playerViewModel = playerViewModel,
            onDismissRequest = {
                showAudioQualityDialog = false
            }
        )
    }

    if (showSongActionDialog) {
        if (currentSongId != null) {
            SongActionSheetDialog(
                playerViewModel = playerViewModel,
                song = SongActionInfo(
                    songId = currentSongId,
                    albumId = currentSong?.album?.id?.takeIf { it > 0 },
                    title = currentSong?.name,
                    artist = artistName,
                    album = currentSong?.album?.name,
                    artworkUrl = currentArtworkUrl,
                    artists = currentSong?.artists.orEmpty()
                        .map { SongActionArtist(artistId = it.id, name = it.name) },
                ),
                navController = navController,
                onDismissRequest = { showSongActionDialog = false },
            )
        } else {
            // 没有歌曲就直接关闭
            showSongActionDialog = false
        }
    }

    // 播放器UI部分前景染色
    val playerUIColor by remember {
        mutableStateOf(getPlayerUIColor(true))
    }

    // 进度条位置
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }

    // 评论数量小数字（展开态切歌时拉取）
    val commentCount = playerViewModel.commentCount.collectAsState().value

    LaunchedEffect(state.isExpanded, currentSongId) {
        if (currentSongId != null) {
            playerViewModel.clearComments()
            playerViewModel.refreshComments(currentSongId)
        }
    }

    // 状态栏颜色控制和全局前景色控制
    LaunchedEffect(state.isExpanded, currentSong, isSystemInDarkTheme) {
        Logger.debug(
            "BottomSheetPlayer",
            "状态栏颜色控制: isExpanded=${state.isExpanded}," +
                    " currentPlaying=${currentSong?.name ?: "null"}," +
                    " isSystemInDarkTheme=$isSystemInDarkTheme"
        )

        val window: Window = context.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (state.isExpanded) {
            withContext(Dispatchers.Main) {
                insetsController.isAppearanceLightStatusBars = false
            }
        }
        else {
            withContext(Dispatchers.Main) {
                insetsController.isAppearanceLightStatusBars = !isSystemInDarkTheme
            }
        }
    }

    // 处理返回键逻辑
    BackHandler(enabled = !state.isCollapsed && state.progress > 0.1f) {
        if (verticalPagerState.currentPage == 0) {
            // 如果是默认的主要视图页，直接折叠 BottomSheet
            state.collapseSoft()
        } else {
            // 否则回到主要视图页
            coroutineScope.launch {
                verticalPagerState.animateScrollToPage(0)
            }
        }
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        collapsedContent = {
            MiniPlayer(
                modifier = modifier.fillMaxWidth(),
                context = context,
                playerViewModel = playerViewModel,
                onPlaylistClick = {
                    // 先展开 BottomSheet
                    state.expandSoft()
                    // 然后跳转到播放列表页面（第1页，index为0）
                    coroutineScope.launch {
                        verticalPagerState.animateScrollToPage(1)
                    }
                }
            )
        },
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // 背景艺术图高斯模糊
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentArtworkUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = currentSong?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(18.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                )
            }
        }
    ) {
        CompositionLocalProvider(
            LocalPlayerUIColor provides playerUIColor
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                // 视图根布局
                Box(
                    modifier = modifier.safeMainPadding()
                ) {
                    // 根视图的分页
                    VerticalPager(
                        state = verticalPagerState,
                        beyondViewportPageCount = 1,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        when (page) {
                            0 -> {
                                ExpandedPlayerMainPage(
                                    modifier = modifier,
                                    state = state,
                                    navController = navController,
                                    playerViewModel = playerViewModel,
                                    context = context,
                                    coroutineScope = coroutineScope,
                                    horizontalPagerState = horizontalPagerState,
                                    verticalPagerState = verticalPagerState,
                                    currentArtworkUrl = currentArtworkUrl,
                                    isPlaying = isPlaying,
                                    isBuffering = isBuffering,
                                    playerCoverType = playerCoverType,
                                    title = title,
                                    artistName = artistName,
                                    isFavorite = isFavorite,
                                    currentSongId = currentSongId,
                                    commentCount = commentCount,
                                    sliderPosition = sliderPosition,
                                    currentPosition = currentPosition,
                                    duration = duration,
                                    repeatMode = repeatMode,
                                    shuffleModeEnabled = shuffleModeEnabled,
                                    onSliderPositionChange = { sliderPosition = it },
                                    onSeekTo = { playerViewModel.seekTo(it) },
                                    onToggleFavorite = { playerViewModel.toggleLike() },
                                    onToggleLoopMode = { playerViewModel.toggleLoopMode() },
                                    onSkipToPrevious = { playerViewModel.skipToPrevious() },
                                    onTogglePlayPause = { playerViewModel.togglePlayPause() },
                                    onSkipToNext = { playerViewModel.skipToNext() },
                                    onShowEqualizerDialog = { showEqualizerDialog = true },
                                    onShowAudioQualityDialog = { showAudioQualityDialog = true },
                                    onShowSongActionDialog = { showSongActionDialog = true },
                                )
                            }

                            1 -> {
                                PlaylistPager(
                                    playerViewModel = playerViewModel,
                                    onCollapseTextClick = {
                                        coroutineScope.launch {
                                            verticalPagerState.animateScrollToPage(0)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@UnstableSaltUiApi
@UnstableApi
private fun ExpandedPlayerMainPage(
    modifier: Modifier,
    state: BottomSheetState,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    context: Activity,
    coroutineScope: CoroutineScope,
    horizontalPagerState: PagerState,
    verticalPagerState: PagerState,
    currentArtworkUrl: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    playerCoverType: Int,
    title: String,
    artistName: String,
    isFavorite: Boolean,
    currentSongId: Long?,
    commentCount: Int,
    sliderPosition: Long?,
    currentPosition: Long,
    duration: Long,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    onSliderPositionChange: (Long?) -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLoopMode: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipToNext: () -> Unit,
    onShowEqualizerDialog: () -> Unit,
    onShowAudioQualityDialog: () -> Unit,
    onShowSongActionDialog: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val adaptiveLayoutMode = rememberAdaptiveLayoutMode(maxWidth = maxWidth)
        val isTabletLandscape = adaptiveLayoutMode == AdaptiveLayoutMode.TabletLandscape
        val horizontalGap by animateDpAsState(
            targetValue = if (isTabletLandscape) 20.dp else 0.dp,
            animationSpec = tween(280),
            label = "playerLandscapeGap"
        )

        Column {
            if (!isTabletLandscape) {
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .systemBarsPadding()
                        .padding(horizontal = PlayerHorizontalPadding, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = title,
                            style = SaltTheme.textStyles.main,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .basicMarquee(iterations = Int.MAX_VALUE),
                            maxLines = 1,
                            color = LocalPlayerUIColor.current
                        )
                        Text(
                            text = artistName,
                            style = SaltTheme.textStyles.sub,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            maxLines = 1,
                            color = LocalPlayerUIColor.current
                        )
                    }
                    Icon(
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable(onClick = {
                                SystemMediaDialogUtils.getInstance(context).showSystemMediaDialog()
                            }),
                        painter = rememberVectorPainter(TablerIcons.Cast),
                        contentDescription = "投送",
                        tint = LocalPlayerUIColor.current
                    )
                }
            }

            if (isTabletLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = PlayerHorizontalPadding)
                ) {
                    HorizontalPager(
                        state = horizontalPagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .sizeIn(maxHeight = 640.dp, maxWidth = 640.dp),
                        beyondViewportPageCount = 1
                    ) { page ->
                        when (page) {
                            0 -> Box(Modifier.fillMaxSize())
                            1 -> CoverPager(
                                artworkUrl = currentArtworkUrl,
                                isPlaying = isPlaying,
                                coverType = playerCoverType
                            )

                            2 -> LyricsPager(playerViewModel = playerViewModel)
                        }
                    }

                    Spacer(modifier = Modifier.size(horizontalGap))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayerControlsSection(
                            context = context,
                            isTabletLandscape = isTabletLandscape,
                            horizontalPagerState = horizontalPagerState,
                            verticalPagerState = verticalPagerState,
                            coroutineScope = coroutineScope,
                            navController = navController,
                            playerViewModel = playerViewModel,
                            title = title,
                            artistName = artistName,
                            isFavorite = isFavorite,
                            currentSongId = currentSongId,
                            commentCount = commentCount,
                            sliderPosition = sliderPosition,
                            currentPosition = currentPosition,
                            duration = duration,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            repeatMode = repeatMode,
                            shuffleModeEnabled = shuffleModeEnabled,
                            onSliderPositionChange = onSliderPositionChange,
                            onSeekTo = onSeekTo,
                            onToggleFavorite = onToggleFavorite,
                            onToggleLoopMode = onToggleLoopMode,
                            onSkipToPrevious = onSkipToPrevious,
                            onTogglePlayPause = onTogglePlayPause,
                            onSkipToNext = onSkipToNext,
                            onShowEqualizerDialog = onShowEqualizerDialog,
                            onShowAudioQualityDialog = onShowAudioQualityDialog,
                            onShowSongActionDialog = onShowSongActionDialog,
                        )
                    }
                }
            } else {
                HorizontalPager(
                    state = horizontalPagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .sizeIn(maxHeight = 600.dp, maxWidth = 600.dp)
                        .align(Alignment.CenterHorizontally),
                    beyondViewportPageCount = 1
                ) { page ->
                    when (page) {
                        0 -> Box(Modifier.fillMaxSize())
                        1 -> CoverPager(
                            artworkUrl = currentArtworkUrl,
                            isPlaying = isPlaying,
                            coverType = playerCoverType
                        )

                        2 -> LyricsPager(playerViewModel = playerViewModel)
                    }
                }

                PlayerControlsSection(
                    context = context,
                    isTabletLandscape = isTabletLandscape,
                    horizontalPagerState = horizontalPagerState,
                    verticalPagerState = verticalPagerState,
                    coroutineScope = coroutineScope,
                    navController = navController,
                    playerViewModel = playerViewModel,
                    title = title,
                    artistName = artistName,
                    isFavorite = isFavorite,
                    currentSongId = currentSongId,
                    commentCount = commentCount,
                    sliderPosition = sliderPosition,
                    currentPosition = currentPosition,
                    duration = duration,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    repeatMode = repeatMode,
                    shuffleModeEnabled = shuffleModeEnabled,
                    onSliderPositionChange = onSliderPositionChange,
                    onSeekTo = onSeekTo,
                    onToggleFavorite = onToggleFavorite,
                    onToggleLoopMode = onToggleLoopMode,
                    onSkipToPrevious = onSkipToPrevious,
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipToNext = onSkipToNext,
                    onShowEqualizerDialog = onShowEqualizerDialog,
                    onShowAudioQualityDialog = onShowAudioQualityDialog,
                    onShowSongActionDialog = onShowSongActionDialog,
                )
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@UnstableSaltUiApi
@UnstableApi
private fun PlayerControlsSection(
    context: Activity,
    isTabletLandscape: Boolean,
    horizontalPagerState: PagerState,
    verticalPagerState: PagerState,
    coroutineScope: CoroutineScope,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    title: String,
    artistName: String,
    isFavorite: Boolean,
    currentSongId: Long?,
    commentCount: Int,
    sliderPosition: Long?,
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    onSliderPositionChange: (Long?) -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLoopMode: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipToNext: () -> Unit,
    onShowEqualizerDialog: () -> Unit,
    onShowAudioQualityDialog: () -> Unit,
    onShowSongActionDialog: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = PlayerHorizontalPadding, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
        ) {
            if (isTabletLandscape) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterVertically)
                        .weight(1f)
                ) {
                    AnimatedVisibility(
                        modifier = Modifier
                            .align(Alignment.CenterVertically),
                        visible = horizontalPagerState.currentPage != 2,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = title,
                                style = SaltTheme.textStyles.main,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .basicMarquee(iterations = Int.MAX_VALUE),
                                maxLines = 1,
                                color = LocalPlayerUIColor.current
                            )
                            Text(
                                text = artistName,
                                style = SaltTheme.textStyles.sub,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                maxLines = 1,
                                color = LocalPlayerUIColor.current
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) {
                if (isTabletLandscape) {
                    IconButton(
                        onClick = {
                            SystemMediaDialogUtils.getInstance(context).showSystemMediaDialog()
                        },
                        modifier = Modifier.padding(4.dp),
                    ) {
                        Icon(
                            painter = rememberVectorPainter(TablerIcons.Cast),
                            contentDescription = "投送",
                            tint = LocalPlayerUIColor.current
                        )
                    }
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        painter = if (isFavorite) {
                            painterResource(id = R.drawable.ic_favorite)
                        } else {
                            painterResource(id = R.drawable.ic_favorite_border)
                        },
                        contentDescription = "收藏",
                        tint = if (isFavorite) Color(0xFFE53935) else LocalPlayerUIColor.current
                    )
                }
                Box {
                    IconButton(
                        onClick = {
                            if (currentSongId != null) {
                                navController.navigate(
                                    ScreenRoute.SongComments.createRoute(
                                        currentSongId
                                    )
                                )
                            }
                        },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chat_bubble_count),
                            contentDescription = "评论",
                            tint = LocalPlayerUIColor.current,
                        )
                    }
                    if (commentCount > 0) {
                        Text(
                            text = if (commentCount > 99) "99+" else commentCount.toString(),
                            style = SaltTheme.textStyles.sub,
                            color = LocalPlayerUIColor.current,
                            modifier = Modifier
                                .padding(end = 4.dp, top = 4.dp)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }

        PlayerDataControlPanel(
            sliderPosition = sliderPosition,
            currentPosition = currentPosition,
            duration = duration,
            onSliderPositionChange = { onSliderPositionChange(it) },
            onSeekTo = { onSeekTo(it) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val loopIconRes = when {
                shuffleModeEnabled -> R.drawable.ic_shuffle_outline
                repeatMode == Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one_outline
                else -> R.drawable.ic_repeat_outline
            }

            ResizableIconButton(
                icon = loopIconRes,
                color = LocalPlayerUIColor.current,
                modifier = Modifier
                    .size(34.dp)
                    .padding(4.dp),
                onClick = onToggleLoopMode,
            )

            ResizableIconButton(
                icon = R.drawable.ic_skip_prev_filled,
                color = LocalPlayerUIColor.current,
                modifier = Modifier
                    .size(34.dp)
                    .padding(4.dp),
                onClick = onSkipToPrevious,
            )

            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(4.dp),
                    color = SaltTheme.colors.highlight
                )
            } else {
                ResizableIconButton(
                    icon = if (isPlaying) R.drawable.ic_pause_filled else R.drawable.ic_play_filled,
                    color = LocalPlayerUIColor.current,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(4.dp),
                    onClick = onTogglePlayPause,
                )
            }

            ResizableIconButton(
                icon = R.drawable.ic_skip_next_filled,
                color = LocalPlayerUIColor.current,
                modifier = Modifier
                    .size(34.dp)
                    .padding(4.dp),
                onClick = onSkipToNext,
            )

            ResizableIconButton(
                icon = R.drawable.ic_playlist,
                color = LocalPlayerUIColor.current,
                modifier = Modifier
                    .size(34.dp)
                    .padding(4.dp),
                onClick = {
                    coroutineScope.launch {
                        verticalPagerState.animateScrollToPage(1)
                    }
                },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onShowEqualizerDialog) {
                Icon(
                    painter = painterResource(R.drawable.ic_equalizer_24px),
                    contentDescription = "均衡器对话框",
                    tint = LocalPlayerUIColor.current,
                    modifier = Modifier.size(24.dp)
                )
            }

            val selectedLevelRaw = playerViewModel.selectedAudioQualityLevel.collectAsState().value
            val selectedLevel = AudioQualityLevel.fromLevel(selectedLevelRaw)
                ?: AudioQualityLevel.default()

            IconButton(
                onClick = onShowAudioQualityDialog,
                modifier = Modifier.sizeIn(minWidth = 72.dp, minHeight = 48.dp)
            ) {
                Text(
                    text = selectedLevel.displayName,
                    style = SaltTheme.textStyles.sub,
                    color = LocalPlayerUIColor.current,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onShowSongActionDialog) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_horiz_24px),
                    contentDescription = "媒体详细信息对话框按钮",
                    tint = LocalPlayerUIColor.current,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun PlayerDataControlPanel(
    sliderPosition: Long?,
    currentPosition: Long,
    duration: Long,
    onSliderPositionChange: (Long?) -> Unit,
    onSeekTo: (Long) -> Unit,
) {
    val sliderColors = SliderDefaults.colors(
        thumbColor = SaltTheme.colors.highlight,
        activeTrackColor = SaltTheme.colors.highlight,
        inactiveTrackColor = SaltTheme.colors.stroke,
    )

    Slider(
        value = (sliderPosition ?: currentPosition).toFloat(),
        valueRange =  0f..(if (duration > 0) duration.toFloat() else 1f),
        onValueChange = { value ->
            onSliderPositionChange(value.toLong())
        },
        onValueChangeFinished = {
            sliderPosition?.let { onSeekTo(it) }
            onSliderPositionChange(null)
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        colors = sliderColors,
        thumb = {
            // disable thumb
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(6.dp),
                thumbTrackGapSize = 1.dp,
                trackInsideCornerSize = 8.dp,
                trackCornerSize = 8.dp,
                drawStopIndicator = null,
                colors = sliderColors
            )
        }
    )

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Text(
            text = formatTimeString(sliderPosition ?: currentPosition),
            style = SaltTheme.textStyles.sub,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = LocalPlayerUIColor.current
        )

        Text(
            text = formatTimeString(duration),
            style = SaltTheme.textStyles.sub,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = LocalPlayerUIColor.current
        )
    }
}
