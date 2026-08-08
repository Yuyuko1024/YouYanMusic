package com.youyuan.music.compose.data.repo

import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.CaptchaApi
import com.youyuan.music.compose.api.apis.LoginApi
import com.youyuan.music.compose.api.apis.ProfileApi
import com.youyuan.music.compose.api.apis.QrCodeLoginApi
import com.youyuan.music.compose.api.apis.SmsLoginApi
import com.youyuan.music.compose.api.model.CaptchaResponse
import com.youyuan.music.compose.api.model.CellphoneLoginResponse
import com.youyuan.music.compose.api.model.LoginStatusData
import com.youyuan.music.compose.api.model.QrCodeLoginCheckData
import com.youyuan.music.compose.api.model.QrCodeLoginImgData
import com.youyuan.music.compose.api.model.QrCodeLoginKeyData
import com.youyuan.music.compose.api.model.UserPlaylistResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户 & 登录数据仓库：持有所有登录/验证码/个人资料相关 API。
 */
@Singleton
class ProfileRepo @Inject constructor(
    private val apiClient: ApiClient,
) {

    private val qrCodeLoginApi: QrCodeLoginApi by lazy { apiClient.createService(QrCodeLoginApi::class.java) }
    private val loginApi: LoginApi by lazy { apiClient.createService(LoginApi::class.java) }
    private val captchaApi: CaptchaApi by lazy { apiClient.createService(CaptchaApi::class.java) }
    private val smsLoginApi: SmsLoginApi by lazy { apiClient.createService(SmsLoginApi::class.java) }
    private val profileApi: ProfileApi by lazy { apiClient.createService(ProfileApi::class.java) }

    // ---- QR 登录 ----

    suspend fun getQrCodeKey(): QrCodeLoginKeyData = withContext(Dispatchers.IO) {
        qrCodeLoginApi.getQrCodeKey()
    }

    suspend fun createQrCode(key: String): QrCodeLoginImgData = withContext(Dispatchers.IO) {
        qrCodeLoginApi.createQrCode(key = key)
    }

    suspend fun checkQrCodeStatus(key: String): QrCodeLoginCheckData = withContext(Dispatchers.IO) {
        qrCodeLoginApi.checkQrCodeStatus(key = key)
    }

    // ---- 登录状态 ----

    suspend fun checkLoginStatus(): LoginStatusData = withContext(Dispatchers.IO) {
        loginApi.checkLoginStatus()
    }

    // ---- 短信验证码 ----

    suspend fun sendCaptcha(phone: String, countryCode: String): CaptchaResponse = withContext(Dispatchers.IO) {
        captchaApi.sendCaptcha(phone = phone, countryCode = countryCode)
    }

    suspend fun verifyCaptcha(phone: String, captcha: String, countryCode: String): CaptchaResponse = withContext(Dispatchers.IO) {
        captchaApi.verifyCaptcha(phone = phone, captcha = captcha, countryCode = countryCode)
    }

    /** 短信验证码登录，返回的 cookie 由调用方负责持久化 */
    suspend fun loginWithCaptcha(phone: String, captcha: String, countryCode: String): CellphoneLoginResponse = withContext(Dispatchers.IO) {
        smsLoginApi.loginWithCaptcha(phone = phone, captcha = captcha, countryCode = countryCode)
    }

    // ---- 用户歌单 ----

    suspend fun getUserPlaylist(uid: Long): UserPlaylistResponse = withContext(Dispatchers.IO) {
        profileApi.getUserPlaylist(uid = uid)
    }
}