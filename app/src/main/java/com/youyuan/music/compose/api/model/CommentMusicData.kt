package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

/**
 * /comment/music 响应（按需精简字段；未声明的字段会被 Gson 自动忽略）
 */
data class CommentMusicResponse(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("comments") val comments: List<CommentItem> = emptyList(),
    @SerializedName("hotComments") val hotComments: List<CommentItem> = emptyList(),
    @SerializedName("total") val total: Int? = null,
    @SerializedName("more") val more: Boolean? = null,
)

data class CommentItem(
    @SerializedName("commentId") val commentId: Long? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("time") val time: Long? = null,
    @SerializedName("timeStr") val timeStr: String? = null,
    @SerializedName("likedCount") val likedCount: Int? = null,
    @SerializedName("liked") val liked: Boolean? = null,
    @SerializedName("user") val user: CommentUser? = null,
    @SerializedName("beReplied") val beReplied: List<BeReplied> = emptyList(),
    @SerializedName("ipLocation") val ipLocation: IpLocation? = null,
)

data class CommentUser(
    @SerializedName("userId") val userId: Long? = null,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
)

data class BeReplied(
    @SerializedName("content") val content: String? = null,
    @SerializedName("user") val user: CommentUser? = null,
)

data class IpLocation(
    @SerializedName("location") val location: String? = null,
)
