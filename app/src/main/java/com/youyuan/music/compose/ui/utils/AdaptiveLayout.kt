package com.youyuan.music.compose.ui.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AdaptiveLayoutMode {
    Phone,
    TabletLandscape,
}

val AdaptiveLayoutMode.contentHorizontalPadding: Dp
    get() = when (this) {
        AdaptiveLayoutMode.Phone -> 0.dp
        AdaptiveLayoutMode.TabletLandscape -> 24.dp
    }

@Composable
fun rememberAdaptiveLayoutMode(maxWidth: Dp): AdaptiveLayoutMode {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTabletLikeLandscape = isLandscape && (
        configuration.smallestScreenWidthDp >= 600 ||
            configuration.screenWidthDp >= 840 ||
            maxWidth >= 840.dp
        )

    return remember(
        configuration.orientation,
        configuration.smallestScreenWidthDp,
        configuration.screenWidthDp,
        maxWidth
    ) {
        if (isTabletLikeLandscape) AdaptiveLayoutMode.TabletLandscape else AdaptiveLayoutMode.Phone
    }
}