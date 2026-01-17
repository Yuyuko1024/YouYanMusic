package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class LikeListData(
    @SerializedName("ids")
    val ids: List<Long>,
    @SerializedName("checkPoint")
    val checkPoint: Long,
    @SerializedName("code")
    val code: Int
)