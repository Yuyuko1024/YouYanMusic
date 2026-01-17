package com.youyuan.music.compose.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel
import com.youyuan.music.compose.ui.viewmodel.SearchViewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.Music
import compose.icons.tablericons.Notes

@UnstableApi
@UnstableSaltUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {

    val searchSuggestions by searchViewModel.searchSuggestions.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(8.dp))

            // 什么都不做时显示的默认内容
            AnimatedVisibility(
                visible = !isLoading && searchResults.isEmpty() && searchSuggestions.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(R.drawable.no_item),
                            contentDescription = stringResource(R.string.cd_search),
                            modifier = Modifier.size(200.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.search_hint),
                            style = SaltTheme.textStyles.main,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Text(
                            text = stringResource(R.string.search_description),
                            style = SaltTheme.textStyles.sub
                        )
                    }
                }
            }

            // 搜索结果列表
            AnimatedVisibility(
                visible = searchResults.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = searchResults,
                        key = { it.id.toString() }
                    ) { result ->
                        SearchResultItem(
                            title = result.name ?: stringResource(R.string.unknown_song),
                            subtitle = result.artists?.joinToString(", ") { it.name ?: "" } ?: stringResource(R.string.unknown_artist),
                            onClick = {
                                playerViewModel.playSong(result)
                            }
                        )
                    }
                }
            }
        }

        // 搜索建议列表
        AnimatedVisibility(
            visible = searchSuggestions.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            searchViewModel.clearSearchSuggestions()
                        }
                    }
            ) {
                // 背景遮罩层
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SaltTheme.colors.subText.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxSize(0.5f)
                            .background(SaltTheme.colors.background)
                            .pointerInput(Unit) {
                                detectTapGestures { }
                            }
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text(
                                    text = stringResource(R.string.search_suggestions),
                                    style = SaltTheme.textStyles.main,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }

                            items(
                                items = searchSuggestions,
                                key = { it }
                            ) { suggestion ->
                                SearchSuggestItem(
                                    title = suggestion,
                                    onClick = {
                                        searchViewModel.clearSearchSuggestions()
                                        searchViewModel.searchSongs(suggestion)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 加载状态（叠加在最上层）
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                RoundedColumn {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.searching),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }
    }

}


@UnstableSaltUiApi
@Composable
private fun SearchSuggestItem(
    title: String,
    onClick: () -> Unit
) {
    Item(
        onClick = onClick,
        text = title,
        iconPainter = rememberVectorPainter(TablerIcons.Notes)
    )
}

@UnstableSaltUiApi
@Composable
private fun SearchResultItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Item(
        onClick = onClick,
        text = title,
        sub = subtitle,
        iconPainter = rememberVectorPainter(TablerIcons.Music)
    )
}