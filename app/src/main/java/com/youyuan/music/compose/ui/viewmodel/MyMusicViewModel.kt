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
import com.youyuan.music.compose.paging.SongIdsPagingSource
import com.youyuan.music.compose.utils.toSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyMusicViewModel @Inject constructor(
    private val apiClient: ApiClient
) : ViewModel() {

    private val songApi = apiClient.createService(SongApi::class.java)
    private val profileApi = apiClient.createService(ProfileApi::class.java)

    // 使用 StateFlow 暴露 PagingData
    private val _songPagingFlow = MutableStateFlow<PagingData<SongDetail>>(PagingData.empty())
    val songPagingFlow = _songPagingFlow.asStateFlow()

    // 缓存所有歌曲 ID，用于播放整个列表
    private val _allSongIds = MutableStateFlow<List<Long>>(emptyList())
    val allSongIds = _allSongIds.asStateFlow()

    // 缓存已加载的歌曲详情（用于快速访问）
    private val _loadedSongs = MutableStateFlow<Map<Long, SongDetail>>(emptyMap())

    fun loadLikedSongs(uid: String) {
        viewModelScope.launch {
            try {
                // 1. 先获取所有 ID
                val likeList = profileApi.getLikelistById(uid)
                val ids = likeList?.ids ?: emptyList()

                // 缓存全部 ID（用于播放整个列表/构建播放队列）
                _allSongIds.value = ids

                if (ids.isNotEmpty()) {
                    // 2. 创建 Pager
                    Pager(
                        config = PagingConfig(
                            pageSize = 20, // 每次请求20首歌
                            enablePlaceholders = true, // 启用占位符！
                            initialLoadSize = 20 // 初始加载数量
                        ),
                        pagingSourceFactory = { SongIdsPagingSource(ids, songApi) }
                    ).flow
                        .cachedIn(viewModelScope)
                        .collect { pagingData ->
                            _songPagingFlow.value = pagingData
                        }
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
            val idsString = songIds.joinToString(",")
            val response = songApi.getSongDetails(idsString)
            val songDetails = response.songs ?: emptyList()

            // 缓存加载的歌曲
            val newCache = _loadedSongs.value.toMutableMap()
            songDetails.forEach { detail ->
                newCache[detail.id] = detail
            }
            _loadedSongs.value = newCache

            songDetails.map { it.toSong() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取已缓存的歌曲（如果存在）
     */
    fun getCachedSong(songId: Long): Song? {
        return _loadedSongs.value[songId]?.toSong()
    }
}