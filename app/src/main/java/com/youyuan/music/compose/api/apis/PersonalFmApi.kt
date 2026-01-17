package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.PersonalFmResponse
import retrofit2.http.GET

interface PersonalFmApi {
    @GET("/personal_fm")
    suspend fun getPersonalFm(): PersonalFmResponse
}
