package com.youyuan.music.compose.data.repo

import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.SearchApi
import com.youyuan.music.compose.data.model.SongItem
import com.youyuan.music.compose.data.model.toSongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索数据仓库：持有 SearchApi，负责搜索请求与模型转换。
 */
@Singleton
class SearchRepo @Inject constructor(
    private val apiClient: ApiClient,
) {
    companion object {
        private const val TAG = "SearchRepo"
    }

    private val searchApi: SearchApi by lazy { apiClient.createService(SearchApi::class.java) }

    private fun throwIfRisk(code: Long?) {
        if (code == -462L) throw RepoRiskException("检测到您的网络环境存在风险，请稍后再试")
    }

    /** 搜索建议，返回关键词列表 */
    suspend fun searchSuggestions(keywords: String): List<String> = withContext(Dispatchers.IO) {
        val response = searchApi.searchSuggestions(keywords = keywords, type = "mobile")
        throwIfRisk(response.code)
        response.result?.allMatch?.mapNotNull { it.keyword } ?: emptyList()
    }

    /** 搜索歌曲，返回 SongItem 列表 */
    suspend fun searchSongs(
        keywords: String,
        limit: Int = 50,
    ): List<SongItem> = withContext(Dispatchers.IO) {
        val response = searchApi.searchSongs(keywords = keywords, limit = limit, type = 1)
        throwIfRisk(response.code)
        response.result?.songs?.map { it.toSongItem() } ?: emptyList()
    }
}