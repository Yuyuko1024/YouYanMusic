package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

/**
 * /playlist/detail 最小模型：只保留歌单页渲染与播放所需字段。
 */
data class PlaylistDetailResponse(
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("playlist")
    val playlist: PlaylistDetail? = null,
)

data class PlaylistDetail(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("coverImgUrl")
    val coverImgUrl: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("trackCount")
    val trackCount: Int? = null,
    @SerializedName("playCount")
    val playCount: Long? = null,
    @SerializedName("tracks")
    val tracks: List<SongDetail>? = null,

    // /playlist/detail 会返回完整 trackIds；用于构建全量播放队列（不必先把所有 SongDetail 拉完）。
    @SerializedName("trackIds")
    val trackIds: List<PlaylistTrackId>? = null,
)

data class PlaylistTrackId(
    @SerializedName("id")
    val id: Long,
)
