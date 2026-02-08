package com.youyuan.music.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.ArtistApi
import com.youyuan.music.compose.api.model.AlbumDetail
import com.youyuan.music.compose.api.model.ArtistProfile
import com.youyuan.music.compose.api.model.SongDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val artistApi: ArtistApi by lazy { apiClient.createService(ArtistApi::class.java) }

    private val _artist = MutableStateFlow<ArtistProfile?>(null)
    val artist: StateFlow<ArtistProfile?> = _artist.asStateFlow()

    private val _topSongs = MutableStateFlow<List<SongDetail>>(emptyList())
    val topSongs: StateFlow<List<SongDetail>> = _topSongs.asStateFlow()

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
                val detailResp = withContext(Dispatchers.IO) {
                    artistApi.getArtistDetail(id = artistId)
                }
                if (detailResp.code != 200) {
                    throw IllegalStateException(
                        detailResp.message
                            ?: "artist/detail 接口返回异常 code=${detailResp.code}"
                    )
                }

                val profile = detailResp.data?.artist
                _artist.value = profile

                val topSongResp = withContext(Dispatchers.IO) {
                    artistApi.getArtistTopSongs(id = artistId)
                }
                if (topSongResp.code != 200) {
                    throw IllegalStateException(
                        "artist/top/song 接口返回异常 code=${topSongResp.code}"
                    )
                }
                _topSongs.value = topSongResp.songs.orEmpty().take(50)

                _albums.value = fetchAllAlbums(artistId)
            } catch (t: Throwable) {
                _error.value = t.toUserMessage()
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun fetchAllAlbums(artistId: Long): List<AlbumDetail> {
        val results = ArrayList<AlbumDetail>(64)
        val limit = 30
        var offset = 0
        var more = true
        var guard = 0

        while (more && guard < 40) {
            guard++
            val resp = withContext(Dispatchers.IO) {
                artistApi.getArtistAlbums(id = artistId, limit = limit, offset = offset)
            }
            if (resp.code != 200) {
                throw IllegalStateException(
                    "artist/album 接口返回异常 code=${resp.code}"
                )
            }

            val batch = resp.hotAlbums.orEmpty()
            if (batch.isEmpty()) break

            results.addAll(batch)
            offset += batch.size
            more = resp.more == true
        }

        return results
    }

    private fun Throwable.toUserMessage(): String {
        val rawMessage = message.orEmpty()
        if (rawMessage.contains("RISK_CONTROL_-462")) {
            return "检测到您的网络环境存在风险，请稍后再试"
        }

        if (this is HttpException) {
            val code = code()
            val errorBody = try {
                response()?.errorBody()?.string()
            } catch (_: Throwable) {
                null
            }

            val serverMessage = errorBody
                ?.let {
                    try {
                        JSONObject(it).optString("message")
                    } catch (_: Throwable) {
                        null
                    }
                }
                ?.takeIf { it.isNotBlank() }

            return serverMessage
                ?: when (code) {
                    404 -> "无相关艺人"
                    else -> "HTTP $code ${rawMessage.ifBlank { message() }}"
                }
        }

        return message ?: "加载失败"
    }
}
