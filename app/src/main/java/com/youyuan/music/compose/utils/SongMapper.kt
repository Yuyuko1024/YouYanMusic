package com.youyuan.music.compose.utils

import com.youyuan.music.compose.api.model.Song
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.api.model.ArtistDetail
import com.youyuan.music.compose.api.model.SongDetailAlbumData

/**
 * 将搜索结果 Song 转换为 SongDetail（用于播放器播放列表统一域模型）。
 * 说明：SongDetail 的字段更精简，部分信息在搜索模型里不存在时会置空。
 */
fun Song.toSongDetail(): SongDetail {
    fun List<String?>?.toNonBlankDistinctNullable(): List<String?>? {
        val out = this
            .orEmpty()
            .asSequence()
            .mapNotNull { it?.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
        return out.takeIf { it.isNotEmpty() }
    }

    return SongDetail(
        name = this.name,
        id = this.id ?: 0L,
        ar = this.artists
            ?.mapNotNull { a ->
                ArtistDetail(
                    id = a.id ?: 0L,
                    name = a.name,
                )
            }
            ?.takeIf { it.isNotEmpty() },
        al = this.album?.let { album ->
            SongDetailAlbumData(
                id = album.id ?: 0L,
                name = album.name,
                picUrl = null,
            )
        },
        alia = this.alias?.map { it as String? }.toNonBlankDistinctNullable() ?: emptyList(),
        dt = this.duration,
        fee = this.fee?.toInt(),
        mv = this.mvid,
        tns = this.transNames?.map { it as String? }.toNonBlankDistinctNullable() ?: emptyList(),
    )
}