package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class PersonalizedNewSongResponse(
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("category")
    val category: Int? = null,
    @SerializedName("result")
    val result: List<PersonalizedNewSongItem>? = null,
)

data class PersonalizedNewSongItem(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("picUrl")
    val picUrl: String? = null,
    @SerializedName("song")
    val song: PersonalizedNewSongSong? = null,
) {
    fun resolvedSongId(): Long? = song?.id ?: id
}

data class PersonalizedNewSongSong(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("artists")
    val artists: List<PersonalizedArtistLite>? = null,
    @SerializedName("album")
    val album: PersonalizedAlbumLite? = null,
)

data class PersonalizedArtistLite(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
)

data class PersonalizedAlbumLite(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("picUrl")
    val picUrl: String? = null,
)
