package com.youyuan.music.compose.data.repo

import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.PlaylistApi
import com.youyuan.music.compose.api.apis.SongApi
import com.youyuan.music.compose.api.model.PlaylistDetail
import com.youyuan.music.compose.data.model.SongItem
import com.youyuan.music.compose.data.model.toSongItem
import com.youyuan.music.compose.data.PlaylistDetailCache
import com.youyuan.music.compose.data.SongDetailPool
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 歌单数据仓库：持有 apiClient、缓存、对象池，ViewModel 只传 id。
 */
@Singleton
class PlaylistRepo @Inject constructor(
    private val apiClient: ApiClient,
    private val playlistDetailCache: PlaylistDetailCache,
    private val songDetailPool: SongDetailPool,
) {

    private val playlistApi: PlaylistApi by lazy { apiClient.createService(PlaylistApi::class.java) }
    private val songApi: SongApi by lazy { apiClient.createService(SongApi::class.java) }

    /**
     * 获取歌单详情（缓存优先，force=true 跳过缓存）。
     * @return null 表示请求失败或接口返回异常。
     */
    suspend fun fetchPlaylistDetail(playlistId: Long, force: Boolean = false): PlaylistDetail? {
        val cached = if (!force) playlistDetailCache.get(playlistId) else null
        if (cached != null && !cached.trackIds.isNullOrEmpty()) return cached

        val resp = playlistApi.getPlaylistDetail(id = playlistId)
        if (resp.code != 200 || resp.playlist == null) return null
        playlistDetailCache.put(resp.playlist)
        return resp.playlist
    }

    /**
     * 按偏移量加载一批 SongItem（池命中优先，缺失走网络）。
     */
    suspend fun loadSongBatch(allIds: List<Long>, offset: Int, count: Int): List<SongItem> {
        val end = minOf(offset + count, allIds.size)
        val idsToLoad = allIds.subList(offset, end)

        val missingIds = idsToLoad.filterNot { songDetailPool.contains(it) }
        if (missingIds.isNotEmpty()) {
            val resp = songApi.getSongDetails(missingIds.joinToString(","))
            val items = resp.songs.orEmpty().map { it.toSongItem() }
            songDetailPool.putAll(items)
        }

        return songDetailPool.getOrdered(idsToLoad)
    }

    fun allInPool(ids: List<Long>): Boolean = ids.all { songDetailPool.contains(it) }
    fun getFromPool(ids: List<Long>): List<SongItem> = songDetailPool.getOrdered(ids)
    fun putToPool(items: List<SongItem>) = songDetailPool.putAll(items)
}