package com.youyuan.music.compose.data.repo

import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.BannerApi
import com.youyuan.music.compose.api.apis.HomeApi
import com.youyuan.music.compose.api.apis.PersonalFmApi
import com.youyuan.music.compose.api.apis.RecommendApi
import com.youyuan.music.compose.api.model.BannerItem
import com.youyuan.music.compose.api.model.PersonalizedNewSongItem
import com.youyuan.music.compose.api.model.PersonalizedPlaylistItem
import com.youyuan.music.compose.api.model.RecommendResourceItem
import com.youyuan.music.compose.api.model.ToplistItem
import com.youyuan.music.compose.data.model.SongItem
import com.youyuan.music.compose.data.model.toSongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 探索页数据仓库：持有 Banner/Home/Recommend/PersonalFm 四个 API。
 */
@Singleton
class ExploreRepo @Inject constructor(
    private val apiClient: ApiClient,
) {
    private val bannerApi: BannerApi by lazy { apiClient.createService(BannerApi::class.java) }
    private val homeApi: HomeApi by lazy { apiClient.createService(HomeApi::class.java) }
    private val recommendApi: RecommendApi by lazy { apiClient.createService(RecommendApi::class.java) }
    private val personalFmApi: PersonalFmApi by lazy { apiClient.createService(PersonalFmApi::class.java) }

    private fun throwIfRisk(code: Int?) {
        if (code == -462) throw RepoRiskException("检测到您的网络环境存在风险，请稍后再试")
    }

    // ---- 公共：Banner & 个性化 & 榜单 ----

    suspend fun getBanner(type: Int = 2): List<BannerItem> = withContext(Dispatchers.IO) {
        val resp = bannerApi.getBanner(type = type)
        throwIfRisk(resp.code)
        resp.banners ?: emptyList()
    }

    suspend fun getPersonalizedPlaylists(limit: Int = 10): List<PersonalizedPlaylistItem> = withContext(Dispatchers.IO) {
        val resp = homeApi.getPersonalized(limit = limit)
        throwIfRisk(resp.code)
        resp.result ?: emptyList()
    }

    suspend fun getPersonalizedNewSongs(limit: Int = 10): List<PersonalizedNewSongItem> = withContext(Dispatchers.IO) {
        val resp = homeApi.getPersonalizedNewSong(limit = limit)
        throwIfRisk(resp.code)
        resp.result ?: emptyList()
    }

    suspend fun getToplistDetail(): List<ToplistItem> = withContext(Dispatchers.IO) {
        val resp = homeApi.getToplistDetail()
        throwIfRisk(resp.code)
        resp.list ?: emptyList()
    }

    // ---- 登录后：每日推荐 & 私人 FM ----

    suspend fun getRecommendPlaylists(): List<RecommendResourceItem> = withContext(Dispatchers.IO) {
        val resp = recommendApi.getRecommendPlaylists()
        throwIfRisk(resp.code)
        resp.recommend ?: emptyList()
    }

    suspend fun getRecommendSongs(): List<SongItem> = withContext(Dispatchers.IO) {
        val resp = recommendApi.getRecommendSongs()
        throwIfRisk(resp.code)
        resp.data?.dailySongs?.map { it.toSongItem() } ?: emptyList()
    }

    suspend fun getPersonalFm(): List<SongItem> = withContext(Dispatchers.IO) {
        val resp = personalFmApi.getPersonalFm()
        throwIfRisk(resp.code)
        resp.data?.map { it.toSongItem() } ?: emptyList()
    }
}