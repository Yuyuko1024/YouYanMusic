package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class AlbumDetailResponse(
    @SerializedName("resourceState")
    val resourceState: Boolean? = null,
    @SerializedName("songs")
    val songs: List<AlbumSong>? = null,
    @SerializedName("code")
    val code: Long? = null,
    @SerializedName("album")
    val album: AlbumDetail? = null
)

data class AlbumSong(
    @SerializedName("rtUrls")
    val rtUrls: List<String>? = null,
    @SerializedName("ar")
    val ar: List<SongArtist>? = null,
    @SerializedName("al")
    val al: SongAlbum? = null,
    @SerializedName("st")
    val st: Long? = null,
    @SerializedName("noCopyrightRcmd")
    val noCopyrightRcmd: NoCopyrightRcmd? = null,
    @SerializedName("songJumpInfo")
    val songJumpInfo: Any? = null,
    @SerializedName("djId")
    val djId: Long? = null,
    @SerializedName("no")
    val no: Long? = null,
    @SerializedName("fee")
    val fee: Long? = null,
    @SerializedName("mv")
    val mv: Long? = null,
    @SerializedName("cd")
    val cd: String? = null,
    @SerializedName("t")
    val t: Long? = null,
    @SerializedName("v")
    val v: Long? = null,
    @SerializedName("rtUrl")
    val rtUrl: String? = null,
    @SerializedName("ftype")
    val ftype: Long? = null,
    @SerializedName("rtype")
    val rtype: Long? = null,
    @SerializedName("rurl")
    val rurl: String? = null,
    @SerializedName("pst")
    val pst: Long? = null,
    @SerializedName("alia")
    val alia: List<String>? = null,
    @SerializedName("pop")
    val pop: Long? = null,
    @SerializedName("rt")
    val rt: String? = null,
    @SerializedName("mst")
    val mst: Long? = null,
    @SerializedName("cp")
    val cp: Long? = null,
    @SerializedName("crbt")
    val crbt: String? = null,
    @SerializedName("cf")
    val cf: String? = null,
    @SerializedName("dt")
    val dt: Long? = null,
    @SerializedName("h")
    val h: AudioQuality? = null,
    @SerializedName("sq")
    val sq: AudioQuality? = null,
    @SerializedName("hr")
    val hr: AudioQuality? = null,
    @SerializedName("l")
    val l: AudioQuality? = null,
    @SerializedName("a")
    val a: String? = null,
    @SerializedName("m")
    val m: AudioQuality? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("tns")
    val tns: List<String>? = null,
    @SerializedName("privilege")
    val privilege: SongPrivilege? = null
)

data class SongArtist(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("alia")
    val alia: List<String>? = null
)

data class SongAlbum(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("pic_str")
    val picStr: String? = null,
    @SerializedName("pic")
    val pic: Long? = null
)

data class NoCopyrightRcmd(
    @SerializedName("type")
    val type: Long? = null,
    @SerializedName("typeDesc")
    val typeDesc: String? = null,
    @SerializedName("songId")
    val songId: String? = null,
    @SerializedName("thirdPartySong")
    val thirdPartySong: String? = null,
    @SerializedName("expInfo")
    val expInfo: String? = null
)

data class AudioQuality(
    @SerializedName("br")
    val br: Long? = null,
    @SerializedName("fid")
    val fid: Long? = null,
    @SerializedName("size")
    val size: Long? = null,
    @SerializedName("vd")
    val vd: Long? = null,
    @SerializedName("sr")
    val sr: Long? = null
)

data class SongPrivilege(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("fee")
    val fee: Long? = null,
    @SerializedName("payed")
    val payed: Long? = null,
    @SerializedName("st")
    val st: Long? = null,
    @SerializedName("pl")
    val pl: Long? = null,
    @SerializedName("dl")
    val dl: Long? = null,
    @SerializedName("sp")
    val sp: Long? = null,
    @SerializedName("cp")
    val cp: Long? = null,
    @SerializedName("subp")
    val subp: Long? = null,
    @SerializedName("cs")
    val cs: Boolean? = null,
    @SerializedName("maxbr")
    val maxbr: Long? = null,
    @SerializedName("fl")
    val fl: Long? = null,
    @SerializedName("toast")
    val toast: Boolean? = null,
    @SerializedName("flag")
    val flag: Long? = null,
    @SerializedName("preSell")
    val preSell: Boolean? = null,
    @SerializedName("playMaxbr")
    val playMaxbr: Long? = null,
    @SerializedName("downloadMaxbr")
    val downloadMaxbr: Long? = null,
    @SerializedName("maxBrLevel")
    val maxBrLevel: String? = null,
    @SerializedName("playMaxBrLevel")
    val playMaxBrLevel: String? = null,
    @SerializedName("downloadMaxBrLevel")
    val downloadMaxBrLevel: String? = null,
    @SerializedName("plLevel")
    val plLevel: String? = null,
    @SerializedName("dlLevel")
    val dlLevel: String? = null,
    @SerializedName("flLevel")
    val flLevel: String? = null,
    @SerializedName("rscl")
    val rscl: String? = null,
    @SerializedName("freeTrialPrivilege")
    val freeTrialPrivilege: FreeTrialPrivilege? = null,
    @SerializedName("rightSource")
    val rightSource: Long? = null,
    @SerializedName("chargeInfoList")
    val chargeInfoList: List<ChargeInfo>? = null,
    @SerializedName("code")
    val code: Long? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("plLevels")
    val plLevels: String? = null,
    @SerializedName("dlLevels")
    val dlLevels: String? = null,
    @SerializedName("ignoreCache")
    val ignoreCache: String? = null,
    @SerializedName("bd")
    val bd: String? = null
)

