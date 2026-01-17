package com.youyuan.music.compose.api.model

data class LyricsData(
    val sgc: Boolean = false,
    val sfy: Boolean = false,
    val qfy: Boolean = false,
    val lrc: LrcData,
    val klyric: LrcData,
    val tlyric: LrcData,
    val romalrc: LrcData,
    val code: Int
)

data class LrcData(
    val version: Int,
    val lyric: String? = ""
)