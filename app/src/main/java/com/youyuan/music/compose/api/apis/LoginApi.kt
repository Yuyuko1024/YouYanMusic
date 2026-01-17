package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.LoginStatusData
import retrofit2.http.POST
import retrofit2.http.Query

interface LoginApi {
    @POST("/login/status")
    suspend fun checkLoginStatus(
        @Query("timestamp") timestamp: Long = System.currentTimeMillis(),
        @Query("ua") ua: String = "pc"
    ) : LoginStatusData
}