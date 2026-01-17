package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class PersonalizedResponse(
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("result")
    val result: List<PersonalizedPlaylistItem>? = null,
    @SerializedName("hasTaste")
    val hasTaste: Boolean? = null,
    @SerializedName("category")
    val category: Int? = null,
)

data class PersonalizedPlaylistItem(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("picUrl")
    val picUrl: String? = null,
    @SerializedName("playCount")
    val playCount: Long? = null,
    @SerializedName("trackCount")
    val trackCount: Int? = null,
    @SerializedName("copywriter")
    val copywriter: String? = null,
)
