package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.LyricsData
import retrofit2.http.GET
import retrofit2.http.Query

interface LyricsApi {

    @GET("/lyric")
    suspend fun getLyricById(
        @Query("id") id: Long
    ) : LyricsData
}