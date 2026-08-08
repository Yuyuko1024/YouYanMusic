package com.youyuan.music.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youyuan.music.compose.api.model.CommentItem
import com.youyuan.music.compose.data.repo.RepoRiskException
import com.youyuan.music.compose.data.repo.SongCommentRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongCommentViewModel @Inject constructor(
    private val songCommentRepo: SongCommentRepo,
) : ViewModel() {

    private val _hotComments = MutableStateFlow<List<CommentItem>>(emptyList())
    val hotComments: StateFlow<List<CommentItem>> = _hotComments.asStateFlow()

    private val _comments = MutableStateFlow<List<CommentItem>>(emptyList())
    val comments: StateFlow<List<CommentItem>> = _comments.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentSongId: Long? = null
    private var offset: Int = 0

    fun consumeError() {
        _error.value = null
    }

    fun load(songId: Long, forceRefresh: Boolean = false) {
        if (songId <= 0) return
        if (!forceRefresh && currentSongId == songId && _comments.value.isNotEmpty()) return

        currentSongId = songId
        offset = 0
        _hotComments.value = emptyList()
        _comments.value = emptyList()
        _total.value = 0
        _hasMore.value = false

        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            try {
                val response = songCommentRepo.load(songId, offset)
                if (response.code != 200) {
                    _error.value = "加载评论失败: code=${response.code}"
                    return@launch
                }
                _hotComments.value = response.hotComments
                _comments.value = response.comments
                _total.value = response.total ?: response.comments.size
                _hasMore.value = response.more == true
                offset = response.comments.size
            } catch (e: RepoRiskException) {
                _error.value = e.message
            } catch (e: Exception) {
                _error.value = "加载评论失败: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadMore() {
        val songId = currentSongId ?: return
        if (_isRefreshing.value || _isLoadingMore.value) return
        if (!_hasMore.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            _error.value = null
            try {
                val response = songCommentRepo.load(songId, offset)
                if (response.code != 200) {
                    _error.value = "加载更多失败: code=${response.code}"
                    return@launch
                }
                _comments.value = _comments.value + response.comments
                _total.value = response.total ?: _total.value
                _hasMore.value = response.more == true
                offset += response.comments.size
            } catch (e: RepoRiskException) {
                _error.value = e.message
            } catch (e: Exception) {
                _error.value = "加载更多失败: ${e.message}"
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
}