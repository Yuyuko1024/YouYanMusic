package com.youyuan.music.compose.data

import com.youyuan.music.compose.api.model.SongDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 一个进程内的 SongDetail 对象池：列表(Paging)与播放器共享。
 *
 * - 列表懒加载时优先命中缓存，缺失才走网络
 * - 播放器补全到的 SongDetail 会写回池子，列表可复用减少请求
 */
@Singleton
class SongDetailPool @Inject constructor() {

    private val map = ConcurrentHashMap<Long, SongDetail>()

    private val _snapshot = MutableStateFlow<Map<Long, SongDetail>>(emptyMap())
    val snapshot: StateFlow<Map<Long, SongDetail>> = _snapshot.asStateFlow()

    fun contains(songId: Long): Boolean = map.containsKey(songId)

    fun get(songId: Long): SongDetail? = map[songId]

    fun putAll(details: Collection<SongDetail>) {
        if (details.isEmpty()) return

        var changed = false
        for (d in details) {
            val prev = map.put(d.id, d)
            if (prev != d) changed = true
        }
        if (changed) {
            _snapshot.value = map.toMap()
        }
    }

    fun getOrdered(songIds: List<Long>): List<SongDetail> {
        if (songIds.isEmpty()) return emptyList()
        return songIds.mapNotNull { map[it] }
    }
}
