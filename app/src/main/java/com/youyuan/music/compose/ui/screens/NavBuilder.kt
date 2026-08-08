package com.youyuan.music.compose.ui.screens

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.Modifier
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
@ExperimentalMaterial3ExpressiveApi
@ExperimentalFoundationApi
fun NavGraphBuilder.navigationBuilder(
    context: Context,
    navController: NavHostController,
    searchViewModel: SearchViewModel,
    playerViewModel: PlayerViewModel,
    profileViewModel: ProfileViewModel,
    openDrawer: () -> Unit,
) {
    composable(ScreenRoute.Explore.route) {
        ExploreScreen(
            modifier = Modifier,
            context = context,
            navController = navController,
            playerViewModel = playerViewModel,
            openDrawer = openDrawer,
            onSearchClick = { navController.navigate(ScreenRoute.Search.route) },
        )
    }
    composable(ScreenRoute.Profile.route) {
        ProfileScreen(
            modifier = Modifier,
            context = context,
            navController = navController,
            profileViewModel = profileViewModel,
            openDrawer = openDrawer,
            onSearchClick = { navController.navigate(ScreenRoute.Search.route) },
        )
    }
    composable(ScreenRoute.Search.route) {
        SearchScreen(
            modifier = Modifier,
            searchViewModel = searchViewModel,
            playerViewModel = playerViewModel,
            onBackClick = { navController.popBackStack() },
        )
    }
    // 设置页面
    composable(ScreenRoute.Settings.route) {
        SettingsScreen(
            modifier = Modifier,
            onBack = { navController.popBackStack() },
        )
    }
    composable(ScreenRoute.DownloadTasks.route) {
        DownloadTasksScreen(
            modifier = Modifier,
            onBack = { navController.popBackStack() },
        )
    }
    // 登录页面
    composable(ScreenRoute.LoginPage.route) {
        LoginScreen(
            modifier = Modifier,
            profileViewModel = profileViewModel,
            navController = navController,
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
            modifier = Modifier,
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
        InAppWebViewScreen(
            modifier = Modifier,
            url = url,
            onBack = { navController.popBackStack() },
        )
    }

    composable(
        route = ScreenRoute.PlaylistDetail.route,
        arguments = listOf(
            navArgument("playlistId") { type = NavType.LongType }
        )
    ) { backStackEntry ->
        val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
        PlaylistDetailScreen(
            modifier = Modifier,
            playlistId = playlistId,
            playerViewModel = playerViewModel,
            navController = navController,
        )
    }

    composable(
        route = ScreenRoute.AlbumDetail.route,
        arguments = listOf(
            navArgument("albumId") { type = NavType.LongType }
        )
    ) { backStackEntry ->
        val albumId = backStackEntry.arguments?.getLong("albumId") ?: 0L
        AlbumScreen(
            modifier = Modifier,
            albumId = albumId,
            navController = navController,
            playerViewModel = playerViewModel,
        )
    }

    composable(
        route = ScreenRoute.ArtistDetail.route,
        arguments = listOf(
            navArgument("artistId") { type = NavType.LongType }
        )
    ) { backStackEntry ->
        val artistId = backStackEntry.arguments?.getLong("artistId") ?: 0L
        ArtistScreen(
            modifier = Modifier,
            artistId = artistId,
            navController = navController,
            playerViewModel = playerViewModel,
        )
    }

}