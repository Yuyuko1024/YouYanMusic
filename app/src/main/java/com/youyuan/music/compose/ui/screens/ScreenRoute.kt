package com.youyuan.music.compose.ui.screens

import android.net.Uri

sealed class ScreenRoute(open val route: String) {
    // 首页
    object Explore : ScreenRoute("explore")
    object Profile : ScreenRoute("profile")

    object InAppWebView : ScreenRoute("webview?url={url}") {
        fun createRoute(url: String): String = "webview?url=${Uri.encode(url)}"
    }

    object Search : ScreenRoute("search")
    object Settings : ScreenRoute("settings")
    object LoginPage : ScreenRoute("loginPage")
    object RegisterPage : ScreenRoute("registerPage")


    object SongComments : ScreenRoute("comments/{songId}") {
        fun createRoute(songId: Long): String = "comments/$songId"
    }

    object PlaylistDetail : ScreenRoute("playlist/{playlistId}") {
        fun createRoute(playlistId: Long): String = "playlist/$playlistId"
    }


    companion object {
        val MainScreens = listOf(
            Explore,
            Profile
        )
    }

}