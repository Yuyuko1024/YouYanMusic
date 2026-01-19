package com.youyuan.music.compose.api.model

data class LyricsData(
    val sgc: Boolean = false,
    val sfy: Boolean = false,
    val qfy: Boolean = false,
    // 服务端字段可能缺失/为 null（例如 romalrc 经常不存在），因此这里必须可空以避免解析崩溃
    val lrc: LrcData? = null,
    val klyric: LrcData? = null,
    val tlyric: LrcData? = null,
    val romalrc: LrcData? = null,
    val code: Int
)

data class LrcData(
    val version: Int = 0,
    val lyric: String? = ""
)