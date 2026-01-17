package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.UserPlaylistResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ProfileApi {
    @GET("/user/playlist")
    suspend fun getUserPlaylist(
        @Query("uid") uid: Long,
        @Query("limit") limit: Int = 1000,
        @Query("offset") offset: Int = 0,
    ): UserPlaylistResponse
}