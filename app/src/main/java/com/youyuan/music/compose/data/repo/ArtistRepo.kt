package com.youyuan.music.compose.data.repo

import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.ArtistApi
import com.youyuan.music.compose.api.model.AlbumDetail
import com.youyuan.music.compose.api.model.ArtistProfile
import com.youyuan.music.compose.data.SongDetailPool
import com.youyuan.music.compose.data.model.SongItem
import com.youyuan.music.compose.data.model.toSongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 艺人数据仓库：持有 ArtistApi，返回统一 SongItem。
 */
@Singleton
class ArtistRepo @Inject constructor(
    private val apiClient: ApiClient,
    private val songDetailPool: SongDetailPool,
) {
    private val artistApi: ArtistApi by lazy { apiClient.createService(ArtistApi::class.java) }

    private fun throwIfRisk(code: Int) {
        if (code == -462) throw RepoRiskException("检测到您的网络环境存在风险，请稍后再试")
    }

    /**
     * 获取艺人详情。
     * @throws IllegalStateException 当 code != 200
     */
    suspend fun getArtistDetail(artistId: Long): ArtistProfile = withContext(Dispatchers.IO) {
        val resp = artistApi.getArtistDetail(id = artistId)
        if (resp.code != 200) {
            throw IllegalStateException(
                resp.message ?: "artist/detail 接口返回异常 code=${resp.code}"
            )
        }
        resp.data?.artist ?: throw IllegalStateException("艺人数据为空")
    }

    /**
     * 获取艺人热门歌曲（转换为 SongItem 并写入池）。
     * @throws IllegalStateException 当 code != 200
     */
    suspend fun getArtistTopSongs(artistId: Long): List<SongItem> = withContext(Dispatchers.IO) {
        val resp = artistApi.getArtistTopSongs(id = artistId)
        if (resp.code != 200) {
            throw IllegalStateException("artist/top/song 接口返回异常 code=${resp.code}")
        }
        val items = resp.songs.orEmpty().take(50).map { it.toSongItem() }
        songDetailPool.putAll(items)
        items
    }

    /**
     * 分页获取艺人专辑。每次调用取一页；调用方循环直到 hasMore=false。
     * @throws IllegalStateException 当 code != 200
     */
    suspend fun getArtistAlbums(
        artistId: Long,
        limit: Int = 30,
        offset: Int = 0,
    ): ArtistAlbumsPage = withContext(Dispatchers.IO) {
        val resp = artistApi.getArtistAlbums(id = artistId, limit = limit, offset = offset)
        if (resp.code != 200) {
            throw IllegalStateException("artist/album 接口返回异常 code=${resp.code}")
        }
        ArtistAlbumsPage(
            albums = resp.hotAlbums.orEmpty(),
            hasMore = resp.more == true,
        )
    }

    data class ArtistAlbumsPage(
        val albums: List<AlbumDetail>,
        val hasMore: Boolean,
    )
}
