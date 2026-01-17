package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.CellphoneLoginResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SmsLoginApi {
    // Login using SMS captcha (verification code)
    @GET("/login/cellphone")
    suspend fun loginWithCaptcha(
        @Query("phone") phone: String,
        @Query("captcha") captcha: String,
        @Query("countrycode") countryCode: String = "86",
        @Query("timestamp") timestamp: Long = System.currentTimeMillis(),
        @Query("ua") ua: String = "pc",
    ): CellphoneLoginResponse
}
