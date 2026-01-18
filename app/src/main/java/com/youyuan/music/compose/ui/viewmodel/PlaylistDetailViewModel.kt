package com.youyuan.music.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.PlaylistApi
import com.youyuan.music.compose.api.apis.SongApi
import com.youyuan.music.compose.api.model.PlaylistDetail
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.data.PlaylistInvalidationBus
import com.youyuan.music.compose.data.PlaylistDetailCache
import com.youyuan.music.compose.data.PlaylistTrackDiff
import com.youyuan.music.compose.data.SongDetailPool
import com.youyuan.music.compose.paging.SongIdsPagingSource
import com.youyuan.music.compose.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val songDetailPool: SongDetailPool,
    private val playlistDetailCache: PlaylistDetailCache,
    private val playlistInvalidationBus: PlaylistInvalidationBus,
) : ViewModel() {

    private val playlistApi: PlaylistApi = apiClient.createService(PlaylistApi::class.java)
    private val songApi: SongApi = apiClient.createService(SongApi::class.java)

    private val _playlist = MutableStateFlow<PlaylistDetail?>(null)
    val playlist: StateFlow<PlaylistDetail?> = _playlist.asStateFlow()

    // LikedSongScreen 同款：Paging + placeholders + 对象池
    private val _songPagingFlow = MutableStateFlow<PagingData<SongDetail>>(PagingData.empty())
    val songPagingFlow: StateFlow<PagingData<SongDetail>> = _songPagingFlow.asStateFlow()

    private val _allSongIds = MutableStateFlow<List<Long>>(emptyList())
    val allSongIds: StateFlow<List<Long>> = _allSongIds.asStateFlow()

    private var pagingJob: Job? = null
    private var currentPlaylistId: Long? = null

    private val _trackDiff = MutableStateFlow<PlaylistTrackDiff?>(null)
    val trackDiff: StateFlow<PlaylistTrackDiff?> = _trackDiff.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // 监听“歌单内容已变更”事件：若当前正在查看该歌单，则强制刷新。
        viewModelScope.launch {
            playlistInvalidationBus.invalidations
                .collectLatest { invalidatedId ->
                    val currentId = currentPlaylistId ?: return@collectLatest
                    if (invalidatedId == currentId) {
                        loadPlaylistDetail(currentId, force = true)
                    }
                }
        }
    }

    fun loadPlaylistDetail(playlistId: Long, force: Boolean = false) {
        if (_loading.value) return
        if (!force && currentPlaylistId == playlistId && _allSongIds.value.isNotEmpty()) return

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            currentPlaylistId = playlistId
            try {
                val oldIds = _allSongIds.value
                val cached = if (!force) playlistDetailCache.get(playlistId) else null
                val detail = if (cached != null && !cached.trackIds.isNullOrEmpty()) {
                    cached
                } else {
                    val resp = playlistApi.getPlaylistDetail(id = playlistId)
                    if (resp.code != 200 || resp.playlist == null) {
                        _playlist.value = null
                        _error.value = "playlist/detail 接口返回异常 code=${resp.code}"
                        _allSongIds.value = emptyList()
                        _songPagingFlow.value = PagingData.empty()
                        return@launch
                    }
                    playlistDetailCache.put(resp.playlist)
                    resp.playlist
                }

                _playlist.value = detail

                val ids = detail.trackIds?.map { it.id }.orEmpty()
                _allSongIds.value = ids

                // 计算增删差异（按顺序保留 added/removed 的原始顺序）
                if (oldIds.isNotEmpty() || ids.isNotEmpty()) {
                    val oldSet = oldIds.toHashSet()
                    val newSet = ids.toHashSet()
                    val added = ids.filter { it !in oldSet }
                    val removed = oldIds.filter { it !in newSet }
                    _trackDiff.value = PlaylistTrackDiff(addedSongIds = added, removedSongIds = removed)
                }

                if (ids.isEmpty()) {
                    _songPagingFlow.value = PagingData.empty()
                    return@launch
                }

                // 如果对象池已完整命中，则无需分页/网络请求，直接喂给列表
                val isFullyCached = ids.all { songDetailPool.contains(it) }
                if (isFullyCached) {
                    _songPagingFlow.value = PagingData.from(songDetailPool.getOrdered(ids))
                    return@launch
                }

                pagingJob?.cancel()
                pagingJob = viewModelScope.launch {
                    Pager(
                        config = PagingConfig(
                            pageSize = 1000,
                            initialLoadSize = 1000,
                            enablePlaceholders = true,
                        ),
                        pagingSourceFactory = {
                            SongIdsPagingSource(
                                songIds = ids,
                                songApi = songApi,
                                songDetailPool = songDetailPool,
                            )
                        }
                    ).flow
                        .cachedIn(viewModelScope)
                        .collect { pagingData ->
                            _songPagingFlow.value = pagingData
                        }
                }
            } catch (e: Exception) {
                _playlist.value = null
                _error.value = e.message ?: "playlist/detail 请求失败"
                Logger.debug("PlaylistDetailViewModel", "loadPlaylistDetail failed: ${e.message}")
                _allSongIds.value = emptyList()
                _songPagingFlow.value = PagingData.empty()
            } finally {
                _loading.value = false
            }
        }
    }

    fun putSongDetailsToPool(details: List<SongDetail>) {
        songDetailPool.putAll(details)
    }
}
