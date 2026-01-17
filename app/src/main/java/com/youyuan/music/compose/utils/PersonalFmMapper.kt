package com.youyuan.music.compose.utils

import com.youyuan.music.compose.api.model.Album
import com.youyuan.music.compose.api.model.Artist
import com.youyuan.music.compose.api.model.PersonalFmSong
import com.youyuan.music.compose.api.model.Song

fun PersonalFmSong.toSong(): Song {
    return Song(
        id = this.id,
        name = this.name,
        artists = this.artists?.map { fmArtist ->
            Artist(
                id = fmArtist.id,
                name = fmArtist.name,
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
        album = this.album?.let { fmAlbum ->
            Album(
                id = fmAlbum.id,
                name = fmAlbum.name,
                artist = null,
                publishTime = null,
                size = null,
                copyrightId = null,
                status = null,
                picId = null,
                mark = null
            )
        },
        duration = this.duration,
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
