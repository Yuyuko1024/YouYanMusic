package com.youyuan.music.compose.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.web.WebView
import com.moriafly.salt.ui.web.rememberWebViewState
import com.youyuan.music.compose.R
import com.youyuan.music.compose.ui.uicomponent.YouYanTitleBar
import com.youyuan.music.compose.ui.view.ScreenScaffold

@UnstableSaltUiApi
@Composable
fun InAppWebViewScreen(
    modifier: Modifier = Modifier,
    url: String? = "",
    onBack: () -> Unit = {},
) {
    val safeUrl = url?.trim().orEmpty()

    ScreenScaffold(
        modifier = modifier,
        useContentPadding = true,
        topBar = {
            YouYanTitleBar(
                onBack = onBack,
                text = stringResource(R.string.title_webview),
            )
        },
    ) { padding: PaddingValues ->
        if (safeUrl.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "URL 为空", color = SaltTheme.colors.subText)
            }
            return@ScreenScaffold
        }

        val state = rememberWebViewState(url = safeUrl)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            WebView(state = state, modifier = Modifier.fillMaxSize())
        }
    }
}