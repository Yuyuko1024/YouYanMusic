package com.youyuan.music.compose.utils

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object MediaCache {

    private const val MAX_CACHE_SIZE_BYTES: Long = 1024L * 1024L * 1024L // 1GB

    @Volatile
    private var cache: Cache? = null

    fun get(context: Context): Cache {
        val existing = cache
        if (existing != null) return existing

        return synchronized(this) {
            val again = cache
            if (again != null) return@synchronized again

            val cacheDir = File(context.cacheDir, "media3")
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
            val databaseProvider = StandaloneDatabaseProvider(context)
            val created = SimpleCache(cacheDir, evictor, databaseProvider)
            cache = created
            created
        }
    }
}
