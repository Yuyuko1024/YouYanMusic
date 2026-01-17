package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class BannerResponse(
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("banners")
    val banners: List<BannerItem>? = null
)

data class BannerItem(
    @SerializedName("bannerId")
    val bannerId: String? = null,
    @SerializedName("pic")
    val pic: String? = null,
    @SerializedName("titleColor")
    val titleColor: String? = null,
    @SerializedName("targetType")
    val targetType: Int? = null,
    @SerializedName("targetId")
    val targetId: Long? = null,
    @SerializedName("typeTitle")
    val typeTitle: String? = null,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("encodeId")
    val encodeId: String? = null,
    @SerializedName("showAdTag")
    val showAdTag: Boolean? = null,
    @SerializedName("adSource")
    val adSource: String? = null,
    @SerializedName("showContext")
    val showContext: String? = null
)
