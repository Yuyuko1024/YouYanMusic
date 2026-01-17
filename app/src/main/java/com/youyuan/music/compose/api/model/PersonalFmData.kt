package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class PersonalFmResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: List<PersonalFmSong>? = null,
)

data class PersonalFmSong(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("artists")
    val artists: List<PersonalFmArtist>? = null,
    @SerializedName("album")
    val album: PersonalFmAlbum? = null,
    @SerializedName("duration")
    val duration: Long? = null,
    @SerializedName("fee")
    val fee: Int? = null,
    @SerializedName("mv")
    val mv: Long? = null,
)

data class PersonalFmArtist(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String? = null,
)

data class PersonalFmAlbum(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("picUrl")
    val picUrl: String? = null,
)
