package com.youyuan.music.compose.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.youyuan.music.compose.api.apis.SongApi
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.data.SongDetailPool
import kotlin.math.min

class SongIdsPagingSource(
    private val songIds: List<Long>,
    private val songApi: SongApi,
    private val songDetailPool: SongDetailPool,
) : PagingSource<Int, SongDetail>() {

    override fun getRefreshKey(state: PagingState<Int, SongDetail>): Int? {
        // 重新刷新时，尝试定位到当前列表中间的位置
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(state.config.pageSize) ?: anchorPage?.nextKey?.minus(state.config.pageSize)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SongDetail> {
        // key 代表当前加载的起始索引，默认为 0
        val key = params.key ?: 0
        val loadSize = params.loadSize

        // 如果已经超出列表范围，返回空
        if (key >= songIds.size) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null
            )
        }

        // 计算本次需要加载的 ID 子列表
        val end = min(key + loadSize, songIds.size)
        val idsToLoad = songIds.subList(key, end)

        return try {
            // 先从对象池命中，缺失的再走网络
            val missingIds = idsToLoad.filterNot { songDetailPool.contains(it) }
            if (missingIds.isNotEmpty()) {
                val response = songApi.getSongDetails(missingIds.joinToString(","))
                val songs = response.songs ?: emptyList()
                songDetailPool.putAll(songs)
            }

            // 严格按 idsToLoad 顺序输出
            val songs = idsToLoad.mapNotNull { songDetailPool.get(it) }

            // 计算下一页的 key (即下一个起始索引)
            val nextKey = if (end < songIds.size) end else null

            // 关键：设置 itemsBefore 和 itemsAfter 以支持占位符 (Placeholders)
            // itemsBefore: 当前页之前有多少项
            // itemsAfter: 当前页之后还有多少项未加载
            LoadResult.Page(
                data = songs,
                prevKey = null, // 我们只支持向下滚动加载
                nextKey = nextKey,
                itemsBefore = key,
                itemsAfter = songIds.size - end
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}