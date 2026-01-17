package com.youyuan.music.compose.data

import com.youyuan.music.compose.api.model.PlaylistDetail
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 歌单详情缓存：用于避免同一个 playlistId 反复请求 /playlist/detail。
 *
 * - 只缓存元信息 + trackIds（SongDetail 由 SongDetailPool 负责缓存）
 */
@Singleton
class PlaylistDetailCache @Inject constructor() {

    private val map = ConcurrentHashMap<Long, PlaylistDetail>()

    fun get(playlistId: Long): PlaylistDetail? = map[playlistId]

    fun put(detail: PlaylistDetail) {
        val id = detail.id ?: return
        map[id] = detail
    }

    fun clear(playlistId: Long) {
        map.remove(playlistId)
    }

    fun clearAll() {
        map.clear()
    }
}
