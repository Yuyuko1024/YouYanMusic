package com.youyuan.music.compose.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.BannerApi
import com.youyuan.music.compose.api.apis.HomeApi
import com.youyuan.music.compose.api.apis.PersonalFmApi
import com.youyuan.music.compose.api.apis.RecommendApi
import com.youyuan.music.compose.api.model.BannerItem
import com.youyuan.music.compose.api.model.PersonalFmSong
import com.youyuan.music.compose.api.model.PersonalizedNewSongItem
import com.youyuan.music.compose.api.model.PersonalizedPlaylistItem
import com.youyuan.music.compose.api.model.RecommendResourceItem
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.api.model.ToplistItem
import com.youyuan.music.compose.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: ApiClient
) : ViewModel() {
    private val bannerApi: BannerApi = apiClient.createService(BannerApi::class.java)
    private val homeApi: HomeApi = apiClient.createService(HomeApi::class.java)
    private val recommendApi: RecommendApi = apiClient.createService(RecommendApi::class.java)
    private val personalFmApi: PersonalFmApi = apiClient.createService(PersonalFmApi::class.java)

    private val _banners = MutableStateFlow<List<BannerItem>>(emptyList())
    val banners: StateFlow<List<BannerItem>> = _banners.asStateFlow()

    private val _bannerLoading = MutableStateFlow(false)
    val bannerLoading: StateFlow<Boolean> = _bannerLoading.asStateFlow()

    private val _bannerError = MutableStateFlow<String?>(null)
    val bannerError: StateFlow<String?> = _bannerError.asStateFlow()

    private val _dailyRecommendPlaylists = MutableStateFlow<List<RecommendResourceItem>>(emptyList())
    val dailyRecommendPlaylists: StateFlow<List<RecommendResourceItem>> = _dailyRecommendPlaylists.asStateFlow()

    private val _dailyRecommendPlaylistsLoading = MutableStateFlow(false)
    val dailyRecommendPlaylistsLoading: StateFlow<Boolean> = _dailyRecommendPlaylistsLoading.asStateFlow()

    private val _dailyRecommendPlaylistsError = MutableStateFlow<String?>(null)
    val dailyRecommendPlaylistsError: StateFlow<String?> = _dailyRecommendPlaylistsError.asStateFlow()

    private val _dailyRecommendSongs = MutableStateFlow<List<SongDetail>>(emptyList())
    val dailyRecommendSongs: StateFlow<List<SongDetail>> = _dailyRecommendSongs.asStateFlow()

    private val _dailyRecommendSongsLoading = MutableStateFlow(false)
    val dailyRecommendSongsLoading: StateFlow<Boolean> = _dailyRecommendSongsLoading.asStateFlow()

    private val _dailyRecommendSongsError = MutableStateFlow<String?>(null)
    val dailyRecommendSongsError: StateFlow<String?> = _dailyRecommendSongsError.asStateFlow()

    private val _personalFmSongs = MutableStateFlow<List<PersonalFmSong>>(emptyList())
    val personalFmSongs: StateFlow<List<PersonalFmSong>> = _personalFmSongs.asStateFlow()

    private val _personalFmSongsLoading = MutableStateFlow(false)
    val personalFmSongsLoading: StateFlow<Boolean> = _personalFmSongsLoading.asStateFlow()

    private val _personalFmSongsError = MutableStateFlow<String?>(null)
    val personalFmSongsError: StateFlow<String?> = _personalFmSongsError.asStateFlow()

    private val _personalizedPlaylists = MutableStateFlow<List<PersonalizedPlaylistItem>>(emptyList())
    val personalizedPlaylists: StateFlow<List<PersonalizedPlaylistItem>> = _personalizedPlaylists.asStateFlow()

    private val _personalizedPlaylistsLoading = MutableStateFlow(false)
    val personalizedPlaylistsLoading: StateFlow<Boolean> = _personalizedPlaylistsLoading.asStateFlow()

    private val _personalizedPlaylistsError = MutableStateFlow<String?>(null)
    val personalizedPlaylistsError: StateFlow<String?> = _personalizedPlaylistsError.asStateFlow()

    private val _personalizedNewSongs = MutableStateFlow<List<PersonalizedNewSongItem>>(emptyList())
    val personalizedNewSongs: StateFlow<List<PersonalizedNewSongItem>> = _personalizedNewSongs.asStateFlow()

    private val _personalizedNewSongsLoading = MutableStateFlow(false)
    val personalizedNewSongsLoading: StateFlow<Boolean> = _personalizedNewSongsLoading.asStateFlow()

    private val _personalizedNewSongsError = MutableStateFlow<String?>(null)
    val personalizedNewSongsError: StateFlow<String?> = _personalizedNewSongsError.asStateFlow()

    private val _toplists = MutableStateFlow<List<ToplistItem>>(emptyList())
    val toplists: StateFlow<List<ToplistItem>> = _toplists.asStateFlow()

    private val _toplistsLoading = MutableStateFlow(false)
    val toplistsLoading: StateFlow<Boolean> = _toplistsLoading.asStateFlow()

    private val _toplistsError = MutableStateFlow<String?>(null)
    val toplistsError: StateFlow<String?> = _toplistsError.asStateFlow()

    fun onLoginStateChanged(isLoggedIn: Boolean) {
        if (!isLoggedIn) {
            clearLoginOnlyContent()
        }
    }

    fun clearLoginOnlyContent() {
        _dailyRecommendPlaylists.value = emptyList()
        _dailyRecommendPlaylistsLoading.value = false
        _dailyRecommendPlaylistsError.value = null

        _dailyRecommendSongs.value = emptyList()
        _dailyRecommendSongsLoading.value = false
        _dailyRecommendSongsError.value = null

        _personalFmSongs.value = emptyList()
        _personalFmSongsLoading.value = false
        _personalFmSongsError.value = null
    }

    fun loadBanner(type: Int = 2, force: Boolean = false) {
        if (_bannerLoading.value) return
        if (!force && _banners.value.isNotEmpty()) return

        viewModelScope.launch {
            _bannerLoading.value = true
            _bannerError.value = null
            try {
                val resp = bannerApi.getBanner(type = type)
                if (resp.code == 200) {
                    _banners.value = resp.banners ?: emptyList()
                } else {
                    _banners.value = emptyList()
                    _bannerError.value = "banner 接口返回异常 code=${resp.code}"
                }
            } catch (e: Exception) {
                _banners.value = emptyList()
                _bannerError.value = e.message ?: "banner 请求失败"
                Logger.debug("ExploreViewModel", "loadBanner failed: ${e.message}")
            } finally {
                _bannerLoading.value = false
            }
        }
    }

    fun loadPersonalizedPlaylists(limit: Int = 10, force: Boolean = false) {
        if (_personalizedPlaylistsLoading.value) return
        if (!force && _personalizedPlaylists.value.isNotEmpty()) return

        viewModelScope.launch {
            _personalizedPlaylistsLoading.value = true
            _personalizedPlaylistsError.value = null
            try {
                val resp = homeApi.getPersonalized(limit = limit)
                if (resp.code == 200) {
                    _personalizedPlaylists.value = resp.result.orEmpty()
                } else {
                    _personalizedPlaylists.value = emptyList()
                    _personalizedPlaylistsError.value = "personalized 接口返回异常 code=${resp.code}"
                }
            } catch (e: Exception) {
                _personalizedPlaylists.value = emptyList()
                _personalizedPlaylistsError.value = e.message ?: "personalized 请求失败"
                Logger.debug("ExploreViewModel", "loadPersonalizedPlaylists failed: ${e.message}")
            } finally {
                _personalizedPlaylistsLoading.value = false
            }
        }
    }

    fun loadPersonalizedNewSongs(limit: Int = 10, force: Boolean = false) {
        if (_personalizedNewSongsLoading.value) return
        if (!force && _personalizedNewSongs.value.isNotEmpty()) return

        viewModelScope.launch {
            _personalizedNewSongsLoading.value = true
            _personalizedNewSongsError.value = null
            try {
                val resp = homeApi.getPersonalizedNewSong(limit = limit)
                if (resp.code == 200) {
                    _personalizedNewSongs.value = resp.result.orEmpty()
                } else {
                    _personalizedNewSongs.value = emptyList()
                    _personalizedNewSongsError.value = "personalized/newsong 接口返回异常 code=${resp.code}"
                }
            } catch (e: Exception) {
                _personalizedNewSongs.value = emptyList()
                _personalizedNewSongsError.value = e.message ?: "personalized/newsong 请求失败"
                Logger.debug("ExploreViewModel", "loadPersonalizedNewSongs failed: ${e.message}")
            } finally {
                _personalizedNewSongsLoading.value = false
            }
        }
    }

    fun loadToplistDetail(force: Boolean = false) {
        if (_toplistsLoading.value) return
        if (!force && _toplists.value.isNotEmpty()) return

        viewModelScope.launch {
            _toplistsLoading.value = true
            _toplistsError.value = null
            try {
                val resp = homeApi.getToplistDetail()
                if (resp.code == 200) {
                    _toplists.value = resp.list.orEmpty()
                } else {
                    _toplists.value = emptyList()
                    _toplistsError.value = "toplist/detail 接口返回异常 code=${resp.code}"
                }
            } catch (e: Exception) {
                _toplists.value = emptyList()
                _toplistsError.value = e.message ?: "toplist/detail 请求失败"
                Logger.debug("ExploreViewModel", "loadToplistDetail failed: ${e.message}")
            } finally {
                _toplistsLoading.value = false
            }
        }
    }

    fun loadDailyRecommendPlaylists(isLoggedIn: Boolean, force: Boolean = false) {
        if (!isLoggedIn) {
            clearLoginOnlyContent()
            return
        }
        if (_dailyRecommendPlaylistsLoading.value) return
        if (!force && _dailyRecommendPlaylists.value.isNotEmpty()) return

        viewModelScope.launch {
            _dailyRecommendPlaylistsLoading.value = true
            _dailyRecommendPlaylistsError.value = null
            try {
                val resp = recommendApi.getRecommendPlaylists()
                if (resp.code == 200) {
                    _dailyRecommendPlaylists.value = resp.recommend.orEmpty()
                } else {
                    _dailyRecommendPlaylists.value = emptyList()
                    _dailyRecommendPlaylistsError.value = "recommend/resource 接口返回异常 code=${resp.code}"
                }
            } catch (e: Exception) {
                _dailyRecommendPlaylists.value = emptyList()
                _dailyRecommendPlaylistsError.value = e.message ?: "recommend/resource 请求失败"
                Logger.debug("ExploreViewModel", "loadDailyRecommendPlaylists failed: ${e.message}")
            } finally {
                _dailyRecommendPlaylistsLoading.value = false
            }
        }
    }

    fun loadDailyRecommendSongs(isLoggedIn: Boolean, force: Boolean = false) {
        if (!isLoggedIn) {
            clearLoginOnlyContent()
            return
        }
        if (_dailyRecommendSongsLoading.value) return
        if (!force && _dailyRecommendSongs.value.isNotEmpty()) return

        viewModelScope.launch {
            _dailyRecommendSongsLoading.value = true
            _dailyRecommendSongsError.value = null
            try {
                val resp = recommendApi.getRecommendSongs()
                if (resp.code == 200) {
                    _dailyRecommendSongs.value = resp.data?.dailySongs.orEmpty()
                } else {
                    _dailyRecommendSongs.value = emptyList()
                    _dailyRecommendSongsError.value = "recommend/songs 接口返回异常 code=${resp.code}"
                }
            } catch (e: Exception) {
                _dailyRecommendSongs.value = emptyList()
                _dailyRecommendSongsError.value = e.message ?: "recommend/songs 请求失败"
                Logger.debug("ExploreViewModel", "loadDailyRecommendSongs failed: ${e.message}")
            } finally {
                _dailyRecommendSongsLoading.value = false
            }
        }
    }

    fun loadPersonalFm(isLoggedIn: Boolean, force: Boolean = false) {
        if (!isLoggedIn) {
            clearLoginOnlyContent()
            return
        }
        if (_personalFmSongsLoading.value) return
        if (!force && _personalFmSongs.value.isNotEmpty()) return

        viewModelScope.launch {
            _personalFmSongsLoading.value = true
            _personalFmSongsError.value = null
            try {
                val resp = personalFmApi.getPersonalFm()
                if (resp.code == 200) {
                    _personalFmSongs.value = resp.data.orEmpty()
                } else {
                    _personalFmSongs.value = emptyList()
                    _personalFmSongsError.value = "personal_fm 接口返回异常 code=${resp.code}"
                }
            } catch (e: Exception) {
                _personalFmSongs.value = emptyList()
                _personalFmSongsError.value = e.message ?: "personal_fm 请求失败"
                Logger.debug("ExploreViewModel", "loadPersonalFm failed: ${e.message}")
            } finally {
                _personalFmSongsLoading.value = false
            }
        }
    }
}