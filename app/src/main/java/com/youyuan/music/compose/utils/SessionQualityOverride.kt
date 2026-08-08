package com.youyuan.music.compose.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 当前播放会话临时音质覆盖（不持久化）。
 * Dialog 切换音质时写入；切歌单/清空队列时清除。
 * Service 读取此值：非 null 时优先使用，null 时回退到 DataStore 默认值。
 */
object SessionQualityOverride {
    private val _level = MutableStateFlow<String?>(null)
    val level: StateFlow<String?> = _level.asStateFlow()

    fun set(qualityLevel: String) {
        _level.value = qualityLevel
    }

    fun clear() {
        _level.value = null
    }
}
