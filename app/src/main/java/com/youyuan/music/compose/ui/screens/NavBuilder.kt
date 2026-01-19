package com.youyuan.music.compose.ui.screens

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel
import com.youyuan.music.compose.ui.viewmodel.ProfileViewModel
import com.youyuan.music.compose.ui.viewmodel.SearchViewModel

@OptIn(UnstableApi::class)
@UnstableSaltUiApi
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
fun NavGraphBuilder.navigationBuilder(
    context: Context,
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    searchViewModel: SearchViewModel,
    playerViewModel: PlayerViewModel,
    profileViewModel: ProfileViewModel,
) {
    composable(ScreenRoute.Explore.route) {
        ExploreScreen(
            context = context,
            navController = navController,
            playerViewModel = playerViewModel,
        )
    }
    composable(ScreenRoute.Profile.route) {
        ProfileScreen(
            context = context,
            navController = navController,
            profileViewModel = profileViewModel,
        )
    }
    composable(ScreenRoute.Search.route) {
        SearchScreen(
            searchViewModel = searchViewModel,
            playerViewModel = playerViewModel,
        )
    }
    // 设置页面
    composable(ScreenRoute.Settings.route) {
        SettingsScreen()
    }
    // 登录页面
    composable(ScreenRoute.LoginPage.route) {
        LoginScreen(
            profileViewModel = profileViewModel,
            navController = navController
        )
    }
    // 歌曲评论
    composable(
        route = ScreenRoute.SongComments.route,
        arguments = listOf(
            navArgument("songId") { type = NavType.LongType }
        )
    ) { backStackEntry ->
        val songId = backStackEntry.arguments?.getLong("songId") ?: 0L
        SongCommentScreen(
            navController = navController,
            songId = songId,
        )
    }

    // 内部 WebView
    composable(
        route = ScreenRoute.InAppWebView.route,
        arguments = listOf(
            navArgument("url") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val url = backStackEntry.arguments?.getString("url")
        InAppWebViewScreen(url = url)
    }

    composable(
        route = ScreenRoute.PlaylistDetail.route,
        arguments = listOf(
            navArgument("playlistId") { type = NavType.LongType }
        )
    ) { backStackEntry ->
        val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
        PlaylistDetailScreen(
            playlistId = playlistId,
            playerViewModel = playerViewModel,
        )
    }

}