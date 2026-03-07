package com.youyuan.music.compose.ui.uicomponent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PadLandscapeDrawerContainer(
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerWidth: Dp = 320.dp,
    drawerStartPadding: Dp = 12.dp,
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = drawerStartPadding + drawerWidth)
                .clipToBounds()
        ) {
            content()
        }

        Box(
            modifier = Modifier
                .padding(start = drawerStartPadding)
                .width(drawerWidth)
                .fillMaxHeight()
        ) {
            drawerContent()
        }
    }
}