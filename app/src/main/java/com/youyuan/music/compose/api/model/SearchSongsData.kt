package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class SearchSongsResponse(
    @SerializedName("result")
    val result: Result? = null,
    @SerializedName("code")
    val code: Long? = null
)

data class Result(
    @SerializedName("songs")
    val songs: List<Song>? = null,
    @SerializedName("hasMore")
    val hasMore: Boolean? = null,
    @SerializedName("songCount")
    val songCount: Long? = null
)

data class Song(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("artists")
    val artists: List<Artist>? = null,
    @SerializedName("album")
    val album: Album? = null,
    @SerializedName("duration")
    val duration: Long? = null,
    @SerializedName("copyrightId")
    val copyrightId: Long? = null,
    @SerializedName("status")
    val status: Long? = null,
    @SerializedName("alias")
    val alias: List<String>? = null,
    @SerializedName("rtype")
    val rtype: Long? = null,
    @SerializedName("ftype")
    val ftype: Long? = null,
    @SerializedName("transNames")
    val transNames: List<String>? = null,
    @SerializedName("mvid")
    val mvid: Long? = null,
    @SerializedName("fee")
    val fee: Long? = null,
    @SerializedName("rUrl")
    val rUrl: String? = null,
    @SerializedName("mark")
    val mark: Long? = null
)

data class Artist(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("picUrl")
    val picUrl: String? = null,
    @SerializedName("alias")
    val alias: List<String>? = null,
    @SerializedName("albumSize")
    val albumSize: Long? = null,
    @SerializedName("musicSize")
    val musicSize: Long? = null,
    @SerializedName("picId")
    val picId: Long? = null,
    @SerializedName("fansGroup")
    val fansGroup: String? = null,
    @SerializedName("recommendText")
    val recommendText: String? = null,
    @SerializedName("appendRecText")
    val appendRecText: String? = null,
    @SerializedName("fansSize")
    val fansSize: Long? = null,
    @SerializedName("img1v1Url")
    val img1v1Url: String? = null,
    @SerializedName("img1v1")
    val img1v1: Long? = null,
    @SerializedName("trans")
    val trans: String? = null
)

data class Album(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("artist")
    val artist: Artist? = null,
    @SerializedName("publishTime")
    val publishTime: Long? = null,
    @SerializedName("size")
    val size: Long? = null,
    @SerializedName("copyrightId")
    val copyrightId: Long? = null,
    @SerializedName("status")
    val status: Long? = null,
    @SerializedName("picId")
    val picId: Long? = null,
    @SerializedName("mark")
    val mark: Long? = null
)