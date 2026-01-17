package com.youyuan.music.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.constants.AppConstants
import com.youyuan.music.compose.pref.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

@HiltViewModel
class AppConfigViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val apiClient: ApiClient,
) : ViewModel() {

    // 注意：DataStore 首次读取是异步的。
    val savedApiUrl: StateFlow<String?> = settingsDataStore.savedAppApiUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val effectiveApiUrl: StateFlow<String> = settingsDataStore.savedAppApiUrl
        .map { saved -> saved.ifBlank { AppConstants.APP_API_ENDPOINT } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppConstants.APP_API_ENDPOINT)

    init {
        // 保持 ApiClient 的 baseUrl 与当前设置同步（运行时切换立即生效）
        viewModelScope.launch {
            effectiveApiUrl
                .collect { url ->
                    if (url.isNotBlank()) {
                        apiClient.setBaseUrl(url)
                    }
                }
        }
    }

    /**
     * 规范化并持久化 API URL，同时立即应用到 ApiClient。
     * @return 规范化后的 URL（确保以 / 结尾）
     */
    suspend fun persistAndApplyApiUrl(raw: String): String {
        val normalized = normalizeApiUrl(raw)
        settingsDataStore.setAppApiUrl(normalized)
        // setBaseUrl 返回 false 时说明 URL 非法（理论上不会发生，因为 normalize 已校验）
        apiClient.setBaseUrl(normalized)
        return normalized
    }

    private fun normalizeApiUrl(raw: String): String {
        val trimmed = raw.trim()
        require(trimmed.isNotBlank()) { "API 地址不能为空" }

        val withSlash = if (trimmed.endsWith('/')) trimmed else "$trimmed/"
        val httpUrl = withSlash.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("API 地址格式不正确")

        require(httpUrl.scheme == "http" || httpUrl.scheme == "https") {
            "仅支持 http/https"
        }

        return httpUrl.toString()
    }
}
