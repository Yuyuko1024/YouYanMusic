package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.SongUrlResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SongUrlApi {
    @GET("/song/url/v1")
    suspend fun getSongUrl(
        @Query("id") songIds: String,
        @Query("level") qualityLevel: String = "standard"
    ) : SongUrlResponse
}