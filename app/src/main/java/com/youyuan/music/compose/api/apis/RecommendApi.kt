package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.RecommendResourceResponse
import com.youyuan.music.compose.api.model.RecommendSongsResponse
import retrofit2.http.GET

interface RecommendApi {
    @GET("/recommend/resource")
    suspend fun getRecommendPlaylists(): RecommendResourceResponse

    @GET("/recommend/songs")
    suspend fun getRecommendSongs(): RecommendSongsResponse
}