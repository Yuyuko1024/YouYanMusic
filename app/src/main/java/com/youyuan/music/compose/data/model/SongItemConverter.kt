package com.youyuan.music.compose.data.model

import android.util.Log
import com.youyuan.music.compose.api.model.AlbumSong
import com.youyuan.music.compose.api.model.AudioQuality
import com.youyuan.music.compose.api.model.PersonalFmSong
import com.youyuan.music.compose.api.model.Song
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.pref.AudioQualityLevel

// ============================================================
// Song (搜索结果) → SongItem
// ============================================================

fun Song.toSongItem(): SongItem = SongItem(
    id = id ?: 0L,
    name = name.orEmpty(),
    artists = artists.orEmpty().map {
        ArtistInfo(id = it.id ?: 0L, name = it.name.orEmpty(), picUrl = it.picUrl)
    },
    album = AlbumInfo(
        id = album?.id ?: 0L,
        name = album?.name.orEmpty(),
        picUrl = null,
    ),
    duration = duration ?: 0L,
    fee = SongFee.fromRaw(fee?.toInt()),
    isAvailable = status != -1L,
    mvId = mvid,
    alias = alias.orEmpty(),
)

// ============================================================
// SongDetail (歌曲详情) → SongItem
// ============================================================

fun SongDetail.toSongItem(): SongItem = SongItem(
    id = id,
    name = name.orEmpty(),
    artists = ar.orEmpty().map {
        ArtistInfo(id = it.id, name = it.name.orEmpty(), picUrl = null)
    },
    album = AlbumInfo(
        id = al?.id ?: 0L,
        name = al?.name.orEmpty(),
        picUrl = al?.picUrl,
    ),
    duration = dt ?: 0L,
    fee = SongFee.fromRaw(fee),
    isAvailable = true,
    mvId = mv,
    alias = buildList {
        alia?.filterNotNull()?.filterNot { it.isBlank() }?.let { addAll(it) }
        tns?.filterNotNull()?.filterNot { it.isBlank() }?.let { addAll(it) }
    },
).also {
    Log.d("SongItemConverter", "SongDetail.toSongItem: id=$id, qualities=${it.qualities.size} (no h/m/l/sq/hr on SongDetail)")
}

// ============================================================
// AlbumSong (专辑歌曲) → SongItem
// 包含内嵌音质元数据（不含 URL，需后续通过 SongUrlApi 解析）
// ============================================================

fun AlbumSong.toSongItem(): SongItem = SongItem(
    id = id ?: 0L,
    name = name.orEmpty(),
    artists = ar.orEmpty().map {
        ArtistInfo(id = it.id ?: 0L, name = it.name.orEmpty(), picUrl = null)
    },
    album = AlbumInfo(
        id = al?.id ?: 0L,
        name = al?.name.orEmpty(),
        picUrl = null,
    ),
    duration = dt ?: 0L,
    fee = SongFee.fromRaw(fee?.toInt()),
    isAvailable = st != -1L,
    mvId = mv,
    alias = buildList {
        alia?.let { addAll(it) }
        tns?.let { addAll(it) }
    },
    qualities = buildMap {
        h?.let { put(AudioQualityLevel.HIGHER, it.toQualityInfo()) }
        m?.let { put(AudioQualityLevel.EXHIGH, it.toQualityInfo()) }
        sq?.let { put(AudioQualityLevel.LOSSLESS, it.toQualityInfo()) }
        hr?.let { put(AudioQualityLevel.HIRES, it.toQualityInfo()) }
        l?.let { put(AudioQualityLevel.STANDARD, it.toQualityInfo()) }
    },
)

// ============================================================
// PersonalFmSong (私人 FM) → SongItem
// ============================================================

fun PersonalFmSong.toSongItem(): SongItem = SongItem(
    id = id,
    name = name.orEmpty(),
    artists = artists.orEmpty().map {
        ArtistInfo(id = it.id, name = it.name.orEmpty(), picUrl = null)
    },
    album = AlbumInfo(
        id = album?.id ?: 0L,
        name = album?.name.orEmpty(),
        picUrl = album?.picUrl,
    ),
    duration = duration ?: 0L,
    fee = SongFee.fromRaw(fee),
    isAvailable = true,
    mvId = mv,
)

// ============================================================
// AudioQuality (专辑内嵌音质) → QualityInfo
// ============================================================

internal fun AudioQuality.toQualityInfo(): QualityInfo =
    QualityInfo(
        url = null,
        br = br ?: 0L,
        size = size,
        encodeType = null,
    )
