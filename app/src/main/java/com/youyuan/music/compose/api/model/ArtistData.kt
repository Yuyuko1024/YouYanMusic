package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

/**
 * /artist/detail?id=
 */
data class ArtistDetailResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: ArtistDetailData? = null,
)

data class ArtistDetailData(
    @SerializedName("artist")
    val artist: ArtistProfile? = null,
)

data class ArtistProfile(
    @SerializedName("id")
    val id: Long,
    @SerializedName("cover")
    val cover: String? = null,
    @SerializedName("avatar")
    val avatar: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("transNames")
    val transNames: List<String>? = null,
    @SerializedName("alias")
    val alias: List<String>? = null,
    @SerializedName("briefDesc")
    val briefDesc: String? = null,
    @SerializedName("albumSize")
    val albumSize: Long? = null,
    @SerializedName("musicSize")
    val musicSize: Long? = null,
    @SerializedName("mvSize")
    val mvSize: Long? = null,
)

/**
 * /artist/top/song?id=
 */
data class ArtistTopSongResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("more")
    val more: Boolean? = null,
    @SerializedName("songs")
    val songs: List<SongDetail>? = null,
)

/**
 * /artist/album?id=
 */
data class ArtistAlbumResponse(
    @SerializedName("artist")
    val artist: AlbumArtist? = null,
    @SerializedName("hotAlbums")
    val hotAlbums: List<AlbumDetail>? = null,
    @SerializedName("more")
    val more: Boolean? = null,
    @SerializedName("code")
    val code: Int,
)
