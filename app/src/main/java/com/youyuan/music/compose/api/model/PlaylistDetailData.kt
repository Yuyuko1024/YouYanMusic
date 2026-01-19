package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

/**
 * /playlist/detail 最小模型：只保留歌单页渲染与播放所需字段。
 */
data class PlaylistDetailResponse(
    @SerializedName("code")
    val code: Int? = null,
    // 该接口除 playlist 外还可能携带一些扩展字段，这里按需补齐常用项。
    @SerializedName("relatedVideos")
    val relatedVideos: Any? = null,
    @SerializedName("playlist")
    val playlist: PlaylistDetail? = null,
    @SerializedName("urls")
    val urls: Any? = null,
    // 与 /song/detail 同款：这里只保留项目中已使用的最小 Privilege 字段集合。
    @SerializedName("privileges")
    val privileges: List<Privilege>? = null,
    @SerializedName("sharedPrivilege")
    val sharedPrivilege: Any? = null,
    @SerializedName("resEntrance")
    val resEntrance: Any? = null,
    @SerializedName("fromUsers")
    val fromUsers: Any? = null,
    @SerializedName("fromUserCount")
    val fromUserCount: Int? = null,
    @SerializedName("songFromUsers")
    val songFromUsers: Any? = null,
)

data class PlaylistDetail(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("userId")
    val userId: Long? = null,

    @SerializedName("coverImgId")
    val coverImgId: Long? = null,
    @SerializedName("coverImgUrl")
    val coverImgUrl: String? = null,
    @SerializedName("coverImgId_str")
    val coverImgIdStr: String? = null,

    @SerializedName("createTime")
    val createTime: Long? = null,
    @SerializedName("updateTime")
    val updateTime: Long? = null,
    @SerializedName("trackUpdateTime")
    val trackUpdateTime: Long? = null,
    @SerializedName("trackNumberUpdateTime")
    val trackNumberUpdateTime: Long? = null,

    @SerializedName("commentThreadId")
    val commentThreadId: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("tags")
    val tags: List<String>? = null,
    @SerializedName("updateFrequency")
    val updateFrequency: String? = null,

    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("privacy")
    val privacy: Int? = null,
    @SerializedName("ordered")
    val ordered: Boolean? = null,
    @SerializedName("highQuality")
    val highQuality: Boolean? = null,
    @SerializedName("newImported")
    val newImported: Boolean? = null,
    @SerializedName("specialType")
    val specialType: Int? = null,
    @SerializedName("adType")
    val adType: Int? = null,

    @SerializedName("trackCount")
    val trackCount: Int? = null,
    @SerializedName("playCount")
    val playCount: Long? = null,
    @SerializedName("subscribedCount")
    val subscribedCount: Long? = null,
    @SerializedName("shareCount")
    val shareCount: Long? = null,
    @SerializedName("commentCount")
    val commentCount: Long? = null,
    @SerializedName("cloudTrackCount")
    val cloudTrackCount: Long? = null,

    @SerializedName("subscribed")
    val subscribed: Boolean? = null,
    @SerializedName("creator")
    val creator: PlaylistCreator? = null,
    @SerializedName("subscribers")
    val subscribers: List<PlaylistSubscriber>? = null,

    @SerializedName("tracks")
    val tracks: List<SongDetail>? = null,

    // /playlist/detail 会返回完整 trackIds；用于构建全量播放队列（不必先把所有 SongDetail 拉完）。
    @SerializedName("trackIds")
    val trackIds: List<PlaylistTrackId>? = null,

    @SerializedName("playlistType")
    val playlistType: String? = null,
    @SerializedName("trialMode")
    val trialMode: Int? = null,
)

data class PlaylistTrackId(
    @SerializedName("id")
    val id: Long,

    // 以下字段用于增量更新/推荐信息（按 JSON 补齐，当前业务主要用 id）。
    @SerializedName("v")
    val v: Int? = null,
    @SerializedName("t")
    val t: Int? = null,
    @SerializedName("at")
    val at: Long? = null,
    @SerializedName("uid")
    val uid: Long? = null,
    @SerializedName("alg")
    val alg: String? = null,
    @SerializedName("rcmdReason")
    val rcmdReason: String? = null,
    @SerializedName("rcmdReasonTitle")
    val rcmdReasonTitle: String? = null,
)
