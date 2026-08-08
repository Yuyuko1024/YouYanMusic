package com.youyuan.music.compose.data.repo

import android.util.Log
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.AlbumApi
import com.youyuan.music.compose.api.apis.CommentApi
import com.youyuan.music.compose.api.apis.SongApi
import com.youyuan.music.compose.api.apis.SongLikeApi
import com.youyuan.music.compose.api.apis.SongUrlApi
import com.youyuan.music.compose.api.model.SongLikeActionResponse
import com.youyuan.music.compose.data.SongDetailPool
import com.youyuan.music.compose.data.model.SongItem
import com.youyuan.music.compose.data.model.toSongItem
import com.youyuan.music.compose.pref.AudioQualityLevel
import com.youyuan.music.compose.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

class RepoRiskException(message: String) : RuntimeException(message)

/**
 * 歌曲数据仓库：持有所有歌曲相关 API + SongDetailPool。
 * PVM 中除 lyricsApi 外的所有 API 调用全部下沉到此。
 */
@Singleton
class SongRepo @Inject constructor(
    private val apiClient: ApiClient,
    private val songDetailPool: SongDetailPool,
) {
    companion object {
        private const val TAG = "SongRepo"
    }

    private val songUrlApi: SongUrlApi by lazy { apiClient.createService(SongUrlApi::class.java) }
    private val albumApi: AlbumApi by lazy { apiClient.createService(AlbumApi::class.java) }
    private val songApi: SongApi by lazy { apiClient.createService(SongApi::class.java) }
    private val commentApi: CommentApi by lazy { apiClient.createService(CommentApi::class.java) }
    private val songLikeApi: SongLikeApi by lazy { apiClient.createService(SongLikeApi::class.java) }

    private fun throwIfRisk(code: Int?) {
        if (code == -462) throw RepoRiskException("检测到您的网络环境存在风险，请稍后再试")
    }

    private fun throwIfRisk(code: Long?) {
        if (code == -462L) throw RepoRiskException("检测到您的网络环境存在风险，请稍后再试")
    }

    /** 获取歌曲播放 URL */
    suspend fun getSongUrl(songId: Long, qualityLevel: String): String? = withContext(Dispatchers.IO) {
        val resp = songUrlApi.getSongUrl(songIds = songId.toString(), qualityLevel = qualityLevel)
        throwIfRisk(resp.code)
        resp.data?.firstOrNull()?.url
    }

    /** 获取某首歌曲在所有候选音质下的可用性 */
    suspend fun checkAllQualities(songId: Long): List<AudioQualityResult> = withContext(Dispatchers.IO) {
        AudioQualityLevel.probeOrder().map { q ->
            try {
                val resp = songUrlApi.getSongUrl(songIds = songId.toString(), qualityLevel = q.level)
                throwIfRisk(resp.code)
                val item = resp.data?.firstOrNull()
                val urlOk = !item?.url.isNullOrBlank()
                val actual = item?.level
                AudioQualityResult(
                    requested = q,
                    available = urlOk && actual?.equals(q.level, ignoreCase = true) == true,
                    actualLevel = actual,
                    br = item?.br,
                    encodeType = item?.encodeType,
                    message = item?.message,
                )
            } catch (e: RepoRiskException) {
                throw e
            } catch (e: Exception) {
                AudioQualityResult(q, false, message = e.message)
            }
        }
    }

    /** 获取歌曲详情：批量请求，结果写入 SongDetailPool，返回 id→SongItem 映射 */
    private suspend fun fetchAndCacheSongItems(ids: List<Long>): Map<Long, SongItem> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyMap()
        val resp = songApi.getSongDetails(ids.joinToString(","))
        throwIfRisk(resp.code)
        val items = resp.songs.orEmpty().map { it.toSongItem() }
        songDetailPool.putAll(items)
        items.associateBy { it.id }
    }

    /** 获取单首 SongItem */
    private suspend fun fetchAndCacheSongItem(songId: Long): SongItem? = withContext(Dispatchers.IO) {
        val resp = songApi.getSongDetails(songId.toString())
        throwIfRisk(resp.code)
        val items = resp.songs.orEmpty().map { it.toSongItem() }
        songDetailPool.putAll(items)
        items.firstOrNull()
    }

    /** 批量获取 SongItem（写入池） */
    suspend fun getSongItems(ids: List<Long>): List<SongItem> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        Log.i(TAG, "getSongItems: requesting ${ids.size} songs")
        val itemMap = fetchAndCacheSongItems(ids)
        val result = ids.mapNotNull { itemMap[it] }
        Log.i(TAG, "getSongItems: returned ${result.size}/${ids.size}")
        result
    }

    /** 获取单首 SongItem */
    suspend fun getSongItem(songId: Long): SongItem? = withContext(Dispatchers.IO) {
        val item = fetchAndCacheSongItem(songId)
        Log.i(TAG, "getSongItem($songId): ${if (item != null) "found, q=${item.qualities.size}" else "NOT FOUND"}")
        item
    }

    /** 获取专辑封面 URL */
    suspend fun fetchAlbumArt(albumId: Long): String? = withContext(Dispatchers.IO) {
        try {
            albumApi.getAlbumDetails(albumId = albumId).album?.picUrl
        } catch (e: Exception) {
            Logger.warn(TAG, "fetchAlbumArt failed: ${e.message}")
            null
        }
    }

    /** 获取歌曲评论数 */
    suspend fun fetchCommentCount(songId: Long): Int? = withContext(Dispatchers.IO) {
        try {
            val resp = commentApi.getMusicComments(songId = songId, limit = 1, offset = 0)
            throwIfRisk(resp.code)
            if (resp.code != 200) null
            else (resp.total ?: 0).coerceAtLeast(0)
        } catch (e: RepoRiskException) {
            throw e
        } catch (e: Exception) {
            Logger.warn(TAG, "fetchCommentCount failed: ${e.message}")
            null
        }
    }

    /** 检查一批歌曲的喜欢状态，返回已喜欢的 songId 集合 */
    suspend fun checkLiked(ids: List<Long>): Set<Long>? = withContext(Dispatchers.IO) {
        try {
            val resp = songLikeApi.checkSongLike(ids = ids.joinToString(prefix = "[", postfix = "]"))
            if (resp.code != null && resp.code != 200) null
            else resp.likedIds().toSet()
        } catch (e: Exception) {
            Logger.warn(TAG, "checkLiked failed: ${e.message}")
            null
        }
    }

    /** 喜欢一首歌，返回 playlistId（"我喜欢的音乐"歌单 id） */
    suspend fun likeSong(songId: Long): SongLikeActionResponse? = withContext(Dispatchers.IO) {
        try {
            val resp = songLikeApi.likeSong(id = songId)
            if (resp.code != null && resp.code != 200) null else resp
        } catch (e: Exception) {
            Logger.warn(TAG, "likeSong failed: ${e.message}")
            null
        }
    }

    /** 取消喜欢一首歌，返回 playlistId */
    suspend fun unlikeSong(songId: Long): SongLikeActionResponse? = withContext(Dispatchers.IO) {
        try {
            val resp = songLikeApi.unlikeSong(id = songId)
            if (resp.code != null && resp.code != 200) null else resp
        } catch (e: Exception) {
            Logger.warn(TAG, "unlikeSong failed: ${e.message}")
            null
        }
    }

    data class AudioQualityResult(
        val requested: AudioQualityLevel,
        val available: Boolean,
        val actualLevel: String? = null,
        val br: Long? = null,
        val encodeType: String? = null,
        val message: String? = null,
    )
}