package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class ToplistDetailResponse(
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("list")
    val list: List<ToplistItem>? = null,
    @SerializedName("artistToplist")
    val artistToplist: ToplistExtra? = null,
    @SerializedName("rewardToplist")
    val rewardToplist: ToplistExtra? = null,
)

data class ToplistItem(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("coverImgUrl")
    val coverImgUrl: String? = null,
    @SerializedName("updateFrequency")
    val updateFrequency: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("playCount")
    val playCount: Long? = null,
)

data class ToplistExtra(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("coverUrl")
    val coverUrl: String? = null,
    @SerializedName("position")
    val position: Int? = null,
    @SerializedName("updateFrequency")
    val updateFrequency: String? = null,
)
