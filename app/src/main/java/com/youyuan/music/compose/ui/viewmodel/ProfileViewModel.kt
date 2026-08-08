package com.youyuan.music.compose.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.model.Profile
import com.youyuan.music.compose.api.model.UserPlaylistItem
import com.youyuan.music.compose.data.PlaylistDetailCache
import com.youyuan.music.compose.data.repo.ProfileRepo
import com.youyuan.music.compose.pref.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户登录和个人资料管理 ViewModel —— 精简版。
 * 网络逻辑全部下沉到 ProfileRepo，VM 只负责状态管理和轮询逻辑。
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepo: ProfileRepo,
    private val apiClient: ApiClient,
    private val playlistDetailCache: PlaylistDetailCache,
) : ViewModel() {

    companion object {
        const val QR_STATUS_EXPIRED = 800
        const val QR_STATUS_WAITING = 801
        const val QR_STATUS_CONFIRMING = 802
        const val QR_STATUS_SUCCESS = 803
    }

    private val tokenDataStore = TokenDataStore(context)

    // ---- 用户歌单 ----

    private val _userPlaylists = MutableStateFlow<List<UserPlaylistItem>>(emptyList())
    val userPlaylists: StateFlow<List<UserPlaylistItem>> = _userPlaylists.asStateFlow()

    private val _userPlaylistsLoading = MutableStateFlow(false)
    val userPlaylistsLoading: StateFlow<Boolean> = _userPlaylistsLoading.asStateFlow()

    private val _userPlaylistsError = MutableStateFlow<String?>(null)
    val userPlaylistsError: StateFlow<String?> = _userPlaylistsError.asStateFlow()

    private var lastLoadedUserPlaylistUid: Long? = null

    // ---- 二维码登录 ----

    private val _qrCodeImage = MutableStateFlow<String?>(null)
    val qrCodeImage: StateFlow<String?> = _qrCodeImage.asStateFlow()

    private val _qrKey = MutableStateFlow<String?>(null)
    val qrKey: StateFlow<String?> = _qrKey.asStateFlow()

    private val _loginStatusCode = MutableStateFlow<Int?>(null)
    val loginStatusCode: StateFlow<Int?> = _loginStatusCode.asStateFlow()

    private val _loginMessage = MutableStateFlow<String?>(null)
    val loginMessage: StateFlow<String?> = _loginMessage.asStateFlow()

    // ---- 用户信息 ----

    private val _userProfile = MutableStateFlow<Profile?>(null)
    val userProfile: StateFlow<Profile?> = _userProfile.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // ---- 通用 ----

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private var pollingJob: Job? = null

    // ============================================================
    // Init
    // ============================================================

    init {
        loadSavedLoginState()
    }

    private fun loadSavedLoginState() {
        viewModelScope.launch {
            val savedCookie = tokenDataStore.authCookie.first()
            if (!savedCookie.isNullOrEmpty()) {
                val userId = tokenDataStore.userId.first()
                val nickname = tokenDataStore.nickname.first()
                val avatarUrl = tokenDataStore.avatarUrl.first()
                val backgroundUrl = tokenDataStore.backgroundUrl.first()
                val signature = tokenDataStore.signature.first()

                if (userId != null && !nickname.isNullOrEmpty()) {
                    _userProfile.value = Profile(
                        userId = userId,
                        nickname = nickname,
                        avatarUrl = avatarUrl,
                        backgroundUrl = backgroundUrl,
                        userType = null, avatarImgId = null, backgroundImgId = null,
                        signature = signature, createTime = null, userName = null,
                        birthday = null, authority = null, gender = null,
                        accountStatus = null, province = null, city = null,
                        authStatus = null, description = null, detailDescription = null,
                        defaultAvatar = false, expertTags = null, experts = null,
                        djStatus = null, locationStatus = null, vipType = null,
                        followed = false, mutual = false, authenticated = false,
                        lastLoginTime = null, lastLoginIP = null, remarkName = null,
                        viptypeVersion = null, authenticationTypes = null,
                        avatarDetail = null, anchor = false,
                    )
                    _isLoggedIn.value = true
                }
                getLoginStatus()
            }
        }
    }

    // ============================================================
    // QR 码登录
    // ============================================================

    fun generateQrCode() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _loginStatusCode.value = null
            _loginMessage.value = null

            try {
                val keyResponse = profileRepo.getQrCodeKey()
                val key = keyResponse.data.unikey
                _qrKey.value = key

                val imgResponse = profileRepo.createQrCode(key = key)
                _qrCodeImage.value = imgResponse.data.qrimg

                startPollingLoginStatus(key)
            } catch (e: Exception) {
                _error.value = "生成二维码失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startPollingLoginStatus(key: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val statusResponse = profileRepo.checkQrCodeStatus(key = key)
                    _loginStatusCode.value = statusResponse.code
                    _loginMessage.value = statusResponse.message

                    when (statusResponse.code) {
                        QR_STATUS_EXPIRED -> {
                            _error.value = "二维码已过期，请重新获取"
                            break
                        }
                        QR_STATUS_SUCCESS -> {
                            val cookie = statusResponse.cookie
                            if (!cookie.isNullOrEmpty()) {
                                tokenDataStore.saveAuthCookie(cookie)
                                apiClient.saveCookieString(cookie)
                                getLoginStatus()
                            }
                            break
                        }
                        // 801 等待扫码, 802 待确认 —— 继续轮询
                    }
                } catch (_: Exception) {
                    // 轮询异常不中断，继续
                }
                delay(3000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun clearQrCode() {
        stopPolling()
        _qrCodeImage.value = null
        _qrKey.value = null
        _loginStatusCode.value = null
        _loginMessage.value = null
    }

    // ============================================================
    // 登录状态 & 用户信息
    // ============================================================

    fun getLoginStatus() {
        viewModelScope.launch {
            try {
                val response = profileRepo.checkLoginStatus()
                val actualCode = response.getActualCode()
                val profile = response.getActualProfile()

                if (actualCode == 200 && profile != null) {
                    _userProfile.value = profile
                    _isLoggedIn.value = true

                    tokenDataStore.saveUserInfo(
                        userId = profile.userId ?: 0L,
                        nickname = profile.nickname ?: "",
                        avatarUrl = profile.avatarUrl,
                        backgroundUrl = profile.backgroundUrl,
                        signature = profile.signature,
                    )

                    loadUserPlaylists(force = true)
                } else {
                    _isLoggedIn.value = false
                    _userProfile.value = null
                    clearUserPlaylists()
                }
            } catch (e: Exception) {
                _error.value = "获取登录状态失败: ${e.message}"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            apiClient.clearCookies()
            tokenDataStore.clearAll()
            _userProfile.value = null
            _isLoggedIn.value = false
            clearUserPlaylists()
        }
    }

    // ============================================================
    // 短信验证码登录
    // ============================================================

    fun sendCaptcha(phone: String, countryCode: String = "86") {
        val normalized = phone.trim()
        if (normalized.isBlank()) {
            _error.value = "请输入手机号"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _notice.value = null
            try {
                val result = profileRepo.sendCaptcha(normalized, countryCode)
                if (result.code == 200 && result.data == true) {
                    _notice.value = "验证码已发送"
                } else {
                    _error.value = "发送验证码失败: ${result.code ?: "unknown"}"
                }
            } catch (e: Exception) {
                _error.value = "发送验证码失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyCaptcha(phone: String, captcha: String, countryCode: String = "86") {
        val normalizedPhone = phone.trim()
        val normalizedCaptcha = captcha.trim()
        if (normalizedPhone.isBlank()) { _error.value = "请输入手机号"; return }
        if (normalizedCaptcha.isBlank()) { _error.value = "请输入验证码"; return }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _notice.value = null
            try {
                val result = profileRepo.verifyCaptcha(normalizedPhone, normalizedCaptcha, countryCode)
                if (result.code == 200 && result.data == true) {
                    _notice.value = "验证码校验通过"
                } else {
                    _error.value = "验证码校验失败: ${result.code ?: "unknown"}"
                }
            } catch (e: Exception) {
                _error.value = "验证码校验失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loginWithCaptcha(phone: String, captcha: String, countryCode: String = "86") {
        val normalizedPhone = phone.trim()
        val normalizedCaptcha = captcha.trim()
        if (normalizedPhone.isBlank()) { _error.value = "请输入手机号"; return }
        if (normalizedCaptcha.isBlank()) { _error.value = "请输入验证码"; return }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _notice.value = null
            try {
                val response = profileRepo.loginWithCaptcha(normalizedPhone, normalizedCaptcha, countryCode)
                if (response.code == 200 && !response.cookie.isNullOrBlank()) {
                    tokenDataStore.saveAuthCookie(response.cookie)
                    apiClient.saveCookieString(response.cookie)
                    _notice.value = "登录成功"
                    getLoginStatus()
                } else {
                    _error.value = response.message ?: "登录失败: ${response.code ?: "unknown"}"
                }
            } catch (e: Exception) {
                _error.value = "登录失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============================================================
    // 用户歌单
    // ============================================================

    fun loadUserPlaylists(
        isLoggedIn: Boolean = _isLoggedIn.value,
        force: Boolean = false,
    ) {
        val uid = _userProfile.value?.userId
        if (!isLoggedIn || uid == null || uid == 0L) {
            clearUserPlaylists()
            return
        }
        if (!force && lastLoadedUserPlaylistUid == uid && _userPlaylists.value.isNotEmpty()) return

        viewModelScope.launch {
            _userPlaylistsLoading.value = true
            _userPlaylistsError.value = null
            try {
                val resp = profileRepo.getUserPlaylist(uid = uid)
                if (resp.code == 200) {
                    updateUserPlaylistsIncrementally(resp.playlist.orEmpty())
                    lastLoadedUserPlaylistUid = uid
                } else {
                    _userPlaylists.value = emptyList()
                    _userPlaylistsError.value = "获取用户歌单失败: ${resp.code ?: "unknown"}"
                }
            } catch (e: Exception) {
                _userPlaylists.value = emptyList()
                _userPlaylistsError.value = e.message ?: "获取用户歌单失败"
            } finally {
                _userPlaylistsLoading.value = false
            }
        }
    }

    private fun updateUserPlaylistsIncrementally(incoming: List<UserPlaylistItem>) {
        val current = _userPlaylists.value
        if (current.isEmpty()) {
            _userPlaylists.value = incoming
            return
        }

        val currentById = current.associateBy { it.id }
        val incomingIdSet = incoming.map { it.id }.toSet()
        val removedIds = currentById.keys - incomingIdSet
        val merged = ArrayList<UserPlaylistItem>(incoming.size)
        var changed = current.size != incoming.size

        incoming.forEachIndexed { index, newItem ->
            val oldItem = currentById[newItem.id]
            when {
                oldItem == null -> {
                    merged += newItem
                    changed = true
                }
                oldItem == newItem -> merged += oldItem
                else -> {
                    merged += newItem
                    changed = true
                }
            }
            if (!changed && index < current.size && current[index].id != newItem.id) {
                changed = true
            }
        }

        if (changed) {
            _userPlaylists.value = merged
        }

        // 清除发生变化的歌单缓存
        (changedPlaylistIds(merged, currentById.keys, removedIds)).forEach { playlistId ->
            playlistDetailCache.clear(playlistId)
        }
    }

    private fun changedPlaylistIds(
        merged: List<UserPlaylistItem>,
        existingIds: Set<Long>,
        removedIds: Set<Long>,
    ): Set<Long> {
        val ids = linkedSetOf<Long>()
        ids.addAll(removedIds)
        merged.forEach { newItem ->
            val oldItem = _userPlaylists.value.find { it.id == newItem.id }
            if (oldItem == null || oldItem != newItem) {
                ids += newItem.id
            }
        }
        return ids
    }

    fun clearUserPlaylists() {
        _userPlaylists.value = emptyList()
        _userPlaylistsLoading.value = false
        _userPlaylistsError.value = null
        lastLoadedUserPlaylistUid = null
    }

    // ============================================================
    // 通用
    // ============================================================

    fun clearError() { _error.value = null }
    fun clearNotice() { _notice.value = null }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}