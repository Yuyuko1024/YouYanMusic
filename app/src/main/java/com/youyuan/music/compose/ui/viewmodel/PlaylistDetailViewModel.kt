package com.youyuan.music.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.PlaylistApi
import com.youyuan.music.compose.api.model.PlaylistDetail
import com.youyuan.music.compose.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val playlistApi: PlaylistApi = apiClient.createService(PlaylistApi::class.java)

    private val _playlist = MutableStateFlow<PlaylistDetail?>(null)
    val playlist: StateFlow<PlaylistDetail?> = _playlist.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadPlaylistDetail(playlistId: Long, force: Boolean = false) {
        if (_loading.value) return
        if (!force && _playlist.value?.id == playlistId) return

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val resp = playlistApi.getPlaylistDetail(id = playlistId)
                if (resp.code == 200) {
                    _playlist.value = resp.playlist
                } else {
                    _playlist.value = null
                    _error.value = "playlist/detail 接口返回异常 code=${resp.code}"
                }
            } catch (e: Exception) {
                _playlist.value = null
                _error.value = e.message ?: "playlist/detail 请求失败"
                Logger.debug("PlaylistDetailViewModel", "loadPlaylistDetail failed: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }
}
