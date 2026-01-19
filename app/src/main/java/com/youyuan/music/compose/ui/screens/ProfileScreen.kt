package com.youyuan.music.compose.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moriafly.salt.ui.ItemButton
import com.moriafly.salt.ui.ItemOuterTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.ui.uicomponent.AccountHeaderCard
import com.youyuan.music.compose.ui.viewmodel.ProfileViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import coil3.compose.AsyncImage
import com.moriafly.salt.ui.SaltTheme
import com.youyuan.music.compose.api.model.UserPlaylistItem

@SuppressLint("UnusedBoxWithConstraintsScope")
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@OptIn(ExperimentalMaterialApi::class)
@UnstableSaltUiApi
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    context: Context,
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    // 获取用户信息和登录状态
    val isLoggedIn by profileViewModel.isLoggedIn.collectAsState()
    val userProfile by profileViewModel.userProfile.collectAsState()

    val userPlaylists by profileViewModel.userPlaylists.collectAsState()
    val userPlaylistsLoading by profileViewModel.userPlaylistsLoading.collectAsState()
    val userPlaylistsError by profileViewModel.userPlaylistsError.collectAsState()

    LaunchedEffect(isLoggedIn, userProfile?.userId) {
        profileViewModel.loadUserPlaylists(isLoggedIn = isLoggedIn)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val uid = userProfile?.userId
        val likedPlaylistId = if (isLoggedIn && uid != null) {
            userPlaylists.firstOrNull { it.creator?.userId == uid && it.name == "我喜欢的音乐" }?.id
        } else {
            null
        }

        val createdPlaylists = if (isLoggedIn && uid != null) {
            userPlaylists.filter { it.creator?.userId == uid && it.id != likedPlaylistId }
        } else {
            emptyList()
        }
        val subscribedPlaylists = if (isLoggedIn && uid != null) {
            userPlaylists.filter { it.creator?.userId != null && it.creator.userId != uid }
        } else {
            emptyList()
        }
        val pullRefreshState = rememberPullRefreshState(
            refreshing = userPlaylistsLoading,
            onRefresh = {
                if (isLoggedIn) {
                    profileViewModel.loadUserPlaylists(isLoggedIn = true, force = true)
                }
            },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    AccountHeaderCard(
                        modifier = Modifier.fillMaxWidth(),
                        profile = if (isLoggedIn) userProfile else null,
                        onClick = {
                            if (!isLoggedIn) {
                                navController.navigate(ScreenRoute.LoginPage.route)
                            } else {
                                // TODO: 实现用户详情页或退出登录功能
                            }
                        }
                    )
                }

                if (!isLoggedIn) {
                    item {
                        RoundedColumn(Modifier.fillMaxWidth()) {
                            Text(
                                text = "登录后可查看你的歌单",
                                modifier = Modifier.padding(12.dp)
                            )
                            ItemButton(
                                text = "去登录",
                                onClick = { navController.navigate(ScreenRoute.LoginPage.route) }
                            )
                        }
                    }
                } else {
                    if (!userPlaylistsError.isNullOrBlank()) {
                        item {
                            RoundedColumn(Modifier.fillMaxWidth()) {
                                Text(
                                    text = userPlaylistsError ?: "",
                                    modifier = Modifier.padding(12.dp)
                                )
                                ItemButton(
                                    text = "重试",
                                    onClick = { profileViewModel.loadUserPlaylists(isLoggedIn = true, force = true) }
                                )
                            }
                        }
                    }

                    if (createdPlaylists.isNotEmpty()) {
                        item { ItemOuterTitle("创建的歌单") }
                        items(createdPlaylists, key = { it.id }) { pl ->
                            PlaylistCard(
                                playlist = pl,
                                onClick = {
                                    navController.navigate(ScreenRoute.PlaylistDetail.createRoute(pl.id))
                                }
                            )
                        }
                    }

                    if (subscribedPlaylists.isNotEmpty()) {
                        item { ItemOuterTitle("收藏的歌单") }
                        items(subscribedPlaylists, key = { it.id }) { pl ->
                            PlaylistCard(
                                playlist = pl,
                                onClick = {
                                    navController.navigate(ScreenRoute.PlaylistDetail.createRoute(pl.id))
                                }
                            )
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = userPlaylistsLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: UserPlaylistItem,
    onClick: () -> Unit,
) {
    val name = playlist.name.orEmpty().ifBlank { "未命名" }
    val cover = playlist.coverImgUrl
    val trackCount = playlist.trackCount ?: 0
    val playCountText = formatCount(playlist.playCount)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = cover,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SaltTheme.colors.background.copy(alpha = 0.6f)),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 0.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append("${trackCount}首")
                    if (playCountText != null) append(" · 播放${playCountText}")
                },
                color = SaltTheme.colors.subText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatCount(value: Long?): String? {
    val v = value ?: return null
    if (v < 0) return null
    return when {
        v < 10_000 -> v.toString()
        v < 100_000_000 -> String.format("%.1f万", v / 10_000.0).trimEnd('0').trimEnd('.')
        else -> String.format("%.1f亿", v / 100_000_000.0).trimEnd('0').trimEnd('.')
    }
}

