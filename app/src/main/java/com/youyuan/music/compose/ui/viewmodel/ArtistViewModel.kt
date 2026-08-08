package com.youyuan.music.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.model.AlbumDetail
import com.youyuan.music.compose.api.model.ArtistProfile
import com.youyuan.music.compose.data.model.SongItem
import com.youyuan.music.compose.data.repo.ArtistRepo
import com.youyuan.music.compose.data.repo.RepoRiskException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val artistRepo: ArtistRepo,
) : ViewModel() {

    private val _artist = MutableStateFlow<ArtistProfile?>(null)
    val artist: StateFlow<ArtistProfile?> = _artist.asStateFlow()

    private val _topSongs = MutableStateFlow<List<SongItem>>(emptyList())
    val topSongs: StateFlow<List<SongItem>> = _topSongs.asStateFlow()

    private val _albums = MutableStateFlow<List<AlbumDetail>>(emptyList())
    val albums: StateFlow<List<AlbumDetail>> = _albums.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun consumeError() {
        _error.value = null
    }

    fun loadArtist(artistId: Long) {
        if (artistId <= 0L) {
            _error.value = "artistId 无效"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val profile = artistRepo.getArtistDetail(artistId)
                _artist.value = profile

                _topSongs.value = artistRepo.getArtistTopSongs(artistId)
                _albums.value = fetchAllAlbums(artistId)
            } catch (e: RepoRiskException) {
                _error.value = e.message
            } catch (t: Throwable) {
                _error.value = t.message ?: "加载失败"
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun fetchAllAlbums(artistId: Long): List<AlbumDetail> {
        val results = ArrayList<AlbumDetail>(64)
        var offset = 0
        var more = true
        var guard = 0

        while (more && guard < 40) {
            guard++
            val page = artistRepo.getArtistAlbums(artistId, limit = 30, offset = offset)
            if (page.albums.isEmpty()) break
            results.addAll(page.albums)
            offset += page.albums.size
            more = page.hasMore
        }

        return results
    }
}
