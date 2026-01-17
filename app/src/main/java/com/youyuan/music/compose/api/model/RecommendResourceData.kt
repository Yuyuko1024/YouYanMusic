package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class RecommendResourceResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("featureFirst")
    val featureFirst: Boolean? = null,
    @SerializedName("haveRcmdSongs")
    val haveRcmdSongs: Boolean? = null,
    @SerializedName("recommend")
    val recommend: List<RecommendResourceItem>? = null,
)

data class RecommendResourceItem(
    @SerializedName("id")
    val id: Long,
    @SerializedName("type")
    val type: Int? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("copywriter")
    val copywriter: String? = null,
    @SerializedName("picUrl")
    val picUrl: String? = null,
    @SerializedName("playcount")
    val playCount: Long? = null,
    @SerializedName("trackCount")
    val trackCount: Int? = null,
)
