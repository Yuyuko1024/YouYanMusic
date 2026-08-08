package com.youyuan.music.compose.data

import com.youyuan.music.compose.data.model.SongItem
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 进程级 SongItem 缓存池。
 * [SongRepo] 和 [PlaylistRepo] 共用，避免对同一首歌重复请求。
 */
@Singleton
class SongDetailPool @Inject constructor() {

    private val pool = ConcurrentHashMap<Long, SongItem>()

    fun contains(id: Long): Boolean = pool.containsKey(id)

    fun putAll(songs: List<SongItem>) {
        songs.forEach { pool[it.id] = it }
    }

    fun getOrdered(ids: List<Long>): List<SongItem> {
        return ids.mapNotNull { pool[it] }
    }
}
