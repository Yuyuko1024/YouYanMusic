package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.CaptchaResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CaptchaApi {
    // Doc endpoint
    @GET("/captcha/sent")
    suspend fun sendCaptcha(
        @Query("phone") phone: String,
        @Query("ctcode") countryCode: String = "86",
        @Query("timestamp") timestamp: Long = System.currentTimeMillis(),
    ): CaptchaResponse

    @GET("/captcha/verify")
    suspend fun verifyCaptcha(
        @Query("phone") phone: String,
        @Query("captcha") captcha: String,
        @Query("ctcode") countryCode: String = "86",
        @Query("timestamp") timestamp: Long = System.currentTimeMillis(),
    ): CaptchaResponse
}
