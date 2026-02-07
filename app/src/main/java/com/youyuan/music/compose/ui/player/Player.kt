package com.youyuan.music.compose.ui.player

import android.annotation.SuppressLint
import android.app.Activity
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
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
import com.youyuan.music.compose.ui.uicomponent.ResizableIconButton
import com.youyuan.music.compose.ui.uicomponent.flowing.FlowingLightBackground
import com.youyuan.music.compose.ui.uicomponent.sheet.AudioQualitySheetDialog
import com.youyuan.music.compose.ui.uicomponent.sheet.MusicFXSheetDialog
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionInfo
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionSheetDialog
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionArtist
import com.youyuan.music.compose.ui.utils.LocalPlayerUIColor
import com.youyuan.music.compose.ui.utils.PlayerForegroundColorLight
import com.youyuan.music.compose.ui.utils.getPlayerUIColor
import com.youyuan.music.compose.ui.screens.ScreenRoute
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel
import com.youyuan.music.compose.utils.Logger
import com.youyuan.music.compose.utils.SystemMediaDialogUtils
import com.youyuan.music.compose.utils.formatTimeString
import compose.icons.FeatherIcons
import compose.icons.TablerIcons
import compose.icons.feathericons.SkipBack
import compose.icons.feathericons.SkipForward
import compose.icons.tablericons.Cast
import compose.icons.tablericons.PlayerPause
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.Playlist
import compose.icons.tablericons.Share
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider

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
    val currentSong = playerViewModel.currentSong.collectAsState().value
    val currentSongId = currentSong?.id
    val isFavorite by playerViewModel.isCurrentSongLiked.collectAsState(initial = false)
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

    // 进度
    val currentPosition = playerViewModel.currentPosition.collectAsState().value
    val duration = playerViewModel.duration.collectAsState().value

    // 设置数据存储
    val settingsDataStore = remember { SettingsDataStore(context) }
    // 播放器进度条波形动画设置项
    val isPlayerSquigglyWaveEnabled by settingsDataStore.isPlayerSquigglyWaveEnabled.collectAsState(initial = true)
    val playerCoverType by settingsDataStore.playerCoverType.collectAsState(initial = PlayerCoverType.DEFAULT.ordinal)

    // 播放状态
    val isPlaying = playerViewModel.isPlaying.collectAsState().value

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
        val songId = currentSongId
        if (songId != null) {
            SongActionSheetDialog(
                playerViewModel = playerViewModel,
                song = SongActionInfo(
                    songId = songId,
                    albumId = currentSong.al?.id,
                    title = currentSong.name,
                    artist = artistName,
                    album = currentSong.al?.name,
                    artworkUrl = currentArtworkUrl,
                    artists = currentSong.ar.orEmpty().map { SongActionArtist(artistId = it.id, name = it.name) },
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
    var playerUIColor by remember {
        mutableStateOf(getPlayerUIColor(isSystemInDarkTheme))
    }

    // 封面是否加载完成
    var coverLoaded by remember {
        mutableStateOf(false)
    }

    // 进度条位置
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }

    // 评论数量小数字（展开态切歌时拉取）
    val commentCount = playerViewModel.commentCount.collectAsState().value

    LaunchedEffect(state.isExpanded, currentSongId) {
        if (currentSongId != null) {
            playerViewModel.clearCommentCount()
            playerViewModel.refreshCommentCount(currentSongId)
        }
    }

    // 状态栏颜色控制和全局前景色控制
    LaunchedEffect(state.isExpanded, currentSong, coverLoaded, isSystemInDarkTheme) {
        // 更新播放器UI颜色逻辑
        playerUIColor = when {
            // 当没有播放内容或封面未加载完成时，根据系统主题决定颜色
            currentSong == null || !coverLoaded -> getPlayerUIColor(isSystemInDarkTheme)
            // 当有播放内容且封面加载完成时，使用浅色前景
            else -> PlayerForegroundColorLight
        }

        Logger.debug("BottomSheetPlayer",
            "状态栏颜色控制: isExpanded=${state.isExpanded}," +
                    " currentPlaying=${currentSong?.name ?: "null"}," +
                    " coverLoaded=$coverLoaded," +
                    " isSystemInDarkTheme=$isSystemInDarkTheme")

        val window: Window = context.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (state.isExpanded) {
            // 展开时的状态栏逻辑，与 playerUIColor 逻辑保持一致
            when {
                // 当没有播放内容或封面未加载完成时，根据系统主题决定状态栏颜色
                currentSong == null || !coverLoaded -> {
                    withContext(Dispatchers.Main) {
                        insetsController.isAppearanceLightStatusBars = !isSystemInDarkTheme
                    }
                }
                // 当有播放内容且封面加载完成时，使用浅色状态栏（因为背景是流光溢彩效果，较暗）
                else -> {
                    withContext(Dispatchers.Main) {
                        insetsController.isAppearanceLightStatusBars = false
                    }
                }
            }
        } else {
            // 折叠时始终根据系统主题决定状态栏颜色
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
                modifier = modifier,
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
            // 流光溢彩背景
            FlowingLightBackground(
                isPlaying = isPlaying,
                imageUrl = currentArtworkUrl,
                modifier = Modifier.fillMaxSize(),
                onImageLoadResult = { result ->
                    coverLoaded = result
                },
            )
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
                                    playerCoverType = playerCoverType,
                                    title = title,
                                    artistName = artistName,
                                    isFavorite = isFavorite,
                                    currentSongId = currentSongId,
                                    commentCount = commentCount,
                                    sliderPosition = sliderPosition,
                                    currentPosition = currentPosition,
                                    duration = duration,
                                    isPlayerSquigglyWaveEnabled = isPlayerSquigglyWaveEnabled,
                                    repeatMode = repeatMode,
                                    shuffleModeEnabled = shuffleModeEnabled,
                                    onSliderPositionChange = { sliderPosition = it },
                                    onSeekTo = { playerViewModel.seekTo(it) },
                                    onToggleFavorite = { playerViewModel.toggleCurrentSongFavorite() },
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
    playerCoverType: Int,
    title: String,
    artistName: String,
    isFavorite: Boolean,
    currentSongId: Long?,
    commentCount: Int,
    sliderPosition: Long?,
    currentPosition: Long,
    duration: Long,
    isPlayerSquigglyWaveEnabled: Boolean,
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
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .systemBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { state.collapseSoft() },
                modifier = modifier.padding(4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_collapse),
                    contentDescription = "收起抽屉",
                    tint = LocalPlayerUIColor.current
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row {
                IconButton(
                    onClick = {
                        SystemMediaDialogUtils.getInstance(context).showSystemMediaDialog()
                    },
                    modifier = modifier.padding(4.dp)
                ) {
                    Icon(
                        painter = rememberVectorPainter(TablerIcons.Cast),
                        contentDescription = "投送",
                        tint = LocalPlayerUIColor.current
                    )
                }
                IconButton(
                    onClick = {},
                    modifier = modifier.padding(4.dp)
                ) {
                    Icon(
                        painter = rememberVectorPainter(TablerIcons.Share),
                        contentDescription = "分享",
                        tint = LocalPlayerUIColor.current
                    )
                }
            }
        }

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
            isPlayerSquigglyWaveEnabled = isPlayerSquigglyWaveEnabled,
            isPlaying = isPlaying,
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

@Composable
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@UnstableSaltUiApi
@UnstableApi
private fun PlayerControlsSection(
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
    isPlayerSquigglyWaveEnabled: Boolean,
    isPlaying: Boolean,
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
            Row(Modifier
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
            Row(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) {
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
                            val songId = currentSongId
                            if (songId != null) {
                                navController.navigate(ScreenRoute.SongComments.createRoute(songId))
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
            isPlayerSquigglyWaveEnabled = isPlayerSquigglyWaveEnabled,
            onSliderPositionChange = { onSliderPositionChange(it) } ,
            onSeekTo = { onSeekTo(it) },
            isPlaying = isPlaying
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val loopIconRes = when {
                shuffleModeEnabled -> R.drawable.ic_shuffle_one
                repeatMode == Player.REPEAT_MODE_ONE -> R.drawable.ic_play_once
                else -> R.drawable.ic_play_cycle
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
                icon = FeatherIcons.SkipBack,
                color = LocalPlayerUIColor.current,
                modifier = Modifier
                    .size(34.dp)
                    .padding(4.dp),
                onClick = onSkipToPrevious,
            )

            ResizableIconButton(
                icon = if (isPlaying) TablerIcons.PlayerPause else TablerIcons.PlayerPlay,
                color = LocalPlayerUIColor.current,
                modifier = Modifier
                    .size(42.dp)
                    .padding(4.dp),
                onClick = onTogglePlayPause,
            )

            ResizableIconButton(
                icon = FeatherIcons.SkipForward,
                color = LocalPlayerUIColor.current,
                modifier = Modifier
                    .size(34.dp)
                    .padding(4.dp),
                onClick = onSkipToNext,
            )

            ResizableIconButton(
                icon = TablerIcons.Playlist,
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
    isPlayerSquigglyWaveEnabled: Boolean,
    onSliderPositionChange: (Long?) -> Unit,
    onSeekTo: (Long) -> Unit,
    isPlaying: Boolean,
) {
    SquigglySlider(
        value = (sliderPosition ?: currentPosition).toFloat(),
        valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
        onValueChange = { value ->
            onSliderPositionChange(value.toLong())
        },
        onValueChangeFinished = {
            sliderPosition?.let { onSeekTo(it) }
            onSliderPositionChange(null)
        },
        modifier = Modifier.fillMaxWidth(),
        squigglesSpec =
            SquigglySlider.SquigglesSpec(
                amplitude = if (isPlayerSquigglyWaveEnabled) {
                    if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp
                } else {
                    0.dp
                },
                strokeWidth = 3.dp,
                wavelength = (24.dp).coerceAtLeast(16.dp),
            ),
        colors = SliderDefaults.colors(
            thumbColor = SaltTheme.colors.highlight,
            activeTrackColor = SaltTheme.colors.highlight,
            inactiveTrackColor = SaltTheme.colors.stroke,
        )
    )

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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
