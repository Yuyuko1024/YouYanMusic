package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class SongDetailResponse(
    @SerializedName("songs")
    val songs: List<SongDetail>?,
    @SerializedName("privileges")
    val privileges: List<Privilege>?,
    @SerializedName("code")
    val code: Int
)

data class SongDetail(
    @SerializedName("name")
    val name: String?,
    @SerializedName("id")
    val id: Long,
    @SerializedName("ar")
    val ar: List<ArtistDetail>?,
    @SerializedName("al")
    val al: SongDetailAlbumData?,
    @SerializedName("alia")
    val alia: List<String?>? = emptyList(), // 别名
    @SerializedName("dt")
    val dt: Long?, // 歌曲时长
    @SerializedName("fee")
    val fee: Int?,
    @SerializedName("mv")
    val mv: Long?,
    @SerializedName("tns")
    val tns: List<String?>? = emptyList(),
)

data class ArtistDetail(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String?
)

data class SongDetailAlbumData(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String?,
    @SerializedName("picUrl")
    val picUrl: String?
)

data class Privilege(
    @SerializedName("id")
    val id: Long,
    @SerializedName("fee")
    val fee: Int,
    @SerializedName("plLevel")
    val plLevel: String?,
    @SerializedName("maxBrLevel")
    val maxBrLevel: String?
)