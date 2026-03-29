package com.youyuan.music.compose.ui.uicomponent

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemArrowType
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R
import com.youyuan.music.compose.ui.screens.ScreenRoute
import compose.icons.TablerIcons
import compose.icons.tablericons.Download
import compose.icons.tablericons.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// 抽取菜单项数据类
data class DrawerMenuItem(
    val title: String,
    val iconResId: Int? = null,
    val iconPainter: Painter? = null,
    val iconImageVector: ImageVector? = null,
    val route: String,
    val isMainScreen: Boolean = false
)

@ExperimentalMaterial3Api
@UnstableSaltUiApi
@Composable
fun AppDrawer(
    modifier: Modifier = Modifier,
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavController,
    currentMainScreenRoute: MutableState<String>,
    isTabletLandscape: Boolean = false,
) {

    val drawerHome = stringResource(R.string.drawer_home)
    val drawerSettings = stringResource(R.string.drawer_settings)
    val drawerDownloadTasks = stringResource(R.string.drawer_download_tasks)
    val titleExplore = stringResource(R.string.title_explore)
    val titleProfile = stringResource(R.string.title_profile)

    val settingsIcon = rememberVectorPainter(TablerIcons.Settings)
    val downloadIcon = rememberVectorPainter(TablerIcons.Download)

    val drawerMenuItems = remember(drawerHome, drawerSettings, drawerDownloadTasks) {
        listOf(
            DrawerMenuItem(
                title = drawerHome,
                iconResId = R.drawable.ic_explore,
                route = "",
                isMainScreen = true,
            ),
            DrawerMenuItem(
                title = drawerDownloadTasks,
                iconPainter = downloadIcon,
                route = ScreenRoute.DownloadTasks.route,
            ),
            DrawerMenuItem(
                title = drawerSettings,
                iconPainter = settingsIcon,
                route = ScreenRoute.Settings.route,
            )
        )
    }

    val tabletMainMenuItems = remember(titleExplore, titleProfile) {
        listOf(
            ScreenRoute.Explore,
            ScreenRoute.Profile,
        ).map { screen ->
            val title = when (screen) {
                ScreenRoute.Explore -> titleExplore
                ScreenRoute.Profile -> titleProfile
                else -> ""
            }
            val iconResId = when (screen) {
                ScreenRoute.Explore -> R.drawable.ic_explore
                ScreenRoute.Profile -> R.drawable.ic_account_circle
                else -> R.drawable.ic_explore
            }

            DrawerMenuItem(
                title = title,
                iconResId = iconResId,
                route = screen.route,
                isMainScreen = true,
            )
        }
    }

    val tabletSecondaryMenuItems = remember(drawerSettings, drawerDownloadTasks) {
        listOf(
            DrawerMenuItem(
                title = drawerDownloadTasks,
                iconPainter = downloadIcon,
                route = ScreenRoute.DownloadTasks.route,
            ),
            DrawerMenuItem(
                title = drawerSettings,
                iconPainter = settingsIcon,
                route = ScreenRoute.Settings.route,
            )
        )
    }

    val selectedItem = remember { mutableStateOf(drawerMenuItems[0].title) }

    DismissibleDrawerSheet(
        drawerState = drawerState,
        drawerContainerColor = SaltTheme.colors.background
    ) {
        if (!isTabletLandscape) {
            RoundedColumn(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                drawerMenuItems.forEach { menuItem ->
                    DrawerItemComponent(
                        menuItem = menuItem,
                        onClick = {
                            selectedItem.value = menuItem.title
                            scope.launch {
                                handleNavigation(
                                    navController = navController,
                                    menuItem = menuItem,
                                    currentMainScreenRoute = currentMainScreenRoute.value
                                )
                                drawerState.close()
                            }
                        }
                    )
                }
            }
        } else {
            RoundedColumn {
                tabletMainMenuItems.forEach { menuItem ->
                    DrawerItemComponent(
                        menuItem = menuItem,
                        onClick = {
                            selectedItem.value = menuItem.title
                            scope.launch {
                                handleNavigation(
                                    navController = navController,
                                    menuItem = menuItem,
                                    currentMainScreenRoute = currentMainScreenRoute.value
                                )
                            }
                        }
                    )
                }
            }

            RoundedColumn(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                tabletSecondaryMenuItems.forEach { menuItem ->
                    DrawerItemComponent(
                        menuItem = menuItem,
                        onClick = {
                            selectedItem.value = menuItem.title
                            scope.launch {
                                handleNavigation(
                                    navController = navController,
                                    menuItem = menuItem,
                                    currentMainScreenRoute = currentMainScreenRoute.value
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@UnstableSaltUiApi
@Composable
private fun DrawerItemComponent(
    menuItem: DrawerMenuItem,
    onClick: () -> Unit
) {
    val resolvedPainter = menuItem.iconPainter
        ?: menuItem.iconImageVector?.let { rememberVectorPainter(it) }
        ?: menuItem.iconResId?.let { painterResource(id = it) }

    Item(
        onClick = onClick,
        text = menuItem.title,
        iconPainter = resolvedPainter,
        textColor = SaltTheme.colors.text,
        arrowType = ItemArrowType.None
    )
}

private fun handleNavigation(
    navController: NavController,
    menuItem: DrawerMenuItem,
    currentMainScreenRoute: String
) {
    when {
        menuItem.isMainScreen -> {
            val targetRoute = menuItem.route.ifEmpty { currentMainScreenRoute }
            // 清空当前导航栈并直达主屏幕路由
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }

        menuItem.route.isNotEmpty() -> {
            navController.navigate(menuItem.route) {
                launchSingleTop = true
            }
        }
    }
}