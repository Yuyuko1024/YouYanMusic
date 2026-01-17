package com.youyuan.music.compose.api.apis

import retrofit2.http.GET

interface RecommendApi {
    @GET("/recommend/resource")
    suspend fun getRecommendPlaylists() : Any?

    @GET("/recommend/songs")
    suspend fun getRecommendSongs() : Any?
}