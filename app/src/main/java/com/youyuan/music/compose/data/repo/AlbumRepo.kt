package com.youyuan.music.compose.data.repo

import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.AlbumApi
import com.youyuan.music.compose.api.model.AlbumDetail
import com.youyuan.music.compose.api.model.AlbumSong
import com.youyuan.music.compose.data.SongDetailPool
import com.youyuan.music.compose.data.model.AlbumInfo
import com.youyuan.music.compose.data.model.ArtistInfo
import com.youyuan.music.compose.data.model.SongFee
import com.youyuan.music.compose.data.model.SongItem
import com.youyuan.music.compose.data.model.toQualityInfo
import com.youyuan.music.compose.pref.AudioQualityLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 专辑数据仓库：持有 AlbumApi，返回统一 SongItem。
 */
@Singleton
class AlbumRepo @Inject constructor(
    private val apiClient: ApiClient,
    private val songDetailPool: SongDetailPool,
) {
    private val albumApi: AlbumApi by lazy { apiClient.createService(AlbumApi::class.java) }

    private fun throwIfRisk(code: Long?) {
        if (code == -462L) throw RepoRiskException("检测到您的网络环境存在风险，请稍后再试")
    }

    data class AlbumResult(
        val album: AlbumDetail?,
        val songs: List<SongItem>,
    )

    /**
     * 获取专辑详情及歌曲列表（全部转为 SongItem）。
     */
    suspend fun getAlbumDetail(albumId: Long): AlbumResult = withContext(Dispatchers.IO) {
        val response = albumApi.getAlbumDetails(albumId)
        throwIfRisk(response.code)
        val detail = response.album
        val albumPicUrl = detail?.picUrl
        val songs = response.songs.orEmpty().map { it.toSongItem(albumPicUrl) }
        songDetailPool.putAll(songs)
        AlbumResult(album = detail, songs = songs)
    }

    private fun AlbumSong.toSongItem(albumPicUrl: String?): SongItem = SongItem(
        id = id ?: 0L,
        name = name.orEmpty(),
        artists = ar.orEmpty().map {
            ArtistInfo(id = it.id ?: 0L, name = it.name.orEmpty())
        },
        album = AlbumInfo(
            id = al?.id ?: 0L,
            name = al?.name.orEmpty(),
            picUrl = albumPicUrl,
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
}
