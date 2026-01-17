package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.QrCodeLoginCheckData
import com.youyuan.music.compose.api.model.QrCodeLoginImgData
import com.youyuan.music.compose.api.model.QrCodeLoginKeyData
import retrofit2.http.GET
import retrofit2.http.Query

interface QrCodeLoginApi {
    @GET("/login/qr/key")
    suspend fun getQrCodeKey(
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): QrCodeLoginKeyData

    @GET("/login/qr/create")
    suspend fun createQrCode(
        @Query("key") key: String,
        @Query("qrimg") qrImg: Boolean = true,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis(),
        @Query("ua") ua: String = "pc"
    ): QrCodeLoginImgData

    @GET("/login/qr/check")
    suspend fun checkQrCodeStatus(
        @Query("key") key: String,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis(),
        @Query("ua") ua: String = "pc"
    ): QrCodeLoginCheckData
}