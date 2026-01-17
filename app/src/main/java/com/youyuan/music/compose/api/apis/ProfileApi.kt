package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.LikeListData
import retrofit2.http.GET
import retrofit2.http.Query

interface ProfileApi {
    @GET("/likelist")
    suspend fun getLikelistById(
        @Query("uid") uid: String
    ) : LikeListData?
}