package com.youyuan.music.compose.data.model

import com.youyuan.music.compose.pref.AudioQualityLevel

/**
 * 应用内部统一的歌曲模型。
 * 所有 UI 层、播放器、播放列表均使用此对象，
 * 不再直接依赖 API 层的 Song / SongDetail / AlbumSong / PersonalFmSong。
 */
data class SongItem(
    val id: Long,
    val name: String,
    val artists: List<ArtistInfo>,
    val album: AlbumInfo,
    val duration: Long,
    val fee: SongFee,
    val isAvailable: Boolean,
    val mvId: Long? = null,
    val alias: List<String> = emptyList(),
    val qualities: Map<AudioQualityLevel, QualityInfo> = emptyMap(),
)

data class ArtistInfo(
    val id: Long,
    val name: String,
    val picUrl: String? = null,
)

data class AlbumInfo(
    val id: Long,
    val name: String,
    val picUrl: String? = null,
)

/**
 * 单个音质等级的信息。
 * [url] 为 null 表示尚未通过 SongUrlApi 解析出播放地址。
 */
data class QualityInfo(
    val url: String?,
    val br: Long,
    val size: Long? = null,
    val encodeType: String? = null,
)

enum class SongFee {
    /** 免费 */
    FREE,

    /** VIP 歌曲 */
    VIP,

    /** 数字专辑 */
    DIGITAL,

    /** 其他未知付费类型 */
    OTHER;

    companion object {
        fun fromRaw(raw: Int?): SongFee = when (raw) {
            0, null -> FREE
            1 -> VIP
            4 -> DIGITAL
            else -> OTHER
        }
    }
}
