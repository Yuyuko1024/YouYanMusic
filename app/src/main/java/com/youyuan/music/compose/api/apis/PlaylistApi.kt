package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.PlaylistDetailResponse
import com.youyuan.music.compose.api.model.TopPlaylistResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PlaylistApi {
    @GET("/top/playlist")
    suspend fun getTopPlaylists(
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0,
        @Query("cat") category: String = "全部",
        @Query("order") order: String = "hot"
    ) : TopPlaylistResponse

    @GET("/playlist/detail")
    suspend fun getPlaylistDetail(
        @Query("id") id: Long,
    ): PlaylistDetailResponse
}