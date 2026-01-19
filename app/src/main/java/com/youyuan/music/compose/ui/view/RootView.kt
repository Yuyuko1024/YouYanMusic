package com.youyuan.music.compose.ui.view

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moriafly.salt.ui.BottomBar
import com.moriafly.salt.ui.BottomBarItem
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.dialog.InputDialog
import com.youyuan.music.compose.R
import com.youyuan.music.compose.constants.AppBarHeight
import com.youyuan.music.compose.constants.MiniPlayerHeight
import com.youyuan.music.compose.constants.NavigationBarAnimationSpec
import com.youyuan.music.compose.constants.NavigationBarHeight
import com.youyuan.music.compose.ui.player.BottomSheetPlayer
import com.youyuan.music.compose.ui.player.COLLAPSED_ANCHOR
import com.youyuan.music.compose.ui.player.rememberBottomSheetState
import com.youyuan.music.compose.ui.screens.ScreenRoute
import com.youyuan.music.compose.ui.screens.navigationBuilder
import com.youyuan.music.compose.ui.uicomponent.TopAppBar
import com.youyuan.music.compose.ui.uicomponent.TopAppBarType
import com.youyuan.music.compose.ui.utils.LocalPlayerAwareWindowInsets
import com.youyuan.music.compose.ui.utils.appBarScrollBehavior
import com.youyuan.music.compose.ui.utils.canGoBack
import com.youyuan.music.compose.ui.viewmodel.AppConfigViewModel
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel
import com.youyuan.music.compose.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.launch
import com.youyuan.music.compose.ui.uicomponent.AppDrawer
import com.youyuan.music.compose.ui.viewmodel.ProfileViewModel

