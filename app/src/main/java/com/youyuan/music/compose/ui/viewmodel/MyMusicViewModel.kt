package com.youyuan.music.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.ProfileApi
import com.youyuan.music.compose.api.apis.SongApi
import com.youyuan.music.compose.api.model.Song
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.data.SongDetailPool
import com.youyuan.music.compose.paging.SongIdsPagingSource
import com.youyuan.music.compose.utils.toSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyMusicViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val songDetailPool: SongDetailPool,
) : ViewModel() {

    private val songApi = apiClient.createService(SongApi::class.java)
    private val profileApi = apiClient.createService(ProfileApi::class.java)

    // 使用 StateFlow 暴露 PagingData
    private val _songPagingFlow = MutableStateFlow<PagingData<SongDetail>>(PagingData.empty())
    val songPagingFlow = _songPagingFlow.asStateFlow()

    // 缓存所有歌曲 ID，用于播放整个列表
    private val _allSongIds = MutableStateFlow<List<Long>>(emptyList())
    val allSongIds = _allSongIds.asStateFlow()

    private var likedSongsJob: Job? = null
    private var likedUid: String? = null
    private var likedLoadedOnce: Boolean = false

    fun putSongDetailsToPool(details: List<SongDetail>) {
        songDetailPool.putAll(details)
    }

    fun loadLikedSongs(uid: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && likedLoadedOnce && likedUid == uid) return

        likedUid = uid
        likedLoadedOnce = true

        likedSongsJob?.cancel()
        likedSongsJob = viewModelScope.launch {
            try {
                // 1. 先获取所有 ID
                val likeList = profileApi.getLikelistById(uid)
                val ids = likeList?.ids ?: emptyList()

                // 缓存全部 ID（用于播放整个列表/构建播放队列）
                _allSongIds.value = ids

                if (ids.isNotEmpty()) {
                    // 如果对象池已完整命中，则无需分页/网络请求，直接喂给列表
                    val isFullyCached = ids.all { songDetailPool.contains(it) }
                    if (isFullyCached) {
                        _songPagingFlow.value = PagingData.from(songDetailPool.getOrdered(ids))
                        return@launch
                    }

                    // 2. 创建 Pager
                    Pager(
                        config = PagingConfig(
                            pageSize = 20, // 每次请求20首歌
                            enablePlaceholders = true, // 启用占位符！
                            initialLoadSize = 20 // 初始加载数量
                        ),
                        pagingSourceFactory = { SongIdsPagingSource(ids, songApi, songDetailPool) }
                    ).flow
                        .cachedIn(viewModelScope)
                        .collect { pagingData ->
                            _songPagingFlow.value = pagingData
                        }
                } else {
                    _songPagingFlow.value = PagingData.empty()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * 根据歌曲ID列表批量获取歌曲详情
     * @param songIds 歌曲ID列表
     * @return 歌曲列表（转换为 Song 类型）
     */
    suspend fun getSongsByIds(songIds: List<Long>): List<Song> {
        if (songIds.isEmpty()) return emptyList()

        return try {
            val missing = songIds.filterNot { songDetailPool.contains(it) }
            if (missing.isNotEmpty()) {
                val response = songApi.getSongDetails(missing.joinToString(","))
                val songDetails = response.songs ?: emptyList()
                songDetailPool.putAll(songDetails)
            }

            // 保证按 songIds 顺序返回
            songIds.mapNotNull { songDetailPool.get(it)?.toSong() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取已缓存的歌曲（如果存在）
     */
    fun getCachedSong(songId: Long): Song? {
        return songDetailPool.get(songId)?.toSong()
    }
}