package com.youyuan.music.compose.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.constants.AppBarHeight
import com.youyuan.music.compose.ui.utils.AdaptiveLayoutMode
import com.youyuan.music.compose.ui.uicomponent.MainTopAppBar
import com.youyuan.music.compose.ui.utils.LocalPlayerAwareWindowInsets
import com.youyuan.music.compose.ui.utils.rememberAdaptiveLayoutMode

@UnstableSaltUiApi
@Composable
fun MainTabScaffold(
    title: String,
    onDrawerClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val playerAwareInsets = LocalPlayerAwareWindowInsets.current
    val basePadding = playerAwareInsets.asPaddingValues()

    val statusBarTop = with(density) { WindowInsets.systemBars.getTop(density).toDp() }

    val contentPaddingValues = remember(basePadding, layoutDirection, statusBarTop) {
        PaddingValues(
            start = basePadding.calculateStartPadding(layoutDirection),
            top = statusBarTop + AppBarHeight,
            end = basePadding.calculateEndPadding(layoutDirection),
            bottom = basePadding.calculateBottomPadding(),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        content(contentPaddingValues)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = basePadding.calculateStartPadding(layoutDirection),
                    end = basePadding.calculateEndPadding(layoutDirection),
                    top = statusBarTop,
                )
        ) {
            MainTopAppBar(
                title = title,
                onDrawerClick = onDrawerClick,
                onSearchClick = onSearchClick,
            )
        }
    }
}
