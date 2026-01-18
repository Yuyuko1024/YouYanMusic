package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

/**
 * /song/like/check
 * 文档描述：返回“被标记为喜爱的歌曲 ID 组成的数组”。
 * 由于不同实现可能包一层 data/ids，这里做兼容字段。
 */
data class SongLikeCheckResponse(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("ids") val ids: List<Long>? = null,
) {
    fun likedIds(): List<Long> = ids ?: emptyList()
}

/**
 * /like
 * 文档描述：成功 code=200。
 */
data class SongLikeActionResponse(
    @SerializedName("songs") val songs: List<Any>? = null,
    @SerializedName("playlistId") val playlistId: Long? = null,
    @SerializedName("code") val code: Int? = null,
)
