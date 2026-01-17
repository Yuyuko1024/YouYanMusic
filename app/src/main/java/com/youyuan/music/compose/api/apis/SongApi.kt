package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.SongDetailResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SongApi {
    @GET("/song/detail")
    suspend fun getSongDetails(
        @Query("ids") ids: String
    ): SongDetailResponse
}