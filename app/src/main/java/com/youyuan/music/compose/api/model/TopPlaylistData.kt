package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class TopPlaylistResponse(
    @SerializedName("playlists")
    val playlists: List<CategoryPlaylist>? = null,
    @SerializedName("total")
    val total: Long? = null,
    @SerializedName("code")
    val code: Long? = null,
    @SerializedName("more")
    val more: Boolean? = null,
    @SerializedName("cat")
    val cat: String? = null
)

data class CategoryPlaylist(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("trackNumberUpdateTime")
    val trackNumberUpdateTime: Long? = null,
    @SerializedName("status")
    val status: Long? = null,
    @SerializedName("userId")
    val userId: Long? = null,
    @SerializedName("createTime")
    val createTime: Long? = null,
    @SerializedName("updateTime")
    val updateTime: Long? = null,
    @SerializedName("subscribedCount")
    val subscribedCount: Long? = null,
    @SerializedName("trackCount")
    val trackCount: Long? = null,
    @SerializedName("cloudTrackCount")
    val cloudTrackCount: Long? = null,
    @SerializedName("coverImgUrl")
    val coverImgUrl: String? = null,
    @SerializedName("iconImgUrl")
    val iconImgUrl: String? = null,
    @SerializedName("coverImgId")
    val coverImgId: Long? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("tags")
    val tags: List<String>? = null,
    @SerializedName("playCount")
    val playCount: Long? = null,
    @SerializedName("trackUpdateTime")
    val trackUpdateTime: Long? = null,
    @SerializedName("specialType")
    val specialType: Long? = null,
    @SerializedName("totalDuration")
    val totalDuration: Long? = null,
    @SerializedName("creator")
    val creator: PlaylistCreator? = null,
    @SerializedName("tracks")
    val tracks: List<String>? = null,
    @SerializedName("subscribers")
    val subscribers: List<PlaylistSubscriber>? = null,
    @SerializedName("subscribed")
    val subscribed: Boolean? = null,
    @SerializedName("commentThreadId")
    val commentThreadId: String? = null,
    @SerializedName("newImported")
    val newImported: Boolean? = null,
    @SerializedName("adType")
    val adType: Long? = null,
    @SerializedName("highQuality")
    val highQuality: Boolean? = null,
    @SerializedName("privacy")
    val privacy: Long? = null,
    @SerializedName("ordered")
    val ordered: Boolean? = null,
    @SerializedName("anonimous")
    val anonimous: Boolean? = null,
    @SerializedName("coverStatus")
    val coverStatus: Long? = null,
    @SerializedName("recommendInfo")
    val recommendInfo: String? = null,
    @SerializedName("socialPlaylistCover")
    val socialPlaylistCover: String? = null,
    @SerializedName("recommendText")
    val recommendText: String? = null,
    @SerializedName("coverText")
    val coverText: String? = null,
    @SerializedName("relateResType")
    val relateResType: String? = null,
    @SerializedName("relateResId")
    val relateResId: String? = null,
    @SerializedName("tsSongCount")
    val tsSongCount: Long? = null,
    @SerializedName("algType")
    val algType: String? = null,
    @SerializedName("playlistType")
    val playlistType: String? = null,
    @SerializedName("uiPlaylistType")
    val uiPlaylistType: String? = null,
    @SerializedName("originalCoverId")
    val originalCoverId: Long? = null,
    @SerializedName("backgroundImageId")
    val backgroundImageId: Long? = null,
    @SerializedName("backgroundImageUrl")
    val backgroundImageUrl: String? = null,
    @SerializedName("topTrackIds")
    val topTrackIds: List<Long>? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("subTitle")
    val subTitle: String? = null,
    @SerializedName("backgroundText")
    val backgroundText: String? = null,
    @SerializedName("shareCount")
    val shareCount: Long? = null,
    @SerializedName("coverImgId_str")
    val coverImgIdStr: String? = null,
    @SerializedName("alg")
    val alg: String? = null,
    @SerializedName("commentCount")
    val commentCount: Long? = null
)

