package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.SongLikeActionResponse
import com.youyuan.music.compose.api.model.SongLikeCheckResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 喜欢/取消喜欢歌曲相关接口。
 *
 * API 文档：`netease-cloudmusic-apidoc-home.md`
 * - /like
 * - /song/like/check
 */
interface SongLikeApi {

    /**
     * 喜欢音乐：`/like?id=347230`
     *
     * 注意：该接口在 like=true 时不要携带 like 参数，否则可能不生效。
     */
    @GET("/like")
    suspend fun likeSong(
        @Query("id") id: Long,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis(),
    ): SongLikeActionResponse

    /**
     * 取消喜欢：`/like?id=347230&like=false`
     *
     * 注意：取消喜欢时必须带上 like=false，否则不会生效。
     */
    @GET("/like")
    suspend fun unlikeSong(
        @Query("id") id: Long,
        @Query("like") like: Boolean = false,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis(),
    ): SongLikeActionResponse

    /**
     * 歌曲是否喜爱：`/song/like/check?ids=[2058263032,1497529942]`
     *
     * 注意：这里按文档传入 JSON 数组字符串。
     */
    @GET("/song/like/check")
    suspend fun checkSongLike(
        @Query("ids") ids: String,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis(),
    ): SongLikeCheckResponse
}
