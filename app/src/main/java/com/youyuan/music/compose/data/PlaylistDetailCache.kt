package com.youyuan.music.compose.data

import com.youyuan.music.compose.api.model.PlaylistDetail
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 进程级 PlaylistDetail 缓存，供 PlaylistRepo 和 ProfileViewModel 共用。
 */
@Singleton
class PlaylistDetailCache @Inject constructor() {

    private val cache = ConcurrentHashMap<Long, PlaylistDetail>()

    fun get(id: Long): PlaylistDetail? = cache[id]
    fun put(detail: PlaylistDetail) { detail.id?.let { cache[it] = detail } }
    fun clear(id: Long) { cache.remove(id) }
}