data class ChargeInfo(
    @SerializedName("rate")
    val rate: Long? = null,
    @SerializedName("chargeUrl")
    val chargeUrl: String? = null,
    @SerializedName("chargeMessage")
    val chargeMessage: String? = null,
    @SerializedName("chargeType")
    val chargeType: Long? = null
)

data class AlbumDetail(
    @SerializedName("songs")
    val songs: List<String>? = null,
    @SerializedName("paid")
    val paid: Boolean? = null,
    @SerializedName("onSale")
    val onSale: Boolean? = null,
    @SerializedName("mark")
    val mark: Long? = null,
    @SerializedName("awardTags")
    val awardTags: String? = null,
    @SerializedName("displayTags")
    val displayTags: String? = null,
    @SerializedName("artists")
    val artists: List<AlbumArtist>? = null,
    @SerializedName("copyrightId")
    val copyrightId: Long? = null,
    @SerializedName("picId")
    val picId: Long? = null,
    @SerializedName("artist")
    val artist: AlbumArtist? = null,
    @SerializedName("briefDesc")
    val briefDesc: String? = null,
    @SerializedName("publishTime")
    val publishTime: Long? = null,
    @SerializedName("company")
    val company: String? = null,
    @SerializedName("picUrl")
    val picUrl: String? = null,
    @SerializedName("commentThreadId")
    val commentThreadId: String? = null,
    @SerializedName("blurPicUrl")
    val blurPicUrl: String? = null,
    @SerializedName("companyId")
    val companyId: Long? = null,
    @SerializedName("pic")
    val pic: Long? = null,
    @SerializedName("alias")
    val alias: List<String>? = null,
    @SerializedName("status")
    val status: Long? = null,
    @SerializedName("subType")
    val subType: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("tags")
    val tags: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("size")
    val size: Long? = null,
    @SerializedName("picId_str")
    val picIdStr: String? = null,
    @SerializedName("info")
    val info: AlbumInfo? = null
)

data class AlbumArtist(
    @SerializedName("img1v1Id")
    val img1v1Id: Long? = null,
    @SerializedName("topicPerson")
    val topicPerson: Long? = null,
    @SerializedName("picId")
    val picId: Long? = null,
    @SerializedName("musicSize")
    val musicSize: Long? = null,
    @SerializedName("albumSize")
    val albumSize: Long? = null,
    @SerializedName("briefDesc")
    val briefDesc: String? = null,
    @SerializedName("picUrl")
    val picUrl: String? = null,
    @SerializedName("img1v1Url")
    val img1v1Url: String? = null,
    @SerializedName("followed")
    val followed: Boolean? = null,
    @SerializedName("trans")
    val trans: String? = null,
    @SerializedName("alias")
    val alias: List<String>? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("img1v1Id_str")
    val img1v1IdStr: String? = null
)

data class AlbumInfo(
    @SerializedName("commentThread")
    val commentThread: CommentThread? = null,
    @SerializedName("latestLikedUsers")
    val latestLikedUsers: String? = null,
    @SerializedName("liked")
    val liked: Boolean? = null,
    @SerializedName("comments")
    val comments: String? = null,
    @SerializedName("resourceType")
    val resourceType: Long? = null,
    @SerializedName("resourceId")
    val resourceId: Long? = null,
    @SerializedName("commentCount")
    val commentCount: Long? = null,
    @SerializedName("likedCount")
    val likedCount: Long? = null,
    @SerializedName("shareCount")
    val shareCount: Long? = null,
    @SerializedName("threadId")
    val threadId: String? = null
)

data class CommentThread(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("resourceInfo")
    val resourceInfo: ResourceInfo? = null,
    @SerializedName("resourceType")
    val resourceType: Long? = null,
    @SerializedName("commentCount")
    val commentCount: Long? = null,
    @SerializedName("likedCount")
    val likedCount: Long? = null,
    @SerializedName("shareCount")
    val shareCount: Long? = null,
    @SerializedName("hotCount")
    val hotCount: Long? = null,
    @SerializedName("latestLikedUsers")
    val latestLikedUsers: String? = null,
    @SerializedName("resourceId")
    val resourceId: Long? = null,
    @SerializedName("resourceOwnerId")
    val resourceOwnerId: Long? = null,
    @SerializedName("resourceTitle")
    val resourceTitle: String? = null
)

data class ResourceInfo(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("userId")
    val userId: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("imgUrl")
    val imgUrl: String? = null,
    @SerializedName("creator")
    val creator: String? = null,
    @SerializedName("encodedId")
    val encodedId: String? = null,
    @SerializedName("subTitle")
    val subTitle: String? = null,
    @SerializedName("webUrl")
    val webUrl: String? = null
)