package com.youyuan.music.compose.data.repo

import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.CommentApi
import com.youyuan.music.compose.api.model.CommentMusicResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongCommentRepo @Inject constructor(
    private val apiClient: ApiClient
) {

    companion object {
        private const val LIMIT = 100
    }

    private val commentApi: CommentApi = apiClient.createService(CommentApi::class.java)

    private fun throwIfRisk(code: Int?) {
        if (code == -462) throw RepoRiskException("检测到您的网络环境存在风险，请稍后再试")
    }

    /**
     * 加载一页歌曲评论。
     * 正常返回 [CommentMusicResponse]；风控或网络异常会直接抛出。
     */
    suspend fun load(songId: Long, offset: Int): CommentMusicResponse {
        val response = commentApi.getMusicComments(songId = songId, limit = LIMIT, offset = offset)
        throwIfRisk(response.code)
        return response
    }

}