package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class UserPlaylistResponse(
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("more")
    val more: Boolean? = null,
    @SerializedName("playlist")
    val playlist: List<UserPlaylistItem>? = null,
)

data class UserPlaylistItem(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("coverImgUrl")
    val coverImgUrl: String? = null,
    @SerializedName("trackCount")
    val trackCount: Int? = null,
    @SerializedName("playCount")
    val playCount: Long? = null,
    @SerializedName("subscribed")
    val subscribed: Boolean? = null,
    @SerializedName("creator")
    val creator: UserPlaylistCreator? = null,
)

data class UserPlaylistCreator(
    @SerializedName("userId")
    val userId: Long? = null,
)
