package com.youyuan.music.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.model.AlbumDetail
import com.youyuan.music.compose.data.model.SongItem
import com.youyuan.music.compose.data.repo.AlbumRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val albumRepo: AlbumRepo,
) : ViewModel() {

    private val _album = MutableStateFlow<AlbumDetail?>(null)
    val album: StateFlow<AlbumDetail?> = _album.asStateFlow()

    private val _songs = MutableStateFlow<List<SongItem>>(emptyList())
    val songs: StateFlow<List<SongItem>> = _songs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadAlbumDetail(albumId: Long) {
        if (albumId <= 0L) {
            _error.value = "albumId 无效"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = albumRepo.getAlbumDetail(albumId)
                _album.value = result.album
                _songs.value = result.songs
            } catch (t: Throwable) {
                _error.value = t.message ?: "加载失败"
            } finally {
                _loading.value = false
            }
        }
    }
}
