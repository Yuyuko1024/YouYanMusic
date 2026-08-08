package com.youyuan.music.compose.ui.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

val PlayerForegroundColorLight = Color.White.copy(alpha = 0.9f)
val PlayerForegroundColorDark = Color.Black.copy(alpha = 0.9f)

private val rememberedPlayerUIColorState = mutableStateOf<Color?>(null)

fun getPlayerUIColor(isDark: Boolean): Color {
    return if (isDark) PlayerForegroundColorLight else PlayerForegroundColorDark
}

fun rememberPlayerUIColor(color: Color) {
    rememberedPlayerUIColorState.value = color
}

val LocalPlayerUIColor = compositionLocalOf {
    PlayerForegroundColorLight
}