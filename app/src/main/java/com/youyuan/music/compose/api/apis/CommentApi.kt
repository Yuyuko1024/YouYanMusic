package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.CommentMusicResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CommentApi {

    /**
     * 获取歌曲评论
     * API: /comment/music
     */
    @GET("/comment/music")
    suspend fun getMusicComments(
        @Query("id") songId: Long,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): CommentMusicResponse
}
