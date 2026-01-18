package com.youyuan.music.compose.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 跨 ViewModel 的歌单失效通知：当歌单内容在别处被修改（例如 /like 修改了“我喜欢的音乐”）
 * 时，相关页面应强制刷新 /playlist/detail。
 */
@Singleton
class PlaylistInvalidationBus @Inject constructor() {

    private val _invalidations = MutableSharedFlow<Long>(
        replay = 0,
        extraBufferCapacity = 64,
    )

    val invalidations: SharedFlow<Long> = _invalidations.asSharedFlow()

    fun invalidate(playlistId: Long) {
        _invalidations.tryEmit(playlistId)
    }
}
