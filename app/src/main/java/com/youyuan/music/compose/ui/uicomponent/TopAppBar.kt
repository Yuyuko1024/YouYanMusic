package com.youyuan.music.compose.ui.uicomponent

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.icons.ArrowBack
import com.moriafly.salt.ui.icons.SaltIcons
import com.youyuan.music.compose.R
import com.youyuan.music.compose.constants.AppBarHeight
import com.youyuan.music.compose.ui.viewmodel.SearchViewModel

@UnstableSaltUiApi
@Composable
fun MainTopAppBar(
    modifier: Modifier = Modifier,
    title: String,
    onDrawerClick: () -> Unit = { },
    onSearchClick: () -> Unit = { }
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppBarHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onDrawerClick() },
            modifier = modifier.padding(4.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_menu_24px),
                contentDescription = stringResource(R.string.cd_drawer)
            )
        }
        Text(
            text = title,
            modifier = modifier.fillMaxWidth().weight(1f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
        IconButton(
            onClick = { onSearchClick() },
            modifier = modifier.padding(4.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_search_24px),
                contentDescription = stringResource(R.string.cd_search)
            )
        }
    }
}

@UnstableSaltUiApi
@Composable
fun SearchTopAppBar(
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel,
    onBackClick: () -> Unit = { }
) {
    val searchQuery by searchViewModel.searchQueryText.collectAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppBarHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val backButtonContentDescription = stringResource(R.string.back_btn)
        IconButton(
            onClick = { onBackClick() },
            modifier = modifier.padding(4.dp)
                .semantics {
                    this.role = Role.Button
                    this.contentDescription = backButtonContentDescription
                }
        ) {
            Icon(
                painter = rememberVectorPainter(SaltIcons.ArrowBack),
                contentDescription = stringResource(R.string.cd_drawer)
            )
        }
        Row(
            modifier = Modifier
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemEdit(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                text = searchQuery,
                onChange = { query ->
                    searchViewModel.searchSuggestions(query)
                },
                hint = stringResource(R.string.search_hint_topbar),
                hintColor = SaltTheme.colors.text,
            )

            // 清除按钮，只在有内容时显示
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = {
                        searchViewModel.clearSearchSuggestions()
                    },
                    modifier = modifier.padding(4.dp)
                        .semantics {
                            this.role = Role.Button
                            this.contentDescription = "清除搜索"
                        }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close_24px),
                        contentDescription = "清除搜索"
                    )
                }
            }

            // 搜索按钮
            IconButton(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        searchViewModel.clearSearchSuggestions()
                        searchViewModel.searchSongs(searchQuery)
                    }
                },
                modifier = modifier.padding(4.dp)
                    .semantics {
                        this.role = Role.Button
                        this.contentDescription = "搜索"
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search_24px),
                    contentDescription = "搜索",
                )
            }
        }
    }
}
