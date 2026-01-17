package com.youyuan.music.compose.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moriafly.salt.ui.web.WebView
import com.moriafly.salt.ui.web.rememberWebViewState
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.SaltTheme

@UnstableSaltUiApi
@Composable
fun InAppWebViewScreen(
    modifier: Modifier = Modifier,
    url: String? = ""
) {
    val safeUrl = url?.trim().orEmpty()
    if (safeUrl.isBlank()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "URL 为空", color = SaltTheme.colors.subText)
        }
        return
    }

    val state = rememberWebViewState(url = safeUrl)
    Box(modifier.fillMaxSize()) {
        WebView(state = state, modifier = Modifier.fillMaxSize())
    }
}