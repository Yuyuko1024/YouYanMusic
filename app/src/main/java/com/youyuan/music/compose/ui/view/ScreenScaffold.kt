package com.youyuan.music.compose.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.constants.AppBarHeight
import com.youyuan.music.compose.ui.utils.LocalPlayerAwareWindowInsets

@UnstableSaltUiApi
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    topBarHeight: Dp = AppBarHeight,
    topBarBackgroundColor: Color = Color.Transparent,
    topBarBlurRadius: Dp = 0.dp,
    useContentPadding: Boolean = false,
    topBar: @Composable BoxScope.() -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val playerAwareInsets = LocalPlayerAwareWindowInsets.current
    val basePadding = playerAwareInsets.asPaddingValues()

    val statusBarTop = with(density) { WindowInsets.systemBars.getTop(density).toDp() }
    val topBarTotalHeight = statusBarTop + topBarHeight

    val contentPaddingValues = remember(
        basePadding,
        layoutDirection,
        statusBarTop,
        topBarHeight,
        useContentPadding,
    ) {
        PaddingValues(
            start = basePadding.calculateStartPadding(layoutDirection),
            top = if (useContentPadding) statusBarTop + topBarHeight else 0.dp,
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

        // 顶栏层（覆盖在内容上方）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = basePadding.calculateStartPadding(layoutDirection),
                    end = basePadding.calculateEndPadding(layoutDirection),
                )
                .height(topBarTotalHeight)
        ) {
            // 背景层（颜色/模糊由 Screen 决定）
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(topBarBackgroundColor)
                    .then(if (topBarBlurRadius > 0.dp) Modifier.blur(topBarBlurRadius) else Modifier)
            )

            // 内容层：把状态栏高度留给 padding，让可点击区域在其下方
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarTop)
                    .height(topBarHeight)
            ) {
                topBar()
            }
        }
    }
}
