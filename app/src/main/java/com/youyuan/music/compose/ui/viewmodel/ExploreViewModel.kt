package com.youyuan.music.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.model.BannerItem
import com.youyuan.music.compose.api.model.PersonalizedNewSongItem
import com.youyuan.music.compose.api.model.PersonalizedPlaylistItem
import com.youyuan.music.compose.api.model.RecommendResourceItem
import com.youyuan.music.compose.api.model.ToplistItem
import com.youyuan.music.compose.data.model.SongItem
import com.youyuan.music.compose.data.repo.ExploreRepo
import com.youyuan.music.compose.data.repo.RepoRiskException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 探索页 ViewModel —— 精简版。
 * 网络逻辑全部下沉到 ExploreRepo，VM 只管理各数据段的加载态、防重复、
 * 登录态关联清空。
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val exploreRepo: ExploreRepo,
) : ViewModel() {

    // ============================================================
    // Banner
    // ============================================================

    private val _banners = MutableStateFlow<List<BannerItem>>(emptyList())
    val banners: StateFlow<List<BannerItem>> = _banners.asStateFlow()
    private val _bannerLoading = MutableStateFlow(false)
    val bannerLoading: StateFlow<Boolean> = _bannerLoading.asStateFlow()
    private val _bannerError = MutableStateFlow<String?>(null)
    val bannerError: StateFlow<String?> = _bannerError.asStateFlow()

    // ============================================================
    // 推荐歌单
    // ============================================================

    private val _personalizedPlaylists = MutableStateFlow<List<PersonalizedPlaylistItem>>(emptyList())
    val personalizedPlaylists: StateFlow<List<PersonalizedPlaylistItem>> = _personalizedPlaylists.asStateFlow()
    private val _personalizedPlaylistsLoading = MutableStateFlow(false)
    val personalizedPlaylistsLoading: StateFlow<Boolean> = _personalizedPlaylistsLoading.asStateFlow()
    private val _personalizedPlaylistsError = MutableStateFlow<String?>(null)
    val personalizedPlaylistsError: StateFlow<String?> = _personalizedPlaylistsError.asStateFlow()

    // ============================================================
    // 新歌推荐
    // ============================================================

    private val _personalizedNewSongs = MutableStateFlow<List<PersonalizedNewSongItem>>(emptyList())
    val personalizedNewSongs: StateFlow<List<PersonalizedNewSongItem>> = _personalizedNewSongs.asStateFlow()
    private val _personalizedNewSongsLoading = MutableStateFlow(false)
    val personalizedNewSongsLoading: StateFlow<Boolean> = _personalizedNewSongsLoading.asStateFlow()
    private val _personalizedNewSongsError = MutableStateFlow<String?>(null)
    val personalizedNewSongsError: StateFlow<String?> = _personalizedNewSongsError.asStateFlow()

    // ============================================================
    // 榜单
    // ============================================================

    private val _toplists = MutableStateFlow<List<ToplistItem>>(emptyList())
    val toplists: StateFlow<List<ToplistItem>> = _toplists.asStateFlow()
    private val _toplistsLoading = MutableStateFlow(false)
    val toplistsLoading: StateFlow<Boolean> = _toplistsLoading.asStateFlow()
    private val _toplistsError = MutableStateFlow<String?>(null)
    val toplistsError: StateFlow<String?> = _toplistsError.asStateFlow()

    // ============================================================
    // 每日推荐歌单（需登录）
    // ============================================================

    private val _dailyRecommendPlaylists = MutableStateFlow<List<RecommendResourceItem>>(emptyList())
    val dailyRecommendPlaylists: StateFlow<List<RecommendResourceItem>> = _dailyRecommendPlaylists.asStateFlow()
    private val _dailyRecommendPlaylistsLoading = MutableStateFlow(false)
    val dailyRecommendPlaylistsLoading: StateFlow<Boolean> = _dailyRecommendPlaylistsLoading.asStateFlow()
    private val _dailyRecommendPlaylistsError = MutableStateFlow<String?>(null)
    val dailyRecommendPlaylistsError: StateFlow<String?> = _dailyRecommendPlaylistsError.asStateFlow()

    // ============================================================
    // 每日推荐歌曲（需登录）
    // ============================================================

    private val _dailyRecommendSongs = MutableStateFlow<List<SongItem>>(emptyList())
    val dailyRecommendSongs: StateFlow<List<SongItem>> = _dailyRecommendSongs.asStateFlow()
    private val _dailyRecommendSongsLoading = MutableStateFlow(false)
    val dailyRecommendSongsLoading: StateFlow<Boolean> = _dailyRecommendSongsLoading.asStateFlow()
    private val _dailyRecommendSongsError = MutableStateFlow<String?>(null)
    val dailyRecommendSongsError: StateFlow<String?> = _dailyRecommendSongsError.asStateFlow()

    // ============================================================
    // 私人 FM（需登录）
    // ============================================================

    private val _personalFmSongs = MutableStateFlow<List<SongItem>>(emptyList())
    val personalFmSongs: StateFlow<List<SongItem>> = _personalFmSongs.asStateFlow()
    private val _personalFmSongsLoading = MutableStateFlow(false)
    val personalFmSongsLoading: StateFlow<Boolean> = _personalFmSongsLoading.asStateFlow()
    private val _personalFmSongsError = MutableStateFlow<String?>(null)
    val personalFmSongsError: StateFlow<String?> = _personalFmSongsError.asStateFlow()

    // ============================================================
    // 防重复加载
    // ============================================================

    private var autoLoadedPublicKey: String? = null
    private var autoLoadedLoginKey: String? = null

    // ============================================================
    // 登录态变化
    // ============================================================

    fun onLoginStateChanged(isLoggedIn: Boolean) {
        if (!isLoggedIn) {
            clearLoginOnlyContent()
            autoLoadedLoginKey = null
        }
    }

    // ============================================================
    // 自动加载 & 全量刷新
    // ============================================================

    fun clearAllContent() {
        _banners.value = emptyList(); _bannerLoading.value = false; _bannerError.value = null
        _personalizedPlaylists.value = emptyList(); _personalizedPlaylistsLoading.value = false; _personalizedPlaylistsError.value = null
        _personalizedNewSongs.value = emptyList(); _personalizedNewSongsLoading.value = false; _personalizedNewSongsError.value = null
        _toplists.value = emptyList(); _toplistsLoading.value = false; _toplistsError.value = null
        clearLoginOnlyContent()
        autoLoadedPublicKey = null
        autoLoadedLoginKey = null
    }

    fun clearLoginOnlyContent() {
        _dailyRecommendPlaylists.value = emptyList(); _dailyRecommendPlaylistsLoading.value = false; _dailyRecommendPlaylistsError.value = null
        _dailyRecommendSongs.value = emptyList(); _dailyRecommendSongsLoading.value = false; _dailyRecommendSongsError.value = null
        _personalFmSongs.value = emptyList(); _personalFmSongsLoading.value = false; _personalFmSongsError.value = null
    }

    /** 冷启动自动加载：同一 endpoint 只执行一次，已在内存则不重复 */
    fun ensureAutoLoaded(baseUrlKey: String, isLoggedIn: Boolean) {
        if (baseUrlKey.isBlank()) return

        if (autoLoadedPublicKey != null && autoLoadedPublicKey != baseUrlKey) {
            clearAllContent()
        }

        if (autoLoadedPublicKey != baseUrlKey) {
            autoLoadedPublicKey = baseUrlKey
            loadBanner(type = 2, force = false)
            loadPersonalizedPlaylists(limit = 10, force = false)
            loadPersonalizedNewSongs(limit = 10, force = false)
            loadToplistDetail(force = false)
        }

        if (isLoggedIn) {
            if (autoLoadedLoginKey != baseUrlKey) {
                autoLoadedLoginKey = baseUrlKey
                loadDailyRecommendPlaylists(isLoggedIn = true, force = false)
                loadDailyRecommendSongs(isLoggedIn = true, force = false)
                loadPersonalFm(isLoggedIn = true, force = false)
            }
        } else {
            clearLoginOnlyContent()
            autoLoadedLoginKey = null
        }
    }

    fun refreshAll(isLoggedIn: Boolean) {
        loadBanner(type = 2, force = true)
        loadPersonalizedPlaylists(limit = 10, force = true)
        loadPersonalizedNewSongs(limit = 10, force = true)
        loadToplistDetail(force = true)
        if (isLoggedIn) {
            loadDailyRecommendPlaylists(isLoggedIn = true, force = true)
            loadDailyRecommendSongs(isLoggedIn = true, force = true)
            loadPersonalFm(isLoggedIn = true, force = true)
        } else {
            clearLoginOnlyContent()
        }
    }

    // ============================================================
    // 各数据段加载方法
    // ============================================================

    fun loadBanner(type: Int = 2, force: Boolean = false) {
        if (_bannerLoading.value) return
        if (!force && _banners.value.isNotEmpty()) return
        viewModelScope.launch {
            _bannerLoading.value = true; _bannerError.value = null
            try {
                _banners.value = exploreRepo.getBanner(type)
            } catch (e: RepoRiskException) {
                _bannerError.value = e.message
            } catch (e: Exception) {
                _bannerError.value = e.message ?: "banner 请求失败"
            } finally {
                _bannerLoading.value = false
            }
        }
    }

    fun loadPersonalizedPlaylists(limit: Int = 10, force: Boolean = false) {
        if (_personalizedPlaylistsLoading.value) return
        if (!force && _personalizedPlaylists.value.isNotEmpty()) return
        viewModelScope.launch {
            _personalizedPlaylistsLoading.value = true; _personalizedPlaylistsError.value = null
            try {
                _personalizedPlaylists.value = exploreRepo.getPersonalizedPlaylists(limit)
            } catch (e: RepoRiskException) {
                _personalizedPlaylistsError.value = e.message
            } catch (e: Exception) {
                _personalizedPlaylistsError.value = e.message ?: "推荐歌单请求失败"
            } finally {
                _personalizedPlaylistsLoading.value = false
            }
        }
    }

    fun loadPersonalizedNewSongs(limit: Int = 10, force: Boolean = false) {
        if (_personalizedNewSongsLoading.value) return
        if (!force && _personalizedNewSongs.value.isNotEmpty()) return
        viewModelScope.launch {
            _personalizedNewSongsLoading.value = true; _personalizedNewSongsError.value = null
            try {
                _personalizedNewSongs.value = exploreRepo.getPersonalizedNewSongs(limit)
            } catch (e: RepoRiskException) {
                _personalizedNewSongsError.value = e.message
            } catch (e: Exception) {
                _personalizedNewSongsError.value = e.message ?: "新歌推荐请求失败"
            } finally {
                _personalizedNewSongsLoading.value = false
            }
        }
    }

    fun loadToplistDetail(force: Boolean = false) {
        if (_toplistsLoading.value) return
        if (!force && _toplists.value.isNotEmpty()) return
        viewModelScope.launch {
            _toplistsLoading.value = true; _toplistsError.value = null
            try {
                _toplists.value = exploreRepo.getToplistDetail()
            } catch (e: RepoRiskException) {
                _toplistsError.value = e.message
            } catch (e: Exception) {
                _toplistsError.value = e.message ?: "榜单请求失败"
            } finally {
                _toplistsLoading.value = false
            }
        }
    }

    fun loadDailyRecommendPlaylists(isLoggedIn: Boolean, force: Boolean = false) {
        if (!isLoggedIn) { clearLoginOnlyContent(); return }
        if (_dailyRecommendPlaylistsLoading.value) return
        if (!force && _dailyRecommendPlaylists.value.isNotEmpty()) return
        viewModelScope.launch {
            _dailyRecommendPlaylistsLoading.value = true; _dailyRecommendPlaylistsError.value = null
            try {
                _dailyRecommendPlaylists.value = exploreRepo.getRecommendPlaylists()
            } catch (e: RepoRiskException) {
                _dailyRecommendPlaylistsError.value = e.message
            } catch (e: Exception) {
                _dailyRecommendPlaylistsError.value = e.message ?: "每日推荐歌单请求失败"
            } finally {
                _dailyRecommendPlaylistsLoading.value = false
            }
        }
    }

    fun loadDailyRecommendSongs(isLoggedIn: Boolean, force: Boolean = false) {
        if (!isLoggedIn) { clearLoginOnlyContent(); return }
        if (_dailyRecommendSongsLoading.value) return
        if (!force && _dailyRecommendSongs.value.isNotEmpty()) return
        viewModelScope.launch {
            _dailyRecommendSongsLoading.value = true; _dailyRecommendSongsError.value = null
            try {
                _dailyRecommendSongs.value = exploreRepo.getRecommendSongs()
            } catch (e: RepoRiskException) {
                _dailyRecommendSongsError.value = e.message
            } catch (e: Exception) {
                _dailyRecommendSongsError.value = e.message ?: "每日推荐歌曲请求失败"
            } finally {
                _dailyRecommendSongsLoading.value = false
            }
        }
    }

    fun loadPersonalFm(isLoggedIn: Boolean, force: Boolean = false) {
        if (!isLoggedIn) { clearLoginOnlyContent(); return }
        if (_personalFmSongsLoading.value) return
        if (!force && _personalFmSongs.value.isNotEmpty()) return
        viewModelScope.launch {
            _personalFmSongsLoading.value = true; _personalFmSongsError.value = null
            try {
                _personalFmSongs.value = exploreRepo.getPersonalFm()
            } catch (e: RepoRiskException) {
                _personalFmSongsError.value = e.message
            } catch (e: Exception) {
                _personalFmSongsError.value = e.message ?: "私人FM请求失败"
            } finally {
                _personalFmSongsLoading.value = false
            }
        }
    }
}