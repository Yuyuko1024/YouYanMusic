package com.youyuan.music.compose.ui.screens

sealed class ScreenRoute(open val route: String) {
    // 首页
    object Explore : ScreenRoute("explore")
    object Profile : ScreenRoute("profile")

    object Search : ScreenRoute("search")
    object Settings : ScreenRoute("settings")
    object LoginPage : ScreenRoute("loginPage")
    object RegisterPage : ScreenRoute("registerPage")

    object LikedSong : ScreenRoute("liked/{userId}") {
        fun createRoute(userId: Long): String = "liked/$userId"
    }

    object SongComments : ScreenRoute("comments/{songId}") {
        fun createRoute(songId: Long): String = "comments/$songId"
    }


    companion object {
        val MainScreens = listOf(
            Explore,
            Profile
        )
    }

}