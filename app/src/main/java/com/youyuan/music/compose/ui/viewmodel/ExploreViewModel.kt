package com.youyuan.music.compose.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.PlaylistApi
import com.youyuan.music.compose.api.apis.ProfileApi
import com.youyuan.music.compose.api.apis.RecommendApi
import com.youyuan.music.compose.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: ApiClient
) : ViewModel() {
    private val recommendApi: RecommendApi = apiClient.createService(RecommendApi::class.java)
    private val profileApi: ProfileApi = apiClient.createService(ProfileApi::class.java)

    private val _data = MutableStateFlow("")
    val data: StateFlow<String?> = _data.asStateFlow()

    fun testRecommendPlaylist() {
        viewModelScope.launch {
            _data.value = ""
            try {
                val response = recommendApi.getRecommendPlaylists()
                Logger.debug("data", response.toString())
                _data.value = response.toString()
            } catch (e: Exception) {

            }
        }
    }

    fun testRecommendSongs() {
        _data.value = ""
        viewModelScope.launch {
            try {
                val response = recommendApi.getRecommendSongs()
                Logger.debug("data", response.toString())
                _data.value = response.toString()
            } catch (e: Exception) {

            }
        }
    }

    fun testLikelist(uid: String) {
        _data.value = ""
        viewModelScope.launch {
            try {
                val response = profileApi.getLikelistById(uid)
                Logger.debug("data", response.toString())
                _data.value = response.toString()
            } catch (e: Exception) {

            }
        }
    }

}