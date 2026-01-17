package com.youyuan.music.compose.utils

import com.youyuan.music.compose.api.model.Album
import com.youyuan.music.compose.api.model.Artist
import com.youyuan.music.compose.api.model.Song
import com.youyuan.music.compose.api.model.SongDetail

/**
 * 将 SongDetail 转换为 Song
 */
fun SongDetail.toSong(): Song {
    return Song(
        id = this.id,
        name = this.name,
        artists = this.ar?.map { artistDetail ->
            Artist(
                id = artistDetail.id,
                name = artistDetail.name,
                picUrl = null,
                alias = null,
                albumSize = null,
                musicSize = null,
                picId = null,
                fansGroup = null,
                recommendText = null,
                appendRecText = null,
                fansSize = null,
                img1v1Url = null,
                img1v1 = null,
                trans = null
            )
        },
        album = this.al?.let { albumDetail ->
            Album(
                id = albumDetail.id,
                name = albumDetail.name,
                artist = null,
                publishTime = null,
                size = null,
                copyrightId = null,
                status = null,
                picId = null,
                mark = null
            )
        },
        duration = this.dt,
        copyrightId = null,
        status = null,
        alias = null,
        rtype = null,
        ftype = null,
        transNames = null,
        mvid = this.mv,
        fee = this.fee?.toLong(),
        rUrl = null,
        mark = null
    )
}