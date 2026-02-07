package com.youyuan.music.compose.api.apis

import com.youyuan.music.compose.api.model.ArtistAlbumResponse
import com.youyuan.music.compose.api.model.ArtistDetailResponse
import com.youyuan.music.compose.api.model.ArtistTopSongResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ArtistApi {
    @GET("/artist/detail")
    suspend fun getArtistDetail(
        @Query("id") id: Long,
    ): ArtistDetailResponse

    @GET("/artist/top/song")
    suspend fun getArtistTopSongs(
        @Query("id") id: Long,
    ): ArtistTopSongResponse

    @GET("/artist/album")
    suspend fun getArtistAlbums(
        @Query("id") id: Long,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0,
    ): ArtistAlbumResponse
}
