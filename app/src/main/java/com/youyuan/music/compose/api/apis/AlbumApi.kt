package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.AlbumDetailResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AlbumApi {

    @GET("/album")
    suspend fun getAlbumDetails(
        @Query("id") albumId: Long
    ) : AlbumDetailResponse

}