data class PlaylistCreator(
    @SerializedName("defaultAvatar")
    val defaultAvatar: Boolean? = null,
    @SerializedName("province")
    val province: Long? = null,
    @SerializedName("authStatus")
    val authStatus: Long? = null,
    @SerializedName("followed")
    val followed: Boolean? = null,
    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,
    @SerializedName("accountStatus")
    val accountStatus: Long? = null,
    @SerializedName("gender")
    val gender: Long? = null,
    @SerializedName("city")
    val city: Long? = null,
    @SerializedName("birthday")
    val birthday: Long? = null,
    @SerializedName("userId")
    val userId: Long? = null,
    @SerializedName("userType")
    val userType: Long? = null,
    @SerializedName("nickname")
    val nickname: String? = null,
    @SerializedName("signature")
    val signature: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("detailDescription")
    val detailDescription: String? = null,
    @SerializedName("avatarImgId")
    val avatarImgId: Long? = null,
    @SerializedName("backgroundImgId")
    val backgroundImgId: Long? = null,
    @SerializedName("backgroundUrl")
    val backgroundUrl: String? = null,
    @SerializedName("authority")
    val authority: Long? = null,
    @SerializedName("mutual")
    val mutual: Boolean? = null,
    @SerializedName("expertTags")
    val expertTags: List<String>? = null,
    @SerializedName("experts")
    val experts: Experts? = null,
    @SerializedName("djStatus")
    val djStatus: Long? = null,
    @SerializedName("vipType")
    val vipType: Long? = null,
    @SerializedName("remarkName")
    val remarkName: String? = null,
    @SerializedName("authenticationTypes")
    val authenticationTypes: Long? = null,
    @SerializedName("avatarDetail")
    val avatarDetail: AvatarDetail? = null,
    @SerializedName("backgroundImgIdStr")
    val backgroundImgIdStr: String? = null,
    @SerializedName("avatarImgIdStr")
    val avatarImgIdStr: String? = null,
    @SerializedName("anchor")
    val anchor: Boolean? = null
)

data class AvatarDetail(
    @SerializedName("userType")
    val userType: Long? = null,
    @SerializedName("identityLevel")
    val identityLevel: Long? = null,
    @SerializedName("identityIconUrl")
    val identityIconUrl: String? = null
)

data class PlaylistSubscriber(
    @SerializedName("defaultAvatar")
    val defaultAvatar: Boolean? = null,
    @SerializedName("province")
    val province: Long? = null,
    @SerializedName("authStatus")
    val authStatus: Long? = null,
    @SerializedName("followed")
    val followed: Boolean? = null,
    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,
    @SerializedName("accountStatus")
    val accountStatus: Long? = null,
    @SerializedName("gender")
    val gender: Long? = null,
    @SerializedName("city")
    val city: Long? = null,
    @SerializedName("birthday")
    val birthday: Long? = null,
    @SerializedName("userId")
    val userId: Long? = null,
    @SerializedName("userType")
    val userType: Long? = null,
    @SerializedName("nickname")
    val nickname: String? = null,
    @SerializedName("signature")
    val signature: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("detailDescription")
    val detailDescription: String? = null,
    @SerializedName("avatarImgId")
    val avatarImgId: Long? = null,
    @SerializedName("backgroundImgId")
    val backgroundImgId: Long? = null,
    @SerializedName("backgroundUrl")
    val backgroundUrl: String? = null,
    @SerializedName("authority")
    val authority: Long? = null,
    @SerializedName("mutual")
    val mutual: Boolean? = null,
    @SerializedName("expertTags")
    val expertTags: List<String>? = null,
    @SerializedName("experts")
    val experts: Experts? = null,
    @SerializedName("djStatus")
    val djStatus: Long? = null,
    @SerializedName("vipType")
    val vipType: Long? = null,
    @SerializedName("remarkName")
    val remarkName: String? = null,
    @SerializedName("authenticationTypes")
    val authenticationTypes: Long? = null,
    @SerializedName("avatarDetail")
    val avatarDetail: AvatarDetail? = null,
    @SerializedName("backgroundImgIdStr")
    val backgroundImgIdStr: String? = null,
    @SerializedName("avatarImgIdStr")
    val avatarImgIdStr: String? = null,
    @SerializedName("anchor")
    val anchor: Boolean? = null
)

data class Experts(
    @SerializedName("1")
    val x1: String? = null,
    @SerializedName("2")
    val x2: String? = null

)