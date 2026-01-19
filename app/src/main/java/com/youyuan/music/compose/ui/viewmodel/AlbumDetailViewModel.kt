package com.youyuan.music.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.AlbumApi
import com.youyuan.music.compose.api.model.AlbumDetail
import com.youyuan.music.compose.api.model.AlbumSong
import com.youyuan.music.compose.api.model.ArtistDetail
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.api.model.SongDetailAlbumData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val albumApi: AlbumApi by lazy { apiClient.createService(AlbumApi::class.java) }

    private val _album = MutableStateFlow<AlbumDetail?>(null)
    val album: StateFlow<AlbumDetail?> = _album.asStateFlow()

    private val _songs = MutableStateFlow<List<SongDetail>>(emptyList())
    val songs: StateFlow<List<SongDetail>> = _songs.asStateFlow()

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
                val response = albumApi.getAlbumDetails(albumId)
                val detail = response.album

                _album.value = detail
                _songs.value = (response.songs.orEmpty()).map { it.toSongDetail(detail) }
            } catch (t: Throwable) {
                _error.value = t.message ?: "加载失败"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun AlbumSong.toSongDetail(albumDetail: AlbumDetail?): SongDetail {
        val coverUrl = albumDetail?.picUrl
        return SongDetail(
            name = name,
            id = id ?: 0L,
            ar = ar?.mapNotNull { a ->
                val artistId = a.id ?: return@mapNotNull null
                ArtistDetail(id = artistId, name = a.name)
            },
            al = SongDetailAlbumData(
                id = al?.id ?: (albumDetail?.id ?: 0L),
                name = al?.name ?: albumDetail?.name,
                picUrl = coverUrl,
            ),
            dt = dt,
            fee = fee?.toInt(),
            mv = mv,
        )
    }
}
