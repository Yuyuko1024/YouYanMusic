package com.youyuan.music.compose.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.CaptchaApi
import com.youyuan.music.compose.api.apis.LoginApi
import com.youyuan.music.compose.api.apis.ProfileApi
import com.youyuan.music.compose.api.apis.QrCodeLoginApi
import com.youyuan.music.compose.api.apis.SmsLoginApi
import com.youyuan.music.compose.api.model.CaptchaResponse
import com.youyuan.music.compose.api.model.Profile
import com.youyuan.music.compose.api.model.UserPlaylistItem
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
 * 用户登录和个人资料管理 ViewModel
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: ApiClient
) : ViewModel() {

    private val qrCodeLoginApi: QrCodeLoginApi = apiClient.createService(QrCodeLoginApi::class.java)
    private val loginApi: LoginApi = apiClient.createService(LoginApi::class.java)
    private val captchaApi: CaptchaApi = apiClient.createService(CaptchaApi::class.java)
    private val smsLoginApi: SmsLoginApi = apiClient.createService(SmsLoginApi::class.java)
    private val profileApi: ProfileApi = apiClient.createService(ProfileApi::class.java)
    private val tokenDataStore = TokenDataStore(context)

    // 用户歌单
    private val _userPlaylists = MutableStateFlow<List<UserPlaylistItem>>(emptyList())
    val userPlaylists: StateFlow<List<UserPlaylistItem>> = _userPlaylists.asStateFlow()

    private val _userPlaylistsLoading = MutableStateFlow(false)
    val userPlaylistsLoading: StateFlow<Boolean> = _userPlaylistsLoading.asStateFlow()

    private val _userPlaylistsError = MutableStateFlow<String?>(null)
    val userPlaylistsError: StateFlow<String?> = _userPlaylistsError.asStateFlow()

    private var lastLoadedUserPlaylistUid: Long? = null

    // 二维码图片 Base64
    private val _qrCodeImage = MutableStateFlow<String?>(null)
    val qrCodeImage: StateFlow<String?> = _qrCodeImage.asStateFlow()

    // 二维码 Key
    private val _qrKey = MutableStateFlow<String?>(null)
    val qrKey: StateFlow<String?> = _qrKey.asStateFlow()

    // 登录状态码 (800=过期, 801=等待扫码, 802=待确认, 803=成功)
    private val _loginStatusCode = MutableStateFlow<Int?>(null)
    val loginStatusCode: StateFlow<Int?> = _loginStatusCode.asStateFlow()

    // 登录状态消息
    private val _loginMessage = MutableStateFlow<String?>(null)
    val loginMessage: StateFlow<String?> = _loginMessage.asStateFlow()

    // 用户信息
    private val _userProfile = MutableStateFlow<Profile?>(null)
    val userProfile: StateFlow<Profile?> = _userProfile.asStateFlow()

    // 是否已登录
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 非错误提示（如“验证码已发送”）
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private var pollingJob: Job? = null

    companion object {
        const val QR_STATUS_EXPIRED = 800
        const val QR_STATUS_WAITING = 801
        const val QR_STATUS_CONFIRMING = 802
        const val QR_STATUS_SUCCESS = 803
    }

    init {
        // 初始化时从本地加载登录状态
        loadSavedLoginState()
    }

    /**
     * 从本地加载已保存的登录状态
     */
    private fun loadSavedLoginState() {
        viewModelScope.launch {
            // 检查是否有保存的 cookie
            val savedCookie = tokenDataStore.authCookie.first()
            if (!savedCookie.isNullOrEmpty()) {
                // 有 cookie，先从本地加载缓存的用户信息
                val userId = tokenDataStore.userId.first()
                val nickname = tokenDataStore.nickname.first()
                val avatarUrl = tokenDataStore.avatarUrl.first()
                val backgroundUrl = tokenDataStore.backgroundUrl.first()

                if (userId != null && !nickname.isNullOrEmpty()) {
                    // 有本地缓存，先显示缓存的用户信息
                    _userProfile.value = Profile(
                        userId = userId,
                        nickname = nickname,
                        avatarUrl = avatarUrl,
                        backgroundUrl = backgroundUrl,
                        userType = null,
                        avatarImgId = null,
                        backgroundImgId = null,
                        signature = null,
                        createTime = null,
                        userName = null,
                        birthday = null,
                        authority = null,
                        gender = null,
                        accountStatus = null,
                        province = null,
                        city = null,
                        authStatus = null,
                        description = null,
                        detailDescription = null,
                        defaultAvatar = false,
                        expertTags = null,
                        experts = null,
                        djStatus = null,
                        locationStatus = null,
                        vipType = null,
                        followed = false,
                        mutual = false,
                        authenticated = false,
                        lastLoginTime = null,
                        lastLoginIP = null,
                        remarkName = null,
                        viptypeVersion = null,
                        authenticationTypes = null,
                        avatarDetail = null,
                        anchor = false
                    )
                    _isLoggedIn.value = true
                }

                // 向服务器请求最新的登录状态（验证 cookie 是否有效）
                getLoginStatus()
            }
        }
    }

    /**
     * 生成二维码
     */
    fun generateQrCode() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _loginStatusCode.value = null
            _loginMessage.value = null

            try {
                // 1. 获取二维码 Key
                val keyResponse = qrCodeLoginApi.getQrCodeKey()
                val key = keyResponse.data.unikey
                _qrKey.value = key

                // 2. 生成二维码图片
                val imgResponse = qrCodeLoginApi.createQrCode(key = key)
                _qrCodeImage.value = imgResponse.data.qrimg

                // 3. 开始轮询登录状态
                startPollingLoginStatus(key)

            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "生成二维码失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 开始轮询登录状态
     */
    private fun startPollingLoginStatus(key: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val statusResponse = qrCodeLoginApi.checkQrCodeStatus(key = key)
                    _loginStatusCode.value = statusResponse.code
                    _loginMessage.value = statusResponse.message

                    when (statusResponse.code) {
                        QR_STATUS_EXPIRED -> {
                            // 二维码过期
                            _error.value = "二维码已过期，请重新获取"
                            break
                        }
                        QR_STATUS_SUCCESS -> {
                            // 登录成功
                            val cookie = statusResponse.cookie
                            if (!cookie.isNullOrEmpty()) {
                                // 保存 cookie 到 DataStore
                                tokenDataStore.saveAuthCookie(cookie)
                                // 保存 cookie 到 ApiClient (CookieManager)
                                apiClient.saveCookieString(cookie)
                                // 获取用户信息
                                getLoginStatus()
                            }
                            break
                        }
                        // 801 等待扫码, 802 待确认 - 继续轮询
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(3000) // 每 3 秒轮询一次
            }
        }
    }

    /**
     * 停止轮询
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * 获取登录状态（用户信息）
     */
    fun getLoginStatus() {
        viewModelScope.launch {
            try {
                val response = loginApi.checkLoginStatus()
                val actualCode = response.getActualCode()
                val profile = response.getActualProfile()

                if (actualCode == 200 && profile != null) {
                    _userProfile.value = profile
                    _isLoggedIn.value = true

                    // 保存用户信息到本地
                    tokenDataStore.saveUserInfo(
                        userId = profile.userId ?: 0L,
                        nickname = profile.nickname ?: "",
                        avatarUrl = profile.avatarUrl,
                        backgroundUrl = profile.backgroundUrl
                    )

                    // 登录成功后，拉取一次用户歌单（避免 UI 需要额外触发）
                    loadUserPlaylists(force = true)
                } else {
                    // 未登录或登录已失效
                    _isLoggedIn.value = false
                    _userProfile.value = null
                    clearUserPlaylists()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "获取登录状态失败: ${e.message}"
            }
        }
    }

    fun loadUserPlaylists(
        isLoggedIn: Boolean = _isLoggedIn.value,
        force: Boolean = false,
    ) {
        val uid = _userProfile.value?.userId
        if (!isLoggedIn || uid == null || uid == 0L) {
            clearUserPlaylists()
            return
        }

        if (!force && lastLoadedUserPlaylistUid == uid && _userPlaylists.value.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            _userPlaylistsLoading.value = true
            _userPlaylistsError.value = null
            try {
                val resp = profileApi.getUserPlaylist(uid = uid)
                if (resp.code == 200) {
                    _userPlaylists.value = resp.playlist.orEmpty()
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

    fun clearUserPlaylists() {
        _userPlaylists.value = emptyList()
        _userPlaylistsLoading.value = false
        _userPlaylistsError.value = null
        lastLoadedUserPlaylistUid = null
    }

    /**
     * 退出登录
     */
    fun logout() {
        viewModelScope.launch {
            // 清除 ApiClient 的 cookie
            apiClient.clearCookies()

            // 清除本地 DataStore 保存的登录信息
            tokenDataStore.clearAll()

            _userProfile.value = null
            _isLoggedIn.value = false
            clearUserPlaylists()
        }
    }

    /**
     * 清除二维码
     */
    fun clearQrCode() {
        stopPolling()
        _qrCodeImage.value = null
        _qrKey.value = null
        _loginStatusCode.value = null
        _loginMessage.value = null
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }

    fun clearNotice() {
        _notice.value = null
    }

    /**
     * 发送短信验证码
     */
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
                val result = try {
                    captchaApi.sendCaptcha(phone = normalized, countryCode = countryCode)
                } catch (e: Exception) {
                    _error.value = "接口调用失败"
                    return@launch
                }

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

    /**
     * 校验验证码（通常可直接调用 /login/cellphone，不必单独 verify；这里保留给需要时使用）
     */
    fun verifyCaptcha(phone: String, captcha: String, countryCode: String = "86") {
        val normalizedPhone = phone.trim()
        val normalizedCaptcha = captcha.trim()
        if (normalizedPhone.isBlank()) {
            _error.value = "请输入手机号"
            return
        }
        if (normalizedCaptcha.isBlank()) {
            _error.value = "请输入验证码"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _notice.value = null
            try {
                val result = captchaApi.verifyCaptcha(
                    phone = normalizedPhone,
                    captcha = normalizedCaptcha,
                    countryCode = countryCode,
                )
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

    /**
     * 使用短信验证码登录：成功后保存 cookie，并刷新 /login/status。
     */
    fun loginWithCaptcha(phone: String, captcha: String, countryCode: String = "86") {
        val normalizedPhone = phone.trim()
        val normalizedCaptcha = captcha.trim()
        if (normalizedPhone.isBlank()) {
            _error.value = "请输入手机号"
            return
        }
        if (normalizedCaptcha.isBlank()) {
            _error.value = "请输入验证码"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _notice.value = null

            try {
                val response = smsLoginApi.loginWithCaptcha(
                    phone = normalizedPhone,
                    captcha = normalizedCaptcha,
                    countryCode = countryCode,
                )

                if (response.code == 200 && !response.cookie.isNullOrBlank()) {
                    val cookie = response.cookie
                    tokenDataStore.saveAuthCookie(cookie)
                    apiClient.saveCookieString(cookie)
                    _notice.value = "登录成功"
                    getLoginStatus()
                } else {
                    _error.value = response.message
                        ?: "登录失败: ${response.code ?: "unknown"}"
                }
            } catch (e: Exception) {
                _error.value = "登录失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
