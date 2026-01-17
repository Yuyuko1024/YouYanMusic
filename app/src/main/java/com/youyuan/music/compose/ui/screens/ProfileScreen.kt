package com.youyuan.music.compose.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.moriafly.salt.ui.ItemButton
import com.moriafly.salt.ui.ItemOuterTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.lazy.LazyColumn
import com.youyuan.music.compose.ui.uicomponent.AccountHeaderCard
import com.youyuan.music.compose.ui.viewmodel.ExploreViewModel
import com.youyuan.music.compose.ui.viewmodel.MyMusicViewModel
import com.youyuan.music.compose.ui.viewmodel.ProfileViewModel

@SuppressLint("UnusedBoxWithConstraintsScope")
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@UnstableSaltUiApi
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    context: Context,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    exploreViewModel: ExploreViewModel,
    myMusicViewModel: MyMusicViewModel,
) {
    // 获取用户信息和登录状态
    val isLoggedIn by profileViewModel.isLoggedIn.collectAsState()
    val userProfile by profileViewModel.userProfile.collectAsState()

    val myMusicPaging = myMusicViewModel.songPagingFlow.collectAsLazyPagingItems()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AccountHeaderCard(
                modifier = Modifier.fillMaxWidth(),
                profile = if (isLoggedIn) userProfile else null,
                onClick = {
                    if (!isLoggedIn) {
                        // 未登录，跳转到登录页面
                        navController.navigate(ScreenRoute.LoginPage.route)
                    } else {
                        // 已登录，可以跳转到用户详情页或显示菜单
                        // TODO: 实现用户详情页或退出登录功能
                    }
                }
            )

            ItemOuterTitle("我的歌单")
            LazyColumn(Modifier.fillMaxWidth()) {
                item {
                    RoundedColumn(Modifier.fillMaxWidth()) {
                        ItemButton(
                            text = "我喜欢的音乐",
                            onClick = {
                                userProfile?.userId?.let {
                                    navController.navigate(
                                        ScreenRoute.LikedSong.createRoute(it)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            ItemOuterTitle("API Test")
            RoundedColumn(Modifier.fillMaxWidth()) {
                ItemButton(
                    text = "获取每日推荐歌单",
                    onClick = {
                        if (!isLoggedIn) {
                            navController.navigate(ScreenRoute.LoginPage.route)
                        } else {
                            exploreViewModel.loadDailyRecommendPlaylists(isLoggedIn = true, force = true)
                        }
                    }
                )
                ItemButton(
                    text = "获取每日推荐歌曲",
                    onClick = {
                        if (!isLoggedIn) {
                            navController.navigate(ScreenRoute.LoginPage.route)
                        } else {
                            exploreViewModel.loadDailyRecommendSongs(isLoggedIn = true, force = true)
                        }
                    }
                )
                ItemButton(
                    text = "获取私人FM",
                    onClick = {
                        if (!isLoggedIn) {
                            navController.navigate(ScreenRoute.LoginPage.route)
                        } else {
                            exploreViewModel.loadPersonalFm(isLoggedIn = true, force = true)
                        }
                    }
                )
                ItemButton(
                    text = "获取喜欢列表",
                    onClick = {
                    }
                )
            }
        }
    }
}

