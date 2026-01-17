package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.BannerResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BannerApi {
    /**
     * 获取 banner(轮播图)
     * @param type 0:pc 1:android 2:iphone 3:ipad
     */
    @GET("/banner")
    suspend fun getBanner(
        @Query("type") type: Int = 2
    ): BannerResponse
}
