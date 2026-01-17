package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class RecommendSongsResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: RecommendSongsData? = null,
)

data class RecommendSongsData(
    @SerializedName("dailySongs")
    val dailySongs: List<SongDetail>? = null,
)