@OptIn(UnstableApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
@UnstableSaltUiApi
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
fun RootView(
    context: Activity,
    modifier: Modifier = Modifier
) {

    val scope = rememberCoroutineScope()
    val appConfigViewModel: AppConfigViewModel = hiltViewModel()
    val savedApiUrl by appConfigViewModel.savedApiUrl.collectAsState()
    val effectiveApiUrl by appConfigViewModel.effectiveApiUrl.collectAsState()

    // 首次安装初始化：若用户未保存过 API Endpoint，则强制填写（不可取消）
    // savedApiUrl == null 表示仍在从 DataStore 加载，避免冷启动闪现弹窗。
    if (savedApiUrl != null && savedApiUrl!!.isBlank()) {
        var apiUrlInput by rememberSaveable(effectiveApiUrl) { mutableStateOf(effectiveApiUrl) }

        InputDialog(
            onDismissRequest = {
                // negative：退出 App（对话框不可被外部取消）
                context.finishAffinity()
            },
            onConfirm = {
                scope.launch {
                    try {
                        appConfigViewModel.persistAndApplyApiUrl(apiUrlInput)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            e.message ?: context.getString(R.string.settings_api_endpoint_invalid),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = stringResource(R.string.settings_api_endpoint_required_title),
            text = apiUrlInput,
            onChange = { apiUrlInput = it },
            hint = stringResource(R.string.settings_api_endpoint_hint),
            cancelText = stringResource(R.string.exit_app),
            confirmText = stringResource(R.string.confirm)
        )
        return
    }

    // 使用 Hilt 注入的 ViewModel（确保 API Endpoint 已就绪）
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val searchViewModel: SearchViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    val playerError by playerViewModel.error.collectAsState()

    LaunchedEffect(playerError) {
        val message = playerError ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        playerViewModel.consumeError()
    }

    BoxWithConstraints (
        modifier = modifier
            .background(SaltTheme.colors.background)
            .fillMaxSize()
    ) {

        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()

        val navigationItems = remember { ScreenRoute.MainScreens }

        val density = LocalDensity.current
        val windowsInsets = WindowInsets.systemBars
        val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }

        var active by rememberSaveable { mutableStateOf(false) }

        val scope = rememberCoroutineScope()

        val shouldShowNavigationBar =
            remember(navBackStackEntry, active) {
                navBackStackEntry?.destination?.route == null ||
                        navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } &&
                        !active
            }

        val navigationBarHeight by animateDpAsState(
            targetValue = if (shouldShowNavigationBar) NavigationBarHeight else 0.dp,
            animationSpec = NavigationBarAnimationSpec,
            label = "",
        )

        val topAppBarScrollBehavior =
            appBarScrollBehavior(
                canScroll = {
                    // HACK: 临时设置，后续判断是否可以滚动
                    true
                }
            )

        // 计算播放器折叠状态下的边界高度
        val animatedCollapsedBound by animateDpAsState(
            targetValue = bottomInset + (if (shouldShowNavigationBar) NavigationBarHeight else 0.dp) + MiniPlayerHeight,
            animationSpec = NavigationBarAnimationSpec,
            label = "collapsedBound"
        )

        val playerBottomSheetState =
            rememberBottomSheetState(
                collapsedBound = animatedCollapsedBound,
                expandedBound = maxHeight,
                initialAnchor = COLLAPSED_ANCHOR
            )

        // 智能的WindowInsets计算
        val playerAwareWindowInsets = remember(bottomInset, shouldShowNavigationBar) {
            var bottom = bottomInset + MiniPlayerHeight
            if (shouldShowNavigationBar) bottom += NavigationBarHeight
            windowsInsets
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                .add(WindowInsets(top = AppBarHeight, bottom = bottom))
        }

        // 记录当前的主屏幕路由，默认为 Explore
        val currentMainScreenRoute = remember { mutableStateOf(ScreenRoute.Explore.route) }
        val currentRoute = navBackStackEntry?.destination?.route

        // 当路由变化时，如果新路由是主屏幕之一，则更新状态
        if (ScreenRoute.MainScreens.any { it.route == currentRoute }) {
            currentRoute?.let { currentMainScreenRoute.value = it }
        }

        // 判断是否为二级页面
        val isSecondaryScreen = remember(currentRoute) {
            currentRoute != null && !ScreenRoute.MainScreens.any { it.route == currentRoute }
        }

        // 根据当前路由设置标题
        val title = when {
            currentRoute == ScreenRoute.Explore.route -> stringResource(R.string.title_explore)
            currentRoute == ScreenRoute.Profile.route -> stringResource(R.string.title_profile)
            currentRoute == ScreenRoute.Settings.route -> stringResource(R.string.drawer_settings)
            currentRoute == ScreenRoute.LoginPage.route -> stringResource(R.string.title_login)
            currentRoute == ScreenRoute.RegisterPage.route -> stringResource(R.string.title_register)
            currentRoute?.startsWith("comments/") == true -> stringResource(R.string.comments_title)
            currentRoute?.startsWith("playlist/") == true -> stringResource(R.string.title_playlist)
            currentRoute == ScreenRoute.InAppWebView.route -> stringResource(R.string.title_webview)
            currentRoute?.startsWith("album/") == true -> stringResource(R.string.title_album)
            else -> stringResource(R.string.app_name)
        }

        CompositionLocalProvider(
            // 提供智能WindowInsets给所有子Screen
            LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
        ) {
            val drawerState = rememberDrawerState(DrawerValue.Closed)

            // 侧边栏打开时，先让返回键优先响应关闭侧边栏
            BackHandler(enabled = drawerState.isOpen) {
                scope.launch { drawerState.close() }
            }

            DismissibleNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    AppDrawer(
                        drawerState = drawerState,
                        scope = scope,
                        navController = navController,
                        currentMainScreenRoute = currentMainScreenRoute
                    )
                },
                gesturesEnabled = false
            ) {
                // 获取当前 TopAppBar 类型
                val currentTopAppBarType = when {
                    currentRoute == ScreenRoute.Search.route -> TopAppBarType.SEARCH
                    isSecondaryScreen -> TopAppBarType.SECONDARY
                    else -> TopAppBarType.MAIN
                }

                TopAppBar(
                    modifier = modifier.systemBarsPadding(),
                    title = title,
                    titleBarType = currentTopAppBarType,
                    searchViewModel = searchViewModel,
                    onBackClick = {
                        if (navController.canGoBack) {
                            navController.popBackStack()
                        }
                    },
                    onDrawerClick = {
                        scope.launch {
                            if (drawerState.isClosed) {
                                drawerState.open()
                            } else {
                                drawerState.close()
                            }
                        }
                    },
                    onSearchClick = {
                        navController.navigate(
                            ScreenRoute.Search.route
                        )
                    }
                )
                NavHost(
                    navController = navController,
                    startDestination = ScreenRoute.Explore.route,
                    enterTransition = {
                        fadeIn(animationSpec = tween(500))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(500))
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(500))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(500))
                    },
                    modifier = Modifier
                        .nestedScroll(
                            topAppBarScrollBehavior.nestedScrollConnection
                        )
                        // 为NavHost添加智能边距
                        .windowInsetsPadding(playerAwareWindowInsets)
                ) {
                    navigationBuilder(
                        context = context,
                        navController = navController,
                        scrollBehavior = topAppBarScrollBehavior,
                        searchViewModel = searchViewModel,
                        playerViewModel = playerViewModel,
                        profileViewModel = profileViewModel,
                    )
                }

                // 监听导航变化以折叠播放器
                LaunchedEffect(navBackStackEntry) {
                    navBackStackEntry?.let {
                        if (playerBottomSheetState.isExpanded) {
                            playerBottomSheetState.collapseSoft()
                        }
                    }
                }
            }

            BottomSheetPlayer(
                state = playerBottomSheetState,
                navController = navController,
                playerViewModel = playerViewModel,
                context = context
            )

            MainBottomBar(
                navController = navController,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset {
                        if (navigationBarHeight == 0.dp) {
                            IntOffset(
                                x = 0,
                                y = (bottomInset + NavigationBarHeight).roundToPx(),
                            )
                        } else {
                            val slideOffset =
                                (bottomInset + NavigationBarHeight) *
                                        playerBottomSheetState.progress.coerceIn(
                                            0f,
                                            1f,
                                        )
                            val hideOffset =
                                (bottomInset + NavigationBarHeight) * (1 - navigationBarHeight / NavigationBarHeight)
                            IntOffset(
                                x = 0,
                                y = (slideOffset + hideOffset).roundToPx(),
                            )
                        }
                    }
                    .navigationBarsPadding()
            )

        }
    }
}

@Composable
@UnstableSaltUiApi
fun MainBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    BottomBar(
        backgroundColor = Color.Transparent,
        modifier = modifier
    ) {
        ScreenRoute.MainScreens.forEach { screen ->
            BottomBarItem(
                text = when (screen) {
                    ScreenRoute.Explore -> stringResource(R.string.title_explore)
                    ScreenRoute.Profile -> stringResource(R.string.title_profile)
                    else -> ""
                },
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                state = currentRoute == screen.route,
                painter = painterResource(
                    id = when (screen) {
                        ScreenRoute.Explore -> R.drawable.ic_explore
                        ScreenRoute.Profile -> R.drawable.ic_account_circle
                        else -> R.drawable.ic_explore
                    }
                ),
            )
        }
    }
}
