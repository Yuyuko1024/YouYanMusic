package com.youyuan.music.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.model.PlaylistDetail
import com.youyuan.music.compose.data.model.SongItem
import com.youyuan.music.compose.data.repo.PlaylistRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepo: PlaylistRepo,
) : ViewModel() {

    data class SongListUiState(
        val songs: List<SongItem> = emptyList(),
        val totalCount: Int = 0,
        val isLoading: Boolean = false,
    )

    companion object {
        private const val BATCH_SIZE = 500
    }

    private val _playlist = MutableStateFlow<PlaylistDetail?>(null)
    val playlist: StateFlow<PlaylistDetail?> = _playlist.asStateFlow()

    private val _songListUiState = MutableStateFlow(SongListUiState())
    val songListUiState: StateFlow<SongListUiState> = _songListUiState.asStateFlow()

    private val _allSongIds = MutableStateFlow<List<Long>>(emptyList())
    val allSongIds: StateFlow<List<Long>> = _allSongIds.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentPlaylistId: Long? = null

    fun loadPlaylistDetail(playlistId: Long, force: Boolean = false) {
        if (_loading.value) return
        if (!force && currentPlaylistId == playlistId && _allSongIds.value.isNotEmpty()) return

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _songListUiState.value = SongListUiState(isLoading = true)
            currentPlaylistId = playlistId

            try {
                val detail = playlistRepo.fetchPlaylistDetail(playlistId, force)
                if (detail == null) {
                    _error.value = "加载歌单失败"
                    _songListUiState.value = SongListUiState()
                    return@launch
                }

                _playlist.value = detail
                val ids = detail.trackIds?.map { it.id }.orEmpty()
                _allSongIds.value = ids

                if (ids.isEmpty()) {
                    _songListUiState.value = SongListUiState(totalCount = 0)
                    return@launch
                }

                // 完整加载所有歌曲
                loadAllSongs(ids)
            } catch (e: Exception) {
                _error.value = e.message ?: "加载歌单失败"
                _songListUiState.value = SongListUiState()
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun loadAllSongs(ids: List<Long>) {
        val allSongs = mutableListOf<SongItem>()
        var offset = 0

        while (offset < ids.size) {
            val songs = playlistRepo.loadSongBatch(ids, offset, BATCH_SIZE)
            allSongs.addAll(songs)
            offset += songs.size
            if (songs.isEmpty()) break
        }

        _songListUiState.value = SongListUiState(
            songs = allSongs,
            totalCount = ids.size,
        )
    }

    fun putSongsToPool(songs: List<SongItem>) {
        playlistRepo.putToPool(songs)
    }
}