package com.youyuan.music.compose.ui.uicomponent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerState

@Composable
fun AdaptiveDrawerContainer(
    isTabletLandscape: Boolean,
    drawerState: DrawerState,
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (isTabletLandscape) {
        PadLandscapeDrawerContainer(
            drawerContent = drawerContent,
            content = content,
            modifier = modifier,
        )
    } else {
        DismissibleNavigationDrawer(
            drawerState = drawerState,
            drawerContent = drawerContent,
            gesturesEnabled = false,
            modifier = modifier,
        ) {
            content()
        }
    }
}