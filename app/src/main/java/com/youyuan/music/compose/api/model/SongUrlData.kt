package com.youyuan.music.compose.api.model

import com.google.gson.annotations.SerializedName

data class SongUrlResponse(
    @SerializedName("data")
    val data: List<SongUrlData>? = null,
    @SerializedName("code")
    val code: Long? = null
)

data class SongUrlData(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("br")
    val br: Long? = null,
    @SerializedName("size")
    val size: Long? = null,
    @SerializedName("md5")
    val md5: String? = null,
    @SerializedName("code")
    val code: Long? = null,
    @SerializedName("expi")
    val expi: Long? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("gain")
    val gain: Double? = null,
    @SerializedName("peak")
    val peak: Double? = null,
    @SerializedName("closedGain")
    val closedGain: Double? = null,
    @SerializedName("closedPeak")
    val closedPeak: Double? = null,
    @SerializedName("fee")
    val fee: Long? = null,
    @SerializedName("uf")
    val uf: String? = null,
    @SerializedName("payed")
    val payed: Long? = null,
    @SerializedName("flag")
    val flag: Long? = null,
    @SerializedName("canExtend")
    val canExtend: Boolean? = null,
    @SerializedName("freeTrialInfo")
    val freeTrialInfo: FreeTrialInfo? = null,
    @SerializedName("level")
    val level: String? = null,
    @SerializedName("encodeType")
    val encodeType: String? = null,
    @SerializedName("channelLayout")
    val channelLayout: String? = null,
    @SerializedName("freeTrialPrivilege")
    val freeTrialPrivilege: FreeTrialPrivilege? = null,
    @SerializedName("freeTimeTrialPrivilege")
    val freeTimeTrialPrivilege: FreeTimeTrialPrivilege? = null,
    @SerializedName("urlSource")
    val urlSource: Long? = null,
    @SerializedName("rightSource")
    val rightSource: Long? = null,
    @SerializedName("podcastCtrp")
    val podcastCtrp: String? = null,
    @SerializedName("effectTypes")
    val effectTypes: String? = null,
    @SerializedName("time")
    val time: Long? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("levelConfuse")
    val levelConfuse: String? = null,
    @SerializedName("musicId")
    val musicId: String? = null,
    @SerializedName("accompany")
    val accompany: String? = null,
    @SerializedName("sr")
    val sr: Long? = null,
    @SerializedName("auEff")
    val auEff: Long? = null,
    @SerializedName("immerseType")
    val immerseType: String? = null,
    @SerializedName("beatType")
    val beatType: Long? = null
)

data class FreeTrialInfo(
    @SerializedName("fragmentType")
    val fragmentType: Long? = null,
    @SerializedName("start")
    val start: Long? = null,
    @SerializedName("end")
    val end: Long? = null,
    @SerializedName("algData")
    val algData: AlgData? = null
)

data class AlgData(
    @SerializedName("fragSource")
    val fragSource: String? = null
)

data class FreeTrialPrivilege(
    @SerializedName("resConsumable")
    val resConsumable: Boolean? = null,
    @SerializedName("userConsumable")
    val userConsumable: Boolean? = null,
    @SerializedName("listenType")
    val listenType: String? = null,
    @SerializedName("cannotListenReason")
    val cannotListenReason: String? = null,
    @SerializedName("playReason")
    val playReason: String? = null,
    @SerializedName("freeLimitTagType")
    val freeLimitTagType: String? = null
)

data class FreeTimeTrialPrivilege(
    @SerializedName("resConsumable")
    val resConsumable: Boolean? = null,
    @SerializedName("userConsumable")
    val userConsumable: Boolean? = null,
    @SerializedName("type")
    val type: Long? = null,
    @SerializedName("remainTime")
    val remainTime: Long? = null
)