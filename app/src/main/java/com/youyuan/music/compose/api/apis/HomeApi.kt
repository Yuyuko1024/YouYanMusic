package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.PersonalizedNewSongResponse
import com.youyuan.music.compose.api.model.PersonalizedResponse
import com.youyuan.music.compose.api.model.ToplistDetailResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface HomeApi {

    @GET("/personalized")
    suspend fun getPersonalized(
        @Query("limit") limit: Int = 10,
    ): PersonalizedResponse

    @GET("/personalized/newsong")
    suspend fun getPersonalizedNewSong(
        @Query("limit") limit: Int = 10,
    ): PersonalizedNewSongResponse

    @GET("/toplist/detail")
    suspend fun getToplistDetail(): ToplistDetailResponse
}
