package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

/**
 * /playlist/track/all 最小模型：用于获取歌单完整歌曲列表（通常比 /playlist/detail 的 tracks 更全）。
 */
data class PlaylistTrackAllResponse(
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("songs")
    val songs: List<SongDetail>? = null,
    @SerializedName("privileges")
    val privileges: List<Privilege>? = null,
    @SerializedName("more")
    val more: Boolean? = null,
)
